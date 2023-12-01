package com.aispeech.export.itn;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.lite.AISpeech;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JChineseConvertor {
    private Map<Character, Character> ts;
    private Map<Character, Character> st;
    private static JChineseConvertor convertor;
    private String path;


    public static JChineseConvertor getInstance() {
        if (convertor == null) {
            convertor = new JChineseConvertor();
        }

        return convertor;
    }

    public String t2s(String s) {
        char[] cs = new char[s.length()];

        for (int i = 0; i < s.length(); ++i) {
            cs[i] = this.t2s(s.charAt(i));
        }

        return new String(cs);
    }

    public String s2t(String s) {
        char[] cs = new char[s.length()];

        for (int i = 0; i < s.length(); ++i) {
            cs[i] = this.s2t(s.charAt(i));
        }

        return new String(cs);
    }

    public Character t2s(char c) {
        return this.ts.get(c) == null ? c : this.ts.get(c);
    }

    public Character s2t(char c) {
        return this.st.get(c) == null ? c : this.st.get(c);
    }

    private List<Character> loadTable(String file) throws IOException {
        List<Character> cs = loadChar(file, "UTF-8");
        if (cs.size() % 2 != 0) {
            throw new RuntimeException("The conversion table may be damaged or not exists");
        } else {
            return cs;
        }
    }

    private JChineseConvertor() {

    }

    public void load() {
        Log.d("Convert", "path " + path);
        if (TextUtils.isEmpty(path)) {
            Log.e("Convert", "path is empty !!");
            return;
        }
        try {
            List<Character> cs = this.loadTable(path);
            this.ts = new HashMap<>();
            this.st = new HashMap<>();

            for (int i = 0; i < cs.size(); i += 2) {
                this.ts.put(cs.get(i), cs.get(i + 1));
                this.st.put(cs.get(i + 1), cs.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Character> loadChar(String path, String charset) throws IOException {
        if (TextUtils.isEmpty(path)) return null;
        List<Character> content = new ArrayList<>();
        InputStreamReader inputStreamReader = null;
        if (path.startsWith("/")) {
            File files = new File(path);
            InputStream is = new FileInputStream(files);
            inputStreamReader = new InputStreamReader(is);
        } else {
            inputStreamReader = new InputStreamReader(AISpeech.getContext().getAssets().open(path));
        }

        BufferedReader in = new BufferedReader(inputStreamReader);

//        BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(file), charset));

        int c;
        while ((c = in.read()) != -1) {
            content.add((char) c);
        }

        in.close();
        return content;
    }

    public void setPath(String paths) {
        if (TextUtils.isEmpty(path)) {
            this.path = paths;
            load();
        }

    }


}
