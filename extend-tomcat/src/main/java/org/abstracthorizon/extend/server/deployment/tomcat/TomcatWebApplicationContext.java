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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.support.spring.service.ServiceApplicationContextModule;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.startup.Embedded;
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
public class TomcatWebApplicationContext extends ServiceApplicationContextModule {

    /** Reference to embedded tomcat */
    protected Embedded tomcat;
    
    /** Reference to the Host this context is deploy under */
    protected Host host;

    /** Context this module is running under */
    protected Context context;

    /** Context of web server - tomcat's service application context */
    protected ApplicationContext webServerContext;

    /** Context path for this module */
    protected String contextPath;
    
    /**
     * Empty constructor
     */
    public TomcatWebApplicationContext(ModuleId moduleId) {
        super(moduleId);
    }

    /**
     * Obtains references to the embedded tomcat and to the host.
     * Sets this module to be depended on tomcat's service application module. 
     */
    protected void createInternal() {
        Module module = (Module)webServerContext;
        getDependsOn().add(module);
        module.getDependOnThis().add(this);

        tomcat = (Embedded)webServerContext.getBean("tomcat");

        StandardEngine engine = (StandardEngine)webServerContext.getBean("tomcat.engine");
        ClassLoader classLoader = getClass().getClassLoader();
        Loader loader = tomcat.createLoader(classLoader);
        engine.setLoader(loader);

        host = (Host)webServerContext.getBean("tomcat.host");
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
            org.abstracthorizon.extend.Extend.info.info("ContextPath=" + contextPath + ", workingLocation=" + URLDecoder.decode(getWorkingLocation().getFile(), "UTF-8"));    
            context = tomcat.createContext(contextPath, URLDecoder.decode(getWorkingLocation().getFile(), "UTF-8"));
    
            ClassLoader classLoader = getClassLoader();
            Loader loader = tomcat.createLoader(classLoader);
            context.setLoader(loader);
            StandardContext standardContext = (StandardContext)context;
            standardContext.setWorkDir("deploy/tomcat.sar/");
    //        File configFile = new File("deploy/tomcat.sar/");
    //        standardContext.setConfigFile(configFile.getAbsolutePath());
            standardContext.setDefaultWebXml("web.xml");
    
            host.addChild(context);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes context from the host.
     */
    @Override
    protected void stopInternal() {
        host.removeChild(context);
        context = null;
    }

    /**
     * Removes all references of the embedded tomcat and the host
     */
    @Override
    protected void destroyInternal() {
        tomcat = null;
        host = null;
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
    
    /**
     * Returns tomcat context
     * 
     * @return tomcat context
     */
    public Context getTomcatContext() {
        return context;
    }

}
