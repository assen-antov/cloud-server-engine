package com.infrarch.engine.worker;

import java.io.File;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.infrarch.commons.db.BinaryFileConnector;
import com.infrarch.commons.db.DataSourceUtils;
import com.infrarch.commons.db.DefaultDataSource;
import com.infrarch.commons.db.Row;

/**
 * A user account manager.
 * 
 * @author Assen Antov
 * @version 1.0, 07/2016
 */
public class UserManager {

	private static final Logger logger = Logger.getLogger(UserManager.class);
	
	private static final String USERS_FILE = "users";

	public static final String FIELD_ID = "id";
	public static final String FIELD_PASS = "pass";
	public static final String FIELD_LEVEL = "level";
	public static final String FIELD_HOME = "home";

	private static final String[] USER_FIELDS = new String[] {
		FIELD_ID, FIELD_PASS, FIELD_LEVEL, FIELD_HOME
	};
	private static final Class<?>[] USER_TYPES = new Class[] { 
		String.class, String.class, Integer.class, String.class
	};
	
	private static final UserManager instance = new UserManager();
	private DefaultDataSource ds;
	
	private UserManager() {
		File file = getUsersFile();
		if (!file.exists()) {
			ds = createDefaultUsersFile(file);
		}
		else {
			ds = (DefaultDataSource) DataSourceUtils.loadBinary(file);
			if (ds == null) {
				logger.error("Could not load users file: " + file.getAbsolutePath());
				ds = createDefaultUsersFile(file);
			}
			else {
				logger.debug("Loaded users file: " + file.getAbsolutePath());
			}
		}
		
		ds.setConnector(new BinaryFileConnector(file, ds)); // connector to save with
		ds.setAutoFlushThreshold(1); // save on each change
	}
	
	private DefaultDataSource createDefaultUsersFile(File file) {
		DefaultDataSource ds = new DefaultDataSource("Users", USER_FIELDS, USER_TYPES);
		
		File p = file.getParentFile();
		if (p != null) p.mkdirs();
		
		logger.debug("Created default users file: " + file.getAbsolutePath());
		
		return ds;
	}
	
	/**
	 * Returns the single manager instance.
	 * 
	 * @return manager instance
	 */
	public static UserManager getInstance() {
		return instance;
	}
	
	/**
	 * Returns manager's {@code DataSource}.
	 * 
	 * @return manager's {@code DataSource}
	 */
	public DefaultDataSource getDataSource() {
		return ds;
	}
	
	private File getUsersFile() {
		return new File(ConfigurationManager.getInstance().getConfigRoot() + USERS_FILE);
	}
		
	/**
	 * Returns a {@code Row} for the parameter user ID or <code>null</code>, if
	 * no such user.
	 * 
	 * @param id user ID to query
	 * @return user's {@code Row}
	 */
	public Row getUserRow(String id) {
		return ds.getFirst(FIELD_ID, id);
	}
	
	/**
	 * Returns an {@code Iterator} with all user {@code Row}s.
	 * 
	 * @return an {@code Iterator} with all users
	 */
	public Iterator<Row> getUsers() {
		return ds.getAll();
	}
	
	/**
	 * Deletes the user with the parameter ID.
	 * 
	 * @param id user ID
	 * @return true on success, false if no such user
	 */
	public boolean deleteUser(String id) {
		Row user = ds.getFirst(FIELD_ID, id);
		if (user == null) return false;
		return user.delete();
	}

	/**
	 * Determines whether a user has access to a specific directory.
	 * 
	 * @param id user ID to query
	 * @param dir directory (relative) to query
	 * @return true if the user has access to the directory
	 */
	public boolean checkUserAccess(String id, String dir) {
		String baseDir = ConfigurationManager.getInstance().getDocRoot();
		String absPath = new File(baseDir, dir).getAbsolutePath();
		String root = getUserRow(id).getString(FIELD_HOME);
		File rootDir = new File(baseDir, root);
		return absPath.startsWith(rootDir.getAbsolutePath());
	}
}
