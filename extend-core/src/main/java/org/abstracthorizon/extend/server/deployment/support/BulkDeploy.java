/*
 * Copyright (c) 2007-2020 Creative Sphere Limited.
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
package org.abstracthorizon.extend.server.deployment.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleUtils;
import org.abstracthorizon.extend.server.deployment.service.AbstractServiceModuleLoader;

/**
 * This class deploys all URL (URIs) given in specified file.
 * 
 * Note: It doesn't return module.
 * 
 * @author daniel
 */
public class BulkDeploy extends AbstractServiceModuleLoader {

    /**
     * Constructor
     */
    public BulkDeploy() {
    }

    
    /**
     * Translates URI to moduleId. This method returns <code>null</code>.
     * @param uri uri
     * @return module id or <code>null</code>
     */
    public ModuleId toModuleId(URI uri) {
        return null;
    }

    
    /**
     * Loads all modules from given URI.
     * 
     * @param uri uri
     * 
     * @return <code>null</code>
     */
    public Module load(URI uri) {
        try {
            URL url = uri.toURL();
            InputStream inputStream = url.openStream();
            try {
                processDeploymentStream(inputStream, new LocalDeployer());
            } finally {
                try {
                    inputStream.close();
                } catch (IOException ignore) {
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    

    /**
     * This method is not supported here.
     * 
     * @param uri uri
     * @param moduleId moduleId
     * 
     * @return <code>null</code>
     */
    public Module loadAs(URI uri, ModuleId moduleId) {
        throw new UnsupportedOperationException("Method not supported");
    }

    protected void processDeploymentStream(InputStream inputStream, ModuleDeployer deployer) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line = in.readLine();
        while (line != null) {
            processDeploymentModule(line, deployer);
            line = in.readLine();
        }
    }

    protected void processDeploymentModules(Collection<String> modules, ModuleDeployer deployer) throws IOException {
        for (String module: modules) {
            processDeploymentModule(module, deployer);
        }
    }
    
    protected void processDeploymentModule(String moduleURI, ModuleDeployer deployer) throws IOException {
        moduleURI = moduleURI.trim();
        if (moduleURI.length() > 0) {
            if (moduleURI.charAt(1) != '#') {
                int i = moduleURI.indexOf('#');
                if (i >= 0) {
                    moduleURI = moduleURI.substring(0, i).trim();
                }
                if (moduleURI.length() > 0) {
                    deployer.deploy(moduleURI);
                }
            }
        }
    }
    
    
    
    public static interface ModuleDeployer {
        void deploy(String artifactURI);
    }
    
    public class LocalDeployer implements ModuleDeployer {
        public void deploy(String artifactURI) {
            try {
                URI uri = new URI(artifactURI);
                deploymentManager.loadAndDeploy(uri);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public class LocalUndeployer implements ModuleDeployer {
        public void deploy(String artifactURI) {
            try {
                URI uri = new URI(artifactURI);
                Module module = ModuleUtils.findLoadedModule(deploymentManager, uri, ModuleUtils.FindStrategy.RETURN_FIRST);
                if (module != null && deploymentManager.getDeployedModules().containsValue(module)) {
                    deploymentManager.undeploy(module);
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
