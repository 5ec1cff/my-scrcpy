package com.genymobile.scrcpy.ext

import android.os.IBinder.DeathRecipient
import five.ec1cff.scrcpy.ext.shared.IInputMethod
import five.ec1cff.scrcpy.ext.shared.IInputMethodClient
import kotlin.Throws
import android.os.IBinder
import com.genymobile.scrcpy.Ln
import com.genymobile.scrcpy.wrappers.ServiceManager
import com.genymobile.scrcpy.ext.IMEController
import android.content.IContentProvider
import android.os.Bundle
import android.os.RemoteException
import com.genymobile.scrcpy.wrappers.ContentProvider

object IMEController {
    interface Listener {
        fun onInputStarted()
        fun onInputFinished()
        fun onCursorChanged(x: Float, y: Float)
    }

    var method: IInputMethod? = null
    var isInputStarted = false
    var listener: Listener? = null
    val client: IInputMethodClient = object : IInputMethodClient.Stub() {

        @Throws(RemoteException::class)
        override fun onInputStarted() {
            isInputStarted = true
            Ln.d("onInputStarted")
            if (listener != null) {
                listener!!.onInputStarted()
            }
        }

        @Throws(RemoteException::class)
        override fun onInputFinished() {
            isInputStarted = false
            Ln.d("onInputFinished")
            if (listener != null) {
                listener!!.onInputFinished()
            }
        }

        @Throws(RemoteException::class)
        override fun onCursorChanged(x: Float, y: Float) {
            Ln.d("onCursorChanged:$x,$y")
            if (listener != null) {
                listener!!.onCursorChanged(x, y)
            }
        }

        override fun onInputMethodStatusChanged(avaliable: Boolean) {

        }
    }

    fun commitText(text: String?): Boolean {
        return try {
            method!!.commitText(text)
            true
        } catch (e: RemoteException) {
            Ln.e("failed to commit text", e)
            false
        }
    }

    fun commitComposingText(text: String?): Boolean {
        return try {
            method!!.commitComposingText(text)
            true
        } catch (e: RemoteException) {
            Ln.e("failed to commit text", e)
            false
        }
    }
}