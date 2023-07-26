package five.ec1cff.scrcpy.multitask

import android.app.ActivityManager
import five.ec1cff.scrcpy.IScrcpyTaskController
import five.ec1cff.scrcpy.util.ActivityTaskManagerWrapper

const val useOrganizer = true

object TaskController : IScrcpyTaskController.Stub() {
    override fun startRecentTaskOnDisplay(task: ActivityManager.RecentTaskInfo, displayId: Int) {
        if (useOrganizer) {
            TaskOrganizer.startTask(task, displayId)
        } else {
            ActivityTaskManagerWrapper.startTaskOnDisplay(task.taskId, displayId)
        }
    }

    override fun startRunningTaskOnDisplay(task: ActivityManager.RunningTaskInfo, displayId: Int) {
        TaskOrganizer.startTask(task, displayId)
    }

    override fun getRecentTasks(): List<ActivityManager.RecentTaskInfo> {
        return ActivityTaskManagerWrapper.getRecentTasks(
            ActivityTaskManagerWrapper.getMaxRecentTasksStatic(),
            ActivityManager.RECENT_IGNORE_UNAVAILABLE,
            0
        )
    }

}