package com.infrarch.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.infrarch.engine.command.Upload;
import com.infrarch.engine.command.Copy;
import com.infrarch.engine.command.Download;
import com.infrarch.engine.command.ListContents;
import com.infrarch.engine.command.Login;
import com.infrarch.engine.command.Ping;
import com.infrarch.engine.command.Recycle;
import com.infrarch.engine.command.Settings;
import com.infrarch.engine.constants.Cmd;
import com.infrarch.engine.constants.Field;
import com.infrarch.engine.constants.Response;
import com.infrarch.engine.worker.ConfigurationManager;

/**
 * This servlet enables remote clients to execute file operations on 
 * a server. Clients send HTTP requests, the servlet executes them and returns 
 * results in either JSON format, when information is requested, or as binary data 
 * (MIME type "application/octet-stream"), when file download is requested.
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
@SuppressWarnings("serial")
public class Engine extends HttpServlet {
	
	public static final String VERSION = "cloud-server-engine 1.0";
	private static final String LOG_FILE_NAME = "engine.log";
	
	/**
	 * The configuration directory - relative from ServletContext.getRealPart("/")
	 */
	public static final String CONFIG_DIR = "WEB-INF/classes/";
	
	/**
	 * Session attribute name for user's name.
	 */
	public static final String ATTRIBUTE_USER_NAME = "name";
	
	/**
	 * Session attribute name for authentication status of the session.
	 */
	public static final String ATTRIBUTE_AUTHENTICATED = "authenticated";

	private Logger logger;
	private ConfigurationManager engineConfig;

	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		// first of all set up the configuration manager and configuration directory
		engineConfig = ConfigurationManager.getInstance();
		try {
			String cd = getServletContext().getRealPath("/");
			engineConfig.setConfigRoot(cd + CONFIG_DIR);
		} catch (Exception e) {
			//logger.warn("Could not obtain servlet context path; using default config path: " + engineConfig.getConfigRoot(), e);
		}

		// init the logger
		initLogger();
		logger.info("Initializing server engine...");
		
		// load settings
		engineConfig.initialize();
		
		// register commands
		CommandManager instance = CommandManager.getInstance();
		instance.registerCommand(Login.getInstance());
		instance.registerCommand(Ping.getInstance());
		instance.registerCommand(ListContents.getInstance());
		instance.registerCommand(Recycle.getInstance());
		instance.registerCommand(Settings.getInstance());
		instance.registerCommand(Upload.getInstance());
		instance.registerCommand(Copy.getInstance());
		
		logger.info("Server engine initialized");
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// store starting time
		long startTime = System.nanoTime();
		
		// servlet init
		request.setCharacterEncoding("UTF-8");
		HttpSession session = request.getSession();

        // initialize common variables
 		JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
 		JsonObjectBuilder paramsBuilder= Json.createObjectBuilder();
 		String user = (String) session.getAttribute(ATTRIBUTE_USER_NAME);
 		if (user == null) user = "unknown";
 		String userIp = EngineUtils.getClientIP(request);
 		String cmdStr = request.getParameter(Cmd.Q_COMMAND);
 		
 		// create a JSON object with the request data (will later add it to the response)
 		paramsBuilder.add(Field.SESSION_USER_NAME, user);
 		paramsBuilder.add(Field.SESSION_IP, userIp);
 		Enumeration<String> e = request.getParameterNames();
		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();
			String param = EngineUtils.urlEncode(request.getParameter(name));
			if (param == null) param = "";
			paramsBuilder.add(name, param);
		}
		JsonObject params = paramsBuilder.build();
 	
		// check for login request
		if (cmdStr != null && Cmd.LOGIN.equalsIgnoreCase(cmdStr)) {
			Command engineCommand = CommandManager.getInstance().getSupportingCommand(cmdStr);
 			if (engineCommand != null) {
 				try {
					engineCommand.execute(request, params, resultBuilder);
				} catch (Throwable t) {
					logger.error("Exception occured while executing command: " + engineCommand.getClass(), t);
					EngineUtils.putStatus(resultBuilder, Response.CODE_ERROR);
				}
 			}
 			else EngineUtils.putStatus(resultBuilder, Response.CODE_UNSUPPORTED_COMMAND); // should not happen
		}
		
 		// authentication check
		else if (!Worker.isAuthenticated(request)) {
 			EngineUtils.putStatus(resultBuilder, Response.CODE_NOT_AUTHENTICATED);
 		}
 		
 		// check the command parameter
 		else if (cmdStr == null || "".equals(cmdStr)) {
 			EngineUtils.putStatus(resultBuilder, Response.CODE_NO_COMMAND);
 		}
 				
 		// look for the appropriate command
 		else {
 			
 			// download is specific because is the only one returning
 			// a stream; cannot initialize and use a Writer and an OutputStream
 			// in one response
 			if (Cmd.DOWNLOAD.equalsIgnoreCase(cmdStr)) {
 				logger.info("(" + user + ", " + userIp + ") " +
 						"Requested download: [" + params.toString() + "]");
 				Download.download(params, request, response, logger);
 				return;
 			}
 			
 			// execute the command
 			Command engineCommand = CommandManager.getInstance().getSupportingCommand(cmdStr);
 			if (engineCommand != null) {
 				try {
					engineCommand.execute(request, params, resultBuilder);
				} catch (Throwable t) {
					logger.error("Exception occured while executing command: " + engineCommand.getClass(), t);
					EngineUtils.putStatus(resultBuilder, Response.CODE_ERROR);
				}
 			}
 			
 			// unrecognized command, if we are here
 			else {
 				EngineUtils.putStatus(resultBuilder, Response.CODE_UNRECOGNIZED_COMMAND);
 			}
 		}

 		// complete the response
 		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
 		PrintWriter out = response.getWriter();
 		resultBuilder.add(Field.QUERY, params);
 		long processingTime = System.nanoTime() - startTime;
 		resultBuilder.add(Field.PROCESSING_TIME, TimeUnit.MILLISECONDS.convert(processingTime, TimeUnit.NANOSECONDS));
 		JsonObject result = resultBuilder.build();
		out.println(result.toString());
		
		// log the request
		logger.info("(" + user + ", " + userIp + ") " + 
				params.toString() + " >>> [" + 
				result.getInt(Field.RESULT_CODE) + "; " + 
				result.getString(Field.RESULT_MESSAGE) + "; " + 
				TimeUnit.MILLISECONDS.convert(processingTime, TimeUnit.NANOSECONDS) + "ms]");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	private synchronized void initLogger() {
		if (logger == null) {
			logger = Logger.getLogger(getClass());

			RollingFileAppender appender = new RollingFileAppender();
			appender.setName("cloud-server-engine");
			appender.setFile(Worker.getConfigRoot().getAbsolutePath() + File.separatorChar + LOG_FILE_NAME);
			appender.setLayout(new PatternLayout("%d{ISO8601} %-5p [%c{1}] %m%n"));
			appender.setThreshold(Level.DEBUG);
			appender.setAppend(true);
			appender.setMaxBackupIndex(2);
			appender.setMaxFileSize("500KB");
			appender.activateOptions();
			Logger.getRootLogger().addAppender(appender);
		}
	}
}
