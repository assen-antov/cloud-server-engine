package com.infrarch.engine.command;

import java.io.File;

import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.infrarch.engine.Engine;
import com.infrarch.engine.EngineUtils;
import com.infrarch.engine.Worker;
import com.infrarch.engine.constants.Cmd;

public class Download {

	public static void download(JsonObject params, HttpServletRequest request, HttpServletResponse response, Logger logger) {
	
		// check if dir present
		String dir;
		try { 
			dir = params.getString(Cmd.Q_DIR);
		} catch (NullPointerException e) {
			logger.info("No directory set");
			return;
		}
		if ("".equals(dir)) {
			logger.info("No directory set");
			return;
		}
		dir = EngineUtils.urlDecode(dir);
				
		// check ref / file name parameters
		String fileName;
		try {
			fileName = params.getString(Cmd.Q_FILE_NAME);
		} catch (NullPointerException e) {
			logger.info("No file name set");
			return;
		}
		if ("".equals(fileName)) {
			logger.info("No file name set");
			return;
		}
		
		// check if the user has access to this directory
		HttpSession session = request.getSession();
 		String user = (String) session.getAttribute(Engine.ATTRIBUTE_USER_NAME);
		if (!Worker.checkUserAccess(user, dir)) {
			logger.info("User " + user + " has no access to directory: " + dir);
			return;
		}
		
		// construct a File for the directory
		File docRoot = Worker.getDocRoot();
		File fDir = new File(docRoot, dir);
		if (!fDir.exists() || !fDir.isDirectory()) {
			logger.info("No such directory: " + dir);
			return;
		}
			
		// download the file
		File file = new File(fDir, fileName);
		boolean b = EngineUtils.downloadFile(file, fileName, request, response);
		
		if (!b) logger.error("I/O error downloading file: " + file.getAbsolutePath());
		else logger.info("Successfully downloaded file: " + file.getAbsolutePath());
	}
}
