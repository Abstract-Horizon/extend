/*
 * Copyright (c) 2005 Creative Sphere Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the LGPL licence
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Contributors:
 *
 *   Creative Sphere - initial API and implementation
 *
 */
package org.abstracthorizon.spring.server.deployment.jetty;

import org.abstracthorizon.extend.support.spring.deployment.ApplicationContextModuleXmlParser;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This is xml parser that handles &lt;context-path&gt; tag.
 *
 * @author Daniel Sendula
 */
public class JettyWarModuleXmlParser extends ApplicationContextModuleXmlParser {

    /**
     * Empty constructor
     */
    public JettyWarModuleXmlParser() {
    }

    /**
     * This method processes all tags in search for &lt;context-path&gt; tag.
     * @param root root element of DOM parsed XML.
     * @throws BeanDefinitionStoreException
     */
    protected void preProcessXml(Element root) throws BeanDefinitionStoreException {
        super.preProcessXml(root);

        BeanDefinitionReader reader = getReaderContext().getReader();

        JettyWebApplicationContext context = (JettyWebApplicationContext)reader.getResourceLoader();

        Node node = root.getFirstChild();
        while (node != null) {
            if ("context-path".equals(node.getNodeName())) {
                context.setContextPath(getValue(node));
                return;
            }
            node = node.getNextSibling();
        }
    }

}
