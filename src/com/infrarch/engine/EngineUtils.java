package com.infrarch.engine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

import javax.activation.MimetypesFileTypeMap;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.infrarch.engine.constants.Field;
import com.infrarch.engine.constants.Response;

public class EngineUtils {

	/**
	 * Puts the status code and human-readable status message in the JSON response.
	 * 
	 * @param builder JSON builder
	 * @param code response code
	 */
	public static void putStatus(JsonObjectBuilder builder, int code) {
		String msg;
		switch (code) {
			case Response.CODE_OK: msg = Response.MSG_OK; break;
			case Response.CODE_ERROR: msg = Response.MSG_ERROR; break;
			case Response.CODE_NOT_AUTHENTICATED: msg = Response.MSG_NOT_AUTHENTICATED; break;
			case Response.CODE_INCORRECT_LOGIN: msg = Response.MSG_INCORRECT_LOGIN; break;
			case Response.CODE_NO_ACCESS: msg = Response.MSG_NO_ACCESS; break;
			case Response.CODE_NO_COMMAND: msg = Response.MSG_NO_COMMAND; break;
			case Response.CODE_UNRECOGNIZED_COMMAND: msg = Response.MSG_UNRECOGNIZED_COMMAND; break;
			case Response.CODE_UNSUPPORTED_COMMAND: msg = Response.MSG_UNSUPPORTED_COMMAND; break;
			case Response.CODE_MISSING_PARAMETER: msg = Response.MSG_MISSING_PARAMETER; break;
			case Response.CODE_WRONG_PARAMETER: msg = Response.MSG_WRONG_PARAMETER; break;
			case Response.CODE_NO_SUCH_FILE: msg = Response.MSG_NO_SUCH_FILE; break;
			case Response.CODE_NO_SUCH_DIR: msg = Response.MSG_NO_SUCH_DIR; break;
			default: msg = "-";
		}
		
		builder
			.add(Field.RESULT_CODE, code)
			.add(Field.RESULT_MESSAGE, msg);
	}
	
	/**
	 * Utility method to get client's IP as a string. 
	 * 
	 * @param request request to process
	 * @return client's IP address
	 */
	public static String getClientIP(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("WL-Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_CLIENT_IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getRemoteAddr();  
        }
        if (ip == null || ip.length() == 0) ip = "unknown";
        
        if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("127.0.0.1")) ip = "localhost";
		return ip;
	}
	
	/**
	 * Encodes a string as URL.
	 * 
	 * @param url URL to encode
	 * @return encoded URL
	 */
	public static String urlEncode(String url) {
		String result = null;
		try {
			result = URLEncoder.encode(url, "utf-8");
		} catch (UnsupportedEncodingException ex) {
			// highly unlikely that UTF-8 is not supported...
		}
		// replace + with %20, because this encoding will be supported both
		// in query string and in path while + is only supported in query string
		return result.replaceAll("\\+", "%20");
	}
	
	/**
	 * Decodes a URL string.
	 * 
	 * @param url URL to decode
	 * @return decoded URL
	 */
	public static String urlDecode(String url) {
		String result = null;
		try {
			result = URLDecoder.decode(url, "utf-8");
		} catch (UnsupportedEncodingException ex) {
			// highly unlikely that UTF-8 is not supported...
		}
		return result;
	}
	
	public static String getRelativeDir(File parentDir, File dir) {
		String d1 = parentDir.getAbsolutePath();
		String d2 = dir.getAbsolutePath();
		if (d2.startsWith(d1)) {
			d2 = d2.substring(d1.length());
			if (d2.isEmpty())
				d2 = "/";
			return formatDirectory(d2);
		}
		return null;
	}
	
	public static String formatDirectory(String dir) {
		if (dir == null || dir.isEmpty())
			return dir;
		dir = dir.replace('\\', '/');
		dir = dir.replace("/+$", "");
		dir = "/" + dir;
		dir = dir.replaceAll("/+", "/");
		return dir;
	}
	
	public static String formatDate(Date date) {
		return formatDate(date, true);
	}
	
	public static String formatDate(Date date, boolean includeTime) {
		if (date == null)
			return "";
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		return String.format(includeTime ? 
				"%02d.%02d.%4d %02d:%02d:%02d" : "%02d.%02d.%4d", 
				cal.get(Calendar.DATE),
				cal.get(Calendar.MONTH) + 1,
				cal.get(Calendar.YEAR),
				cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE),
				cal.get(Calendar.SECOND));
	}
	
	public static OutputStream prepareDownloadStream(String fileName, HttpServletRequest request, HttpServletResponse response) throws IOException {
		String userAgent = request.getHeader("User-agent");
		
		String mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName);
		response.setContentType(mimeType);
		
		String disposition = mimeType.startsWith("text") || mimeType.startsWith("image") ? "inline" : "attachment";
		String fileNameHeader;
		if (userAgent.contains("MSIE")) {
			// IE
			fileNameHeader = "filename=" + urlEncode(fileName);
		} else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
			// plain Safari
			fileNameHeader = "filename=" + fileName;
		} else {
			// all the rest
			fileNameHeader = "filename*=UTF-8''" + urlEncode(fileName);
		}
		response.setHeader("Content-disposition", disposition + ";" + fileNameHeader + ";");
		
		return response.getOutputStream();
	}
	
	public static boolean downloadFile(File file, HttpServletRequest request, HttpServletResponse response) {
		return downloadFile(file, file.getName(), request, response);
	}
	
	public static boolean downloadFile(File file, String name, HttpServletRequest request, HttpServletResponse response) {
		InputStream input = null;
		OutputStream output = null;
		try {
			// prepare to stream the file to the client
			input = new BufferedInputStream(new FileInputStream(file));
			output = prepareDownloadStream(name, request, response);
	
			byte[] buffer = new byte[8192];
			int count;
			while ((count = input.read(buffer)) > 0) {
				output.write(buffer, 0, count);
			}
			output.flush();
		} catch (IOException iox) {
			Logger.getLogger(EngineUtils.class).error("Error downloading file", iox);
			return false;
			
		} finally {
			try {
				if (input != null) input.close();
				if (output != null) output.close();
			} catch (IOException iox) {
				Logger.getLogger(EngineUtils.class).error("Error downloading file", iox);
				return false;
			}
		}
		
		return true;
	}
}
