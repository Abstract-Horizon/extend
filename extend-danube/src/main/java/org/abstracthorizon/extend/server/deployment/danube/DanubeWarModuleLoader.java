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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.support.URLUtils;
import org.abstracthorizon.extend.support.spring.deployment.AbstractApplicationContextModule;
import org.abstracthorizon.extend.support.spring.service.ServiceModuleLoader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <p>
 * Danube war archive (or directory) module loader (deployer).
 * It checks if &quot;web-application.xml&quot; file is present and if so
 * deploys archive with Danube.
 * </p>
 * <p>
 * Current implementation creates context and deploys it with
 * current host (defined under &quot;tomcat.host&quot; name in
 * tomcat service archive).
 * </p>
 *
 * @author Daniel Sendula
 */
public class DanubeWarModuleLoader extends ServiceModuleLoader {

    /**
     * Empty constructor.
     */
    public DanubeWarModuleLoader() {
    }

    /**
     * Returns <code>true</code> if &quot;web-application.xml&quot; exists in specified directory.
     * @param uri URI
     * @return <code>true</code> if &quot;web-application.xml&quot; exists in specified directory.
     */
    public boolean canLoad(URI uri) {
        if (super.canLoad(uri)) {
            try {
                URI webContextURL = null;
                String s = uri.toString();
                if (s.endsWith(".xml")) {
                    webContextURL = uri;
                } else {
                    if (!URLUtils.isFolder(uri)) {
                        webContextURL = new URI("jar:" + uri + "!/web-application.xml" );
                    } else {
                        webContextURL = URLUtils.add(uri, "web-application.xml");
                    }
                }

                return URLUtils.exists(webContextURL);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * Creates new module {@link DanubeWebApplicationContext} module
     * @param url url for the module
     * @return newly created module
     */
    protected AbstractApplicationContextModule createModule(URL url) {
        AbstractApplicationContextModule module = new DanubeWebApplicationContext(ModuleId.createModuleIdFromFileName(url.getPath()));
        return module;
    }

    /**
     * Called before module is pronounced defined
     * @param service service
     */
    protected void preDefinitionProcessing(AbstractApplicationContextModule service) {
        DanubeWebApplicationContext danubeContext = (DanubeWebApplicationContext)service;
        danubeContext.setWebServerContext(root);
        // danubeContext.setParent(root.getParent());
        danubeContext.setParent(root);
    }

    /**
     * Sets application context
     * @param applicationContext application context this bean is created under
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.root = (ConfigurableApplicationContext)applicationContext;
    }
}
