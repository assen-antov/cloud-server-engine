package com.infrarch.commons.db;

/**
 * Represents a row of a {@code DataSource}. 
 * 
 * @author Assen Antov
 * @version 1.2, 02/2006
 * @version 2.0, 05/2016
 * 
 * @see DataSource
 * @see DataSource#get(String, Object)
 * @see DataSource#getAll()
 */
public interface Row {

	/**
	 * Returns the values of row's fields.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 * 
	 * @return an array of field values
	 */
	public Object[] getData();

	/**
	 * Sets the values of row's fields.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 * <p>
	 * Note: this is an optional operation.
	 * 
	 * @param data an array of field values
	 */
	public boolean setData(Object[] data);
	
	/**
	 * Gets the data from the field at the parameter index.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 *  
	 * @param field field index to get data from
	 * @return the data from the field at the requested index
	 */
	public Object get(int field);

	/**
	 * Gets the data from the field with the given name.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 *  
	 * @param field field name to get data from
	 * @return the data from the field with the requested name
	 */
	public Object get(String field);
	
	/**
	 * Gets the data from the field with the given name as {@code int}.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 *  
	 * @param field field name to get data from
	 * @return the data from the field with the requested name
	 */
	public int getInteger(String field);
	
	/**
	 * Gets the data from the field with the given name as {@code long}.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 *  
	 * @param field field name to get data from
	 * @return the data from the field with the requested name
	 */
	public long getLong(String field);
	
	/**
	 * Gets the data from the field with the given name as {@code float}.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 *  
	 * @param field field name to get data from
	 * @return the data from the field with the requested name
	 */
	public float getFloat(String field);
	
	/**
	 * Gets the data from the field with the given name as {@code double}.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 *  
	 * @param field field name to get data from
	 * @return the data from the field with the requested name
	 */
	public double getDouble(String field);
	
	/**
	 * Gets the data from the field with the given name as {@code String}.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 *  
	 * @param field field name to get data from
	 * @return the data from the field with the requested name
	 */
	public String getString(String field);
	
	/**
	 * Gets the data from the field with the given name as {@code char}.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 *  
	 * @param field field name to get data from
	 * @return the data from the field with the requested name
	 */
	public char getChar(String field);
	
	/**
	 * Gets the data from the field with the given name as {@code byte}.
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 *  
	 * @param field field name to get data from
	 * @return the data from the field with the requested name
	 */
	public byte getByte(String field);
	
	/**
	 * Sets the data for the field at the parameter index. 
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 * <p>
	 * Note: this is an optional operation.
	 *  
	 * @param field index of the field to set
	 * @param value value to set
	 * @return success or failure
	 */
	public boolean set(int field, Object value);
	
	/**
	 * Sets the data for the field with the given name. 
	 * <p>
	 * Will throw an {@code IllegalStateException}, if called after {@link #delete()}.
	 * <p>
	 * Note: this is an optional operation.
	 *  
	 * @param field name of the field to set
	 * @param value value to set
	 * @return success or failure
	 */
	public boolean set(String field, Object value);
	
	/**
	 * Deletes the row from the data source.
	 * <p>
	 * Note: this is an optional operation.
	 *
	 * @return success or failure
	 */
	public boolean delete();
	
	/**
	 * Returns the state of the {@code Row} - true if deleted, false otherwise.
	 * <p>
	 * Note: if {@link #delete()} is unsupported, the method must always
	 * return <code>false</code>.
	 * 
	 * @return if the {@code Row} has been deleted
	 */
	public boolean isDeleted();
	
	/**
	 * Returns the names of the available fields. The names are transformed to
	 * upper case. References to them are case insensitive. The exact format of
	 * field names depends on the underlying database.
	 * 
	 * @return an array of the available field names
	 */
	public String[] getFields();
	
	/**
	 * Returns the <code>DataSource</code> the row belongs to. 
	 * <p>
	 * Will throw an {@code IllegalStateException} if called after {@link #delete()}.
	 * 
	 * @return the parent data source
	 */
	public DataSource getDataSource();
}