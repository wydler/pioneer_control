package eu.wydler.helper;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Helper {
	public static float normalizeAccelerationValue(float acc) {
		float tmp = (float) (java.lang.Math.rint( acc * 10.19 ) / 100F);
		if (tmp > 1.0F)
			tmp = 1.0F;
		if (tmp < -1.0F)
			tmp = -1.0F;
		return tmp;
	}
	
	public static String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	    	throw new RuntimeException(ex);
	    }
	    
	    return null;
	}
}
