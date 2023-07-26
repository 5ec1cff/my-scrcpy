package android.app;

import android.graphics.Bitmap;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(ActivityManager.TaskDescription.class)
public class TaskDescriptionHidden {
    public Bitmap getInMemoryIcon() {
        throw new RuntimeException("STUB!");
    }

    public String getIconFilename() {
        throw new RuntimeException("STUB!");
    }
}
