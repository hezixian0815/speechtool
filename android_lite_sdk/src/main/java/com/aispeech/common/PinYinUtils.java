package com.aispeech.common;

import android.text.TextUtils;

import com.aispeech.export.exception.IllegalPinyinException;
import com.aispeech.lite.AISpeech;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 拼音检查
 *
 * @author hehr
 */
public class PinYinUtils {

    private static final String TAG = PinYinUtils.class.getSimpleName();


    /**
     * 检查拼音是否合法
     *
     * @param pinyin 唤醒词发音
     * @return boolean 唤醒词是否合法
     */
    public static void checkPinyin(String[] pinyin) {
        if (AISpeech.illegalPingyinCheck) {
            for (String py : pinyin) {
                if (TextUtils.isEmpty(py)) {
                    throw new IllegalPinyinException();
                }

                String[] len = py.split(" ");

                if (!(len.length < PINYIN_LENGTH_MAX && len.length > PINYIN_LENGTH_MIN)) {
                    Log.e(TAG, py + " length out of range, pinyin length limit is 2-8");
                    throw new IllegalPinyinException();
                }
                for (String item : len) {
                    if (!PINYIN_DICT.contains(item)) {
                        Log.e(TAG, py + " contains illegal item " + item);
                        throw new IllegalPinyinException();
                    }
                }
            }
        }
    }

    private static final Pattern mPattern = Pattern.compile("[\u4E00-\u9FA5]+");

    public static boolean isIllegalPinyin(String py) {
        if (AISpeech.illegalPingyinCheck) {
            if (mPattern.matcher(py).matches()) {
                return true;
            }

            if (TextUtils.isEmpty(py)) {
                return true;
            }

            String[] len = py.split(" ");

            if (!(len.length < PINYIN_LENGTH_MAX && len.length > PINYIN_LENGTH_MIN)) {
                Log.e(TAG, py + " length out of range, pinyin length limit is 2-8");
                //throw new IllegalPinyinException();
                return true;
            }
            for (String item : len) {
                if (!PINYIN_DICT.contains(item)) {
                    Log.e(TAG, py + " contains illegal item " + item);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查拼音是否合法
     *
     * @param pinyin 唤醒词发音
     * @return boolean 唤醒词是否合法
     */
    public static boolean checkPinyin(String pinyin) {

        if (TextUtils.isEmpty(pinyin)) {
            return false;
        }

        String[] len = pinyin.split(" ");

        if (!(len.length < PINYIN_LENGTH_MAX && len.length > PINYIN_LENGTH_MIN)) {
            Log.e(TAG, pinyin + " length out of range, pinyin length limit is 2-8");
            return false;
        }

        for (String item : len) {
            if (!PINYIN_DICT.contains(item)) {
                Log.e(TAG, pinyin + " contains illegal item " + item);
                return false;
            }
        }

        return true;

    }

    /**
     * 拼音最大长度
     */
    private static final int PINYIN_LENGTH_MAX = 9;
    /**
     * 拼音最小长度
     */
    private static final int PINYIN_LENGTH_MIN = 1;

    /**
     * 拼音词典,不在此集合內的拼音皆可视作非法拼音
     */
    private static Set<String> PINYIN_DICT;

    static {
        PINYIN_DICT = new HashSet<String>() {{
            add("a");
            add("ai");
            add("an");
            add("ang");
            add("ao");
            add("ba");
            add("bai");
            add("ban");
            add("bang");
            add("bao");
            add("bei");
            add("ben");
            add("beng");
            add("bi");
            add("bian");
            add("biao");
            add("bie");
            add("bin");
            add("bing");
            add("bo");
            add("bu");
            add("ca");
            add("cai");
            add("can");
            add("cang");
            add("cao");
            add("ce");
            add("cen");
            add("ceng");
            add("cha");
            add("chai");
            add("chan");
            add("chang");
            add("chao");
            add("che");
            add("chen");
            add("cheng");
            add("chi");
            add("chong");
            add("chou");
            add("chu");
            add("chuai");
            add("chuan");
            add("chuang");
            add("chui");
            add("chun");
            add("chuo");
            add("ci");
            add("cong");
            add("cou");
            add("cu");
            add("cuan");
            add("cui");
            add("cun");
            add("cuo");
            add("da");
            add("dai");
            add("dan");
            add("dang");
            add("dao");
            add("de");
            add("dei");
            add("den");
            add("deng");
            add("di");
            add("dia");
            add("dian");
            add("diao");
            add("die");
            add("ding");
            add("diu");
            add("dong");
            add("dou");
            add("du");
            add("duan");
            add("dui");
            add("dun");
            add("duo");
            add("e");
            add("ei");
            add("en");
            add("er");
            add("fa");
            add("fan");
            add("fang");
            add("fei");
            add("fen");
            add("feng");
            add("fo");
            add("fou");
            add("fu");
            add("ga");
            add("gai");
            add("gan");
            add("gang");
            add("gao");
            add("ge");
            add("gei");
            add("gen");
            add("geng");
            add("gong");
            add("gou");
            add("gu");
            add("gua");
            add("guai");
            add("guan");
            add("guang");
            add("gui");
            add("gun");
            add("guo");
            add("ha");
            add("hai");
            add("han");
            add("hang");
            add("hao");
            add("he");
            add("hei");
            add("hen");
            add("heng");
            add("hng");
            add("hong");
            add("hou");
            add("hu");
            add("hua");
            add("huai");
            add("huan");
            add("huang");
            add("hui");
            add("hun");
            add("huo");
            add("ji");
            add("jia");
            add("jian");
            add("jiang");
            add("jiao");
            add("jie");
            add("jin");
            add("jing");
            add("jiong");
            add("jiu");
            add("juan");
            add("jue");
            add("jun");
            add("jv");
            add("ka");
            add("kai");
            add("kan");
            add("kang");
            add("kao");
            add("ke");
            add("ken");
            add("keng");
            add("kong");
            add("kou");
            add("ku");
            add("kua");
            add("kuai");
            add("kuan");
            add("kuang");
            add("kui");
            add("kun");
            add("kuo");
            add("la");
            add("lai");
            add("lan");
            add("lang");
            add("lao");
            add("le");
            add("lei");
            add("leng");
            add("li");
            add("lia");
            add("lian");
            add("liang");
            add("liao");
            add("lie");
            add("lin");
            add("ling");
            add("liu");
            add("lo");
            add("long");
            add("lou");
            add("lu");
            add("luan");
            add("lue");
            add("lun");
            add("luo");
            add("lv");
            add("ma");
            add("mai");
            add("man");
            add("mang");
            add("mao");
            add("me");
            add("mei");
            add("men");
            add("meng");
            add("mi");
            add("mian");
            add("miao");
            add("mie");
            add("min");
            add("ming");
            add("miu");
            add("mo");
            add("mou");
            add("mu");
            add("na");
            add("nai");
            add("nan");
            add("nang");
            add("nao");
            add("ne");
            add("nei");
            add("nen");
            add("neng");
            add("ng");
            add("ni");
            add("nian");
            add("niang");
            add("niao");
            add("nie");
            add("nin");
            add("ning");
            add("niu");
            add("nong");
            add("nou");
            add("nu");
            add("nuan");
            add("nue");
            add("nuo");
            add("nv");
            add("o");
            add("ou");
            add("pa");
            add("pai");
            add("pan");
            add("pang");
            add("pao");
            add("pei");
            add("pen");
            add("peng");
            add("pi");
            add("pian");
            add("piao");
            add("pie");
            add("pin");
            add("ping");
            add("po");
            add("pou");
            add("pu");
            add("qi");
            add("qia");
            add("qian");
            add("qiang");
            add("qiao");
            add("qie");
            add("qin");
            add("qing");
            add("qiong");
            add("qiu");
            add("quan");
            add("que");
            add("qun");
            add("qv");
            add("ran");
            add("rang");
            add("rao");
            add("re");
            add("ren");
            add("reng");
            add("ri");
            add("rong");
            add("rou");
            add("ru");
            add("ruan");
            add("rui");
            add("run");
            add("ruo");
            add("sa");
            add("sai");
            add("san");
            add("sang");
            add("sao");
            add("se");
            add("sen");
            add("seng");
            add("sha");
            add("shai");
            add("shan");
            add("shang");
            add("shao");
            add("she");
            add("shei");
            add("shen");
            add("sheng");
            add("shi");
            add("shou");
            add("shu");
            add("shua");
            add("shuai");
            add("shuan");
            add("shuang");
            add("shui");
            add("shun");
            add("shuo");
            add("si");
            add("song");
            add("sou");
            add("su");
            add("suan");
            add("sui");
            add("sun");
            add("suo");
            add("ta");
            add("tai");
            add("tan");
            add("tang");
            add("tao");
            add("te");
            add("tei");
            add("teng");
            add("ti");
            add("tian");
            add("tiao");
            add("tie");
            add("ting");
            add("tong");
            add("tou");
            add("tu");
            add("tuan");
            add("tui");
            add("tun");
            add("tuo");
            add("wa");
            add("wai");
            add("wan");
            add("wang");
            add("wei");
            add("wen");
            add("weng");
            add("wo");
            add("wu");
            add("xi");
            add("xia");
            add("xian");
            add("xiang");
            add("xiao");
            add("xie");
            add("xin");
            add("xing");
            add("xiong");
            add("xiu");
            add("xuan");
            add("xue");
            add("xun");
            add("xv");
            add("ya");
            add("yan");
            add("yang");
            add("yao");
            add("ye");
            add("yi");
            add("yin");
            add("ying");
            add("yo");
            add("yong");
            add("you");
            add("yu");
            add("yuan");
            add("yue");
            add("yun");
            add("za");
            add("zai");
            add("zan");
            add("zang");
            add("zao");
            add("ze");
            add("zei");
            add("zen");
            add("zeng");
            add("zha");
            add("zhai");
            add("zhan");
            add("zhang");
            add("zhao");
            add("zhe");
            add("zhei");
            add("zhen");
            add("zheng");
            add("zhi");
            add("zhong");
            add("zhou");
            add("zhu");
            add("zhua");
            add("zhuai");
            add("zhuan");
            add("zhuang");
            add("zhui");
            add("zhun");
            add("zhuo");
            add("zi");
            add("zong");
            add("zou");
            add("zu");
            add("zuan");
            add("zui");
            add("zun");
            add("zuo");
        }};
    }
}
