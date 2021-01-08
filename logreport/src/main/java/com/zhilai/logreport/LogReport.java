package com.zhilai.logreport;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.zhilai.logreport.crash.CrashHandler;
import com.zhilai.logreport.encryption.IEncryption;
import com.zhilai.logreport.save.ISave;
import com.zhilai.logreport.save.imp.LogWriter;
import com.zhilai.logreport.upload.ILogUpload;
import com.zhilai.logreport.upload.UploadService;
import com.zhilai.logreport.upload.email.EmailBean;
import com.zhilai.logreport.upload.email.TestEmail;
import com.zhilai.logreport.util.HandlerUtil;
import com.zhilai.logreport.util.LogUtil;
import com.zhilai.logreport.util.NetUtil;
import com.zhilai.logreport.util.RegexUtil;
import com.zhilai.logreport.util.ToastUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 日志崩溃管理框架
 */
public class LogReport implements Handler.Callback {

    private static final String TAG = "LogReport";

    private static LogReport mLogReport;
    /**
     * 设置上传的方式
     */
    private ILogUpload mUpload;
    /**
     * 设置缓存文件夹的大小,默认是30MB
     */
    private long mCacheSize = 30 * 1024 * 1024;

    /**
     * 设置日志保存的路径
     */
    private String mROOT;

    /**
     * 设置加密方式
     */
    private IEncryption mEncryption;

    /**
     * 设置日志的保存方式
     */
    private ISave mLogSaver;

    /**
     * 设置在哪种网络状态下上传，true为只在wifi模式下上传，false是wifi和移动网络都上传
     */
    private boolean mWifiOnly = true;

    /**
     * 是否有异常日志后自动上报，true 有异常自动上报， false 只要触发就上报，不检测是否有异常日志
     */
    public boolean isAutoReport;

    private Context mContext;
    private int uploadType;
    public static final int UPLOAD_TYPE_EMAIL = 1;
    public static final int UPLOAD_TYPE_HTTP = 2;
    private EmailBean emailBean;

    private LogReport() {
    }


    public static LogReport getInstance() {
        if (mLogReport == null) {
            synchronized (LogReport.class) {
                if (mLogReport == null) {
                    mLogReport = new LogReport();
                }
            }
        }
        return mLogReport;
    }

    public LogReport setCacheSize(long cacheSize) {
        this.mCacheSize = cacheSize;
        return this;
    }

    public LogReport setEncryption(IEncryption encryption) {
        this.mEncryption = encryption;
        return this;
    }

    public LogReport setUploadType(ILogUpload logUpload) {
        mUpload = logUpload;
        return this;
    }

    public LogReport setUploadType(int uploadType) {
        this.uploadType = uploadType;
        return this;
    }

    public void setEmailBean(EmailBean emailBean) {
        this.emailBean = emailBean;
    }

    public LogReport setWifiOnly(boolean wifiOnly) {
        mWifiOnly = wifiOnly;
        return this;
    }

    public LogReport setLogDir(Context context, String logDir) {
        if (TextUtils.isEmpty(logDir)) {
            //如果SD不可用，则存储在沙盒中
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                mROOT = context.getExternalCacheDir().getAbsolutePath();
            } else {
                mROOT = context.getCacheDir().getAbsolutePath();
            }
        } else {
            mROOT = logDir;
        }
        return this;
    }

    public LogReport setLogSaver(ISave logSaver) {
        this.mLogSaver = logSaver;
        return this;
    }

    /**
     * 是否有异常日志后自动上报，true 有异常自动上报， false 只要触发就上报，不检测是否有异常日志
     */
    public LogReport isAutoReport(boolean isAutoReport) {
        this.isAutoReport = isAutoReport;
        return this;
    }

    public String getROOT() {
        return mROOT;
    }

    public void init(Context context) {
        mContext = context;
        if (TextUtils.isEmpty(mROOT)) {
            //如果SD不可用，则存储在沙盒中
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                mROOT = context.getExternalCacheDir().getAbsolutePath();
            } else {
                mROOT = context.getCacheDir().getAbsolutePath();
            }
        }
        if (mEncryption != null) {
            mLogSaver.setEncodeType(mEncryption);
        }
        CrashHandler.getInstance().init(mLogSaver);
        LogWriter.getInstance().init(mLogSaver);
    }

    public ILogUpload getUpload() {
        return mUpload;
    }

    public long getCacheSize() {
        return mCacheSize;
    }

    /**
     * 调用此方法，上传日志信息
     *
     * @param applicationContext 全局的application context，避免内存泄露
     */
    public void upload(Context applicationContext) {
        //如果网络可用，而且是移动网络，但是用户设置了只在wifi下上传，返回
        if (NetUtil.isConnected(applicationContext) && !NetUtil.isWifi(applicationContext) && mWifiOnly) {
            return;
        }
        Intent intent = new Intent(applicationContext, UploadService.class);
        applicationContext.startService(intent);
    }

    public void sendFile(File zipFile, ILogUpload.OnUploadFinishedListener onUploadFinishedListener) {
        if (uploadType == UPLOAD_TYPE_EMAIL) {
            LogUtil.d(TAG, "=====Email发送====");
            sendEmail(zipFile, onUploadFinishedListener);
        } else if (uploadType == UPLOAD_TYPE_HTTP) {
            LogUtil.d(TAG, "=====http发送====");
            sendHttp(zipFile, onUploadFinishedListener);
        }
    }

    public void sendEmail(File zipFile, ILogUpload.OnUploadFinishedListener onUploadFinishedListener) {
        try {
            if (emailBean == null || TextUtils.isEmpty(emailBean.getToEmail())) {
                return;
            }
            emailBean.setZipfile(zipFile);
            String[] address = emailBean.getToEmail().split(";");
            ArrayList<Address> stringArrayList = new ArrayList<>();
            for (String s : address) {
                if (RegexUtil.checkEmail(s)) {
                    stringArrayList.add(new InternetAddress(s));
                } else {
                    HandlerUtil.sendMsg(handler, TOAST_STRING_MSG, getResString(R.string.email_format_is_incorrect));
                    return;
                }
            }
//          Address[] addresses = stringArrayList.toArray(new Address[stringArrayList.size()]);
            Address[] addresses = stringArrayList.toArray(new Address[0]);
            TestEmail.sendEmail(handler, emailBean, addresses, onUploadFinishedListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendHttp(File zipFile, final ILogUpload.OnUploadFinishedListener onUploadFinishedListener) {
//        String url = "";
//        HttpGetRequest.getRequest("", url, new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                LogUtil.e(TAG, "====onFailure====" + e.getMessage());
//                onUploadFinishedListener.onError("Send fail！ " + e.getMessage());
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) {
//                try {
//                    String jsonstr = response.body().string();
//                    LogUtil.d(TAG, "返回报文：" + jsonstr);
//                    Gson gson = new Gson();
//
//                    org.json.JSONObject jsonObject = new org.json.JSONObject(jsonstr);
//                    org.json.JSONObject ZMSGObject = jsonObject.getJSONObject("ZMSG");
//                    String reustCode = ZMSGObject.getJSONObject("ZHEAD").getString("RetCode");
//                    String reustMsg = ZMSGObject.getJSONObject("ZHEAD").getString("RetMsg");
//                    if ("0000".equals(reustCode)) {
//                        onUploadFinishedListener.onSuceess();
//                    } else {
//
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    LogUtil.e(TAG, "====Exception====" + e.getMessage());
//                }
//            }
//        });
        
        uploadFile(zipFile, onUploadFinishedListener);
    }

    // 使用OkHttp上传文件
    private void uploadFile(File zipFile, final ILogUpload.OnUploadFinishedListener onUploadFinishedListener) {
        OkHttpClient client = new OkHttpClient();
        MediaType contentType = MediaType.parse("text/plain"); // 上传文件的Content-Type

//        FormBody.Builder builder = new FormBody.Builder();
//        builder.add("method", "");
//        FormBody body = builder.build();

        RequestBody body = RequestBody.create(contentType, zipFile); // 上传文件的请求体
        Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw") // 上传地址
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 文件上传成功
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse成功: " + response.body().string());
                    onUploadFinishedListener.onSuceess();
                } else {
                    Log.d(TAG, "onResponse失败: " + response.message());
                    onUploadFinishedListener.onError(response.message());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                onUploadFinishedListener.onError(e.getMessage());
                // 文件上传失败
                Log.d(TAG, "onFailure: " + e.getMessage());
            }
        });
    }

    private String getResString(int resId) {
        return mContext.getResources().getString(resId);
    }

    private Handler handler = new Handler(this);

    public static final int EMAIL_SEND_SUCCESS = 0;
    private static final int TOAST_INT_MSG = 1;
    private static final int TOAST_STRING_MSG = 2;

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case EMAIL_SEND_SUCCESS:
                ToastUtil.setToastMsg(mContext
                        , getResString(R.string.email_sent_successfully));
                break;
            case TOAST_INT_MSG:
                int msg = (int) message.obj;
                ToastUtil.setToastMsg(mContext, getResString(msg));
                break;
            case TOAST_STRING_MSG:
                String content = (String) message.obj;
                ToastUtil.setToastMsg(mContext, content);
                break;
        }
        return false;
    }
}
