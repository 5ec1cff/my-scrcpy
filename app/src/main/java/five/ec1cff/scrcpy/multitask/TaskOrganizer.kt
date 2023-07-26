package five.ec1cff.scrcpy.multitask

import android.app.ActivityOptions
import android.app.TaskInfo
import android.app.TaskInfoHidden
import android.app.WindowConfiguration
import android.util.Log
import android.view.SurfaceControl
import android.window.*
import dev.rikka.tools.refine.Refine
import five.ec1cff.scrcpy.launcher.LauncherActivity
import five.ec1cff.scrcpy.util.ActivityTaskManagerWrapper

object TaskOrganizer {
    private const val TAG = "TaskOrganizer"
    private lateinit var windowController: IWindowOrganizerController
    private lateinit var displayController: IDisplayAreaOrganizerController
    private lateinit var taskController: ITaskOrganizerController

    private val displayTokens = mutableMapOf<Int, WindowContainerToken>()
    private val displayRootTasks = mutableMapOf<Int, WindowContainerToken>()

    private val displayOrganizer = object : IDisplayAreaOrganizer.Stub() {
        override fun onDisplayAreaAppeared(
            displayAreaInfo: DisplayAreaInfo?,
            leash: SurfaceControl?
        ) {
            displayAreaInfo ?: return
            displayTokens[displayAreaInfo.displayId] = displayAreaInfo.token
            Log.d(TAG, "display appeared: ${displayAreaInfo.displayId} ${displayAreaInfo.token.asBinder()}")
        }

        override fun onDisplayAreaVanished(displayAreaInfo: DisplayAreaInfo?) {
            displayAreaInfo ?: return
            displayTokens.remove(displayAreaInfo.displayId)
            Log.d(TAG, "display vanished: ${displayAreaInfo.displayId}")
        }

        override fun onDisplayAreaInfoChanged(displayAreaInfo: DisplayAreaInfo?) {
            displayAreaInfo ?: return
            displayTokens[displayAreaInfo.displayId] = displayAreaInfo.token
            Log.d(TAG, "display changed: ${displayAreaInfo.displayId}")
        }

    }

    fun init() {
        try {
            windowController = ActivityTaskManagerWrapper.getWindowOrganizerController()
            displayController = windowController.displayAreaOrganizerController
            taskController = windowController.taskOrganizerController
            displayController.registerOrganizer(
                displayOrganizer,
                DisplayAreaOrganizer.FEATURE_DEFAULT_TASK_CONTAINER
            )
            getRootTaskForDisplay(0)
        } catch (t: Throwable) {
            Log.e(TAG, "failed to initiate TaskOrganizer", t)
        }
    }

    fun getRootTaskForDisplay(displayId: Int): WindowContainerToken {
        synchronized(displayRootTasks) {
            var rootTaskToken = displayRootTasks[displayId]
            if (rootTaskToken == null) {
                val taskInfo = taskController.createRootTask(displayId, WindowConfiguration.WINDOWING_MODE_UNDEFINED) as TaskInfoHidden
                rootTaskToken = taskInfo.token!!
                taskController.setLaunchRoot(displayId, rootTaskToken)
                displayRootTasks[displayId] = rootTaskToken
            }
            return rootTaskToken
        }
    }

    fun startTask(task: TaskInfo, displayId: Int) {
        val realTask = task as TaskInfoHidden
        if (realTask.isRunning) {
            try {
                val targetDisplay = getRootTaskForDisplay(displayId)
                val wct = WindowContainerTransaction()
                wct.reparent(realTask.token, targetDisplay, true)
                windowController.applyTransaction(wct)
                ActivityTaskManagerWrapper.moveTaskToFront(task.taskId, 0, ActivityOptions.makeBasic())
            } catch (t: Throwable) {
                Log.e(TAG, "failed to move existing task $task to display $displayId", t)
            }
        } else {
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = displayId
            ActivityTaskManagerWrapper.startActivity(realTask.baseIntent, options).also {
                Log.d(TAG, "startActivity $task on $displayId return $it ")
            }
        }
    }

    fun moveTasksToDisplay(displayFrom: Int, displayTo: Int) {
        val rootTask = displayRootTasks[displayFrom] ?: return
        val primaryRootTask = displayRootTasks[0] ?: return
        val childTasks = taskController.getChildTasks(rootTask, intArrayOf(WindowConfiguration.ACTIVITY_TYPE_STANDARD))
        val targetDisplayToken = displayTokens[displayTo]
        if (targetDisplayToken == null) {
            Log.w(TAG, "no token for target display $displayTo found")
            return
        }
        val wct = WindowContainerTransaction()
        childTasks.forEach { task ->
            wct.reparent((task as TaskInfoHidden).token, primaryRootTask, false)
        }
        Log.d(TAG, "Move ${childTasks.size} tasks from display $displayFrom to $displayTo")
        try {
            windowController.applyTransaction(wct)
        } catch (t: Throwable) {
            Log.e(TAG, "failed to move tasks from display $displayFrom to $displayTo", t)
        }
    }
}
