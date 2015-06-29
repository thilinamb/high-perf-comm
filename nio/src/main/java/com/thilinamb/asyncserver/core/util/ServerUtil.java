package com.thilinamb.asyncserver.core.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Thilina Buddhika
 */
public class ServerUtil {

    public static InetAddress getHostInetAddress() {
        InetAddress inetAddr;
        try {
            inetAddr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            inetAddr = InetAddress.getLoopbackAddress();
        }
        return inetAddr;
    }

}
