package com.example.lyw.androidsocket;

import java.net.Socket;

/**
 * Created by lyw on 2017/11/15.
 */

public class Config {
    public static Socket sk;
    public static  Thread thread;
    public static boolean isCurrentStepGoing = false;//当前步骤是否正在进行中
    //public static long lastReceiveHeartBeatTime = 0L;//记录上次接收心跳包的时间
    public static boolean isLogin = false;
    public static boolean isClientPause = false;
    public static String speaker = "xiaolin";
    public static final String CC00 = "CC00";//自定义id:Socket断开的消息
    public static final String CC001 = "CC001";//自定义id:停止当前行为的消息
    public static final String SC01 = "SC01";
    public static final String SC02 = "SC02";
    public static final String SC03 = "SC03";
    public static final String SC04 = "SC04";
    public static final String SC05 = "SC05";
    public static final String SC06 = "SC06";
    public static final String SC08 = "SC08";
    public static final String CC04 = "CC04";
    public static final String CC05 = "CC05";
    public static final String CC06 = "CC06";
    public static final String CC07 = "CC07";
}
