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
package org.abstracthorizon.extend.support.spring.service;

import org.abstracthorizon.extend.server.deployment.service.ServiceBean;
import org.abstracthorizon.extend.support.spring.deployment.ApplicationContextModuleXmlParser;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This is xml parser that handles &lt;service&gt; tag.
 *
 * @author Daniel Sendula
 */
public class ServiceApplicationContextModuleXmlParser extends ApplicationContextModuleXmlParser {

    public static final String BEAN_TAG = "bean";

    public static final String SERVICE_TAG = "service";

    public static final String NAME_ATTRIBUTE = "name";

    public static final String ID_ATTRIBUTE = "id";

    public static final String CREATE_METHOD_TAG = "create-method";

    public static final String START_METHOD_TAG = "start-method";

    public static final String STOP_METHOD_TAG = "stop-method";

    public static final String DESTROY_METHOD_TAG = "destroy-method";

    /**
     * Empty constructor
     */
    public ServiceApplicationContextModuleXmlParser() {
    }

    /**
     * This method processes all tags in search for &lt;service&gt; tag.
     * @param root root element of DOM parsed XML.
     * @throws BeanDefinitionStoreException
     */
    protected void preProcessXml(Element root) throws BeanDefinitionStoreException {
        super.preProcessXml(root);

        BeanDefinitionReader reader = getReaderContext().getReader();

        ServiceApplicationContextModule context = (ServiceApplicationContextModule)reader.getResourceLoader();

        Node node = root.getFirstChild();
        while (node != null) {
            inspectNode(context, node);
            node = node.getNextSibling();
        }
    }

    /**
     * This method checks given node if &lt;service&gt; tag is present.
     * @param context service module
     * @param node node to be checked
     */
    protected void inspectNode(ServiceApplicationContextModule context, Node node) {
        if (BEAN_TAG.equals(node.getNodeName())) {
            Node child = node.getFirstChild();
            while ((child != null) && (child.getNodeType() != Node.ELEMENT_NODE)) {
                child = child.getNextSibling();
            }
            if ((child != null) && SERVICE_TAG.equals(child.getNodeName())) {
                String name = null;
                Node nameNode = node.getAttributes().getNamedItem(NAME_ATTRIBUTE);
                if (nameNode != null) {
                    name = nameNode.getNodeValue();
                }
                if (name == null) {
                    nameNode = node.getAttributes().getNamedItem(ID_ATTRIBUTE);
                    if (nameNode != null) {
                        name = nameNode.getNodeValue();
                    }
                }
                if (name != null) {
                    processNode(context, child, name);
                }
                node.removeChild(child);
            }
        }
    }

    /**
     * Processes &lt;service&gt; tag node.
     * @param context service module
     * @param node service tag node
     * @param name name of the bean containing service tag
     */
    protected void processNode(ServiceApplicationContextModule context, Node node, String name) {
        ServiceBean serviceBean = new ServiceBean(name);
        Node child = node.getFirstChild();
        while (child != null) {

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (CREATE_METHOD_TAG.equals(child.getNodeName())) {
                    String methodName = getValue(child);
                    serviceBean.setCreateMethodName(methodName);
                } else if (START_METHOD_TAG.equals(child.getNodeName())) {
                    String methodName = getValue(child);
                    serviceBean.setStartMethodName(methodName);
                } else if (STOP_METHOD_TAG.equals(child.getNodeName())) {
                    String methodName = getValue(child);
                    serviceBean.setStopMethodName(methodName);
                } else if (DESTROY_METHOD_TAG.equals(child.getNodeName())) {
                    String methodName = getValue(child);
                    serviceBean.setDestroyMethodName(methodName);
                }
            }

            child = child.getNextSibling();
        }
        context.getServiceBeans().add(serviceBean);
    }

}
