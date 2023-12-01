package com.aispeech.common;

import android.text.TextUtils;

/**
 * Created by nemo on 17-11-21.
 */

public class AuthError {
    private static final String TAG = "AuthError";

    public static final String NETWORK_ERROR = "070601";
    public static final String CANNOT_GET_VALID_PROFILE = "070602";
    public static final String INVALID_APIKEY = "070603";
    public static final String INVALID_PRODUCTID = "070604";
    public static final String READ_PROFILE_FAILED = "070605";
    public static final String PROFILE_DISABLED = "070606";
    public static final String PROFILE_EXPIRED = "070607";
    public static final String PROFILE_ILLEGAL_FOR_DEVICE = "070608";
    public static final String WRITE_PROFILE_FAILED = "070609";
    public static final String PROFILE_ILLEGAL_FOR_PRODUCTID = "070610";
    public static final String INVALID_SHA256 = "070611";
    public static final String INVALID_CERTIFICATION = "070612";
    public static final String NETWORK_CONNECT_TIMEOUT = "070613";
    public static final String INVALID_SIGNATURE = "070614";
    public static final String EXCEEDED_MAX_TRIALS = "070615";


    public static String getMessage(String errorType) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "network abnormal, can not connect to auth server";
            case CANNOT_GET_VALID_PROFILE:
                return "can not get valid profile";
            case INVALID_APIKEY:
                return "invalid api key";
            case INVALID_PRODUCTID:
                return "Invalid product id";
            case READ_PROFILE_FAILED:
                return "read provision file failed";
            case PROFILE_DISABLED:
                return "profile file is disabled";
            case PROFILE_EXPIRED:
                return "profile file is expired";
            case PROFILE_ILLEGAL_FOR_DEVICE:
                return "profile file is illegal for this device";
            case WRITE_PROFILE_FAILED:
                return "can not save profile";
            case PROFILE_ILLEGAL_FOR_PRODUCTID:
                return "profile file is illegal for this productId";
            case INVALID_SHA256:
                return "invalid api key or the runtime SHA256 does not match the SHA256 when configuring apikey information";
            case INVALID_CERTIFICATION:
                return "invalid certification";
            case NETWORK_CONNECT_TIMEOUT:
                return "connect server timeout";
            case INVALID_SIGNATURE:
                return "invalid productKey or productSecret";
            case EXCEEDED_MAX_TRIALS:
                return "exceed the max number of trials";
            default:
                return "无法预知的错误发生了，请在www.dui.ai上联系我们.";
        }
    }


    public enum AUTH_ERR_MSG {

        /**
         * SDK未初始化
         */
        ERR_SDK_NO_INIT("070900", "SDK no init"),

        /**
         * 网络错误
         */
        ERR_NET_CONNECT(NETWORK_ERROR, "network abnormal, can not connect to auth server"),

        /**
         * 下载授权文件失败
         */
        ERR_PROFILE_GET(CANNOT_GET_VALID_PROFILE, "can not get valid profile"),

        /**
         * 无效的apiKey
         */
        ERR_API_KEY_INVALID(INVALID_APIKEY, "invalid api key"),

        /**
         * 无效的productId
         */
        ERR_PRODUCT_ID_INVALID(INVALID_PRODUCTID, "Invalid product id"),

        /**
         * 读取授权文件失败
         */
        ERR_PROFILE_READ(READ_PROFILE_FAILED, "read provision file failed"),

        /**
         * 授权文件无效，allow = false
         */
        ERR_PROFILE_DISABLED(PROFILE_DISABLED, "profile file is disabled"),

        /**
         * 授权文件过期
         */
        ERR_PROFILE_EXPIRED(PROFILE_EXPIRED, "profile file is expired"),

        /**
         * 授权文件和deviceId/deviceName不匹配
         */
        ERR_PROFILE_NO_MATCH_DEVICE(PROFILE_ILLEGAL_FOR_DEVICE, "profile file is illegal for this device"),

        /**
         * 授权文件保存失败
         */
        ERR_PROFILE_SAVE(WRITE_PROFILE_FAILED, "can not save profile"),

        /**
         * 授权文件和productId不匹配
         */
        ERR_PROFILE_NO_MACTH_PRODUCT_ID(PROFILE_ILLEGAL_FOR_PRODUCTID, "profile file is illegal for this productId"),

        /**
         * SHA256无效
         */
        ERR_SHA256_INVALID(INVALID_SHA256, "invalid api key or the runtime SHA256 does not match the SHA256 when configuring apikey information"),

        /**
         * ssl证书认证失败，一般由于时间不对引起
         */
        ERR_CERTIFICATION_INVALID(INVALID_CERTIFICATION, "invalid certification"),

        /**
         * 连接服务器超时
         */
        ERR_NET_TIMEOUT(NETWORK_CONNECT_TIMEOUT, "connect server timeout"),

        /**
         * productKey或productSecret无效
         */
        ERR_PRODUCT_KEY_INVALID(INVALID_SIGNATURE, "invalid productKey or productSecret"),

        /**
         * 超过最大试用次数
         */
        ERR_EXCEED_MAX_TRIAL_NUM(EXCEEDED_MAX_TRIALS, "exceed the max number of trials"),

        /**
         * 授权文件中模块类型不匹配
         */
        ERR_PROFILE_SCOPE("070616", "invalid scope in profile"),

        /**
         * 授权文件不存在(第一次使用，需要动态注册)
         */
        ERR_PROFILE_NO_EXIST("070629", "profile no exist"),

        /**
         * 未知错误
         */
        ERR_DEFALUT("070630", "无法预知的错误发生了，请在www.dui.ai上联系我们."),

        // 认证时服务器给出的错误，参见 https://wiki.aispeech.com.cn/pages/viewpage.action?pageId=27341998
        ERR_SERVER_070631("070631", "201900", "请求参数非法，参数不全、不在值域内等"),
        ERR_SERVER_070632("070632", "201901", "非法的Licence 或 license已被使用"),
        ERR_SERVER_070633("070633", "201902", "Licence 已经被使用"),
        ERR_SERVER_070634("070634", "201903", "非android、ios设备，未找到设备唯一性字段"),
        ERR_SERVER_070635("070635", "201904", "当前设备激活过，不要再次使用license进行激活（此次传入的license并没有被消费）"),
        ERR_SERVER_070636("070636", "201905", "未激活过的设备，需要使用license进行激活"),
        ERR_SERVER_070637("070637", "201906", "设置的唯一性校验字段未全部赋值"),
        ERR_SERVER_070638("070638", "201907", "唯一性校验字段与初始值不一致"),
        ERR_SERVER_070639("070639", "201908", "productSecret在数据库中不存在"),
        ERR_SERVER_070640("070640", "201909", "验证码不正确"),
        ERR_SERVER_070641("070641", "201910", "product key 与productId不匹配"),
        ERR_SERVER_070642("070642", "201911", "数据库中不存在该apikey"),
        ERR_SERVER_070643("070643", "201912", "apikey和产品不匹配"),
        ERR_SERVER_070644("070644", "201913", "设备校验太过频繁"),
        ERR_SERVER_070645("070645", "201914", "未设置产品的授权类型"),
        ERR_SERVER_070646("070646", "201915", "未提供且无法生成设备名信息（仅Android、IOS可根据请求信息生成）"),
        ERR_SERVER_070647("070647", "201916", "无效的设备名，即该产品下不存在该设备名"),
        ERR_SERVER_070648("070648", "201917", "产品的设备被禁用"),
        ERR_SERVER_070649("070649", "201918", "产品的新设备激活被禁用，允许激活过的设备激活"),
        ERR_SERVER_070650("070650", "201919", "未找到设备的secret信息，未激活时提示该错误"),
        ERR_SERVER_070651("070651", "201920", "设备设置了过期时间，且已过期"),
        ERR_SERVER_070652("070652", "201921", "无效的apikey"),
        ERR_SERVER_070653("070653", "201922", "apikey与其类型对应的校验信息不匹配"),
        ERR_SERVER_070654("070654", "201923", "设备首次激活时出现并发激活"),
        ERR_SERVER_070655("070655", "201924", "产品设置为强管控模式，未设置可激活量或可激活量小于等于已激活量"),
        ERR_SERVER_070656("070656", "201925", "产品被禁用"),
        ERR_SERVER_070657("070657", "201926", "设备被禁用"),
        ERR_SERVER_070658("070658", "201927", "设备的激活次数被限制，包括仅首次激活有效、产品级与设备级重复激活次数耗尽"),
        ERR_SERVER_070659("070659", "201928", "签名错误，客户端需要重新激活");

        private final String value;
        private final String id;
        private final String serverErrorId;

        public String getValue() {
            return value;
        }

        public String getId() {
            return id;
        }

        public String getServerErrorId() {
            return serverErrorId;
        }

        AUTH_ERR_MSG(String id, String value) {
            this.id = id;
            // 本地错误用 -1 占服务器错误id
            this.serverErrorId = "";
            this.value = value;
        }

        AUTH_ERR_MSG(String id, String serverErrorId, String value) {
            this.id = id;
            this.serverErrorId = serverErrorId;
            this.value = value;
        }

        /**
         * 新加的服务器错误信息，给dds使用
         *
         * @return true 新加的，false 以前就有的
         */
        public boolean isNewError() {
            return this.serverErrorId.length() > 0;
        }

        /**
         * 根据服务器错误id返回相应的枚举
         *
         * @param serverErrorId 服务器返回的错误id
         * @return 认证错误枚举类，找不到返回 {@link #ERR_NET_CONNECT}
         */
        public static AUTH_ERR_MSG parseServerErrorId(String serverErrorId) {
            if (TextUtils.isEmpty(serverErrorId)) {
                Log.w(TAG, "auth serverErrorId is empty");
                return AUTH_ERR_MSG.ERR_PRODUCT_KEY_INVALID;
            }
            for (AUTH_ERR_MSG em : AUTH_ERR_MSG.class.getEnumConstants()) {
                if (em.getServerErrorId().equals(serverErrorId)) {
                    return em;
                }
            }
            // 默认网络错误
            return AUTH_ERR_MSG.ERR_PRODUCT_KEY_INVALID;
        }
    }


}
