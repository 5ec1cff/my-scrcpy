package android.app;

import android.annotation.SuppressLint;
import android.window.WindowContainerToken;

import dev.rikka.tools.refine.RefineAs;

@SuppressLint("NewApi")
@RefineAs(android.app.TaskInfo.class)
public class TaskInfoHidden extends TaskInfo {
    public int userId;
    public int displayId;
    public WindowContainerToken token;
}
