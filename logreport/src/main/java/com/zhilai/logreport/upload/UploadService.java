package com.zhilai.logreport.upload;

import android.app.IntentService;
import android.content.Intent;

import com.zhilai.logreport.LogReport;
import com.zhilai.logreport.save.imp.LogWriter;
import com.zhilai.logreport.util.CompressUtil;
import com.zhilai.logreport.util.FileUtil;
import com.zhilai.logreport.util.LogUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * 此Service用于后台发送日志
 */
public class UploadService extends IntentService {

    public static final String TAG = "UploadService";

    /**
     * 压缩包名称的一部分：时间戳
     */
    public final static SimpleDateFormat ZIP_FOLDER_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS", Locale.getDefault());

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public UploadService() {
        super(TAG);
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * 同一时间只会有一个耗时任务被执行，其他的请求还要在后面排队，
     * onHandleIntent()方法不会多线程并发执行，所有无需考虑同步问题
     *
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        final File logfolder = new File(LogReport.getInstance().getROOT() + "Log/");
        // 如果Log文件夹都不存在，说明不存在崩溃日志，检查缓存是否超出大小后退出
        if (!logfolder.exists() || logfolder.listFiles().length == 0) {
            LogUtil.d("Log文件夹都不存在，无需上传");
            LogWriter.writeLog(TAG, "Log文件夹都不存在，无需上传");
            return;
        }
        //只存在log文件，但是不存在崩溃日志，也不会上传
        ArrayList<File> crashFileList = FileUtil.getCrashList(logfolder);
        if (crashFileList.size() == 0) {
            LogUtil.d(TAG, "只存在log文件，但是不存在崩溃日志，所以不上传");
            LogWriter.writeLog(TAG, "只存在log文件，但是不存在崩溃日志，所以不上传");
            return;
        }
        final File zipfolder = new File(LogReport.getInstance().getROOT() + "AlreadyUploadLog/");
        String fileName = "UploadOn" + ZIP_FOLDER_TIME_FORMAT.format(System.currentTimeMillis()) + ".zip";
        File zipfile = new File(zipfolder, fileName);
        final File rootdir = new File(LogReport.getInstance().getROOT());
        final String path = LogReport.getInstance().getROOT();

        //创建文件，如果父路径缺少，创建父路径
        FileUtil.createFile(zipfolder, zipfile);

        LogUtil.d(TAG, "logfolder.getAbsolutePath()===" + logfolder.getAbsolutePath());
        LogUtil.d(TAG, "zipfile.getAbsolutePath()===" + zipfile.getAbsolutePath());
        //把日志文件压缩到压缩包中
        if (CompressUtil.zipFileAtPath(logfolder.getAbsolutePath(), zipfile.getAbsolutePath())) {
            LogUtil.d(TAG, "把日志文件压缩到压缩包中 ----> 成功");
            LogWriter.writeLog(TAG, "把日志文件压缩到压缩包中 ----> 成功");
//            for (File crash : crashFileList) {
//                content.append(FileUtil.getText(crash));
//                content.append("\n");
//            }
            LogReport.getInstance().sendFile(zipfile, new ILogUpload.OnUploadFinishedListener() {
                @Override
                public void onSuceess() {
                    LogUtil.d(TAG, "日志发送成功！！");
                    LogWriter.writeLog(TAG, "日志发送成功！！");
//                    boolean isOk = FileUtil.deleteDir(zipfolder);
//                    LogUtil.d(TAG, "删除文件是否成功==" + isOk);
                    LogReport.getInstance().checkCacheSize(path + "log/");
//                    LogUtil.d(TAG, "缓存大小检查，是否删除root下的所有文件 = " + checkresult);
//                    LogWriter.writeLog(TAG, "缓存大小检查，是否删除root下的所有文件 = " + checkresult);
                    stopSelf();
                }

                @Override
                public void onError(String error) {
                    LogUtil.d(TAG, "日志发送失败：" + error);
                    LogWriter.writeLog(TAG, "日志发送失败：" + error);
                    LogReport.getInstance().checkCacheSize(path + "log/");
//                    LogUtil.d(TAG, "缓存大小检查，是否删除root下的所有文件 " + checkresult);
//                    LogWriter.writeLog(TAG, "缓存大小检查，是否删除root下的所有文件 " + checkresult);
                    stopSelf();
                }
            });
//            LogReport.getInstance().getUpload().sendFile(zipfile, content.toString(), new ILogUpload.OnUploadFinishedListener() {
//                @Override
//                public void onSuceess() {
//                    LogUtil.d("日志发送成功！！");
//                    FileUtil.deleteDir(logfolder);
//                    boolean checkresult = checkCacheSize(rootdir);
//                    LogUtil.d("缓存大小检查，是否删除root下的所有文件 = " + checkresult);
//                    stopSelf();
//                }
//
//                @Override
//                public void onError(String error) {
//                    LogUtil.d("日志发送失败：  = " + error);
//                    boolean checkresult = checkCacheSize(rootdir);
//                    LogUtil.d("缓存大小检查，是否删除root下的所有文件 " + checkresult);
//                    stopSelf();
//                }
//            });
        } else {
            LogUtil.d(TAG, "把日志文件压缩到压缩包中 ----> 失败");
            LogWriter.writeLog(TAG, "把日志文件压缩到压缩包中 ----> 失败");
        }
    }


    /**
     * 检查文件夹是否超出缓存大小
     *
     * @param dir 需要检查大小的文件夹
     * @return 返回是否超过大小，true为是，false为否
     */

    public boolean checkCacheSize(File dir) {
        long dirSize = FileUtil.folderSize(dir);
        return dirSize >= LogReport.getInstance().getCacheSize() && FileUtil.deleteDir(dir);
    }

//    /**
//     * 检查文件夹是否超出缓存大小
//     *
//     * @param dir 需要检查大小的文件夹
//     * @return 返回是否超过大小，true为是，false为否
//     */
//
//    public boolean checkCacheSize(File dir, int maxBackupIndex) {
//        long dirSize = FileUtil.folderSize(dir);
//        if (dirSize >= LogReport.getInstance().getCacheSize()) {
//            FileUtil.rollOver(dir, maxBackupIndex);
//            return true;
//        }
//        return false;
//    }
}
