package com.infrarch.engine.command;

import java.io.File;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.infrarch.engine.AbstractCommand;
import com.infrarch.engine.Command;
import com.infrarch.engine.Engine;
import com.infrarch.engine.EngineUtils;
import com.infrarch.engine.Worker;
import com.infrarch.engine.constants.Cmd;
import com.infrarch.engine.constants.Field;
import com.infrarch.engine.constants.Response;

/**
 * This class supports three commands: {@code Cmd.LIST_CONTENTS}, {@code Cmd.LIST_DIRS}, 
 * {@code Cmd.LIST_FILES}, {@code Cmd.FILE_INFO}, {@code Cmd.MAKE_DIR}. 
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class ListContents extends AbstractCommand {

	private static final ListContents instance = new ListContents();
	
	public static Command getInstance() {
		return instance;
	}
	
	@Override
	public String[] getSupportedCommands() {
		return new String[] { Cmd.LIST_CONTENTS, Cmd.LIST_DIRS, Cmd.LIST_FILES, Cmd.FILE_INFO, Cmd.MAKE_DIR };
	}
	
	@Override
	public void execute(HttpServletRequest request, JsonObject params, JsonObjectBuilder builder) throws Throwable {
		
		// check if dir present
		String dir;
		try { 
			dir = params.getString(Cmd.Q_DIR);
		} catch (NullPointerException e) {
			EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
			return;
		}
		if ("".equals(dir)) {
			EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
			return;
		}
		dir = EngineUtils.urlDecode(dir);
		
		// check if the user has access to this directory
		HttpSession session = request.getSession();
 		String user = (String) session.getAttribute(Engine.ATTRIBUTE_USER_NAME);
		if (!Worker.checkUserAccess(user, dir)) {
			EngineUtils.putStatus(builder, Response.CODE_NO_ACCESS);
			return;
		}
		
		// construct a File for the directory
		File docRoot = Worker.getDocRoot();
		File fDir = new File(docRoot, dir);
		
		// is make_dir command?
		String cmdStr = params.getString(Cmd.Q_COMMAND);
		if (Cmd.MAKE_DIR.equalsIgnoreCase(cmdStr)) {
			if (fDir.mkdirs()) EngineUtils.putStatus(builder, Response.CODE_OK);
			else EngineUtils.putStatus(builder, Response.CODE_ERROR);
			return;
		}
		
		// check if dir exists
		if (!fDir.exists() || !fDir.isDirectory()) {
			EngineUtils.putStatus(builder, Response.CODE_NO_SUCH_DIR);
			return;
		}
		
		// Cmd.LIST_CONTENTS
		if (Cmd.LIST_CONTENTS.equalsIgnoreCase(cmdStr)) {
			builder.add(Field.DIRECTORIES, listDirectories(fDir, docRoot));
			builder.add(Field.FILES, listFiles(fDir, user, dir));
		}
		
		// Cmd.LIST_DIRS
		else if (Cmd.LIST_DIRS.equalsIgnoreCase(cmdStr)) {
			builder.add(Field.DIRECTORIES, listDirectories(fDir, docRoot));
		}
		
		// Cmd.LIST_FILES
		else if (Cmd.LIST_FILES.equalsIgnoreCase(cmdStr)) {
			builder.add(Field.FILES, listFiles(fDir, user, dir));
		}
		
		// Cmd.FILE_INFO
		else if (Cmd.FILE_INFO.equalsIgnoreCase(cmdStr)) {
			
			// check file name parameter
			String fileName;
			try { 
				fileName = params.getString(Cmd.Q_FILE_NAME);
			} catch (NullPointerException e) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			if ("".equals(fileName)) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			fileName = EngineUtils.urlDecode(fileName);
						
			// get file info
			File f = new File(fDir, fileName);
			JsonObjectBuilder fi = fileInfo(f);
			if (fi != null) builder.add(Field.FILE_INFO, fi);
			else {
				EngineUtils.putStatus(builder, Response.CODE_NO_SUCH_FILE);
				return;
			}
		}

		else {
			logger.error("Unrecognized command");
			EngineUtils.putStatus(builder, Response.CODE_UNRECOGNIZED_COMMAND);
			return;
		}
			
		EngineUtils.putStatus(builder, Response.CODE_OK);
	}
	
	private JsonArrayBuilder listDirectories(File f, File docRoot) {
		JsonArrayBuilder filesArray = Json.createArrayBuilder();
		File[] files = f.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (!files[i].isDirectory() || Worker.isSystemFile(files[i])) continue;
			boolean hasSubd = hasSubdirs(files[i]);
			String relDir = EngineUtils.getRelativeDir(docRoot, files[i]);
			filesArray.add(Json.createObjectBuilder()
				.add("name", files[i].getName())
				.add("relativePath", relDir == null? "" : relDir)
				.add("lastModified", files[i].lastModified())
				.add("hasSubdirs", hasSubd)
			);
		}
		return filesArray;
	}
	
	private boolean hasSubdirs(File f) {
		if (!f.isDirectory()) return false;
		File[] files = f.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) return true;
		}
		return false;
	}
	
	private JsonArrayBuilder listFiles(File f, String user, String dir) {
		JsonArrayBuilder filesArray = Json.createArrayBuilder();
		File[] files = f.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory() || Worker.isSystemFile(files[i])) continue;
			filesArray.add(fileInfo(files[i]));
		}
		return filesArray;
	}
		
	private JsonObjectBuilder fileInfo(File f) {
		if (!f.exists()) return null;
		
		JsonObjectBuilder infoBuilder = Json.createObjectBuilder();
		infoBuilder
			.add("name", f.getName())
			.add("lastModified", f.lastModified())
			.add("size", f.length())
			.add("hidden", f.isHidden())
			.add("canRead", f.canRead())
			.add("canWrite", f.canWrite());
		return infoBuilder;
	}
}
