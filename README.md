# 崩溃日志上传框架

本框架是基于GitHub上的开源框架进行优化而产生的，在此向原作者致敬，如有任何冒犯的地方请联系我QQ534837240，原作者GitHub地址为：https://github.com/wenmingvs/LogReport
本仓库与原作者的仓库存在不同的地方是：邮件发送模块进行替换，我这边支持所有邮件发送，而且配置很简单。

当App崩溃的时，把崩溃信息保存到本地的同时支持邮件上传和HTTP上传
   
特性介绍  
    
| 特性|简介|
| ------ | ------ |
|自定义日志保存路径 |默认保存在SD卡与程序名同名的文件夹/log中|
|自定义日志缓存大小|默认大小为30M，可动态配置，超出后会自动清空文件夹|
|支持多种上传方式|目前支持邮件上传与HTTP上传，会一并把文件夹下的所有日志打成压缩包作为附件上传|
|日志加密保存|提供AES，DES两种加密解密方式支持，默认不加密|
|日志按天保存|目前崩溃日志和Log信息是按天保存，你可以继承接口来实现更多的保存样式|
|携带设备与OS信息|在创建日志的时候，会一并记录OS版本号，App版本，手机型号等信息，方便还原崩溃|
|自定义日志上传的时机|默认只在Wifi状态下上传支持，也支持在Wifi和移动网络下上传|
|支持保存Log日志|在打印Log的同时，把Log写入到本地（保存的时候会附带线程名称，线程id，打印时间），还原用户操作路径，为修复崩溃提供更多细节信息|

## 依赖添加
在你的项目根目录下的build.gradle文件中加入依赖
``` java
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
添加依赖
``` java
dependencies {
    implementation 'com.github.zhuyuhao121:LogReport-master:1.0.1'
}
```

## 初始化
在自定义Application文件加入以下几行代码即可，默认使用email发送。如果您只需要在本地存储崩溃信息，不需要发送出去，请把initEmailReport（）删掉即可。
``` java
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initCrashReport();
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
                eBean.setEmailPwd("xxx");//发送人邮箱密码
                eBean.setMailHost("smtp.qiye.163.com");//发件服务器地址（需要根据邮箱类型进行修改）
                eBean.setToEmail("534837240@qq.com;");//收送人邮箱，支持批量添加邮箱，邮箱与邮箱之间用英文";"隔开
                LogReport.getInstance().setUploadType(LogReport.UPLOAD_TYPE_EMAIL);
                LogReport.getInstance().setEmailBean(eBean);
    }
}

``` 

## 上传
在任意地方，调用以下方法即可，崩溃发生后，会在下一次App启动的时候使用Service异步打包日志，然后上传日志，发送成功与否，Service都会自动退出释放内存
``` java
LogReport.getInstance().upload(context);
```

## 发往服务器

如果您有自己的服务器，想往服务器发送本地保存的日志文件，而不是通过邮箱发送。请使用以下方法替换initEmailReporter方法
``` java

    /**
     * 使用HTTP发送日志
     */
    private void initHttpReporter() {
        HttpReporter http = new HttpReporter(this);
        http.setUrl("http://crashreport.jd-app.com/your_receiver");//发送请求的地址
        http.setFileParam("fileName");//文件的参数名
        http.setToParam("to");//收件人参数名
        http.setTo("你的接收邮箱");//收件人
        http.setTitleParam("subject");//标题
        http.setBodyParam("message");//内容
        LogReport.getInstance().setUploadType(http);
    }
```
## 保存Log到本地
使用以下方法，打印Log的同时，把Log信息保存到本地（保存的时候会附带线程名称，线程id，打印时间），并且随同崩溃日志一起，发送到特定的邮箱或者服务器上。帮助开发者还原用户的操作路径，更好的分析崩溃产生的原因
``` java
LogWriter.writeLog("TAG", "打Log测试！！！！");
```
