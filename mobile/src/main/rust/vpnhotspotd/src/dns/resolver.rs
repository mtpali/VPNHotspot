use std::io;
use std::net::SocketAddr;
use std::os::fd::{AsRawFd, RawFd};
use std::sync::OnceLock;
use std::time::Duration;

use libc::c_int;
use socket2::{Domain, Protocol, SockAddr, Socket, Type};
use tokio::io::unix::AsyncFd;
use tokio::io::{AsyncReadExt, AsyncWriteExt, Interest, Ready};
use tokio::net::{TcpStream, UdpSocket};
use tokio::time::timeout;

use crate::{platform, report, socket, upstream};
use vpnhotspotd::shared::dns_wire;
use vpnhotspotd::shared::model::Network;

use super::DNS_MAX_PACKET;

// android/multinetwork.h: ANDROID_RESOLV_NO_RETRY.
const ANDROID_RESOLV_NO_RETRY: u32 = 1 << 0;
// RFC 1035 section 4.2.1 recommends a minimum retransmission interval of 5 seconds.
const DNS_RESPONSE_TIMEOUT: Duration = Duration::from_secs(5);

type ResNsend = unsafe extern "C" fn(u64, *const u8, usize, u32) -> c_int;
type ResNresult = unsafe extern "C" fn(c_int, *mut c_int, *mut u8, usize) -> c_int;
type ResCancel = unsafe extern "C" fn(c_int);

struct ResolverApi {
    send: ResNsend,
    result: ResNresult,
    cancel: ResCancel,
}

/// Raw Android resolver functions were introduced in API 29. Load them only on that runtime;
/// strong ELF imports would prevent an API-28 daemon from starting at all.
/// https://android.googlesource.com/platform/frameworks/base/+/android-10.0.0_r1/native/android/net.c
/// https://android.googlesource.com/platform/frameworks/base/+/android-9.0.0_r1/native/android/net.c
fn resolver_api() -> io::Result<&'static ResolverApi> {
    static API: OnceLock<Option<ResolverApi>> = OnceLock::new();
    API.get_or_init(|| unsafe {
        // libandroid is already loaded by upstream::set_socket_network's public API-23 import.
        let send = libc::dlsym(libc::RTLD_DEFAULT, c"android_res_nsend".as_ptr());
        let result = libc::dlsym(libc::RTLD_DEFAULT, c"android_res_nresult".as_ptr());
        let cancel = libc::dlsym(libc::RTLD_DEFAULT, c"android_res_cancel".as_ptr());
        if send.is_null() || result.is_null() || cancel.is_null() {
            None
        } else {
            Some(ResolverApi {
                send: std::mem::transmute::<*mut libc::c_void, ResNsend>(send),
                result: std::mem::transmute::<*mut libc::c_void, ResNresult>(result),
                cancel: std::mem::transmute::<*mut libc::c_void, ResCancel>(cancel),
            })
        }
    })
    .as_ref()
    .ok_or_else(|| io::Error::new(io::ErrorKind::Unsupported, "Android resolver API missing"))
}

struct ResolverQuery {
    fd: Option<RawFd>,
    api: &'static ResolverApi,
}

impl ResolverQuery {
    fn finish(mut self) -> io::Result<Vec<u8>> {
        let mut rcode = 0;
        let mut response = vec![0u8; DNS_MAX_PACKET];
        let size = unsafe {
            (self.api.result)(
                self.fd.take().unwrap(),
                &mut rcode,
                response.as_mut_ptr(),
                response.len(),
            )
        };
        if size < 0 {
            Err(io::Error::from_raw_os_error(-size))
        } else {
            response.truncate(size as usize);
            Ok(response)
        }
    }
}

impl AsRawFd for ResolverQuery {
    fn as_raw_fd(&self) -> RawFd {
        self.fd.unwrap()
    }
}

impl Drop for ResolverQuery {
    fn drop(&mut self) {
        if let Some(fd) = self.fd.take() {
            unsafe {
                (self.api.cancel)(fd);
            }
        }
    }
}

pub(super) async fn query_network(
    network: Network,
    servers: &[SocketAddr],
    query: &[u8],
) -> (io::Result<Vec<u8>>, bool) {
    if platform::android_api_level() < 29 {
        return query_legacy(network, servers, query).await;
    }
    let api = match resolver_api() {
        Ok(api) => api,
        Err(error) => {
            report::message(
                "dns.resolver_api",
                error.to_string(),
                format!("{:?}", error.kind()),
            );
            return (Err(error), false);
        }
    };
    let fd = unsafe {
        (api.send)(
            network,
            query.as_ptr(),
            query.len(),
            ANDROID_RESOLV_NO_RETRY,
        )
    };
    if fd < 0 {
        return (Err(io::Error::from_raw_os_error(-fd)), false);
    }
    let fd = ResolverQuery { fd: Some(fd), api };
    if let Err(error) = socket::set_nonblocking(fd.as_raw_fd()) {
        return (Err(error), true);
    }
    let fd = match AsyncFd::new(fd) {
        Ok(fd) => fd,
        Err(error) => return (Err(error), true),
    };
    (read_resolver_result(fd).await, true)
}

async fn read_resolver_result(fd: AsyncFd<ResolverQuery>) -> io::Result<Vec<u8>> {
    // android_res_nresult is the public result reader/closer, but it performs synchronous reads.
    // dnsproxyd writes one result then closes the socket; wait for EOF before handing it back.
    loop {
        let mut ready = fd.ready(Interest::READABLE | Interest::ERROR).await?;
        let state = ready.ready();
        if state.is_read_closed() || state.is_error() {
            drop(ready);
            return fd.into_inner().finish();
        }
        ready.clear_ready_matching(Ready::READABLE);
    }
}

async fn query_legacy(
    network: Network,
    servers: &[SocketAddr],
    query: &[u8],
) -> (io::Result<Vec<u8>>, bool) {
    if dns_wire::servfail_response(query).is_none() {
        return (
            Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "invalid DNS query",
            )),
            false,
        );
    }
    let mut sent = false;
    let mut last_error = io::Error::new(
        io::ErrorKind::NotConnected,
        "selected network has no DNS servers",
    );
    for &server in servers {
        let result = timeout(
            DNS_RESPONSE_TIMEOUT,
            exchange_legacy(network, server, query, &mut sent),
        )
        .await;
        let error = match result {
            Ok(Ok(response)) => return (Ok(response), sent),
            Ok(Err(error)) => error,
            Err(_) => io::Error::new(io::ErrorKind::TimedOut, "DNS server response timeout"),
        };
        if error.kind() != io::ErrorKind::TimedOut && !socket::is_route_unreachable(&error) {
            let reported = match error.raw_os_error() {
                Some(errno) => io::Error::from_raw_os_error(errno),
                None => io::Error::new(error.kind(), error.to_string()),
            };
            report::io_with_details(
                "dns.legacy_query",
                reported,
                [
                    ("network", network.to_string()),
                    ("server", server.to_string()),
                ],
            );
        }
        last_error = error;
    }
    (Err(last_error), sent)
}

async fn exchange_legacy(
    network: Network,
    server: SocketAddr,
    query: &[u8],
    sent: &mut bool,
) -> io::Result<Vec<u8>> {
    let udp = Socket::new(
        Domain::for_address(server),
        Type::DGRAM,
        Some(Protocol::UDP),
    )?;
    upstream::set_socket_network(network, udp.as_raw_fd())?;
    udp.set_nonblocking(true)?;
    udp.connect(&SockAddr::from(server))?;
    let udp = UdpSocket::from_std(udp.into())?;
    if udp.send(query).await? != query.len() {
        return Err(io::Error::new(
            io::ErrorKind::WriteZero,
            "short DNS query write",
        ));
    }
    *sent = true;
    let mut response = vec![0u8; DNS_MAX_PACKET];
    loop {
        let length = udp.recv(&mut response).await?;
        if dns_wire::response_matches_query(query, &response[..length]) {
            response.truncate(length);
            break;
        }
    }
    // RFC 1035 TC bit: retry a truncated response over TCP on the same selected network/server.
    if response[2] & 0x02 == 0 {
        return Ok(response);
    }
    let tcp = Socket::new(
        Domain::for_address(server),
        Type::STREAM,
        Some(Protocol::TCP),
    )?;
    upstream::set_socket_network(network, tcp.as_raw_fd())?;
    tcp.set_nonblocking(true)?;
    if let Err(error) = tcp.connect(&SockAddr::from(server)) {
        if error.kind() != io::ErrorKind::WouldBlock
            && error.raw_os_error() != Some(libc::EINPROGRESS)
        {
            return Err(error);
        }
        socket::await_connect(&tcp).await?;
    }
    let mut tcp = TcpStream::from_std(tcp.into())?;
    let length = u16::try_from(query.len())
        .map_err(|_| io::Error::new(io::ErrorKind::InvalidData, "DNS query too large"))?;
    tcp.write_all(&length.to_be_bytes()).await?;
    tcp.write_all(query).await?;
    let length = tcp.read_u16().await? as usize;
    let mut response = vec![0; length];
    tcp.read_exact(&mut response).await?;
    if !dns_wire::response_matches_query(query, &response) {
        return Err(io::Error::new(
            io::ErrorKind::InvalidData,
            "mismatched DNS TCP response",
        ));
    }
    Ok(response)
}
