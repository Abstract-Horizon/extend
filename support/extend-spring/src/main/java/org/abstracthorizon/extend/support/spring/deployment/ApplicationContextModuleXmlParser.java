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
package org.abstracthorizon.extend.support.spring.deployment;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This is xml parser that handles &lt;depends-on&gt; tag.
 *
 * @author Daniel Sendula
 */
public class ApplicationContextModuleXmlParser extends DefaultBeanDefinitionDocumentReader {

    public static final String DEPENDS_ON_TAG = "depends-on";
    public static final String DELEGATE_TO_TAG = "delegate-to";

    /** Context this parser is created for */
    protected AbstractApplicationContextModule context;

    /**
     * Empty constructor
     */
    public ApplicationContextModuleXmlParser() {
    }

    /**
     * This method extracts &lt;depends-on&gt; tags and calls modules
     * {@link AbstractApplicationContextModule#processDependencies(List)} method with all
     * tags (if any).
     * @param root root
     * @throws BeanDefinitionStoreException
     */
    protected void preProcessXml(Element root) throws BeanDefinitionStoreException {
        Dependency delegateTo = null;
        List<Dependency> dependencies = new ArrayList<Dependency>();
        List<Node> nodes = new ArrayList<Node>();
        Node node = root.getFirstChild();
        while (node != null) {
            if (DEPENDS_ON_TAG.equals(node.getNodeName())) {
                int type = node.getNodeType();
                if (type == Node.ELEMENT_NODE) {
                    Node c = node.getFirstChild();
                    while ((c != null) && (c.getNodeType() != Node.TEXT_NODE)) {
                        c = c.getNextSibling();
                    }
                    if (c != null) {
                        // TODO - add processing for optional, provided and isURI attributes!
                        String value = node.getFirstChild().getNodeValue();
                        Dependency dependency = new Dependency();
                        dependency.setValue(value);
                        dependencies.add(dependency);
                    }
                }
                nodes.add(node);
            } else if (DELEGATE_TO_TAG.equals(node.getNodeName())) {
                int type = node.getNodeType();
                if (type == Node.ELEMENT_NODE) {
                    Node c = node.getFirstChild();
                    while ((c != null) && (c.getNodeType() != Node.TEXT_NODE)) {
                        c = c.getNextSibling();
                    }
                    if (c != null) {
                        // TODO - add processing for optional, provided and isURI attributes!
                        String value = node.getFirstChild().getNodeValue();
                        Dependency dependency = new Dependency();
                        dependency.setValue(value);
                        if (delegateTo != null) {
                            throw new RuntimeException("Only one \"delegate-to\" is allowed");
                        }
                        delegateTo = dependency;
                    }
                }
                nodes.add(node);
            }
            node = node.getNextSibling();
        }
        for (Node n : nodes) {
            root.removeChild(n);
        }
        BeanDefinitionReader reader = getReaderContext().getReader();

        AbstractApplicationContextModule context = (AbstractApplicationContextModule)reader.getResourceLoader();
        if (!context.processDependencies(delegateTo, dependencies)) {
            // If we do not have deployed parent - we cannot proceed with any beans.
            // We will remove them from definition and then wait to be 'created' again
            ArrayList<Node> ns = new ArrayList<Node>();
            Node nn = root.getFirstChild();
            while (nn != null) {
                ns.add(nn);
                nn = nn.getNextSibling();
            }
            for (Node n : ns) {
                root.removeChild(n);
            }
        }
    }

    /**
     * Extracts value from node
     * @param node text node
     * @return string value
     */
    protected String getValue(Node node) {
        Node c = node.getFirstChild();
        while ((c != null) && (c.getNodeType() != Node.TEXT_NODE)) {
            c = c.getNextSibling();
        }
        if (c != null) {
            String value = node.getFirstChild().getNodeValue();
            if (value.length() > 0) {
                return value;
            }
        }
        return null;
    }

}
