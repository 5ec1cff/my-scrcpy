package com.genymobile.scrcpy;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import five.ec1cff.scrcpy.FdHelperKt;

public final class DesktopConnection implements Closeable {

    private static final int DEVICE_NAME_FIELD_LENGTH = 64;

    private final Socket videoSocket;
    private final FileDescriptor videoFd;

    private final Socket controlSocket;

    private final ControlMessageReader reader = new ControlMessageReader();
    private final DeviceMessageWriter writer = new DeviceMessageWriter();

    private DesktopConnection(Socket videoSocket, Socket controlSocket) throws IOException {
        this.videoSocket = videoSocket;
        this.controlSocket = controlSocket;
        videoFd = FdHelperKt.getSocketFileDescriptor(videoSocket);
    }

    private static LocalSocket connect(String abstractName) throws IOException {
        LocalSocket localSocket = new LocalSocket();
        localSocket.connect(new LocalSocketAddress(abstractName));
        return localSocket;
    }

    public static DesktopConnection open(ScreenDevice device, ServerSocket serverSocket) throws IOException {
        Socket videoSocket;
        Socket controlSocket;
        videoSocket = serverSocket.accept();
        // send one byte so the client may read() to detect a connection error
        //videoSocket.getOutputStream().write(0);
        try {
            controlSocket = serverSocket.accept();
        } catch (IOException | RuntimeException e) {
            videoSocket.close();
            throw e;
        }
        DesktopConnection connection = new DesktopConnection(videoSocket, controlSocket);
        Size videoSize = device.getScreenInfo().getVideoSize();
        connection.send(ScreenDevice.getDeviceName(), videoSize.getWidth(), videoSize.getHeight());
        return connection;
    }

    public void close() throws IOException {
        videoSocket.shutdownInput();
        videoSocket.shutdownOutput();
        videoSocket.close();
        controlSocket.shutdownInput();
        controlSocket.shutdownOutput();
        controlSocket.close();
    }

    private void send(String deviceName, int width, int height) throws IOException {
        byte[] buffer = new byte[DEVICE_NAME_FIELD_LENGTH + 4];

        byte[] deviceNameBytes = deviceName.getBytes(StandardCharsets.UTF_8);
        int len = StringUtils.getUtf8TruncationIndex(deviceNameBytes, DEVICE_NAME_FIELD_LENGTH - 1);
        System.arraycopy(deviceNameBytes, 0, buffer, 0, len);
        // byte[] are always 0-initialized in java, no need to set '\0' explicitly

        buffer[DEVICE_NAME_FIELD_LENGTH] = (byte) (width >> 8);
        buffer[DEVICE_NAME_FIELD_LENGTH + 1] = (byte) width;
        buffer[DEVICE_NAME_FIELD_LENGTH + 2] = (byte) (height >> 8);
        buffer[DEVICE_NAME_FIELD_LENGTH + 3] = (byte) height;
        IO.writeFully(videoFd, buffer, 0, buffer.length);
    }

    public FileDescriptor getVideoFd() {
        return videoFd;
    }
}
