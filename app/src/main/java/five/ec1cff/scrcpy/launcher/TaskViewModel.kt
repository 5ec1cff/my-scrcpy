package five.ec1cff.scrcpy.launcher

import android.app.ActivityManager
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModel
import five.ec1cff.scrcpy.util.ActivityTaskManagerWrapper
import kotlin.concurrent.thread

class TaskViewModel : ViewModel() {
    data class RecentTask(
        val label: String,
        val taskId: Int,
        val icon: Drawable?,
        val taskInfo: ActivityManager.RecentTaskInfo
    )

    val recentTasks = mutableListOf<RecentTask>()

    fun loadRecentTasks(context: Context, callback: () -> Unit) {
        thread {
            recentTasks.clear()
            val tasks = ActivityTaskManagerWrapper.getRecentTasks(
                ActivityTaskManagerWrapper.getMaxRecentTasksStatic(),
                ActivityManager.RECENT_IGNORE_UNAVAILABLE,
                0
            )
            val packageManager = context.packageManager
            for (task in tasks) {
                val desc = task.taskDescription
                var label: String? = desc?.label
                var icon: Drawable? = ActivityTaskManagerWrapper.getTaskIcon(task)?.toDrawable(context.resources)
                val activityName = task.baseActivity ?: task.topActivity ?: task.origActivity ?: task.baseIntent.component
                if (activityName != null) {
                    val activityInfo = packageManager.getActivityInfo(activityName, 0)
                    label = activityInfo.loadLabel(packageManager).toString()
                    icon = activityInfo.loadIcon(packageManager)
                }
                if (label == null) label = "<unknown>"
                recentTasks.add(RecentTask(label, task.taskId, icon, task))
            }
            callback()
        }
    }
}