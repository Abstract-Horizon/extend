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


/**
 * This class represents project
 * 
 * @author Daniel Sendula
 */
public class POM extends Artifact {

    protected Parent parent;

    protected Dependencies dependencies;

    protected Properties properties;

    protected DependencyManagement dependencyManagement;
    
    protected POM parentPOM;
    
    protected String packaging;
    
    protected Repositories repositories;
    
    protected String name;
    
    protected String description;
    
    public POM() {
    }

    public POM getParentPOM() {
        return parentPOM;
    }
    
    public void setParentPOM(POM parentPOM) {
        this.parentPOM = parentPOM;
    }
    
    public Parent getParent() {
        return parent;
    }
    
    public Parent addParent() {
        if (parent == null) {
            parent = new Parent();
            return parent;
        } else {
            throw new RuntimeException("There can be only one Parent tag");
        }
    }
    
    public Dependencies getDependencies() {
        return dependencies;
    }

    public Dependencies addDependencies() {
        if (dependencies == null) {
            dependencies = new Dependencies();
        }
        return dependencies;
    }
    
    public Repositories getRepositories() {
        return repositories;
    }

    public Repositories addRepositories() {
        if (repositories == null) {
            repositories = new Repositories();
        }
        return repositories;
    }
    
    public DependencyManagement getDependencyManagement() {
        return dependencyManagement;
    }

    public DependencyManagement addDependencyManagement() {
        if (dependencyManagement == null) {
            dependencyManagement = new DependencyManagement();
        }
        return dependencyManagement;
    }
    
    public Properties getProperties() {
        return properties;
    }

    public Properties addProperties() {
        if (properties == null) {
            properties = new Properties();
        }
        return properties;
    }
    
    public String getPackaging() {
        return packaging;
    }
    
    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public Dependency findDependency(String groupId, String artifactId) {
        if (dependencies != null) {
            return dependencies.findDependency(groupId, artifactId);
        } else {
            return null;
        }
    }
    
    public Dependency findManagedDependency(String groupId, String artifactId) {
        Dependency dependency = null;
        if (dependencyManagement != null) {
            Dependencies dependencies = dependencyManagement.getDependencies();
            if (dependencies != null) {
                dependency = dependencies.findDependency(groupId, artifactId);
            }
        }
        if ((dependency == null) && (parentPOM != null)) {
            return parentPOM.findManagedDependency(groupId, artifactId);
        }
        return dependency;
    }
    
    
    public String toString() {
        StringBuffer res = new StringBuffer();
        ToStringHelper.openTag("project", res, 0);
        if (parent != null) {
            res.append(parent.toString(2));
        }
        res.append(super.toString(2));
        if (packaging != null) {
            ToStringHelper.valueTag("packaging", packaging, res, 2);
        }
        
        if (dependencies != null) {
            res.append(dependencies.toString(2));
        }
        if (dependencyManagement != null) {
            res.append(dependencyManagement.toString(2));
        }
        if (properties != null) {
            res.append(properties.toString(2));
        }
        ToStringHelper.closeTag("project", res, 0);
        return res.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
