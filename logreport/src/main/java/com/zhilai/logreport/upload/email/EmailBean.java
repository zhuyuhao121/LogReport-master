package com.zhilai.logreport.upload.email;

import java.io.File;
import java.io.UnsupportedEncodingException;

import javax.mail.internet.MimeUtility;

public class EmailBean {

    private String subject;//标题
    private String content;//内容
    private String attachment;//附件
    private File zipfile;//附件
    private String toEmail;//收件人邮箱
    private String fileName;//附件名称
    private String emailAccount;//发件人邮箱账号（最好使用公司账号）
    private String emailPwd;//发件人邮箱密码
    private String mailHost;//发件服务器地址

    public String getToEmail() {
        return null == toEmail ? "534837240@qq.com" : toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public File getZipfile() {
        return zipfile;
    }

    public void setZipfile(File zipfile) {
        this.zipfile = zipfile;
    }

    public String getFileName() {
        try {
            return MimeUtility.encodeText(fileName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getEmailAccount() {
        return emailAccount;
    }

    public void setEmailAccount(String emailAccount) {
        this.emailAccount = emailAccount;
    }

    public String getEmailPwd() {
        return emailPwd;
    }

    public void setEmailPwd(String emailPwd) {
        this.emailPwd = emailPwd;
    }

    public String getMailHost() {
        return mailHost;
    }

    public void setMailHost(String mailHost) {
        this.mailHost = mailHost;
    }
}
