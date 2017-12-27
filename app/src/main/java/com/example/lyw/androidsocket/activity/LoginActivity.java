package com.example.lyw.androidsocket.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.lyw.androidsocket.Config;
import com.example.lyw.androidsocket.R;
import com.example.lyw.androidsocket.bean.LoginBackBean;
import com.example.lyw.androidsocket.bean.LoginBean;
import com.example.lyw.androidsocket.bean.SC01MsgVo;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import sun.misc.BASE64Decoder;

public class LoginActivity extends AppCompatActivity {

    @Bind(R.id.ip_tv)
    EditText ipTv;
    @Bind(R.id.user_name_tv)
    EditText userNameTv;
    @Bind(R.id.password_tv)
    EditText passwordTv;
    @Bind(R.id.login_bt)
    Button loginBt;
    @Bind(R.id.clear_name_iv)
    ImageView clear_name_Iv;
    @Bind(R.id.clear_password_iv)
    ImageView clear_password_Iv;
    private SharedPreferences sp;
    private Socket socket = null;
    //private String IP = "10.43.10.52";
    private String IP = "";
    private int PORT = 9090;
    private BufferedInputStream bis = null;
    private DataInputStream dis = null;
    private OutputStream os = null;
    private StringBuffer sb = null;
    private String ret = "";
    private String loginStr = "";
    private LoginBackBean LoginBackVo;
    private boolean isChinese = true;
    private Thread mThread;
    private boolean isUnConnect = false;
    private String ACTION_NAME = "com.example.lyw.androidsocket.broadcast";
    private String TAG = "mylog";
    private final String SC01 = "SC01";
    private final String SC02 = "SC02";
    private final String SC03 = "SC03";
    private final String SC04 = "SC04";
    private final String SC05 = "SC05";
    private final String SC06 = "SC06";
    private final String CC04 = "CC04";
    private final String CC05 = "CC05";
    private SC01MsgVo msgVo = null;
    private long sendTime = 0L;
    private static final long HEART_BEAT_RATE = 30 * 1000;//心跳间隔,10s
    private int COUNT_HEART_BEAT_RATE = 3;
    Handler handler=new Handler();
    private boolean isStopConnServer = false;//是否还需要尝试连接服务端
    static int i = 0;
    Runnable runnable=new Runnable() {
        @Override
        public void run() {

            if(Config.lastReceiveHeartBeatTime != 0 && (System.currentTimeMillis() - Config
                    .lastReceiveHeartBeatTime) >= HEART_BEAT_RATE
                    * COUNT_HEART_BEAT_RATE){
                Log.d(TAG,"socket is unconnect..");
                isUnConnect = true;
                Config.isCurrentStepGoing = false;
                ConnectServer();
                i++;
                if(i == 3){
                    Log.d(TAG,"isStopConnServer = true ");
                    isStopConnServer = true;
                    sendBroadcastToActivity("","");
                }
            }else{
                //要做的事情
                String message = CC04 + "=[]";
                Log.d(TAG,"send a heart beat..");
                sendMsg(message);
            }
            handler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitive_login);
        ButterKnife.bind(this);
        sp = this.getSharedPreferences("account", MODE_PRIVATE);//如果存在则打开它，否则创建新的Preferences
        setLanguage();
        getUserInfo();
        registerBoradcastReceiver();
        isStopConnServer = false;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(ACTION_NAME) && intent.getStringExtra("id").equals(SC03)){
                //Config.isCurrentStepGoing = false;
                //2.跳转至MainActivity
                Log.d(TAG,"to MainActivity...");
                Intent i = new Intent(LoginActivity.this,MainActivity.class);
                startActivity(i);
                finish();
                //1.开始发送心跳包
                handler.postDelayed(runnable, HEART_BEAT_RATE);
            }
        }
    };

    public void registerBoradcastReceiver(){
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(ACTION_NAME);
        //注册广播
        registerReceiver(mBroadcastReceiver, myIntentFilter);
    }

    private void setLanguage(){
        String lang = sp.getString("language","");
        if(lang.length() == 0 || lang.equals("zh_cn")){
            isChinese = true;
        }else{
            isChinese = false;
        }
        initLables();
    }

    private void initLables(){
        if(isChinese){
            ipTv.setHint(R.string.ip_hint_cn);
            userNameTv.setHint(R.string.user_hint_cn);
            passwordTv.setHint(R.string.psd_hint_cn);
            loginBt.setText(R.string.login_cn);
        }else {
            ipTv.setHint(R.string.ip_hint_eg);
            userNameTv.setHint(R.string.user_hint_eg);
            passwordTv.setHint(R.string.psd_hint_eg);
            loginBt.setText(R.string.login_eg);
        }
    }


    @OnClick({R.id.clear_ip_iv,R.id.clear_name_iv, R.id.clear_password_iv,R.id.login_bt})
    public void onclick(View v) {
        switch (v.getId()) {
            case R.id.clear_name_iv:
                userNameTv.setText("");
                break;
            case R.id.clear_password_iv:
                passwordTv.setText("");
                break;
            case R.id.clear_ip_iv:
                ipTv.setText("");
                break;
            case R.id.login_bt:
                if (!checkInput()) return;
                //mLoading.show();
                ConnectServer();
                break;
        }
    }

    private void getUserInfo() {
        if (sp.getString("user_name", "") != "") {
            String name = sp.getString("user_name", "");
            String pwd = sp.getString("password", "");
            String ip = sp.getString("ip", "");
            userNameTv.setText(name);
            passwordTv.setText(pwd);
            ipTv.setText(ip);
            ipTv.setSelection(ip.length());
        }
    }

    public void ConnectServer(){
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                if(!isStopConnServer){
                    Log.d(TAG,"start try to connect server!");
                    CommunicateWithServer();
                }else {
                    Log.d(TAG,"already stop to connect server!");
                }
            }
        });
        mThread.start();
    }

    public void CommunicateWithServer(){
        try {
            String ip,username,password;
            if(isUnConnect){
                username = sp.getString("user_name", "");
                password = sp.getString("password", "");
                ip = sp.getString("ip", "");
            }else{
                ip = ipTv.getText().toString().trim();
                username = userNameTv.getText().toString().trim();
                password = passwordTv.getText().toString().trim();
            }
            socket = new Socket(ip,PORT);
            if(socket.isConnected()){
                Log.d(TAG,"connect server success!");
            }
            bis = new BufferedInputStream(socket.getInputStream());
            dis = new DataInputStream(bis);
            byte[] bytes = new byte[1024*1024]; // 不用一次读取一个byte，否则会造成socket阻塞
            sb = new StringBuffer();
            String json = new Gson().toJson(new LoginBean(username,password));
            //登录:客户端消息	CC03	客户端发送登录消息到服务端
            loginStr = "CC03=[" + json + "]";
            Log.d(TAG,"loginStr is: " + loginStr);
            sendMsg(loginStr);

            while (dis.read(bytes) != -1) {
                ret += new String(bytes, "UTF-8");
                //Log.d(TAG,"receive data is: " + ret);
                //服务端消息	SC03	服务端返回登录状态
                //if(ret.length() >= 4 && ret.substring(0,4).equals(SC03) && ret.charAt
                //        (ret.length() - 1) == ']'){
                //Config.isCurrentStepGoing：确保客户端在接收消息之前，当前接收到的消息已处理完毕
                if(!Config.isCurrentStepGoing && ret.startsWith("SC03=") && ret.contains("]")){
                    Config.isCurrentStepGoing = true;
                    //json = ret.substring(6,ret.length()-1);
                    json = ret.substring(6,ret.indexOf("]"));
                    Log.d(TAG,"SC03 is: " + json);

                    LoginBackVo = new Gson().fromJson(json,LoginBackBean.class);
                    if(LoginBackVo.getMobileNumber().equals(username) && LoginBackVo.isAllowLogin()){
                        Log.d(TAG,"login success!");
                        saveUserInfo();

                        Config.sk = socket;
                        isUnConnect = false;
                        //向MainActivity发送登录成功的广播
                        sendBroadcastToActivity(SC03,"");
                    }else{
                        isUnConnect = true;
                        ToastOnUI("登录失败！");
                        Log.d(TAG,"login fail!");
                    }
                    ret = "";
                }
                //服务端消息	SC01	服务端广播当前步骤消息到已连接客户端
                //if(ret.length() >= 4 && ret.substring(0,4).equals(SC01) && ret.charAt(ret
                //.length() - 1) == ']'){
                if(!Config.isCurrentStepGoing && ret.startsWith("SC01=") && ret.contains("]")){
                    Config.isCurrentStepGoing = true;
                    //json = ret.substring(6,ret.length()-1);
                    json = ret.substring(6,ret.indexOf("]"));
                    final String s = getFromBase64(json);
                    Log.d(TAG,"SC01 is: " + s);
                    Log.d(TAG,"SC01 ,Config.isCurrentStepGoing is: " + Config.isCurrentStepGoing);
                    //向服务端反馈已接收到消息
                    msgVo = new Gson().fromJson(s,SC01MsgVo.class);
                    String str = CC05 + "=[" + msgVo.getStepId() + "]";
                    sendMsg(str);

                    //向MainActivity发送服务端当前的步骤信息的广播
                    sendBroadcastToActivity(SC01,s);
                    ret = "";
                }
                        /*
                        * 服务端消息	SC05	服务端广播当前暂停/启动状态消息到客户端
                                           true：暂停
                                           false：启动
                         */
                //if(ret.length() >= 4 && ret.substring(0,4).equals(SC05) && ret.charAt(ret
                //.length() - 1) == ']'){
                if(!Config.isCurrentStepGoing && ret.startsWith("SC05=") && ret.contains("]")){
                    Config.isCurrentStepGoing = true;
                    //String serverActionStr = ret.substring(6,ret.length() -1);
                    String serverActionStr = ret.substring(6,ret.indexOf("]"));
                    Log.d(TAG,"SC05 is : " + serverActionStr);
                    Log.d(TAG,"SC05 Config.isCurrentStepGoing is: " + Config.isCurrentStepGoing);
                    //向MainActivity发送服务端当前的暂停/启动状态的广播
                    sendBroadcastToActivity(SC05,serverActionStr);
                    ret = "";
                }
                //服务端消息	SC06	服务端广播重复次数到后的文字提示消息到客户端
                //if(ret.length() >= 4 && ret.substring(0,4).equals(SC06) && ret.charAt(ret
                // .length() - 1) == ']'){
                if(!Config.isCurrentStepGoing && ret.startsWith("SC06=") && ret.contains("]")){
                    Config.isCurrentStepGoing = true;
                    String warnStr = ret.substring(6,ret.indexOf("]"));
                    warnStr = getFromBase64(warnStr);
                    Log.d(TAG,"SC06 is : " + warnStr);
                    //向MainActivity发送服务端当前的文字提示的广播
                    sendBroadcastToActivity(SC06,warnStr);
                    ret = "";
                }
                //服务端消息	SC04	服务端返回心跳连接当前时间
                //if(ret.length() >= 4 && ret.substring(0,4).equals(SC04) && ret.charAt(ret
                //.length() - 1) == ']'){
                if(ret.startsWith("SC04=") && ret.contains("]")){
                    Log.d(TAG,"get a heart beat from server..");
                    Config.lastReceiveHeartBeatTime = System.currentTimeMillis();
                    Log.d(TAG,"Config.lastReceiveHeartBeatTime is: " + Config.lastReceiveHeartBeatTime);
                    ret = "";
                }
            }
        } catch(UnknownHostException e) {
            ToastOnUI("socket通信失败！");
            Log.d(TAG,e + "");
        } catch(Exception e) {
            ToastOnUI("socket通信失败！");
            Log.d(TAG,e + "");
        }
    }

    public void sendMsg(final String str){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    os = socket.getOutputStream();
                    os.write(str.getBytes());
                    os.flush();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //发送广播
    public void sendBroadcastToActivity(String messageId,String content){
        Intent i = new Intent();
        i.setAction(ACTION_NAME);
        i.putExtra("id",messageId);
        i.putExtra("content",content);
        sendBroadcast(i);
    }

    public static String getFromBase64(String s) {
        byte[] b = null;
        String result = null;
        if (s != null) {
            BASE64Decoder decoder = new BASE64Decoder();
            try {
                b = decoder.decodeBuffer(s);
                result = new String(b, "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public void ToastOnUI(final String s){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /***
     * 保存账户与密码
     */
    private void saveUserInfo() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("user_name", userNameTv.getText().toString().trim());
        editor.putString("password", passwordTv.getText().toString().trim());
        editor.putString("ip", ipTv.getText().toString().trim());
        editor.commit();
    }

    public boolean checkInput() {
        String username = userNameTv.getText().toString().trim();
        String password = passwordTv.getText().toString().trim();
        String ip = ipTv.getText().toString().trim();
        if (password.equals("")) {
            Toast.makeText(LoginActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (username.equals("")) {
            Toast.makeText(LoginActivity.this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (ip.equals("")) {
            Toast.makeText(LoginActivity.this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            boolean isExitApp = intent.getBooleanExtra("exit", false);
            if (isExitApp) {
                this.finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
