/*
 * Copyright (c) 2011 Creative Sphere Limited.
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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.support.ArchiveUtils;
import org.abstracthorizon.extend.server.support.URLUtils;
import org.abstracthorizon.extend.support.spring.deployment.AbstractApplicationContextModule;
import org.abstracthorizon.extend.support.spring.service.ServiceModuleLoader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <p>
 * Tomcat war archive (or directory) module loader (deployer).
 * It checks if &quot;WEB-INF&quot; directory is present and if so
 * deploys archive with (embedded) Tomcat.
 * </p>
 * <p>
 * Current implementation creates context and deploys it with
 * current host (defined under &quot;tomcat.host&quot; name in
 * tomcat service archive).
 * </p>
 *
 * @author Daniel Sendula
 */
public class FullTomcatWarModuleLoader extends ServiceModuleLoader {

    private FullTomcatController tomcatController;
    
    /**
     * Empty constructor.
     */
    public FullTomcatWarModuleLoader() {
    }

    public FullTomcatController getTomcatController() {
        return tomcatController;
    }
    
    public void setTomcatController(FullTomcatController tomcatController) {
        this.tomcatController = tomcatController;
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

    public Module loadAs(URI uri, ModuleId moduleId) {
        if ("war".equalsIgnoreCase(moduleId.getType())) {

            try {
                URL originalLocation = uri.toURL();
                URL location = uri.toURL();
                try {
                    File archiveFile = new File(URLDecoder.decode(originalLocation.getFile(), "UTF-8"));
                    String archiveFileName = archiveFile.getName();
                    if (!"file".equals(originalLocation.getProtocol())) {
                        File tmp = File.createTempFile("sas_", archiveFileName);
                        ArchiveUtils.downloadArchive(originalLocation, tmp, getBufferSize());
                        archiveFile = tmp;
                    }
                    location = archiveFile.toURI().toURL();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
    
                AbstractApplicationContextModule service = createModule(location);
                if (moduleId != null) {
                    service.setModuleId(moduleId);
                }
                service.setParent(root);
                service.setLocation(location);
                service.setOriginalLocation(originalLocation);
                
                preDefinitionProcessing(service);
                service.setState(Module.DEFINED);
                postDefinitionProcessing(service);
                if (redeployURLScanner != null) {
                    addFilesForScanning(service);
                }
                return service;
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return super.loadAs(uri, moduleId);
        }
    }
    
    /**
     * Creates new module {@link FullTomcatWebApplicationContext} module
     * @param url url for the module
     * @return newly created module
     */
    protected AbstractApplicationContextModule createModule(URL url) {
        ModuleId moduleId = ModuleId.createModuleIdFromFileName(url.getPath());
        return new FullTomcatWebApplicationContext(moduleId, getTomcatController());
    }

    /**
     * Called before module is pronounced defined
     * @param service service
     */
    protected void preDefinitionProcessing(AbstractApplicationContextModule service) {
        FullTomcatWebApplicationContext tomcatContext = (FullTomcatWebApplicationContext)service;
        tomcatContext.setWebServerContext(root);
        tomcatContext.setParent(root.getParent());
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
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                } catch (MalformedURLException ignore) {
                }
            }
        }
    }
}
