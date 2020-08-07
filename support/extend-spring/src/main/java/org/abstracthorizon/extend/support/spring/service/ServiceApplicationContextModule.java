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

import java.util.HashSet;
import java.util.Set;

import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.service.ServiceBean;
import org.abstracthorizon.extend.server.support.ClassUtils;
import org.abstracthorizon.extend.support.spring.deployment.AbstractApplicationContextModule;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * Module that represents a service. It tracks service beans (beans marked with &lt;service&gt;
 * tag). When module is created/started/stopped/destroyed then create/start/stop/destroy method is called on all service beans.
 * Note: actual method name can be specified in &lt;service&gt; tag.
 *
 * @author Daniel Sendula
 */
public class ServiceApplicationContextModule extends AbstractApplicationContextModule {

    /** List of service beans */
    protected Set<ServiceBean> serviceBeans = new HashSet<ServiceBean>();

    /**
     * Empty constructor
     */
    public ServiceApplicationContextModule(ModuleId moduleId) {
        super(moduleId);
    }

    /**
     * Sets up parser for this application context.
     * This implementation uses {@link ServiceApplicationContextModuleXmlParser} without validation
     * with {@link #internalClassLoader}.
     */
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader xmlbeandefinitionreader) {
        xmlbeandefinitionreader.setDocumentReaderClass(ServiceApplicationContextModuleXmlParser.class);
        xmlbeandefinitionreader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        xmlbeandefinitionreader.setBeanClassLoader(internalClassLoader);
    }

    /**
     * Invokes create method on all service beans
     */
    protected void createInternal() {
        for (ServiceBean serviceBean : serviceBeans) {
            Object bean = getBean(serviceBean.getBeanName());
            if (bean != null) {
                serviceBean.setBean(bean);
                if (serviceBean.getCreateMethodName() != null) {
                    ClassUtils.invokeMethod(bean, serviceBean.getCreateMethodName(), false);
                }
            }
        }
    }

    /**
     * Invokes start method on all service beans
     */
    protected void startInternal() {
        for (ServiceBean serviceBean : serviceBeans) {
            Object bean = serviceBean.getBean();
            if (bean == null) {
                bean = getBean(serviceBean.getBeanName());
                serviceBean.setBean(bean);
            }
            if ((bean != null)  && (serviceBean.getStopMethodName() != null)) {
                ClassUtils.invokeMethod(bean, serviceBean.getStartMethodName(), false);
            }
        }
    }

    /**
     * Invokes stop method on all service beans
     */
    protected void stopInternal() {
        for (ServiceBean serviceBean : serviceBeans) {
            Object bean = serviceBean.getBean();
            if (bean == null) {
                bean = getBean(serviceBean.getBeanName());
                serviceBean.setBean(bean);
            }
            if ((bean != null) && (serviceBean.getStopMethodName() != null)) {
                ClassUtils.invokeMethod(bean, serviceBean.getStopMethodName(), false);
            }
        }
    }

    /**
     * Invokes destroy method on all service beans
     */
    protected void destroyInternal() {
        for (ServiceBean serviceBean : serviceBeans) {
            Object bean = serviceBean.getBean();
            if (bean == null) {
                bean = getBean(serviceBean.getBeanName());
                serviceBean.setBean(bean);
            }
            if ((bean != null) && (serviceBean.getDestroyMethodName() != null)) {
                ClassUtils.invokeMethod(bean, serviceBean.getDestroyMethodName(), false);
            }
        }
    }

    /**
     * @return Returns the serviceBeans.
     */
    public Set<ServiceBean> getServiceBeans() {
        return serviceBeans;
    }

    /**
     * @param serviceBeans The serviceBeans to set.
     */
    public void setServiceBeans(Set<ServiceBean> serviceBeans) {
        this.serviceBeans = serviceBeans;
    }


}
