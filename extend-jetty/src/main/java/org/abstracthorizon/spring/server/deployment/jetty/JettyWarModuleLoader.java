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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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
 * Jetty war archive (or directory) module loader (deployer).
 * It checks if &quot;WEB-INF&quot; directory is present and if so
 * deploys archive with (embedded) Jetty.
 * </p>
 * <p>
 * Current implementation creates context and deploys it with
 * current host (defined under &quot;jetty.host&quot; name in
 * jetty service archive).
 * </p>
 *
 * @author Daniel Sendula
 */
public class JettyWarModuleLoader extends ServiceModuleLoader {

    /**
     * Empty constructor.
     */
    public JettyWarModuleLoader() {
    }

    /**
     * Returns <code>true</code> if &quot;WEB-INF&quot; exists in specified directory.
     * @param uri URI
     * @return <code>true</code> if &quot;WEB-INF&quot; exists in specified directory.
     */
    public boolean canLoad(URI uri) {
        if (super.canLoad(uri)) {
            if (URLUtils.isFolder(uri)) {
                try {
                    URI webINFURL = URLUtils.add(uri, "WEB-INF");
                    return URLUtils.exists(webINFURL);
                } catch (URISyntaxException ignore) {
                }
            } else {
                // TODO it's archive - so it must be OK. Right?
                return true;
            }
        }
        return false;
    }

    /**
     * Creates new module {@link JettyWebApplicationContext} module
     * @param url url for the module
     * @return newly created module
     */
    protected AbstractApplicationContextModule createModule(URL url) {
        return new JettyWebApplicationContext(ModuleId.createModuleIdFromFileName(url.getPath()));
    }

    /**
     * Called before module is pronounced defined
     * @param service service
     */
    protected void preDefinitionProcessing(AbstractApplicationContextModule service) {
        JettyWebApplicationContext jettyContext = (JettyWebApplicationContext)service;
        jettyContext.setWebServerContext(root);
        jettyContext.setParent(root.getParent());
    }

    /**
     * Sets application context
     * @param applicationContext application context this bean is created under
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.root = (ConfigurableApplicationContext)applicationContext;
    }

    /**
     * Hook method that collects all important files from newly created module and
     * adds then to the URL redeployment scanner.
     * @param module module
     */
    protected void addFilesForScanning(AbstractApplicationContextModule module) {
        super.addFilesForScanning(module);
        if (redeployURLScanner != null) {
            // TODO should this be set in case of working location only?!

            URL locationURL = module.getOriginalLocation();
            if (locationURL.equals(module.getWorkingLocation())) {
                URL serviceFileURL = module.getServiceFile();
                redeployURLScanner.addURL(serviceFileURL, module);
                try {
                    URL url = URLUtils.add(locationURL, "WEB-INF/web.xml");
                    if (URLUtils.exists(url)) {
                        redeployURLScanner.addURL(url, module);
                    }
                } catch (UnsupportedEncodingException e){
                    throw new RuntimeException(e);
                } catch (MalformedURLException ignore) {
                }
            }
        }
    }
}
