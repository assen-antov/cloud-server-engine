package com.infrarch.commons.db;

import java.io.File;

/**
 * Connects a {@code DataSource} to a file.
 * 
 * @author Assen Antov
 * @version 1.0, 05/2016
 */
public abstract class AbstractFileConnector implements Connector {

	private final File file;
	private final DataSource ds;
	
	/**
	 * Constructs a {@code Connector} instance from file the {@code Connector}
	 * must be associated to and a {@code DataSource} to connect.
	 * 
	 * @param file {@code Connector}'s file
	 * @param ds the {@code DataSource} to connect
	 */
	public AbstractFileConnector(File file, DataSource ds) {
		if (file == null || ds == null) throw new IllegalArgumentException("cannot construct Connector with null parameters");
		this.file = new File(file.getAbsolutePath());
		this.ds = ds;
	}

	/**
	 * Returns the file this {@code Connector} is associated to.
	 * 
	 * @return {@code Connector}'s file
	 */
	public File getFile() {
		return new File(file.getAbsolutePath());
	}
	
	/**
	 * Returns the connected {@code DataSource}.
	 * 
	 * @return {@code Connector}'s {@code DataSource}
	 */
	public DataSource getDataSource() {
		return ds;
	}

	@Override
	public abstract boolean save();
}
