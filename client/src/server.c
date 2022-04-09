#include "server.h"

#include <assert.h>
#include <errno.h>
#include <inttypes.h>
#include <libgen.h>
#include <stdio.h>
#include <SDL2/SDL_timer.h>
#include <SDL2/SDL_platform.h>

#include "adb.h"
#include "util/log.h"
#include "util/net.h"
#include "util/str_util.h"
#include "control_msg.h"
#include "scrcpy.h"

static const char *
log_level_to_server_string(enum sc_log_level level) {
    switch (level) {
        case SC_LOG_LEVEL_VERBOSE:
            return "verbose";
        case SC_LOG_LEVEL_DEBUG:
            return "debug";
        case SC_LOG_LEVEL_INFO:
            return "info";
        case SC_LOG_LEVEL_WARN:
            return "warn";
        case SC_LOG_LEVEL_ERROR:
            return "error";
        default:
            assert(!"unexpected log level");
            return "(unknown)";
    }
}

/*
static process_t
execute_server(struct server *server, const struct server_params *params) {
    char max_size_string[6];
    char bit_rate_string[11];
    char max_fps_string[6];
    char lock_video_orientation_string[5];
    char display_id_string[11];
    sprintf(max_size_string, "%"PRIu16, params->max_size);
    sprintf(bit_rate_string, "%"PRIu32, params->bit_rate);
    sprintf(max_fps_string, "%"PRIu16, params->max_fps);
    sprintf(lock_video_orientation_string, "%"PRIi8, params->lock_video_orientation);
    sprintf(display_id_string, "%"PRIu32, params->display_id);
    const char *const cmd[] = {
        "shell",
        "CLASSPATH=" DEVICE_SERVER_PATH,
        "/data/local/tmp/scrcpy-wrapper",
#ifdef SERVER_DEBUGGER
# define SERVER_DEBUGGER_PORT "5005"
# ifdef SERVER_DEBUGGER_METHOD_NEW
        /* Android 9 and above 
        "-XjdwpProvider:internal -XjdwpOptions:transport=dt_socket,suspend=y,server=y,address="
# else
        /* Android 8 and below 
        "-agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address="
# endif
            SERVER_DEBUGGER_PORT,
#endif
        "/", // unused
        "com.genymobile.scrcpy.Server",
        SCRCPY_VERSION,
        log_level_to_server_string(params->log_level),
        max_size_string,
        bit_rate_string,
        max_fps_string,
        lock_video_orientation_string,
        server->tunnel_forward ? "true" : "false",
        params->crop ? params->crop : "-",
        "true", // always send frame meta (packet boundaries + timestamp)
        params->control ? "true" : "false",
        display_id_string,
        params->show_touches ? "true" : "false",
        params->stay_awake ? "true" : "false",
        params->codec_options ? params->codec_options : "-",
        params->encoder_name ? params->encoder_name : "-",
        params->power_off_on_close ? "true" : "false",
        "ime" // ext options
    };
#ifdef SERVER_DEBUGGER
    LOGI("Server debugger waiting for a client on device port "
         SERVER_DEBUGGER_PORT "...");
    // From the computer, run
    //     adb forward tcp:5005 tcp:5005
    // Then, from Android Studio: Run > Debug > Edit configurations...
    // On the left, click on '+', "Remote", with:
    //     Host: localhost
    //     Port: 5005
    // Then click on "Debug"
#endif
    return adb_execute(server->serial, cmd, ARRAY_LEN(cmd));
}*/

/*
static socket_t
connect_and_read_byte(uint16_t port) {
    socket_t socket = net_connect(IPV4_LOCALHOST, port);
    if (socket == INVALID_SOCKET) {
        return INVALID_SOCKET;
    }

    char byte;
    // the connection may succeed even if the server behind the "adb tunnel"
    // is not listening, so read one byte to detect a working connection
    if (net_recv(socket, &byte, 1) != 1) {
        // the server is not listening yet behind the adb tunnel
        net_close(socket);
        return INVALID_SOCKET;
    }
    return socket;
}

static socket_t
connect_to_server(uint16_t port, uint32_t attempts, uint32_t delay) {
    do {
        LOGD("Remaining connection attempts: %d", (int) attempts);
        socket_t socket = connect_and_read_byte(port);
        if (socket != INVALID_SOCKET) {
            // it worked!
            return socket;
        }
        if (attempts) {
            SDL_Delay(delay);
        }
    } while (--attempts > 0);
    return INVALID_SOCKET;
}*/

static void
close_socket(socket_t socket) {
    assert(socket != INVALID_SOCKET);
    net_shutdown(socket, SHUT_RDWR);
    if (!net_close(socket)) {
        LOGW("Could not close socket");
    }
}

bool
server_init(struct server *server) {
    server->addr.v4 = INADDR_NONE;
    atomic_flag_clear_explicit(&server->server_socket_closed,
                               memory_order_relaxed);

    server->server_socket = INVALID_SOCKET;
    server->video_socket = INVALID_SOCKET;
    server->control_socket = INVALID_SOCKET;

    server->local_port = 0;

    return true;
}

bool
server_start(struct server *server, const struct server_params *params) {
    if (params->address) {
        uint32_t addr = inet_addr(params->address);
        if (addr == INADDR_NONE) {
            LOGE("invalid ipv4 address %s", params->address);
            return false;
        }
        server->addr.v4 = addr;
    }

    if (params->port > 0 && params->port < 65535) {
        server->local_port = params->port;
    } else {
        LOGE("no port specified");
        return false;
    }

    /*

    if (!push_server(params->address)) {
        /* server->serial will be freed on server_destroy() /
        return false;
    }

    if (!enable_tunnel_any_port(server, params->port_range,
                                params->force_adb_forward)) {
        return false;
    }

    // server will connect to our server socket
    server->process = execute_server(server, params);
    if (server->process == PROCESS_NONE) {
        goto error;
    }

    // If the server process dies before connecting to the server socket, then
    // the client will be stuck forever on accept(). To avoid the problem, we
    // must be able to wake up the accept() call when the server dies. To keep
    // things simple and multiplatform, just spawn a new thread waiting for the
    // server process and calling shutdown()/close() on the server socket if
    // necessary to wake up any accept() blocking call.
    bool ok = sc_thread_create(&server->wait_server_thread, run_wait_server,
                               "wait-server", server);
    if (!ok) {
        process_terminate(server->process);
        process_wait(server->process, true); // ignore exit code
        goto error;
    }

    server->tunnel_enabled = true;*/

    return true;
}

static bool
send_init_msg(socket_t socket,
              const struct control_msg *msg) {
    static unsigned char serialized_msg[128];
    size_t length = control_msg_serialize(msg, serialized_msg);
    if (!length) {
        return false;
    }
    int w = net_send_all(socket, serialized_msg, length);
    return (size_t) w == length;
}

static bool
device_read_info(socket_t device_socket, char *device_name, struct size *size) {
    unsigned char buf[DEVICE_NAME_FIELD_LENGTH + 4];
    int r = net_recv_all(device_socket, buf, sizeof(buf));
    if (r < DEVICE_NAME_FIELD_LENGTH + 4) {
        LOGE("Could not retrieve device information");
        return false;
    }
    // in case the client sends garbage
    buf[DEVICE_NAME_FIELD_LENGTH - 1] = '\0';
    // strcpy is safe here, since name contains at least
    // DEVICE_NAME_FIELD_LENGTH bytes and strlen(buf) < DEVICE_NAME_FIELD_LENGTH
    strcpy(device_name, (char *) buf);
    size->width = (buf[DEVICE_NAME_FIELD_LENGTH] << 8)
            | buf[DEVICE_NAME_FIELD_LENGTH + 1];
    size->height = (buf[DEVICE_NAME_FIELD_LENGTH + 2] << 8)
            | buf[DEVICE_NAME_FIELD_LENGTH + 3];
    return true;
}

bool
server_connect_to(struct server *server, char *device_name, struct size *size, const struct scrcpy_options *options) {
    uint32_t attempts = 100;
    uint32_t delay = 100; // ms
    server->video_socket =
        net_connect(server->addr.v4, server->local_port);
    if (server->video_socket == INVALID_SOCKET) {
        return false;
    }

    struct control_msg msg;
    msg.type = CONTROL_MSG_TYPE_INIT_VIDEO;
    msg.video_init.version = strdup(SCRCPY_VERSION);
    msg.video_init.maxSize = options->max_size;
    msg.video_init.maxFps = options->max_fps;
    msg.video_init.bitRate = options->bit_rate;
    msg.video_init.displayId = options->display_id;
    msg.video_init.lockedVideoOrientation = options->lock_video_orientation;
    char *codec_options = options->codec_options;
    if (codec_options == NULL) codec_options = "";
    msg.video_init.codecOptions = strdup(codec_options);
    char *encoder_name = options->encoder_name;
    if (encoder_name == NULL) encoder_name = "";
    msg.video_init.encoderName = strdup(encoder_name);
    if (!send_init_msg(server->video_socket, &msg)) return false;

    // we know that the device is listening, we don't need several attempts
    server->control_socket =
        net_connect(server->addr.v4, server->local_port);
    if (server->control_socket == INVALID_SOCKET) {
        return false;
    }

    struct control_msg msg2;
    msg2.type = CONTROL_MSG_TYPE_INIT_CONTROL;
    msg2.control_init.version = strdup(SCRCPY_VERSION);
    msg2.control_init.displayId = options->display_id;
    msg2.control_init.lockedVideoOrientation = options->lock_video_orientation;
    msg2.control_init.maxSize = options->max_size;
    if (!send_init_msg(server->control_socket, &msg2)) return false;

    // The sockets will be closed on stop if device_read_info() fails
    return device_read_info(server->video_socket, device_name, size);
}

void
server_stop(struct server *server) {
    if (server->server_socket != INVALID_SOCKET
            && !atomic_flag_test_and_set(&server->server_socket_closed)) {
        close_socket(server->server_socket);
    }
    if (server->video_socket != INVALID_SOCKET) {
        close_socket(server->video_socket);
    }
    if (server->control_socket != INVALID_SOCKET) {
        close_socket(server->control_socket);
    }
}

void
server_destroy(struct server *server) {
}
