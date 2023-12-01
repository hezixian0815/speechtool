package com.aispeech.common;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


public class URLUtils {

    public static final String HTTP_PROTOCOL_HEAD = "http://";
    public static final int HTTP_PROTOCOL_HEAD_LENGTH = HTTP_PROTOCOL_HEAD
            .length();
    public static final String HTTPS_PROTOCOL_HEAD = "https://";
    public static final int HTTPS_PROTOCOL_HEAD_LENGTH = HTTPS_PROTOCOL_HEAD
            .length();

    public static final String WSS_PROTOCOL_HEAD = "wss://";
    public static final int WSS_PROTOCOL_HEAD_LENGTH = WSS_PROTOCOL_HEAD
            .length();

    public static final String WS_PROTOCOL_HEAD = "ws://";
    public static final int WS_PROTOCOL_HEAD_LENGTH = WS_PROTOCOL_HEAD
            .length();


    public static final boolean isLetterOrDigit(char c) {
        return ('0' <= c && c <= '9') || ('a' <= c && c <= 'z')
                || ('A' <= c && c <= 'Z');
    }

    /**
     * 获取url的域名部分，注意：域名部分包括端口号。
     *
     * @param url
     * @return
     */
    public static final String getDomain(String url) {
        // 0. 参数检验
        if (url == null)
            return null;

        // 1. 剔出域名
        String domain;

        int l = 0;
        if (url.startsWith(HTTP_PROTOCOL_HEAD)) {
            l = HTTP_PROTOCOL_HEAD_LENGTH;
        } else if (url.startsWith(HTTPS_PROTOCOL_HEAD)) {
            l = HTTPS_PROTOCOL_HEAD_LENGTH;
        } else if (url.startsWith(WSS_PROTOCOL_HEAD)) {
            l = WSS_PROTOCOL_HEAD_LENGTH;
        } else if (url.startsWith(WS_PROTOCOL_HEAD)) {
            l = WS_PROTOCOL_HEAD_LENGTH;
        }
        int slash = url.indexOf('/', l);
        if (slash > 0) {
            domain = url.substring(l, slash);
        } else {
            if (l > 0) {
                domain = url.substring(l);
            } else {
                domain = url;
            }
        }
        return domain;
    }

    public static final String getDomainWithoutPort(String url) {
        String domain = getDomain(url);
        if (domain == null)
            return null;
        int idx = domain.indexOf(':');
        if (idx > 0) {
            return domain.substring(0, idx);
        } else {
            return domain;
        }
    }

    public static final String getDomainWithProtocal(String url) {
        // 0. 参数检验
        if (url == null)
            return null;

        // 1. 剔出域名
        String domain;

        int l = 0;
        if (url.startsWith(HTTP_PROTOCOL_HEAD)) {
            l = HTTP_PROTOCOL_HEAD_LENGTH;
        } else if (url.startsWith(HTTPS_PROTOCOL_HEAD)) {
            l = HTTPS_PROTOCOL_HEAD_LENGTH;
        }

        int slash = url.indexOf('/', l);
        if (slash > 0) {
            domain = url.substring(0, slash);
        } else {
            domain = url;
        }
        return domain;
    }

    public static final String regulateUrl(String url) {
        if (url == null)
            return null;

        boolean needProt = false;
        boolean needTail = false;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            needProt = true;
        }
        if (isDomain(url) && !url.endsWith("/")) {
            needTail = true;
        }
        if (needProt) {
            url = "http://" + url;
        }
        if (needTail) {
            url += '/';
        }

        return url;
    }

    /**
     * 判定一个字符串是否为URL，并返回归一化后的URL字符串。 归一化规则：1.以http://打头; 2.端口号为80时，要省略; 3.
     * 纯域名时，要加"/"作结尾
     *
     * @param query
     *            检查字符串
     * @return 当结果为URL时，返回归一化的结果，否则返回null。
     */
    private static final String[] traditionalUrlPostfix = { ".com", ".biz",
            ".pro", ".aero", ".coop", ".museum", ".mobi", ".edu", ".gov",
            ".info", ".mil", ".name", ".net", ".org", ".jobs", ".travel",
            ".mil", ".arpa", ".int", ".cat", ".asia", ".tel" };
    private static final String[] internationalTraditionalUrlPostfix = {
            ".com", ".net", ".edu", };
    private static final String[] regionalUrlPostfix = {

            ".ac", ".ad", ".ae", ".af", ".ag", ".ai", ".al", ".am", ".an", ".ao",
            ".aq", ".ar", ".as", ".at", ".au", ".aw", ".az", ".ba", ".bb",
            ".bd", ".be", ".bf", ".bg", ".bh", ".bi", ".bj", ".bm", ".bn",
            ".bo", ".br", ".bs", ".bt", ".bv", ".bw", ".by", ".bz", ".ca",
            ".cc", ".cd", ".cf", ".cg", ".ch", ".ci", ".ck", ".cl", ".cm",
            ".cn", ".co", ".cr", ".cs", ".cu", ".cv", ".cx", ".cy", ".cz",
            ".de", ".dj", ".dk", ".dm", ".do", ".dz", ".ec", ".eu", ".fi",
            ".fj", ".fk", ".fm", ".fo", ".fr", ".fx", ".ga", ".gb", ".gd",
            ".ge", ".gf", ".gh", ".gi", ".gl", ".gp", ".gq", ".gf", ".gm",
            ".gn", ".gr", ".gs", ".gt", ".gu", ".gw", ".gy", ".hk", ".hm",
            ".hn", ".hr", ".ht", ".hu", ".id", ".ie", ".il", ".in", ".io",
            ".iq", ".ir", ".is", ".it", ".jm", ".jo", ".jp", ".ke", ".kg",
            ".kh", ".ki", ".km", ".kn", ".kp", ".kr", ".kw", ".ky", ".kz",
            ".la", ".lb", ".lc", ".li", ".lk", ".lr", ".ls", ".lt", ".lu",
            ".lv", ".ly", ".ma", ".mc", ".md", ".mg", ".mh", ".mk", ".ml",
            ".mm", ".mn", ".mo", ".mp", ".mq", ".mr", ".ms", ".mt", ".mu",
            ".mv", ".mw", ".mx", ".my", ".mz", ".na", ".nc", ".ne", ".nf",
            ".ng", ".ni", ".nl", ".no", ".np", ".nr", ".nt", ".nu", ".nz",
            ".om", ".pa", ".pe", ".pf", ".pg", ".ph", ".pk", ".pl", ".pm",
            ".pn", ".pt", ".pr", ".pw", ".py", ".qa", ".re", ".ro", ".ru",
            ".rw", ".sa", ".sb", ".sc", ".sd", ".se", ".sg", ".sh", ".si",
            ".sj", ".sk", ".sl", ".sm", ".sn", ".so", ".sr", ".st", ".su",
            ".sv", ".sy", ".sz", ".tc", ".td", ".tf", ".tg", ".th", ".tj",
            ".tk", ".tm", ".tn", ".to", ".tp", ".tr", ".tt", ".tv", ".tw",
            ".tz", ".ua", ".ug", ".uk", ".um", ".us", ".uy", ".uz", ".va",
            ".vc", ".ve", ".vg", ".vi", ".vn", ".vu", ".wf", ".ws", ".ye",
            ".yt", ".yu", ".za", ".zm", ".zr", ".zw", ".ad", ".ae", ".af",
            ".ag", ".ai", ".al", ".am", ".an", ".ao", ".aq", ".ar", ".as",
            ".at", ".au", ".aw", ".az", ".ba", ".bb", ".bd", ".be", ".bf",
            ".bg", ".bh", ".bi", ".bj", ".bm", ".bn", ".bo", ".br", ".bs",
            ".bt", ".bv", ".bw", ".by", ".bz", ".ca", ".cc", ".cf", ".cg",
            ".ch", ".ci", ".ck", ".cl", ".cm", ".cn", ".co", ".cq", ".cr",
            ".cu", ".cv", ".cx", ".cy", ".cz", ".de", ".dj", ".dk", ".dm",
            ".do", ".dz", ".ec", ".ee", ".eg", ".eh", ".es", ".et", ".ev",
            ".fi", ".fj", ".fk", ".fm", ".fo", ".fr", ".ga", ".gb", ".gd",
            ".ge", ".gf", ".gh", ".gi", ".gl", ".gm", ".gn", ".gp", ".gr",
            ".gt", ".gu", ".gw", ".gy", ".hk", ".hm", ".hn", ".hr", ".ht",
            ".hu", ".id", ".ie", ".il", ".in", ".io", ".iq", ".ir", ".is",
            ".it", ".jm", ".jo", ".jp", ".ke", ".kg", ".kh", ".ki", ".km",
            ".kn", ".kp", ".kr", ".kw", ".ky", ".kz", ".la", ".lb", ".lc",
            ".li", ".lk", ".lr", ".ls", ".lt", ".lu", ".lv", ".ly", ".ma",
            ".mc", ".md", ".me", ".mg", ".mh", ".ml", ".mm", ".mn", ".mo",
            ".mp", ".mq", ".mr", ".ms", ".mt", ".mv", ".mw", ".mx", ".my",
            ".mz", ".na", ".nc", ".ne", ".nf", ".ng", ".ni", ".nl", ".no",
            ".np", ".nr", ".nt", ".nu", ".nz", ".om", ".pa", ".pe", ".pf",
            ".pg", ".ph", ".pk", ".pl", ".pm", ".pn", ".pr", ".pt", ".pw",
            ".py", ".qa", ".re", ".ro", ".rs", ".ru", ".rw", ".sa", ".sb",
            ".sc", ".sd", ".se", ".sg", ".sh", ".si", ".sj", ".sk", ".sl",
            ".sm", ".sn", ".so", ".sr", ".st", ".su", ".sy", ".sz", ".tc",
            ".td", ".tf", ".tg", ".th", ".tj", ".tk", ".tl", ".tm", ".tn",
            ".to", ".tp", ".tr", ".tt", ".tv", ".tw", ".tz", ".ua", ".ug",
            ".uk", ".us", ".uy", ".va", ".vc", ".ve", ".vg", ".vn", ".vu",
            ".wf", ".ws", ".ye", ".yu", ".za", ".zm", ".zr", ".zw"

    };
    // A C－科研机构； COM－工、商、金融等专业； EDU－教育机构； GOV－政府部门； NET－互
    // 联网络、接入网络的信息中心和运行中心； ORG
    private static final String[] fixupPostfix = new String[] { ".cn", ".bj",
            ".id", ".co", ".il", ".co", ".jp", ".co", ".kr", ".co", ".nr",
            ".co", ".uk", ".co", ".uz", ".co", ".cn", ".ac", ".cn", ".com",
            ".cn", ".edu", ".cn", ".gov", ".cn", ".net", ".cn", ".org", ".cn",
            ".sh", ".cn", ".tj", ".cn", ".cq", ".cn", ".he", ".cn", ".sx",
            ".cn", ".nm", ".cn", ".ln", ".cn", ".jl", ".cn", ".hl", ".cn",
            ".js", ".cn", ".zj", ".cn", ".ah", ".cn", ".fj", ".cn", ".jx",
            ".cn", ".sd", ".cn", ".ha", ".cn", ".hb", ".cn", ".hn", ".cn",
            ".gd", ".cn", ".gx", ".cn", ".hi", ".cn", ".sc", ".cn", ".gz",
            ".cn", ".yn", ".cn", ".xz", ".cn", ".sn", ".cn", ".gs", ".cn",
            ".qh", ".cn", ".nx", ".cn", ".xj", ".cn", ".tw", ".cn", ".hk",
            ".cn", ".mo", ".ru", ".net", };
    public static HashMap<String,HashMap<String,Object>> urlPostfixMap = new HashMap<String,HashMap<String,Object>>();
    public static HashMap<String,String> regionalUrlPostfixMap = new HashMap<String,String>();
    public static HashMap<String,String> traditionalUrlPostfixMap = new HashMap<String,String>();
    // 所有顶级域名列表(不带前边的.)
    public static HashMap<String,HashMap<String,Object>> urlPostfixMap_noDot = new HashMap<String,HashMap<String,Object>>();
    // 最初定义的几组业务相关的顶级域名列表(不带前边的.)
    public static HashMap<String,String> regionalUrlPostfixMap_noDot = new HashMap<String,String>();
    // 后来加入的国家级顶级域名列表(不带前边的.)
    public static HashMap<String,String> traditionalUrlPostfixMap_noDot = new HashMap<String,String>();

    static {
        for (int i = 0; i < traditionalUrlPostfix.length; i++) {
            if (traditionalUrlPostfix[i] != null) {
                String temp = traditionalUrlPostfix[i].trim();
                traditionalUrlPostfixMap.put(temp, null);
                urlPostfixMap.put(temp, null);
                if (temp.length() > 0) {
                    temp = temp.substring(1);
                    traditionalUrlPostfixMap_noDot.put(temp, null);
                    urlPostfixMap_noDot.put(temp, null);
                }
            }
        }
        for (int i = 0; i < regionalUrlPostfix.length; i++) {
            if (regionalUrlPostfix[i] != null) {
                String temp = regionalUrlPostfix[i].trim();
                regionalUrlPostfixMap.put(temp, null);
                urlPostfixMap.put(temp, null);
                HashMap<String, Object> obj = (HashMap<String, Object>) urlPostfixMap.get(temp);
                for( String international : internationalTraditionalUrlPostfix ){
                    if( obj == null ){
                        obj = new HashMap<String, Object>();
                        urlPostfixMap.put(temp, obj);
                    }
                    obj.put(international, null);
                }
                if (temp.length() > 0) {
                    temp = temp.substring(1);
                    regionalUrlPostfixMap_noDot.put(temp.substring(1), null);
                    urlPostfixMap_noDot.put(temp, null);
                }
            }
        }
        for (int i = 0; i < fixupPostfix.length && i + 1 < fixupPostfix.length; i += 2) {
            String key = fixupPostfix[i];
            String val = fixupPostfix[i + 1];
            {
                HashMap<String,Object> obj = (HashMap<String,Object>) urlPostfixMap.get(key);
                if (obj == null) {
                    obj = new HashMap<String,Object>();
                    urlPostfixMap.put(key, obj);
                }
                obj.put(val, null);
            }
            if (key.length() > 0 && val.length() > 0) {
                key = key.substring(1);
                val = val.substring(1);
                HashMap<String,Object> obj = (HashMap<String,Object>) urlPostfixMap_noDot.get(key);
                if (obj == null) {
                    obj = new HashMap<String,Object>();
                    urlPostfixMap_noDot.put(key, obj);
                }
                obj.put(val, null);
            }
        }
    }

    public static final String URL_PATH_SEPERATOR = "/";
    public static final String URL_HTTP_HEAD = "http://";
    public static final String URL_DOMAIN_SEPERATOR = ".";

    /**
     * 判定一个字符串是否为URL，并返回归一化后的URL字符串。 归一化规则：1.以http://打头; 2.端口号为80时，要省略; 3.
     * 纯域名时，要加"/"作结尾
     *
     * @param query
     *            检查字符串
     * @return 当结果为URL时，返回归一化的结果，否则返回null。
     */
    public static final String getLookupUrl(String query) {
        String temp = query.trim();
        String domain;
        String filePath = URL_PATH_SEPERATOR;

        String protocalHead = HTTP_PROTOCOL_HEAD;

        String tempLower = temp.toLowerCase();
        if (tempLower.startsWith(HTTP_PROTOCOL_HEAD)) {
            protocalHead = HTTP_PROTOCOL_HEAD;
            temp = temp.substring(HTTP_PROTOCOL_HEAD_LENGTH);
        } else if (tempLower.startsWith(HTTPS_PROTOCOL_HEAD)) {
            protocalHead = HTTPS_PROTOCOL_HEAD;
            temp = temp.substring(HTTPS_PROTOCOL_HEAD_LENGTH);
        }
        int idxSlash = temp.indexOf('/');
        int idxColon = temp.indexOf(':');

        int port = 80;
        if (idxSlash < 0) { // 纯域名
            if (idxColon > 0) {
                try {
                    port = Integer.parseInt(temp.substring(idxColon + 1));
                } catch (NumberFormatException e) {
                    return null;
                }
                domain = temp.substring(0, idxColon);
            } else
                domain = temp;
            filePath = URL_PATH_SEPERATOR;
        } else { // 域名＋目录
            if (idxColon > 0 && idxColon < idxSlash) {
                try {
                    port = Integer.parseInt(temp.substring(idxColon + 1,
                            idxSlash));
                } catch (NumberFormatException e) {
                    return null;
                }
                domain = temp.substring(0, idxColon);
            } else {
                domain = temp.substring(0, idxSlash);
            }
            filePath = temp.substring(idxSlash);
        }
        // 判断 port 是否在合法范围内
        if (port <= 0 || port > 65535) {
            return null;
        }
        // 判断域名部分是否合法

        domain = validateDomain(domain);
        // 确定为URL
        if (domain != null) {

            String result;
            if (port == 80) {
                result = protocalHead + domain + filePath;
            } else {
                result = protocalHead + domain + ':' + port + filePath;
            }
            return result;
        }
        return null;
    }

    public static final boolean isIP(String domain){
        if( domain == null ) return false;

        boolean isValid = false;
        // 判断一：xxx.xxx.xxx.xxx形式的IP地址
        try {
            StringTokenizer token = new StringTokenizer(domain,
                    URL_DOMAIN_SEPERATOR);
            int i;
            for (i = 0; i < 4; i++) {
                int tempInt = Integer.parseInt(token.nextToken());
                if (tempInt < 0 || tempInt > 255)
                    break;
            }
            if (i == 4) {
                if (!token.hasMoreTokens()) {
                    // 验证成功
                    isValid = true;
                }
            }
        } catch (NoSuchElementException e) {
        } catch (NumberFormatException e) {
        }
        return isValid;
    }


    /**
     * 检验域名部分是否符合RFC规范
     *
     * @param domain
     * @return 如果返回null，说明参数不是域名，否则就返回domain自身
     *
     */
    public static final String validateDomain(String domain) {
        if (domain == null)
            return null;

        // 判断零：不含非法字符
        for (int i = 0; i < domain.length(); i++) {

            char c = domain.charAt(i);

            if (c > 0x7f) {
                return null;
            } else if (!isLetterOrDigit(c)) {
                // 域名不能包含符号, 但可以包含'.'或'-'或'_',且不能以这三个符号打头或结尾
                if ((c == '.' && i != 0 && i != domain.length() - 1)
                        || ( (c == '-' || c == '_') && i != 0 && i != domain.length() - 1)) {
                    continue;
                } else {
                    return null;
                }
            }
        }

        boolean isValid = false;
        do{
            if( isIP(domain) ){
                isValid = true;
                break;
            }
            // 否则判断是否满足其他的形式
            {
                isValid = true;
                // 判断二.1：xx.xxxx.com形式的域名(判断字符组成的合法性)
                StringTokenizer token = new StringTokenizer(domain,
                        URL_DOMAIN_SEPERATOR);
                while (token.hasMoreTokens()) {
                    String tok = token.nextToken();
                    if (tok.length() == 0 || tok.startsWith(".")
                            || tok.endsWith(".") || tok.startsWith("-")
                            || tok.endsWith("-") || tok.startsWith("_")
                            || tok.endsWith("_")) {
                        isValid = false;
                        break;
                    }
                }
                if( isValid && domain.indexOf("..") >= 0 )
                    isValid = false;
                // 不满足域名形式，跳出
                if (!isValid)
                    break; // do .. while(false);
            }
            // 判断二：xx.xxxx.com形式的域名(根据后缀判断)
            {
                isValid = false;
                domain = domain.toLowerCase();
                int p = domain.lastIndexOf('.');
                try {
                    String postfix = domain.substring(p);
                    if (urlPostfixMap.containsKey(postfix)) {
                        isValid = true;
                        // 验证成功，跳出不再执行其他的模板判断
                        break; // do .. while(false);
                    }
                } catch (IndexOutOfBoundsException e) {
                }
            }
        }while(false);

        // 确定为URL
        if (isValid) {
            return domain;
        } else {
            return null;
        }
    }

    /**
     * 判断URL是不是域名形式的URL
     *
     * @param url
     *            url必须是符合本类中的URL归一化函数规则的URL
     * @return
     */
    public static final boolean isDomain(String url) {
        int t = 0;
        if (url.startsWith(HTTP_PROTOCOL_HEAD)) {
            t = HTTP_PROTOCOL_HEAD_LENGTH;
        } else if (url.startsWith(HTTPS_PROTOCOL_HEAD)) {
            t = HTTPS_PROTOCOL_HEAD_LENGTH;
        }
        t = url.indexOf('/', t);
        if (t < 0 || t == url.length() - 1)
            return true;
        return false;
    }

    /**
     * 找出url的一级域名 一级域名的格式: [a-z0-9]([a-z0-9\-]*[a-z0-9])?\.{顶级域名} 或
     * [a-z0-9]([a-z0-9\-]*[a-z0-9])?\.{域名商提供的域名}.{顶级域名}
     *
     * @param url
     *            url必须是符合本类中的URL归一化函数规则的URL
     * @return 如果参数不是一个url返回null, 否则返回对应的顶级域名串,如:
     *         "http://www.sogou.com.cn/"返回值是"sogou.com.cn"
     */
    @SuppressWarnings("rawtypes")
    public static final String getMainDomain(String url) {

        String domain = getDomainWithoutPort(url);

        if (domain == null)
            return null;

        HashMap map = urlPostfixMap;
        int lastDot = domain.length();
        int last = lastDot;
        do {
            last = domain.lastIndexOf('.', lastDot - 1);

            // 前边已经没有'.'了
            if (last < 0)
                break;
            // 已经没有第n+1级域名了
            if (map == null)
                break;

            String topDomain = domain.substring(last, lastDot);

            if (!map.containsKey(topDomain))
                break;
            else
                map = (HashMap)map.get(topDomain);
            lastDot = last;
        } while (true);
        if (lastDot == domain.length()) {
            return null; // 没有顶级域名
        } else {
            if (last < 0) { // xxx.com.cn
                return domain;
            } else { // xxx.domain.com.cn
                return domain.substring(last + 1);
            }
        }
    }

    /**
     * 检查url是否为不以www开头的一级域名 一级域名的格式: [a-z0-9]([a-z0-9\-]*[a-z0-9])?\.{任意顶级域名} 或
     * [a-z0-9]([a-z0-9\-]*[a-z0-9])?\.{传统顶级域名}.{地区顶级域名}
     *
     * @param url
     *            url必须是符合本类中的URL归一化函数规则的URL
     * @return
     */
    public static final boolean isNonWWW(String url) {

        String domain = getDomainWithoutPort(url);

        if (domain == null)
            return false;

        String mainDomain = getMainDomain(domain);
        return (mainDomain != null && mainDomain.equals(domain));

    }

    /**
     * 从URL串中获取QueryString串
     *
     * @param url
     *            完整的URL串，包括协议头、域名部分等
     * @return null 如果url参数为null，或url中不含'?'字符
     *         否则根据RFC标准,返回第一个'?'以后，第一个'#'中间的部分作为QueryString串
     */
    public static final String getQueryString(String url) {
        if (url == null)
            return null;
        int index = url.indexOf('?');
        if (index < 0) {
            return null;
        }
        index++;
        int hash = url.indexOf('#', index);
        if (hash < 0) {
            return url.substring(index);
        } else {
            return url.substring(index, hash);
        }
    }

    private static final boolean checkHexChar(byte[] str, int i){
        if( str == null
                || i >= str.length
                ) return false;
        byte ch1 = str[i];
        return (ch1 >= '0' && ch1 <= '9')
                || (ch1 >='a' && ch1 <='f')
                || (ch1 >='A' && ch1 <='F');
    }
    private static final boolean checkMultiHexChar(byte[] str, int idx, int n){
        for(int i=0;i<n;i++){
            if(! checkHexChar(str, idx+i) ) return false;
        }
        return true;
    }
    private static final boolean tryPut(byte[]buff, int idx, byte b){
        if( buff == null || idx < 0 || idx >=buff.length ) return false;
        buff[idx] = b;
        return true;
    }
//	private static final boolean tryMultiPut(byte[]buff, int idx, byte[] b){
//		if( buff == null || b == null || idx < 0 || idx + b.length > buff.length ) return false;
//		System.arraycopy(b, 0, buff, idx, b.length);
//		return true;
//	}



    /**
     * 从url参数，或者带参数的url中截取一个特定的参数，考虑了"#"锚标的情况
     * @param url 原始字符串
     * @param param 需要找的参数
     * @return 找到的对应参数
     *    null 参数不存在，或者参数非法
     */
    public static final String getParameter(String url, String param){
        if( url == null || param == null ) return null;
        String key = param + "=";
        int right = url.indexOf("/#");
        if (right < 0) {
            right = url.indexOf('#');
        } else {
            right = url.indexOf('#', right + 2);
        }
        if( right < 0 ) right = url.length();
        int left = -1;
        while(true){
            int idx = url.indexOf(key, left + 1);
            if( idx < 0 ) {
                return null;
            } else if( idx == 0 ) {
                left = idx;
                break;
            } else if( url.charAt(idx-1) == '?' || url.charAt(idx-1) == '&' || url.charAt(idx-1) == '#'){
                left = idx;
                break;
            } else{
                left = idx ;
            }
        }
        // 未找到
        if( left < 0 ) return null;
        left += key.length();
        // 处理#锚标
        if( left >= right ) return null;

        int end = url.indexOf('&', left+1);
        if( end > 0 && end < right ) right = end;
        return url.substring(left, right);
    }
}
