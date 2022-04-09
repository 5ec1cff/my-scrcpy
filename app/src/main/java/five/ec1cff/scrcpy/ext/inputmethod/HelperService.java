package five.ec1cff.scrcpy.ext.inputmethod;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.CursorAnchorInfo;
import android.widget.CheckBox;
import android.widget.TextView;

import five.ec1cff.scrcpy.R;

public class HelperService extends Service {
    private static final String TAG = "helperservice";
    WindowManager wm;
    View root;
    TextView statusTextView;
    boolean isInit;

    static HelperService instance;

    static HelperService get() {
        return instance;
    }

    static class Marker {
        WindowManager wm;
        LayoutParams lp;
        View v;
        boolean show;
        boolean isAllowedShow;
        boolean isRealShow;

        public Marker(Context c, WindowManager wm, int color) {
            v = new View(c);
            v.setBackgroundColor(color);
            this.wm = wm;
            lp = new LayoutParams();
            lp.type = LayoutParams.TYPE_APPLICATION_OVERLAY;
            lp.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | LayoutParams.FLAG_LAYOUT_NO_LIMITS // | LayoutParams.FLAG_LAYOUT_INSET_DECOR
                    | LayoutParams.FLAG_NOT_TOUCHABLE;
            lp.format = PixelFormat.TRANSPARENT;
            lp.width = 15;
            lp.height = 15;
            lp.alpha = (float) 0.5;
            lp.gravity = Gravity.LEFT | Gravity.TOP;
            show = false;
        }

        public void updatePosition(float x, float y) {
            if ((Float.isNaN(x) || Float.isNaN(y))) {
                setShow(false);
                return;
            }
            lp.x = (int) (x - (lp.width + 1) / 2);
            lp.y = (int) (y - (lp.height + 1) / 2);
            setShow(true);
        }

        void updateShow() {
            if (isAllowedShow && show) {
                if (isRealShow) {
                    wm.updateViewLayout(v, lp);
                } else {
                    wm.addView(v, lp);
                }
                isRealShow = true;
            } else {
                if (isRealShow) {
                    wm.removeView(v);
                }
                isRealShow = false;
            }
        }

        public void setAllowedShow(boolean allowed) {
            isAllowedShow = allowed;
            updateShow();
        }

        public void setShow(boolean isShow) {
            show = isShow;
            updateShow();
        }
    }

    Marker topMarker;
    Marker blMarker;
    Marker bottomMarker;

    class Controller implements View.OnTouchListener{
        int mLastX;
        int mLastY;
        LayoutParams lp;
        View v;

        public Controller(View v, LayoutParams lp) {
            this.lp = lp;
            this.v = v;
        }

        void updatePosition(int x, int y) {
            this.lp.x = x;
            this.lp.y = y;
            wm.updateViewLayout(v, this.lp);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Log.d("viewtest", "onTOuch:"+v+" event="+event);
            int mInScreenX = (int) event.getRawX();
            int mInScreenY = (int) event.getRawY();
            // Log.d(TAG, "event " + event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mLastX = (int) event.getRawX();
                    mLastY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    updatePosition(lp.x + mInScreenX - mLastX, lp.y + mInScreenY - mLastY);
                    mLastX = mInScreenX;
                    mLastY = mInScreenY;
                    break;
                case MotionEvent.ACTION_UP:
                    if (event.getEventTime() - event.getDownTime() < 300)
                        v.performClick();
                    break;
            }
            return true;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "helper service created");
        instance = this;
        if (Settings.canDrawOverlays(this)) {
            this.init();
        } else {
            Log.d(TAG, "no float window permission!");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "helper service destroyed");
        super.onDestroy();
        instance = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void init() {
        wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        root = LayoutInflater.from(this).inflate(R.layout.helper_window, null);
        root.setBackgroundColor(Color.WHITE);
        statusTextView = (TextView) root.findViewById(R.id.status);
        ((CheckBox) root.findViewById(R.id.toggle)).setOnClickListener((v) -> {
            CheckBox c = (CheckBox) v;
            setMarkersAllowedShow(c.isChecked());
        });
        LayoutParams lpRoot;
        lpRoot = new LayoutParams();
        lpRoot.x = 10;
        lpRoot.y = 10;
        lpRoot.width = 500;
        lpRoot.height = 400;
        lpRoot.alpha = (float) 0.7;
        lpRoot.gravity = Gravity.LEFT | Gravity.TOP;
        lpRoot.type = LayoutParams.TYPE_APPLICATION_OVERLAY;
        lpRoot.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | LayoutParams.FLAG_LAYOUT_NO_LIMITS | LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        lpRoot.format = PixelFormat.TRANSPARENT;
        wm.addView(root, lpRoot);
        root.setOnTouchListener(new Controller(root, lpRoot));
        topMarker = new Marker(this, wm, Color.RED);
        blMarker = new Marker(this, wm, Color.GREEN);
        bottomMarker = new Marker(this, wm, Color.BLUE);
        updateCursor("init",null);
        isInit = true;
    }

    public void updateCursor(String reason, CursorAnchorInfo info) {
        float horizontal;
        float baseline;
        float top;
        float bottom;
        if (info == null) {
            statusTextView.setText("No cursor(" + reason + ")");
            setMarkersShow(false);
            return;
        }
        horizontal = info.getInsertionMarkerHorizontal();
        float[] pts = new float[]{horizontal, info.getInsertionMarkerTop()};
        info.getMatrix().mapPoints(pts);
        top = pts[1];
        pts = new float[]{horizontal, info.getInsertionMarkerBottom()};
        info.getMatrix().mapPoints(pts);
        bottom = pts[1];
        pts = new float[]{horizontal, info.getInsertionMarkerBaseline()};
        info.getMatrix().mapPoints(pts);
        baseline = pts[1];
        horizontal = pts[0];
        statusTextView.setText(new StringBuilder("horizontal=").append(horizontal)
                .append(" baseline=").append(baseline)
                .append(" top=").append(top)
                .append(" bottom=").append(bottom)
        );
        topMarker.updatePosition(horizontal, top);
        blMarker.updatePosition(horizontal, baseline);
        bottomMarker.updatePosition(horizontal, bottom);
    }

    void setMarkersShow(boolean isShow) {
        if (topMarker != null) {
            topMarker.setShow(isShow);
        }
        if (blMarker != null) {
            blMarker.setShow(isShow);
        }
        if (bottomMarker != null) {
            bottomMarker.setShow(isShow);
        }
    }

    void setMarkersAllowedShow(boolean allow) {
        if (topMarker != null) {
            topMarker.setAllowedShow(allow);
        }
        if (blMarker != null) {
            blMarker.setAllowedShow(allow);
        }
        if (bottomMarker != null) {
            bottomMarker.setAllowedShow(allow);
        }
    }

    public void dismissAll() {
        if (!isInit) return;
        if (root != null) {
            wm.removeView(root);
        }
        setMarkersAllowedShow(false);
        stopSelf();
    }
}
