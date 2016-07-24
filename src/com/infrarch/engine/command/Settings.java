package com.infrarch.engine.command;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;

import com.infrarch.engine.AbstractCommand;
import com.infrarch.engine.Command;
import com.infrarch.engine.CommandManager;
import com.infrarch.engine.Engine;
import com.infrarch.engine.EngineUtils;
import com.infrarch.engine.Worker;
import com.infrarch.engine.constants.Cmd;
import com.infrarch.engine.constants.Field;
import com.infrarch.engine.constants.Response;

/**
 * {@code Cmd.SERVER_INFO}, {@code Cmd.SETTINGS_GET}, {@code Cmd.SETTINGS_SET},
 * {@code Cmd.LIST_SETTINGS}.
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class Settings extends AbstractCommand {

	// attribute names
	public static final String SERVER_IP = "serverIP";
	public static final String SERVER_HOST_NAME = "serverHostName";
	public static final String LOCAL_DATE_AND_TIME = "localDateAndTime";
	public static final String DISK_SPACE_FREE = "diskSpaceFree";
	public static final String DISK_SPACE_TAKEN = "diskSpaceTaken";
	public static final String WEB_SERVER_VERSION = "webServerVersion";
	public static final String JAVA_VERSION = "JavaVersion";
	public static final String SERVER_APPLICATION_ROOT = "serverApplicationRoot";
	public static final String DOCUMENT_ROOT = "documentRoot";
	public static final String CONFIG_ROOT = "configurationRoot";
	public static final String VERSION = "version";
	public static final String COMMANDS = "supported_commands";
	
	private static final Settings instance = new Settings();
	
	private String hostName = null, serverIP = null;
	
	private Settings() {
	    try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			hostName = inetAddress.getHostName();
	        serverIP = inetAddress.getHostAddress();
	    } catch (UnknownHostException e) {}
	}
	
	public static Command getInstance() {
		return instance;
	}
	
	@Override
	public String[] getSupportedCommands() {
		return new String[] { Cmd.SERVER_INFO, Cmd.SETTINGS_GET, Cmd.SETTINGS_SET, Cmd.LIST_SETTINGS };				
	}
	
	@Override
	public void execute(HttpServletRequest request, JsonObject params, JsonObjectBuilder builder) throws Throwable {

		// Cmd.SERVER_INFO
		String cmdStr = params.getString(Cmd.Q_COMMAND);
		if (Cmd.SERVER_INFO.equalsIgnoreCase(cmdStr)) {
			
			JsonArrayBuilder cmds = Json.createArrayBuilder();
			String[] cmdStrings = CommandManager.getInstance().getSupportedCommands();
			for (String c: cmdStrings) cmds.add(c);
			
			builder
				.add(VERSION, Engine.VERSION)
				.add(COMMANDS, cmds)
				.add(DOCUMENT_ROOT, Worker.getDocRoot().getAbsolutePath())
				.add(CONFIG_ROOT, Worker.getConfigRoot().getAbsolutePath())
				.add(SERVER_APPLICATION_ROOT, request.getServletContext().getRealPath(""))
				.add(JAVA_VERSION, System.getProperty("java.version") + ", " + System.getProperty("java.vendor"))
				.add(WEB_SERVER_VERSION, request.getServletContext().getServerInfo())
				.add(DISK_SPACE_TAKEN, Worker.getSizeOnDisk())
				.add(DISK_SPACE_FREE, Worker.getDocRoot().getUsableSpace())
				.add(LOCAL_DATE_AND_TIME, EngineUtils.formatDate(new Date()));
			
			if (hostName != null && serverIP != null) builder.add(SERVER_HOST_NAME, hostName).add(SERVER_IP, serverIP);
		}
		
		// Cmd.LIST_SETTINGS
		else if (Cmd.LIST_SETTINGS.equalsIgnoreCase(cmdStr)) {
			String[] keys = Worker.getConfigKeys();
			for (String key: keys) {
				builder.add(key, Worker.getConfigProperty(key));
			}
		}
				
		// Cmd.SETTINGS_GET
		else if (Cmd.SETTINGS_GET.equalsIgnoreCase(cmdStr)) {
			
			// get the property name 
			String property;
			try { 
				property = params.getString(Cmd.Q_PROPERTY);
			} catch (NullPointerException e) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			if ("".equals(property)) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			property = EngineUtils.urlDecode(property);
			
			// get the property value
			String value = Worker.getConfigProperty(property);
			builder
				.add(Field.PROPERTY, property)
				.add(Field.VALUE, value);			
		}
		
		// Cmd.SETTINGS_SET
		else if (Cmd.SETTINGS_SET.equalsIgnoreCase(cmdStr)) {
			
			// get the property name 
			String property;
			try { 
				property = params.getString(Cmd.Q_PROPERTY);
			} catch (NullPointerException e) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			if ("".equals(property)) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			property = EngineUtils.urlDecode(property);
				
			// get the property value
			String value;
			try { 
				value = params.getString(Cmd.Q_VALUE);
			} catch (NullPointerException e) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			if ("".equals(value)) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			value = EngineUtils.urlDecode(value);
			
			// set the property value
			Worker.setConfigProperty(property, value);
		}
		
		else {
			EngineUtils.putStatus(builder, Response.CODE_UNRECOGNIZED_COMMAND);
			return;
		}
		
		EngineUtils.putStatus(builder, Response.CODE_OK);
	}
}
