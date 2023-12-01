package com.aispeech.auth;

import static com.aispeech.auth.Auth.KEY_ALLOW;
import static com.aispeech.auth.Auth.KEY_BIND_API_KEY;
import static com.aispeech.auth.Auth.KEY_DEFAULT_PROFILE_NAME;
import static com.aispeech.auth.Auth.KEY_DEFAULT_SCOPE;
import static com.aispeech.auth.Auth.KEY_DEVICE_ID;
import static com.aispeech.auth.Auth.KEY_DEVICE_INFO;
import static com.aispeech.auth.Auth.KEY_DEVICE_NAME;
import static com.aispeech.auth.Auth.KEY_DEVICE_SECRET;
import static com.aispeech.auth.Auth.KEY_EXPIRE;
import static com.aispeech.auth.Auth.KEY_PRODUCT_ID;
import static com.aispeech.auth.Auth.KEY_SCOPE;
import static com.aispeech.auth.Auth.KEY_TIMES_LIMIT;
import static com.aispeech.auth.Auth.KEY_TRIAL;

import android.content.Context;
import android.text.TextUtils;

import com.aispeech.auth.config.AIAuthConfig;
import com.aispeech.common.AISpeechBridge;
import com.aispeech.common.AuthError;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.jnihelper.AuthJniHelper;
import com.aispeech.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author wuwei
 * @date 2019-09-25 19:56
 * @email wei.wu@aispeech.com
 */
public class AIProfile {
    private static final String TAG = "AIProfile";
    private AIAuthConfig mConfig;
    private Context mContext;
    private ProfileState.AUTH_TYPE mAuthType = ProfileState.AUTH_TYPE.ONLINE;
    private String mDeviceName;
    private String mDeviceSecret;
    private static final String TEMP_NAME = ".temp";
    /**
     * Profile 内的原始内容
     */
    private String authDataFromServer;


    public AIProfile(Context context, AIAuthConfig config) {
        this.mContext = context;
        this.mConfig = config;
        this.authDataFromServer = null;
        ProfileState state = isProfileValid(null);
        if (!state.isValid() || state.getAuthType() != ProfileState.AUTH_TYPE.ONLINE) {
            copyOfflineProfile();
        }
    }

    public String getProductId() {
        return mConfig.getProductId();
    }

    public String getProductKey() {
        return mConfig.getProductKey();
    }

    public String getApiKey() {
        return mConfig.getApiKey();
    }

    public String getProductSecret() {
        return mConfig.getProductSecret();
    }


    public String getProfilePath() {
        String profilePath;
        String offlineProfileName = mConfig.getOfflineProfileName() == null ? "" : mConfig.getOfflineProfileName();
        String fileName = offlineProfileName + KEY_DEFAULT_PROFILE_NAME;
        if (!TextUtils.isEmpty(mConfig.getProfilePath())) {
            //create custom deviceProfilePath
            File parentDir = new File(mConfig.getProfilePath());
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            } else if (!parentDir.isDirectory()) {
                Log.w(TAG, "user set profile path is not directory");
            }
            String customDeviceId = mConfig.getCustomDeviceId();
            if (!TextUtils.isEmpty(customDeviceId)) {
                if (FileUtils.isFileExists(mConfig.getProfilePath() + File.separator + AuthUtil.getDeviceId(mContext, customDeviceId))) {
                    mAuthType = ProfileState.AUTH_TYPE.OFFLINE;
                    fileName = AuthUtil.getDeviceId(mContext,customDeviceId);
                    Log.d(TAG, "offline profile " + mConfig.getProfilePath() + File.separator + fileName);
                }
            }
            profilePath = mConfig.getProfilePath() + File.separator + fileName;
        } else {
            profilePath = mContext.getFilesDir() + File.separator + fileName;
        }
        return profilePath;
    }


    public boolean checkApiKey(String apiKey, String secretCode) {
        return AuthJniHelper.checkApikey(apiKey, secretCode);
    }

    public String getDeviceName() {
        return isProfileValid(null).isValid() ? mDeviceName : "";
    }

    public String getDeviceSecret() {
        return isProfileValid(null).isValid() ? mDeviceSecret : "";
    }

    public ProfileState isProfileValid(String scopeType) {
        return isProfileValid(false, scopeType);
    }

    /**
     * 读取解密后的授权文件内容
     *
     * @return 授权文件内容字符串
     */
    public static String decodeProfile(String profilePath) {
        File profile = new File(profilePath);
        if (!profile.exists()) {
//            throw new IOException("profile no exist");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(profile))) {
            String str = "";
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
//            throw new IOException("profile open failed");
            return null;
        }

        if (TextUtils.isEmpty(sb.toString())) {
//            throw new IOException("profile is empty");
            return null;
        }
        byte[] result = new byte[2048];
        int ret = Auth.DecryptProfile(sb.toString(), result);
        Log.d(TAG, "DecryptProfile: " + ret);
        String retStr = new String(result).trim();
        if (ret != 0) {
//            throw new IOException("profile decrypt failed");
            return null;
        } else {
            return retStr;
        }
    }


    protected synchronized ProfileState isProfileValid(boolean forceReadFromFile, String scopeType) {
        ProfileState profileState = new ProfileState();
        //需要置位全局变量，防止：在线(默认)->试用->重试更新后为在线后还显示试用类型
        mAuthType = ProfileState.AUTH_TYPE.ONLINE;
        if (BuildConfig.NO_AUTH) {
            mDeviceName = "DeviceNameNoAuth";
            mDeviceSecret = "DeviceSecretNoAuth";
            profileState.setValid(true);
            return profileState;
        }
        // getProfilePath 有可能会设置 mAuthType 为 OFFLINE
        String authFilePath = getProfilePath();
        if (mContext == null) {
            profileState.setAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_SDK_NO_INIT);
            return profileState;
        }

        if (authDataFromServer == null || authDataFromServer.length() == 0 || forceReadFromFile) {
            if (!new File(authFilePath).exists()) {
                String pathTemp = authFilePath + TEMP_NAME;
                // 授权文件不存在则判断一下temp文件是否存在，存在则读取。主要是解决功能使用时将离线授权文件更新成在线授权文件读取不到的问题。
                if (new File(pathTemp).exists())
                    authFilePath = pathTemp;
            }
            authDataFromServer = readProfile(authFilePath);
        }
        final String content = decryptProfile(authDataFromServer);
        //经过 getProfilePath() 后 mAuthType 可能为 offline 类型，需要更新状态，否则下面校验内容会异常
        profileState.setAuthType(mAuthType);
        if (TextUtils.isEmpty(content)) {
            Log.w(TAG, "profile content is null");
            //修复：离线授权文件解析失败后，需要删除离线授权文件，否则在线授权文件解析判断过不了
            if (mAuthType == ProfileState.AUTH_TYPE.OFFLINE) {
                Log.write(TAG, "delete invalid offline profile and ret " + deleteProfile());
            }
            if (TextUtils.isEmpty(getOfflineEngine())) {
                profileState.setAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_PROFILE_NO_EXIST);
            }
        } else {
            handleProfileContent(profileState, content, scopeType);
        }

        if (!TextUtils.isEmpty(getOfflineEngine())) {
            profileState.setValid(true);
        }

        return profileState;
    }


    private ProfileState handleProfileContent(ProfileState profileState, String content, String scopeType) {
        try {
            JSONObject object = new JSONObject(content);
            //check productId
            if (!TextUtils.equals(mConfig.getProductId(), object.optString(KEY_PRODUCT_ID, null))) {
                Log.w(TAG, "productId is invalid in profile");
                profileState.setAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_PROFILE_NO_MACTH_PRODUCT_ID);
                return profileState;
            }
            //check has trial key
            if (object.has(KEY_TRIAL)) {
                mAuthType = ProfileState.AUTH_TYPE.TRIAL;
                profileState.setAuthType(ProfileState.AUTH_TYPE.TRIAL);
                profileState.setTimesLimit(object.optInt(KEY_TIMES_LIMIT, -1));
            }
            //新增需求：https://jira.aispeech.com.cn/browse/DUIXQGL-901
            //当传进来scope才需要检查类型和次数
            if (object.has(KEY_SCOPE) && !TextUtils.isEmpty(scopeType)) {
                //检查scope是否合法
                if (!checkScopeValid(object, scopeType)) {
                    profileState.setAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_PROFILE_SCOPE);
                    return profileState;
                }

                //试用模式下，检查是否超过最大试用次数
                if (profileState.getAuthType() == ProfileState.AUTH_TYPE.TRIAL
                        && profileState.getTimesLimit() != -1
                        && !checkTimesLimitValid(profileState.getTimesLimit(), scopeType)) {
                    profileState.setAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_EXCEED_MAX_TRIAL_NUM);
                    return profileState;
                }
            }
            //check bindApiKey valid
            if (!checkBindApiKeyValid(object)) {
                Log.w(TAG, "apiKey is invalid for bindApiKey");
                profileState.setAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_API_KEY_INVALID);
                return profileState;
            }
            //check deviceId
            if (!checkDeviceIdValid(object, profileState)) {
                Log.w(TAG, "deviceId is invalid");
                profileState.setAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_PROFILE_NO_MATCH_DEVICE);
                return profileState;
            }
            //check expired
            if (checkExpired(object)) {
                Log.w(TAG, "profile is expired");
                profileState.setAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_PROFILE_EXPIRED);
                return profileState;
            }
            //check allow or not
            if (!checkAllow(object)) {
                Log.w(TAG, "profile is not allowed");
                profileState.setAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_PROFILE_DISABLED);
                return profileState;
            }
            profileState.setValid(true);
            Log.d(TAG, "profile is valid " + profileState.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return profileState;
    }


    /**
     * 更新试用次数
     *
     * @param scope
     */
    public void updateUsedTimes(String scope) {
        AuthUtil.updateUsedTimes(mContext, mConfig.getProfilePath(), scope);
    }


    /**
     * 判断该scope已经是否已经超过最大试用次数
     *
     * @param timesLimit
     * @param scope
     * @return
     */
    private boolean checkTimesLimitValid(int timesLimit, String scope) {
        int usedTimes = AuthUtil.getUsedTimes(mContext, scope, mConfig.getProfilePath());
        return usedTimes < timesLimit;
    }


    /**
     * 检查授权文件中是否含有该scope
     *
     * @param object
     * @param scope
     * @return
     */
    private boolean checkScopeValid(JSONObject object, String scope) {
        Log.d(TAG, "current scope is : " + scope);
        JSONArray scopes = object.optJSONArray(KEY_SCOPE);
        if (scopes.toString().contains(KEY_DEFAULT_SCOPE) || BuildConfig.NO_SCOPE) {
            Log.d(TAG, "all scope, ret true");
            return true;
        } else if (!scopes.toString().contains(scope)) {
            Log.e(TAG, "invalid scope in profile, and current scopes: ->\n" + scopes.toString());
            //试用授权文件无效，抛错
            return false;
        }
        return true;
    }


    /**
     * --ONLINE： deviceId == deviceName
     * --OFFLINE: deviceId == deviceId
     * --TRIAL: deviceId为固定字符串，无需检查
     *
     * @param object
     * @param state
     * @return
     */
    private boolean checkDeviceIdValid(JSONObject object, ProfileState state) {
        mDeviceName = object.optString(KEY_DEVICE_NAME, null);
        mDeviceSecret = object.optString(KEY_DEVICE_SECRET, null);
        Log.d(TAG, "deviceName is " + mDeviceName);
        Log.d(TAG, "deviceSecret is " + mDeviceSecret);
        if (state.getAuthType() == ProfileState.AUTH_TYPE.ONLINE) {
            JSONObject deviceInfo = object.optJSONObject(KEY_DEVICE_INFO);
            if (deviceInfo == null) {
                return false;
            }
            return TextUtils.equals(getDeviceId(),
                    deviceInfo.optString(KEY_DEVICE_ID, null));
        } else if (state.getAuthType() == ProfileState.AUTH_TYPE.OFFLINE) {
            return TextUtils.equals(AuthUtil.getDeviceId(mContext, mConfig.getCustomDeviceId()), mDeviceName);
        } else {
            return true;
        }
    }

    /**
     * 获取allow字段的值
     *
     * @param object
     * @return
     */
    private boolean checkAllow(JSONObject object) {
        return object.optBoolean(KEY_ALLOW, true);
    }


    /**
     * 将服务端下发的授权文件写入本地
     *
     * @param content
     * @return
     */
    public boolean writeProfile(String content) {
        File file = new File(getProfilePath());
        if (file.exists()) {
            Log.write(TAG, "delete file before write :" + FileUtils.deleteFile(file));
        }

        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        FileUtils.deleteFile(getProfilePath() + TEMP_NAME);
        return true;
    }


    /**
     * 注意：
     * 1.车载的 在线授权方式 deviceId 实际对应传入设备号 deviceName ，因此需要判断 customDeviceName
     * 是否为空，不为空则取 customDeviceName 的值。
     * 2.车载项目要求在线授权方式必须指定设备号 ：CustomDeviceName
     *
     * @return 设备激活号
     */
    public String getDeviceId() {
        String deviceId;
        if (mAuthType == ProfileState.AUTH_TYPE.ONLINE) {
            if (!TextUtils.isEmpty(mConfig.getCustomDeviceName())) {
                deviceId = mConfig.getCustomDeviceName();
            } else {
                deviceId = mConfig.getCustomDeviceId();
            }
        } else {
            deviceId = mConfig.getCustomDeviceId();
        }
        return AuthUtil.getDeviceId(mContext, deviceId);
    }


    private boolean checkExpired(JSONObject object) {
        boolean expired = false;
        if (!object.isNull(KEY_EXPIRE)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            try {
                Date expireDate = sdf.parse(object.getString(KEY_EXPIRE));
                if (expireDate.getTime() < System.currentTimeMillis()) {
                    expired = true;
                }
            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }
        }
        return expired;
    }

    private boolean checkBindApiKeyValid(JSONObject object) {
        boolean isValid = false;
        if (object.isNull(KEY_BIND_API_KEY)) {
            return true;
        }
        JSONArray bindApiKeyJsonArr = null;
        try {
            bindApiKeyJsonArr = object.getJSONArray(KEY_BIND_API_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (bindApiKeyJsonArr != null) {
            for (int i = 0; i < bindApiKeyJsonArr.length(); i++) {
                try {
                    String apiKeyStr = bindApiKeyJsonArr.getString(i);
                    if (mConfig.getApiKey().equals(apiKeyStr)) {
                        isValid = true;
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    isValid = false;
                }
            }
        }
        return isValid;
    }


    /**
     * 读取解密后的授权文件内容
     *
     * @return 授权文件内容字符串
     */
    private static String readProfile(String profilePath) {
        File profile = new File(profilePath);
        if (!profile.exists()) {
            Log.d(TAG, "profile no exist");
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(profile))) {
            String str = "";
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }

    private static String decryptProfile(String profileContent) {
        if (TextUtils.isEmpty(profileContent)) {
            return null;
        }
        byte[] result = new byte[2048];
        int ret = AuthJniHelper.decryptProfile(profileContent, result);
        Log.d(TAG, "decryptProfile: " + ret);
        String retStr = new String(result).trim();
        if (ret != 0) {
            return null;
        } else {
            return retStr;
        }
    }


    /**
     * 拷贝assets目录下下指定的离线授权文件
     */
    private void copyOfflineProfile() {
        //copy assets目录授权文件 to 指定目录/.profile
        String customProfile = mConfig.getOfflineProfileName();
        boolean copySuccess = false;
        if (!TextUtils.isEmpty(customProfile)) {
            Log.d(TAG, "need to copy offline auth file: " + customProfile + " getProfilePath(): " + getProfilePath());
            boolean copySD = false;
            int copyAssets = Util.copyFilesFromAssets(mContext, customProfile, getProfilePath());
            if (copyAssets != 0) {
                File authFile = new File(mConfig.getProfilePath(), customProfile);
                Log.d(TAG, "sd authFile: " + authFile.getAbsolutePath());
                copySD = FileUtils.copySDFile(authFile, new File(getProfilePath()), true);
            }
            copySuccess = copyAssets == 0 || copySD;
            Log.d(TAG, "copy: copyAssets " + copyAssets + " copySD " + copySD);
        }
        if (!copySuccess && FileUtils.isFileExists(getProfilePath() + TEMP_NAME)) {
            Log.d(TAG, "temp profile is exist, exception happened before, revert trial Profile");
            revertTrailProfile(TEMP_NAME);
        }
    }


    /**
     * 恢复试用授权文件
     *
     * @param temp
     */
    public void revertTrailProfile(String temp) {
        Log.d(TAG, "reset trail profile before");
        String profilePath = getProfilePath();
        Log.d(TAG, "trail profile is: " + profilePath);
        if (FileUtils.isFile(profilePath)) {
            boolean deleteOK = FileUtils.deleteFile(profilePath);
            Log.d(TAG, "trail profile is exist, delete first and deleteOK = " + deleteOK);
        }
        boolean isOK = FileUtils.moveFile(profilePath + temp, profilePath);
        Log.d(TAG, "reset trail profile end and isOk = " + isOK);
    }

    /**
     * 拷贝试用授权文件
     */
    public void copyTrailProfile() {
        Log.d(TAG, "copy trial profile before");
        String profilePath = getProfilePath();
        if (FileUtils.isFile(profilePath + TEMP_NAME)) {
            boolean deleteOK = FileUtils.deleteFile(profilePath + TEMP_NAME);
            Log.d(TAG, "temp trial profile is exist, delete first and deleteOK = " + deleteOK);
        }
        Log.d(TAG, "trial profile is: " + profilePath);
        boolean isOK = FileUtils.moveFile(profilePath, profilePath + TEMP_NAME);
        Log.d(TAG, "copy trail profile end and isOk = " + isOK);
    }

    public boolean deleteProfile() {
        String profilePath = getProfilePath();
        Log.write(TAG, "deleteProfile:" + profilePath);
        return FileUtils.deleteFile(profilePath);
    }

    private String getOfflineEngine() {
        return AISpeechBridge.offlineEngineAuth;
    }

    @Override
    public String toString() {
        return "AIProfile{" +
                "mConfig=" + mConfig +
                ", mAuthType=" + mAuthType +
                ", mDeviceName='" + mDeviceName + '\'' +
                ", mDeviceSecret='" + mDeviceSecret + '\'' +
                '}';
    }
}
