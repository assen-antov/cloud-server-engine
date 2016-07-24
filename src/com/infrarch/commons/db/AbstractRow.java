package com.infrarch.commons.db;

/**
 * Partial implementation of the <code>Row</code> interface.
 * 
 * @author Assen Antov
 * @version 1.2, 02/2006
 * @version 2.0, 05/2016
 */
public abstract class AbstractRow implements Row {

	private Object[] data;
	protected final DataSource ds;
	
	public AbstractRow(DataSource ds, Object[] data) {
		synchronized (ds) {
			this.ds = ds;
			this.data = copyArray(data);
			
			// if no data, populate the fields with default instances
			if (data == null) {
				Class<?>[] types = ds.getTypes();
				this.data = new Object[types.length];
				for (int i = 0; i < types.length; i++) {
					try {
						this.data[i] = types[i].newInstance();
					} catch (InstantiationException ie) {
						this.data[i] = null;
					} catch (IllegalAccessException iae) {
						this.data[i] = null;
					}
				}
			}
		}
	}
	
	@Override
	public Object[] getData() {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			return copyArray(data);
		}
	}

	@Override
	public boolean setData(Object[] d) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			if (data.length != d.length) throw new IllegalArgumentException("incorrect data length: " + d.length + "; should be " + data.length);
			data = copyArray(d);
			ds.changed();
			return true;
		}
	}
	
	private Object[] copyArray(Object[] d) {
		Object[] copy = new Object[d.length];
		for (int i = 0; i < d.length; i++) {
			copy[i] = d[i];
		}
		return copy;
	}
	
	@Override
	public Object get(int field) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			if (field < 0 || field >= data.length) throw new IllegalArgumentException("field index out of bounds: " + field + "; should be >0 and <" + data.length);
			return data[field];
		}
	}
	
	@Override
	public Object get(String field) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			int idx = ds.getFieldIndex(field);
			if (idx == -1) throw new IllegalArgumentException("no such field: " + field);
			return data[idx];
		}
	}
	
	@Override
	public int getInteger(String field) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			int idx = ds.getFieldIndex(field);
			if (idx == -1) throw new IllegalArgumentException("no such field: " + field);
			return (int) data[idx];
		}
	}

	@Override
	public long getLong(String field) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			int idx = ds.getFieldIndex(field);
			if (idx == -1) throw new IllegalArgumentException("no such field: " + field);
			return (long) data[idx];
		}
	}

	@Override
	public float getFloat(String field) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			int idx = ds.getFieldIndex(field);
			if (idx == -1) throw new IllegalArgumentException("no such field: " + field);
			return (float) data[idx];
		}
	}

	@Override
	public double getDouble(String field) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			int idx = ds.getFieldIndex(field);
			if (idx == -1) throw new IllegalArgumentException("no such field: " + field);
			return (double) data[idx];
		}
	}

	@Override
	public String getString(String field) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			int idx = ds.getFieldIndex(field);
			if (idx == -1) throw new IllegalArgumentException("no such field: " + field);
			return (String) data[idx];
		}
	}

	@Override
	public char getChar(String field) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			int idx = ds.getFieldIndex(field);
			if (idx == -1) throw new IllegalArgumentException("no such field: " + field);
			return (char) data[idx];
		}
	}
	
	@Override
	public byte getByte(String field) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			int idx = ds.getFieldIndex(field);
			if (idx == -1) throw new IllegalArgumentException("no such field: " + field);
			return (byte) data[idx];
		}
	}

	@Override
	public boolean set(int field, Object value) {
		synchronized (ds) {
			if (field < 0 || field >= ds.getFields().length) throw new IllegalArgumentException("field index out of bounds: " + field + "; should be >0 and <" + ds.getFields().length);
			if (data[field] != null) {
				if (!data[field].getClass().equals(value.getClass())) throw new IllegalArgumentException("incorrect data type at field " + field + ": " + value.getClass() + "; should be " + data[field].getClass());
			}
			data[field] = value;
			ds.changed();
			return true;
		}
	}
	
	@Override
	public boolean set(String field, Object value) {
		synchronized (ds) {
			if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
			int idx = ds.getFieldIndex(field);
			if (idx == -1) throw new IllegalArgumentException("no such field: " + field);
			return set(idx, value);
		}
	}
	
	@Override
	public abstract boolean delete();

	@Override
	public abstract boolean isDeleted();
	
	@Override
	public String[] getFields() {
		return ds.getFields();
	}
	
	@Override
	public DataSource getDataSource() {
		if (isDeleted()) throw new IllegalStateException("attempting to access a deleted row");
		return ds;
	}
	
	@Override
	public String toString() {
		synchronized (ds) {
			StringBuilder sb = new StringBuilder();
			for (Object d : data) {
				sb.append(d);
				sb.append("\t");
			}
			return sb.toString();
		}
	}
}