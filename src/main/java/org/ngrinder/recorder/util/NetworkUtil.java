/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ngrinder.recorder.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ngrinder.recorder.infra.NGrinderRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common network utility. This contains very careful implementation to detect current machine's ip.
 * There are the following cases which block to get the appropriate ip.
 * 
 * <ul>
 * <li>If there are VM in the same machine</li>
 * <li>If /etc/hosts are not very well specified</li>
 * </ul>
 * 
 * @author JunHo Yoon
 * @author Mavlarn
 * 
 * @since 1.0
 * 
 */
public abstract class NetworkUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtil.class);

	/**
	 * Get the available port using the given default port. If it's not available, it chooses the
	 * random port.
	 * 
	 * @param localHostAddress
	 *            address in which the port will be assigned.
	 * @param defaultPort
	 *            default port.
	 * @return usable port.
	 */
	public static int getAvailablePort(InetAddress localHostAddress, int defaultPort) {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(defaultPort, 50, localHostAddress);

			return socket.getLocalPort();
		} catch (IOException e) {
			LOGGER.error("Error during openning port. {}", e.getMessage());
			LOGGER.debug("Details:{}", e.getMessage(), e);
			return getAvailablePort(localHostAddress);
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					LOGGER.error("Error during port close");
				}
			}
		}
	}

	/**
	 * Get a available port.
	 * 
	 * @param localHostAddress
	 *            localHostAddress
	 * 
	 * @return min port available from scanStartPort
	 */
	public static int getAvailablePort(InetAddress localHostAddress) {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0, 50, localHostAddress);
			return socket.getLocalPort();
		} catch (IOException e) {
			LOGGER.error("Error during openning port. {}", e.getMessage());
			LOGGER.debug("Details:{}", e.getMessage(), e);
			return 16000;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					LOGGER.error("Error during closing port");
				}
			}
		}
	}

	/**
	 * Get the local host address, try to get actual IP.
	 * 
	 * @return ip form of host address
	 */
	public static InetAddress getLocalHostAddress() {
		InetAddress localHost = null;
		try {
			localHost = getFirstNonLoopbackAddress(true, false);
		} catch (Exception e) {
			LOGGER.error("Error while get localhost address", e);
		}
		if (localHost != null && !localHost.isLoopbackAddress()) {
			return localHost;
		}
		try {
			localHost = InetAddress.getLocalHost();
		} catch (Exception e) {
			LOGGER.error("Error while get localhost address", e);
		}
		if (!localHost.isLoopbackAddress()) {
			return localHost;
		}
		return getLocalHostAddress("www.google.com", 80);
	}

	/**
	 * Get local address by connecting to a server.
	 * 
	 * @param byConnecting
	 *            the server address to connect.
	 * @param port
	 *            the port to connect
	 * @return IP address local IP address
	 */
	public static InetAddress getLocalHostAddress(String byConnecting, int port) {
		InetAddress addr = getLocalInetAddress(byConnecting, port);
		if (addr != null) {
			return addr;
		} else {
			// It's final...
			try {
				return InetAddress.getByName("127.0.0.1");
			} catch (UnknownHostException e) {
				return null;
			}
		}
	}

	/**
	 * Get local host name by connecting to a server.
	 * 
	 * @param byConnecting
	 *            the server address to connect.
	 * @param port
	 *            the port to connect
	 * @return localhost name. if fails, return "localhost"
	 */
	public static String getLocalHostName(String byConnecting, int port) {
		InetAddress addr = getLocalInetAddress(byConnecting, port);
		if (addr != null) {
			return addr.getHostName();
		} else {
			return "localhost";
		}
	}

	private static InetAddress getLocalInetAddress(String byConnecting, int port) {
		InetAddress addr = getAddressWithSocket(byConnecting, port);
		if (addr == null) {
			addr = getAddressWithSocket("www.baidu.com", 80);
		}
		return addr;
	}

	private static InetAddress getAddressWithSocket(String byConnecting, int port) {
		Socket s = null;
		try {
			s = new Socket();
			SocketAddress addr = new InetSocketAddress(byConnecting, port);
			s.connect(addr, 2000); // 2 seconds timeout
			return s.getLocalAddress();
		} catch (Exception e) {
			return null;
		} finally {
			IOUtils.closeQuietly(s);
		}
	}

	private static InetAddress getFirstNonLoopbackAddress(boolean preferIpv4, boolean preferIPv6)
					throws SocketException {
		Enumeration<?> en = NetworkInterface.getNetworkInterfaces();
		while (en.hasMoreElements()) {
			NetworkInterface i = (NetworkInterface) en.nextElement();
			if (!i.isUp()) {
				continue;
			}
			if (StringUtils.containsIgnoreCase(i.getDisplayName(), "Host-Only")) {
				continue;
			}
			for (Enumeration<?> en2 = i.getInetAddresses(); en2.hasMoreElements();) {
				InetAddress addr = (InetAddress) en2.nextElement();
				if (!addr.isLoopbackAddress()) {
					if (addr instanceof Inet4Address) {
						if (preferIPv6) {
							continue;
						}
						return addr;
					}
					if (addr instanceof Inet6Address) {
						if (preferIpv4) {
							continue;
						}
						return addr;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get local host name. On some platform, InetAddress.getLocalHost().getHostName() will return
	 * "localhost". If the /etc/hosts file is not set properly, it will return "localhost" or throw
	 * exception. So, at this circumstance, we will get the address by connecting a network address.
	 * 
	 * @return local host name
	 */
	public static String getLocalHostName() {
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			LOGGER.error("Error while get localhost name", e);
		}
		if (hostName != null && !"localhost".equals(hostName)) {
			return hostName;
		}
		return getLocalHostName("www.google.com", 80);

	}

	/**
	 * Get the IP addresses from host name.
	 * 
	 * @param host
	 *            host
	 * @return {@link InetAddress} array
	 */
	public static InetAddress[] getIpsFromHost(String host) {
		try {
			return InetAddress.getAllByName(host);
		} catch (UnknownHostException e) {
			LOGGER.error("Error while get localhost name for {}", host, e);
			return new InetAddress[] {};
		}
	}

	/**
	 * Download a file from the given URL string.
	 * 
	 * @param urlString
	 *            URL string
	 * @param toFile
	 *            file to be saved.
	 */
	public static void downloadFile(String urlString, File toFile) {
		FileOutputStream os = null;
		InputStream in = null;
		URLConnection connection = null;
		try {
			URL url = new URL(urlString);
			connection = url.openConnection();
			connection.connect();
			byte[] buffer = new byte[4 * 1024];
			int read;
			os = new FileOutputStream(toFile);
			in = connection.getInputStream();
			while ((read = in.read(buffer)) > 0) {
				os.write(buffer, 0, read);
			}
		} catch (Exception e) {
			LOGGER.error("download file from {} was failed", urlString, e);
			throw processException("Error while download " + urlString, e);
		} finally {
			((HttpURLConnection) connection).disconnect();
			IOUtils.closeQuietly(os);
			IOUtils.closeQuietly(in);
		}
		return;
	}

	private static final String PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	private static final Pattern IP_PATTERN = Pattern.compile(PATTERN);

	/**
	 * Check if the given IP is well formed.
	 * 
	 * @param ip
	 *            ip
	 * @return true if valid
	 */
	public static boolean isValidIP(final String ip) {
		Matcher matcher = IP_PATTERN.matcher(ip);
		return matcher.matches();
	}
}
