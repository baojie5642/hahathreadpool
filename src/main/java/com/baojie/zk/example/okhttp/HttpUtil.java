package com.baojie.zk.example.okhttp;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.CharArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class HttpUtil {

    private static Logger log = LoggerFactory.getLogger(HttpUtil.class);

    private static int CONN_TIMEOUT = 30 * 1000;
    private static int SO_TIMEOUT = 60 * 1000;

    private static String HTTP_ERROR = "HTTPUtil Error";

    public static Map<String, String> httpGetWithResponseMap(String url, Map<String, String> headerParams,
            boolean... allowRedirects) {
        Map<String, String> resultMap = new HashMap<String, String>();

        HttpURLConnection conn = getHttpURLConnection(url, headerParams);
        try {
            if (allowRedirects.length > 0) {
                conn.setInstanceFollowRedirects(allowRedirects[0]);
            } else {
                conn.setInstanceFollowRedirects(false);
            }
            conn.setReadTimeout(SO_TIMEOUT);
            conn.setConnectTimeout(CONN_TIMEOUT);
            conn.setUseCaches(false); // 不允许使用
            conn.setRequestMethod("GET"); // 请求方式
            conn.connect();

            InputStream inputStream = conn.getInputStream();

            String contentType = "";
            try {
                contentType = conn.getContentType();
            } catch (Exception e) {
            }
            String charset = getContentType(contentType);

            int contentLength = conn.getContentLength();
            String contentEncoding = conn.getContentEncoding();
            Map<String, List<String>> headers = conn.getHeaderFields();
            int statusCode = conn.getResponseCode();
            String html = getHTML(inputStream, contentLength, contentEncoding, charset);
            resultMap.put("status", statusCode + "");

            resultMap.put("html", html);
            Set<String> headerFieldKeys = headers.keySet();

            for (String key : headerFieldKeys) {
                resultMap.put(key, headers.get(key).get(0));
            }

            return resultMap;
        } catch (Exception e) {
            throw new RuntimeException(HTTP_ERROR + ":" + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {
            }
        }
    }

    public static Map<String, String> httpPostWithResponseMap(String url, List<BasicNameValuePair> paramPair,
            Map<String, String> headerParams, boolean... allowRedirects) {
        String params = "";
        for (BasicNameValuePair pair : paramPair) {
            try {
                params += pair.getName() + "=" + URLEncoder.encode(pair.getValue(), "UTF-8") + "&";
            } catch (Exception e) {
            }
        }
        return httpPostWithResponseMap(url, params, headerParams, allowRedirects);
    }

    public static Map<String, String> httpPostWithResponseMap(String url, String params,
            Map<String, String> headerParams, boolean... allowRedirects) {

        Map<String, String> resultMap = new HashMap<String, String>();
        HttpURLConnection conn = getHttpURLConnection(url, headerParams);
        try {
            if (allowRedirects.length > 0) {
                conn.setInstanceFollowRedirects(allowRedirects[0]);
            } else {
                conn.setInstanceFollowRedirects(false);
            }
            conn.setReadTimeout(SO_TIMEOUT);
            conn.setConnectTimeout(CONN_TIMEOUT);
            conn.setDoInput(true); //
            conn.setDoOutput(true); //
            conn.setUseCaches(false); // 不允许使用
            conn.setRequestMethod("POST"); // 请求方式

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(params);
            writer.flush();
            conn.connect();

            InputStream inputStream = conn.getInputStream();

            String contentType = "";
            try {
                contentType = conn.getContentType();
            } catch (Exception e) {
            }
            String charset = getContentType(contentType);

            int contentLength = conn.getContentLength();
            String contentEncoding = conn.getContentEncoding();
            Map<String, List<String>> headers = conn.getHeaderFields();
            int statusCode = conn.getResponseCode();
            String html = getHTML(inputStream, contentLength, contentEncoding, charset);
            resultMap.put("status", statusCode + "");
            resultMap.put("html", html);
            Set<String> headerFieldKeys = headers.keySet();
            for (String key : headerFieldKeys) {
                resultMap.put(key, headers.get(key).get(0));
            }
            return resultMap;
        } catch (Exception e) {
            throw new RuntimeException(HTTP_ERROR + ":" + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {
            }
        }
    }

    public static String httpGet(boolean proxy, String _url, Map<String, String> headerParams, boolean...
            allowRedirects) {

        HttpURLConnection conn = getConnection(proxy, _url, headerParams);
        try {
            if (allowRedirects.length > 0) {
                conn.setInstanceFollowRedirects(allowRedirects[0]);
            } else {
                conn.setInstanceFollowRedirects(true);
            }
            conn.setReadTimeout(SO_TIMEOUT);
            conn.setConnectTimeout(CONN_TIMEOUT);
            conn.setUseCaches(false); // 不允许使用
            conn.setRequestMethod("GET"); // 请求方式
            conn.connect();

            int statusCode = conn.getResponseCode();

            InputStream inputStream = null;
            if (statusCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }

            String contentType = "";
            try {
                contentType = conn.getContentType();
            } catch (Exception e) {
            }
            String charset = getContentType(contentType);

            int contentLength = conn.getContentLength();
            String contentEncoding = conn.getContentEncoding();
            return getHTML(inputStream, contentLength, contentEncoding, charset);
        } catch (Exception e) {
            throw new RuntimeException(HTTP_ERROR + ":" + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {
            }
        }
    }

    public static String httpPost(boolean proxy, String _url, List<BasicNameValuePair> paramPair,
            Map<String, String> headerParams,
            boolean... allowRedirects) {
        String params = "";
        for (BasicNameValuePair pair : paramPair) {
            try {
                params += pair.getName() + "=" + URLEncoder.encode(pair.getValue(), "UTF-8") + "&";
            } catch (Exception e) {
            }
        }

        return httpPost(false, _url, params, headerParams, allowRedirects);
    }

    public static String httpPost(boolean proxy, String _url, Map<String, String> paramPair,
            Map<String, String> headerParams,
            boolean... allowRedirects) {
        String params = "";

        Set<String> keys = paramPair.keySet();
        for (String key : keys) {
            try {
                params += key + "=" + URLEncoder.encode(paramPair.get(key), "UTF-8") + "&";
            } catch (Exception e) {
            }
        }
        return httpPost(false, _url, params, headerParams, allowRedirects);
    }

    // 暂时这样修改，后续需要同意重构，因为这种需要制定的
    public static String httpPost(boolean proxy, String _url, String params, Map<String, String> headerParams,
            boolean... allowRedirects) {
        HttpURLConnection conn = getConnection(proxy, _url, headerParams);
        try {
            if (allowRedirects.length > 0) {
                conn.setInstanceFollowRedirects(allowRedirects[0]);
            } else {
                conn.setInstanceFollowRedirects(false);
            }

            conn.setReadTimeout(SO_TIMEOUT);
            conn.setConnectTimeout(CONN_TIMEOUT);
            conn.setDoInput(true); // 允许输
            conn.setDoOutput(true); // 允许输
            conn.setUseCaches(false); // 不允许使用
            conn.setRequestMethod("POST"); // 请求方式
            conn.setChunkedStreamingMode(-1);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(params);
            writer.flush();
            conn.connect();

            int statusCode = conn.getResponseCode();

            InputStream inputStream = null;
            if (statusCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }

            String contentType = "";
            try {
                contentType = conn.getContentType();
            } catch (Exception e) {
            }
            String charset = getContentType(contentType);

            int contentLength = conn.getContentLength();
            String contentEncoding = conn.getContentEncoding();
            return getHTML(inputStream, contentLength, contentEncoding, charset);
        } catch (Exception e) {
            throw new RuntimeException(HTTP_ERROR + ":" + e.toString(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {
            }
        }
    }

    private static HttpURLConnection getConnection(boolean proxy, String _url, Map<String, String> headerParams) {
        if (proxy) {
            return proxyConnection(_url, headerParams);
        } else {
            return getHttpURLConnection(_url, headerParams);
        }
    }

    private static HttpURLConnection proxyConnection(String _url, Map<String, String> headerParams) {
        return getHttpURLConnection(_url, headerParams);
    }

    // ------------------------------------------------------------
    private static HttpURLConnection getHttpURLConnection(String _url, Map<String, String> headerParams,
            String... proxyParams) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(_url);
            boolean ssl = false;
            if (_url.toLowerCase().startsWith("https")) {
                ssl = true;
            }

            if (!ssl) {
                if (proxyParams.length == 2) {
                    InetSocketAddress sa = new InetSocketAddress(proxyParams[0], Integer.valueOf(proxyParams[1]));

                    Proxy proxy = new Proxy(Proxy.Type.HTTP, sa);
                    conn = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
            } else {
                HttpsURLConnection.setDefaultSSLSocketFactory(getSslSocketFactory());
                HostnameVerifier allHostsValid = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

                if (proxyParams.length == 2) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP,
                            new InetSocketAddress(proxyParams[0], Integer.valueOf(proxyParams[1])));
                    conn = (HttpsURLConnection) url.openConnection(proxy);
                } else {
                    conn = (HttpsURLConnection) url.openConnection();
                }
            }

            if (headerParams != null) {
                Set<String> keys = headerParams.keySet();
                for (String key : keys) {
                    conn.setRequestProperty(key, headerParams.get(key));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(HTTP_ERROR + ":" + e.getMessage(), e);
        }
        return conn;
    }

    private static SSLSocketFactory getSslSocketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getContentType(String contentType) {
        try {
            if (!StringUtils.isBlank(contentType)) {
                contentType = contentType.toUpperCase();
                if (contentType.startsWith("TEXT")) {
                    String[] element = contentType.split(";");
                    if (element.length == 2) {
                        String charset = element[1].replace("CHARSET=", "").trim();
                        return charset.replace("\"", "").replace("\'", "");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(HTTP_ERROR, e);
        }
        return "UTF-8";
    }

    private static String getHTML(InputStream inputStream, int contentLength, String contentEncoding, String charset) {

        log.debug("contentLength=" + contentLength + ", contentEncoding=" + contentEncoding + ", charset=" + charset);

        BufferedReader br = null;
        try {
            if (!StringUtils.isBlank(contentEncoding) && contentEncoding.indexOf("gzip") > 0) {
                br = new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream), charset));
            } else {
                br = new BufferedReader(new InputStreamReader(inputStream, charset));
            }

            if (contentLength < 0) {
                contentLength = 4096;
            }
            CharArrayBuffer buffer = new CharArrayBuffer(contentLength);
            char[] tmp = new char[1024];
            int l;
            while ((l = br.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
            return buffer.toString();
        } catch (Exception e) {
            throw new RuntimeException(HTTP_ERROR + ":" + e.getMessage(), e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
            }
        }

    }
}
