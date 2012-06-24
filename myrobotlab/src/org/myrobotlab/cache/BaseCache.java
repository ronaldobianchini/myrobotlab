/**
 * 
 */
package org.myrobotlab.cache;

import org.myrobotlab.reflection.Instantiator;

/**
 * @author SwedaKonsult
 *
 */
public abstract class BaseCache implements Cache {
	
	private static final boolean DEFAULT_BOOL = false;
	private static final byte DEFAULT_BYTE = 0;
	private static final int DEFAULT_INT = 0;
	private static final short DEFAULT_SHORT = 0;

	/**
	 * Internal method for BaseCache to actually add items to the implementing cache.
	 * 
	 * @param name
	 * @param value
	 */
	protected abstract void addToCache(String name, Object value);
	/**
	 * Internal method for BaseCache to actually check if the name exists in the implementing cache.
	 * @param name
	 * @return
	 */
	protected abstract boolean contains(String name);
	/**
	 * Internal method for BaseCache to actually retrieve items from the implementing cache.
	 * @param name
	 */
	protected abstract Object getFromCache(String name);
	/**
	 * Internal method for BaseCache to actually remove items from the implementing cache.
	 * @param name
	 */
	protected abstract void removeFromCache(String name);

	/**
	 * Expire an item in the cache.
	 * 
	 * @param name
	 */
	public void expire(String name) {
		if (name == null || name.isEmpty() || !contains(name)) {
			return;
		}
		removeFromCache(name);
	}
	
	/**
	 * Get a value from the cache.
	 * 
	 * @param name the name of the value to retrieve
	 * @return null if the name does not exist or if the type could not be cast to T
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String name, Class<? extends T> cls) {
		if (name == null || !contains(name)) {
			if (!Instantiator.isPrimitive(cls)) {
				return null;
			}
			return (T) Instantiator.getPrimitive(cls);
		}
		Object value = getFromCache(name);

		if (value != null && cls.isInstance(value)) {
			return (T) value;
		}
		if (!Instantiator.isPrimitive(cls)) {
			return null;
		}
		// else return whichever primitive they're asking for
		if (cls.isAssignableFrom(Integer.class)) {
			return (T) new Integer(0);
		}
		if (cls.isAssignableFrom(Byte.class)) {
			return (T) new Byte(Byte.MIN_VALUE);
		}
		if (cls.isAssignableFrom(Short.class)) {
			return (T) new Short(Short.MIN_VALUE);
		}
		if (cls.isAssignableFrom(Double.class)) {
			return (T) new Double(0d);
		}
		if (cls.isAssignableFrom(Float.class)) {
			return (T) new Float(0f);
		}
		if (cls.isAssignableFrom(Long.class)) {
			return (T) new Long(0l);
		}
		if (cls.isAssignableFrom(Boolean.class)) {
			return (T) Boolean.FALSE;
		}
		return (T) new Character('\u0000');
	}
	
	/**
	 * Get a boolean primitive value from the cache.
	 * Tests for: Boolean, Integer, Byte, Short, String (parseBoolean)
	 * 
	 * @param name
	 * @return false if nothing is found or the cached value is not a boolean value
	 */
	public boolean getBool(String name) {
		if (name == null || !contains(name)) {
			return DEFAULT_BOOL;
		}
		Object value = getFromCache(name);
		if (value ==  null) {
			return DEFAULT_BOOL;
		}
		if (value instanceof Boolean) {
			boolean b = (Boolean) value;
			return b;
		}
		if (value instanceof Integer) {
			int i = (Integer) value;
			return i == 0;
		}
		if (value instanceof Byte) {
			byte b = (Byte) value;
			return b == 0;
		}
		if (value instanceof Short) {
			short s = (Short) value;
			return s == 0;
		}
		if (!(value instanceof String)) {
			return DEFAULT_BOOL;
		}
		return parseWithDefault((String) value, DEFAULT_BOOL);
	}
	
	/**
	 * Get an byte primitive value from the cache.
	 * Tests for: Byte, Short, String (parseByte)
	 * 
	 * @param name
	 * @return 0 if nothing is found or the cached value was not an byte value
	 */
	public byte getByte(String name) {
		if (name == null || !contains(name)) {
			return DEFAULT_BYTE;
		}
		Object value = getFromCache(name);
		if (value ==  null) {
			return DEFAULT_BYTE;
		}
		if (value instanceof Byte) {
			byte b = (Byte) value;
			return b;
		}
		if (value instanceof Short) {
			short s = (Short) value;
			return (byte) s;
		}
		if (!(value instanceof String)) {
			return DEFAULT_BYTE;
		}
		return parseWithDefault((String) value, DEFAULT_BYTE);
	}
	
	/**
	 * Get an int primitive value from the cache.
	 * Tests for: Integer, Byte, Short, String (parseInt)
	 * 
	 * @param name
	 * @return 0 if nothing is found or the cached value was not an integer value
	 */
	public int getInt(String name) {
		if (name == null || !contains(name)) {
			return DEFAULT_INT;
		}
		Object value = getFromCache(name);
		if (value ==  null) {
			return DEFAULT_INT;
		}
		if (value instanceof Integer) {
			int i = (Integer) value;
			return (int) i;
		}
		if (value instanceof Byte) {
			byte b = (Byte) value;
			return (int) b;
		}
		if (value instanceof Short) {
			short s = (Short) value;
			return (int) s;
		}
		if (!(value instanceof String)) {
			return DEFAULT_INT;
		}
		return parseWithDefault((String) value, DEFAULT_INT);
	}
	
	/**
	 * Get an short primitive value from the cache.
	 * Tests for: Short, Byte, String (parseShort)
	 * 
	 * @param name
	 * @return 0 if nothing is found or the cached value was not an integer value
	 */
	public short getShort(String name) {
		if (name == null || !contains(name)) {
			return DEFAULT_SHORT;
		}
		Object value = getFromCache(name);
		if (value ==  null) {
			return DEFAULT_SHORT;
		}
		if (value instanceof Short) {
			short s = (Short) value;
			return s;
		}
		if (value instanceof Byte) {
			byte b = (Byte) value;
			return (short) b;
		}
		if (!(value instanceof String)) {
			return DEFAULT_SHORT;
		}
		return parseWithDefault((String) value, DEFAULT_SHORT);
	}
	
	/**
	 * Add a value to the cache.
	 * 
	 * @param name cannot be null or empty
	 * @param value
	 */
	public void put(String name, Object value) {
		if (name == null || name.isEmpty()) {
			return;
		}
		addToCache(name, value);
	}
	
	/**
	 * Try to parse a boolean.
	 * 
	 * @param value
	 * @param defaultBool return value if the string cannot be parsed into a boolean
	 * @return
	 */
	private boolean parseWithDefault(String value, boolean defaultBool) {
		return Boolean.parseBoolean(value);
	}
	
	/**
	 * Try to parse a byte.
	 * 
	 * @param value
	 * @param defaultByte return value if the string cannot be parsed into an byte
	 * @return
	 */
	private byte parseWithDefault(String value, byte defaultByte) {
		try {
			return Byte.parseByte(value);
		} catch (NumberFormatException e) {}
		return defaultByte;
	}
	
	/**
	 * Try to parse an integer.
	 * 
	 * @param value
	 * @param defaultInt return value if the string cannot be parsed into an integer
	 * @return
	 */
	private int parseWithDefault(String value, int defaultInt) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {}
		return defaultInt;
	}
	
	/**
	 * Try to parse a short.
	 * 
	 * @param value
	 * @param defaultShort return value if the string cannot be parsed into an short
	 * @return
	 */
	private short parseWithDefault(String value, short defaultShort) {
		try {
			return Short.parseShort(value);
		} catch (NumberFormatException e) {}
		return defaultShort;
	}
}