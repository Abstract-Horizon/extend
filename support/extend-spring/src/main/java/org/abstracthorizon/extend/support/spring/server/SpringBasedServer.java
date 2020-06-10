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
package org.abstracthorizon.extend.support.spring.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.support.BulkDeploy;
import org.abstracthorizon.extend.server.deployment.support.DeploymentDirectoryModule;
import org.abstracthorizon.extend.server.support.ClassUtils;
import org.abstracthorizon.extend.server.support.EnhancedMap;
import org.abstracthorizon.extend.server.support.KernelScheduler;
import org.abstracthorizon.extend.server.support.URLUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * This class sets up the server.
 * 
 * @author Daniel Sendula
 */
public class SpringBasedServer {

    /** Home location of server. Roor path where bin directory is. */
    protected URL homeLocation;

    /** Setver's location */
    protected URL serverLocation;

    /** Root class loader for server */
    protected ClassLoader classLoader;

    /** Server's configuration */
    protected SpringBasedServerApplicationContext serverApplicationContext;

    /** Empty constructor */
    public SpringBasedServer() {
        try {
            serverLocation = new File(".").toURI().toURL();
            homeLocation = serverLocation;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructgor.
     * 
     * @param homeLocation
     *                home locaiton of server. Root path where bin directory is.
     * @param serverLocation
     *                server's location
     */
    public SpringBasedServer(URL homeLocation, URL serverLocation) {
        setHomeLocation(homeLocation);
        setServerLocation(serverLocation);
    }

    /**
     * This method sets up class loader and loads server context.
     */
    public void create() {
        try {
            URL lib = URLUtils.add(serverLocation, "lib/");

            classLoader = ClassUtils.createClassLoader(getClass().getClassLoader(), lib);

            // URL serverConfigLocation = URLUtils.add(serverLocation, "config/server.xml");
            serverApplicationContext = new SpringBasedServerApplicationContext(serverLocation);
            serverApplicationContext.setClassLoader(classLoader);
            serverApplicationContext.refresh();
            serverApplicationContext.getBeanFactory().registerSingleton("Server", this);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method deploys &quot;deploy&quot; directory. Effectively this function starts up the server.
     */
    public void start() {
        Extend.info.info("Setting up the server");
        try {
            if ("file".equals(serverLocation.getProtocol())) {
                String f = URLDecoder.decode(serverLocation.getFile(), "UTF-8");
                if ((f.length() > 1) && f.endsWith("/")) {
                    f = f.substring(0, f.length() - 1);
                }
                System.setProperty("user.dir", f);
            }
            URL defaultDeploymentLocation = URLUtils.add(serverLocation, "deploy/");

            BulkDeploy bulkDeploy = (BulkDeploy) serverApplicationContext.getBean("BulkDeploy");

            bootstrapDeploy(bulkDeploy);

            DeploymentManager deploymentManager = (DeploymentManager) serverApplicationContext
                    .getBean(DeploymentManager.DEPLOYMENT_MANAGER_DEFAULT_NAME);
            try {
                DeploymentDirectoryModule defaultDeploymentDirectory = (DeploymentDirectoryModule) serverApplicationContext
                        .getBean("DeploymentDirectoryModule");

                defaultDeploymentDirectory.setModuleId(new ModuleId("system.DefaultDeploymentDirectory:current:internal"));
                defaultDeploymentDirectory.setLocation(defaultDeploymentLocation);
                defaultDeploymentDirectory.setState(Module.DEFINED);

                try {
                    deploymentManager.deploy(defaultDeploymentDirectory.getModuleId(), defaultDeploymentDirectory);
                } catch (Exception e) {
                    Extend.info.warn("Caught exception trying to deploy module " + defaultDeploymentDirectory.getModuleId() + ".", e);
                }
            } catch (NoSuchBeanDefinitionException ignore) {
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Does nothing.
     * 
     */
    public void stop() {
        DeploymentManager deploymentManager = (DeploymentManager) serverApplicationContext
                .getBean(DeploymentManager.DEPLOYMENT_MANAGER_DEFAULT_NAME);
        EnhancedMap<ModuleId, Module> modules = deploymentManager.getDeployedModules();
        ArrayList<Module> copy = new ArrayList<Module>(modules.values());
        for (Module module : copy) {
            try {
                deploymentManager.undeploy(module);
            } catch (Exception e) {
                Extend.info.warn("Caught exception trying to undeploy module " + module.getModuleId() + ".", e);
            }
        }

        KernelScheduler kernelScheduler = (KernelScheduler) serverApplicationContext.getBean("KernelScheduler");
        kernelScheduler.destroy();
        Extend.info.info("Undeployed all modules. Shutting down.");
    }

    /**
     * Closes server's context and clears down class loader
     * 
     */
    public void destroy() {
        serverApplicationContext.close();
        classLoader = null;
    }

    /**
     * Utility method that calls {@link #stop()} method and then {@link #destroy()} method.
     */
    public void shutdown() {
        try {
            stop();
        } finally {
            destroy();
        }
    }

    /**
     * Deploys bootstrap modules from &quot;/config/bootstrap.deploy&quot; file.
     * 
     * @param bulkDeploy
     */
    public void bootstrapDeploy(BulkDeploy bulkDeploy) {
        try {
            URL url = URLUtils.add(getServerLocation(), "/config/bootstrap.deploy");
            if (!URLUtils.exists(url)) {
                url = getClass().getResource("/config/bootstrap.deploy");
            }
            if (url != null) {
                InputStream inputStream = url.openStream();
                try {
                    bulkDeploy.load(url.toURI());
                } finally {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns server's location
     * 
     * @return server's location
     */
    public URL getServerLocation() {
        return serverLocation;
    }

    /**
     * Sets server's location
     * 
     * @param serverLocation
     *                server's location
     */
    public void setServerLocation(URL serverLocation) {
        this.serverLocation = serverLocation;
    }

    /**
     * Returns home location
     * 
     * @return home location
     */
    public URL getHomeLocation() {
        return homeLocation;
    }

    /**
     * Sets home location
     * 
     * @param homeLocation
     *                home location
     */
    public void setHomeLocation(URL homeLocation) {
        this.homeLocation = homeLocation;
    }

    /**
     * Returns class loader
     * 
     * @return class loader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets class loader
     * 
     * @param classLoader
     *                class loader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Returns server application context
     * 
     * @return server application context
     */
    public SpringBasedServerApplicationContext getServerApplicationContext() {
        return serverApplicationContext;
    }

    /**
     * Helper method for registering singleton beans with the server's application context
     * 
     * @param name
     *                bean name
     * @param bean
     *                bean to be registered
     */
    public void registerBean(String name, Object bean) {
        serverApplicationContext.getBeanFactory().registerSingleton(name, bean);
    }

}
