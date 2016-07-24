package com.infrarch.commons.db;

import java.util.Iterator;

/**
 * This interface defines a set of basic methods to operate various data
 * sources. Such sources may be SQL tables or DBF databases, as well as
 * delimited text files. Field names and types can be obtained using the
 * {@link #getFields()} and {@link #getTypes()} methods. Simple queries can be
 * performed using the {@link #get(String, Object)} method. The query methods
 * return {@code Iterator}s of {@link Row} instances. {@code Row}s can be used
 * to manipulate the data in the {@code DataSource}.
 * 
 * @author Assen Antov
 * @version 1.2, 02/2006
 * @version 2.0, 05/2016
 * 
 * @see Row
 */
public interface DataSource {

	/**
	 * Used to obtain the name of the data source. It may include spaces and 
	 * special symbols.
	 * 
	 * @return the name of the data source
	 */
	public String getName();

	/**
	 * Returns the names of the available fields. The names are transformed to
	 * upper case but references to them may be case insensitive. The exact format of
	 * field names depends on the underlying database.
	 * 
	 * @return an array of the available field names
	 */
	public String[] getFields();

	/**
	 * Returns the types of the available fields. The types are implementation
	 * specific. A type may be not only one of the primitive data types or their
	 * wrapper classes, but any class.
	 * 
	 * @return an array of fields' types
	 */
	public Class<?>[] getTypes();

	/**
	 * Adds a field with the specified name and type to the table. All rows will
	 * be initialized with a default instance of the type or <code>null</code>.
	 * <p>
	 * Note: this is an optional operation.
	 * 
	 * @param field field name to add
	 * @param type field class
	 */
	public void addField(String field, Class<?> type);
	
	/**
	 * Deletes a field from the table.
	 * <p>
	 * Note: this is an optional operation.
	 * 
	 * @param field field to delete
	 * @return success or failure
	 */
	public boolean deleteField(String field);
	
	/**
	 * Returns an iterator of <code>Row</code> instances, representing the rows
	 * for which the parameter <code>field</code> is equal to the parameter
	 * <code>value</code>. Some implementations may return {@code ListIterator}s.
	 * 
	 * @param field field to query
	 * @param value value to query
	 * @return an iterator of rows
	 * @see Row
	 */
	public Iterator<Row> get(String field, Object value);
	
	/**
	 * Returns the first <code>Row</code> for which the parameter <code>field</code> 
	 * is equal to the parameter <code>value</code>.
	 * 
	 * @param field field to query
	 * @param value value to query
	 * @return {@code Row} or <code>null</code>
	 * @see Row
	 */
	public Row getFirst(String field, Object value);

	/**
	 * Returns an iterator of <code>Row</code> instances for all records. Some 
	 * implementations may return {@code ListIterator}s. 
	 * 
	 * @return an iterator of all rows
	 * @see Row
	 */
	public Iterator<Row> getAll();

	/**
	 * Searches the {@code DataSource} to return the value of field {@code valueField} 
	 * of the first {@code Row} which {@code keyField} is found to be equal to 
	 * the {@code key}. 
	 * 
	 * @param keyField the key field
	 * @param key key to find
	 * @param valueField the field to lookup 
	 * @return value of {@code valueField} or <code>null</code>
	 */
	public Object lookup(String keyField, Object key, String valueField);
	
	/**
	 * Sets the value of {@code keyField} to {@code value} for all rows which 
	 * {@code keyField} is found to be equal to {@code key}. 
	 * 
	 * @param keyField the key field
	 * @param key key to find
	 * @param value the value to set 
	 * @return the number of changes made
	 */
	public int set(String keyField, Object key, Object value);
	
	/**
	 * Sets the value of {@code field} to {@code value} for all rows which
	 * {@code keyField} is found to be equal to {@code key}. 
	 * 
	 * @param keyField the key field
	 * @param key key to find
	 * @param field the field to set
	 * @param value the value to set 
	 * @return the number of changes made
	 */
	public int set(String keyField, Object key, String field, Object value);
	
	/**
	 * Sets the value of {@code keyField} to {@code value} for the first row which 
	 * {@code keyField} is found to be equal to {@code key}. 
	 * 
	 * @param keyField the key field
	 * @param key key to find
	 * @param value the value to set 
	 * @return the number of changes made
	 */
	public int setFirst(String keyField, Object key, Object value);
	
	/**
	 * Sets the value of {@code field} to {@code value} for the first row which 
	 * {@code keyField} is found to be equal to {@code key}. 
	 * 
	 * @param keyField the key field
	 * @param key key to find
	 * @param field the field to set
	 * @param value the value to set 
	 * @return the number of changes made
	 */
	public int setFirst(String keyField, Object key, String field, Object value);
	
	/**
	 * Deletes all rows which {@code keyField} is found to be equal to {@code key}. 
	 * 
	 * @param keyField the key field
	 * @param key key to find
	 * @return the number of deletions made
	 */
	public int delete(String keyField, Object key);
	
	/**
	 * Deletes the first row which {@code keyField} is found to be equal to {@code key}. 
	 * 
	 * @param keyField the key field
	 * @param key key to find
	 * @return the number of deletions made
	 */
	public int deleteFirst(String keyField, Object key);
	
	/**
	 * Appends the parameter data to the {@code DataSource}.
	 * 
	 * @param data data for the row to append
	 * @return a <code>Row</code> instance that was created as a result of the
	 *         addition or <code>null</code>, if the addition was unsuccessful
	 */
	public Row append(Object[] data);

	/**
	 * Returns the index of the parameter field.
	 * 
	 * @return the index of the field or -1 if unavailable
	 */
	public int getFieldIndex(String field);

	/**
	 * Calling this method on the {@code DataSource} will result in
	 * permanently saving any changes made using {@code DataSource}'s methods and
	 * {@link Row#set(int, Object)}, {@link Row#set(String, Object)} and
	 * {@link Row#delete()} methods. It should be expected that until such time
	 * that the method is invoked any changes to the {@code DataSource}
	 * exist only in memory. It is possible, however, that an implementation
	 * permanently saves changes immediately and does not need this method to be
	 * called. Although an implementation may occasionally perform flushes, it
	 * is not guaranteed that they are automatically invoked. 
	 * <p>
	 * If the {@code DataSource} implementation is thread-safe and locks 
	 * operations with the {@code DataSource} while flushing, the method must set
	 * the changes counter to 0. If {@code DataSource} operations are not locked
	 * while flushing, the method should keep track of concurrent changes.
	 */
	public void flush();

	/**
	 * Closes the {@code DataSource} and releases any external resources used.
	 * <p>
	 * Note: this is an optional operation.
	 */
	public void close();
	
	/**
	 * Returns the number of rows in the {@code DataSource}.
	 * <p>
	 * Note: this is an optional operation.
	 * 
	 * @return number of rows
	 */
	public int size();

	/**
	 * Returns a positive number, if changes to the {@code DataSource} or 
	 * to its {@code Row}s have been made since loading or the last {@link #flush()}. 
	 * This number may or may not be equal to the number of changes that have occurred. 
	 * If no changes have been made since loading or the last {@link #flush()}, the 
	 * method returns 0.
	 * 
	 * @return if there have been changes or not
	 */
	public int hasChanged();
	
	/**
	 * Deletes all rows of the {@code DataSource}.
	 */
	public void clear();
	
	/**
	 * Marks that a change has occurred to the {@code DataSource} or the data
	 * stored in its {@code Row}. Must be called by all methods that change the
	 * structure of the {@code DataSource} and the data stored in its
	 * {@code Row}s. 
	 */
	public void changed();
	
	/**
	 * Specifies a field to order the results of queries by.
	 * <p>
	 * Note: this is an optional operation.
	 * 
	 * @param field field name
	 */
	public void setOrderingField(String field);
	
	/**
	 * Returns the field used to order the results of queries by.
	 * <p>
	 * Note: this is an optional operation.
	 * 
	 * @param field field name
	 */
	public String getOrderingField();
	/**
	 * Determines how query results will be sorted - in ascending or descending order.
	 * <p>
	 * Note: this is an optional operation.
	 * 
	 * @param b <code>true</code> for ascending, <code>false</code> for descending
	 */
	public void setOrderAscending(boolean b);
	
	/**
	 * Returns how query results are sorted - in ascending or descending order.
	 * <p>
	 * Note: this is an optional operation.
	 * 
	 * @return <code>true</code> for ascending, <code>false</code> for descending
	 */
	public boolean getOrderAscending();
}