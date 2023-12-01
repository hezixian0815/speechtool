package com.aispeech.export.lasr;


public class LasrSqlEntity {

    public static final int TYPE_LOCAL_FILE = 0;
    public static final int TYPE_HTTP_FILE = 1;

    /**
     * 数据库的id
     */
    private int id;
    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 0 本地文件，1 http 文件
     */
    private int type;
    /**
     * 本地文件的绝对路径或者http开头的可访问的url
     */
    private String uri;
    /**
     * 音频文件大小
     */
    private long fileLength;

    // 分片上传 ==>
    /**
     * 本地文件上传时的 audio_id
     */
    private String audioId;
    /**
     * 上传文件时用到的唯一uuid
     */
    private String uuid;

    /**
     * 每片大小,单位 byte，大小在1M-4M
     */
    private int blockSize;
    /**
     * 总分片数
     */
    private int sliceNum;
    /**
     * 已上传的最大分片编号，-1说明还没有上传
     */
    private int sliceIndex = -1;

    // 分片上传 END <==

    /**
     * 识别任务的 task_id
     */
    private String taskId;

    /**
     * 识别进度
     */
    private int progress;
    /**
     * 识别结果
     */
    private String asr;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public String getAudioId() {
        return audioId;
    }

    public void setAudioId(String audioId) {
        this.audioId = audioId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public int getSliceNum() {
        return sliceNum;
    }

    public void setSliceNum(int sliceNum) {
        this.sliceNum = sliceNum;
    }

    public int getSliceIndex() {
        return sliceIndex;
    }

    public void setSliceIndex(int sliceIndex) {
        this.sliceIndex = sliceIndex;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getAsr() {
        return asr;
    }

    public void setAsr(String asr) {
        this.asr = asr;
    }

    public LasrSqlEntity() {
    }

    public LasrSqlEntity(int type, String uri, long fileLength) {
        this.type = type;
        this.uri = uri;
        this.fileLength = fileLength;
    }

    public LasrSqlEntity(int type, String uri, long fileLength, String audioId, String uuid, int blockSize, int sliceNum) {
        this.type = type;
        this.uri = uri;
        this.fileLength = fileLength;
        this.audioId = audioId;
        this.uuid = uuid;
        this.blockSize = blockSize;
        this.sliceNum = sliceNum;
    }

    public LasrSqlEntity(int id, String timestamp, int type, String uri, long fileLength, String audioId, String uuid, int blockSize, int sliceNum, int sliceIndex, String taskId, int progress, String asr) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.uri = uri;
        this.fileLength = fileLength;
        this.audioId = audioId;
        this.uuid = uuid;
        this.blockSize = blockSize;
        this.sliceNum = sliceNum;
        this.sliceIndex = sliceIndex;
        this.taskId = taskId;
        this.progress = progress;
        this.asr = asr;
    }

    @Override
    public String toString() {
        return "LasrSqlEntity{" +
                "id=" + id +
                ", timestamp='" + timestamp + '\'' +
                ", type=" + type +
                ", uri='" + uri + '\'' +
                ", fileLength=" + fileLength +
                ", audioId='" + audioId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", blockSize=" + blockSize +
                ", sliceNum=" + sliceNum +
                ", sliceIndex=" + sliceIndex +
                ", taskId='" + taskId + '\'' +
                ", progress=" + progress +
                ", asr='" + (asr == null || asr.length() < 20 ? asr : asr.substring(0, 20) + "...") + '\'' +
                '}';
    }

    public boolean isUploadSucess() {
        // Log.d("isUploadSucess", "isUploadSucess sliceIndex " + sliceIndex + " sliceNum " + sliceNum);
        return this.sliceIndex < 0 ? false : (this.sliceIndex + 1) * 100 / sliceNum == 100;
    }

    public boolean isLocalFile() {
        return type == 0;
    }

    public int getUploadOffset() {
        return blockSize * (sliceIndex + 1);
    }

    public int getUploadProgress() {
        return this.sliceIndex < 0 ? 0 : (this.sliceIndex + 1) * 100 / sliceNum;
    }

    public static int calculateBlockSize(long fileLength) {
        int M1 = 1048576;
        int M2 = M1 * 2;
        int M4 = M1 * 4;

        int M10 = M1 * 10;
        int M50 = M1 * 50;

        if (fileLength <= 0)
            return 0;

        if (fileLength <= M4)
            return M4;        // 4M 以内不分片
        else if (fileLength <= M10)
            return M1;
        else if (fileLength <= M50)
            return M2;
        else
            return M4;
    }

    public static int calculateSliceNum(long fileLength, int blockSize) {
        if (fileLength == 0 || blockSize == 0) {
            return 0;
        }
        int sliceNum = (int) (fileLength / blockSize);
        if (fileLength % blockSize > 0)
            sliceNum += 1;

        return sliceNum;
    }
}
