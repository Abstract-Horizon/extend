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
package org.abstracthorizon.extend.server.deployment.danube;

import org.abstracthorizon.danube.connection.ConnectionHandler;
import org.abstracthorizon.danube.http.Selector;
import org.abstracthorizon.danube.http.matcher.Prefix;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.support.spring.service.ServiceApplicationContextModule;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;

/**
 * Module that represents Danube web application.
 *
 * @author Daniel Sendula
 */
public class DanubeWebApplicationContext extends ServiceApplicationContextModule {

    /** &quot;path&quot this context is deployed at root level with. */
    protected Prefix matcher;

    /** Context path this web application is going to be deployed on */
    protected String contextPath;

    /** Context */
    protected ApplicationContext webServerContext;

    /**
     * Empty constructor.
     */
    public DanubeWebApplicationContext(ModuleId moduleId) {
        super(moduleId);
    }


    /**
     * Sets up parser for this application context.
     * This implementation uses {@link DanubeWarModuleXmlParser} without validation
     * with {@link #internalClassLoader}.
     */
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader xmlbeandefinitionreader) {
        xmlbeandefinitionreader.setDocumentReaderClass(DanubeWarModuleXmlParser.class);
        xmlbeandefinitionreader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        xmlbeandefinitionreader.setBeanClassLoader(internalClassLoader);
    }

    /**
     * Sets this module dependancy on (danube's) module that created loader.
     */
    protected void createInternal() {
        super.createInternal();
        Module module = (Module)webServerContext;
        getDependsOn().add(module);
        module.getDependOnThis().add(this);
    }

    /**
     * Obtains reference to the &quot;web-application&quot; bean and uses it while
     * creating {@link Prefix} matcher with context path. Context path is or one supplied through
     * &quot;&lt;context-path&gt;&quot; tag or name of the module if tag is not specified.
     * Newly created matcher is then added to the http server connection handler.
     */
    @Override
    protected void startInternal() {
        super.startInternal();
        Selector selector = (Selector)webServerContext.getBean("httpServerSelector");
        ConnectionHandler handler = (ConnectionHandler)getBean("web-application");
        matcher = new Prefix();

        ThreadContextHandler threadContextHandler = new ThreadContextHandler(handler, this);

        String contextPath = getContextPath();
        if (contextPath == null) {
            contextPath = "/" + getModuleId().getArtifactId();
            setContextPath(contextPath);
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
            setContextPath(contextPath);
        }

        matcher.setPrefix(contextPath);
        matcher.setConnectionHandler(threadContextHandler);
        selector.getComponents().add(matcher);
    }

    /**
     * Removes previously created matcher.
     */
    @Override
    protected void stopInternal() {
        super.stopInternal();
        Selector selector = (Selector)webServerContext.getBean("httpServerSelector");
        selector.getComponents().remove(matcher);
    }

    /**
     * Empty implementation
     */
    @Override
    protected void destroyInternal() {
        super.destroyInternal();
    }

    /**
     * Returns &quot;web-application.xml&quot;
     * @return &quot;web-application.xml&quot;
     */
    protected String getContextFileName() {
        return "web-application.xml";
    }

    /**
     * Returns web server's context
     * @return web server's context
     */
    public ApplicationContext getWebServerContext() {
        return webServerContext;
    }

    /**
     * Sets web server's context
     * @param webServerContext web server's context
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
