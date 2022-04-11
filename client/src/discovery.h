#ifndef DISCOVERY_H
#define DISCOVERY_H

#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

#include <fcntl.h>
#ifdef _WIN32
#include <winsock2.h>
#include <Ws2tcpip.h>
#include <iphlpapi.h>
#endif

int
send_query(const char *name, const char **addr, const char **port);

#define DISCOVERY_PORT htons((unsigned short)1415)
// 224.0.0.252
#define DISCOVERY_MULTICAST_ADDR_V4 htonl((((uint32_t)224U) << 24U) | ((uint32_t)252U))
// ff:02::fc
#define DISCOVERY_MULTICAST_ADDR_V6 ({struct in6_addr addr = { \
			.u = "\xFF\x02\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\xFC" \
		}; addr;})

//! Open and setup a IPv4 socket for mDNS/DNS-SD. To bind the socket to a specific interface, pass
//! in the appropriate socket address in saddr, otherwise pass a null pointer for INADDR_ANY. To
//! send one-shot discovery requests and queries pass a null pointer or set 0 as port to assign a
//! random user level ephemeral port. To run discovery service listening for incoming discoveries
//! and queries, you must set MDNS_PORT as port.
static inline int
mdns_socket_open_ipv4(const struct sockaddr_in* saddr);

//! Setup an already opened IPv4 socket for mDNS/DNS-SD. To bind the socket to a specific interface,
//! pass in the appropriate socket address in saddr, otherwise pass a null pointer for INADDR_ANY.
//! To send one-shot discovery requests and queries pass a null pointer or set 0 as port to assign a
//! random user level ephemeral port. To run discovery service listening for incoming discoveries
//! and queries, you must set MDNS_PORT as port.
static inline int
mdns_socket_setup_ipv4(int sock, const struct sockaddr_in* saddr);

//! Open and setup a IPv6 socket for mDNS/DNS-SD. To bind the socket to a specific interface, pass
//! in the appropriate socket address in saddr, otherwise pass a null pointer for in6addr_any. To
//! send one-shot discovery requests and queries pass a null pointer or set 0 as port to assign a
//! random user level ephemeral port. To run discovery service listening for incoming discoveries
//! and queries, you must set MDNS_PORT as port.
static inline int
mdns_socket_open_ipv6(const struct sockaddr_in6* saddr);

//! Setup an already opened IPv6 socket for mDNS/DNS-SD. To bind the socket to a specific interface,
//! pass in the appropriate socket address in saddr, otherwise pass a null pointer for in6addr_any.
//! To send one-shot discovery requests and queries pass a null pointer or set 0 as port to assign a
//! random user level ephemeral port. To run discovery service listening for incoming discoveries
//! and queries, you must set MDNS_PORT as port.
static inline int
mdns_socket_setup_ipv6(int sock, const struct sockaddr_in6* saddr);

//! Close a socket opened with mdns_socket_open_ipv4 and mdns_socket_open_ipv6.
static inline void
mdns_socket_close(int sock);

static inline int
mdns_unicast_send(int sock, const void* address, size_t address_size, const void* buffer,
                  size_t size);

static inline int
mdns_multicast_send(int sock, const void* buffer, size_t size);

static inline int
mdns_socket_open_ipv4(const struct sockaddr_in* saddr) {
	int sock = (int)socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
	if (sock < 0)
		return -1;
	if (mdns_socket_setup_ipv4(sock, saddr)) {
		mdns_socket_close(sock);
		return -1;
	}
	return sock;
}

static inline int
mdns_socket_setup_ipv4(int sock, const struct sockaddr_in* saddr) {
	unsigned char ttl = 1;
	unsigned char loopback = 1;
	unsigned int reuseaddr = 1;
	struct ip_mreq req;

	setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (const char*)&reuseaddr, sizeof(reuseaddr));
#ifdef SO_REUSEPORT
	setsockopt(sock, SOL_SOCKET, SO_REUSEPORT, (const char*)&reuseaddr, sizeof(reuseaddr));
#endif
	setsockopt(sock, IPPROTO_IP, IP_MULTICAST_TTL, (const char*)&ttl, sizeof(ttl));
	setsockopt(sock, IPPROTO_IP, IP_MULTICAST_LOOP, (const char*)&loopback, sizeof(loopback));

	memset(&req, 0, sizeof(req));
	req.imr_multiaddr.s_addr = DISCOVERY_MULTICAST_ADDR_V4;
	if (saddr)
		req.imr_interface = saddr->sin_addr;
	if (setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char*)&req, sizeof(req)))
		return -1;

	struct sockaddr_in sock_addr;
	if (!saddr) {
		memset(&sock_addr, 0, sizeof(struct sockaddr_in));
		sock_addr.sin_family = AF_INET;
		sock_addr.sin_addr.s_addr = INADDR_ANY;
#ifdef __APPLE__
		sock_addr.sin_len = sizeof(struct sockaddr_in);
#endif
	} else {
		memcpy(&sock_addr, saddr, sizeof(struct sockaddr_in));
		setsockopt(sock, IPPROTO_IP, IP_MULTICAST_IF, (const char*)&sock_addr.sin_addr,
		           sizeof(sock_addr.sin_addr));
#ifndef _WIN32
		sock_addr.sin_addr.s_addr = INADDR_ANY;
#endif
	}

	if (bind(sock, (struct sockaddr*)&sock_addr, sizeof(struct sockaddr_in)))
		return -1;

#ifdef _WIN32
	unsigned long param = 1;
	ioctlsocket(sock, FIONBIO, &param);
#else
	const int flags = fcntl(sock, F_GETFL, 0);
	fcntl(sock, F_SETFL, flags | O_NONBLOCK);
#endif

	return 0;
}

static inline int
mdns_socket_open_ipv6(const struct sockaddr_in6* saddr) {
	int sock = (int)socket(AF_INET6, SOCK_DGRAM, IPPROTO_UDP);
	if (sock < 0)
		return -1;
	if (mdns_socket_setup_ipv6(sock, saddr)) {
		mdns_socket_close(sock);
		return -1;
	}
	return sock;
}

static inline int
mdns_socket_setup_ipv6(int sock, const struct sockaddr_in6* saddr) {
	int hops = 1;
	unsigned int loopback = 1;
	unsigned int reuseaddr = 1;
	struct ipv6_mreq req;

	setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (const char*)&reuseaddr, sizeof(reuseaddr));
#ifdef SO_REUSEPORT
	setsockopt(sock, SOL_SOCKET, SO_REUSEPORT, (const char*)&reuseaddr, sizeof(reuseaddr));
#endif
	setsockopt(sock, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, (const char*)&hops, sizeof(hops));
	setsockopt(sock, IPPROTO_IPV6, IPV6_MULTICAST_LOOP, (const char*)&loopback, sizeof(loopback));

	memset(&req, 0, sizeof(req));
	req.ipv6mr_multiaddr = DISCOVERY_MULTICAST_ADDR_V6;
	if (setsockopt(sock, IPPROTO_IPV6, IPV6_JOIN_GROUP, (char*)&req, sizeof(req)))
		return -1;

	struct sockaddr_in6 sock_addr;
	if (!saddr) {
		memset(&sock_addr, 0, sizeof(struct sockaddr_in6));
		sock_addr.sin6_family = AF_INET6;
		sock_addr.sin6_addr = in6addr_any;
#ifdef __APPLE__
		sock_addr.sin6_len = sizeof(struct sockaddr_in6);
#endif
	} else {
		memcpy(&sock_addr, saddr, sizeof(struct sockaddr_in6));
		unsigned int ifindex = 0;
		setsockopt(sock, IPPROTO_IPV6, IPV6_MULTICAST_IF, (const char*)&ifindex, sizeof(ifindex));
#ifndef _WIN32
		sock_addr.sin6_addr = in6addr_any;
#endif
	}

	if (bind(sock, (struct sockaddr*)&sock_addr, sizeof(struct sockaddr_in6)))
		return -1;

#ifdef _WIN32
	unsigned long param = 1;
	ioctlsocket(sock, FIONBIO, &param);
#else
	const int flags = fcntl(sock, F_GETFL, 0);
	fcntl(sock, F_SETFL, flags | O_NONBLOCK);
#endif

	return 0;
}

static inline void
mdns_socket_close(int sock) {
#ifdef _WIN32
	closesocket(sock);
#else
	close(sock);
#endif
}

static inline int
mdns_unicast_send(int sock, const void* address, size_t address_size, const void* buffer,
                  size_t size) {
	if (sendto(sock, (const char*)buffer, (int)size, 0, (const struct sockaddr*)address,
	           (socklen_t)address_size) < 0)
		return -1;
	return 0;
}

static inline int
mdns_multicast_send(int sock, const void* buffer, size_t size) {
	struct sockaddr_storage addr_storage;
	struct sockaddr_in addr;
	struct sockaddr_in6 addr6;
	struct sockaddr* saddr = (struct sockaddr*)&addr_storage;
	socklen_t saddrlen = sizeof(struct sockaddr_storage);
	if (getsockname(sock, saddr, &saddrlen))
		return -1;
	if (saddr->sa_family == AF_INET6) {
		memset(&addr6, 0, sizeof(addr6));
		addr6.sin6_family = AF_INET6;
#ifdef __APPLE__
		addr6.sin6_len = sizeof(addr6);
#endif
		addr6.sin6_addr = DISCOVERY_MULTICAST_ADDR_V6;
		addr6.sin6_port = DISCOVERY_PORT;
		saddr = (struct sockaddr*)&addr6;
		saddrlen = sizeof(addr6);
	} else {
		memset(&addr, 0, sizeof(addr));
		addr.sin_family = AF_INET;
#ifdef __APPLE__
		addr.sin_len = sizeof(addr);
#endif
		addr.sin_addr.s_addr = DISCOVERY_MULTICAST_ADDR_V4;
		addr.sin_port = DISCOVERY_PORT;
		saddr = (struct sockaddr*)&addr;
		saddrlen = sizeof(addr);
	}

	if (sendto(sock, (const char*)buffer, (int)size, 0, saddr, saddrlen) < 0)
		return -1;
	return 0;
}

#endif