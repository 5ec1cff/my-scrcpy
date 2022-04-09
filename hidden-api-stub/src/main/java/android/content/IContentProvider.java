package android.content;

import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public interface IContentProvider {
    Bundle call(String callingPkg, String method, @Nullable String arg, @Nullable Bundle extras)
            throws RemoteException;

    @RequiresApi(29)
    Bundle call(String callingPkg, String authority, String method, @Nullable String arg, @Nullable Bundle extras)
            throws RemoteException;

    @RequiresApi(30)
    Bundle call(String callingPkg, String featureId, String authority, String method, @Nullable String arg, @Nullable Bundle extras)
            throws RemoteException;
}