/*
 * Copyright (c) 2009 Creative Sphere Limited.
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
package org.abstracthorizon.extend.repository.maven;

import java.net.URL;
import java.util.Map;

import org.abstracthorizon.extend.repository.maven.pom.RepositoryDefinition;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.ModuleLoader;

/**
 * This interface describes repository loader.
 *
 * @author Daniel Sendula
 */
public interface RepositoryLoader extends ModuleLoader {

    boolean isCheckSnapshotVersions();
    
    void setCheckSnapshotVersions(boolean check);
    
    Map<String, RepositoryDefinition> getRepositories();
    
    void addRepository(String id, URL url, boolean releases, boolean snapshots);
    
    DeploymentManager getDeploymentManager();
    
    Map<URL, Long> getFailedURLs();
    
    Map<URL, Long> getSucceddedURLs();
    
    void setDeploymentManager(DeploymentManager manager);
    
    void start();
    
    void stop();
    
}
