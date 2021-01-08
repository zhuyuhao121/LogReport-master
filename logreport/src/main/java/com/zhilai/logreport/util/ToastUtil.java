package com.zhilai.logreport.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by yuhao.zhu on 2017/4/25.
 */

public class ToastUtil {

    public static void setToastMsg(Context mContext, String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    public static void setToastMsg(Context mContext, int id) {
        String msg = mContext.getResources().getString(id);
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }
}
