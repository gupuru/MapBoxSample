package gupuru.mapboxsample;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.constants.MapboxConstants;

public final class ApiAccess {

    public static String getToken(@NonNull Context context) {
        try {
            // read out AndroidManifest
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String token = appInfo.metaData.getString(MapboxConstants.KEY_META_DATA_MANIFEST);
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException();
            }
            return token;
        } catch (Exception e) {
            // use fallback on string resource, used for development
            int tokenResId = context.getResources().getIdentifier("mapbox_access_token", "string", context.getPackageName());
            return tokenResId != 0 ? context.getString(tokenResId) : null;
        }
    }

}