#include "net.h"

#include <stdio.h>
#include <SDL2/SDL_platform.h>

#include "log.h"

#ifdef __WINDOWS__
  typedef int socklen_t;
#else
# include <sys/types.h>
# include <sys/socket.h>
# include <netinet/in.h>
# include <arpa/inet.h>
# include <unistd.h>
# define SOCKET_ERROR -1
  typedef struct sockaddr_in SOCKADDR_IN;
  typedef struct sockaddr SOCKADDR;
  typedef struct in_addr IN_ADDR;
#endif

socket_t
net_connect(const char *host, const char *port) {
    struct addrinfo hints;
    struct addrinfo *result, *rp;
    char buf[32];
    // 11003: https://stackoverflow.com/questions/22748234/getaddrinfo-function-throw-error-n-11003
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = 0;
    hints.ai_protocol = 0;
    int s = getaddrinfo(host, port, &hints, &result);
    if (s != 0) {
        LOGE("getaddrinfo for %s %s (%d): %s", host, port, s, gai_strerror(s));
        return INVALID_SOCKET;
    }

    socket_t sock = INVALID_SOCKET;
    for (rp = result; rp != NULL; rp = rp->ai_next) {
        sock = socket(rp->ai_family, rp->ai_socktype, 0);
        if (sock == INVALID_SOCKET) {
            // perror("socket");
            continue;
        }
        // https://gist.github.com/twslankard/1001201#file-getaddrinfo-c-L30
        inet_ntop(rp->ai_family, &((SOCKADDR_IN*)rp->ai_addr)->sin_addr, buf, 32);
        LOGV("try connect to %s", buf);
        

        if (connect(sock, (SOCKADDR *) rp->ai_addr, rp->ai_addrlen) == SOCKET_ERROR) {
            // perror("connect");
            net_close(sock);
            continue;
        }

        break;
    }

    freeaddrinfo(result);

    return sock;
}

socket_t
net_listen(uint32_t addr, uint16_t port, int backlog) {
    socket_t sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == INVALID_SOCKET) {
        perror("socket");
        return INVALID_SOCKET;
    }

    int reuse = 1;
    if (setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (const void *) &reuse,
                   sizeof(reuse)) == -1) {
        perror("setsockopt(SO_REUSEADDR)");
    }

    SOCKADDR_IN sin;
    sin.sin_family = AF_INET;
    sin.sin_addr.s_addr = htonl(addr); // htonl() harmless on INADDR_ANY
    sin.sin_port = htons(port);

    if (bind(sock, (SOCKADDR *) &sin, sizeof(sin)) == SOCKET_ERROR) {
        perror("bind");
        net_close(sock);
        return INVALID_SOCKET;
    }

    if (listen(sock, backlog) == SOCKET_ERROR) {
        perror("listen");
        net_close(sock);
        return INVALID_SOCKET;
    }

    return sock;
}

socket_t
net_accept(socket_t server_socket) {
    SOCKADDR_IN csin;
    socklen_t sinsize = sizeof(csin);
    return accept(server_socket, (SOCKADDR *) &csin, &sinsize);
}

ssize_t
net_recv(socket_t socket, void *buf, size_t len) {
    return recv(socket, buf, len, 0);
}

ssize_t
net_recv_all(socket_t socket, void *buf, size_t len) {
    return recv(socket, buf, len, MSG_WAITALL);
}

ssize_t
net_send(socket_t socket, const void *buf, size_t len) {
    return send(socket, buf, len, 0);
}

ssize_t
net_send_all(socket_t socket, const void *buf, size_t len) {
    ssize_t w = 0;
    while (len > 0) {
        w = send(socket, buf, len, 0);
        if (w == -1) {
            return -1;
        }
        len -= w;
        buf = (char *) buf + w;
    }
    return w;
}

bool
net_shutdown(socket_t socket, int how) {
    return !shutdown(socket, how);
}

bool
net_init(void) {
#ifdef __WINDOWS__
    WSADATA wsa;
    int res = WSAStartup(MAKEWORD(2, 2), &wsa) < 0;
    if (res < 0) {
        LOGC("WSAStartup failed with error %d", res);
        return false;
    }
#endif
    return true;
}

void
net_cleanup(void) {
#ifdef __WINDOWS__
    WSACleanup();
#endif
}

bool
net_close(socket_t socket) {
#ifdef __WINDOWS__
    return !closesocket(socket);
#else
    return !close(socket);
#endif
}
