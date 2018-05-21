package com.example.lyw.androidsocket.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
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
import com.example.lyw.androidsocket.widget.BaseActivity;
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

public class LoginActivity extends BaseActivity {

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
    private boolean isUnConnect = false;//是否已断开连接
    private String ACTION_NAME = "com.example.lyw.androidsocket.broadcast";//自定义广播
    private String TAG = "mylog";//除心跳连接外的log
    private String HEARTCONNTAG = "heartlog";//心跳连接的log
    private SC01MsgVo msgVo = null;
    private long lastReceiveHeartBeatTime = 0L;//上次接收到服务端的心跳反馈的时间
    private static final long HEART_BEAT_RATE = 30 * 1000;//心跳间隔,30s
    private int COUNT_HEART_BEAT_RATE = 2;//心跳连接断开后，尝试连接的次数
    private Handler handler = new Handler();
    private boolean isStopConnServer = false;//是否还需要尝试连接服务端
    private IntentFilter myIntentFilter = null;

    private Runnable runnable=new Runnable() {
        @Override
        public void run() {
            if(!isStopConnServer){//isStopConnServer=true就停止线程
                //获取当前时间 与 上次接收到服务端的心跳反馈的时间的时间间隔diffTime
                long diffTime = System.currentTimeMillis() - lastReceiveHeartBeatTime;
                Log.d(HEARTCONNTAG,"lastReceiveHeartBeatTime is: " + lastReceiveHeartBeatTime);
                Log.d(HEARTCONNTAG,"diffTime is: " + diffTime);
                //如果时间间隔diffTime 超过了 心跳连接的尝试连接的时间（30s * 2次），就认为心跳连接已断开
                if(lastReceiveHeartBeatTime != 0 &&  diffTime>= HEART_BEAT_RATE * COUNT_HEART_BEAT_RATE){
                    Log.d(HEARTCONNTAG,"socket is unconnect..");
                    //标记心跳连接已断开
                    isUnConnect = true;
                    //不再重新登录连接socket
                    isStopConnServer = true;
                    Log.d(HEARTCONNTAG,"isStopConnServer = true ");
                    //发送心跳连接已断开的全局广播
                    sendBroadcastToActivity(Config.CC00,"");
                }else{
                    //发出心跳连接的消息
                    String message = Config.CC04 + "=[]";
                    Log.d(HEARTCONNTAG,"send a heart beat..");
                    sendMsg(message);
                }
                //每隔指定的时间HEART_BEAT_RATE，发起一次心跳连接
                handler.postDelayed(this, HEART_BEAT_RATE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitive_login);
        ButterKnife.bind(this);
        sp = this.getSharedPreferences("account", MODE_PRIVATE);//如果存在则打开它，否则创建新的Preferences
        setLanguage();//设置语言
        getUserInfo();//获取用户信息，如果之前已经成功登录过，则自动填写用户信息
        registerBoradcastReceiver();//注册广播
        isStopConnServer = false;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //接收登录成功的广播
            if(action.equals(ACTION_NAME) && intent.getStringExtra("id").equals(Config.SC03)){
                //获取发音人
                String getSpeakerStr = "CC08" + "=[]";
                sendMsg(getSpeakerStr);
                //获取服务端的状态
                String getStatusStr = "CC09" + "=[]";
                sendMsg(getStatusStr);
                //2.跳转至MainActivity
                Log.d(TAG,"to MainActivity...");
                Intent i = new Intent(LoginActivity.this,MainActivity.class);
                startActivity(i);
                LoginActivity.this.finish();
                //30s后开始发送心跳包
                handler.postDelayed(runnable, HEART_BEAT_RATE);
                //handler.post(runnable);
            }
        }
    };

    //注册本地广播，更快，高效，安全
    public void registerBoradcastReceiver(){
        myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(ACTION_NAME);
        LocalBroadcastManager.getInstance(LoginActivity.this).registerReceiver(mBroadcastReceiver,
                myIntentFilter);
    }

    //设置语言
    private void setLanguage(){
        String lang = sp.getString("language","");
        if(lang.length() == 0 || lang.equals("zh_cn")){
            isChinese = true;
        }else{
            isChinese = false;
        }
        initLables();//初始化文本显示
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
            case R.id.clear_name_iv://清除输入的用户名
                userNameTv.setText("");
                break;
            case R.id.clear_password_iv://清除输入的密码
                passwordTv.setText("");
                break;
            case R.id.clear_ip_iv://清除输入的ip
                ipTv.setText("");
                break;
            case R.id.login_bt://点击登录
                //检查输入的用户信息是否合法
                if (!checkInput()) return;
                mLoading.show();
                //Socket通信尝试
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
                    Log.d(HEARTCONNTAG,"start try to connect server!");
                    CommunicateWithServer();
                }else {
                    if(mLoading.isShowing()) mLoading.dismiss();
                    Log.d(HEARTCONNTAG,"already stop to connect server!");
                }
            }
        });
        mThread.start();
    }

    public void CommunicateWithServer(){
        try {
            String ip,username,password;
            ip = ipTv.getText().toString().trim();
            username = userNameTv.getText().toString().trim();
            password = passwordTv.getText().toString().trim();
            socket = new Socket(ip,PORT);
            if(!socket.isConnected()){
                return;
            }
            Log.d(TAG,"connect server success!");
            if(mLoading.isShowing()) mLoading.dismiss();
            bis = new BufferedInputStream(socket.getInputStream());
            dis = new DataInputStream(bis);
            byte[] bytes = new byte[1024*1024]; // 不用一次读取一个byte，否则会造成socket阻塞
            sb = new StringBuffer();
            //将username,password信息组合成json串
            String loginJsonStr = new Gson().toJson(new LoginBean(username,password));
            //登录:客户端消息	CC03	客户端发送登录消息到服务端
            loginStr = "CC03=[" + loginJsonStr + "]";
            Log.d(TAG,"loginStr is: " + loginStr);
            sendMsg(loginStr);

            while (dis.read(bytes) != -1) {
                ret += new String(bytes, "UTF-8");
                ret = ret.substring(0,ret.indexOf("]") + 1);
                Log.d(TAG,"receive data is: " + ret);
                String json = "";
                //服务端消息	SC03	服务端返回登录状态
                if(ret.length() != 0 && ret.startsWith("SC03=") && ret.contains("]")){
                    json = ret.substring(6,ret.indexOf("]"));
                    Log.d(TAG,"SC03 is: " + json);
                    LoginBackVo = new Gson().fromJson(json,LoginBackBean.class);
                    if(LoginBackVo.getMobileNumber().equals(username) && LoginBackVo.isAllowLogin()){
                        Log.d(TAG,"login success!");
                        saveUserInfo();//保存用户信息
                        Config.sk = socket;
                        isUnConnect = false;
                        //向MainActivity发送登录成功的广播
                        sendBroadcastToActivity(Config.SC03,"");
                    }else{
                        isUnConnect = true;
                        //ToastOnUI("登录失败！");
                        Log.d(TAG,"login fail!");
                    }
                    ret = "";
                }
                //服务端消息	SC01	服务端广播当前步骤消息到已连接客户端
                if(ret.length() != 0  && ret.startsWith("SC01=") && ret.contains("]")){
                    json = ret.substring(6,ret.indexOf("]"));
                    final String s = getFromBase64(json);//Base64解密
                    Log.d(TAG,"SC01 is: " + s);

                    //客户端消息 CC05  客户端发送当前步骤授受消到服务端，内容为接收的stepId
                    msgVo = new Gson().fromJson(s,SC01MsgVo.class);
                    String str = Config.CC05 + "=[" + msgVo.getStepId() + "]";
                    sendMsg(str);

                    //向MainActivity发送服务端当前的步骤信息的广播
                    sendBroadcastToActivity(Config.SC01,s);
                    ret = "";
                }
                /*
                * 服务端消息	SC05	服务端广播当前暂停/启动状态消息到客户端
                                   true：暂停
                                   false：启动
                 */
                if(ret.length() != 0 && ret.startsWith("SC05=") && ret.contains("]")){
                    String serverActionStr = ret.substring(6,ret.indexOf("]"));
                    Log.d(TAG,"SC05 is : " + serverActionStr);
                    //向MainActivity发送服务端当前的暂停/启动状态的广播
                    sendBroadcastToActivity(Config.SC05,serverActionStr);
                    ret = "";
                }
                //服务端消息	SC06	服务端广播重复次数到后的文字提示消息到客户端
                if(ret.length() != 0  && ret.startsWith("SC06=") && ret.contains("]")){
                    String warnStr = ret.substring(6,ret.indexOf("]"));
                    warnStr = getFromBase64(warnStr);//Base64解密
                    Log.d(TAG,"SC06 is : " + warnStr);
                    //向MainActivity发送服务端当前的文字提示的广播
                    sendBroadcastToActivity(Config.SC06,warnStr);
                    ret = "";
                }

                //服务端消息	SC04	服务端返回心跳连接当前时间
                if(ret.length() != 0 && ret.startsWith("SC04=") && ret.contains("]")){
                    //记录服务端返回心跳连接的时间
                    lastReceiveHeartBeatTime = System.currentTimeMillis();
                    Log.d(HEARTCONNTAG,"get a heart beat from server..");
                    Log.d(HEARTCONNTAG,"lastReceiveHeartBeatTime is: " + lastReceiveHeartBeatTime);
                    ret = "";
                }

                //服务端消息	SC08	服务端广播语音朗读者信息到客户端
                if(ret.length() != 0  && ret.startsWith("SC08=") && ret.contains("]")){
                    String str = ret.substring(6,ret.indexOf("]"));
                    Log.d(TAG,"SC08 is: " + str);
                    sendBroadcastToActivity(Config.SC08,str);
                    ret = "";
                }
            }
        } catch(UnknownHostException e) {
            //ToastOnUI("socket通信失败！");
            Log.d(TAG,e + "");
            if(mLoading.isShowing()) mLoading.dismiss();
        } catch(Exception e) {
            //ToastOnUI("socket通信失败！");
            Log.d(TAG,e + "");
            if(mLoading.isShowing()) mLoading.dismiss();
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
        //sendBroadcast(i);
        //本地广播更高效，安全
        LocalBroadcastManager.getInstance(LoginActivity.this).sendBroadcast(i);
    }

    //注销广播接收器
    public void unRegisterBoradcastReceiver(){
        if (null != myIntentFilter) {
            LocalBroadcastManager.getInstance(LoginActivity.this).unregisterReceiver(mBroadcastReceiver);
        }
    }

    //Base64解密
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

    //检查输入的内容是否合法
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

    //onNewIntent(Intent intent) 是Override Activity的父类方法，只有仅在点Home键退出Activity而再次启动新的Intent进来才被调用到;
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
    protected void onResume() {
        super.onResume();
        registerBoradcastReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterBoradcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterBoradcastReceiver();
        mThread.interrupt();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
