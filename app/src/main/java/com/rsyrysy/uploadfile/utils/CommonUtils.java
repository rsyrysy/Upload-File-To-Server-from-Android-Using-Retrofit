package com.rsyrysy.uploadfile.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.rsyrysy.uploadfile.R;


public class CommonUtils {
    public static final String COMPANYLOGO = "/fileupload/companylogo/";
    public static final String SIGN = "/fileupload/Sign/";
    public static final String PNRDATA = "/fileupload/pnr/";
    private final static String NOMEDIA = ".nomedia";

    public static final String APP_NAME = "fileupload";

    public static void requestPermission(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public static boolean checkPermission(Activity activity, String permissionName) {
        int result = ContextCompat.checkSelfPermission(activity, permissionName);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }


    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void showNetworkIssueMessage(Context context) {
        Toast.makeText(context, R.string.neterror, Toast.LENGTH_LONG).show();
    }

    public static void showerrorMessage(Context context) {
        Toast.makeText(context, R.string.errormessage, Toast.LENGTH_LONG).show();
    }

    public static void showToastMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
