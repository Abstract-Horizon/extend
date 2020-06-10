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

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.support.spring.service.ServiceApplicationContextModule;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;

/**
 * <p>
 *   Module that represents a war module deployed with Jetty. Also it can define beans in
 *   a same way as service application module (this is an extension of it).
 * </p>
 * <p>
 *   It uses &quot;WEB-INF/web-application.xml&quot; for application context xml.
 * </p>
 * @author Daniel Sendula
 */
public class JettyWebApplicationContext extends ServiceApplicationContextModule {

    /** Reference to embedded Jetty */
    protected Server jetty;

    /** Jetty context */
    protected WebAppContext context;

    /** Context of web server - Jetty's service application context */
    protected ApplicationContext webServerContext;

    /** Context path for this module */
    protected String contextPath;

    /**
     * Empty constructor
     */
    public JettyWebApplicationContext(ModuleId moduleId) {
        super(moduleId);
    }

    /**
     * Obtains references to the embedded Jetty and to the host.
     * Sets this module to be depended on Jetty's service application module.
     */
    protected void createInternal() {
        Module module = (Module)webServerContext;
        getDependsOn().add(module);
        module.getDependOnThis().add(this);

        jetty = (Server)webServerContext.getBean("jetty");
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

        context = new WebAppContext(getWorkingLocation().toString(), contextPath);
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("Started context: " + contextPath);
        }

        jetty.addHandler(context);

        try {
            context.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes context from the host.
     */
    @Override
    protected void stopInternal() {
        jetty.removeHandler(context);
        try {
            context.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        context = null;
    }

    /**
     * Removes all references of the embedded Jetty and the host
     */
    @Override
    protected void destroyInternal() {
        jetty = null;
    }

    /**
     * It uses &quot;WEB-INF/web-application.xml&quot; for application context xml.
     * @return &quot;WEB-INF/web-application.xml&quot;
     */
    protected String getContextFileName() {
        return "WEB-INF/web-application.xml";
    }

    /**
     * Jetty's service application context
     * @return Jetty's service application context
     */
    public ApplicationContext getWebServerContext() {
        return webServerContext;
    }

    /**
     * Set's Jetty's service application context
     * @param webServerContext Jetty's service application context
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
     * Returns Jetty context
     *
     * @return Jetty context
     */
    public Context getJettyContext() {
        return context;
    }

}
