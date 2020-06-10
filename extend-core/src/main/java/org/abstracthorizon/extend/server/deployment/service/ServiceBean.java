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
/**
 *
 */
package org.abstracthorizon.extend.server.deployment.service;

/**
 * Definition of service bean.
 *
 * @author Daniel Sendula
 */
public class ServiceBean {

    /** Bean's name */
    protected String beanName;

    /** Reference to the bean */
    protected Object bean;

    /** Create method name */
    protected String createMethodName = "create";

    /** Start method name */
    protected String startMethodName = "start";

    /** Stop mehtod name */
    protected String stopMethodName = "stop";

    /** Destroy method name */
    protected String destroyMethodName = "destroy";

    /**
     * Constructor
     * @param beanName name of the bean
     */
    public ServiceBean(String beanName) {
        this.beanName = beanName;
    }

    /**
     * @return Returns the createMethodName.
     */
    public String getCreateMethodName() {
        return createMethodName;
    }

    /**
     * @param createMethodName The createMethodName to set.
     */
    public void setCreateMethodName(String createMethodName) {
        this.createMethodName = createMethodName;
    }

    /**
     * @return Returns the startMethodName.
     */
    public String getStartMethodName() {
        return startMethodName;
    }

    /**
     * @param startMethodName The startMethodName to set.
     */
    public void setStartMethodName(String startMethodName) {
        this.startMethodName = startMethodName;
    }

    /**
     * @return Returns the bean.
     */
    public Object getBean() {
        return bean;
    }

    /**
     * @param bean The bean.
     */
    public void setBean(Object bean) {
        this.bean = bean;
    }

    /**
     * @return Returns the beanName.
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * @return Returns the destroyMethodName.
     */
    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    /**
     * @param destroyMethodName The destroyMethodName to set.
     */
    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }

    /**
     * @return Returns the stopMethodName.
     */
    public String getStopMethodName() {
        return stopMethodName;
    }

    /**
     * @param stopMethodName The stopMethodName to set.
     */
    public void setStopMethodName(String stopMethodName) {
        this.stopMethodName = stopMethodName;
    }

    public int hashCode() {
        return beanName.hashCode();
    }

    public boolean equals(Object object) {
        if (object instanceof ServiceBean) {
            String bn = ((ServiceBean)object).getBeanName();
            return beanName.equals(bn);
        }
        return false;
    }


}
