// IScrcpyTaskController.aidl
package five.ec1cff.scrcpy;

import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningTaskInfo;

interface IScrcpyTaskController {
    oneway void startRecentTaskOnDisplay(in RecentTaskInfo task, int displayId);
    oneway void startRunningTaskOnDisplay(in RunningTaskInfo task, int displayId);
    List<RecentTaskInfo> getRecentTasks();
}