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
package org.abstracthorizon.extend.server.deployment.danube;

import org.abstracthorizon.extend.support.spring.service.ServiceApplicationContextModuleXmlParser;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This is xml parser that handles &lt;context-path&gt; tag.
 *
 * @author Daniel Sendula
 */
public class DanubeWarModuleXmlParser extends ServiceApplicationContextModuleXmlParser {

    /**
     * Empty constructor
     */
    public DanubeWarModuleXmlParser() {
    }

    /**
     * This method processes all tags in search for &lt;context-path&gt; tag.
     * @param root root element of DOM parsed XML.
     * @throws BeanDefinitionStoreException
     */
    protected void preProcessXml(Element root) throws BeanDefinitionStoreException {
        super.preProcessXml(root);

        BeanDefinitionReader reader = getReaderContext().getReader();

        DanubeWebApplicationContext context = (DanubeWebApplicationContext)reader.getResourceLoader();

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
