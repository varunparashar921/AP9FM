package com.versatilemobitech.ap9fm;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by 3164 on 23-11-2015.
 */
public class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    public static boolean isInternetAvailable(Context context) {
        if (context == null) {
            return false;
        }
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo();
        if (info == null) {
            return false;
        } else {
            return info.isConnected();
        }
    }

}
