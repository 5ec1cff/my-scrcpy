package five.ec1cff.scrcpy

import android.annotation.SuppressLint
import android.os.ParcelFileDescriptor
import java.io.FileDescriptor
import java.lang.reflect.AccessibleObject
import java.net.Socket
import java.net.SocketImpl

fun <T: AccessibleObject> T.access(): T {
    this.isAccessible = true
    return this
}

@SuppressLint("DiscouragedPrivateApi", "SoonBlockedPrivateApi")
fun createSocketFromFd(pfd: ParcelFileDescriptor): Socket {
    val dfd = pfd.detachFd()
    val fd = FileDescriptor::class.java.getDeclaredConstructor(Integer.TYPE).access().newInstance(dfd)
    val socketImpl = Class.forName("java.net.SocksSocketImpl").getDeclaredConstructor().access().newInstance() as SocketImpl
    SocketImpl::class.java.getDeclaredField("fd").access().set(socketImpl, fd)
    Socket::class.java.apply {
        val socket = getDeclaredConstructor(SocketImpl::class.java).access().newInstance(socketImpl)
        getDeclaredMethod("setCreated").access()
            .invoke(socket)
        getDeclaredMethod("setConnected").access()
            .invoke(socket)
        return socket
    }
}

@SuppressLint("DiscouragedPrivateApi")
fun getFd(fd: FileDescriptor) = FileDescriptor::class.java.getDeclaredField("descriptor").access().get(fd) as Int

@SuppressLint("DiscouragedPrivateApi")
fun getSocketFileDescriptor(socket: Socket): FileDescriptor {
    val impl = Socket::class.java.getDeclaredField("impl").access().get(socket) as SocketImpl
    return SocketImpl::class.java.getDeclaredField("fd").access().get(impl) as FileDescriptor
}

fun getSocketFd(socket: Socket): Int {
    return getFd(getSocketFileDescriptor(socket))
}
