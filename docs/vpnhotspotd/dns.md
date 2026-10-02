# DNS

The DNS runtime is per session. It owns per-MAC daemon listeners for downstream
DNS and the handoff to Android's resolver API. Routing owns how downstream
packets reach those listeners.

## Listener Ownership

[`dns::Runtime::start`](../../mobile/src/main/rust/vpnhotspotd/src/dns.rs)
attempts to bind TCP and UDP listeners on ephemeral ports for each allowed
client MAC. Each MAC/protocol listener is an independent best-effort
capability. A listener that starts publishes its port to routing; a listener
that fails is reported as a structured nonfatal with downstream, MAC, and
protocol, and that MAC/protocol DNS redirect is omitted from routing.

Routing redirects downstream IPv4 DNS with MAC-matched DNAT rules to the
ephemeral ports that exist. Each redirected port also has a `filter INPUT`
guard: packets are allowed to reach the listener only when conntrack says the
original destination was the downstream gateway on port 53. Direct connections
to another client's listener port are rejected before the daemon accepts them.
Packets still addressed to the downstream gateway on port 53 are rejected in
`filter INPUT`, so blocked clients and missing capability cases do not fall
through to an accidental local DNS service.

NAT66 TCP and UDP also special-case DNS to the NAT66 gateway on port 53 and
call the same DNS handlers from the per-MAC NAT66 listener context. This keeps
resolver selection, MAC attribution, accounting, and DNS response generation in
one runtime instead of duplicating DNS behavior in NAT66.

## Config Snapshots

The DNS runtime receives the shared session config. For each query or TCP
connection, it clones a snapshot before resolving. That snapshot determines the
primary and fallback Android networks used for resolver calls.

Current selection is simple:

1. use `primary_network` when present;
2. otherwise use `fallback_network` when present;
3. otherwise return a DNS failure.

DNS does not hold the config mutex while waiting for Android resolver results.
The selected network is internal resolver state; DNS counters are not persisted
by actual upstream interface.

## Resolver Handoff

On API 29+, DNS queries are sent through bionic's Android resolver API. The
three function pointers are resolved once with `dlsym` only on that runtime;
there are no strong ELF imports of functions absent on Android 9:

- `android_res_nsend` starts a one-shot query on the selected Android network;
- `android_res_nresult` reads and closes the result;
- `android_res_cancel` is used if the query object is dropped before finish.

`android_res_nsend` returns a file descriptor. The daemon sets it nonblocking
and wraps it in `AsyncFd`. The daemon waits for the resolver-side socket to
become readable and then for EOF before calling `android_res_nresult`.

This shape exists because `android_res_nresult` is the public result
reader/closer. The root README records the platform assumption: the resolver
service writes the complete result before returning and closes the socket when
the result is ready, so the daemon can stay nonblocking while still using the
public result API.

DNS accounting counts the DNS payload bytes handed to `android_res_nsend` and
the response bytes returned by `android_res_nresult`. It does not try to account
for Android's physical DNS transport, DNS-over-TLS, packet headers, or resolver
retransmits.

Locally generated DNS errors are client responses, not resolver responses. If
no resolver query is handed to Android, the daemon returns the client-visible
error without increasing DNS counters. If `android_res_nsend` accepts a query
but no resolver response is returned, only the sent query side is counted.

## Android 9 DNS

API 28 has no `android_res_nsend`/`android_res_nresult`/`android_res_cancel`.
Kotlin includes the primary and fallback networks' `LinkProperties.dnsServers`
(including IPv6 scope IDs) in each session config. For the same selected network
as the normal resolver path, the daemon:

- creates a nonblocking UDP socket and binds it to that network with the public
  `android_setsocknetwork` API before connecting to a configured DNS address on port 53;
- sends the original query and accepts only responses matching its ID, opcode,
  question count and question bytes from that connected peer;
- retries truncated UDP answers over length-prefixed TCP on the same network/server;
- uses a five-second response deadline per configured server, following RFC 1035's
  minimum retransmission interval, and then tries the next DNS server of the same network;
- returns SERVFAIL when the selected network has no servers or all attempts fail.

No public resolver is substituted and failure never causes fallback to a different
network. Primary-network presence retains precedence over the fallback even when
its DNS servers are absent or unavailable. This avoids bypassing the selected VPN.
The Android-9 forwarding path does not inherit system Private DNS/DoT: transport
is ordinary UDP/TCP inside the selected network, so encrypted transit requires a
VPN-provided DNS route. Android 10+ keeps the platform resolver behavior.

The sockets exist only for the query future. Listener/session cancellation drops
that future and closes its sockets; normal completion and timeouts do the same.
There are no new kernel rules, resolver settings, caches, or persistent files, and
Clean has no additional DNS state to remove. Unexpected socket/resolver failures
are emitted as structured nonfatal reports with network/server context and errno.
DNS counters count a logical query and its accepted answer once, regardless of
retransmission or TCP fallback, excluding transport headers as on the newer path.

## TCP DNS

TCP DNS accepts normal DNS-over-TCP framing:

- read a two-byte length;
- read exactly that many query bytes;
- resolve the query through the selected Android network;
- write a two-byte response length and response bytes.

Each framed query and framed response is one DNS TCP accounting unit. DNS TCP
does not try to infer lower-layer TCP packet counts.

Unexpected EOF while reading the next frame ends the connection cleanly.
Host- or network-unreachable response writes are treated as downstream
reachability churn and logged. Other I/O failures are returned to the
connection task.

## UDP DNS

UDP DNS reads one datagram, clones the config snapshot, and resolves the query
in a child task. If resolution succeeds, the response is sent back to the
datagram source. Host- or network-unreachable reply sends are treated as
downstream reachability churn and logged with source context. Other send
failures are reported with source context.

The UDP listener intentionally does not serialize all queries through one
worker. Each query has its own child task tied to the session stop token.
Each query datagram and response datagram is one DNS UDP accounting unit.

## Failure Semantics

Resolver failure normally returns a SERVFAIL response when the query can be
parsed enough to build one. If a SERVFAIL response cannot be generated, the
query is dropped.

Unexpected per-query resolver failures are logged but do not stop the DNS
runtime. Routine resolver unavailability, such as no selected upstream network
or an Android resolver timeout, returns SERVFAIL when possible without emitting
one stderr log per query. Listener setup failures do not stop the session.
Routing omits the missing DNS redirect, so normal IP traffic and manually
configured downstream DNS can still work.

Per-MAC listener setup and routing failures remove only that MAC/protocol DNS
capability. If a listener was staged but routing did not commit the matching
MAC redirect and direct-port guard, the staged listener is cancelled before the
session publishes committed capabilities. A TCP listener accept failure after
cancellation is treated as teardown; transient active-listener accept failures
are retried.
