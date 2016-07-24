package com.infrarch.commons.db;

import java.io.File;

/**
 * A connector capable of saving {@code DataSources} as binary
 * serialized files.
 * 
 * @author Assen Antov
 * @version 1.0, 05/2016
 */
public class BinaryFileConnector extends AbstractFileConnector {

	/**
	 * Constructs a {@code Connector} instance capable of operating
	 * binary files.
	 * 
	 * @param file {@code Connector}'s file
	 * @param ds the {@code DataSource} to connect
	 */
	public BinaryFileConnector(File file, DataSource ds) {
		super(file, ds);
	}

	@Override
	public synchronized boolean save() {
		return DataSourceUtils.saveBinary(getDataSource(), getFile());
	}
}
