package android.window;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.SurfaceControl;

import java.util.List;
import java.util.Map;

public final class WindowContainerTransaction implements Parcelable {

    public WindowContainerTransaction() {}

    /**
     * Resize a container.
     */
    public WindowContainerTransaction setBounds(
            WindowContainerToken container, Rect bounds) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Resize a container's app bounds. This is the bounds used to report appWidth/Height to an
     * app's DisplayInfo. It is derived by subtracting the overlapping portion of the navbar from
     * the full bounds.
     */
    
    public WindowContainerTransaction setAppBounds(
            WindowContainerToken container,Rect appBounds) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Resize a container's configuration size. The configuration size is what gets reported to the
     * app via screenWidth/HeightDp and influences which resources get loaded. This size is
     * derived by subtracting the overlapping portions of both the statusbar and the navbar from
     * the full bounds.
     */
    public WindowContainerTransaction setScreenSizeDp(
            WindowContainerToken container, int w, int h) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Notify activities within the hierarchy of a container that they have entered picture-in-picture
     * mode with the given bounds.
     */
    
    public WindowContainerTransaction scheduleFinishEnterPip(
            WindowContainerToken container,Rect bounds) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Send a SurfaceControl transaction to the server, which the server will apply in sync with
     * the next bounds change. As this uses deferred transaction and not BLAST it is only
     * able to sync with a single window, and the first visible window in this hierarchy of type
     * BASE_APPLICATION to resize will be used. If there are bound changes included in this
     * WindowContainer transaction (from setBounds or scheduleFinishEnterPip), the SurfaceControl
     * transaction will be synced with those bounds. If there are no changes, then
     * the SurfaceControl transaction will be synced with the next bounds change. This means
     * that you can call this, apply the WindowContainer transaction, and then later call
     * dismissPip() to achieve synchronization.
     */
    public WindowContainerTransaction setBoundsChangeTransaction(
            WindowContainerToken container, SurfaceControl.Transaction t) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Like {@link #setBoundsChangeTransaction} but instead queues up a setPosition/WindowCrop
     * on a container's surface control. This is useful when a boundsChangeTransaction needs to be
     * queued up on a Task that won't be organized until the end of this window-container
     * transaction.
     *
     * This requires that, at the end of this transaction, `task` will be organized; otherwise
     * the server will throw an IllegalArgumentException.
     *
     * WARNING: Use this carefully. Whatever is set here should match the expected bounds after
     *          the transaction completes since it will likely be replaced by it. This call is
     *          intended to pre-emptively set bounds on a surface in sync with a buffer when
     *          otherwise the new bounds and the new buffer would update on different frames.
     */
    public WindowContainerTransaction setBoundsChangeTransaction(
            WindowContainerToken task, Rect surfaceBounds) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Set the windowing mode of children of a given root task, without changing
     * the windowing mode of the Task itself. This can be used during transitions
     * for example to make the activity render it's fullscreen configuration
     * while the Task is still in PIP, so you can complete the animation.
     */
    public WindowContainerTransaction setActivityWindowingMode(
            WindowContainerToken container, int windowingMode) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Sets the windowing mode of the given container.
     */
    public WindowContainerTransaction setWindowingMode(
            WindowContainerToken container, int windowingMode) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Sets whether a container or any of its children can be focusable. When {@code false}, no
     * child can be focused; however, when {@code true}, it is still possible for children to be
     * non-focusable due to WM policy.
     */
    public WindowContainerTransaction setFocusable(
            WindowContainerToken container, boolean focusable) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Sets whether a container or its children should be hidden. When {@code false}, the existing
     * visibility of the container applies, but when {@code true} the container will be forced
     * to be hidden.
     */
    public WindowContainerTransaction setHidden(
            WindowContainerToken container, boolean hidden) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Set the smallestScreenWidth of a container.
     */
    public WindowContainerTransaction setSmallestScreenWidthDp(
            WindowContainerToken container, int widthDp) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Reparents a container into another one. The effect of a {@code null} parent can vary. For
     * example, reparenting a stack to {@code null} will reparent it to its display.
     *
     * @param onTop When {@code true}, the child goes to the top of parent; otherwise it goes to
     *              the bottom.
     */
    public WindowContainerTransaction reparent(WindowContainerToken child,
                                               WindowContainerToken parent, boolean onTop) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Reorders a container within its parent.
     *
     * @param onTop When {@code true}, the child goes to the top of parent; otherwise it goes to
     *              the bottom.
     */
    public WindowContainerTransaction reorder(WindowContainerToken child, boolean onTop) {
        throw new RuntimeException("STUB!");
    }

    /**
     * Merges another WCT into this one.
     * @param transfer When true, this will transfer everything from other potentially leaving
     *                 other in an unusable state. When false, other is left alone, but
     *                 SurfaceFlinger Transactions will not be merged.
     */
    public void merge(WindowContainerTransaction other, boolean transfer) {
        throw new RuntimeException("STUB!");
    }

    public Map<IBinder, Change> getChanges() {
        throw new RuntimeException("STUB!");
    }

    public List<HierarchyOp> getHierarchyOps() {
        throw new RuntimeException("STUB!");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new RuntimeException("STUB!");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    
    public static final Creator<WindowContainerTransaction> CREATOR =
            new Creator<WindowContainerTransaction>() {
                @Override
                public WindowContainerTransaction createFromParcel(Parcel in) {
                    throw new RuntimeException("STUB!");
                }

                @Override
                public WindowContainerTransaction[] newArray(int size) {
                    throw new RuntimeException("STUB!");
                }
            };

    /**
     * Holds changes on a single WindowContainer including Configuration changes.
     */
    public static class Change implements Parcelable {
        public static final int CHANGE_FOCUSABLE = 1;
        public static final int CHANGE_BOUNDS_TRANSACTION = 1 << 1;
        public static final int CHANGE_PIP_CALLBACK = 1 << 2;
        public static final int CHANGE_HIDDEN = 1 << 3;
        public static final int CHANGE_BOUNDS_TRANSACTION_RECT = 1 << 4;

        public Change() {}

        protected Change(Parcel in) {
            throw new RuntimeException("STUB!");
        }

        /**
         * @param transfer When true, this will transfer other into this leaving other in an
         *                 undefined state. Use this if you don't intend to use other. When false,
         *                 SurfaceFlinger Transactions will not merge.
         */
        public void merge(Change other, boolean transfer) {
            throw new RuntimeException("STUB!");
        }

        public int getWindowingMode() {
            throw new RuntimeException("STUB!");
        }

        public int getActivityWindowingMode() {
            throw new RuntimeException("STUB!");
        }

        public Configuration getConfiguration() {
            throw new RuntimeException("STUB!");
        }

        /** Gets the requested focusable state */
        public boolean getFocusable() {
            throw new RuntimeException("STUB!");
        }

        /** Gets the requested hidden state */
        public boolean getHidden() {
            throw new RuntimeException("STUB!");
        }

        public int getChangeMask() {
            throw new RuntimeException("STUB!");
        }

        public int getConfigSetMask() {
            throw new RuntimeException("STUB!");
        }

        public int getWindowSetMask() {
            throw new RuntimeException("STUB!");
        }

        /**
         * Returns the bounds to be used for scheduling the enter pip callback
         * or null if no callback is to be scheduled.
         */
        public Rect getEnterPipBounds() {
            throw new RuntimeException("STUB!");
        }

        public SurfaceControl.Transaction getBoundsChangeTransaction() {
            throw new RuntimeException("STUB!");
        }

        public Rect getBoundsChangeSurfaceBounds() {
            throw new RuntimeException("STUB!");
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            throw new RuntimeException("STUB!");
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Change> CREATOR = new Creator<Change>() {
            @Override
            public Change createFromParcel(Parcel in) {
                return new Change(in);
            }

            @Override
            public Change[] newArray(int size) {
                return new Change[size];
            }
        };
    }

    /**
     * Holds information about a reparent/reorder operation in the hierarchy. This is separate from
     * Changes because they must be executed in the same order that they are added.
     * @hide
     */
    public static class HierarchyOp implements Parcelable {
        public HierarchyOp(IBinder container, IBinder reparent, boolean toTop) {
            throw new RuntimeException("STUB!");
        }

        public HierarchyOp(IBinder container, boolean toTop) {
            throw new RuntimeException("STUB!");
        }

        public HierarchyOp(HierarchyOp copy) {
            throw new RuntimeException("STUB!");
        }

        protected HierarchyOp(Parcel in) {
            throw new RuntimeException("STUB!");
        }

        public boolean isReparent() {
            throw new RuntimeException("STUB!");
        }

        public IBinder getNewParent() {
            throw new RuntimeException("STUB!");
        }

        
        public IBinder getContainer() {
            throw new RuntimeException("STUB!");
        }

        public boolean getToTop() {
            throw new RuntimeException("STUB!");
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            throw new RuntimeException("STUB!");
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<HierarchyOp> CREATOR = new Creator<HierarchyOp>() {
            @Override
            public HierarchyOp createFromParcel(Parcel in) {
                return new HierarchyOp(in);
            }

            @Override
            public HierarchyOp[] newArray(int size) {
                return new HierarchyOp[size];
            }
        };
    }
}
