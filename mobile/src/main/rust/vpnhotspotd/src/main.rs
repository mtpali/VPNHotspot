mod control;
mod dns;
mod downstream;
mod firewall;
mod ipsec;
mod nat66;
mod neighbour;
mod netlink;
mod platform;
mod process_io;
mod report;
mod routing;
mod session;
mod socket;
mod traffic;
mod upstream;

use std::env;
use std::io;

#[tokio::main]
async fn main() -> io::Result<()> {
    let mut args = env::args().skip(1);
    let socket_name = args
        .next()
        .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidInput, "missing socket name"))?;
    let api_level = args
        .next()
        .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidInput, "missing Android API level"))?
        .parse::<i32>()
        .map_err(|error| {
            io::Error::new(
                io::ErrorKind::InvalidInput,
                format!("invalid Android API level: {error}"),
            )
        })?;
    if api_level < 28 {
        return Err(io::Error::new(
            io::ErrorKind::Unsupported,
            "Android 9 or newer required",
        ));
    }
    if let Some(arg) = args.next() {
        return Err(io::Error::new(
            io::ErrorKind::InvalidInput,
            format!("unexpected argument {arg}"),
        ));
    }
    platform::ANDROID_API_LEVEL
        .set(api_level)
        .map_err(|_| io::Error::other("Android API level already initialized"))?;
    control::run(socket_name).await
}
