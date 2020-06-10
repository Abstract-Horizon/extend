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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repository.maven.pom.Artifact;
import org.abstracthorizon.extend.repository.maven.pom.Dependency;
import org.abstracthorizon.extend.repository.maven.pom.Exclusions;
import org.abstracthorizon.extend.repository.maven.pom.POM;
import org.abstracthorizon.extend.repository.maven.pom.Repositories;
import org.abstracthorizon.extend.repository.maven.pom.Repository;
import org.abstracthorizon.extend.repository.maven.pom.RepositoryDefinition;
import org.abstracthorizon.extend.server.support.URLUtils;

/**
 * Utility methods.
 *
 * @author Daniel Sendula
 */
public class Utils {

    public static String stackToString(Stack<Artifact> stack) {
        StringWriter r = new StringWriter();
        PrintWriter out = new PrintWriter(r);
        
        out.println();
        out.println("Stack:\n");
        for (Artifact a : stack) {
            out.println(a.getFullId());
        }
        out.println();
        
        return r.toString();
    }

    public static boolean excludesContain(Set<Artifact> excludes, Dependency dependency) {
        for (Artifact artifact : excludes) {
            if (artifact.getGroupId().equals(dependency.getGroupId())) {
                if (artifact.getArtifactId().equals(dependency.getArtifactId())) {
                    if ((artifact.getVersion() == null)
                            || (dependency.getVersion() == null)
                            || artifact.getVersion().equals(dependency.getVersion())) {
    
                        if ((artifact.getType() == null)
                                || (dependency.getType() == null)
                                || artifact.getType().equals(dependency.getType())) {
    
                            if ((artifact.getClassifier() == null)
                                    || (dependency.getClassifier() == null)
                                    || artifact.getClassifier().equals(dependency.getClassifier())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Copies files and directories
     * @param fromFile source file/dir
     * @param toFile destination file/dir
     * @return
     */
    public static boolean copyFile(File fromFile, File toFile) {
        if (!toFile.equals(fromFile)) {
            try {
                FileInputStream fromInputStream = new FileInputStream(fromFile);
                try {
                    FileOutputStream toOutputStream = new FileOutputStream(toFile);
                    try {
                        FileChannel inChannel = fromInputStream.getChannel();
                        try {
                            FileChannel outChannel = toOutputStream.getChannel();
                            try {
                                MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
    
                                outChannel.write(buffer);
                            } finally {
                                outChannel.close();
                            }
                        } finally {
                            inChannel.close();
                        }
                    } finally {
                        toOutputStream.close();
                    }
                } finally {
                    fromInputStream.close();
                }
                toFile.setLastModified(fromFile.lastModified());
                return true;
            } catch (IOException exc) {
                return false;
            }
        } else {
            return true;
        }
    }

    public static Artifact parseArtifact(String fileId) {
        Artifact artifact = new Artifact();
        StringTokenizer tokenizer = new StringTokenizer(fileId, ":");
        artifact.setGroupId(tokenizer.nextToken());
        artifact.setArtifactId(tokenizer.nextToken());
        artifact.setVersion(tokenizer.nextToken());
        if (tokenizer.hasMoreTokens()) {
            artifact.setType(tokenizer.nextToken());
        }
        if (tokenizer.hasMoreTokens()) {
            artifact.setClassifier(tokenizer.nextToken());
        }
    
        return artifact;
    }

    public static URL createURL(URL repository, Artifact artifact) {
        StringBuffer fileName = new StringBuffer();
        fileName.append(artifact.getArtifactId()).append('-').append(artifact.getVersion());
        if (artifact.getClassifier() != null) {
            fileName.append('-').append(artifact.getClassifier());
        }
        fileName.append('.').append(artifact.getType());
        
        return Utils.createURL(repository, artifact, fileName.toString());
    }

    public static URL createURL(URL repository, Artifact artifact, String fileName) {
        try {
            StringBuffer path = new StringBuffer();
            path.append(artifact.getGroupId().replace('.', '/')).append('/').append(artifact.getArtifactId()).append('/').append(artifact.getVersion()).append('/');
            path.append(fileName);
            URL finalURL = URLUtils.add(repository, path.toString());
            return finalURL;
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static File createLocalArtifactFileName(Artifact artifact, String version, File localRepository) {
        StringBuffer fileName = new StringBuffer();
        fileName.append(artifact.getArtifactId()).append('-').append(version);
        if (artifact.getClassifier() != null) {
            fileName.append('-').append(artifact.getClassifier());
        }
        fileName.append('.').append(artifact.getType());
    
        return Utils.createLocalFileName(artifact, fileName.toString(), localRepository);
    }

    public static File createLocalFileName(Artifact artifact, String name, File localRepository) {
        StringBuffer fileName = new StringBuffer();
        fileName.append(artifact.getGroupId().replace('.', ParallelRepositoryModuleLoader.separator)).append(ParallelRepositoryModuleLoader.separator);
        fileName.append(artifact.getArtifactId()).append(ParallelRepositoryModuleLoader.separator);
        fileName.append(artifact.getVersion()).append(ParallelRepositoryModuleLoader.separator);
        fileName.append(name);
    
        File localFile = new File(localRepository, fileName.toString());
        return localFile;
    }

    public static void collectProperties(Map<String, String> properties, POM pom) {
        Map<String, String> p = pom.getProperties();
        if (p != null) {
            for (Map.Entry<String, String> entry : p.entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        POM parent = pom.getParentPOM();
        if (parent != null) {
            collectProperties(properties, parent);
        }
    }

    public static void updateDependency(POM pom, Dependency dependency) {
        Dependency managedDependency = pom.findManagedDependency(dependency.getGroupId(), dependency.getArtifactId());
        if (managedDependency != null) {
            if (dependency.getVersion() == null) {
                dependency.setVersion(managedDependency.getVersion());
            }
            if (dependency.getScope() == null) {
                dependency.setScope(managedDependency.getScope());
            }
            if (dependency.getType() == null) {
                dependency.setType(managedDependency.getType());
            }
            if (dependency.getClassifier() == null) {
                dependency.setClassifier(managedDependency.getClassifier());
            }
            if (managedDependency.getExclusions() != null) {
                if (dependency.getExclusions() != null) {
                    Exclusions exclusions = dependency.getExclusions();
                    exclusions.getExclusions().addAll(managedDependency.getExclusions().getExclusions());
                } else {
                    Exclusions exclusions = dependency.addExclusions();
                    exclusions.getExclusions().addAll(managedDependency.getExclusions().getExclusions());
                }
            }
        } else {
            if (dependency.getVersion() == null) {
                throw new RuntimeException("No defined managed dependency for " + dependency.getGroupId() + ":" + dependency.getArtifactId());
            }
        }
    }

    public static Map<String, RepositoryDefinition> updateRepositories(POM pom, Map<String, RepositoryDefinition> repositories, boolean overrideAllowed) {
        boolean old = true;
        while (pom != null) {
            Repositories rreps = pom.getRepositories();
            if (rreps != null) {
                List<Repository> reps = rreps.getRepositories();
                if (reps != null) {
                    for (Repository repository : reps) {
                        if (!repositories.containsKey(repository.getId())) {
                            if (old) {
                                Map<String, RepositoryDefinition> newRepositories = new HashMap<String, RepositoryDefinition>();
                                newRepositories.putAll(repositories);
                                repositories = newRepositories;
                                old = false;
                            }
                            if (overrideAllowed || !repositories.containsKey(repository.getId())) {
                                try {
                                    repositories.put(repository.getId(), 
                                            new RepositoryDefinition(repository.getId(),
                                                    new URL(repository.getUrl()),
                                                    repository.getReleases().isEnabled(),
                                                    repository.getSnapshots().isEnabled()));
                                } catch (MalformedURLException e) {
                                    Extend.info.warn("Cannot add repository id=" + repository.getId() + " url='" + repository.getUrl() + "'; " , e);
                                }
                            }
                        }
                    }
                }
            }
            pom = pom.getParentPOM();
        }
        
        return repositories;
    }

}
