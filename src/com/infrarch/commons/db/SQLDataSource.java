package com.infrarch.commons.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * An SQL database wrapper class. The implementation is thread-safe: that is, 
 * no concurrency issues should arise, if a single {@code SQLDataSource} is
 * used by more than one thread, regardless to whether the underlying
 * SQL {@code Connection} is thread-safe or not.
 * 
 * @author Assen Antov
 * @version 1.2, 02/2006
 * @version 2.0, 05/2016
 */
public class SQLDataSource implements DataSource {

	protected final AtomicInteger changes = new AtomicInteger(0);
	private final Connection connection;
	private String table;
	private final Statement statement;
	
	private String[] fields;
	private Class<?>[] types;
	
	private String orderingField = null;
	private boolean ascending = true;
	
	private PreparedStatement psGetAll, psDeleteAll, psAppend;
	private PreparedStatement[] psGetColumn;
		
	/**
	 * Creates an instance by driver, URL of the database and table name to 
	 * operate. 
	 * 
	 * @param driver driver to use
	 * @param url URL of the database 
	 * @param table table name
	 */
	public SQLDataSource(String driver, String url, String table) {		
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			log("cannot find database driver class: " + driver);
			throw new RuntimeException(e);
		}
		
		try {
			connection = DriverManager.getConnection(url);
		} catch (SQLException e) {
			log("cannot create connection to database: " + url);
			log(e.toString());
			throw new RuntimeException(e);
		}
		
		try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			log("cannot create statement for database: " + url);
			log(e.toString());
			throw new RuntimeException(e);
		}
		
		setTable(table);
	}
	
	/**
	 * Creates an instance by driver, URL of the database and table name to 
	 * operate. Creates a table with the parameter fields and classes, if no
	 * table with the same name exists.
	 * 
	 * @param driver driver to use
	 * @param url URL of the database 
	 * @param fields table fields
	 * @param types table types
	 * @param table table name
	 */
	public SQLDataSource(String driver, String url, String table, String[] fields, Class<?>[] types) {
		if (fields == null || types == null) throw new IllegalArgumentException("attempt to set null fields or types");
		if (fields.length != types.length) throw new IllegalArgumentException("fields.length != types.length: " + fields.length + " != " + types.length);
		
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			log("cannot find database driver class: " + driver);
			throw new RuntimeException(e);
		}
		
		try {
			connection = DriverManager.getConnection(url);
		} catch (SQLException e) {
			log("cannot create connection to database: " + url);
			log(e.toString());
			throw new RuntimeException(e);
		}
		
		try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			log("cannot create statement for database: " + url);
			log(e.toString());
			throw new RuntimeException(e);
		}
		
		String sql = "CREATE TABLE IF NOT EXISTS " + table + " (";
		for (int i = 0; i < fields.length; i++) {
			if (i != 0) sql += ", ";
			sql += fields[i] + " " + getSQLTypeAsString(types[i]);
		}
		sql += ")";

		try {
			statement.execute(sql);
		} catch (SQLException e) {
			log("cannot create table for database: " + url);
			log(e.toString());
			throw new RuntimeException(e);
		}

		setTable(table);
	}
	
	
	/**
	 * Creates an instance from an existing {@code Connection} and table name.
	 * 
	 * @param connection the {@link Connection} to use
	 * @param table table name
	 */
	public SQLDataSource(Connection connection, String table) {
		this.connection = connection;
		
		try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			log("cannot create statement from connection: " + connection);
			log(e.toString());
			throw new RuntimeException(e);
		}

		setTable(table);
	}
	
	/**
	 * Reads database's meta data to populate {@code DataSource}s internal fields.
	 */
	protected void readMetaData() {
		try {
			DatabaseMetaData md = connection.getMetaData();	// get db metadata
			ResultSet rs = md.getColumns(null, null, table, "%" );	// get all columns of the active table

			if (fields == null) fields = new String[1];
			if (types == null) types = new Class<?>[1];
			
			List<String> fld = new LinkedList<String>();
			List<Class<?>> typ = new LinkedList<Class<?>>();
			while (rs.next()) {
				fld.add(rs.getString("COLUMN_NAME")); 
				typ.add(getColumnClass(rs.getString("TYPE_NAME")));
				//log(rs.getString("COLUMN_NAME") + " " + rs.getInt("DATA_TYPE") + " " + rs.getString("TYPE_NAME"));
			}
			fields = fld.toArray(fields);
			types = typ.toArray(types);
			
		} catch (SQLException e) {
			log("error wihile reading database structure: " + e);
			throw new RuntimeException(e);
		}
		changes.set(0);
	}
	
	protected static Class<?> getColumnClass(String type) {
		if (type.equals("CHAR") || type.equals("VARCHAR") || type.equals("LONGVARCHAR") || type.equals("TEXT")) 
			return String.class;
		else if (type.equals("BIT")) 
			return Boolean.class;
		else if (type.equals("TINYINT") ||type.equals("SMALLINT") || type.equals("INTEGER"))
			return Integer.class;
		else if (type.equals("BIGINT"))
			return Long.class;
		else if (type.equals("FLOAT"))
			return Float.class;
		else if (type.equals("DOUBLE"))
			return Double.class;
		else if (type.equals("DATE"))
			return Date.class;
		else if (type.equals("TIME"))
			return Time.class;
		else if (type.equals("TIMESTAMP"))
			return Timestamp.class;
		else
			return Object.class;
	}
	
	protected static String getSQLTypeAsString(Class<?> type) {
		if (type == String.class) return "TEXT";
		else if (type == Boolean.class) return "BIT";
		else if (type == Integer.class) return "INTEGER";
		else if (type == Long.class) return "BIGINT";
		else if (type == Float.class) return "REAL";
		else if (type == Double.class) return "DOUBLE";
		else if (type == Date.class) return "DATE";
		else if (type == Time.class) return "TIME";
		else if (type == Timestamp.class) return "TIMESTAMP";
		else return "TEXT";
	}
	
	/**
	 * Prepares statements likely to be used.
	 */
	protected void prepareStatements() {
		try {
			// prepare for getAll()
			String SQL_GET_ALL = "SELECT * FROM " + table;
			if (orderingField != null) SQL_GET_ALL += " ORDER BY " + orderingField + (ascending? " ASC" : " DESC");
			psGetAll = prepare(SQL_GET_ALL, null);
			
			// prepare for get(String, Object)
			psGetColumn = new PreparedStatement[fields.length];
			for (int i = 0; i < psGetColumn.length; i++) {
				String SQL_GET_COLUMN = "SELECT * FROM " + table + " WHERE " + fields[i] + " = ?";
				if (orderingField != null) SQL_GET_COLUMN += " ORDER BY " + orderingField + (ascending? " ASC" : " DESC");
				psGetColumn[i] = prepare(SQL_GET_COLUMN, null);
			}
			
			// prepare for append(Object[])
			String SQL_INSERT = "INSERT INTO " + table + " VALUES (";
			for (int i = 0; i < fields.length; i++) {
				if (i != 0) SQL_INSERT += ", ";
				SQL_INSERT += "?";
			}
			SQL_INSERT += ")";
			psAppend = prepare(SQL_INSERT, null);
			
			// prepare for clear()
			String SQL_DELETE_ALL = "DELETE FROM " + table;
			psDeleteAll = prepare(SQL_DELETE_ALL, null);
		} catch (SQLException e) {
			log("error wihile preparing statements: " + e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Prepares a statement from query string and data bindings.
	 */
	private PreparedStatement prepare(String query, Object[] bindings) throws SQLException {
		PreparedStatement stat = connection.prepareStatement(query);
		int index = 1;
		if (bindings != null) {
			for (Object value: bindings) {
				if (value != null) stat.setObject(index++, value);
			}
		}
		return stat;
	}
	
	@Override
	public synchronized String[] getFields() {
		return fields;
	}
	
	@Override
	public synchronized Class<?>[] getTypes() {
		return types;
	}
	
	
	@Override
	public synchronized void addField(String field, Class<?> type) {
		String sql = "ALTER TABLE " + table + " ADD " + field + " " + getSQLTypeAsString(type);
		executeUpdate(sql);
	}

	@Override
	public synchronized boolean deleteField(String field) {
		int idx = getFieldIndex(field);
		if (idx == -1) return false;
		
		String sql = "ALTER TABLE " + table + " DROP " + field;
		int ch;
		try {
			ch = executeUpdate(sql);
		} catch (RuntimeException e) {
			return false;
		}
		return ch == 0? false : true;
	}

	@Override
	public synchronized void setOrderingField(String field) {
		if (field == null) throw new IllegalStateException("attempt to set a null ordering field");
		if (orderingField != field) {
			orderingField = field;
			prepareStatements();
		}
	}
	
	@Override
	public synchronized String getOrderingField() { 
		return orderingField; 
	}
	
	@Override
	public synchronized void setOrderAscending(boolean b) { 
		if (ascending != b) {
			ascending = true;
			prepareStatements();
		}
	}
	
	@Override
	public synchronized boolean getOrderAscending() { 
		return ascending; 
	}
	
	/**
	 * Returns the SQL table being operated.
	 * 
	 * @return SQL table wrapped by the {@code SQLDataSource}
	 */
	public synchronized String getTable() {
		return table;
	}

	/**
	 * Sets the SQL table to be operated by this {@code SQLDataSource}.
	 * 
	 * @param table table to be operated
	 */
	public synchronized void setTable(String table) {
		if (table == null) throw new IllegalStateException("attempt to set a null table");
		if (this.table != table) {
			this.table = table;
			readMetaData();
			prepareStatements();
		}
	}

	@Override
	public synchronized Iterator<Row> get(String field, Object value) {
		int idx = getFieldIndex(field);
		if (idx == -1) {
			throw new IllegalArgumentException("no such field: " + field);
		}
		
		try {
			psGetColumn[idx].setObject(1, value);
			ResultSet rs = psGetColumn[idx].executeQuery();
			return new ImmutableListIterator<Row>(toList(rs).listIterator());
		} catch (SQLException e) {
			log("error executing query: " + psGetColumn[idx]);
			log(e.toString());
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public synchronized Iterator<Row> getAll() {
		try {
			ResultSet rs = psGetAll.executeQuery();
			return new ImmutableListIterator<Row>(toList(rs).listIterator());
		} catch (SQLException e) {
			log("error executing query: " + psGetAll);
			log(e.toString());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Creates a {@code LinkedList} from a {@code ResultSet}.
	 */
	public List<Row> toList(ResultSet rs) {
		List<Row> result = new LinkedList<Row>();
		try	{
			while (rs.next()) {
				Object[] data = new Object[fields.length];
				for (int i = 0; i < fields.length; i++)
					data[i] = rs.getObject(i+1);
				result.add(new ImmutableRow(this, data));
			}
		} catch (SQLException e) {
			log("error reading ResultSet: " + rs);
			log(e.toString());
			throw new RuntimeException(e);
		}
		return result;
	}
	
	@Override
	public synchronized int getFieldIndex(String field) {
		if (fields == null) throw new IllegalStateException("fields not set");
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].equalsIgnoreCase(field)) {
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public synchronized String getName() {
		return table;
	}

	@Override
	public Row getFirst(String field, Object value) {
		Iterator<Row> iter = get(field, value);
		if (iter.hasNext()) return iter.next();
		else return null;
	}

	@Override
	public int hasChanged() {
		return changes.get();
	}
	
	@Override
	public int size() {
		ResultSet rs = executeQuery("SELECT COUNT(*) FROM " + table);
		try {
			return rs.getInt(1);
		} catch (SQLException e) {
			log("error fetching table size for: " + table);
			log(e.toString());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Used to log events. May be overridden by superclasses.
	 * 
	 * @param s the event to log
	 */
	protected void log(String s) {
		System.err.println("[" + getClass().getName() + ", \"" + getName()+ "\"]: " + s);
	}
	
	@Override
	public synchronized Row append(Object[] data) {
		if (fields.length != data.length) 
			throw new IllegalArgumentException("fields.length != data.length: " + fields.length + "!=" + data.length);
		
		try {
			for (int i = 0; i < data.length; i++) {
				psAppend.setObject(i+1, data[i]);
			}
			psAppend.executeUpdate();
		} catch (SQLException e) {
			log("error appending record to table");
			log(e.toString());
			throw new RuntimeException(e);
		}
		changes.incrementAndGet();
		return new ImmutableRow(this, data);
	}
	
	@Override
	public synchronized void flush() {
		try {
			if (!connection.getAutoCommit()) {
				connection.commit();
			}
		} catch (SQLException e) {
			log("could not commit table: " + table);
			log(e.toString());
		}
		changes.set(0);
	}

	@Override
	public synchronized void clear() {
		int ch = 0;
		try {
			ch = psDeleteAll.executeUpdate();
		} catch (SQLException e) {
			log("error executing query: " + psDeleteAll);
			log(e.toString());
			throw new RuntimeException(e);
		}
		changes.addAndGet(ch);
	}
	
	@Override
	public synchronized Object lookup(String keyField, Object key, String valueField) {
		String query = "SELECT " + valueField + " FROM " + table + " WHERE " + keyField + "=" + objToString(key);
		if (orderingField != null) query += " ORDER BY " + orderingField + (ascending? " ASC" : " DESC");
		
		ResultSet rs = executeQuery(query);
		try {
			while (rs.next()) {
				return rs.getObject(1);
			}
		} catch (SQLException e) {
			log("error executing query: " + query);
			log(e.toString());
			throw new RuntimeException(e);
		}
				
		return null;
	}
	

	@Override
	public int set(String keyField, Object key, Object value) {
		return set(keyField, key, keyField, value);
	}

	@Override
	public synchronized int set(String keyField, Object key, String field, Object value) {
		String query = "UPDATE " + table + " SET " + field + "=" + objToString(value) + 
				" WHERE " + keyField + "=" + objToString(key);
		int ch = executeUpdate(query);
		if (ch != 0) changes.addAndGet(ch);
		return ch;
	}

	@Override
	public int setFirst(String keyField, Object key, Object value) {
		return setFirst(keyField, key, keyField, value);
	}

	@Override
	public int setFirst(String keyField, Object key, String field, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized int delete(String keyField, Object key) {
		String query = "DELETE FROM " + table + " WHERE " + keyField + "=" + objToString(key);
		int ch = executeUpdate(query);
		if (ch != 0) changes.addAndGet(ch);
		return ch;
	}

	@Override
	public int deleteFirst(String keyField, Object key) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void changed() {
		changes.incrementAndGet();
	}
	
	/**
	 * Surrounds with '', if the type is String or Character.
	 */
	private String objToString(Object key) {
		String result;
		if (key instanceof String || key instanceof Character) result = "'" + key + "'";
		else result = key.toString();
		return result;
	}
	
	/**
	 * Shortcut method to execute unprepared queries.
	 * 
	 * @param query the query to execute
	 * @return a {@link ResultSet}
	 */
	public synchronized ResultSet executeQuery(String query) {
		try {
			ResultSet rs = statement.executeQuery(query);
			return rs;
		} catch (SQLException e) {
			log("error executing query: " + query);
			log(e.toString());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Shortcut method to execute unprepared updates.
	 * 
	 * @param query the update query to execute
	 * @retrurn number of modifications
	 */
	public synchronized int executeUpdate(String query) {
		try {
			return statement.executeUpdate(query);
		} catch (SQLException e) {
			log("error executing update: " + query);
			log(e.toString());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Flushes and closes the SQL connection.
	 * 
	 * @see DataSource#close()
	 */
	public synchronized void close() {
		flush();
		try {
			connection.close();
		} catch (SQLException e) {
			log("could not close connection for table: " + table);
			log(e.toString());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns the auto commit state of the underlying SQL {@link Connection}.
	 * 
	 * @return auto commit mode or not
	 */
	public synchronized boolean getAutoCommit() {
		try {
			return connection.getAutoCommit();
		} catch (SQLException e) {
			log(e.toString());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Sets the auto commit state of the underlying SQL {@link Connection}.
	 * 
	 * @param b auto commit mode or not
	 */
	public synchronized void setAutoCommit(boolean b) {
		try {
			connection.setAutoCommit(b);
		} catch (SQLException e) {
			log(e.toString());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * An immutable {@code Row} implementation to contain data from {@code ResultSet}s.
	 */
	private class ImmutableRow extends AbstractRow implements Serializable {
		
		private static final long serialVersionUID = 7824754762333471156L;
		
		ImmutableRow(DataSource ds, Object[] data) {
			super(ds, data);
		}
		
		@Override
		public boolean setData(Object[] d) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean set(int field, Object value) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean set(String field, Object value) { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean delete() { throw new UnsupportedOperationException(); }
		
		@Override
		public boolean isDeleted() { return false; }
	}
}