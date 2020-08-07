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
package org.abstracthorizon.extend.support.spring.server;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.support.URLUtils;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * Servers application context
 *
 * @author Daniel Sendula
 */
public class SpringBasedServerApplicationContext extends AbstractXmlApplicationContext {

    /** Server protocol url prefix */
    public static final String SERVER_PREFIX = "server-";

    /** Server location protocol url */
    public static final String SERVER_LOCATION = SERVER_PREFIX + "location:";

    /** Server location protocol url len */
    protected static final int SERVER_LOCATION_LEN = SERVER_LOCATION.length();

    /** Server data protocol url */
    public static final String SERVER_DATA = SERVER_PREFIX + "data:";

    /** Server data protocol url len */
    protected static final int SERVER_DATA_LEN = SERVER_DATA.length();

    /** Server deploy protocol url */
    public static final String SERVER_DEPLOY = SERVER_LOCATION + "deploy:";

    /** Server deploy protocol url len*/
    protected static final int SERVER_DEPLOY_LEN = SERVER_DEPLOY.length();

    /** Server config protocol url */
    public static final String SERVER_CONFIG = SERVER_LOCATION + "config:";
    
    /** Server config protocol url len*/
    protected static final int SERVER_CONFIG_LEN = SERVER_CONFIG.length();

    /** Module location */
    public static final String MODULE_LOCATION = "module-location:";

    /** Module location len. */
    protected static final int MODULE_LOCATION_LEN = MODULE_LOCATION.length();
    
    /** Server's location */
    protected URL serverLocation;

    /** Server's config location */
    protected URL serverConfigLocation;

    /** Server's config file location */
    protected URL serverConfigFile;

    /** Server's data location */
    protected URL serverDataLocation;

    /** Server's deploy location */
    protected URL serverDeployLocation;

    /**
     * Empty constructor
     */
    public SpringBasedServerApplicationContext() {
    }

    /**
     * Constructor
     * @param serverConfigLocation server's config location
     */
    public SpringBasedServerApplicationContext(URL serverLocation) {
        setServerLocation(serverLocation);
        try {
            setServerConfigLocation(URLUtils.add(serverLocation, "config/"));
            setServerConfigFile(URLUtils.add(serverLocation, "config/server.xml"));
            setServerDeployLocation(URLUtils.add(serverLocation, "deploy/"));
            setServerDataLocation(URLUtils.add(serverLocation, "data/"));
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets class loader from this classes class loader
     */
    public void refresh() {
        super.refresh();
    }

    /**
     * Returns server's config location
     * @return server's config location
     */
    protected String[] getConfigLocations() {
        if (URLUtils.exists(getServerConfigFile())) {
            return new String[]{serverConfigFile.toString()};
        } else {
            return new String[]{"classpath:/config/server.xml"};
        }
    }

    /**
     * Returns server's location
     * @return server's location
     */
    public URL getServerLocation() {
        return serverLocation;
    }

    /**
     * Sets server's location
     * @param serverLocation server's location
     */
    public void setServerLocation(URL serverLocation) {
        this.serverLocation = serverLocation;
    }

    /**
     * Returns server's config location
     * @return server's config location
     */
    public URL getServerConfigLocation() {
        return serverConfigLocation;
    }

    /**
     * Sets server's config location
     * @param serverConfigLocation server's config location
     */
    public void setServerConfigLocation(URL serverConfigLocation) {
        this.serverConfigLocation = serverConfigLocation;
    }

    /**
     * Returns server's config file
     * @return server's config file
     */
    public URL getServerConfigFile() {
        return serverConfigFile;
    }

    /**
     * Sets server's config file
     * @param serverConfigFile server's config file
     */
    public void setServerConfigFile(URL serverConfigFile) {
        this.serverConfigFile = serverConfigFile;
    }

    /**
     * Returns server's data location
     * @return server's data location
     */
    public URL getServerDataLocation() {
        return serverDataLocation;
    }

    /**
     * Sets server's data location
     * @param serverDataLocation server's data location
     */
    public void setServerDataLocation(URL serverDataLocation) {
        this.serverDataLocation = serverDataLocation;
    }

    /**
     * Returns server's deploy location
     * @return server's deploy location
     */
    public URL getServerDeployLocation() {
        return serverDeployLocation;
    }

    /**
     * Sets server's deploy location
     * @param serverDataLocation server's deploy location
     */
    public void setServerDeployLocation(URL serverDeployLocation) {
        this.serverDeployLocation = serverDeployLocation;
    }

    /**
     * Returns resource. This is wrapper over spring's own implementaiton to
     * add new URL types:
     * <ul>
     * <li>server-location - URL of server directory</li>
     * <li>server-data - URL of server data path</li>
     * <li>server-deploy - URL of server deploy path</li>
     * <li>server-config - URL of server config path</li>
     * </ul>
     */
    public Resource getResource(String location) {
        if (location.startsWith(SERVER_PREFIX)) {
            try {
                if (location.startsWith(SERVER_DATA)) {
                    return makeURLResource(serverDataLocation, location.substring(SERVER_DATA_LEN));
                } else if (location.startsWith(SERVER_DEPLOY)) {
                    return makeURLResource(serverDeployLocation, location.substring(SERVER_DEPLOY_LEN));
                } else if (location.startsWith(SERVER_CONFIG)) {
                    return makeURLResource(serverConfigLocation, location.substring(SERVER_CONFIG_LEN));
                } else if (location.startsWith(SERVER_LOCATION)) {
                    return makeURLResource(serverLocation, location.substring(SERVER_LOCATION_LEN));
                } else if (location.startsWith(MODULE_LOCATION)) {
                    location = location.substring(MODULE_LOCATION_LEN);
                    
                    DeploymentManager manager = (DeploymentManager)getBean("DeploymentManager");
                    ModuleId moduleId = new ModuleId(location);
                    Module module = manager.getDeployedModules().get(moduleId);
                    return makeURLResource(module.getWorkingLocation(), "");
                }
            } catch (UnsupportedEncodingException e){
                throw new RuntimeException(e);
            } catch (MalformedURLException ignore){
            }
        }
        return super.getResource(location);
    }

    /**
     * Creates URL resource
     * @param url url
     * @return URL resource
     * @throws MalformedURLException
     * @throws UnsupportedEncodingException 
     */
    protected Resource makeURLResource(URL url, String location) throws MalformedURLException, UnsupportedEncodingException {
        url = URLUtils.add(url, location);
        return new UrlResource(url);
    }

}
