/*
 * Copyright (c) 2005-2007 Creative Sphere Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Creative Sphere - initial API and implementation
 *
 */
package org.abstracthorizon.extend.server.support;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * This map maintains separate set of values. This allows if two key/value pairs contain same
 * value and {@link #remove(Object)} method is called with value both values are going to be
 * removed from the map. Also, having set of values {@link #values()} method returns that
 * set which is faster. Values set is implemented as {@link LinkedHashSet}
 * 
 */
public class EnhancedMap<KeyType, ValueType> implements Map<KeyType, ValueType> {

    /** Map */
    protected LinkedHashMap<KeyType, ValueType> map = new LinkedHashMap<KeyType, ValueType>();
    
    /** Values */
    protected InternalSet<ValueType> values = new InternalSet<ValueType>();
    
    /** Empty constractor */
    public EnhancedMap() {
    }

    /**
     * Returns get value
     * @param key key
     * @return value
     */
    public ValueType get(Object key) {
        return map.get(key);
    }
    
    /**
     * Adds new element to the map
     * @param key key
     * @param value value
     * @return previous value under that key
     */
    public ValueType put(KeyType key, ValueType value) {
        values.add(value);
        ValueType res = map.put(key, value);
        return res;
    }

    /**
     * Removes value value
     * @param value value to be removed
     * @return <code>true</code> if removed
     */
    @SuppressWarnings("unchecked")
    public ValueType remove(Object value) {
        if (values.remove(value)) {
            while (map.values().remove(value)) {
            }
            return (ValueType)value;
        } else {
            ValueType result = map.remove(value);
            if (result != null) {
                if (!map.values().contains(result)) {
                    values.remove(result);
                }
            }
            return result;
        }
    }
    

    /**
     * Clears the map.
     */
    public void clear() {
        map.clear();
        values.clear();
    }

    /**
     * Returns values
     * @return values
     */
    public Collection<ValueType> values() {
        return values;
    }

    /**
     * Checks map if key exsits
     * @param key key
     * @return <code>true</code> if key exists
     */
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     * Checks values set if value exsits
     * @param value value
     * @return <code>true</code> if value exists
     */
    public boolean containsValue(Object value) {
        return values.contains(value);
    }

    /**
     * Returns entry set of a map
     * @return entry set of a map
     */
    public Set<Map.Entry<KeyType, ValueType>> entrySet() {
        return map.entrySet();
    }

    /**
     * Returns if is empty
     * @return <code>true</code> if is empty
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns key set
     * @return key set
     */
    public Set<KeyType> keySet() {
        return map.keySet();
    }

    /**
     * Stores all
     * @param t map
     */
    public void putAll(Map<? extends KeyType, ? extends ValueType> t) {
        values.addAll(t.values());
        map.putAll(t);
    }

    /**
     * Returns map size
     * @return map size
     */
    public int size() {
        return map.size();
    }
    
    /**
     * Internal implementation of {@link LinkedHashSet} so invoking {@link LinkedHashSet#remove(Object)}
     * method removes all values from the map as well
     * 
     * @param <Type>
     * @author Daniel Sendula
     */
    protected class InternalSet<Type> extends LinkedHashSet<Type> {
        public boolean remove(Object object) {
            if (removeInternal(object)) {
                while (map.values().remove(object)) {
                }                
                return true;
            } else {
                return false;
            }
        }
       
        /**
         * Internal remove method - invokes super remove method.
         * @param object object to be removed
         * @return <code>true</code> if removed
         */
        protected boolean removeInternal(Object object) {
            boolean removed = super.remove(object);
            return removed;
        }
    }
    
    public static void main(String[] args) throws Exception {
        EnhancedMap<String, Object>  map = new EnhancedMap<String, Object>();
        
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        
        Object o1 = new Object() { public String toString() { return "o1"; } };
        Object o2 = new Object() { public String toString() { return "o2"; } };
        
        map.put(key1, o1);
        map.put(key2, o2);
        map.put(key3, o1);
        
        System.out.println("-----");
        
        for (Object value : map.values()) {
            System.out.println(value);
        }

        map.remove(key2);
        
        System.out.println("-----");
        
        for (Object value : map.values()) {
            System.out.println(value);
        }
        
        
    }
}