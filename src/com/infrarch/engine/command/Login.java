package com.infrarch.engine.command;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;

import com.infrarch.commons.db.Row;
import com.infrarch.engine.AbstractCommand;
import com.infrarch.engine.Command;
import com.infrarch.engine.EngineUtils;
import com.infrarch.engine.Worker;
import com.infrarch.engine.constants.Cmd;
import com.infrarch.engine.constants.Field;
import com.infrarch.engine.constants.Response;

/**
 * This class supports commands {@code Cmd.LOGIN}, {@code Cmd.LOGOUT}, 
 * {@code Cmd.CHANGE_PASSWORD}, {@code Cmd.USER_INFO}, {@code Cmd.LIST_USERS}, 
 * {@code Cmd.ADD_USER}, {@code Cmd.EDIT_USER}, {@code Cmd.DELETE_USER}.
 *  
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class Login extends AbstractCommand {

	private static final Login instance = new Login();
	
	public static Command getInstance() {
		return instance;
	}
	
	@Override
	public String[] getSupportedCommands() {
		return new String[] { Cmd.LOGIN, Cmd.LOGOUT, Cmd.CHANGE_PASSWORD, Cmd.USER_INFO, Cmd.LIST_USERS,
				Cmd.ADD_USER, Cmd.EDIT_USER, Cmd.DELETE_USER };
				
	}
	
	@Override
	public void execute(HttpServletRequest request, JsonObject params, JsonObjectBuilder builder) throws Throwable {
		
		// Cmd.LOGIN
		String cmdStr = params.getString(Cmd.Q_COMMAND);
		if (Cmd.LOGIN.equalsIgnoreCase(cmdStr)) {

			// get name and password
			String user, pass;
			try {
				user = params.getString(Cmd.Q_USER);
				pass = params.getString(Cmd.Q_PASSWORD);
			} catch (NullPointerException e) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			if ("".equals(user) || "".equals(pass)) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			
			// get user info
			Row row = Worker.getUserRow(user);
			if (row == null) {
				// if trying to login as admin, create a default user
				if ("admin".equals(user)) {
					row = Worker.addUser("admin", "admin", "/");
				}
				else {
					try {
						TimeUnit.SECONDS.sleep(Worker.getConfigPropertyInt(Worker.KEY_INCORRECT_LOGIN_DELAY, 3));
					} catch (InterruptedException e) {}
					
					EngineUtils.putStatus(builder, Response.CODE_INCORRECT_LOGIN);
					return;
				}
			}
			
			// compare password and return result
			String encryptedPassword = (String) row.get(Worker.FIELD_PASS); 
			boolean userOk = Worker.matchEncryptedPassword(pass, encryptedPassword);
			if (userOk) {
				Worker.setAuthenticated(user, true, request);
				EngineUtils.putStatus(builder, Response.CODE_OK);
				return;
			}
			else {
				try {
					TimeUnit.SECONDS.sleep(Worker.getConfigPropertyInt(Worker.KEY_INCORRECT_LOGIN_DELAY, 3));
				} catch (InterruptedException e) {}
				
				EngineUtils.putStatus(builder, Response.CODE_INCORRECT_LOGIN);
				return;
			}
		}
		
		
		// Cmd.LOGOUT
		if (Cmd.LOGOUT.equalsIgnoreCase(cmdStr)) {
			Worker.setAuthenticated(null, false, request);
		}
		
		// Cmd.CHANGE_PASSWORD
		else if (Cmd.CHANGE_PASSWORD.equalsIgnoreCase(cmdStr)) {
			
			// get password
			String pass;
			try {
				pass = params.getString(Cmd.Q_PASSWORD);
			} catch (NullPointerException e) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			if ("".equals(pass)) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			
			// get user info
			// common error, if no such user (could happen only if logged in, 
			// then user is deleted and 'change_pass' is called afterwards)
			String user = params.getString(Field.SESSION_USER_NAME);
			Row row = Worker.getUserRow(user);
			if (row == null) {
				EngineUtils.putStatus(builder, Response.CODE_ERROR);
				return;
			}
			
			// set a new password 
			row.set(Worker.FIELD_PASS, Worker.encryptPassword(pass));
		}
		
		// Cmd.USER_INFO
		else if (Cmd.USER_INFO.equalsIgnoreCase(cmdStr)) {

			// get user name
			String name;
			try {
				name = params.getString(Cmd.Q_USER);
			} catch (NullPointerException e) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			if ("".equals(name)) {
					EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
					return;
				}
					
			getUserInfo(name, builder);
		}
		
		// Cmd.DELETE_USER
		else if (Cmd.DELETE_USER.equalsIgnoreCase(cmdStr)) {

			// get user name
			String name;
			try {
				name = params.getString(Cmd.Q_USER);
			} catch (NullPointerException e) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			if ("".equals(name)) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}

			Worker.deleteUser(name, builder);
		}
		
		// Cmd.LIST_USERS
		else if (Cmd.LIST_USERS.equalsIgnoreCase(cmdStr)) {
			JsonArrayBuilder usersArray = Json.createArrayBuilder();
			Iterator<Row> iter = Worker.getUsersDataSource().getAll();
			while (iter.hasNext()) {
				Row r = iter.next();
				JsonObjectBuilder b = Json.createObjectBuilder();
				getUserInfo(r.getString(Worker.FIELD_ID), b);
				usersArray.add(b.build());
			}
			
			builder.add(Field.USERS, usersArray.build());
		}
		
		// Cmd.ADD_USER
		else if (Cmd.ADD_USER.equalsIgnoreCase(cmdStr)) {

			// get the required parameters - name, pass, home dir
			String name, pass, dir;
			try { 
				name = params.getString(Cmd.Q_USER);
				pass = params.getString(Cmd.Q_PASSWORD);
				dir = params.getString(Cmd.Q_DIR);
			} catch (NullPointerException e) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			if ("".equals(name) || "".equals(pass) || "".equals(dir)) {
				EngineUtils.putStatus(builder, Response.CODE_MISSING_PARAMETER);
				return;
			}
			name = EngineUtils.urlDecode(name);
			pass = EngineUtils.urlDecode(pass);
			dir = EngineUtils.urlDecode(dir);
			
			// add new user
			Row row = Worker.addUser(name, pass, dir);
			if (row == null) {
				EngineUtils.putStatus(builder, Response.CODE_WRONG_PARAMETER);
				return;
			}
		}
		
		// Cmd.EDIT_USER
		else if (Cmd.EDIT_USER.equalsIgnoreCase(cmdStr)) {

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

			// get user info
			// common error, if no such user (could happen only if logged in, 
			// then user is deleted and 'edit_user' is called afterwards)
			String user = params.getString(Field.SESSION_USER_NAME);
			Row row = Worker.getUserRow(user);
			if (row == null) {
				EngineUtils.putStatus(builder, Response.CODE_ERROR);
				return;
			}

			// set field's value
			int idx = row.getDataSource().getFieldIndex(property);
			if (idx == -1) {
				EngineUtils.putStatus(builder, Response.CODE_WRONG_PARAMETER);
				return;
			}
			Object val = null;
			Class<?> type = row.getDataSource().getTypes()[idx];
			try {
				if (type == Integer.class) val = new Integer(value);
				else if (type == Long.class) val = new Long(value);
				else if (type == Boolean.class) val = new Boolean(value);
				else if (type == String.class) val = value.toString();
				else val = value;
			} catch (NumberFormatException e) {
				EngineUtils.putStatus(builder, Response.CODE_WRONG_PARAMETER);
				return;
			}
			row.set(idx, val);
		}
		
		EngineUtils.putStatus(builder, Response.CODE_OK);
	}
	
	protected void getUserInfo(String name, JsonObjectBuilder builder) {
		
		// get user info
		Row row = Worker.getUserRow(name);
		if (row == null) {
			EngineUtils.putStatus(builder, Response.CODE_NO_SUCH_USER);
			return;
		}
				
		// write user fields
		String[] fields = row.getFields();
		for (int i = 0; i < fields.length; i++) {
			String field = fields[i].toLowerCase();
			Object val = row.get(i);
			if (val.getClass() == Integer.class) 
				builder.add(field, (Integer) row.get(i));
			else if (val.getClass() == Long.class) 
				builder.add(field, (Long) row.get(i));
			else 
				builder.add(field, "" + row.get(i));	
		}
	}
}
