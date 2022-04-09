package five.ec1cff.scrcpy.ext;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import five.ec1cff.scrcpy.ext.inputmethod.MyInputMethod;

public class Provider extends ContentProvider {
    public static String METHOD_SEND_BINDER = "sendBinder";
    public static String EXTRA_BINDER = "EXTRA_BINDER";

    @Override
    public int delete(Uri uri, String selection, String[]selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri){
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate(){
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[]projection, String selection,
                        String[]selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public int update(Uri uri,ContentValues values,String selection,
                      String[]selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (method.equals(METHOD_SEND_BINDER)) {
            if (extras != null) {
                IBinder binder = extras.getBinder(EXTRA_BINDER);
                MyInputMethod.onBindScrcpyInputMethod(binder);
                return new Bundle();
            }
        }
        return null;
    }
}
