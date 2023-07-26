package android.window;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Interface for a window container to communicate with the window manager. This also acts as a
 * token.
 */
public final class WindowContainerToken implements Parcelable {

    private final IWindowContainerToken mRealToken;

    public WindowContainerToken(IWindowContainerToken realToken) {
        mRealToken = realToken;
    }

    private WindowContainerToken(Parcel in) {
        mRealToken = IWindowContainerToken.Stub.asInterface(in.readStrongBinder());
    }

    public IBinder asBinder() {
        return mRealToken.asBinder();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(mRealToken.asBinder());
    }

    
    public static final Creator<WindowContainerToken> CREATOR =
            new Creator<WindowContainerToken>() {
                @Override
                public WindowContainerToken createFromParcel(Parcel in) {
                    return new WindowContainerToken(in);
                }

                @Override
                public WindowContainerToken[] newArray(int size) {
                    return new WindowContainerToken[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        return mRealToken.asBinder().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WindowContainerToken)) {
            return false;
        }
        return mRealToken.asBinder() == ((WindowContainerToken) obj).asBinder();
    }
}
