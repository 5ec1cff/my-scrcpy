package five.ec1cff.scrcpy.util

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.IBinder
import android.os.ServiceManager
import dev.rikka.tools.refine.Refine
import five.ec1cff.scrcpy.App
import five.ec1cff.scrcpy.Global
import java.lang.IllegalArgumentException

@SuppressLint("DiscouragedPrivateApi")
object ActivityTaskManagerWrapper {
    private val mInterface by lazy {
        var binder = ServiceManager.getService("activity_task")
        if (Global.isApp) {
            binder = App.scrcpyShizukuService!!.getBinderWrapper(binder)
        }
        IActivityTaskManager.Stub.asInterface(binder)
    }

    fun startActivity(intent: Intent, options: ActivityOptions): Int {
        return mInterface.startActivity(
            null,
            "android", // "com.android.shell" if uid == 0 or 2000
            null,
            intent,
            null, null, null, 0, 0, null,
            options.toBundle()
        )
    }

    fun startActivityFromRecents(taskId: Int, options: ActivityOptions): Int {
        return mInterface.startActivityFromRecents(taskId, options.toBundle())
    }

    fun getRecentTasks(numTasks: Int, flags: Int, userId: Int): List<ActivityManager.RecentTaskInfo> {
        return mInterface.getRecentTasks(numTasks, flags, userId).list
    }

    fun getTaskIcon(info: TaskInfo): Bitmap? {
        val desc = info.taskDescription ?: return null
        val descHidden = Refine.unsafeCast<TaskDescriptionHidden>(desc)
        descHidden.inMemoryIcon?.let { return it }
        val infoHidden = Refine.unsafeCast<TaskInfoHidden>(info)
        if (descHidden.iconFilename == null) return null
        return mInterface.getTaskDescriptionIcon(descHidden.iconFilename, infoHidden.userId)
    }

    fun moveTaskToFront(taskId: Int, flags: Int, options: ActivityOptions) {
        mInterface.moveTaskToFront(null, "android",
            taskId, flags, options.toBundle()
        )
    }

    fun moveStackToDisplay(stackId: Int, displayId: Int) {
        mInterface.moveStackToDisplay(stackId, displayId)
    }

    fun startTaskOnDisplay(taskId: Int, displayId: Int) {
        val options = ActivityOptions.makeBasic()
        // options.launchDisplayId = displayId
        try {
            moveStackToDisplay(taskId, displayId)
        } catch (e: IllegalArgumentException) {
            // ignore the exception when the task was in our target display
        }
        moveTaskToFront(taskId, 0, options)
    }

    fun getWindowOrganizerController() = mInterface.windowOrganizerController!!

    private val tokenField by lazy {
        Activity::class.java.getDeclaredField("mToken").also { it.isAccessible = true }
    }

    fun getDisplayIdForActivity(a: Activity ): Int {
        return mInterface.getDisplayId(tokenField.get(a) as IBinder)
    }

    fun getMaxRecentTasksStatic() = ActivityTaskManager.getMaxRecentTasksStatic()
}