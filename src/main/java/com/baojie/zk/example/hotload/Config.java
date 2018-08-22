package com.baojie.zk.example.hotload;

import com.baojie.zk.example.ip.IPConfig;
import com.baojie.zk.example.sleep.Sleep;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private static final String DEFAULT_PROXY_IP = "127.0.0.1";
    private static final String DEFAULT_PROXY_PORT = "3128";

    @HotReloadable("${page.query.page.size}")
    private static volatile int pageSize = 1000;   //默认1000条

    @HotReloadable("${url.checkOCSUser}")
    private static volatile String ocsUrl = "base";

    @HotReloadable("${url.jituanCalculate}")
    private static volatile String jtcalcu = "base";

    @HotReloadable("${url.jituanCharge}")
    private static volatile String jtcharge = "base";

    @HotReloadable("${url.jituanOrderCheck}")
    private static volatile String jtcheck = "base";

    @HotReloadable("${url.oppinfobyprodnum}")
    private static volatile String oppinfo = "base";

    @HotReloadable("${url.queryOutOfPkg}")
    private static volatile String outofpack = "base";

    @HotReloadable("${url.blackwhite.pushmsg}")
    private static volatile String blackwhite = "base";

    @HotReloadable("${url.queryBPnbrList}")
    private static volatile String bpnbr = "base";

    @HotReloadable("${url.dljsinfo}")
    private static volatile String dljs = "base";

    @HotReloadable("${url.queryHistoryOwe}")
    private static volatile String historyowefee = "base";

    @HotReloadable("${url.orderNumExternalOrderNum}")
    private static volatile String externalOrder = "base";

    @HotReloadable("${url.queryOwe}")
    private static volatile String owefee = "base";

    @HotReloadable("${url.queryprepayamount}")
    private static volatile String prepay = "base";

    @HotReloadable("${url.roamFeeDat}")
    private static volatile String roamfeedat = "base";

    @HotReloadable("${url.roamLocationStatus}")
    private static volatile String roamlocal = "base";

    @HotReloadable("${url.subassetattrinfo}")
    private static volatile String subasset = "base";

    @HotReloadable("${url.validateInBuilding}")
    private static volatile String inbuilding = "base";

    @HotReloadable("${url.zdtx.prom.asset}")
    private static volatile String zdtxasset = "base";

    @HotReloadable("${wx.token.ttl}")
    private static volatile long wxtokenttl = 6200;
    @HotReloadable("${wx.token.appid}")
    private static volatile String wxappid = "";
    @HotReloadable("${wx.token.secret}")
    private static volatile String wxsecret = "";

    @HotReloadable("${monitor.comp.name}")
    private static volatile String comp = "comp_name";

    @HotReloadable("${monitor.api.green.name}")
    private static volatile String apigreen = "green";
    @HotReloadable("${monitor.api.validate.name}")
    private static volatile String validate = "validate";

    @HotReloadable("${open.api.proxy}")
    private static volatile String proxy = "off";
    @HotReloadable("${api.proxy.ip}")
    private static volatile String proxyIp = DEFAULT_PROXY_IP;
    @HotReloadable("${api.proxy.port}")
    private static volatile String proxyPort = DEFAULT_PROXY_PORT;

    private final ConfigMonitor monitor = new ConfigMonitor("cnapi_config_monitor");
    private final ReentrantLock mainLock = new ReentrantLock();
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final Semaphore signal = new Semaphore(1);
    private final PropertiesManager pm;
    private final ResourceHandler rh;

    @Autowired
    public Config(ResourceHandler reh) {
        if (null == reh) {
            throw new NullPointerException();
        }
        init();
        this.rh = reh;
        this.pm = new PropertiesManager(rh);
        this.pm.startHotLoad(signal);
        flushMillis(1);
        this.monitor.keep();
        signal.release(1);
        log.info("cnapi_config_manager is working");
    }

    private void init() {
        try {
            signal.acquire(1);
        } catch (InterruptedException e) {
            log.error(e.toString(), e);
            throw new IllegalStateException();
        } catch (Throwable t) {
            log.error(t.toString(), t);
            throw new IllegalStateException();
        }
    }

    private void flushMillis(long millis) {
        Thread.yield();
        Sleep.locksptSleep(TimeUnit.MILLISECONDS, millis);
        Thread.yield();
    }

    @PreDestroy
    public void destory() {
        if (stop.get()) {
            return;
        } else {
            if (stop.compareAndSet(false, true)) {
                shutDown();
                log.info("cnapi_monitor_config_monitor stop completed");
            } else {
                return;
            }
        }
    }

    private void shutDown() {
        try {
            pm.destory();
        } finally {
            signal.release(1);
        }
    }

    private void print() {
        flushMillis(30);
        final ReentrantLock lock = mainLock;
        lock.lock();
        try {
            log.debug("/*****************************  start print config info  *******************************/");
            log.debug("get page size=" + pageSize());
            log.debug("getOcsUrl=" + getOcsUrl());
            log.debug("getJtcalcu=" + getJtcalcu());
            log.debug("getJtcharge=" + getJtcharge());
            log.debug("getJtcheck=" + getJtcheck());
            log.debug("getOppinfo=" + getOppinfo());
            log.debug("getOutofpack=" + getOutofpack());
            log.debug("getBlackwhite=" + getBlackwhite());
            log.debug("getBpnbr=" + getBpnbr());
            log.debug("getDljs=" + getDljs());
            log.debug("getHistoryowefee=" + getHistoryowefee());
            log.debug("getExternalOrder=" + getExternalOrder());
            log.debug("getOwefee=" + getOwefee());
            log.debug("getPrepay=" + getPrepay());
            log.debug("getRoamfeedat=" + getRoamfeedat());
            log.debug("getRoamlocal=" + getRoamlocal());
            log.debug("getSubasset=" + getSubasset());
            log.debug("getInbuilding=" + getInbuilding());
            log.debug("getWxappid=" + getWxappid());
            log.debug("getWxtokenttl=" + getWxtokenttl());
            log.debug("getWxsecret=" + getWxsecret());
            log.debug("getZdtxasset=" + getZdtxasset());
            log.debug("getComp=" + getComp());
            log.debug("getApigreen=" + getApigreen());
            log.debug("getValidate=" + getValidate());
            log.debug("get IP=" + IPConfig.ip());
            log.debug("ipFaceName=" + IPConfig.ipFaceName());
            log.debug("open proxy=" + Config.getProxy());
            log.debug("proxy ip=" + Config.getProxyIp());
            log.debug("proxy port=" + Config.getProxyPort());
            log.debug("/*****************************  finish print config info  *******************************/");
        } finally {
            lock.unlock();
        }
    }

    public static final int pageSize() {
        int ps = pageSize;
        if (0 >= ps) {
            return 1000;
        } else {
            return ps;
        }
    }

    public static final String getOcsUrl() {
        String val = ocsUrl;
        if (null == val) {
            error("ocs url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getJtcalcu() {
        String val = jtcalcu;
        if (null == val) {
            error("jtcalcu url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getJtcharge() {
        String val = jtcharge;
        if (null == val) {
            error("jtcharge url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getJtcheck() {
        String val = jtcheck;
        if (null == val) {
            error("jtcheck url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getOppinfo() {
        String val = oppinfo;
        if (null == val) {
            error("oppinfo url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getOutofpack() {
        String val = outofpack;
        if (null == val) {
            error("outofpack url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getBlackwhite() {
        String val = blackwhite;
        if (null == val) {
            error("blackwhite url");
            return "";
        } else {
            return val.trim();
        }
    }


    public static final String getBpnbr() {
        String val = bpnbr;
        if (null == val) {
            error("bpnbr url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getDljs() {
        String val = dljs;
        if (null == val) {
            error("dljs url");
            return "";
        } else {
            return val.trim();
        }
    }


    public static final String getHistoryowefee() {
        String val = historyowefee;
        if (null == val) {
            error("historyowefee url");
            return "";
        } else {
            return val.trim();
        }
    }


    public static final String getExternalOrder() {
        String val = externalOrder;
        if (null == val) {
            error("externalOrder url");
            return "";
        } else {
            return val.trim();
        }
    }


    public static final String getOwefee() {
        String val = owefee;
        if (null == val) {
            error("owefee url");
            return "";
        } else {
            return val.trim();
        }
    }


    public static final String getPrepay() {
        String val = prepay;
        if (null == val) {
            error("prepay url");
            return "";
        } else {
            return val.trim();
        }
    }


    public static final String getRoamfeedat() {
        String val = roamfeedat;
        if (null == val) {
            error("roamfeedat url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getRoamlocal() {
        String val = roamlocal;
        if (null == val) {
            error("roamlocal url");
            return "";
        } else {
            return val.trim();
        }
    }


    public static final String getSubasset() {
        String val = subasset;
        if (null == val) {
            error("subasset url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getInbuilding() {
        String val = inbuilding;
        if (null == val) {
            error("inbuilding url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final String getWxappid() {
        String val = wxappid;
        if (null == val) {
            error("wxappid url");
            return "";
        } else {
            return val.trim();
        }
    }

    public static final long getWxtokenttl() {
        long val = wxtokenttl;
        if (val <= 0) {
            error("wxtokenttl url");
            return 6200;
        } else {
            return val;
        }
    }


    public static final String getWxsecret() {
        String val = wxsecret;
        if (null == val) {
            error("wxsecret url");
            return "";
        } else {
            return val.trim();
        }
    }


    public static final String getZdtxasset() {
        String val = zdtxasset;
        if (null == val) {
            error("zdtxasset url");
            return "";
        } else {
            return val.trim();
        }
    }

    /*******************************************************************/

    public static final String getProxy() {
        String p = proxy;
        if (null == p) {
            error("not in config file");
            return "off";
        } else {
            return p.trim();
        }
    }

    public static final String getProxyIp() {
        String p = proxyIp;
        if (null == p) {
            error("not in config file");
            return DEFAULT_PROXY_IP;
        } else {
            return p.trim();
        }
    }

    public static String getProxyPort() {
        String port = proxyPort;
        if (null == port) {
            error("error proxy port null");
            return DEFAULT_PROXY_PORT;
        } else {
            port = port.trim();
            if (!StringUtils.isNumeric(port)) {
                error("not num proxy port=" + port);
                return DEFAULT_PROXY_PORT;
            } else {
                return port;
            }
        }
    }

    /*******************************************************************/

    public static final String getComp() {
        String val = comp;
        if (null == val) {
            error("comp url");
            return "dataflow-ill";
        } else {
            return val.trim();
        }
    }

    public static final String getApigreen() {
        String val = apigreen;
        if (null == val) {
            error("apigreen url");
            return "greenChannel";
        } else {
            return val.trim();
        }
    }

    public static final String getValidate() {
        String val = validate;
        if (null == val) {
            error("validate url");
            return "validateCampaign";
        } else {
            return val.trim();
        }
    }

    private static final void error(String error) {
        log.error(error + ", in config get null.");
    }

    /*********************************************************/

    private final class ConfigMonitor extends KeepVolatile4Config {

        public ConfigMonitor(String name) {
            super(name);
        }

        @Override
        public void work() {
            try {
                for (; ; ) {
                    acquire();
                    if (stop.get()) {
                        return;
                    } else {
                        print();
                    }
                }
            } finally {
                log.debug(name + " has stopped");
            }
        }

        private void acquire() {
            if (stop.get()) {
                return;
            }
            try {
                signal.acquire(1);
            } catch (InterruptedException e) {
                error(e);
            } catch (Throwable t) {
                error(t);
            }
        }

        private void error(Throwable t) {
            if (!stop.get()) {
                log.error(t.toString(), t);
                // 如果没有显示的中断，那么擦除中断状态
                Thread.currentThread().interrupted();
            }
        }

    }

    private abstract class KeepVolatile4Config implements Runnable {

        protected final Logger log = LoggerFactory.getLogger(KeepVolatile4Config.class);
        private final CountDownLatch latch = new CountDownLatch(1);
        protected final Thread thread;
        protected final String name;

        protected KeepVolatile4Config(String name) {
            if (null == name || name.length() == 0) {
                throw new IllegalStateException();
            }
            this.name = name;
            this.thread = new Thread(this, name);
        }

        public void keep() {
            try {
                thread.start();
            } finally {
                latch.countDown();
            }
        }

        public void work() {
            throw new IllegalArgumentException();
        }

        @Override
        public void run() {
            try {
                waitLatch();
            } finally {
                work();
            }
        }

        private void waitLatch() {
            boolean suc = false;
            try {
                suc = latch.await(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.error(e.toString(), e);
            } catch (Throwable t) {
                log.error(t.toString(), t);
            }
            if (suc) {
                return;
            } else {
                throw new Error("prop monitor latch");
            }
        }
    }

}
