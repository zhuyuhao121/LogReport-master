package com.zhilai.logreport.save.imp;

import com.zhilai.logreport.LogReport;
import com.zhilai.logreport.save.ISave;

/**
 * 用于写入Log到本地
 */
public class LogWriter {
    private static LogWriter mLogWriter;
    private static ISave mSave;

    private LogWriter() {
    }


    public static LogWriter getInstance() {
        if (mLogWriter == null) {
            synchronized (LogReport.class) {
                if (mLogWriter == null) {
                    mLogWriter = new LogWriter();
                }
            }
        }
        return mLogWriter;
    }


    public LogWriter init(ISave save) {
        mSave = save;
        return this;
    }

    public static void writeLog(String tag, String content) {
//        LogUtil.d(tag, content);
        if (mSave != null) {
            mSave.writeLog(tag, content);
        }
    }
}