package com.aispeech.lite.semantic;

import android.text.TextUtils;

import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.Log;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.FileSaveScenes;

import java.nio.charset.Charset;

/**
 * Description: 保存语义结果，目前仅保存BCDV2 https://wiki.aispeech.com.cn/pages/viewpage.action?pageId=150798943
 * Author: junlong.huang
 * CreateTime: 2023/2/9
 */
public class SemanticDataHelper {

    private String TAG = "SemanticDataHelper";
    private final String NEW_LINE = System.getProperty("line.separator");
    private FileSaveUtil bcdv2InputSaveUtil;
    private FileSaveUtil bcdv2OriginalOutputSaveUtil;
    private FileSaveUtil bcdv2PassDiscardSaveUtil;
    private FileSaveUtil bcdv2FinalOutputSaveUtil;

    public void init() {

        if (FileSaveScenes.isSavingFile(FileSaveScenes.LOCAL_SEMANTIC_BCDV2)) {

            String savePath = AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH;
            if (TextUtils.isEmpty(savePath)) {
                savePath = AISpeech.getContext().getFilesDir().getAbsolutePath();
            }
            Log.i(TAG, "savePath: " + savePath);
            bcdv2InputSaveUtil = new FileSaveUtil(FileSaveUtil.FILE_TYPE_TXT);
            bcdv2OriginalOutputSaveUtil = new FileSaveUtil(FileSaveUtil.FILE_TYPE_TXT);
            bcdv2PassDiscardSaveUtil = new FileSaveUtil(FileSaveUtil.FILE_TYPE_TXT);
            bcdv2FinalOutputSaveUtil = new FileSaveUtil(FileSaveUtil.FILE_TYPE_TXT);

            bcdv2InputSaveUtil.init(savePath, "semanticIO");
            bcdv2OriginalOutputSaveUtil.init(savePath, "semanticIO");
            bcdv2PassDiscardSaveUtil.init(savePath, "semanticIO");
            bcdv2FinalOutputSaveUtil.init(savePath, "semanticIO");

            bcdv2InputSaveUtil.prepare("bcd_input");
            bcdv2OriginalOutputSaveUtil.prepare("bcd_original_output");
            bcdv2PassDiscardSaveUtil.prepare("bcd_pass_discard_output");
            bcdv2FinalOutputSaveUtil.prepare("bcd_final_output");
        }


    }

    /**
     * 原始输入
     *
     * @param refText
     */
    public void saveBCDV2RefText(String refText) {
        if (bcdv2InputSaveUtil == null) {
            init();
        }

        if (bcdv2InputSaveUtil != null) {
            refText = refText + NEW_LINE;
            bcdv2InputSaveUtil.feedTypeCustom(refText.getBytes(Charset.forName("UTF-8")));
            bcdv2InputSaveUtil.flush();
        }
    }

    /**
     * 内核原始抛出结果
     *
     * @param result
     */
    public void saveBCDV2OriginalOutput(String result) {
        if (bcdv2OriginalOutputSaveUtil != null) {
            result = result + NEW_LINE;
            bcdv2OriginalOutputSaveUtil.feedTypeCustom(result.getBytes(Charset.forName("UTF-8")));
            bcdv2OriginalOutputSaveUtil.flush();
        }
    }

    /**
     * 打开了拒识并且通过了的结果
     *
     * @param result
     */
    public void saveBCDV2PassDiscard(String result) {
        if (bcdv2PassDiscardSaveUtil != null) {
            result = result + NEW_LINE;
            bcdv2PassDiscardSaveUtil.feedTypeCustom(result.getBytes(Charset.forName("UTF-8")));
            bcdv2PassDiscardSaveUtil.flush();
        }
    }

    /**
     * 对外抛出的结果
     *
     * @param result
     */
    public void saveBCDV2FinalOutput(String result) {
        if (bcdv2FinalOutputSaveUtil != null) {
            result = result + NEW_LINE;
            bcdv2FinalOutputSaveUtil.feedTypeCustom(result.getBytes(Charset.forName("UTF-8")));
            bcdv2FinalOutputSaveUtil.flush();
        }
    }

    public void release() {
        Log.i(TAG, "release");

        if (bcdv2InputSaveUtil != null) {
            bcdv2InputSaveUtil.close();
            bcdv2InputSaveUtil = null;
        }

        if (bcdv2OriginalOutputSaveUtil != null) {
            bcdv2OriginalOutputSaveUtil.close();
            bcdv2OriginalOutputSaveUtil = null;
        }

        if (bcdv2PassDiscardSaveUtil != null) {
            bcdv2PassDiscardSaveUtil.close();
            bcdv2PassDiscardSaveUtil = null;
        }

        if (bcdv2FinalOutputSaveUtil != null) {
            bcdv2FinalOutputSaveUtil.close();
            bcdv2FinalOutputSaveUtil = null;
        }
    }


}
