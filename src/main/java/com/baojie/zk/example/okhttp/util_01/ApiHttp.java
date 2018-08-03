package com.baojie.zk.example.okhttp.util_01;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okio.Okio;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;

@Service
public class ApiHttp {
    private static final Logger log = LoggerFactory.getLogger(ApiHttp.class);
    private static final int DEFAULT_PORT = 3128;

    private final OkHttpClient client;

    @Autowired
    public ApiHttp() {
        this.client = OkHttpUtil.client();
    }

    @PreDestroy
    private void destory() {
        OkClose.close(client);
    }

    public String basePost(boolean proxy, String url, String xml, String sa) {
        if (null == url) {
            log.error("url null, xml=" + xml + ", soap action=" + sa);
            return "";
        } else if (null == xml) {
            log.error("xml null, url=" + url + ", soap action=" + sa);
            return "";
        } else {
            return base(proxy, url, xml, sa);
        }
    }

    private String base(boolean proxy, String url, String xml, String sa) {
        BaseSoapPost sp = new BaseSoapPost(xml);
        Headers hs = Headers.of(MapUtil.basePost(sa));
        Request req = new Request.Builder().url(url).headers(hs).post(sp).build();
        Response resp = switchCall(proxy, req);
        return shiftResp(resp, req);
    }

    private String shiftResp(Response resp, Request req) {
        try {
            if (null == resp) {
                log.error("resp null, req=" + req);
                return "";
            } else {
                String ct = resp.header("Content-Type");
                Charset charset = charset(ct);
                return string(resp, charset);
            }
        } finally {
            Util.closeQuietly(resp);
        }
    }

    private Response switchCall(boolean proxy, Request req) {
        if (!proxy) {
            return call(req, client);
        } else {
            if ("on".equals("off")) {
                final OkHttpClient temp = proxyClient("127.0.0.1", "8080");
                return call(req, temp);
            } else {
                return call(req, client);
            }
        }
    }

    private OkHttpClient proxyClient(String ip, String port) {
        int p;
        if (StringUtils.isNumeric(port)) {
            p = Integer.valueOf(port);
        } else {
            p = DEFAULT_PORT;
        }
        InetSocketAddress address = new InetSocketAddress(ip, p);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
        return client.newBuilder().proxy(proxy).build();
    }

    private Response call(Request req, OkHttpClient okcl) {
        try {
            return okcl.newCall(req).execute();
        } catch (IOException ie) {
            log.error(ie.toString() + ", request=" + req.toString(), ie);
        } catch (Throwable te) {
            log.error(te.toString() + ", request=" + req.toString(), te);
        }
        return null;
    }

    private Charset charset(String ct) {
        if (null == ct) {
            return Util.UTF_8;
        } else if (!ct.contains(";")) {
            return Util.UTF_8;
        } else {
            String cs[] = ct.split(";");
            if (cs.length <= 1) {
                return Util.UTF_8;
            } else {
                String type = cs[1];
                if (null == type) {
                    return Util.UTF_8;
                } else if (!type.contains("charset=")) {
                    return Util.UTF_8;
                } else {
                    String real = StringUtils.replace(type, "charset=", "");
                    if (null == real) {
                        return Util.UTF_8;
                    } else {
                        return forName(real.trim());
                    }
                }
            }
        }
    }

    private Charset forName(String real) {
        try {
            return Charset.forName(real);
        } catch (Throwable te) {
            log.error(te.toString() + ", charset=" + real + "_end", te);
        }
        return Util.UTF_8;
    }

    private String string(Response resp, Charset charset) {
        try {
            return Okio.buffer(resp.body().source()).readString(charset);
        } catch (IOException ie) {
            log.error(ie.toString() + ", resp=" + resp.toString(), ie);
        } catch (Throwable te) {
            log.error(te.toString() + ", resp=" + resp.toString(), te);
        }
        return "";
    }

    public String jsonPost(boolean proxy, String url, String json) {
        JsonPost sp = new JsonPost(json);
        Headers hs = Headers.of(MapUtil.jsonPost());
        Request req = new Request.Builder().url(url).headers(hs).post(sp).build();
        Response resp = switchCall(proxy, req);
        return shiftResp(resp, req);
    }

    public String normalGet(boolean proxy, String url) {
        Headers hs = Headers.of(MapUtil.jsonGet());
        Request req = new Request.Builder().url(url).headers(hs).get().build();
        Response resp = switchCall(proxy, req);
        return shiftResp(resp, req);
    }

}
