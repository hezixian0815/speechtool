package com.aispeech.export.config;

import android.text.TextUtils;

import com.aispeech.lite.base.BaseConfig;

import java.util.List;

/**
 * 长语音识别，根据需要识别的音频文件配置参数
 */
public class AICloudLASRConfig extends BaseConfig {

    /**
     * 音频参数
     */
    public static class AudioParam {
        /**
         * 必选，音频类型，支持：wav, ogg_speex, ogg_oups, mp3
         */
        private String audioType;
        /**
         * 可选，音频文件的采样率。
         * <p>
         * 可选值： 16000（默认值）， 8000
         * <p>
         * 客户端没有传递sample_rate参数时，等同于sample_rate=16000。
         * <p>
         * (警告) 转写服务会在音频检查时，重新检测音频本身的采样率，并以检测后的结果为准。
         */
        private int sampleRate = 16000;
        /**
         * 可选，取样字节数，默认2
         */
        private int sampleBytes = 2;
        /**
         * 可选，音频通道数，默认1
         */
        private int channel = 1;

        public boolean isValid() {
            return !TextUtils.isEmpty(audioType);
        }

        /**
         * 音频是否符合以下信息：
         * 音频格式为 wav, ogg_speex, ogg_oups, mp3，并且音频 sampleRate 为 16000 Hz，channel 为 1。
         *
         * @return true 是，false 不是
         */
        public boolean isAudioStandard() {
            return sampleRate == 16000 && channel == 1 &&
                    ("mp3".equals(audioType) || "wav".equals(audioType)
                            || "ogg_opus".equals(audioType) || "ogg_speex".equals(audioType));
        }

        public AudioParam(String audioType) {
            this.audioType = audioType;
        }

        @Override
        public String toString() {
            return "AudioParam{" +
                    "audioType='" + audioType + '\'' +
                    ", sampleRate=" + sampleRate +
                    ", sampleBytes=" + sampleBytes +
                    ", channel=" + channel +
                    '}';
        }

        public String getAudioType() {
            return audioType;
        }

        /**
         * 音频类型
         *
         * @param audioType 音频类型
         */
        public void setAudioType(String audioType) {
            this.audioType = audioType;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        /**
         * 可选，音频文件的采样率。
         * <p>
         * 可选值： 16000（默认值）， 8000
         * <p>
         * 客户端没有传递sample_rate参数时，等同于sample_rate=16000。
         * <p>
         * (警告) 转写服务会在音频检查时，重新检测音频本身的采样率，并以检测后的结果为准。
         *
         * @param sampleRate 16000
         */
        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public int getSampleBytes() {
            return sampleBytes;
        }

        /**
         * 可选，取样字节数，默认2
         *
         * @param sampleBytes 取样字节数
         */
        public void setSampleBytes(int sampleBytes) {
            this.sampleBytes = sampleBytes;
        }

        public int getChannel() {
            return channel;
        }

        /**
         * 可选，音频通道数，默认1
         *
         * @param channel 音频通道数
         */
        public void setChannel(int channel) {
            this.channel = channel;
        }
    }

    public static class TaskParam {

        private AudioParam audioParam;

        /**
         * 是
         * 音频文件路径，要保障集群内能够直接访问。
         * <p>
         * 如果上传音频文件使用的上述 4.1 的接口，则此处填写流媒体服务地址。将 objectId 参数替换为 4.1 接口返回的 objectId 对应值即可。
         * <p>
         * http://streaming-media.cloud.svc.cluster.local.:29000/audio/stream/v1?objectId=5cbe728d9cfdd500012f39fc
         * <p>
         * 但是分为如下两种情况。
         * <p>
         * 如果音频格式为 wav, ogg_speex, ogg_oups 或者 mp3，并且音频 sampleRate 为 16000 Hz，channel 为 1。则直接使用上述链接。
         * 如果音频不符合 1 的情况。则链接需要添加额外的 format, sampleRate 和 channel 参数，此时从该 URL 获取的音频会直接转换为对应符合要求的格式。
         * http://streaming-media.cloud.svc.cluster.local.:29000/audio/stream/v1?objectId=5cbe728d9cfdd500012f39fc&format=wav&sampleRate=16000&channel=1
         * PS: 如果不确定音频规格，则可以统一全部加上情况 2 中的参数。
         */
        // private String file_path;
        /**
         * 是	音频文件长度。单位：byte
         */
        private int fileLen;
        /**
         * 否	1：机器转写任务（1小时音频，5分钟之内），默认值。2：人机混合任务（1小时音频，15分钟之内）。3：人工转写任务（1小时音频，24小时之内）
         */
        private int taskType = 1;
        /**
         * 否	口语顺滑开关。0：不使用，1：使用（默认）。
         */
        private boolean useTxtSmooth = true;
        /**
         * 否	逆文本转换开关。0：不使用，1：使用（默认）。
         */
        private boolean useInverseTxt = true;

        /**
         * 否	识别完成时的回调HTTP(s)地址。当设置了callback地址，当识别融合后，会把最终结果以POST方式回传（也可以通过/lasr/task/result的方式来主动获取）。
         */
        private String callback;

        /**
         * 否	音频的时长(单位：秒), 用户校验剩余时长是否充足
         */
        private double checkLength;
        /**
         * 否
         * 发音人个数，可选值：-1-8。0：表示跳过说话人聚类 。大于0：音频里发音人个数。默认值：-1：盲分。备注：
         * <ul>
         *     <li>跳过说话人聚类时，任务的最终result里，没有speaker信息。</li>
         *     <li>跳过说话人聚类时，任务的最终metrics指标里，没有diarizated_t。</li>
         * </ul>
         */
        private int speakerNumber = 0;

        /**
         * 是否返回每个分词的信息。默认值：false， 不返回。
         */
        private boolean useSegment;

        /**
         * 是否以中文句号作为分句。默认值：false
         */
        private boolean useFullstop;
        /**
         * 是否返回情绪（emotion） / 年龄（age） / 性别（gender）信息。
         * 默认值：false， 不返回。
         * 设置为true时，返回值里回增加emotion, age, gender字段。
         */
        private boolean useAux;

        /**
         * 是否在分词结果中增加语义分段标志。默认值：false。值为true时开启
         * 本参数仅限useSegment=true时有效
         */
        private boolean useParagraph;

        public boolean isUseParagraph() {
            return useParagraph;
        }

        /**
         * 是否在分词结果中增加语义分段标志。默认值：false。值为true时开启,本参数仅限useSegment=true时有效
         * @param useParagraph
         */
        public void setUseParagraph(boolean useParagraph) {
            this.useParagraph = useParagraph;
        }

        public boolean isUseAux() {
            return useAux;
        }

        /**
         * 是否返回情绪（emotion） / 年龄（age） / 性别（gender）信息。
         * 默认值：false， 不返回。
         * 设置为true时，返回值里回增加emotion, age, gender字段。
         * @param useAux
         */
        public void setUseAux(boolean useAux) {
            this.useAux = useAux;
        }

        public boolean isValid() {
            return audioParam != null && !TextUtils.isEmpty(audioParam.audioType);
        }

        /**
         * 构造方法
         *
         * @param audioParam 音频参数
         * @param fileLen    音频文件长度
         */
        public TaskParam(AudioParam audioParam, int fileLen) {
            this.audioParam = audioParam;
            this.fileLen = fileLen;
        }

        @Override
        public String toString() {
            return "TaskParam{" +
                    "audioParam=" + audioParam +
                    ", fileLen=" + fileLen +
                    ", taskType=" + taskType +
                    ", useTxtSmooth=" + useTxtSmooth +
                    ", useInverseTxt=" + useInverseTxt +
                    ", callback='" + callback + '\'' +
                    ", checkLength=" + checkLength +
                    ", speakerNumber=" + speakerNumber +
                    ", useSegment=" + useSegment +
                    ", useFullstop=" + useFullstop +
                    ", res='" + res + '\'' +
                    ", lang='" + lang + '\'' +
                    ", speakerRate=" + speakerRate +
                    ", phraseFileId='" + phraseFileId + '\'' +
                    ", hotwords=" + hotwords +
                    ", lmid='" + lmid + '\'' +
                    ", sensitiveFileId='" + sensitiveFileId + '\'' +
                    '}';
        }

        /**
         * 资源场景支持
         */
        private String res;

        /**
         * 语种支持。
         * <p>
         * 可选值： cn（默认值， 中文）, en（英文）, ce（中英文混合），sichuantone-mix（四川话+普通话）， cantonese-mix（粤语+普通话）
         * <p>
         * 当客户端没有传lang参数时，等同于lang=cn。
         */
        private String lang;

        /**
         * 说话人聚类使用的采样率。
         * <p>
         * 可选值： 16000（默认值同sample_rate）， 8000, 0 表示没有设置
         * <p>
         * 客户端没有传递speaker_rate参数时，等同于sample_rate。
         * <p>
         * speaker_rate设置为8000时，会导致即使sample_rate为16000时，说话人仍然使用8000采样率的资源。
         */
        private int speakerRate = 0;

        /**
         * 用户的热词文件ID。 绝对路径为： <PHRASE_FILES_ROOT>/<phrase_file_id>/v1.txt
         * <p>
         * (警告) （注意）：和hotwords参数只会有一个生效。
         */
        private String phraseFileId;

        /**
         * 自定义的热词列表。每个词用英文逗号分割，中文必须使用utf-8编码，单个词必须在2 ~10个汉字之间，热词列表不能超过1000个词。
         * <p>
         * (警告) （注意）：和phrase_file参数只会有一个生效。
         */
        private List<String> hotwords;

        /**
         * 自训练模型文件LMID。对应的二路模型文件在：<CUSTOME_LM_FILES_ROOT>/<productId>/<lmid>/latest/lm.pat
         */
        private String lmid;

        /**
         * 用户的敏感词文件ID。绝对路径为： <SENSITIVE_FILES_ROOT>/<sensitive_file_id>/v1.txt
         */
        private String sensitiveFileId;

        ////////////////

        public AudioParam getAudioParam() {
            return audioParam;
        }

        /**
         * 音频参数
         *
         * @param audioParam 音频参数
         */
        public void setAudioParam(AudioParam audioParam) {
            this.audioParam = audioParam;
        }

        public int getFileLen() {
            return fileLen;
        }

        /**
         * 音频文件长度。单位：byte
         *
         * @param fileLen 音频文件长度
         */
        public void setFileLen(int fileLen) {
            this.fileLen = fileLen;
        }

        public int getTaskType() {
            return taskType;
        }

        /**
         * 任务类型，1：机器转写任务（1小时音频，5分钟之内），默认值。2：人机混合任务（1小时音频，15分钟之内）。3：人工转写任务（1小时音频，24小时之内）
         *
         * @param taskType 任务类型，默认1 机器转写
         */
        public void setTaskType(int taskType) {
            this.taskType = taskType;
        }

        public boolean isUseTxtSmooth() {
            return useTxtSmooth;
        }

        /**
         * 口语顺滑开关。false：不使用，true：使用（默认）。
         *
         * @param useTxtSmooth 口语顺滑开关
         */
        public void setUseTxtSmooth(boolean useTxtSmooth) {
            this.useTxtSmooth = useTxtSmooth;
        }

        public boolean isUseInverseTxt() {
            return useInverseTxt;
        }

        /**
         * 逆文本转换开关。false：不使用，true：使用（默认）。
         *
         * @param useInverseTxt 逆文本转换开关
         */
        public void setUseInverseTxt(boolean useInverseTxt) {
            this.useInverseTxt = useInverseTxt;
        }

        public String getCallback() {
            return callback;
        }

        /**
         * 识别完成时的回调HTTP(s)地址。当设置了callback地址，当识别融合后，会把最终结果以POST方式回传（也可以通过/lasr/task/result的方式来主动获取）。
         *
         * @param callback 识别完成时的回调HTTP(s)地址
         */
        public void setCallback(String callback) {
            this.callback = callback;
        }

        public double getCheckLength() {
            return checkLength;
        }

        /**
         * 音频的时长(单位：秒), 用户校验剩余时长是否充足
         *
         * @param checkLength 音频的时长
         */
        public void setCheckLength(double checkLength) {
            this.checkLength = checkLength;
        }

        public int getSpeakerNumber() {
            return speakerNumber;
        }

        /**
         * 否
         * 发音人个数，可选值：-1-8。0：表示跳过说话人聚类 。大于0：音频里发音人个数。默认值：-1：盲分。备注：
         * <ul>
         *     <li>跳过说话人聚类时，任务的最终result里，没有speaker信息。</li>
         *     <li>跳过说话人聚类时，任务的最终metrics指标里，没有diarizated_t。</li>
         * </ul>
         *
         * @param speakerNumber 发音人个数，默认0：表示跳过说话人聚类
         */
        public void setSpeakerNumber(int speakerNumber) {
            this.speakerNumber = speakerNumber;
        }

        public boolean isUseSegment() {
            return useSegment;
        }

        /**
         * 是否返回每个分词的信息。默认值：false， 不返回。
         *
         * @param useSegment 是否返回每个分词的信息
         */
        public void setUseSegment(boolean useSegment) {
            this.useSegment = useSegment;
        }

        public boolean isUseFullstop() {
            return useFullstop;
        }

        /**
         * 是否以中文句号作为分句。默认值：false
         *
         * @param useFullstop 是否以中文句号作为分句
         */
        public void setUseFullstop(boolean useFullstop) {
            this.useFullstop = useFullstop;
        }

        public String getRes() {
            return res;
        }

        /**
         * 资源场景支持
         *
         * @param res 资源名
         */
        public void setRes(String res) {
            this.res = res;
        }

        public String getLang() {
            return lang;
        }

        /**
         * 语种支持。
         * <p>
         * 可选值： cn（默认值， 中文）, en（英文）, ce（中英文混合），sichuantone-mix（四川话+普通话）， cantonese-mix（粤语+普通话）
         * <p>
         * 当客户端没有传lang参数时，等同于lang=cn。
         *
         * @param lang 语种
         */
        public void setLang(String lang) {
            this.lang = lang;
        }

        public int getSpeakerRate() {
            return speakerRate;
        }

        /**
         * 说话人聚类使用的采样率。
         * <p>
         * 可选值： 16000（默认值同sample_rate）， 8000, 0 表示没有设置
         * <p>
         * 客户端没有传递speaker_rate参数时，等同于sample_rate。
         * <p>
         * speaker_rate设置为8000时，会导致即使sample_rate为16000时，说话人仍然使用8000采样率的资源。
         *
         * @param speakerRate 说话人聚类使用的采样率
         */
        public void setSpeakerRate(int speakerRate) {
            this.speakerRate = speakerRate;
        }

        public String getPhraseFileId() {
            return phraseFileId;
        }

        /**
         * 用户的热词文件ID。 绝对路径为： PHRASE_FILES_ROOT/phrase_file_id/v1.txt
         * <p>
         * (警告) （注意）：和hotwords参数只会有一个生效。
         *
         * @param phraseFileId 用户的热词文件ID
         */
        public void setPhraseFileId(String phraseFileId) {
            this.phraseFileId = phraseFileId;
        }

        public List<String> getHotwords() {
            return hotwords;
        }

        /**
         * 自定义的热词列表。每个词用英文逗号分割，中文必须使用utf-8编码，单个词必须在2 ~10个汉字之间，热词列表不能超过1000个词。
         * <p>
         * (警告) （注意）：和phrase_file参数只会有一个生效。
         *
         * @param hotwords 自定义的热词列表
         */
        public void setHotwords(List<String> hotwords) {
            this.hotwords = hotwords;
        }

        public String getLmid() {
            return lmid;
        }

        /**
         * 自训练模型文件LMID。对应的二路模型文件在：CUSTOME_LM_FILES_ROOT/productId/lmid/latest/lm.pat
         *
         * @param lmid 自训练模型文件LMID
         */
        public void setLmid(String lmid) {
            this.lmid = lmid;
        }

        public String getSensitiveFileId() {
            return sensitiveFileId;
        }

        /**
         * 用户的敏感词文件ID。绝对路径为： SENSITIVE_FILES_ROOT/sensitive_file_id/v1.txt
         *
         * @param sensitiveFileId 用户的敏感词文件ID
         */
        public void setSensitiveFileId(String sensitiveFileId) {
            this.sensitiveFileId = sensitiveFileId;
        }
    }
}
