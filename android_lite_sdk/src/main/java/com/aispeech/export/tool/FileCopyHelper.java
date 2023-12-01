package com.aispeech.export.tool;

import android.content.Context;
import android.text.TextUtils;

import com.aispeech.common.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileCopyHelper {

    private static final String TAG = "FileCopyHelper";
    private static final int BUFF_SIZE = 10240;
    private static final byte[] MAGIC = {'P', 'K', 0x3, 0x4};

    private FileCopyHelper() {
    }

    /**
     * assets 里的文件复制到sd卡目录下，如果目标文件夹下有同名文件，可选择是否强制覆盖
     *
     * @param context          context
     * @param fileNameInAssets assets里的文件名
     * @param destDirPath      sd下的目标文件夹的绝对路径
     * @param force            是否强制覆盖
     * @return true 复制成功，false 失败
     */
    public static boolean copyAssetsFile(Context context, String fileNameInAssets, String destDirPath, boolean force) {
        File file = new File(destDirPath, fileNameInAssets);
        if (!force && file.exists())
            return true;
        Log.d(TAG, "copyAssetsFile() fileNameInAssets " + fileNameInAssets + "  destDirPath " + destDirPath);
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = context.getAssets().open(fileNameInAssets);
            fos = new FileOutputStream(file);
            byte[] data = new byte[BUFF_SIZE];
            int len;
            while ((len = is.read(data)) != -1) {
                fos.write(data, 0, len);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 从assets目录中拷贝资源文件到SD下，
     * 若有md5则会检查目标文件的md5，不相同则会删除目标文件，再复制。
     *
     * @param context          context
     * @param fileNameInAssets 文件名
     * @param destDirPath      资源复制的目标文件夹路径
     * @param md5              assets文件的md5
     * @return -1 拷贝失败; -2 参数错误; 0 MD5相同,略过拷贝; 1 复制成功
     */
    public static synchronized int copyAssetsFile(Context context, String fileNameInAssets, String destDirPath, String md5) {
        return copyAssetsFile(context, fileNameInAssets, destDirPath, md5, false);
    }

    /**
     * 从assets目录中拷贝资源文件到SD下，
     * 若有md5则会检查目标文件的md5，不相同则会删除目标文件，再复制。
     * 如果是zip文件也可选择是否解压，解压只在复制文件成功后才会操作。如果目标文件已经存在，无需复制，也不会进行解压操作。
     *
     * @param context          context
     * @param fileNameInAssets 文件名
     * @param destDirPath      资源复制的目标文件夹路径
     * @param md5              资源的md5
     * @param unzip            如果是压缩文件则解压,解压到 destDirPath 参数指定的目录下
     * @return -1 拷贝失败; -2 参数错误; 0 MD5相同,略过拷贝; 1 复制成功;2 复制成功但是解压缩失败
     */
    public static synchronized int copyAssetsFile(Context context, String fileNameInAssets, String destDirPath, String md5, boolean unzip) {
        if (context == null || TextUtils.isEmpty(fileNameInAssets) || TextUtils.isEmpty(destDirPath)) {
            return -2;
        }
        if (!checkAssetsFileCanRead(context, fileNameInAssets))
            return -1;

        File destFile = new File(destDirPath, fileNameInAssets);
        if (!destFile.getParentFile().exists())
            destFile.mkdirs();

        if (destFile.exists()) {
            if (md5 != null && md5.length() == 32) {
                if (checkFileMD5(destFile.getAbsolutePath(), md5))
                    return 0;   // 目的文件md5与设定的md5一致，无需复制
            } else
                return 0;

            // md5检查不通过则删除
            destFile.delete();
        }

        // 复制
        boolean suc = copyAssetsFile(context, fileNameInAssets, destDirPath, false);
        if (!suc)
            return -1;
        if (!unzip)
            return 1;

        if (isZipFile(destFile)) {
            // unzip
            suc = unZip(destFile);
        }
        return suc ? 1 : 2;
    }

    public static int copySDFile(String OriginalFilePath, String destDirPath, String md5) {
        return copySDFile(OriginalFilePath, destDirPath, md5, false);
    }

    public static int copySDFile(String OriginalFilePath, String destDirPath, String md5, boolean unzip) {
        if (TextUtils.isEmpty(OriginalFilePath) || TextUtils.isEmpty(destDirPath)) {
            return -2;
        }

        File OriginalFile = new File(OriginalFilePath);
        if (!OriginalFile.exists())
            return -1;

        File destFile = new File(destDirPath, OriginalFile.getName());
        if (!destFile.getParentFile().exists())
            destFile.mkdirs();

        if (destFile.exists()) {
            if (md5 != null && md5.length() == 32) {
                if (checkFileMD5(destFile.getAbsolutePath(), md5))
                    return 0;   // 目的文件md5与设定的md5一致，无需复制
            } else
                return 0;

            // md5检查不通过则删除
            destFile.delete();
        }

        // 复制
        boolean suc = copySDFile(OriginalFilePath, destDirPath, false);
        if (!suc)
            return -1;
        if (!unzip)
            return 0;

        if (isZipFile(destFile)) {
            // unzip
            suc = unZip(destFile);
        }
        return suc ? 0 : 2;
    }

    public static boolean copySDFile(String OriginalFilePath, String destDirPath, boolean force) {
        File OriginalFile = new File(OriginalFilePath);
        if (!OriginalFile.exists())
            return false;

        File file = new File(destDirPath, OriginalFile.getName());
        if (!force && file.exists())
            return true;

        Log.d(TAG, "copySDFile() OriginalFilePath " + OriginalFilePath);
        Log.d(TAG, "copySDFile() destDirPath " + destDirPath);
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(OriginalFile);
            fos = new FileOutputStream(file);
            byte[] data = new byte[BUFF_SIZE];
            int len;
            while ((len = fis.read(data)) != -1) {
                fos.write(data, 0, len);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static boolean checkFileMD5(String filepath, String md5) {
        try {
            String md5Target = getFileMD5String(filepath);
            Log.d(TAG, " md5Target: " + md5Target + "  filepath:" + filepath);
            return md5.equalsIgnoreCase(md5Target);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getFileMD5String(String filepath) {
        if (TextUtils.isEmpty(filepath))
            return "";
        File file = new File(filepath);
        if (!file.exists())
            return "";

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[BUFF_SIZE];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                messagedigest.update(buffer, 0, length);
            }
            return byte2HexFormatted(messagedigest.digest(), false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

    /**
     * byte转换成String
     *
     * @param arr    byte数据
     * @param format 是否用冒号(:)间隔
     * @return 字符串
     */
    public static String byte2HexFormatted(byte[] arr, boolean format) {
        StringBuilder str = new StringBuilder(arr.length * 2);
        for (int i = 0; i < arr.length; i++) {
            String h = Integer.toHexString(arr[i]);
            int l = h.length();
            if (l == 1)
                h = "0" + h;
            if (l > 2)
                h = h.substring(l - 2, l);
            str.append(h.toUpperCase());
            if (format) {
                if (i < (arr.length - 1)) {
                    str.append(':');
                }
            }
        }
        return str.toString();
    }


    /**
     * get File MD5 codes string
     *
     * @param in inputstream 方法内部不会关闭流
     * @return
     */
    private static String getFileMD5String(InputStream in) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFF_SIZE];
            int length;
            while ((length = in.read(buffer)) != -1) {
                messagedigest.update(buffer, 0, length);
            }
            return byte2HexFormatted(messagedigest.digest(), false);
        } catch (NoSuchAlgorithmException nsaex) {
            nsaex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isZipFile(File f) {
        boolean isZip = true;
        byte[] buffer = new byte[MAGIC.length];
        try {
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            raf.readFully(buffer);
            for (int i = 0; i < MAGIC.length; i++) {
                if (buffer[i] != MAGIC[i]) {
                    isZip = false;
                    break;
                }
            }
            raf.close();
        } catch (Throwable e) {
            isZip = false;
        }
        return isZip;
    }

    /**
     * 通过读 1 byte 来确定是否可以读取 assets 文件
     *
     * @param context  context
     * @param fileName 文件名
     * @return true 可以正常读取，false 不能读取
     */
    public static boolean checkAssetsFileCanRead(Context context, String fileName) {
        InputStream is = null;
        try {
            is = context.getAssets().open(fileName);
            is.read(new byte[1]);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "checkAssetsFileCanRead(): " + fileName + " not found in assets folder " +
                    "And It cannot be picture or media file!");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * unzip zipfile under destDir, support subdir
     *
     * @param zipfileName the zip file
     * @return true 解压成功，false 失败
     */
    public static boolean unZip(File zipfileName) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zipfileName);
            Enumeration<? extends ZipEntry> emu = zipFile.entries();
            byte data[] = new byte[BUFF_SIZE];
            Log.d(TAG, "unZip " + zipfileName);
            while (emu.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) emu.nextElement();
                if (entry.isDirectory()) {
                    new File(zipfileName.getParentFile(), entry.getName()).mkdirs();
                    continue;
                }
                BufferedInputStream bis = null;
                BufferedOutputStream bos = null;
                try {
                    bis = new BufferedInputStream(zipFile.getInputStream(entry));
                    OutputStream outputStream = new FileOutputStream(new File(zipfileName.getParentFile(),
                            entry.getName()));
                    bos = new BufferedOutputStream(outputStream, BUFF_SIZE);
                    int readSize;
                    while ((readSize = bis.read(data, 0, BUFF_SIZE)) != -1) {
                        bos.write(data, 0, readSize);
                    }
                    bos.flush();
                } catch (IOException e) {
                    throw new IOException("unZip inner error");
                } finally {
                    if (bis != null)
                        bis.close();
                    if (bos != null)
                        bos.close();
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 获取资源目录
     *
     * @param context context
     * @return 应用的默认文件夹 context.getFilesDir()
     */
    public static String getFilesDir(Context context) {
        if (context == null) {
            return null;
        }
        File file = context.getFilesDir();
        if (file == null) {
            file = new File("/data/data/" + context.getPackageName() + "/files");
        }
        String dirPath = file.getAbsolutePath();
        return dirPath;
    }
}
