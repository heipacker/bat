package com.dlmu.bat.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.TreeSet;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public class OSUtils {

    private static final Logger logger = LoggerFactory.getLogger(OSUtils.class);

    public static String getProcessName() {
        String cmdLine = System.getProperty("sun.java.command");
        if (cmdLine != null && !cmdLine.isEmpty()) {
            String fullClassName = cmdLine.split("\\s+")[0];
            String[] classParts = fullClassName.split("\\.");
            cmdLine = classParts[classParts.length - 1];
        }
        return (cmdLine == null || cmdLine.isEmpty()) ? "Unknown" : cmdLine;
    }

    /**
     * <p>Get the best IP address that represents this node.</p>
     * <p>
     * This is complicated since nodes can have multiple network interfaces,
     * and each network interface can have multiple IP addresses.  What we're
     * looking for here is an IP address that will serve to identify this node
     * to HTrace.  So we prefer site-local addresess (i.e. private ones on the
     * LAN) to publicly routable interfaces.  If there are multiple addresses
     * to choose from, we select the one which comes first in textual sort
     * order.  This should ensure that we at least consistently call each node
     * by a single name.
     */
    public static String getBestIpString() {
        Enumeration<NetworkInterface> ifaces;
        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.error("Error getting network interfaces", e);
            return "127.0.0.1";
        }
        TreeSet<String> siteLocalCandidates = new TreeSet<String>();
        TreeSet<String> candidates = new TreeSet<String>();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            for (Enumeration<InetAddress> addrs =
                 iface.getInetAddresses(); addrs.hasMoreElements(); ) {
                InetAddress addr = addrs.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr.isSiteLocalAddress()) {
                        siteLocalCandidates.add(addr.getHostAddress());
                    } else {
                        candidates.add(addr.getHostAddress());
                    }
                }
            }
        }
        if (!siteLocalCandidates.isEmpty()) {
            return siteLocalCandidates.first();
        }
        if (!candidates.isEmpty()) {
            return candidates.first();
        }
        return "127.0.0.1";
    }

    /**
     * <p>Get the process id from the operating system.</p>
     * <p>
     * Unfortunately, there is no simple method to get the process id in Java.
     * The approach we take here is to use the shell method (see
     * {TracerId#getOsPidFromShellPpid}) unless we are on Windows, where the
     * shell is not available.  On Windows, we use
     * {TracerId#getOsPidFromManagementFactory}, which depends on some
     * undocumented features of the JVM, but which doesn't require a shell.
     */
    public static long getOsPid() {
        if ((System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)).
                contains("windows")) {
            return getOsPidFromManagementFactory();
        } else {
            return getOsPidFromShellPpid();
        }
    }

    /**
     * <p>Get the process ID by executing a shell and printing the PPID (parent
     * process ID).</p>
     * <p>
     * This method of getting the process ID doesn't depend on any undocumented
     * features of the virtual machine, and should work on almost any UNIX
     * operating system.
     */
    private static long getOsPidFromShellPpid() {
        Process p = null;
        StringBuilder sb = new StringBuilder();
        try {
            p = new ProcessBuilder("/usr/bin/env", "sh", "-c", "echo $PPID").
                    redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line.trim());
            }
            int exitVal = p.waitFor();
            if (exitVal != 0) {
                throw new IOException("Process exited with error code " +
                        Integer.valueOf(exitVal).toString());
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while getting operating system pid from " +
                    "the shell.", e);
            return 0L;
        } catch (IOException e) {
            logger.error("Error getting operating system pid from the shell.", e);
            return 0L;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        try {
            return Long.parseLong(sb.toString());
        } catch (NumberFormatException e) {
            logger.error("Error parsing operating system pid from the shell.", e);
            return 0L;
        }
    }

    /**
     * <p>Get the process ID by looking at the name of the managed bean for the
     * runtime system of the Java virtual machine.</p>
     * <p>
     * Although this is undocumented, in the Oracle JVM this name is of the form
     * [OS_PROCESS_ID]@[HOSTNAME].
     */
    private static long getOsPidFromManagementFactory() {
        try {
            return Long.parseLong(ManagementFactory.getRuntimeMXBean().
                    getName().split("@")[0]);
        } catch (NumberFormatException e) {
            logger.error("Failed to get the operating system process ID from the name " +
                    "of the managed bean for the JVM.", e);
            return 0L;
        }
    }
}
