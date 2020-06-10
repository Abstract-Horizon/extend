package org.abstracthorizon.extend.repo.util;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLUtils {

    public static Document load(InputStream inputStream) throws IOException {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xml = dBuilder.parse(inputStream);
            return xml;
        } catch (IOException e) {
            throw e;
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }
    
    public static String getFirstValue(Document xml, String tag) {
        NodeList list = xml.getElementsByTagName(tag);
        String value = null;
        
        if (list != null && list.getLength() > 0) {
            value = list.item(0).getChildNodes().item(0).getNodeValue();
        }
        return value;
    }

    public static String getLastValue(Document xml, String tag) {
        NodeList list = xml.getElementsByTagName(tag);
        String value = null;
        
        if (list != null && list.getLength() > 0) {
            value = list.item(list.getLength() - 1).getChildNodes().item(0).getNodeValue();
        }
        return value;
    }
    
    public static String getFirstValue(Element xml, String tag) {
        NodeList list = xml.getElementsByTagName(tag);
        String value = null;
        
        if (list != null && list.getLength() > 0) {
            value = list.item(0).getChildNodes().item(0).getNodeValue();
        }
        return value;
    }

    public static String getLastValue(Element xml, String tag) {
        NodeList list = xml.getElementsByTagName(tag);
        String value = null;
        
        if (list != null && list.getLength() > 0) {
            value = list.item(list.getLength() - 1).getChildNodes().item(0).getNodeValue();
        }
        return value;
    }

}
