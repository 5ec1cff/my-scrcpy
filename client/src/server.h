#ifndef SERVER_H
#define SERVER_H

#include "common.h"

#include <stdatomic.h>
#include <stdbool.h>
#include <stdint.h>

#include "adb.h"
#include "coords.h"
#include "scrcpy.h"
#include "util/log.h"
#include "util/net.h"
#include "util/thread.h"

struct server {
    atomic_flag server_socket_closed;

    socket_t server_socket; // only used if !tunnel_forward
    socket_t video_socket;
    socket_t control_socket;
    const char *addr;
    const char *port; // selected from port_range
};


// init default values
bool
server_init(struct server *server);

// discovery server
bool
server_discovery(struct server *server, const struct scrcpy_options *options);

#define DEVICE_NAME_FIELD_LENGTH 64
// block until the communication with the server is established
// device_name must point to a buffer of at least DEVICE_NAME_FIELD_LENGTH bytes
bool
server_connect_to(struct server *server, char *device_name, struct size *size, const struct scrcpy_options *options);

// disconnect and kill the server process
void
server_stop(struct server *server);

// close and release sockets
void
server_destroy(struct server *server);

#endif
