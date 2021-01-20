package com.zhilai.logreportdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhilai.logreport.LogReport;
import com.zhilai.logreport.save.imp.CrashWriter;
import com.zhilai.logreport.save.imp.LogWriter;
import com.zhilai.logreport.upload.email.EmailBean;
import com.zhilai.logreport.util.FileUtil;

import java.io.File;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions();
        setUpListener();
    }

    private void setUpListener() {
        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        Button button3 = findViewById(R.id.button3);
        Button button4 = findViewById(R.id.button4);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = null;
                s.length();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogWriter.writeLog(TAG, "打Log测试！！！！");
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileUtil.deleteDir(new File(LogReport.getInstance().getROOT()));
            }
        });

        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogReport.getInstance().upload(MainActivity.this);
            }
        });
    }

    @SuppressLint("CheckResult")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void verifyStoragePermissions() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) {
                        if (granted) {
                            Log.d(TAG, "已申请到权限");
                            // All requested permissions are granted
                            initCrashReport();
                        } else {
                            Log.d(TAG, "权限已被拒绝");
                            // At least one permission is denied
                        }
                    }
                });
    }

    private void initCrashReport() {
        String path = "sdcard/"
                + this.getString(this.getApplicationInfo()
                .labelRes) + "/";
        LogReport.getInstance()
                .setCacheSize(30 * 1024 * 1024)//支持设置缓存大小，超出后清空
                .setLogDir(getApplicationContext(), path)//定义路径为：sdcard/[app name]/
                .setWifiOnly(false)//设置只在Wifi状态下上传，设置为false为Wifi和移动网络都上传
                .setLogSaver(new CrashWriter(getApplicationContext()))//支持自定义保存崩溃信息的样式
                //.setEncryption(new AESEncode()) //支持日志到AES加密或者DES加密，默认不开启
                //是否有异常日志后自动上报，true 有异常自动上报， false 只要触发就上报，不检测是否有异常日志
                .isAutoReport(false)
                .checkCacheSize(path + "log/")//检查指定路径下的所有文件大小，超过上面设置的缓存大小后，会递归删除文件，直到剩下的文件大小小于缓存大小后停止查询
                .init(MainActivity.this);

        initEmailReporter();
//        initHttpReporter();

//        LogReport.getInstance().upload(MainActivity.this);
    }

    /**
     * 使用EMAIL发送日志
     */
    private void initEmailReporter() {
        EmailBean eBean = new EmailBean();
        eBean.setContent("邮件内容可以自定义");
        eBean.setSubject("邮件主题可以自定义");
        eBean.setFileName("文件名称可以自定义" + ".zip");
        //TODO 记得修改发件人邮箱和密码，否则邮件发不出去
        eBean.setEmailAccount("xxx.com");//发送人邮箱
        eBean.setEmailPwd("123456");//发送人邮箱密码
        eBean.setMailHost("smtp.qiye.163.com");//发件服务器地址（需要根据邮箱类型进行修改）
        eBean.setToEmail("534837240@qq.com;");//收送人邮箱，支持批量添加邮箱，邮箱与邮箱之间用英文";"隔开
        LogReport.getInstance().setUploadType(LogReport.UPLOAD_TYPE_EMAIL);
        LogReport.getInstance().setEmailBean(eBean);
    }

    /**
     * 使用HTTP发送日志
     */
    private void initHttpReporter() {
//        HttpReporter http = new HttpReporter(this);
//        http.setUrl("http://crashreport.jd-app.com/your_receiver");//发送请求的地址
//        http.setFileParam("fileName");//文件的参数名
//        http.setToParam("to");//收件人参数名
//        http.setTo("534837240@qq.com");//收件人
//        http.setTitleParam("subject");//标题
//        http.setBodyParam("message");//内容
//        LogReport.getInstance().setUploadType(http);
        LogReport.getInstance().setUploadType(LogReport.UPLOAD_TYPE_HTTP);
    }
}
