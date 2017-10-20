package com.example.administrator.bluetootchdemo.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 *
 * Created by Administrator on 2017/8/21.
 */

public class AppUtils {

    public static  final  boolean DEBUG = true;
    public static final String TAG = "TAG";
    private static AppUtils sLogUtil;

    public static AppUtils getInstance() {
        if (sLogUtil == null) {
            synchronized (AppUtils.class) {
                if (sLogUtil == null) {
                    sLogUtil = new AppUtils();
                }
            }
        }
        return sLogUtil;
    }

    public void debug(String msg){
        if(DEBUG){
            Log.d(TAG,msg);
        }
    }

    public void info(String msg){
        if(DEBUG){
            Log.i(TAG,msg);
        }
    }

    public void error(String msg){
        if(DEBUG){
            Log.e(TAG,msg);
        }
    }

    public void warn(String msg){
        if(DEBUG){
            Log.w(TAG,msg);
        }
    }

    public void ToastString(Context context ,String msg){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
