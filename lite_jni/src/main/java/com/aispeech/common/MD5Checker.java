package com.aispeech.common;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/*
 *  从 Util.copyResource copy过来的逻辑，简单做一些修改 ：
 *  # 老逻辑有md5sum的时候，比较的是 “ asset下的md5sum生成的md5值 ” “ copy到data/data/xxx/file下md5sum生成的md5值 ”
 *  # 这里改为比较 “ asset下的md5sum下的string值 ” “copy到data/data/xxx/file下的bin文件生成的md5 string值 ”
 *  # 老逻辑，资源文件从assets下copy到 files下时，没有再次校验，这里copy完再对比下md5值
 *  # 老逻辑，没有全局的finally，有io无close的风险，这里增加
 *
 * @see https://jira.aispeech.com.cn/browse/XIAOPENGDUI-658
 */
public class MD5Checker {

    private String TAG = getClass().getSimpleName();
    private static final int BUFF_SIZE = 10240;

    /* 出错 */
    public static final int ID_ERROR = -1;
    /* md5 相同 */
    public static final int ID_MD5_SAME = 0;
//    /* 第一次copy文件 */
//    public static final int ID_COPY_FILE = 1;

    /* 重试n次*/
    private final static int retryCount = 1;
    /* 目前处于重试第几次 */
    private volatile int nowCount = 0;

    public int copyResource(Context context, String resName,
                            boolean isMD5, String resMd5sumName) {
        if (context == null) {
            return ID_ERROR;
        }
        File destFile = new File(Util.getResourceDir(context), resName);
        InputStream is = getAssetsBinStream(context, resName);
        InputStream assetsMD5Stream = null;
        try {
            if (is == null) { // assets下问题有问题
                return ID_ERROR;
            }
            if (resMd5sumName == null) {
                Log.i(TAG, "there is no md5 file of : " + resName);
                // if file exists, verify MD5 code
                if (isMD5 && checkMD5(is, destFile)) {
                    return ID_MD5_SAME;
                } else {
                    saveDestFile(context, is, resName);
                    if (Util.isZipFile(getFile(context, resName))) {
                        // unzip
                        Util.unZip(context, destFile);
                    }
                    // copy完，要重新校验一次md5
                    return checkAfterCopy(context, resName, isMD5, resMd5sumName, is);
//                    return ID_COPY_FILE;
                }
            } else {
                Log.i(TAG, "there is md5 file of : " + resName);
                assetsMD5Stream = getAssetsMD5Stream(context, resMd5sumName);
                // 获取asset下md5sum的string值
                String md5sumString = getMD5sumString(context, assetsMD5Stream);
                // 最原始md5sum的string值 跟 copy后的file生成的md5 string值做比较
                if (isMD5 && checkMD5(md5sumString, destFile)) {
                    return ID_MD5_SAME;
                } else {
                    Log.i(TAG, "md5sum file in assets and data drectory bin md5 string is not same : " + resName);
                    saveDestFile(context, is, resName);
                    saveDestFile(context, assetsMD5Stream, resMd5sumName);
                    if (Util.isZipFile(getFile(context, resName))) {
                        // unzip
                        Util.unZip(context, destFile);
                    }
                    // copy完，要重新校验一次md5
                    return checkAfterCopy(context, resName, isMD5, resMd5sumName, md5sumString);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (assetsMD5Stream != null) {
                    assetsMD5Stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ID_ERROR;
    }

    private int checkAfterCopy(Context context, String resName,
                               boolean isMD5, String resMd5sumName, String md5sumString) {
        File destFileCopy = new File(Util.getResourceDir(context), resName);
        if (isMD5 && checkMD5(md5sumString, destFileCopy)) {
            nowCount = 0;
            return ID_MD5_SAME;
        } else {//
            return retryCopy(context, resName, isMD5, resMd5sumName, destFileCopy);
        }
    }

    /*
     * 没有md5sum的情况，只比较原始assets文件下跟copy后文件生成的md5值
     */
    private int checkAfterCopy(Context context, String resName,
                               boolean isMD5, String resMd5sumName, InputStream is) {
        File destFileCopy = new File(Util.getResourceDir(context), resName);
        if (isMD5 && checkMD5(is, destFileCopy)) {
            nowCount = 0;
            return ID_MD5_SAME;
        } else {//
            return retryCopy(context, resName, isMD5, resMd5sumName, destFileCopy);
        }
    }

    /*
     * 失败，重试copy操作，
     */
    private int retryCopy(Context context, String resName,
                          boolean isMD5, String resMd5sumName, File destFileCopy) {
        if (nowCount < retryCount) {// copy失败，重试
            Log.w(TAG, "copy and md5 not the same,delete and retry time: " + nowCount);
            if (destFileCopy.exists()) {
                boolean delete = destFileCopy.delete();
                Log.i(TAG, "copy file del: " + destFileCopy.getName() + " , suc : " + delete);
            }
            nowCount++;
            return copyResource(context, resName, isMD5, resMd5sumName);
        } else {//
            Log.e(TAG, "retry fail,please check your system or try reboot process");
        }
        return ID_ERROR;
    }

    public void deleteFile(Context context, String resName) {
        Log.i(TAG, "=======deleteFile : " + resName);
        File destFileCopy = new File(Util.getResourceDir(context), resName);
        if (destFileCopy.exists()) {
            boolean delete = destFileCopy.delete();
            Log.i(TAG, "copy file del: " + destFileCopy.getName() + " , suc : " + delete);
        }
    }


    private String getMD5sumString(Context context, InputStream isMd5sumIs) throws IOException {
        if (isMd5sumIs != null) {
            byte[] bytes = new byte[isMd5sumIs.available()];
            isMd5sumIs.read(bytes);
            if (bytes != null) {
                return new String(bytes);
            }
        }
        return "";
    }

    private InputStream getAssetsBinStream(Context context, String resName) {
        InputStream is = null;
        try {
            is = context.getAssets().open(resName);
            if (is != null) {
                is.read(new byte[1]);
                is.reset();
            }
        } catch (IOException e) {
            e.printStackTrace();
            String s = " not found in assest folder, Did you forget add it? or file should be one of the suffix below to avoid be compressed in assets floder.“.jpg”, “.jpeg”, “.png”, “.gif”, “.wav”, “.mp2″, “.mp3″, “.ogg”, “.aac”, “.mpg”, “.mpeg”, “.mid”, “.midi”, “.smf”, “.jet”, “.rtttl”, “.imy”, “.xmf”, “.mp4″, “.m4a”, “.m4v”, “.3gp”, “.3gpp”, “.3g2″, “.3gpp2″, “.amr”, “.awb”, “.wma”, “.wmv”";
            Log.e(TAG, "file " + resName + s);
        }
        return is;
    }

    private InputStream getAssetsMD5Stream(Context context, String resMd5sumName) {
        InputStream isMd5sumIs = null;
        try {
            isMd5sumIs = context.getAssets().open(resMd5sumName);
        } catch (IOException e) {
            Log.e(TAG, "file " + resMd5sumName
                    + " not found in assest floder, Did you forget add it?");
            e.printStackTrace();
        }
        return isMd5sumIs;
    }

    /**
     * 比较原始string值跟file生成的md5 string值
     * 内部未关闭输入流
     *
     * @param s
     * @param file
     * @return true MD5校验通过， false 文件不存在或MD5校验未通过
     */
    private boolean checkMD5(String s, File file) {
        if (file != null && file.exists()) {
            FileInputStream destFis = null;
            try {
                destFis = new FileInputStream(file);
                if (destFis != null) {
                    byte[] md5_1 = getFileMD5String(destFis);
                    String fileMd5String = byteArrayToHex(md5_1);
                    Log.i(TAG, "copy file md5 string = " + fileMd5String);
                    if (!TextUtils.isEmpty(s) && !TextUtils.isEmpty(fileMd5String)) {
                        s = s.replaceAll("\r|\n", "");
                        if (TextUtils.equals(s, fileMd5String)) {
                            return true;
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (destFis != null) {
                    try {
                        destFis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    /**
     * 比较输入流和文件的MD5码是否相同，内部未关闭输入流
     *
     * @param is
     * @param file
     * @return true MD5校验通过， false 文件不存在或MD5校验未通过
     */
    private boolean checkMD5(final InputStream is, File file) {
        if (file.exists()) {
            FileInputStream destFis = null;
            try {
                destFis = new FileInputStream(file);
                byte[] md5_1 = getFileMD5String(destFis);
                byte[] md5_2 = getFileMD5String(is);
                boolean same = true;
                int minLength = (md5_1.length > md5_2.length) ? md5_2.length : md5_1.length;
                for (int k = 0; k < minLength; k++) {
                    if (md5_1[k] != md5_2[k]) {
                        same = false;
                        break;
                    }
                }
//                destFis.close();
                return same;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (destFis != null) {
                    try {
                        destFis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    /**
     * get File MD5 codes string
     *
     * @param in inputstream
     * @return
     */
    private byte[] getFileMD5String(InputStream in) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[BUFF_SIZE];
            int length = -1;
            while ((length = in.read(buffer)) != -1) {
                messagedigest.update(buffer, 0, length);
            }
            return messagedigest.digest();

        } catch (NoSuchAlgorithmException nsaex) {
            nsaex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private File getFile(Context context, String fileName) {
        File file = new File(context.getFilesDir() + File.separator + fileName);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        return file;
    }

    public void saveDestFile(Context context, InputStream is, String resName) {
        Log.i(TAG, "save to data : " + resName);
        FileOutputStream fos = null;
        try {
            is.reset();
            fos = new FileOutputStream(getFile(context, resName));
            byte[] data = new byte[BUFF_SIZE];
            int len = 0;
            while ((len = is.read(data)) != -1) {
                fos.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * md5转string值
     * */
    public String byteArrayToHex(byte[] byteArray) {
        if (byteArray == null) {
            Log.w(TAG, "file byteArray is null");
            return "";
        }
        // 首先初始化一个字符数组，用来存放每个16进制字符
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
        char[] resultCharArray = new char[byteArray.length * 2];
        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        // 字符数组组合成字符串返回
        return new String(resultCharArray);
    }

}
