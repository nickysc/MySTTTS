package com.baidu.speech.myapplication.BaiduST;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.speech.asr.SpeechConstant;
import com.baidu.speech.myapplication.R;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by ankeranker on 2019/5/23.
 */

public class STActivity extends STCommonActivity implements IStatus, View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private static final String TAG = "STActivity";

    private Button mBtn;
    private TextView mTvResult;
    private TextView mTvMsg;
    private RadioGroup mRgVad, mRgOffline;
    private RadioButton mRbDnn, mRbTouch, mRbOfflineTrue, mRbOfflineFalse;
    private EditText mEtPid;
    private Spinner mSpPid;
    /**
     * 识别控制器，使用MyRecognizer控制识别的流程
     */
    protected MyRecognizer myRecognizer;

    /*
     * 本Activity中是否需要调用离线命令词功能。根据此参数，判断是否需要调用SDK的ASR_KWS_LOAD_ENGINE事件
     */
    protected boolean enableOffline;


    /**
     * 控制UI按钮的状态
     */
    protected int status;


    @Override
    protected int getLayout() {
        return R.layout.activity_st;
    }

    @Override
    protected void handleMsg(Message msg) {
        if (mTvMsg != null && msg.obj != null) {
            mTvMsg.append(msg.obj.toString() + "\n");
        }
        switch (msg.what) { // 处理MessageStatusRecogListener中的状态回调
            case STATUS_FINISHED:
                if (msg.arg2 == 1) {
                    mTvResult.setText(msg.obj.toString());
                }
                status = msg.what;
                updateBtnTextByStatus();
                break;
            case STATUS_NONE:
            case STATUS_READY:
            case STATUS_SPEAKING:
            case STATUS_RECOGNITION:
                status = msg.what;
                updateBtnTextByStatus();
                break;
            default:
                break;

        }

    }


    @Override
    protected void initView() {
        status = STATUS_NONE;

        mBtn = findViewById(R.id.btn);
        mTvResult = findViewById(R.id.txtResult);
        mTvMsg = findViewById(R.id.txtLog);
        mRgVad = findViewById(R.id.rg_vad);
        mRbDnn = findViewById(R.id.rb_dnn);
        mRbTouch = findViewById(R.id.rb_touch);
        mEtPid = findViewById(R.id.et_pid);
        mRgOffline = findViewById(R.id.rg_offline);
        mRbOfflineTrue = findViewById(R.id.rb_0ffline_true);
        mRbOfflineFalse = findViewById(R.id.rb_0ffline_false);
        mSpPid = findViewById(R.id.sp_pid);

        mRgOffline.setOnCheckedChangeListener(this);


        mBtn.setOnClickListener(this);

        //状态监听，主要为识别过程中各个状态变化
        IRecogListener listener = new MessageStatusRecogListener(handler);
        //初始化：new一个IRecogListener示例 & new 一个 MyRecognizer 示例,并注册输出事件
        myRecognizer = new MyRecognizer(this, listener);
    }


    /**
     * 开始录音，点击“开始”按钮后调用。
     */
    protected void start() {
        final Map<String, Object> params = getParams();
        checkError(params);
        myRecognizer.start(params);
    }

    private void checkError(Map<String, Object> params) {
        // 复制此段可以自动检测常规错误
        (new AutoCheck(getApplicationContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
                        mTvMsg.append(message + "\n");
                        ; // 可以用下面一行替代，在logcat中查看代码
                        // Log.w("AutoCheckMessage", message);
                    }
                }
            }
        }, enableOffline)).checkAsr(params);
    }

    private Map<String, Object> getParams() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        if (enableOffline) {
            params.put(SpeechConstant.DECODER, 2);
        }
        // 基于SDK集成2.1 设置识别参数
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        // params.put(SpeechConstant.NLU, "enable");

        /*
            静音时长设置，作用是静音断句的时长设置，值建议800ms-3000ms可以调节此参数，0时表示长语音，长语音不能与VAD=touch联用
         */
        if (!mEtPid.getText().toString().isEmpty()){
            params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, Integer.valueOf(mEtPid.getText().toString()));
        }

        /*
            dnn：开启VAD，根据静音时长切分句子/touch：关闭VAD，60s内音频，SDK等待调用stop事件结束录音
         */
        if (mRbDnn.isChecked()){
            params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
        }else{
            params.put(SpeechConstant.VAD, SpeechConstant.VAD_TOUCH);
        }
        params.put(SpeechConstant.PID, Constant.mPid[mSpPid.getSelectedItemPosition()]); // 中文输入法模型，有逗号

        // params.put(SpeechConstant.IN_FILE, "res:///com/baidu/android/voicedemo/16k_test.pcm");


        /* 语音自训练平台特有参数 */
        // params.put(SpeechConstant.PID, 8002);
        // 语音自训练平台特殊pid，8002：搜索模型类似开放平台 1537  具体是8001还是8002，看自训练平台页面上的显示
        // params.put(SpeechConstant.LMID,1068); // 语音自训练平台已上线的模型ID，https://ai.baidu.com/smartasr/model
        // 注意模型ID必须在你的appId所在的百度账号下
        /* 语音自训练平台特有参数 */
        return params;
    }

    /**
     * 开始录音后，手动停止录音。SDK会识别在此过程中的录音。点击“停止”按钮后调用。
     */
    protected void stop() {
        myRecognizer.stop();
    }

    /**
     * 开始录音后，取消这次录音。SDK会取消本次识别，回到原始状态。点击“取消”按钮后调用。
     */
    protected void cancel() {
        myRecognizer.cancel();
    }

    /**
     * 销毁时需要释放识别资源。
     */
    @Override
    protected void onDestroy() {
        // 如果之前调用过myRecognizer.loadOfflineEngine()， release()里会自动调用释放离线资源
        // 卸载离线资源(离线时使用) release()方法中封装了卸载离线资源的过程
        // 退出事件管理器
        myRecognizer.release();
        Log.i(TAG, "onDestory");
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (status) {
            case STATUS_NONE: // 初始状态
                start();
                status = STATUS_WAITING_READY;
                updateBtnTextByStatus();
                mTvMsg.setText("");
                mTvResult.setText("");
                break;
            case STATUS_WAITING_READY: // 调用本类的start方法后，即输入START事件后，等待引擎准备完毕。
            case STATUS_READY: // 引擎准备完毕。
            case STATUS_SPEAKING: // 用户开始讲话
            case STATUS_FINISHED: // 一句话识别语音结束
            case STATUS_RECOGNITION: // 识别中
                stop();
                status = STATUS_STOPPED; // 引擎识别中
                updateBtnTextByStatus();
                break;
            case STATUS_LONG_SPEECH_FINISHED: // 长语音识别结束
            case STATUS_STOPPED: // 引擎识别中
                cancel();
                status = STATUS_NONE; // 识别结束，回到初始状态
                updateBtnTextByStatus();
                break;
            default:
                break;
        }
    }

    private void updateBtnTextByStatus() {
        switch (status) {
            case STATUS_NONE:
                mBtn.setText("开始录音");
                mBtn.setEnabled(true);
                break;
            case STATUS_WAITING_READY:
            case STATUS_READY:
            case STATUS_SPEAKING:
            case STATUS_RECOGNITION:
                mBtn.setText("停止录音");
                mBtn.setEnabled(true);

                break;
            case STATUS_LONG_SPEECH_FINISHED:
            case STATUS_STOPPED:
                mBtn.setText("取消整个识别过程");
                mBtn.setEnabled(true);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_0ffline_false:
                enableOffline = false;
                break;
            case R.id.rb_0ffline_true:
                enableOffline = true;
                break;
            default:
                break;
        }
    }
}
