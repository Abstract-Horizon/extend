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
package org.abstracthorizon.extend.server.deployment.tomcat;

import java.io.IOException;
import java.net.URLDecoder;

import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.support.spring.service.ServiceApplicationContextModule;
import org.springframework.context.ApplicationContext;

/**
 * <p>
 *   Module that represents a war module deployed with tomcat. Also it can define beans in
 *   a same way as service application module (this is an extension of it).
 * </p>
 * <p>
 *   It uses &quot;WEB-INF/web-application.xml&quot; for application context xml.
 * </p>
 * @author Daniel Sendula
 */
public class FullTomcatWebApplicationContext extends ServiceApplicationContextModule {

    /** Context of web server - tomcat's service application context */
    protected ApplicationContext webServerContext;

    /** Context path for this module */
    protected String contextPath;
    
    /** Host module is deployed on. Set after module is deployed. */
    private String deployedHostName;

    /** Port number module is deployed on. Set after module is deployed. */
    private int deployedPort;
    
    /** Is deployed on SSL enabled host (only SSL enabled host!). Set after module is deployed. */
    private boolean deployedOnOnlySSL = false;
    
    protected FullTomcatController tomcatController;
    
    /**
     * Empty constructor
     */
    public FullTomcatWebApplicationContext(ModuleId moduleId, FullTomcatController tomcatController) {
        super(moduleId);
        this.tomcatController = tomcatController;
    }

    public String getDeployedHostName() {
        return deployedHostName;
    }

    public void setDeployedHostName(String deployedHostName) {
        this.deployedHostName = deployedHostName;
    }

    public int getDeployedPort() {
        return deployedPort;
    }

    public void setDeployedPort(int deployedPort) {
        this.deployedPort = deployedPort;
    }
    
    public boolean getDeployedOnOnlySSL() {
        return deployedOnOnlySSL;
    }

    public void setDeployedOnOnlySSL(boolean deployedOnOnlySSL) {
        this.deployedOnOnlySSL = deployedOnOnlySSL;
    }
    

    /**
     * Obtains references to the embedded tomcat and to the host.
     * Sets this module to be depended on tomcat's service application module. 
     */
    protected void createInternal() {
        Module module = (Module)webServerContext;
        getDependsOn().add(module);
        module.getDependOnThis().add(this);

    }

    /**
     * Creates and adds context to the obtained reference of the host.
     */
    @Override
    protected void startInternal() {

        // TODO check protocol
        String contextPath = getContextPath();
        if (contextPath == null) {
            contextPath = "/" + getModuleId().getArtifactId();
            setContextPath(contextPath);
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
            setContextPath(contextPath);
        }
    
        
        try {
            org.abstracthorizon.extend.Extend.info.info("Deploying context path=" + contextPath + " from working location=" + URLDecoder.decode(getWorkingLocation().getFile(), "UTF-8"));    
            tomcatController.deploy(this);
            org.abstracthorizon.extend.Extend.info.info("Deployed context path=" + contextPath + " from working location=" + URLDecoder.decode(getWorkingLocation().getFile(), "UTF-8"));    
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes context from the host.
     */
    @Override
    protected void stopInternal() {
        try {
            tomcatController.undeploy(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes all references of the embedded tomcat and the host
     */
    @Override
    protected void destroyInternal() {
    }

    /**
     * It uses &quot;WEB-INF/web-application.xml&quot; for application context xml.
     * @return &quot;WEB-INF/web-application.xml&quot;
     */
    protected String getContextFileName() {
        return "WEB-INF/web-application.xml";
    }

    /**
     * Tomcat's service application context
     * @return tomcat's service application context
     */
    public ApplicationContext getWebServerContext() {
        return webServerContext;
    }

    /**
     * Set's tomcat's service application context
     * @param webServerContext tomcat's service application context
     */
    public void setWebServerContext(ApplicationContext webServerContext) {
        this.webServerContext = webServerContext;
    }

    /**
     * @return Returns the contextPath.
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * @param contextPath The contextPath to set.
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
