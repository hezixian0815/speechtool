package com.hzx.ui;


import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONUtil;
import com.aispeech.export.Command;
import com.aispeech.export.DmInfo;
import com.aispeech.export.MultiModal;
import com.aispeech.export.NativeApi;
import com.aispeech.export.Speaker;
import com.aispeech.export.config.AICloudDMConfig;
import com.aispeech.export.config.AILocalTTSConfig;
import com.aispeech.export.config.AIWakeupConfig;
import com.aispeech.export.engines2.AICloudDMEngine;
import com.aispeech.export.engines2.AILocalTTSEngine;
import com.aispeech.export.engines2.AIWakeupEngine;
import com.aispeech.export.intent.AICloudDMIntent;
import com.aispeech.export.intent.AILocalTTSIntent;
import com.aispeech.export.intent.AIWakeupIntent;
import com.aispeech.export.listeners.AICloudDMListener;
import com.aispeech.export.listeners.AITTSListener;
import com.aispeech.export.listeners.AIWakeupListener;
import com.aispeech.export.widget.callback.CallbackWidget;
import com.aispeech.export.widget.callback.CallbackWidgetType;

import com.aispeech.export.widget.feedback.ContentFeedbackWidget;
import com.aispeech.export.widget.feedback.ListFeedbackWidget;
import com.aispeech.export.widget.feedback.TextFeedbackWidget;
import com.aispeech.lite.AIType;
import com.aispeech.lite.oneshot.OneshotCache;
import com.hzx.speechtool.R;
import com.hzx.util.SampleConstants;

import org.json.JSONException;
import org.json.JSONObject;

public class CloudDmActivity extends AppCompatActivity implements View.OnClickListener {
    final String TAG = "AI_CloudDM";

    private AICloudDMEngine mDmEngine;
    private Button btn_stop,btn_start;
    private TextView tv_dmAsr,tv_dmResult;
    private AICloudDMIntent cloudDMIntent;
    private AILocalTTSIntent ttsIntent;

    private AILocalTTSEngine mTTSEngine;
    private AIWakeupEngine mWakeupEngine;

    private Toast mToast;

    boolean isRunning = false;



    /**
     * 是否处于合成中
     */
    private volatile boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_dm);

        initView();
        initCloudDmIntent();

        initEngine();
        initTtsEngine();
//        initWakeupEngine();
    }

    private void initWakeupEngine() {
        mWakeupEngine = AIWakeupEngine.createInstance();
        AIWakeupConfig config = new AIWakeupConfig();
        config.setWakeupResource(SampleConstants.WAKEUP_RES); // SampleConstants.WAKEUP_RES
        config.setOneShotCacheTime(1000);
        mWakeupEngine.init(config, new AIWakeupListener() {
            @Override
            public void onWakeup(String recordId, double confidence, String wakeupWord) {
                Log.i(TAG, "---onWakeup() recordId:" + recordId + ",confidence=" + confidence + ",wakeupWord=" + wakeupWord);
                //那么就会直接在唤醒引擎的onResult回调中输出结果，如果检测到不是连说（只有唤醒词），那么会回调onNotOneShot(),用户可以在onNotOneShot()兼容老版本的内容
                if (mDmEngine != null) {
                    mDmEngine.stop();
                }
            }

            @Override
            public void onPreWakeup(String recordId, double confidence, String wakeupWord) {

            }

            @Override
            public void onVprintCutDataReceived(int dataType, byte[] data, int size) {

            }

            @Override
            public void onResultDataReceived(byte[] buffer, int size) {

            }

            @Override
            public void onRawWakeupDataReceived(byte[] wkpData, int length) {

            }

            @Override
            public void onOneshot(String word, OneshotCache<byte[]> buffer) {
                Log.d(TAG, "---onOneshot() buffer : " + buffer.size() + " , isValid:" + buffer.isValid());

                isRunning = !isRunning;
                btn_start.setText(isRunning ? "停止对话" : "开始对话");
                if (isRunning) {
                    tv_dmResult.setText("");
                    AICloudDMIntent intent = new AICloudDMIntent();
                    intent.setWakeupWords(new String[]{"你好小乐"});
                    intent.setOneshotCache(buffer.clone());
                    intent.setEnableNumberConvert(true);
                    intent.setAIType(AIType.DM);
                    mDmEngine.start(intent);
                    if (isPlaying) {
                        mTTSEngine.stop();
                    }
                } else {
                    if (isPlaying) {
                        mTTSEngine.stop();
                    }
                    mDmEngine.stop();
                }
            }

            @Override
            public void onNotOneshot(String word) {
                //非oneshot交互模式，播报提示音后启动识别
                Log.d(TAG, "---onNotOneshot() word : " + word);
            }

            @Override
            public void onReadyForSpeech() {

            }

            @Override
            public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

            }

            @Override
            public void onRawDataReceived(byte[] buffer, int size) {

            }

            @Override
            public void onInit(int status) {
                if (status == AIConstant.OPT_SUCCESS) {
                    Log.i(TAG, "---onInit() 唤醒引擎初始化成功! ");
//                    startWakeup();
                } else {
                    Log.i(TAG, "---onInit() 初始化失败!code:" + status);
                }
            }

            @Override
            public void onError(AIError error) {
                Log.i(TAG, "---onError() error:" + error.toString());
            }
        });
    }

    private String[] mBackResBinArray = new String[]{SampleConstants.TTS_BACK_RES_CHUXIF, SampleConstants.TTS_BACK_RES_ZHILING,
            SampleConstants.TTS_BACK_RES_LUCY, SampleConstants.TTS_BACK_RES_XIJUN,
    };
    private String[] mBackResBinMd5sumArray = new String[]{SampleConstants.TTS_BACK_RES_CHUXIF_MD5, SampleConstants.TTS_BACK_RES_ZHILING_MD5,
            SampleConstants.TTS_BACK_RES_LUCY_MD5, SampleConstants.TTS_BACK_RES_XIJUN_MD5,
    };
    private void initTtsEngine() {
        mTTSEngine = AILocalTTSEngine.createInstance();
        AILocalTTSConfig ttsConfig = new AILocalTTSConfig();
        // 设置assets目录下合成字典名
        ttsConfig.setDictResource(SampleConstants.TTS_DICT_RES);//普通话字典
        // 设置assets目录下前端合成资源名
        ttsConfig.setFrontBinResource(SampleConstants.TTS_FRONT_RES);//普通话前端资源
        ttsConfig.setUseCache(false);
        //设置后端合成音色资源，如果只需设置一个，则array只需要传一个成员值就可以
        ttsConfig.addSpeakerResource(mBackResBinArray, mBackResBinMd5sumArray);
        mTTSEngine.init(ttsConfig, new AITTSListener() {
            @Override
            public void onInit(int status) {
                Log.i(TAG, "初始化完成，status：" + status);
            }

            @Override
            public void onError(String utteranceId, AIError error) {

            }

            @Override
            public void onReady(String utteranceId) {
                isPlaying = true;
                Log.i(TAG, "TTS开始播放");
                if (mDmEngine != null) {
                    syncPlayerState("on");
                }
            }

            @Override
            public void onCompletion(String utteranceId) {

            }

            @Override
            public void onProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished) {

            }

            @Override
            public void onSynthesizeStart(String utteranceId) {

            }

            @Override
            public void onSynthesizeDataArrived(String utteranceId, byte[] audioData) {
                //Log.d(Tag, "合成pcm音频数据:" + audioData.length);
                //正常合成结束后会收到size大小为0的audioData,即audioData.length == 0。应用层可以根据该标志停止播放
                //若合成过程中取消(stop或release)，则不会收到该结束标志
            }

            @Override
            public void onSynthesizeFinish(String utteranceId) {
                Log.d(TAG, "TTS合成完成");

            }

            @Override
            public void onTimestampReceived(byte[] timeStampJson, int size) {

            }

            @Override
            public void onPhonemesDataArrived(String utteranceId, String phonemes) {

            }
        });

    }

    private void initCloudDmIntent() {
        cloudDMIntent = new AICloudDMIntent();
        //对话模式
        cloudDMIntent.setAIType(AIType.DM);
        //设置识别引擎的资源类型,默认为aicar
        cloudDMIntent.setRes("comm");
        //开启识别结果中文转阿拉伯数字
        cloudDMIntent.setEnableNumberConvert(true);
    }

    private AILocalTTSIntent ttsIntent() {
        if (ttsIntent == null) {
            ttsIntent = new AILocalTTSIntent();
            // 设置合成音语速，范围为0.5～2.0
            ttsIntent.setSpeed(0.85f);

            ttsIntent.setUseSSML(false); // 设置是否使用ssml合成语法，默认为false
            ttsIntent.setVolume(100);    // 设置合成音频的音量，范围为1～500
            ttsIntent.setLmargin(10);    //设置头部静音段，范围5-20
            ttsIntent.setRmargin(10);    //设置尾部静音段，范围5-20
            ttsIntent.setUseTimeStamp(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ttsIntent.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                ttsIntent.setStreamType(AudioManager.STREAM_MUSIC);//设置合成音播放的音频流,默认为音乐流
            }
        }
        return ttsIntent;
    }

    private void initEngine() {
        mDmEngine = AICloudDMEngine.createInstance();
        AICloudDMConfig.Builder builder = new AICloudDMConfig.Builder();
        AICloudDMConfig config = builder.setConnectTimeout(3000)
                .setAliasKey("prod")
//                .setUseVad(true)
//                .setVadRes(SampleConstants.VAD_RES)
                .setUseFullDuplex(true)//开启全双工
                .setUseFullDuplexNoSpeechTimeOut(true) //是否在全双工模式下未检测到语音超时反馈语播报
                .build();
        mDmEngine.init(config, new AICloudDMListener() {
            @Override
            public void onQuery(NativeApi api) {
                Log.e(TAG, "NativeApi: " + api.toString());
                Log.i(TAG, "onQuery: " + api.toString());
                processApis(api.getApi());
            }

            @Override
            public void onCall(Command command) {
                Log.e(TAG, "Command : " + command.toString());
            }

            @Override
            public void onPlay(Speaker speaker) {
                Log.e(TAG, "Speak: " + speaker.toString());
                //关闭对话引擎
                if (mDmEngine != null) {
                    mDmEngine.stop();
                }
                tv_dmResult.setText(speaker.getNlg());
                if (mTTSEngine != null) {
                    mTTSEngine.speak(ttsIntent(), speaker.getNlg(), "1024");
                }

            }

            @Override
            public void onDisplay(CallbackWidgetType type, CallbackWidget callbackWidget) {

            }

            @Override
            public void onAsr(boolean isLast, String asr) {
                Log.d(TAG, "onAsr() called with: isLast = [" + isLast + "], asr = [" + asr + "]");
                boolean hasSemetic = false;
                JSONObject object = JSONUtil.build(asr);
                if (object != null) {
                    hasSemetic = object.has("dm");
                }

                if (!hasSemetic) {
                    //非语义识别部分
                    if (isLast) {
//                        show("\ninput: " + asr);
                        Log.d(TAG, "onAsr :" + asr);
                        try {
                            new JSONObject(asr);
                        } catch (JSONException e) {
                            tv_dmAsr.setText(asr);
                        }
                    } else {
                        tv_dmAsr.setText(asr);
                    }
                }

                if (hasSemetic) {
                    //语义部分
                    show("\n semantics: " + asr);
                }
            }

            @Override
            public void onEnd(String sessionId) {
                Log.i(TAG, "onEnd: \\n对话结束!\\nsessionId:\" + sessionId + \"\\n---------------------------------------------------------------");
                isRunning = false;
                btn_start.setText("开始对话");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                Log.d(TAG, "onRmsChanged :" + rmsdB);
//        showTip("RmsDB = " + rmsdB);
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech");

            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech");

            }

            @Override
            public void onDmResult(DmInfo dmInfo) {
                if (dmInfo.isEndSkillDm()) {// endSkillDm = true 后，需设置云端 filterSwitch = on
                    //updateFilterSwitch(true);
                    show("endSkillDm: " + true + "，sync filterSwitch on\n");
//                    setRejectAfterDispatch(true); //关闭语义拒识开关(调度后过滤)
                }
            }

            @Override
            public void onConnect(boolean isConnected) {
                Log.i(TAG, "onConnect: " + isConnected);
            }

            @Override
            public void onReadyForSpeech() {
                Log.d(TAG, "onReadyForSpeech");
                tv_dmAsr.setText("请讲话...");

                if (isPlaying) {
                    mTTSEngine.stop();
                }
            }

            @Override
            public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

            }

            @Override
            public void onRawDataReceived(byte[] buffer, int size) {

            }

            @Override
            public void onInit(int status) {

            }

            @Override
            public void onError(AIError error) {

            }
        });
    }

    private void initView() {
        tv_dmAsr = findViewById(R.id.tv_dmAsr);
        tv_dmResult = findViewById(R.id.tv_dmResult);
        btn_stop = findViewById(R.id.btn_stopDm);
        btn_start = findViewById(R.id.btn_startDm);
        btn_stop.setOnClickListener(this);
        btn_start.setOnClickListener(this);

    }


    @Override
    public void onClick(View view) {

        if (view == btn_start) {
//            mDmEngine.start(cloudDMIntent);
            isRunning = !isRunning;
//            mEngine.start(getCloudDMIntent());
            btn_start.setText(isRunning ? "停止对话" : "开始对话");
            if (isRunning) {


                tv_dmResult.setText("");
                mDmEngine.start(cloudDMIntent);


                if (isPlaying) {
                    if (mTTSEngine != null) {
                        mTTSEngine.stop();
                    }
                }
            } else {
                if (isPlaying) {
                    if (mTTSEngine != null) {
                        mTTSEngine.stop();
                    }
                }
                mDmEngine.stop();
            }
        } else if (view == btn_stop) {

        }

    }

    /**
     * 同步播放状态
     *
     * @param playerState on/off
     */
    private void syncPlayerState(String playerState) {
        MultiModal mModal = new MultiModal();
        mModal.setPlayerState(playerState);
        mDmEngine.async(mModal);
    }

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    private StringBuffer contentStringBuffer;


    /**
     * 显示文本,在主线程中调用
     *
     * @param content
     */
    private void show(final String content) {
        Log.i(TAG, "---show() content:" + content);
        tv_dmResult.post(new Runnable() {
            @Override
            public void run() {
                if (contentStringBuffer == null) {
                    contentStringBuffer = new StringBuffer(content + "\n");
                } else {
                    contentStringBuffer.insert(0, content + "\n");
                }
                tv_dmResult.setText(contentStringBuffer.toString());
            }
        });

    }


    private static final String ILEJA_NAVI_ADDRESS_GET = "com.ileja.navi.address.get";

    private static final String ALARM_API_SET = "alarm.api.set";

    private static final String WAKEUP_QUERY = "speech.wakeup.word.query";

    private static final String INTERRUPT = "interrupt";//全双工打断

    private void processApis(String api) {
        switch (api) {
            case ILEJA_NAVI_ADDRESS_GET:
                if (mDmEngine != null) {
                    mDmEngine.feedback(new ListFeedbackWidget()
                            .addContentWidget(new ContentFeedbackWidget().setTitle("世界之窗1号").setSubTitle("世界之窗"))
                            .addContentWidget(new ContentFeedbackWidget().setTitle("世界之窗2号").setSubTitle("广东省深圳市深圳湾社区深南大道"))
                    );
                }
                break;
            case ALARM_API_SET:
                if (mDmEngine != null) {
                    mDmEngine.feedback(new TextFeedbackWidget());
                }
                break;
            case WAKEUP_QUERY:
                if (mDmEngine != null) {
                    mDmEngine.feedback(new TextFeedbackWidget().setText("我叫小驰"));
                }
                break;
            case INTERRUPT:
                Log.d(TAG, "interrupt .....");
                if (mTTSEngine != null) {
                    mTTSEngine.stop();
                }
                break;
            default:
                Log.e(TAG, "not included api : " + api);
                break;
        }
    }

    private void startWakeup() {
        AIWakeupIntent intent = new AIWakeupIntent();
        // 唤醒词支持 中文拼音、英文
        intent.setWakeupWord(new String[]{"ni hao xiao he"}, new float[]{0.1f});
        mWakeupEngine.start(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDmEngine != null)
            mDmEngine.stop();
        if (mWakeupEngine != null) {
            mWakeupEngine.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWakeupEngine != null) {
            mWakeupEngine.destroy();
            mWakeupEngine = null;
        }
        if (mDmEngine != null) {
            mDmEngine.destroy();
            mDmEngine = null;
        }

        if (mTTSEngine != null) {
            mTTSEngine.destroy();
            mTTSEngine = null;
        }

    }
}