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
    union {
        uint32_t v4;
    } addr;
    uint16_t local_port; // selected from port_range
};

struct server_params {
    const char *address;
    enum sc_log_level log_level;
    const char *crop;
    const char *codec_options;
    const char *encoder_name;
    uint16_t port;
    uint16_t max_size;
    uint32_t bit_rate;
    uint16_t max_fps;
    int8_t lock_video_orientation;
    bool control;
    uint32_t display_id;
    bool show_touches;
    bool stay_awake;
    bool force_adb_forward;
    bool power_off_on_close;
};

// init default values
bool
server_init(struct server *server);

// push, enable tunnel et start the server
bool
server_start(struct server *server, const struct server_params *params);

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
