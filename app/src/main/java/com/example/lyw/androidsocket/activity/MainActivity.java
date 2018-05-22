package com.example.lyw.androidsocket.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lyw.androidsocket.Config;
import com.example.lyw.androidsocket.R;
import com.example.lyw.androidsocket.bean.FeedbackBean;
import com.example.lyw.androidsocket.bean.LoginBackBean;
import com.example.lyw.androidsocket.bean.SC01MsgVo;
import com.example.lyw.androidsocket.bean.SC06MsgVo;
import com.google.gson.Gson;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import sun.misc.BASE64Decoder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    @Bind(R.id.exit_bt)
    Button exitBt;
    @Bind(R.id.pause_bt)
    Button pauseBt;
    @Bind(R.id.title_lb)
    TextView titleLb;
    @Bind(R.id.lang_lb)
    TextView langLb;
    @Bind(R.id.show_lb)
    TextView showLb;
    @Bind(R.id.show_tv)
    TextView showTv;
    @Bind(R.id.time_lb)
    TextView timeLb;
    @Bind(R.id.time_tv)
    TextView timeTv;
    @Bind(R.id.result_lb)
    TextView resultLb;
    @Bind(R.id.result_tv)
    TextView resultTv;
    @Bind(R.id.status_lb)
    TextView statusLb;
    @Bind(R.id.status_tv)
    TextView statusTv;
    @Bind(R.id.radioGroup1)
    RadioGroup radioGroup;
    @Bind(R.id.radio0)
    RadioButton radioButton0;
    @Bind(R.id.radio1)
    RadioButton radioButton1;
    @Bind(R.id.input_et)
    EditText input_Et;
    @Bind(R.id.send_bt)
    Button send_Bt;
    @Bind(R.id.input_ll)
    LinearLayout input_Ll;

    private String IP = "";
    private int PORT = 9090;
    private Socket socket = null;
    private SC01MsgVo sc01MsgVo = null;
    private SC06MsgVo sc06MsgVo = null;
    private String successStr = "已连接";
    private String failStr = "未连接";
    //科大讯飞语音识别的结果
    private Map<String,String> mIatResults = new HashMap();
    private OutputStream os = null;
    private String feedbackStr = "";//客户端的回答
    private String pauseStr = "";
    private boolean isPermissionGranted = false;
    private boolean isPause = true;
    private SharedPreferences sp;
    private int waitTime = 0;
    private boolean isChinese = true;
    private boolean isSpeaked = false;//客户端是否已经讲完话
    private String ACTION_NAME = "com.example.lyw.androidsocket.broadcast";
    private String currentId = "";//当前步骤id
    private boolean isNotNeedSpeak = true;//客户端不需要回复：如读取PLC，手动输入
    private String FeedWait = "等待";
    private String FeedGoOn = "继续";
    private boolean isConnect = true;
    private String TAG = "mylog";//除心跳连接外的log
    private String PAUSETAG = "pauselog";
    private SpeechSynthesizer mTts = null;
    private long firstTime = 0L;
    private IntentFilter myIntentFilter,myIntentFilterHome;
    private RecognizerDialog mDialog = null;//语音识别悬浮框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        input_Ll.setVisibility(View.GONE);
        //注册科大讯飞语音识别的Appid
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=59ddeb3e");
        sp = this.getSharedPreferences("account", MODE_PRIVATE);//如果存在则打开它，否则创建新的Preferences
        requestPermissions();//权限申请，如录音，联网，访问存储等
        setLanguage();//设置语言
        radioGroup.setOnCheckedChangeListener(mChangeRadio);//监听中英文切换的动作
        socket = Config.sk;
        registerBoradcastReceiver();//在MainActivity中，注册监听广播

        if(Config.sk != null && Config.sk.isConnected()){//已连接
            isConnect = true;
            showTv(statusTv,isChinese?getResources().getString(R.string.status_on_cn):getResources().getString(R.string.status_on_eg));
        }else {//未连接
            isConnect = false;
            showTv(statusTv,isChinese?getResources().getString(R.string.status_off_cn):getResources().getString(R.string.status_off_eg));
        }
        initTTS();//初始化科大讯飞语音的相关参数配置
    }

    public void initTTS(){
        //1.创建 SpeechSynthesizer 对象, 第二个参数:本地合成时传 InitListener
        mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this, null);
        //2.合成参数设置,详见《MSC Reference Manual》SpeechSynthesizer 类
        //设置发音人
        mTts.setParameter(SpeechConstant.LANGUAGE,"en_us");
        //mTts.setParameter(SpeechConstant.VAD_BOS, "1000");//超时设置
        /*发音人：
        * xiaolin:中英文（台湾普通话）
        * catherine：英文
         */
        /*if(isChinese){
            mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        }else{
            mTts.setParameter(SpeechConstant.VOICE_NAME, "catherine");
        }*/
        //mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaolin");
        if(Config.speaker.trim().equals("")){
            Config.speaker = "xiaoyan";
        }
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");//默认“xiaoyan”
        //mTts.setParameter(SpeechConstant.VOICE_NAME, Config.speaker);
        //设置语速
        mTts.setParameter(SpeechConstant.SPEED, "50");
        //设置音量
        mTts.setParameter(SpeechConstant.VOLUME, "80");
        //设置音量,范围 0~100
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);//设置云端
        //设置合成音频保存位置(可自定义保存位置),保存在“./sdcard/iflytek.pcm”
        //保存在 SD 卡需要在 AndroidManifest.xml 添加写 SD 卡权限
        // 仅支持保存为 pcm 和 wav 格式,如果不需要保存合成音频,注释该行代码
        // mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, "./sdcard/iflytek.pcm"); //3.开始合成
        //String trim = mResultText.getText().toString().trim();
    }
    //监听home点击事件
    private BroadcastReceiver mBroadcastReceiverHome = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                Log.d(TAG,"isConnect is: " + isConnect);
                if(isConnect && !isPause){//如果当前不是暂停状态，就暂停掉
                    pauseBt.callOnClick();
                }
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String content = intent.getStringExtra("content");//内容
            currentId = intent.getStringExtra("id");//当前步骤id
            showTv(resultTv,"");//结果框置空
            showTv(timeTv,"");//倒计时框置空
            switch(currentId){
                case Config.SC01:
                    SC01Action(content);//服务端消息	SC01	服务端广播当前步骤消息到已连接客户端
                    break;
                case Config.SC05:
                    SC05Action(content);//服务端消息	SC05	服务端广播当前暂停/启动状态消息到客户端
                    break;
                case Config.SC06:
                    SC06Action(content);//服务端消息	SC06	服务端广播重复次数到后的文字提示消息到客户端
                    break;
                case Config.SC08:
                    SC08Action(content);//服务端消息	SC08	服务端广播语音朗读者信息到客户端
                    break;
                case Config.CC00://socket已经断开的广播
                    isConnect = false;
                    UnConnectServerAction();//与服务器已经断开时的界面显示
                    unRegisterBoradcastReceiver();//注销广播
                    if(socket != null && socket.isConnected()){
                        try {
                            socket.close();
                            socket = null;
                            Log.d(TAG,"this socket is closed!");
                        }catch (Exception e){
                            //do nothing
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    //服务端消息	SC01	服务端广播当前步骤消息到已连接客户端
    public void SC01Action(String content){
        sc01MsgVo = new Gson().fromJson(content,SC01MsgVo.class);
        String str = isChinese?sc01MsgVo.getStepCNName():sc01MsgVo.getStepENName();
        showTv(showTv,str);//在手机界面上显示当前步骤的内容
        TTS(str);//语音合成，播放步骤的内容
    }

    //服务端消息	SC05	服务端广播当前暂停/启动状态消息到客户端
    public void SC05Action(String content) {
        //服务端动作：true：暂停；false:启动
        if (content.equals("True")) {//暂停：当前是已启动状态，按钮显示的是“暂停”
            Log.d(PAUSETAG,"content is true");
            pauseAction();//暂停
        } else if (content.equals("False")) {//当前是已暂停状态，按钮显示的是“启动”
            Log.d(PAUSETAG,"content is false");
            startAction();//开始或继续
        }
    }

    //暂停
    public void pauseAction(){
        if(mTts != null){
            mTts.pauseSpeaking();
            Log.d(TAG,"pause speaking...");
        }
        pauseBt.setText(isChinese? getResources().getString(R.string.start_cn):getResources().getString(R.string.start_eg));
        isPause = true;
    }

    //开始或继续
    public void startAction(){
        if(mTts != null){
            Log.d(TAG,"start speaking..");
            mTts.startSpeaking(showTv.getText().toString().trim(),mTtsListener);
        }
        pauseBt.setText(isChinese?getResources().getString(R.string.pause_cn):getResources().getString(R.string.pause_eg));
        isPause = false;
    }

    //服务端消息	SC06	服务端广播重复次数到后的文字提示消息到客户端
    public void SC06Action(String content){
        sc06MsgVo = new Gson().fromJson(content,SC06MsgVo.class);
        String str = isChinese?sc06MsgVo.getPromptCN():sc06MsgVo.getPromptEN();
        showTv(showTv,str);//在手机界面上显示当前步骤的内容
        TTS(str);//语音合成，播放步骤的内容
    }

    //服务端消息	SC08	服务端广播语音朗读者信息到客户端
    public void SC08Action(String content){
        Config.speaker = content;
        //Config.speaker = "xiaoyan";
        Log.d(TAG,"Speaker is: " + content);
    }

    public void UnConnectServerAction(){
        showTv(statusTv,isChinese?getResources().getString(R.string.status_off_cn):getResources().getString(R.string.status_off_eg));
        pauseBt.setClickable(false);
        pauseBt.setTextColor(Color.GRAY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterBoradcastReceiver();
    }

    //注册广播
    public void registerBoradcastReceiver(){
        myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(ACTION_NAME);
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mBroadcastReceiver,
                myIntentFilter);

        //注册Home键被按下的广播
        myIntentFilterHome = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mBroadcastReceiverHome, myIntentFilterHome);
    }

    //设置语言
    private void setLanguage(){
        String lang = sp.getString("language","");
        if(lang.length() == 0 || lang.equals("zh_cn")){
            radioGroup.check(radioButton0.getId());
            isChinese = true;
        }else{
            radioGroup.check(radioButton1.getId());
            isChinese = false;
        }
        initLables();
    }

    private void initLables(){
        if(isChinese){
            titleLb.setText(R.string.title_cn);
            langLb.setText(R.string.lang_cn);
            showLb.setText(R.string.show_cn);
            timeLb.setText(R.string.time_cn);
            resultLb.setText(R.string.result_cn);
            statusLb.setText(R.string.status_cn);

            exitBt.setText(getResources().getString(R.string.exit_cn));
            if(isPause){//如果当前已处于暂停状态
                pauseBt.setText(getResources().getString(R.string.start_cn));
            }else{
                pauseBt.setText(getResources().getString(R.string.pause_cn));
            }
            if(statusTv.getText().equals(getResources().getString(R.string.status_on_eg))){
                statusTv.setText(R.string.status_on_cn);
            }else {
                statusTv.setText(R.string.status_off_cn);
            }
        }else {
            titleLb.setText(R.string.title_eg);
            langLb.setText(R.string.lang_eg);
            showLb.setText(R.string.show_eg);
            timeLb.setText(R.string.time_eg);
            resultLb.setText(R.string.result_eg);
            statusLb.setText(R.string.status_eg);

            exitBt.setText(getResources().getString(R.string.exit_eg));
            if(isPause){//如果当前已处于暂停状态
                pauseBt.setText(getResources().getString(R.string.start_eg));
            }else{
                pauseBt.setText(getResources().getString(R.string.pause_eg));
            }

            if(statusTv.getText().equals(getResources().getString(R.string.status_on_cn))){
                statusTv.setText(R.string.status_on_eg);
            }else {
                statusTv.setText(R.string.status_off_eg);
            }
        }
    }

    //中英文切换的监听
    private RadioGroup.OnCheckedChangeListener mChangeRadio = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            SharedPreferences.Editor editor = sp.edit();
            if (checkedId == radioButton0.getId()) {
                editor.putString("language", "zh_cn");
                isChinese = true;
            } else if (checkedId == radioButton1.getId()) {
                editor.putString("language", "en_us");
                isChinese = false;
            }
            editor.commit();
            initLables();
        }
    };

    //权限申请
    public void requestPermissions(){
        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission
                .READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission
                .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission
                .RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission
                .INTERNET) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.INTERNET);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission
                .ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if(!permissionList.isEmpty()){
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else {
            isPermissionGranted = true;
        }
    }

    //用户权限授权的结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults.length > 0){
                    for(int result : grantResults){
                        if(result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(MainActivity.this,"必须同意所有的权限",Toast.LENGTH_SHORT).show();
                            isPermissionGranted = false;
                            return;
                        }
                    }
                    isPermissionGranted = true;
                }else {
                    Toast.makeText(MainActivity.this,"发送未知错误",Toast.LENGTH_SHORT
                    ).show();
                }
                break;
        }
    }

    @OnClick({R.id.exit_bt,R.id.pause_bt,R.id.send_bt})
    public void onClick(View v) {
        if(isPermissionGranted){
            switch (v.getId()){
                case R.id.exit_bt:
                    String titleStr = "";
                    String contentStr = "";
                    String posStr = "";
                    String negStr = "";
                    if(isChinese){
                        titleStr = getResources().getString(R.string.dialog_title_cn);
                        contentStr = getResources().getString(R.string.dialog_content_cn);
                        posStr = getResources().getString(R.string.dialog_pos_cn);
                        negStr = getResources().getString(R.string.dialog_neg_cn);
                    }else {
                        titleStr = getResources().getString(R.string.dialog_title_eg);
                        contentStr = getResources().getString(R.string.dialog_content_eg);
                        posStr = getResources().getString(R.string.dialog_pos_eg);
                        negStr = getResources().getString(R.string.dialog_neg_eg);
                    }

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(titleStr)
                            .setMessage(contentStr)
                            .setCancelable(true)
                            .setPositiveButton(posStr, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //退出
                                    try{
                                        socket.close();
                                        socket = null;
                                    }catch (IOException e){

                                    }
                                    /*Intent intent = new Intent(MainActivity.this,
                                            MainActivity.class);
                                    intent.putExtra("exit", true);
                                    MainActivity.this.startActivity(intent);*/
                                    System.exit(0);
                                }
                            })
                            .setNegativeButton(negStr, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).create().show();

                    break;
                case R.id.pause_bt://暂停或启动
                    if(socket != null && socket.isConnected()) {
                        if(isPause){//已暂停，按钮显示的是“启动”
                            startAction();//开始或继续播放
                            requestPauseOrStart(false);//启动
                        }else {//已启动状态，按钮显示的是“暂停”
                            pauseAction();
                            requestPauseOrStart(true);//暂停
                        }
                    }else {
                        Toast.makeText(MainActivity.this,"与服务器的连接已断开，请先连接服务器！",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.send_bt:
                    String inputText = input_Et.getText().toString().trim();
                    if(currentId == Config.SC01 && (sc01MsgVo.getCommandType() == 2)){
                        if(inputText.length() == 0){
                            input_Et.setText("");
                            Toast.makeText(MainActivity.this,"请输入数据",Toast.LENGTH_SHORT).show();
                        }else{
                            commitJSONtoServer(inputText);
                            input_Ll.setVisibility(View.GONE);
                            input_Et.setText("");
                        }
                    }
                    break;
            }
        }else {
            requestPermissions();
        }
    }

    public void requestPauseOrStart(boolean b){
        if(b){//暂停
            pauseStr = "CC02=[True]";
        }else {//启动
            pauseStr = "CC02=[False]";
        }
        Log.d(TAG,"your pauseStr is: " + pauseStr);
        sendMsg(pauseStr);
    }

    //倒计时
    private void startCountDownTime(final long time) {
        final CountDownTimer timer = new CountDownTimer(time * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                if(isSpeaked){//客户端已经讲完话、
                    timeTv.setText("");
                    this.onFinish();
                }else {
                    timeTv.setText((millisUntilFinished / 1000) + "s");
                }
            }

            @Override
            public void onFinish() {
                timeTv.setText("");
            }
        };
        timer.start();
    }

    public void showTv(final TextView tv,final String s){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText(s);
                if((tv.getId() == R.id.status_tv) && isConnect){
                    //tv.setTextColor(Color.rgb(176,226,255));
                    tv.setTextColor(Color.BLUE);
                }else if((tv.getId() == R.id.status_tv) && !isConnect){
                    tv.setTextColor(Color.RED);
                }
            }
        });
    }

    //语音合成（TTS）监听
    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {

        }

        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {

        }

        @Override
        public void onSpeakPaused() {

        }

        @Override
        public void onSpeakResumed() {

        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {

        }

        @Override
        public void onCompleted(SpeechError speechError) {
            if (speechError == null) {
                //客户端消息	CC07	客户端发送语音播放完毕消息到服务端
                Log.d(TAG,"send cc07...");
                String message = Config.CC07 + "=[]";
                sendMsg(message);

                switch (currentId){
                    case Config.SC01:
                        waitTime = sc01MsgVo.getWaitTime();
                        if(sc01MsgVo.getCommandType() == 1){//语音提示等待回答
                            input_Ll.setVisibility(View.GONE);
                            //开始倒计时
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    timeTv.setText(waitTime + "s");
                                    isSpeaked =false;//用户还未开始讲话
                                    startCountDownTime(waitTime);
                                }
                            });
                            Log.d(TAG,"sc01 type is 1");
                            Feedback();//用户回答
                        }else if(sc01MsgVo.getCommandType() == 2){//语音提示等待录入
                            input_Ll.setVisibility(View.VISIBLE);//显示输入框
                            //开始倒计时
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    timeTv.setText(waitTime + "s");
                                    isSpeaked =false;
                                    startCountDownTime(waitTime);
                                }
                            });
                            Log.d(TAG,"sc01 type is 2");
                        }else if(sc01MsgVo.getCommandType() == 4){//获取PLC后语音提示
                            input_Ll.setVisibility(View.GONE);
                            //开始倒计时
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    timeTv.setText(waitTime + "s");
                                    isSpeaked =false;
                                    startCountDownTime(waitTime);
                                }
                            });
                            Log.d(TAG,"sc01 type is 4");
                        }else {//语音提示获取PLC  ,  语音提示
                            input_Ll.setVisibility(View.GONE);
                            //开始倒计时
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    timeTv.setText(waitTime + "s");
                                    isSpeaked =false;
                                    startCountDownTime(waitTime);
                                }
                            });
                            Log.d(TAG,"sc01 type is 3 或 0");
                        }
                        break;
                    case Config.SC06:
                        Feedback();//用户回答
                        break;
                    default:
                        break;
                }
            } else if (speechError != null) {
                Log.d(TAG,"语音播放出现问题..");
            }
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    //用户回答
    public void Feedback(){
        if(socket != null && socket.isConnected()){
            Log.d(TAG,"start to STT()");
            STT();//语音识别，用户回答
        }
    }

    public void TTS(String content){
        if(content != ""){
            if (TextUtils.isEmpty(content)) {//步骤内容为空的处理
                if(isChinese){
                    mTts.startSpeaking(getResources().getString(R.string.speek_null_cn), mTtsListener);
                }else {
                    mTts.startSpeaking(getResources().getString(R.string.speek_null_eg), mTtsListener);
                }
            }else {//步骤内容不为空的处理
                mTts.startSpeaking(content, mTtsListener);
            }
        }
    }

    //语音识别，用户回答
    public void STT(){
        showTv(resultTv,"");//结果框置空
        //1.创建RecognizerDialog对象
        mDialog = new RecognizerDialog(MainActivity.this, new InitListener() {
            @Override
            public void onInit(int i) {

            }
        });
        String waitTime = "";
        /*if(msgVo.getWaitTime()>10){
            waitTime = 10 * 1000 + "";
        }else {
            waitTime = msgVo.getWaitTime() + "";
        }*/
        mDialog.setParameter(SpeechConstant.VAD_BOS, "10000");//设置超时时间
        //2.设置accent、language等参数
        if(isChinese){
            mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        }else {
            mDialog.setParameter(SpeechConstant.LANGUAGE, "en_us");
        }
        mDialog.setParameter(SpeechConstant.ASR_PTT,"0");//是否显示标点符号
        mDialog.setListener(new RecognizerDialogListener() {
            @Override
            public void onResult(com.iflytek.cloud.RecognizerResult recognizerResult, boolean b) {
                if(mDialog.isShowing()){
                    mDialog.dismiss();
                }
                isSpeaked = true;//用户回答完毕
                //将用户回答的结果提交至服务端
                submitToServer(recognizerResult);
            }

            @Override
            public void onError(SpeechError speechError) {
                Log.d(TAG,"call STT() error: " + speechError);
                if(mDialog.isShowing()){
                    Log.d(TAG,"dismiss dialog..");
                    mDialog.dismiss();//超时后，对话框自动消失
                }
                showTv(resultTv,speechError + "s");
                isSpeaked = true;
                commitJSONtoServer("");
            }
        });
        // 4.显示dialog,接收语音输入
        mDialog.show();
        Log.d(TAG,"mDialog.show() is called..");
    }

    //将用户回答的结果提交至服务端
    private void submitToServer(com.iflytek.cloud.RecognizerResult results) {
        JsonParser jsonParser = new JsonParser();
        String text = jsonParser.parseIatResult(results.getResultString());
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);
        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        String result = resultBuffer.toString();//最终的语音解析结果
        Log.d(TAG,"result is: " + result);
        showTv(resultTv,result);//显示结果
        commitJSONtoServer(result);
    }

    public void commitJSONtoServer(String str) {
        //回答步骤（sc01）的结果
        if(currentId.equals(Config.SC01)) {
            if (sc01MsgVo != null) {
                String json = new Gson().toJson(new FeedbackBean(sc01MsgVo.getStepId(), str));
                feedbackStr = "CC01=[" + json + "]";
                Log.d(TAG, "your speak is: " + feedbackStr);
                sendMsg(feedbackStr);
            }
        }else if (currentId.equals(Config.SC06)) {//回答超时（sc06）的结果
            String feedStr = "";
            if(str.equals(FeedGoOn)){//继续
                feedStr = Config.CC06 + "=[" + 1 + "]";
            }else if(str.equals(FeedWait)){//等待
                feedStr = Config.CC06 + "=[" + 2 + "]";
            }else{//缺省值,超时或者除继续和等待外的回答
                feedStr = Config.CC06 + "=[" + 2 + "]";
            }
            Log.d(TAG, "your feed is: " + feedStr);
            sendMsg(feedStr);
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

    //按下Back的处理
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            long secondTime = System.currentTimeMillis();
            if (secondTime - firstTime > 2000) {
                Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                firstTime = secondTime;
                return true;
            } else {
                //退出
                try{
                    socket.close();
                    socket = null;
                }catch (IOException e){
                    //do  nothing
                }
                /*Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.putExtra("exit", true);
                MainActivity.this.startActivity(intent);*/
                System.exit(0);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    //注销广播接收器
    public void unRegisterBoradcastReceiver(){
        if (null != myIntentFilter) {
            LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver
                    (mBroadcastReceiver);
        }
        if (null != myIntentFilterHome) {
            try {
                unregisterReceiver(mBroadcastReceiverHome);
            } catch (Exception e){
                //do nothing
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBoradcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterBoradcastReceiver();
    }
}
