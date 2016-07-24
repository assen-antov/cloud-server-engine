package com.infrarch.engine.constants;

public interface Cmd {

	// query parameters
	public static final String Q_COMMAND = "command";
	public static final String Q_DIR = "dir";
	public static final String Q_FILE_NAME = "file_name";
	public static final String Q_USER = "name";
	public static final String Q_PASSWORD = "pass";
	public static final String Q_PROPERTY = "property";
	public static final String Q_VALUE = "value";
	public static final String Q_COMMENT = "comment";
	public static final String Q_SRC_DIR = "src_dir";
	public static final String Q_DEST_DIR = "dest_dir";
	public static final String Q_OVERWRITE = "overwrite";
	
	// commands
	public static final String LIST_CONTENTS = "list_contents";
	public static final String LIST_DIRS = "list_dirs";
	public static final String LIST_FILES = "list_files";
	public static final String FILE_INFO = "file_info";
	public static final String PING = "ping";
	public static final String DOWNLOAD = "download";
	public static final String UPLOAD = "upload";
	public static final String RECYCLE_FILE = "recycle_file";
	public static final String RECYCLE_DIRECTORY = "recycle_dir";
	public static final String LOGIN = "login";
	public static final String LOGOUT = "logout";
	public static final String CHANGE_PASSWORD = "change_pass";
	public static final String USER_INFO = "user_info";
	public static final String SERVER_INFO = "server_info";
	public static final String SETTINGS_GET = "settings_get";
	public static final String SETTINGS_SET = "settings_set";
	public static final String LIST_SETTINGS = "list_settings";
	public static final String LIST_USERS = "list_users";
	public static final String MAKE_DIR = "make_dir";
	public static final String DELETE_USER = "delete_user";
	public static final String ADD_USER = "add_user";
	public static final String EDIT_USER = "edit_user";
	public static final String COPY_FILE = "copy_file";
	public static final String MOVE_FILE = "move_file";
}