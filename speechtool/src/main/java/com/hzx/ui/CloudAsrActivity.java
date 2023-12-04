package com.hzx.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONResultParser;
import com.aispeech.export.config.AICloudASRConfig;
import com.aispeech.export.config.AIFespCarConfig;
import com.aispeech.export.engines2.AICloudASREngine;
import com.aispeech.export.engines2.AIFespCarEngine;
import com.aispeech.export.intent.AICloudASRIntent;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AIFespCarListener;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.sspe.SspeConstant;
import com.hzx.speechtool.R;

import java.util.ArrayList;
import java.util.List;

public class CloudAsrActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "CloudAsr";

    private TextView tv_asrResult;
    private Button btn_cloud_asr_start,btn_stop;
    private AICloudASREngine mAsrEngine;

    Toast mToast;
    private AICloudASRIntent aiCloudASRIntent;
    private AIFespCarEngine aiFespCarEngine;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_asr);
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);


        initView();
//        initSspeEngine();
        initCloudASREngine();
        initCloudEngineIntent();
    }

    /**
     * 初始化前端信号处理引擎
     */
    private void initSspeEngine() {
        aiFespCarEngine = AIFespCarEngine.getInstance();
        AIFespCarConfig.Builder build = new AIFespCarConfig.Builder();
        build.setSspeType(SspeConstant.SSPE_TYPE_CAR_FOUR);
        AIFespCarConfig config = build.create();
        config.setSspeType(4);
        config.setSspeResource("");
        aiFespCarEngine.init(config, new AIFespCarListener() {
            @Override
            public void onInit(int status) {

            }

            @Override
            public void onError(AIError error) {

            }

            @Override
            public void onWakeup(String recordId, double confidence, String wakeupWord) {

            }

            @Override
            public void onInterceptWakeup(int doa, double confidence, String wakeupWord) {

            }

            @Override
            public void onSubWordWakeup(double confidence, String wakeupWord) {

            }

            @Override
            public void onDoaResult(int doa) {

            }

            @Override
            public void onDoaResult(int doa, int type) {

            }

            @Override
            public void onReadyForSpeech() {

            }

            @Override
            public void onRawDataReceived(byte[] buffer, int size) {

            }

            @Override
            public void onResultDataReceived(byte[] buffer, int size, int wakeup_type) {

            }

            @Override
            public void onResultDataReceived(byte[] vad, byte[] asr) {

            }

            @Override
            public void onResultDataReceived(byte[] buffer, boolean isUseDoubleVad) {

            }

            @Override
            public void onVprintCutDataReceived(int dataType, byte[] data, int size) {

            }

            @Override
            public void onOneshot(String word, OneshotCache<byte[]> buffer) {

            }

            @Override
            public void onNotOneshot(String word) {

            }
        });

    }


    private void initCloudEngineIntent() {
        aiCloudASRIntent = new AICloudASRIntent();
        aiCloudASRIntent.setEnablePunctuation(false);//设置是否启用标点符号识别,默认为false关闭
        aiCloudASRIntent.setResourceType("aihome");//设置识别引擎的资源类型,默认为comm
        aiCloudASRIntent.setCloudVadEnable(false);
        aiCloudASRIntent.setEnableNumberConvert(true);//设置启用识别结果汉字数字转阿拉伯数字功能
        aiCloudASRIntent.setEnableSNTime(true);//设置rec结果增加对齐信息接口
        aiCloudASRIntent.setCloudVadEnable(true);//设置是否开启服务端的vad功能,默认开启为true
        aiCloudASRIntent.setIgnoreEmptyResult(true, 3);
//        aICloudASRIntent.setEnableTone(false);//设置音调功能接口
//        aICloudASRIntent.setEnableLanguageClassifier(false);//设置语言分类功能接口
            /* 设置 lmList 后需要等待更多时间，setWaitingTimeout 设置更长些
            String[] lmListValues = new String[]{"abc", "de"};数组信息
            aiCloudASRIntent.setLmList(lmListValues)*/

        List<String> customWakeupWord = new ArrayList<>();
        customWakeupWord.add("你好晓乐");
        // 设置自定义唤醒词，是否过滤唤醒词，默认为false，不过滤
        aiCloudASRIntent.setCustomWakeupWord(customWakeupWord);
        // 设置自定义唤醒词，是否过滤唤醒词，默认为false，不过滤
        // aICloudASRIntent.setCustomWakeupWord(customWakeupWord,true)

        aiCloudASRIntent.setEnableAlignment(true);
        aiCloudASRIntent.setPauseTime(500);
        aiCloudASRIntent.setWaitingTimeout(5000);//设置等待识别结果超时时长，默认5000ms
        aiCloudASRIntent.setNoSpeechTimeOut(0);
        //保存的音频路径,格式为.ogg  是否保存所有的原始音频，默认false
//            aiCloudASRIntent.setSaveAudioPath("/sdcard/aispeech", false);
        // aICloudASRIntent.setMaxSpeechTimeS(0);//音频最大录音时长
        aiCloudASRIntent.setAudioType(AICloudASRIntent.PCM_ENCODE_TYPE.OGG);
        aiCloudASRIntent.setEnableFirstDec(true);
        aiCloudASRIntent.setEnableFirstDecForce(true);
        // aiCloudASRIntent.setEnableEmotion(false);    // 是否开启情感识别

        // 设置额外参数，除了基本数据类型、简单的对象外，复杂对象请设置为 JSONObject 类型
        // aiCloudASRIntent.putExtraParam("s3d", "eg24")
        //敏感词过滤，默认开启
        aiCloudASRIntent.setEnableSensitiveWdsNorm(true);
        aiCloudASRIntent.setEnableRecUppercase(false);
//            aiCloudASRIntent.setPhrasesList(new Phrase[]{new Phrase(new String[]{"跟新", "建康"})});
        aiCloudASRIntent.setRealback(true);
        aiCloudASRIntent.setEnableRealBackFastend(true);
        aiCloudASRIntent.setEnableTxtSmooth(false);
    }

    private void initCloudASREngine() {
        mAsrEngine = AICloudASREngine.createInstance();
        AICloudASRConfig aiCloudASRConfig = new AICloudASRConfig();
        aiCloudASRConfig.setLocalVadEnable(true);
        aiCloudASRConfig.setVadResource("vad/vad_aihome_v0.11.bin");
        mAsrEngine.init(aiCloudASRConfig, new AIASRListener() {
            @Override
            public void onInit(int status) {
                Log.i(TAG, "onInit: " + status);
            }

            @Override
            public void onError(AIError error) {
                Log.i(TAG, "onError: " + error.toString());
            }

            @Override
            public void onResults(AIResult result) {
                Log.i(TAG, "onResults: " + result.toString());
                if (result.isLast() && result.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                    String recordId = result.getRecordId();
                    Log.i(TAG, "recordId = " + recordId);
                    Log.i(TAG, "result JSON = " + result.getResultObject().toString());
                    // 可以使用JSONResultParser来解析识别结果
                    // 结果按概率由大到小排序
                    JSONResultParser parser = new JSONResultParser(result.getResultObject()
                            .toString());
                    tv_asrResult.append("识别结果为 :  " + parser.getText() + "\n");
                    tv_asrResult.append("AIResult为 :  " + result.getResultObject().toString());
                }
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                showTip("RmsDB = " + rmsdB);
            }

            @Override
            public void onBeginningOfSpeech() {
                tv_asrResult.append("检测到说话...\n");
            }

            @Override
            public void onEndOfSpeech() {
                tv_asrResult.append("检测到语音停止，开始识别...\n");
            }

            @Override
            public void onRawDataReceived(byte[] buffer, int size) {

            }

            @Override
            public void onResultDataReceived(byte[] buffer, int size) {

            }

            @Override
            public void onNotOneShot() {

            }

            @Override
            public void onReadyForSpeech() {
                Log.i(TAG, "onReadyForSpeech: " + "准备说话");
                tv_asrResult.setText("请说话...\n");
            }

            @Override
            public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

            }
        });
    }

    private void initView() {
        tv_asrResult = findViewById(R.id.tv_asr_result);
        btn_cloud_asr_start = findViewById(R.id.btn_startAsr);
        btn_stop = findViewById(R.id.btn_stop);

        btn_cloud_asr_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);

    }



    @Override
    public void onClick(View v) {
        if (v == btn_cloud_asr_start) {
            mAsrEngine.start(aiCloudASRIntent);
        }else if (v == btn_stop) {
            mAsrEngine.cancel();
            tv_asrResult.setText("已取消");
        }
    }

    public void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }
}