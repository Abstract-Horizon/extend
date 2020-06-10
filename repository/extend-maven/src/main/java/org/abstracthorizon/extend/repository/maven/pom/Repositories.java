/*
 * Copyright (c) 2007 Creative Sphere Limited.
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
package org.abstracthorizon.extend.repository.maven.pom;

import java.util.ArrayList;
import java.util.List;

public class Repositories {

    protected List<Repository> repositories = new ArrayList<Repository>();
    
    public Repositories() {
    }
    
    public List<Repository> getRepositories() {
        return repositories;
    }
    
    public Repository addRepository() {
        Repository repository = new Repository();
        repositories.add(repository);
        return repository;
    }
    
    public Repository findRepository(String id) {
        for (Repository repository : repositories) {
            if (id.equals(repository.getId())) {
                return repository;
            }
        }
        return null;
    }
    
    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        ToStringHelper.openTag("repositories", res, indent);
        for (Repository rep : repositories) {
            res.append(rep.toString(indent + 2));
        }
        ToStringHelper.closeTag("repositories", res, indent);
        return res.toString();
    }

    public String toString() {
        return toString(2);
    }

}
