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
}
