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
package org.abstracthorizon.extend.server.deployment;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for common {@link Module} functions.
 *
 * @author Daniel Sendula
 */
public class ModuleUtils {

    /**
     * Empty set
     */
    public static final Set<Module> EMPTY_SET = Collections.emptySet();

    private ModuleUtils() {
    }
    
    /**
     * Converts a state to a string
     * @param state state
     * @return state as a string
     */
    public static String stateAsString(int state) {
        if (state == Module.UNDEFINED) {
            return "UNDEFINED";
        } else if (state == Module.DEFINED) {
            return "DEFINED";
        } else if (state == Module.WAITING_ON_CREATE) {
            return "WAITING ON DEPENDENCY CREATE";
        } else if (state == Module.WAITING_ON_CREATE_TO_START) {
            return "WAITING ON DEPENDENCY CREATE AND START";
        } else if (state == Module.CREATED) {
            return "CREATED";
        } else if (state == Module.WAITING_ON_START) {
            return "WAITING ON DEPENDENCY START";
        } else if (state == Module.STARTED) {
            return "STARTED";
        } else {
            return "UNLKNOWN";
        }
    }

    public static enum FindStrategy {
        RETURN_FIRST,
        RETURN_LAST,
        FAIL_IF_MORE_THAN_ONE
    }
    
    public static class MoreThanOneException extends RuntimeException { }
    
    public static Module findLoadedModule(DeploymentManager deploymentManager, URI uri, FindStrategy strategy) throws MoreThanOneException {
        List<ModuleId> moduleIds = new ArrayList<ModuleId>();
        
        for (ModuleLoader moduleLoader : deploymentManager.getModuleLoaders()) {
            if (moduleLoader.canLoad(uri)) {
                ModuleId moduleId = moduleLoader.toModuleId(uri);
                moduleIds.add(moduleId);
            }
        }
        
        Module module = null;
        Map<ModuleId, Module> modules = deploymentManager.getDeployedModules();
        for (ModuleId moduleId : moduleIds) {
            Module m = modules.get(moduleId);
            if (m != null) {
                if (FindStrategy.RETURN_FIRST == strategy) {
                    return m;
                } else if ((module != null) && (m != null) && (FindStrategy.FAIL_IF_MORE_THAN_ONE == strategy)) {
                    throw new MoreThanOneException();
                } else {
                    module = m;
                }
            }
        }
        return module;
    }
    
}
