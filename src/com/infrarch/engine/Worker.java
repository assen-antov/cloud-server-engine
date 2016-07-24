package com.infrarch.engine;

import java.io.File;
import java.io.IOException;

import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jasypt.util.password.BasicPasswordEncryptor;

import com.infrarch.commons.db.DataSource;
import com.infrarch.commons.db.DefaultDataSource;
import com.infrarch.commons.db.Row;
import com.infrarch.engine.worker.ConfigurationManager;
import com.infrarch.engine.worker.ExtendedMultipartRequest;
import com.infrarch.engine.worker.FileManager;
import com.infrarch.engine.worker.FileUtils;
import com.infrarch.engine.worker.UserManager;
import com.infrarch.engine.constants.Response;

public class Worker {

	public static final String FIELD_PASS = UserManager.FIELD_PASS;
	public static final String FIELD_ID = UserManager.FIELD_ID;

	public static final String KEY_INCORRECT_LOGIN_DELAY = ConfigurationManager.KEY_INCORRECT_LOGIN_DELAY;
	
	/**
	 * Returns the document root directory. No files can be accessed
	 * outside this directory.
	 * 
	 * @return document root
	 */
	public static File getDocRoot() {
		return FileManager.getInstance().getDocRoot();
	}
	
	/**
	 * Returns the configuration directory.
	 * 
	 * @return configuration root
	 */
	public static File getConfigRoot() {
		return new File(ConfigurationManager.getInstance().getConfigRoot());
	}
	
	/**
	 * Checks if a file belongs to the system.
	 * 
	 * @param f file to check
	 * @return file is system or not
	 */
	public static boolean isSystemFile(File f) {
		return FileManager.getInstance().isSystemFile(f);
	}
	
	/**
	 * Determines whether a user has access to a specific directory.
	 * 
	 * @param id user ID to query
	 * @param dir directory (relative) to query
	 * @return true if the user has access to the directory
	 */
	public static boolean checkUserAccess(String id, String dir) {
		return UserManager.getInstance().checkUserAccess(id, dir);
	}
	
	/**
	 * Moves the contents of the specified directory to the recycle bin 
	 * (if such is supported) and deletes the original.
	 * 
	 * @param fDir directory
	 * @return success or failure
	 */
	public static boolean recycleDir(File fDir) {
		return FileUtils.recycleDir(fDir);
	}
	
	/**
	 * Moves the file to the recycle bin (if such is supported) or 
	 * simply deletes it.
	 * 
	 * @param f file to delete
	 * @return success or failure
	 */
	public static boolean recycleFile(File f) {
		return FileUtils.recycleFile(f);
	}
	
	/**
	 * Copies the file referred in the file system by {@code ref} from
	 * the source directory to the destination directory. If {@code overwrite} 
	 * is set to <code>true</code> and a file of the same name exists, it 
	 * will be replaced with the source revision. If {@code overwrite} is set to 
	 * <code>false</code>, a new name for the file will be generated.
	 * 
	 * @param f source file 
	 * @param destDir destination directory 
	 * @param overwrite remove the file with the same name from the destination or not
	 * @return success or failure
	 */
	public static File copyFile(File f, File destDir, boolean overwrite) {
		return FileUtils.copyFile(f, destDir, overwrite);
	}
	
	/**
	 * Moves the file referred in the file system by {@code ref} from
	 * the source directory to the destination directory. If {@code overwrite} 
	 * is set to <code>true</code> and a file of the same name exists, it 
	 * will be replaced with the source revision. If {@code overwrite} is set to 
	 * <code>false</code>, a new name for the file will be generated.
	 * 
	 * @param f source file 
	 * @param destDir destination directory 
	 * @param overwrite remove the file with the same name from the destination or not
	 * @return success or failure
	 */
	public static File moveFile(File f, File destDir, boolean overwrite) {
		return FileUtils.moveFile(f, destDir, overwrite);
	}
		
	private static BasicPasswordEncryptor encryptor = new BasicPasswordEncryptor();
	
	/**
	 * Encrypts a password using the {@link BasicPasswordEncryptor} class.
	 * 
	 * @param password password to encrypt
	 * @return encrypted password
	 */
	public static String encryptPassword(String password) {
		return encryptor.encryptPassword(password);
	}
	
	/**
	 * Matches a non-encrypted password with an encrypted password using the 
	 * {@link BasicPasswordEncryptor} class.
	 * 
	 * @param password user password 
	 * @param encryptedPassword password to match
	 * @return match or not
	 */
	public static boolean matchEncryptedPassword(String password, String encryptedPassword) {
		return encryptor.checkPassword(password, encryptedPassword);
	}

	/**
	 * Determines whether the session is authenticated.
	 * 
	 * @param request request to obtain the session from 
	 * @return user authenticated or not
	 */
	public static boolean isAuthenticated(HttpServletRequest request) {
		return !(!request.isRequestedSessionIdValid() || request.getSession().getAttribute(Engine.ATTRIBUTE_AUTHENTICATED) == null);
	}

	/**
	 * Sets the authentication status of the session.
	 * 
	 * @param request request to obtain the session from 
	 * @param authenticated authentication status to set
	 */
	public static void setAuthenticated(String user, boolean authenticated, HttpServletRequest request) {
		HttpSession session = request.getSession();
		if (authenticated) {
			session.setAttribute(Engine.ATTRIBUTE_USER_NAME, user);
			session.setAttribute(Engine.ATTRIBUTE_AUTHENTICATED, true);
		}
		else {
			session.setAttribute(Engine.ATTRIBUTE_USER_NAME, null);
			session.setAttribute(Engine.ATTRIBUTE_AUTHENTICATED, null);
			session.invalidate();
		}
	}
	
	/**
	 * Returns a {@code Row} for the parameter user ID or <code>null</code>, if
	 * no such user.
	 * 
	 * @param id user ID to query
	 * @return user's {@code Row}
	 */
	public static Row getUserRow(String id) {
		return UserManager.getInstance().getUserRow(id);
	}
	
	/**
	 * Returns the {@code DataSource} with user definitions.
	 * 
	 * @return user's {@code DataSource}
	 */
	public static DefaultDataSource getUsersDataSource() {
		return UserManager.getInstance().getDataSource();
	}
	
	/**
	 * Creates a new user with the minimum possible parameters and 
	 * that is: user name, password and home directory. Any other fields
	 * will be left blank. 
	 * 
	 * @param name user name
	 * @param pass user password
	 * @param dir user home directory
	 * @return <code>null</code>, if a user with the same name already exists
	 */
	public static Row addUser(String name, String pass, String dir) {
		DataSource ds = Worker.getUsersDataSource();
		Row r = ds.getFirst(FIELD_ID, name);
		if (r != null) return null;
		
		return ds.append(new Object[] { name, Worker.encryptPassword(pass), 1, dir });  // TODO: check calling user's privilege level
	}
	
	/**
	 * Deletes the user profile with the parameter name.
	 * 
	 * @param name user profile to delete
	 * @return <code>false</code>, if no such user
	 */
	public static boolean deleteUser(String name, JsonObjectBuilder builder) {
		if (name.equals("admin")) {
			EngineUtils.putStatus(builder, Response.CODE_WRONG_PARAMETER);	// cannot delete account admin
			return false; 
		}
		Row r = getUserRow(name);
		if (r == null) {
			EngineUtils.putStatus(builder, Response.CODE_NO_SUCH_USER);
			return false;
		}
		boolean b = r.delete();	// TODO: check calling user's privilege level
		if (b) EngineUtils.putStatus(builder, Response.CODE_OK);
		else EngineUtils.putStatus(builder, Response.CODE_ERROR);
		return b;
	}
	
	/**
	 * Calculates the size on disk taken by the system. That is, the size
	 * of the document root and configuration directory (the backup root
	 * is not included).
	 * 
	 * @return the size on the disk
	 */
	public static long getSizeOnDisk() {
		return FileManager.getInstance().getSizeOnDisk();
	}
	
	/**
	 * Returns a configuration property.
	 * 
	 * @param key property name to lookup
	 * @return property's value
	 */
	public static String getConfigProperty(String key) {
		return ConfigurationManager.getInstance().get(key);
	}
	
	/**
	 * Returns a configuration property.
	 * 
	 * @param key property name to lookup
	 * @param def default value, if not found 
	 * @return property's value
	 */
	public static String getConfigProperty(String key, String def) {
		return ConfigurationManager.getInstance().get(key, def);
	}
	
	/**
	 * Returns an integer configuration property.
	 * 
	 * @param key property name to lookup
	 * @param def default value, if not found
	 * @return property's value
	 */
	public static int getConfigPropertyInt(String key, int def) {
		return ConfigurationManager.getInstance().getInt(key, def);
	}
	
	/**
	 * Saves a configuration key/value pair.
	 * 
	 * @param key key to use
	 * @param value value to store
	 */
	public static void setConfigProperty(String key, String value) {
		ConfigurationManager config = ConfigurationManager.getInstance();
		config.set(key, value);
		config.store();
	}
	
	/**
	 * Returns all property names.
	 * 
	 * @return an array of property names
	 */
	public static String[] getConfigKeys() {
		return ConfigurationManager.getInstance().getKeys();
	}
	
	/**
	 * A convenience method to create a new {@link ExtendedMultipartRequest}.
	 * 
	 * @param request the HTTP request
	 * @param dirName directory to save the files to
	 * @param overwrite overwrite or not
	 * @return an {@code ExtendedMultipartRequest} instance
	 * @throws IOException 
	 */
	public static ExtendedMultipartRequest getMultipartRequest(HttpServletRequest request, String dirName, boolean overwrite) 
			throws IOException {
		return new ExtendedMultipartRequest(request, dirName, ConfigurationManager.getInstance().getMaxUploadSize(), "UTF-8", overwrite);
	}
}
