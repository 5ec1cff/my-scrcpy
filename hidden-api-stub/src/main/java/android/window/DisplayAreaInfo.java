package android.window;

import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Stores information about a particular com.android.server.wm.DisplayArea. This object will
 * be sent to registered DisplayAreaOrganizer to provide information when the DisplayArea
 * is added, removed, or changed.
 *
 */
public final class DisplayAreaInfo implements Parcelable {
    public final WindowContainerToken token;

    public final Configuration configuration = new Configuration();

    /**
     * The id of the display this display area is associated with.
     */
    public final int displayId;

    public final int featureId;


    public DisplayAreaInfo(WindowContainerToken token, int displayId, int featureId) {
        this.token = token;
        this.displayId = displayId;
        this.featureId = featureId;
    }

    private DisplayAreaInfo(Parcel in) {
        token = WindowContainerToken.CREATOR.createFromParcel(in);
        configuration.readFromParcel(in);
        displayId = in.readInt();
        featureId = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        token.writeToParcel(dest, flags);
        configuration.writeToParcel(dest, flags);
        dest.writeInt(displayId);
        dest.writeInt(featureId);
    }

    public static final Creator<DisplayAreaInfo> CREATOR = new Creator<DisplayAreaInfo>() {
        @Override
        public DisplayAreaInfo createFromParcel(Parcel in) {
            return new DisplayAreaInfo(in);
        }

        @Override
        public DisplayAreaInfo[] newArray(int size) {
            return new DisplayAreaInfo[size];
        }
    };

    @Override
    public String toString() {
        return "DisplayAreaInfo{token=" + token
                + " config=" + configuration + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
