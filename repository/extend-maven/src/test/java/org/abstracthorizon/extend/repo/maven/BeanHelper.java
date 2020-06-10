package org.abstracthorizon.extend.repo.maven;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BeanHelper {

    public static String readAsString(Object bean, String path) {
        Object r = read(bean, path);
        if (r != null) {
            return r.toString();
        }
        return null;
    }

    public static Object read(Object bean, String path) {
        Object current = bean;
        
        String[] segments = path.split("\\.");
        
        for (String segment : segments) {
            String s = segment;
            int index = -1;
            int start = segment.indexOf("(");
            if (start > 0) {
                if (segment.charAt(segment.length() - 1) != ')') {
                    throw new RuntimeException("Wrong segment: '" + segment);
                }
                s = segment.substring(0, start);
                index = Integer.parseInt(segment.substring(start + 1, segment.length() - 1));
            }
            
            try {
                Class<?> cls = current.getClass();
                Method method = cls.getMethod(s);
                current = method.invoke(current);
                if (index >= 0) {
                    if (current instanceof Collection) {
                        List<?> seq = (List<?>)current;
                        current = seq.get(index);
                    } else if (current instanceof Collection) {
                        Collection<?> seq = (Collection<?>)current;
                        Iterator<?> t = seq.iterator();
                        for (int j = 0; j < index; j++) {
                            current = t.next();
                        }
                    } else {
                        throw new RuntimeException("Cannot select index of " + current);
                    }
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Failed to find method '" + s + "' on object: " + current + "; path='" + path + "'", e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Failed to find method '" + s + "' on object: " + current + "; path='" + path + "'", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to find method '" + s + "' on object: " + current + "; path='" + path + "'", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Failed to find method '" + s + "' on object: " + current + "; path='" + path + "'", e);
            }
        }
        
        return null;
    }

}
