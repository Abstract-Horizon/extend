package org.abstracthorizon.extend.repository.maven;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SubstitutionTraverser {

    protected static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    
    protected static final Class<?>[] STRING_CLASS_ARRAY = new Class[]{String.class};
    
    protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    
    public static void substitute(Object object, Map<String, String> substitutions) {

        Class<?> cls = object.getClass();
        Method[] methods = cls.getMethods();
        for (Method m : methods) {
            if (!Modifier.isStatic(m.getModifiers())) {
                String name = m.getName();
                if (!name.equals("getClass") && name.startsWith("get") && (name.length() > 3)) {
                    Class<?> returnType = m.getReturnType();
                    if (returnType.equals(String.class)) {
                        try {
                            String value = (String)m.invoke(object, EMPTY_OBJECT_ARRAY);
                            if ((value != null) && (value.indexOf("${") >= 0)) {
                                String newValue = substitute(value, substitutions);
                                Method writeBackMethod = cls.getMethod("s" + name.substring(1), STRING_CLASS_ARRAY);
                                writeBackMethod.invoke(object, new Object[]{newValue});
                            }
                        } catch (IllegalArgumentException ignore) {
                        } catch (IllegalAccessException ignore) {
                        } catch (InvocationTargetException ignore) {
                        } catch (SecurityException ignore) {
                        } catch (NoSuchMethodException ignore) {
                        }
                    } else if (!returnType.isPrimitive()) {
                        try {
                            Object obj = m.invoke(object, EMPTY_OBJECT_ARRAY);
                            if (obj != null) {
                                if (obj instanceof Collection) {
                                    for (Object o : (Collection<?>)obj) {
                                        substitute(o, substitutions);
                                    }
                                } else if (obj.getClass().isArray()) {
                                    for (Object o : (Object[])obj) {
                                        substitute(o, substitutions);
                                    }
                                } else {
                                    substitute(obj, substitutions);
                                }
                            }
                        } catch (IllegalArgumentException ignore) {
                        } catch (IllegalAccessException ignore) {
                        } catch (InvocationTargetException ignore) {
                        }
                    }
                }
            }
        }
    }
    
    public static String substitute(String value, Map<String, String> substitutions) {
        int len = value.length();
        StringBuffer buf = new StringBuffer(len);
        int i = 0;
        int ptr = 0;
        int state = 0;
        while (i < len) {
            char c = value.charAt(i);
            if (state == 0) {
                if (c == '$') {
                    state = 1;
                }
            } else if (state == 1) {
                if (c == '{') {
                    if (i > 1) {
                        buf.append(value.substring(ptr, i - 1));
                    }
                    ptr = i + 1;
                    state = 2;
                } else {
                    state = 0;
                }
            } else if (state == 2) {
                if (c == '}') {
                    String key = value.substring(ptr, i);
                    String sub = substitutions.get(key);
                    if (sub != null) {
                        buf.append(sub);
                    } else {
                        buf.append("${").append(key).append('}');
                    }
                    ptr = i + 1;
                    state = 0;
                }
            }
            i++;
        }
        if (ptr != i) {
            if (state == 2) {
                buf.append("${");
            }
            buf.append(value.substring(ptr));
        }
        return buf.toString();
    }
    
    public static void main(String[] args) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("pom.version", "0.4.0");
        test("${pom.version}x", map);
        test("y${pom.version}x", map);
        test("y${pom.version}", map);
        test("y${pom.version", map);
        test("pom.version}", map);
        test("pom.version", map);
        test("y${pom.version}asd${some}", map);

        test("${pom.versionx}x", map);
        test("y${pom.versionx}x", map);
        test("y${pom.versionx}", map);
//        substitute(new SubstitutionTraverser(), null);
    }

    public static void test(String value, Map<String, String> map) {
        System.out.println(value + " = '" + substitute(value, map) + "'");
    }
}
