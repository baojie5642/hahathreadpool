package com.baojie.zk.example.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

public final class IPConfig {

    private static final Logger log = LoggerFactory.getLogger(IPConfig.class);

    private IPConfig() {
        throw new IllegalArgumentException("init");
    }

    private static final String IP_ERROR = "127.0.0.1";
    private static final String NET_INTERFACE_ERROR = "error";
    private static final String BRIAGE = ":";
    private static final boolean IS_WINDOWS;
    private static volatile String IP;
    private static volatile String IP_FACE;

    static {
        boolean isWin = false;
        try {
            String osName = System.getProperty("os.name");
            if (null == osName) {
                throw new IOException("os.name not found");
            }
            osName = osName.toLowerCase(Locale.ENGLISH);
            if (osName.contains("windows")) {
                isWin = true;
            } else if (osName.contains("linux") || osName.contains("mpe/ix") || osName.contains("freebsd")
                    || osName.contains("irix") || osName.contains("digital unix") || osName.contains("unix")
                    || osName.contains("mac os x")) {
                isWin = false;
            }
        } catch (Throwable ex) {
            log.error(ex.toString(), ex);
            throw new IllegalStateException("os.name not found");
        }
        IS_WINDOWS = isWin;
    }

    public static String ip() {
        String ip = IP;
        if (null != ip) {
            return ip.trim();
        } else {
            String ipf = ipFaceName();
            if (null != ipf) {
                return split(ipf);
            } else {
                return IP_ERROR;
            }
        }
    }

    private static String split(String ipf) {
        String ip = IP;
        String ip_split = firstLocation(ipf);
        if (ip == null) {
            synchronized (IPConfig.class) {
                if (null == IP) {
                    IP = ip_split;
                }
            }
            ip = IP;
            if (null != ip) {
                return ip.trim();
            } else {
                return IP_ERROR;
            }
        } else {
            return ip.trim();
        }
    }

    private static String firstLocation(String ipf) {
        try {
            String[] ipn = ipf.split(BRIAGE);
            return ipn[0];
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return IP_ERROR;
    }

    public static String ipFaceName() {
        String ipf = IP_FACE;
        if (null != ipf) {
            return ipf.trim();
        } else {
            synchronized (IPConfig.class) {
                if (null == IP_FACE) {
                    IP_FACE = ipSwitch();
                }
            }
            ipf = IP_FACE;
            if (null != ipf) {
                return ipf.trim();
            } else {
                return join(IP_ERROR, NET_INTERFACE_ERROR);
            }
        }
    }

    private static String ipSwitch() {
        try {
            if (IS_WINDOWS) {
                return winIPFaceName();
            } else {
                return linuxIPFaceName();
            }
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return join(IP_ERROR, NET_INTERFACE_ERROR);
    }

    private static String winIPFaceName() {
        InetAddress add = winAdd();
        if (null != add) {
            String ip = add.getHostAddress();
            NetworkInterface ni = winFaceName(add);
            if (null != ni) {
                String nin = ni.getName();
                return join(ip, nin);
            } else {
                return join(ip, NET_INTERFACE_ERROR);
            }
        } else {
            return join(IP_ERROR, NET_INTERFACE_ERROR);
        }
    }

    private static String join(String ip, String nin) {
        final StringBuilder sbu = new StringBuilder();
        sbu.append(ip);
        sbu.append(BRIAGE);
        sbu.append(nin);
        return sbu.toString();
    }

    private static InetAddress winAdd() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException ue) {
            log.error(ue.toString(), ue);
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return null;
    }

    private static NetworkInterface winFaceName(InetAddress add) {
        try {
            return NetworkInterface.getByInetAddress(add);
        } catch (SocketException se) {
            log.error(se.toString(), se);
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return null;
    }

    private static String linuxIPFaceName() {
        final List<String> faceList = new ArrayList<>();
        Enumeration<NetworkInterface> allFace = allFace();
        if (null == allFace) {
            return join(IP_ERROR, NET_INTERFACE_ERROR);
        } else {
            check(allFace, faceList);
        }
        final int size = faceList.size();
        if (size > 1) {
            log.warn("windows or linux should be never happen.");
        }
        if (0 == size) {
            log.error("ips and net interfaces must not be empty in list.");
            return join(IP_ERROR, NET_INTERFACE_ERROR);
        }
        return faceList.get(0);
    }

    private static void check(Enumeration<NetworkInterface> allFace, List<String> faceList) {
        while (allFace.hasMoreElements()) {
            NetworkInterface ni = oneFace(allFace);
            if (null == ni) {
                continue;
            } else if (isLoopback(ni)) {
                continue;
            } else if (isVisual(ni)) {
                continue;
            } else if (!isUp(ni)) {
                continue;
            } else {
                forEach(ni, faceList);
            }
        }
    }

    private static void forEach(NetworkInterface nif, List<String> faceList) {
        Enumeration<InetAddress> add = addEnum(nif);
        String faceName = nif.getName();
        while (add.hasMoreElements()) {
            InetAddress iadd = address(add);
            if (null == iadd) {
                continue;
            } else if (!iadd.isSiteLocalAddress()) {
                continue;
            } else if (iadd instanceof Inet6Address) {
                continue;
            } else {
                fill(iadd, faceName, faceList);
            }
        }
    }

    private static Enumeration<InetAddress> addEnum(NetworkInterface nif) {
        try {
            return nif.getInetAddresses();
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return null;
    }

    private static void fill(InetAddress iadd, String faceName, List<String> faceList) {
        String ip = iadd.getHostAddress();
        if (null == ip) {
            return;
        } else {
            if ((ip.contains(".")) && (!ip.contains(":"))) {
                if ((!ip.contains("::")) && (!ip.contains("0:0:")) && (!ip.contains("fe80"))) {
                    faceList.add(join(ip.trim(), faceName));
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static Enumeration<NetworkInterface> allFace() {
        try {
            return NetworkInterface.getNetworkInterfaces();
        } catch (SocketException se) {
            log.error(se.toString(), se);
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return null;
    }

    private static boolean isLoopback(NetworkInterface ni) {
        try {
            return ni.isLoopback();
        } catch (SocketException se) {
            log.error(se.toString(), se);
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return true;
    }

    private static boolean isVisual(NetworkInterface ni) {
        try {
            return ni.isVirtual();
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return true;
    }

    private static boolean isUp(NetworkInterface ni) {
        try {
            return ni.isUp();
        } catch (SocketException se) {
            log.error(se.toString(), se);
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return false;
    }

    private static InetAddress address(Enumeration<InetAddress> address) {
        if (null == address) {
            return null;
        }
        try {
            return address.nextElement();
        } catch (NoSuchElementException ne) {
            log.error(ne.toString(), ne);
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return null;
    }

    private static NetworkInterface oneFace(Enumeration<NetworkInterface> allFace) {
        try {
            return allFace.nextElement();
        } catch (NoSuchElementException ne) {
            log.error(ne.toString(), ne);
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
        return null;
    }

}
