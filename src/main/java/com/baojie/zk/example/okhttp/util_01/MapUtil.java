package com.baojie.zk.example.okhttp.util_01;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MapUtil {
    private static final Logger log = LoggerFactory.getLogger(MapUtil.class);

    private MapUtil() {
        throw new IllegalArgumentException();
    }

    public static String getValue(String key, Map<String, String> map) {
        if (null == map) {
            log.error("map null, key=" + key);
            return "";
        }
        if (null == key) {
            log.error("key null");
            return "";
        }
        String value = get(key, map);
        if (null == value) {
            log.error("get from map null, key=" + key + ", value null");
            return "";
        } else {
            return value;
        }
    }

    private static String get(String key, Map<String, String> map) {
        try {
            return map.get(key);
        } catch (ClassCastException e) {
            log.error(e.toString() + ", key=" + key, e);
        } catch (NullPointerException e) {
            log.error(e.toString() + ", key=" + key, e);
        } catch (Exception e) {
            log.error(e.toString() + ", key=" + key, e);
        } catch (Throwable e) {
            log.error(e.toString() + ", key=" + key, e);
        }
        return null;
    }

    public static Map<String, String> jsonGet() {
        final Map<String, String> map = new HashMap<>(2);
        map.put("Content-Type", "application/json; charset=utf-8");
        return map;
    }

    public static Map<String, String> jsonPost() {
        return MapUtil.jsonGet();
    }

    public static Map<String, String> blankGet(){
        return new HashMap<>(1);
    }

    public static Map<String, String> basePost(String sa) {
        final Map<String, String> hp = new HashMap<>(8);
        hp.put("Content-Type", "text/xml; charset=utf-8");
        hp.put("Accept", "application/soap+xml,application/dime,multipart/related, text/*");
        // 默认采用gzip压缩，如果显示的set
        // 那么okhttp会认为用户会自己解析，所以用户忘记解析会出错
        // 所以直接采用默认，将解压缩的动作交给okhttp
        //hp.put("Accept-Encoding", "gzip");
        if (!StringUtils.isBlank(sa)) {
            hp.put("SOAPAction", sa);
        }
        return hp;
    }

}
