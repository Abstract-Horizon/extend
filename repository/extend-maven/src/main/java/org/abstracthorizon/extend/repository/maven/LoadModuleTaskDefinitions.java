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

import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.abstracthorizon.extend.repository.concurent.TaskGroup;
import org.abstracthorizon.extend.repository.maven.pom.Artifact;
import org.abstracthorizon.extend.repository.maven.pom.RepositoryDefinition;
import org.abstracthorizon.extend.server.deployment.Module;

/**
 * Load module task definitions.
 *
 * @author Daniel Sendula
 */
public class LoadModuleTaskDefinitions {

    private Artifact originalArtifact; 
    private Set<Artifact> excludes;
    private boolean optional;
    private Map<String, RepositoryDefinition> repositories;
    private Stack<Artifact> stack;
    private TaskGroup<Module, LoadModuleTaskDefinitions> group;
    private Artifact finalArtifact;
    private Artifact snapshotArtifact;
    private int hashCode;
    private boolean emptyType;

    public LoadModuleTaskDefinitions(
            Artifact artifact, Set<Artifact> excludes, 
            boolean optional, Map<String, RepositoryDefinition> repositories,
            Stack<Artifact> stack,
            TaskGroup<Module, LoadModuleTaskDefinitions> group) {
        
        this.originalArtifact = artifact;
        this.excludes = excludes;
        this.optional = optional;
        this.repositories = repositories;
        this.stack = stack;
        this.group = group;
        this.finalArtifact = artifact.toNonSnapshotArtifact();
        this.snapshotArtifact = artifact.toSnapshotArtifact();

        hashCode = 0;
        if (artifact.getGroupId() != null) {
            hashCode = Integer.rotateLeft(hashCode ^ artifact.getGroupId().hashCode(), 1);
        }
        if (artifact.getArtifactId() != null) {
            hashCode = Integer.rotateLeft(hashCode ^ artifact.getArtifactId().hashCode(), 1);
        }
        emptyType = artifact.getType() == null;
    }

    public Artifact getArtifact() {
        return originalArtifact;
    }

    public Artifact getFinalArtifact() {
        return finalArtifact;
    }

    public Artifact getSnapshotArtifact() {
        return snapshotArtifact;
    }

    public Set<Artifact> getExcludes() {
        return excludes;
    }

    public boolean isOptional() {
        return optional;
    }

    public Map<String, RepositoryDefinition> getRepositories() {
        return repositories;
    }

    public Stack<Artifact> getStack() {
        return stack;
    }
    
    public TaskGroup<Module, LoadModuleTaskDefinitions> getGroup() {
        return group;
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    public boolean equals(Object o) {
        if (o instanceof LoadModuleTaskDefinitions) {
            LoadModuleTaskDefinitions other = (LoadModuleTaskDefinitions)o;
            
            if (finalArtifact == other.finalArtifact) {
                return true;
            }
            
            String groupId = getArtifact().getGroupId();
            String otherGroupId = other.getArtifact().getGroupId(); 
            
            String artifactId = getArtifact().getArtifactId();
            String otherArtifactId = other.getArtifact().getArtifactId(); 

            String version = getFinalArtifact().getVersion();
            String otherVersion = other.getFinalArtifact().getVersion(); 

            String classifier = getArtifact().getClassifier();
            String otherClassifier = other.getArtifact().getClassifier(); 
            
            if (emptyType) {
                boolean res = (((otherGroupId == groupId) || ((groupId != null) && groupId.equals(otherGroupId)))
                        && ((otherArtifactId == artifactId) || ((artifactId != null) && artifactId.equals(otherArtifactId)))
                        && ((otherVersion == version) || ((version != null) && version.equals(otherVersion)))
                        && ((otherClassifier == classifier) || ((classifier != null) && classifier.equals(otherClassifier))));

                return res;
            } else {
                if (finalArtifact != null) {
                    return finalArtifact.equals(other.finalArtifact);
                }
            }
        }
        return super.equals(o);
    }
    
    public String toString() {
        return "LoadModuleTaskDefs[" + finalArtifact + "]";
    }
    
}