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
package org.abstracthorizon.extend.support.spring.service;

import java.net.URI;

import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.service.AbstractServiceModuleLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Service module loader. It loads &quot;service.xml&quot; file as
 * spring's application context xml configuration file.
 * TODO: explain donwloading and unpacking of archives
 *
 * @author Daniel Sendula
 */
public abstract class SpringAbstractServiceModuleLoader extends AbstractServiceModuleLoader implements ApplicationContextAware {

    /** Root context this loader is created in */
    protected ConfigurableApplicationContext root;

    /**
     * Empty constructor
     */
    public SpringAbstractServiceModuleLoader() {
    }

    /**
     * Returns <code>true</code> if module loader knows how to load (create) module from given URI.
     * @param uri URI
     * @return <code>true</code> if module loader knows how to load (create) module from given URI.
     */
    public boolean canLoad(URI uri) {
        String file = uri.getPath();
        if (file != null) {
            if (file.endsWith("/")) {
                if ("file".equals(uri.getScheme())) {
                    file = file.substring(0, file.length() - 1);
                } else {
                    return false;
                }
            }
            for (String extension : extensions) {
                if (file.endsWith(extension)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets root application context to be used when creating new {@link ServiceApplicationContextModule} instances.
     * @param applicationContext root application context
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.root = (ConfigurableApplicationContext)applicationContext;
    }

    /**
     * Adds this loader to &quot;DeploymentManager&quot; {@link DeploymentManager}.
     */
    public void start() {
    	if (getDeploymentManager() == null) {
	        DeploymentManager deploymentManager = (DeploymentManager)root.getBean(DeploymentManager.DEPLOYMENT_MANAGER_DEFAULT_NAME);
	        setDeploymentManager(deploymentManager);
    	}
        super.start();
    }
}
