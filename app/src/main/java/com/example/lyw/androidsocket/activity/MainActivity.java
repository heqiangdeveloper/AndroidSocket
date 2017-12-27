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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
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
    String ret = "";
    BufferedInputStream bis = null;
    DataInputStream dis = null;

    private String IP = "";
    private int PORT = 9090;
    private Socket socket = null;
    ArrayList<String> JsonLists = new ArrayList<>();
    String json = "";
    StringBuffer sb = null;
    private SC01MsgVo sc01MsgVo = null;
    private SC06MsgVo sc06MsgVo = null;
    private String successStr = "已连接";
    private String failStr = "未连接";
    private Map<String,String> mIatResults = new HashMap();
    private final String SC01 = "SC01";
    private final String SC02 = "SC02";
    private final String SC03 = "SC03";
    private final String SC05 = "SC05";
    private final String SC06 = "SC06";
    private final String CC06 = "CC06";
    private final String CC07 = "CC07";
    private OutputStream os = null;
    private boolean isNeedFeedback = false;
    private Handler mHandler = null;
    private String feedbackStr = "";//客户端的回答
    private String pauseStr = "";
    private boolean isPermissionGranted = false;
    private boolean isPause = true;
    private SharedPreferences sp;
    private int waitTime = 0;
    private boolean isChinese = true;
    private final long tryTime = 5 * 1000;//5s
    private RecognizerDialog mIatDialog;
    private boolean isSpeaked = false;//客户端是否已经讲完话
    private boolean isTTSEnd = true;//客户端是否已经播放完语音
    private LoginBackBean LoginBackVo;
    private String ACTION_NAME = "com.example.lyw.androidsocket.broadcast";
    private String currentId = "";
    private String FeedWait = "等待";
    private String FeedGoOn = "继续";
    private boolean isConnect = true;
    private String TAG = "mylog";
    private SpeechSynthesizer mTts = null;
    private long firstTime = 0L;
    private IntentFilter myIntentFilter,myIntentFilterHome;

    private static final String LOG_TAG = "HomeReceiver";
    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    private static final String SYSTEM_DIALOG_REASON_LOCK = "lock";
    private static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=59ddeb3e");
        sp = this.getSharedPreferences("account", MODE_PRIVATE);
        requestPermissions();
        setLanguage();
        radioGroup.setOnCheckedChangeListener(mChangeRadio);
        socket = Config.sk;
        registerBoradcastReceiver();

        if(Config.sk != null && Config.sk.isConnected()){//已连接
            isConnect = true;
            if(isChinese){
                showTv(statusTv,getResources().getString(R.string.status_on_cn));
            }else {
                showTv(statusTv,getResources().getString(R.string.status_on_eg));
            }
        }else {
            isConnect = false;
            if(isChinese){
                showTv(statusTv,getResources().getString(R.string.status_off_cn));
            }else {
                showTv(statusTv,getResources().getString(R.string.status_off_eg));
            }
        }
        //ConnectServer();
        //getConnectStatus(tryTime);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(LOG_TAG, "onReceive: action: " + action);
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                // android.intent.action.CLOSE_SYSTEM_DIALOGS
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                Log.i(LOG_TAG, "reason: " + reason);

                if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                    // 短按Home键
                    Log.i(LOG_TAG, "homekey");

                }
                else if (SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)) {
                    // 长按Home键 或者 activity切换键
                    Log.i(LOG_TAG, "long press home key or activity switch");
                }
                else if (SYSTEM_DIALOG_REASON_LOCK.equals(reason)) {
                    // 锁屏
                    Log.i(LOG_TAG, "lock");
                }
                else if (SYSTEM_DIALOG_REASON_ASSIST.equals(reason)) {
                    // samsung 长按Home键
                    Log.i(LOG_TAG, "assist");
                }
            }

            String content = intent.getStringExtra("content");
            currentId = intent.getStringExtra("id");
            showTv(resultTv,"");
            showTv(timeTv,"");
            switch(currentId){
                case SC01:
                    SC01Action(content);//服务端消息	SC01	服务端广播当前步骤消息到已连接客户端
                    break;
                case SC05:
                    SC05Action(content);//服务端消息	SC05	服务端广播当前暂停/启动状态消息到客户端
                    break;
                case SC06:
                    SC06Action(content);//服务端消息	SC06	服务端广播重复次数到后的文字提示消息到客户端
                    break;
                case "":
                    isConnect = false;
                    UnConnectServerAction();//与服务器已经断开的消息
                    break;
                default:
                    break;
            }
        }

    };

    //服务端消息	SC01	服务端广播当前步骤消息到已连接客户端
    public void SC01Action(String content){
        sc01MsgVo = new Gson().fromJson(content,SC01MsgVo.class);
        String str = "";
        if(isChinese){
            str = sc01MsgVo.getStepCNName();
        }else {
            str = sc01MsgVo.getStepENName();
        }
        Log.d(TAG,"sc01 is : " + str);
        showTv(showTv,str);
        TTS(str);
    }

    //服务端消息	SC05	服务端广播当前暂停/启动状态消息到客户端
    public void SC05Action(String content) {
        //服务端动作：true：暂停；false:启动
        if (content.equals("True") && !isPause) {
            if(!isTTSEnd && mTts != null && mTts.isSpeaking()){
                mTts.pauseSpeaking();
            }
            if (isChinese) {
                pauseBt.setText(getResources().getString(R.string.start_cn));
            } else {
                pauseBt.setText(getResources().getString(R.string.start_eg));
            }
            isPause = true;
        } else if (content.equals("False") && isPause) {//已启动状态，按钮显示的是“暂停”
            if(!isTTSEnd && mTts != null && !mTts.isSpeaking()){
                mTts.resumeSpeaking();
            }
            if (isChinese) {
                pauseBt.setText(getResources().getString(R.string.pause_cn));
            } else {
                pauseBt.setText(getResources().getString(R.string.pause_eg));
            }
            isPause = false;
        }
        Config.isCurrentStepGoing = false;
    }

    //服务端消息	SC06	服务端广播重复次数到后的文字提示消息到客户端
    public void SC06Action(String content){
        sc06MsgVo = new Gson().fromJson(content,SC06MsgVo.class);
        String str = "";
        if(isChinese){
            str = sc06MsgVo.getPromptCN();
        }else {
            str = sc06MsgVo.getPromptEN();
        }
        showTv(showTv,str);
        TTS(str);
    }

    public void UnConnectServerAction(){
        if(isChinese){
            showTv(statusTv,getResources().getString(R.string.status_off_cn));
        }else {
            showTv(statusTv,getResources().getString(R.string.status_off_eg));
        }
    }

    public void registerBoradcastReceiver(){
        myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(ACTION_NAME);
        myIntentFilterHome = new IntentFilter();
        myIntentFilterHome.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //注册广播
        registerReceiver(mBroadcastReceiver, myIntentFilter);
        registerReceiver(mBroadcastReceiver, myIntentFilterHome);
    }


    public void getConnectStatus(final long tryTime){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();
                while (true){
                    CountDownTimer timer = new CountDownTimer(tryTime, 1000) {

                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {

                            if(Config.sk!= null && !Config.sk.isConnected()){//连接断开
                                try {
                                    IP = sp.getString("ip", "");
                                    Log.d("mythread","ip is: " + IP);
                                    socket = new Socket(IP,PORT);
                                    if(isChinese){
                                        showTv(statusTv,getResources().getString(R.string.status_on_cn));
                                    }else {
                                        showTv(statusTv,getResources().getString(R.string.status_on_eg));
                                    }
                                    Config.sk = socket;
                                }catch (IOException e){
                                    if(isChinese){
                                        showTv(statusTv,getResources().getString(R.string
                                                .status_off_cn));
                                    }else {
                                        showTv(statusTv,getResources().getString(R.string
                                                .status_off_eg));
                                    }
                                }
                            }
                        }
                    };
                    timer.start();
                }
            }
        }).start();
    }

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

    @OnClick({R.id.exit_bt,R.id.pause_bt})
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
                                    }catch (IOException e){

                                    }
                                    Intent intent = new Intent(MainActivity.this,
                                            MainActivity.class);
                                    intent.putExtra("exit", true);
                                    MainActivity.this.startActivity(intent);
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
                case R.id.pause_bt:
                    if(socket != null && socket.isConnected()) {
                        if(isPause){//已暂停，按钮显示的是“启动”
                            //pause_Btn.setText("暂停");
                            if(isChinese){
                                pauseBt.setText(getResources().getString(R.string.pause_cn));
                            }else{
                                pauseBt.setText(getResources().getString(R.string.pause_eg));
                            }
                            if(!isTTSEnd && mTts != null && !mTts.isSpeaking()){
                                mTts.resumeSpeaking();
                            }
                            //pauseBt.setBackgroundColor(Color.rgb(102,205,170));
                            requestPauseOrStart(false);//启动
                            isPause = false;
                        }else {//已启动状态，按钮显示的是“暂停”
                            //pause_Btn.setText("启动");
                            Config.isCurrentStepGoing = true;
                            if(isChinese){
                                pauseBt.setText(getResources().getString(R.string.start_cn));
                            }else{
                                pauseBt.setText(getResources().getString(R.string.start_eg));

                            }
                            if(mTts != null && mTts.isSpeaking()){
                                mTts.pauseSpeaking();
                                isTTSEnd = false;
                            }
                            //pauseBt.setBackgroundColor(Color.GREEN);
                            requestPauseOrStart(true);//暂停
                            isPause = true;
                        }
                    }else {
                        Toast.makeText(MainActivity.this,"与服务器的连接已断开，请先连接服务器！",Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

        }else {
            requestPermissions();
        }
    }

    public void requestPauseOrStart(boolean b){
        //if(sc01MsgVo != null){
            if(b){
                pauseStr = "CC02=[" + true + "]";
            }else {
                pauseStr = "CC02=[" + false + "]";
            }
            Log.d(TAG,"your pauseStr is: " + pauseStr);
            sendMsg(pauseStr);
        //}
    }

    private void startCountDownTime(final long time) {
        final CountDownTimer timer = new CountDownTimer(time * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                if(isSpeaked){
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
                Log.d("hqtest","result is: " + s);
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
                //Toast.makeText(MainActivity.this,"播放完成",Toast.LENGTH_SHORT).show();
                isTTSEnd = true;
                Log.d(TAG,"send cc07...");
                String message = CC07 + "=[]";
                sendMsg(message);

                switch (currentId){
                    case SC01:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                waitTime = sc01MsgVo.getWaitTime();
                                timeTv.setText(waitTime + "s");
                                isSpeaked =false;
                                startCountDownTime(waitTime);
                            }
                        });
                        if(sc01MsgVo.getCommandType() == 1){
                            Feedback();
                        }
                        break;
                    case SC06:
                        Feedback();
                        break;
                    default:
                        break;
                }
            } else if (speechError != null) {
                Config.isCurrentStepGoing = false;
                Toast.makeText(MainActivity.this,"播放失败",Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    public void Feedback(){
        if(socket != null && socket.isConnected()){
            STT();
        }else{
            Toast.makeText(MainActivity.this,"请连接服务端！",Toast.LENGTH_SHORT).show();
        }
    }

    public void TTS(String content){
        isTTSEnd = true;
        if(content != ""){
            //1.创建 SpeechSynthesizer 对象, 第二个参数:本地合成时传 InitListener
            mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this, null);
            //2.合成参数设置,详见《MSC Reference Manual》SpeechSynthesizer 类
            //设置发音人
            mTts.setParameter(SpeechConstant.LANGUAGE,"en_us");
            //mTts.setParameter(SpeechConstant.VAD_BOS, "xiaoyan");
            /*发音人：
            * xiaolin:中英文（台湾普通话）
            * catherine：英文
             */
            /*if(isChinese){
                mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
            }else{
                mTts.setParameter(SpeechConstant.VOICE_NAME, "catherine");
            }*/
            mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaolin");
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
            if (TextUtils.isEmpty(content)) {
                if(isChinese){
                    mTts.startSpeaking(getResources().getString(R.string.speek_null_cn), mTtsListener);
                }else {
                    mTts.startSpeaking(getResources().getString(R.string.speek_null_eg), mTtsListener);
                }
            }else {
                mTts.startSpeaking(content, mTtsListener);
            }
        }
    }

    public void STT(){
        showTv(resultTv,"");
        //1.创建RecognizerDialog对象
        final RecognizerDialog mDialog = new RecognizerDialog(MainActivity.this, new InitListener
                () {
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
                isSpeaked = true;
                submitToServer(recognizerResult);
            }

            @Override
            public void onError(SpeechError speechError) {
                isSpeaked = true;
                commitJSONtoServer("");
                mDialog.dismiss();//超时后，对话框自动消失
            }
        });
        // 4.显示dialog,接收语音输入
        mDialog.show();
    }

    private void submitToServer(com.iflytek.cloud.RecognizerResult results) {
        JsonParser jsonParser = new JsonParser();
        String text = jsonParser.parseIatResult(results.getResultString());
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            Log.d("heq","resultJson is: " + resultJson);
            sn = resultJson.optString("sn");
            Log.d("heq","sn is: " + sn);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);
        StringBuffer resultBuffer = new StringBuffer();
        Log.d("heq","mIatResults.keySet is: " + mIatResults.keySet());
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        String result = resultBuffer.toString();//最终的语音解析结果
        showTv(resultTv,result);
        commitJSONtoServer(result);
    }

    public void commitJSONtoServer(String str) {
        if(currentId.equals(SC01)) {
            if (sc01MsgVo != null) {
                String json = new Gson().toJson(new FeedbackBean(sc01MsgVo.getStepId(), str));
                feedbackStr = "CC01=[" + json + "]";
                Log.d(TAG, "your speak is: " + feedbackStr);
                sendMsg(feedbackStr);
            }
        }else if (currentId.equals(SC06)) {
            String feedStr = "";
            if(str.equals(FeedGoOn)){//继续
                feedStr = CC06 + "=[" + 1 + "]";
            }else if(str.equals(FeedWait)){//等待
                feedStr = CC06 + "=[" + 2 + "]";
            }else{//缺省值,超时或者除继续和等待外的回答
                feedStr = CC06 + "=[" + 2 + "]";
            }
            Log.d(TAG, "your feed is: " + feedbackStr);
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
                    Config.isCurrentStepGoing = false;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
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
                }catch (IOException e){

                }
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.putExtra("exit", true);
                MainActivity.this.startActivity(intent);
                System.exit(0);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if (null != myIntentFilterHome || null != myIntentFilter) {
            unregisterReceiver(mBroadcastReceiver);
        }*/
    }
}