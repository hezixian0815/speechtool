package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.JSONUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wanbing on 2021/9/6 12:19
 *
 * @deprecated use LocalSignalProcessingConfig
 */
@Deprecated
public class FespxCarConfig extends AIEngineConfig {
    public static final String KEY_AEC_BIN_PTAH = "aecBinPath";
    public static final String KEY_WAKUP_BIN_PATH = "wakeupBinPath";
    public static final String KEY_BEAMFORMING_BIN_PATH = "beamformingBinPath";
    public static final String KEY_ENV = "env";
    public static final String KEY_WORD = "words";
    public static final String KEY_THRESH = "thresh";
    public static final String KEY_THRESH_2 = "thresh2";
    public static final String KEY_MAJOR = "major";
    public static final String KEY_DCHECK = "dcheck";
    public static final String KEY_MAX_VOLUME = "maxVolume";
    public static final Object KEY_RANGES = "ranges";

    /**
     * "aecBinPath": "./third/res/fespl/aec.bin",
     * "wakeupBinPath": "./third/res/fespl/wakeup_aihome_tcent_ed_20170704_8bit.bin",
     * "beamformingBinPath ": ". / third / res / fespl / fend.bin ",
     * "env ": " words = xiao le;thresh = 0.05;major = 1;",
     */

    protected JSONObject jsonObject;
    private String aecBinPath = "";
    private String wakupBinPath = "";
    private String beamformingBinPath = "";
    private int rollBack = 0;
    private String[] wakupWords;
    private float[] threshs;
    private float[] threshs2;
    private int[] majors;
    private int[] dchecks;
    private int[] ranges; //定位模式下唤醒切换音区，"1"表示切换，"0"表示不切换，如 [1,0,0]
    private boolean vad = true;
    private String env;
    private int boundary = -1;//开启/关闭声纹唤醒词截断，0:关闭，1：开启
    private int maxVolume = -1;
    // sspe 资源路径
    private String sspeBinPath;
    /**
     * oneshot 配置
     */
    private OneshotConfig oneshotConfig;

    public OneshotConfig getOneshotConfig() {
        return oneshotConfig;
    }

    public void setOneshotConfig(OneshotConfig oneshotConfig) {
        this.oneshotConfig = oneshotConfig;
    }

    private static String getSoFileName(String libraryName) {
        return "lib" + libraryName + ".so";
    }


    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public int getMaxVolume() {
        return maxVolume;
    }

    public void setMaxVolume(int maxVolume) {
        this.maxVolume = maxVolume;
    }

    public boolean isVad() {
        return vad;
    }

    public void setVad(boolean vad) {
        this.vad = vad;
    }

    public int getBoundary() {
        return boundary;
    }

    public void setBoundary(int boundary) {
        this.boundary = boundary;
    }

    public String getAecBinPath() {
        return aecBinPath;
    }

    public void setAecBinPath(String aecBinPath) {
        this.aecBinPath = aecBinPath;
    }

    public String getWakupBinPath() {
        return wakupBinPath;
    }

    public void setWakupBinPath(String wakupBinPath) {
        this.wakupBinPath = wakupBinPath;
    }

    public String getBeamformingBinPath() {
        return beamformingBinPath;
    }

    public void setBeamformingBinPath(String beamformingBinPath) {
        this.beamformingBinPath = beamformingBinPath;
    }


    public String[] getWakupWords() {
        return wakupWords;
    }

    public void setWakupWords(String[] wakupWords) {
        this.wakupWords = wakupWords;
    }

    public float[] getThreshs() {
        return threshs;
    }

    public void setThreshs(float[] threshs) {
        this.threshs = threshs;
    }

    public void setThreshs2(float[] threshs2) {
        this.threshs2 = threshs2;
    }

    public int[] getMajors() {
        return majors;
    }

    public void setMajors(int[] majors) {
        this.majors = majors;
    }

    public void setDchecks(int[] dchecks) {
        this.dchecks = dchecks;
    }

    public int[] getRanges() {
        return ranges;
    }

    public void setRanges(int[] ranges) {
        this.ranges = ranges;
    }


    public List<String> getRangesWords() {
        if (ranges != null && wakupWords != null && ranges.length == wakupWords.length) {
            List<String> rangesWords = new ArrayList<>();
            for (int i = 0; i < ranges.length; i++) {
                if (ranges[i] == 0) {
                    rangesWords.add(wakupWords[i]);
                }
            }
            return rangesWords;
        }
        return null;
    }


    private String toEnv() {
        StringBuilder envSb = new StringBuilder();
        appendWords(envSb);
        appendThreshs(envSb);

        // 是否开启大音量检测判断
        appendThresh2(envSb);
        appendMajors(envSb);

        if (dchecks != null && dchecks.length > 0) {
            StringBuilder dcheckSb = new StringBuilder();
            for (int i = 0; i < dchecks.length; i++) {
                dcheckSb.append(dchecks[i]);
                if (i != dchecks.length - 1) {
                    dcheckSb.append(",");
                }
            }
            envSb.append(KEY_DCHECK + "=" + dcheckSb.toString() + ";");
        }

        envSb.append("vad=").append(vad ? "1" : "0").append(";");
        if (boundary != -1) {//不设置默认不加此参数，避免sspe内核报错
            envSb.append("boundary=").append(boundary >= 1 ? "1" : "0").append(";");
        }
        return envSb.toString();
    }

    private void appendMajors(StringBuilder envSb) {
        if (majors != null && majors.length > 0) {
            StringBuilder majorSb = new StringBuilder();
            for (int i = 0; i < majors.length; i++) {
                majorSb.append(majors[i]);
                if (i != majors.length - 1) {
                    majorSb.append(",");
                }
            }
            envSb.append(KEY_MAJOR + "=" + majorSb.toString() + ";");
        }
    }

    private void appendThresh2(StringBuilder envSb) {
        if (threshs2 != null && threshs2.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < threshs2.length; i++) {
                sb.append("" + threshs2[i]);
                if (i != threshs2.length - 1) {
                    sb.append(",");
                }
            }
            envSb.append(KEY_THRESH_2 + "=" + sb + ";");
        }
    }

    private void appendThreshs(StringBuilder envSb) {
        if (threshs != null && threshs.length > 0) {
            StringBuilder threshSb = new StringBuilder();
            for (int i = 0; i < threshs.length; i++) {
                threshSb.append(threshs[i]);
                if (i != threshs.length - 1) {
                    threshSb.append(",");
                }
            }
            envSb.append(KEY_THRESH + "=" + threshSb.toString() + ";");
        }
    }

    private void appendWords(StringBuilder envSb) {
        if (wakupWords != null && wakupWords.length > 0) {
            StringBuilder wordSb = new StringBuilder();
            for (int i = 0; i < wakupWords.length; i++) {
                wordSb.append(wakupWords[i]);
                if (i != wakupWords.length - 1) {
                    wordSb.append(",");
                }
            }
            envSb.append(KEY_WORD + "=" + wordSb.toString() + ";");
        }
    }

    /**
     * 参数JSON化
     *
     * @return JSONObject
     */
    @Override
    public JSONObject toJson() {
        jsonObject = super.toJson();
        if (!TextUtils.isEmpty(aecBinPath)) {
            JSONUtil.putQuietly(jsonObject, KEY_AEC_BIN_PTAH, aecBinPath);
        }
        if (!TextUtils.isEmpty(wakupBinPath)) {
            JSONUtil.putQuietly(jsonObject, KEY_WAKUP_BIN_PATH, wakupBinPath);
        }

        if (!TextUtils.isEmpty(sspeBinPath)) {
            JSONUtil.putQuietly(jsonObject, "sspeBinPath", sspeBinPath);
        }

        if (!TextUtils.isEmpty(beamformingBinPath)) {
            JSONUtil.putQuietly(jsonObject, KEY_BEAMFORMING_BIN_PATH, beamformingBinPath);
        }
        if (!TextUtils.isEmpty(env)) {
            JSONUtil.putQuietly(jsonObject, KEY_ENV, this.env);
        } else {
            JSONUtil.putQuietly(jsonObject, KEY_ENV, toEnv());
        }

        if (maxVolume == 1) {
            JSONUtil.putQuietly(jsonObject, KEY_MAX_VOLUME, 1);
        } else {

        }
        calNearWakeupJsons();
        return jsonObject;
    }

    private void calNearWakeupJsons() {

    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public String getSspeBinPath() {
        return sspeBinPath;
    }

    public void setSspeBinPath(String sspeBinPath) {
        this.sspeBinPath = sspeBinPath;
    }

    public boolean isSspe() {
        return !TextUtils.isEmpty(sspeBinPath);
    }


}
