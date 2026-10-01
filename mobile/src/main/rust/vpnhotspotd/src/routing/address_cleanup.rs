use std::io;
use std::net::IpAddr;

use rtnetlink::packet_route::AddressFamily;

use crate::{netlink, report};
use vpnhotspotd::shared::model::{
    ipv6_nat_gateway, ipv6_nat_prefix, ANDROID_ROUTE_TABLE_LOCAL_NETWORK, DAEMON_TABLE,
};
use vpnhotspotd::shared::proto::daemon::CleanRoutingCommand;

use super::netlink_commands::{
    address_details, apply_address_command, apply_route_command, flush_routes, is_missing,
    is_missing_address, route_details, IpAddressCommand, IpOperation, IpRouteCommand, RouteType,
};

pub(super) async fn clean_ip(
    connection: &mut netlink::RequestConnection,
    command: &CleanRoutingCommand,
) -> io::Result<()> {
    flush_routes(connection, AddressFamily::Inet6, DAEMON_TABLE).await?;
    for interface in netlink::link_names(connection).await?.into_values() {
        let prefix = ipv6_nat_prefix(&command.ipv6_nat_prefix_seed, &interface);
        let gateway = ipv6_nat_gateway(prefix);
        let address = IpAddressCommand {
            operation: IpOperation::Delete,
            address: IpAddr::V6(gateway.address()),
            prefix_len: gateway.network_length(),
            interface: interface.clone(),
        };
        if let Err(e) = apply_address_command(connection, &address).await {
            if !is_missing_address(&e) {
                report::io_with_details("routing.clean_ip.address", e, address_details(&address));
            }
        }
        let route = IpRouteCommand {
            operation: IpOperation::Delete,
            route_type: RouteType::Unicast,
            destination: IpAddr::V6(prefix.first_address()),
            prefix_len: prefix.network_length(),
            interface,
            table: ANDROID_ROUTE_TABLE_LOCAL_NETWORK,
        };
        if let Err(e) = apply_route_command(connection, &route).await {
            if !is_missing(&e) {
                report::io_with_details("routing.clean_ip.route", e, route_details(&route));
            }
        }
    }
    Ok(())
}
