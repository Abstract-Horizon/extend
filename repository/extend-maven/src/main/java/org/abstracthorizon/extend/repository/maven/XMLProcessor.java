/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
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
package org.abstracthorizon.extend.repository.maven;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.abstracthorizon.extend.repository.maven.pom.POM;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class processes pom and obtains all dependencies for it.
 * 
 * @author Daniel Sendula
 */
public class XMLProcessor extends DefaultHandler {

    protected static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    
    protected static final Class<?>[] STRING_CLASS_ARRAY = new Class[]{String.class};
    
    protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    
    protected static final Object MAP_MARKER = new Object();
    
    protected static final Object PLACEHOLDER = new Object();
    
    protected static final Object FIRST_OBJECT = new Object();
    
    protected File xmlFile;
    
    protected InputStream inputStream;
    
    protected StringBuffer buffer = new StringBuffer();

    protected Stack<Object> stack = new Stack<Object>();
    
    protected Object startObject;
    
    protected long startTime;

    protected static SAXParserFactory myFactory;

    
    public XMLProcessor(File xmlFile) {
        this.xmlFile = xmlFile;
        // TODO
        if (myFactory == null) {
            myFactory = SAXParserFactory.newInstance();
        }
    }
    
    public XMLProcessor(InputStream inputStream) {
        this.inputStream = inputStream;
        // TODO
        if (myFactory == null) {
            myFactory = SAXParserFactory.newInstance();
        }
    }
    
    public Object getStartObject() {
        return startObject;
    }
    
    public void setStartObject(Object startObject) {
        this.startObject = startObject;
    }
    
    public void process() throws Exception {
        stack.clear();

        // Turn on XML Schema validation
        myFactory.setFeature("http://apache.org/xml/features/validation/schema", true);

        // Now get an instance of the parser with schema validation enabled
        SAXParser parser = myFactory.newSAXParser();
        
        if (xmlFile != null) {
            FileReader reader = new FileReader(xmlFile);
            try {
                InputSource inputSource = new InputSource(reader);
    
                parser.parse(inputSource, this);
            } finally {
                reader.close();
            }
            //            xmlFile = null;
        } else if (inputStream != null) {
            try {
                InputSource inputSource = new InputSource(inputStream);
    
                parser.parse(inputSource, this);
            } finally {
                inputStream.close();
            }
            inputStream = null;
        } else {
            throw new IOException("Already parsed!");
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(ch, start, length);
    }

    public void startDocument() throws SAXException {
        startTime = System.currentTimeMillis();
    }

    public void endDocument() throws SAXException {
//        if (logger.isDebugEnabled()) {
//            logger.debug("    * Parsing lasted " + Long.toString(System.currentTimeMillis() - startTime) + "ms");
//        }
    }

    public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
        Object newObject = null;
        Object object = null;
        if (stack.size() == 0) {
            object = getStartObject();
            newObject = object;
        } else {
            object = stack.peek();
        }
        
        Class<?> cls = object.getClass();
        if (object instanceof Map) {
            newObject = MAP_MARKER;
        }
        String capitalisedName1 = null;
        if (newObject == null) {
            try {
                capitalisedName1 = capitalise(name, true);
                Method m = cls.getMethod("add" + capitalisedName1, EMPTY_CLASS_ARRAY);
                newObject = m.invoke(object, EMPTY_OBJECT_ARRAY);
            } catch (SecurityException ignore) {
            } catch (NoSuchMethodException ignore) {
            } catch (IllegalArgumentException ignore) {
            } catch (IllegalAccessException ignore) {
            } catch (InvocationTargetException ignore) {
            }
        }
        String capitalisedName2 = null;
        if (newObject == null) {
            try {
                capitalisedName2 = capitalise(name, false);
                Method m = cls.getMethod("add" + capitalisedName2, EMPTY_CLASS_ARRAY);
                newObject = m.invoke(object, EMPTY_OBJECT_ARRAY);
            } catch (SecurityException ignore) {
            } catch (NoSuchMethodException ignore) {
            } catch (IllegalArgumentException ignore) {
            } catch (IllegalAccessException ignore) {
            } catch (InvocationTargetException ignore) {
            }
        }
        if (newObject == null) {
            try {
                Method m = cls.getMethod("set" + capitalisedName1, STRING_CLASS_ARRAY);
                if (m != null) {
                    newObject = m;
                }
            } catch (SecurityException ignore) {
            } catch (NoSuchMethodException ignore) {
            } catch (IllegalArgumentException ignore) {
            }
        }
        if (newObject == null) {
            try {
                Method m = cls.getMethod("set" + capitalisedName2, STRING_CLASS_ARRAY);
                if (m != null) {
                    newObject = m;
                }
            } catch (SecurityException ignore) {
            } catch (NoSuchMethodException ignore) {
            } catch (IllegalArgumentException ignore) {
            }
        }
        if (newObject == null) {
            newObject = PLACEHOLDER;
        }
        stack.push(newObject);
    }

    @SuppressWarnings("unchecked")
    public void endElement(String uri, String localName, String name) throws SAXException {
        String value = buffer.toString().trim();
        Object thisObject = stack.pop();
        if (thisObject == MAP_MARKER) {
            Map<String, String> map = (Map<String, String>)stack.peek();
            map.put(name, value);
        } else if (thisObject instanceof Method) {
            Object obj = stack.peek();
            Method m = (Method)thisObject;
            try {
                m.invoke(obj, value);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        buffer.delete(0, buffer.length());
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
        InputSource is = super.resolveEntity(publicId, systemId);
        return is;
    }
    
    public static String capitalise(String string, boolean firstOnly) {
        if (string.length() == 0) { 
            return string;
        }
        if (string.length() == 1) {
            return string.toUpperCase();
        }
        if (firstOnly) {
            return Character.toUpperCase(string.charAt(0)) + string.substring(1);
        }
        StringBuffer res = new StringBuffer(string.length());
        int i = 0;
        while ((i < string.length()) && Character.isLowerCase(i)) {
            res.append(Character.toUpperCase(string.charAt(i)));
            i++;
        }
        if (i < string.length()) {
            res.append(string.substring(i));
        }
        return res.toString();
    }

    public static void main(String[] args) throws Exception {

        POM pom = new POM();
        
        File file = new File(System.getProperty("user.home") + "/.m2/repository/org/abstracthorizon/extend/extend/0.4.0/extend-0.4.0.pom");
        XMLProcessor processor = new XMLProcessor(file);
        processor.setStartObject(pom);
        processor.process();

        System.out.println(pom);
        
        if (pom.getProperties() != null) {
            Map<String, String> map = pom.getProperties();
            if (pom.getVersion() != null) {
                map.put("pom.version", pom.getVersion());
            }
            if (pom.getGroupId() != null) {
                map.put("pom.groupId", pom.getGroupId());
            }
            if (pom.getArtifactId() != null) {
                map.put("pom.artifactId", pom.getArtifactId());
            }
            SubstitutionTraverser.substitute(pom, map);
            System.out.println("-----------------------------------------------------------------------");
            System.out.println(pom);
        }
        
        
    }
    
}
