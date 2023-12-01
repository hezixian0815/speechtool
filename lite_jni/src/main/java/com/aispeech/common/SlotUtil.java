package com.aispeech.common;

import android.content.Context;
import android.text.TextUtils;

import com.aispeech.export.Vocab;
import com.aispeech.export.itn.Utils;


public class SlotUtil {
    private static final String TAG = "SlotUtil";

    public static String vocab2Slot(Vocab vocab) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(vocab.getInputPath())) {
            sb.append(FileIOUtils.readFile2StringDelSpecial(vocab.getInputPath()));
        } else {
            for (String content : vocab.getContents()) {
                String text = Utils.delSpecialCharacters(content);
                if (TextUtils.isEmpty(text)){
                    continue;
                }
                sb.append(text).append("\n");
            }
        }
        String data = sb.toString();
        Log.d(TAG, data);
        return data;
    }


    public static String vocab2SlotPath(Context context,Vocab vocab) {
        StringBuilder sb = new StringBuilder();
        for (String content : vocab.getContents()) {
            sb.append(content + "\n");
        }
        String filePath = context.getFilesDir() + "/asr/" + vocab.getName() + ".txt";
        FileIOUtils.writeFileFromString(filePath, sb.toString());
        Log.d(TAG, sb.toString());
        return filePath;
    }
}
