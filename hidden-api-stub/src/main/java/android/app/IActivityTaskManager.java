package android.app;

import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.window.IWindowOrganizerController;

public interface IActivityTaskManager {
    int startActivity(IApplicationThread caller, String callingPackage,
                      String callingFeatureId, Intent intent, String resolvedType,
                      IBinder resultTo, String resultWho, int requestCode,
                      int flags, ProfilerInfo profilerInfo, Bundle options);

    int startActivityFromRecents(int taskId, Bundle options);
    ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags, int userId);
    Bitmap getTaskDescriptionIcon(String filename, int userId);
    void moveTaskToFront(IApplicationThread app, String callingPackage, int task,
                         int flags, Bundle options);
    void moveStackToDisplay(int stackId, int displayId);
    int getDisplayId(IBinder activityToken);
    IWindowOrganizerController getWindowOrganizerController();

    abstract class Stub implements IActivityTaskManager {
        public static IActivityTaskManager asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
