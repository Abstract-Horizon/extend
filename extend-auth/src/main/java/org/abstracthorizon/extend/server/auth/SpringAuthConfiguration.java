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

package org.abstracthorizon.extend.server.auth;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * Authorisation configuiration that can be defined through spring configuration (or in that matter
 * any other programmable way.
 *
 * @author Daniel Sendula
 */
public class SpringAuthConfiguration extends Configuration {

    /** Login modules mapped to configuration entries */
    protected Map<String, AppConfigurationEntry[]> entries = new HashMap<String, AppConfigurationEntry[]>();

    /**
     * Constructor
     */
    public SpringAuthConfiguration() {
    }

    /**
     * Establishes itself as a authorisation configuration
     *
     */
    public void init() {
        setConfiguration(this);
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String loginContext) {
        return entries.get(loginContext);
    }

    /**
     * Sets array of application configuration entry at given login context
     * @param loginContext login context
     * @param entries application configuraiton entries
     */
    public void setAppConfigurationEntry(String loginContext, AppConfigurationEntry[] entries) {
        this.entries.put(loginContext, entries);
    }

    /**
     * Removes login context
     * @param loginContext login context
     */
    public void removeAppConfigurationEntry(String loginContext) {
        this.entries.remove(loginContext);
    }

//    public void setAppConfigurationEntry(String name, List<AppConfigurationEntry> entries) {
//        AppConfigurationEntry[] es = new AppConfigurationEntry[entries.size()];
//        es = (AppConfigurationEntry[])entries.toArray(es);
//        this.entries.put(name, es);
//    }

    /**
     * Does nothing
     */
    @Override
    public void refresh() {
    }

}
