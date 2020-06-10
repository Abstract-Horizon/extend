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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repository.concurent.AbstractTask;
import org.abstracthorizon.extend.repository.concurent.Task;
import org.abstracthorizon.extend.repository.concurent.TaskGroup;
import org.abstracthorizon.extend.repository.maven.pom.Artifact;
import org.abstracthorizon.extend.repository.maven.pom.Dependencies;
import org.abstracthorizon.extend.repository.maven.pom.Dependency;
import org.abstracthorizon.extend.repository.maven.pom.MavenMetadata;
import org.abstracthorizon.extend.repository.maven.pom.POM;
import org.abstracthorizon.extend.repository.maven.pom.RepositoryDefinition;
import org.abstracthorizon.extend.repository.maven.pom.Snapshot;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.support.AbstractModule;
import org.abstracthorizon.extend.server.deployment.support.ProvisionalModule;

/**
 * Task that handles loading modules.
 *
 * @author Daniel Sendula
 */
public class LoadModuleTask extends AbstractTask<Module, LoadModuleTaskDefinitions> {

    private ParallelRepositoryModuleLoader loader;
    private Map<LoadModuleTaskDefinitions, LoadModuleTask> waitingOn = new HashMap<LoadModuleTaskDefinitions, LoadModuleTask>();
    private Map<LoadModuleTaskDefinitions, LoadModuleTask> waitingOnThis = new HashMap<LoadModuleTaskDefinitions, LoadModuleTask>();
    private Set<Module> dependsOn = new HashSet<Module>();
    private File artifactFile;
    
    public LoadModuleTask(LoadModuleTasks owner, LoadModuleTaskDefinitions definitions, ParallelRepositoryModuleLoader loader) {
        super(owner, definitions);
        this.loader = loader;
    }
    
    public Map<LoadModuleTaskDefinitions, LoadModuleTask> getWaitingOn() {
        return waitingOn;
    }
    
    public Map<LoadModuleTaskDefinitions, LoadModuleTask> getWaitingOnThis() {
        return waitingOnThis;
    }
    
    public Set<Module> getDependsOn() {
        return dependsOn;
    }
    
    public File getArtifactFile() {
        return artifactFile;
    }
    
    public void execute() {    
        if (Extend.debug.isInfoEnabled()) { Extend.debug.info(getDefinitions().getArtifact() + ": loading..."); }

        boolean checkedPom = false;
        POM pom = null;
        Artifact finalArtifact = getDefinitions().getFinalArtifact();
        Artifact snapshotArtifact = getDefinitions().getSnapshotArtifact();
        
        if (finalArtifact.getType() == null) {
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(getDefinitions().getArtifact() + ": type is null - obtaning POM..."); }
            pom = obtainPOM(finalArtifact.toPOMArtifact(), getDefinitions().getRepositories(), getDefinitions().getStack());
            checkedPom = true;
            if (pom != null) {
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(getDefinitions().getArtifact() + ": got POM. New type is " + pom.getPackaging()); }
                finalArtifact.setType(pom.getPackaging());
                snapshotArtifact.setType(finalArtifact.getType());
            }
            if (finalArtifact.getType() == null) {
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(getDefinitions().getArtifact() + ": type is still null setting it to 'jar'"); }
                finalArtifact.setType("jar");
                snapshotArtifact.setType("jar");
            }
        }
        
        // Just in case...
        Module m = loader.getDeploymentManager().getDeployedModules().get(finalArtifact);
        if ((m != null) && !(m instanceof ProvisionalModule)) {
            setResult(m);
            return;
        }
        
        
        File file = obtainFile(finalArtifact, snapshotArtifact, false);
        if (file != null) {
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(getDefinitions().getArtifact() + ": got file " + file.getAbsolutePath() + ". Loading module..."); }
            loadModule(file);
            
            if (getResult() != null) {
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(getDefinitions().getArtifact() + ": loaded it as module."); }
                if ((pom == null) && !checkedPom) {
                    if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(getDefinitions().getArtifact() + ": obtaining POM..."); }
                    pom = obtainPOM(finalArtifact.toPOMArtifact(), getDefinitions().getRepositories(), getDefinitions().getStack());
                }
    
                if (pom != null) {
                    if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(getDefinitions().getArtifact() + ": processing POM dependencies..."); }
                    processDependencies(pom, getResult(), getDefinitions().getExcludes(), getDefinitions().getRepositories(), getDefinitions().getStack());
                }
            } else {
                if (Extend.debug.isWarnEnabled()) { Extend.debug.warn(getDefinitions().getArtifact() + ": Couldn't load module from file " + file.getAbsolutePath()); }
            }
        } else if (!getDefinitions().isOptional()) {
            if (Extend.debug.isWarnEnabled()) { Extend.debug.warn(getDefinitions().getArtifact() + ": cannot get file."); }
        } else {
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(getDefinitions().getArtifact() + ": cannot get file."); }
        }

        if (Extend.debug.isInfoEnabled()) { Extend.debug.info(getDefinitions().getArtifact() + ": finished."); }
    }

    protected POM obtainPOM(Artifact pomArtifact, Map<String, RepositoryDefinition> repositories, Stack<Artifact> stack) {
        if (pomArtifact.getType() == null) {
            pomArtifact.setType("pom");
        }
        
        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(pomArtifact + ": Loading... "); }

        String pomId = pomArtifact.getShortId();

        POM pom = loader.getPOMCache().get(pomId);
        if (pom == null) {
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(pomArtifact + ": POM not in cache. Loading... "); }
            Artifact pomSnapshotArtifact = pomArtifact.toSnapshotArtifact();
            File pomFile = obtainFile(pomArtifact, pomSnapshotArtifact, true);
            if (pomFile == null) {
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(pomArtifact + ": POM not available."); }
                return null;
            }

            pom = new POM();

            try {
                XMLProcessor processor = new XMLProcessor(pomFile);
                processor.setStartObject(pom);
                processor.process();
            } catch (Exception e) {
                Extend.debug.warn("Caught exception trying to obtain pom " + pomArtifact + " @ " + Utils.stackToString(stack), e);
                return null;
            }

            if (pom.getParent() != null) {
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(pomArtifact + ": Has parent. Obtaining parent POM..."); }
                stack.push(pom);
                POM parentPom = obtainPOM(pom.getParent(), getDefinitions().getRepositories(), stack);
                stack.pop();
                pom.setParentPOM(parentPom);
            }

            HashMap<String, String> properties = new HashMap<String, String>();
            Utils.collectProperties(properties, pom);
            if (pom.getVersion() != null) {
                properties.put("pom.version", pom.getVersion());
                properties.put("project.version", pom.getVersion());
            } else {
                properties.put("pom.version", pomArtifact.getVersion());
                properties.put("project.version", pomArtifact.getVersion());
            }
            if (pom.getGroupId() != null) {
                properties.put("pom.groupId", pom.getGroupId());
                properties.put("project.groupId", pom.getGroupId());
            } else {
                properties.put("pom.groupId", pomArtifact.getGroupId());
                properties.put("project.groupId", pomArtifact.getGroupId());
            }
            if (pom.getArtifactId() != null) {
                properties.put("pom.artifactId", pom.getArtifactId());
                properties.put("project.artifactId", pom.getArtifactId());
            } else {
                properties.put("pom.artifactId", pomArtifact.getArtifactId());
                properties.put("project.artifactId", pomArtifact.getArtifactId());
            }
            SubstitutionTraverser.substitute(pom, properties);

            loader.getPOMCache().put(pomId, pom);
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(pomArtifact + ": POM loaded. Storing it in cache."); }
        } else {
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(pomArtifact + ": POM in cache."); }
        }
        return pom;
    }

    public void processDependencies(POM pom, Module module, Set<Artifact> excludes, Map<String, RepositoryDefinition> repositories, Stack<Artifact> stack) {
        if (Extend.debug.isInfoEnabled()) { Extend.debug.info(pom.getFullId() + ": processing dependencies..."); }
        
        repositories = Utils.updateRepositories(pom, repositories, loader.isAllowRepositoryOverride());
        
        Dependencies dependencies = pom.getDependencies();
        if (dependencies != null) {
            
            for (Dependency dependency : dependencies.getDependencies()) {
                if (pom.getParentPOM() != null) {
                    Utils.updateDependency(pom, dependency);
                }

                String scope = dependency.getScope();
                if (((scope == null)
                        || scope.equals("runtime")
                        || scope.equals("compile"))
                    && !Utils.excludesContain(excludes, dependency)) {

                    if (Extend.debug.isInfoEnabled()) {
                        if (scope != null) {
                            Extend.debug.info("    Dependency " + dependency.getFullId() + " as " + scope);
                        } else {
                            Extend.debug.info("    Dependency " + dependency.getFullId() + " (no defined scope)");
                        }
                    }

                    Set<Artifact> innerExcludes = excludes;
                    if (dependency.getExclusions() != null) {
                        innerExcludes = new HashSet<Artifact>();
                        innerExcludes.addAll(excludes);
                        innerExcludes.addAll(dependency.getExclusions().getExclusions());
                    }

                    Artifact dependencyArtifact = new Artifact(dependency);
                    Artifact finalDependencyArtifact = new Artifact(dependencyArtifact.toNonSnapshotArtifact());
                    
                    
                    Module m = loader.getDeploymentManager().getDeployedModules().get(finalDependencyArtifact);
                    if (m instanceof ProvisionalModule) {
                        m = null;
                    }
                    if ((m == null) && (finalDependencyArtifact.getType() == null)) {
                        POM dependencyPom = loader.getPOMCache().get(finalDependencyArtifact.getShortId());
                        if (dependencyPom != null) {
                            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(dependency.getFullId() + ": type is null but found POM, so setting type from the POM."); }
                            
                            finalDependencyArtifact.setType(dependencyPom.getPackaging());
                            if (finalDependencyArtifact.getType() == null) {
                                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(dependency.getFullId() + ": POM had no packaging - setting default 'jar'."); }
                                finalDependencyArtifact.setType("jar");
                            }
                            m = loader.getDeploymentManager().getDeployedModules().get(finalDependencyArtifact);
                        }
                    }
                    if (m != null) {
                        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(dependency.getFullId() + ": found module. Id=" + finalDependencyArtifact); }
                        synchronized (this) {
                            getDependsOn().add(m);
                        }
                    } else {
                        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(dependency.getFullId() + ": module not found. Adding new task. Id=" + finalDependencyArtifact); }
                        stack.push(pom);
                        try {
                            synchronized (this) {
                                LoadModuleTaskDefinitions definitions = new LoadModuleTaskDefinitions(dependencyArtifact, innerExcludes, dependency.isOptional(), repositories, stack, getDefinitions().getGroup());
                                LoadModuleTask task = (LoadModuleTask)getOwner().submitNewTask(getDefinitions().getGroup(), definitions);
                                waitingOn.put(task.getDefinitions(), task);
                                synchronized (task) {
                                    task.getWaitingOnThis().put(this.getDefinitions(), this);
                                }
                            }
                        } finally {
                            stack.pop();
                        }
                    }
                }
            }
        }
        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(pom.getFullId() + ": processing dependencies done."); }
    }

    protected void loadModule(File file) {
        URI fileURI = file.toURI();
        Artifact finalArtifact = getDefinitions().getFinalArtifact();
        if (loader.getDeploymentManager().canLoad(fileURI)) {
            Module module = loader.getDeploymentManager().loadAs(fileURI, finalArtifact);
            setResult(module);
            if (module instanceof AbstractModule) {
                AbstractModule abstractModule = (AbstractModule)module;
                abstractModule.setModuleId(finalArtifact);
            }
        } else {
            Module module = new ProvisionalModule(fileURI, finalArtifact);
            setResult(module);
        }
    }
    

    protected File obtainFile(Artifact finalArtifact, Artifact snapshotArtifact, boolean pom) {
        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(finalArtifact + ": obtainging file... (snapshot fallback to " + snapshotArtifact + ")"); }
        File finalFile = Utils.createLocalArtifactFileName(finalArtifact, finalArtifact.getVersion(), loader.getLocalRepositoryFile());
        if (finalFile.exists()) {
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(finalArtifact + ": final file exists: " + finalFile.getAbsolutePath()); }
            return finalFile;
        }
        
        File snapshotFile = Utils.createLocalArtifactFileName(snapshotArtifact, snapshotArtifact.getVersion(), loader.getLocalRepositoryFile());
        if (snapshotFile.exists() && !loader.isCheckSnapshotVersions()) {
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(snapshotArtifact + ": snapshot file exists & quick : " + snapshotFile.getAbsolutePath()); }
            // This means that once someone does "mvn install" original snapshot will 
            // take precendence over any other possible snapshots on the server...
            return snapshotFile;
        }
        
        if (pom) {
            File skipPOMFile = new File(finalFile.getParent(), finalFile.getName() + ".skip");
            if (skipPOMFile.exists()) {
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(finalArtifact + ": it's POM and have flag to skip POM set " + skipPOMFile.getAbsolutePath()); }
                return null;
            }
        }
        
        ArrayList<Task<File, DownloadTaskDefinitions>> resultSnapshots = new ArrayList<Task<File, DownloadTaskDefinitions>>();
        
        TaskGroup<File, DownloadTaskDefinitions> taskGroup = loader.getDownloadTasks().newTaskGroup();

        File localSnapshotMetadataFile = Utils.createLocalFileName(snapshotArtifact, "maven-metadata-local.xml", loader.getLocalRepositoryFile());
        if (localSnapshotMetadataFile.exists()) {
            // TODO this must be successful!!!
            Task<File, DownloadTaskDefinitions> localTask = loader.getDownloadTasks().submitNewTask(taskGroup, 
                    new DownloadTaskDefinitions(loader, null, true, localSnapshotMetadataFile, getDefinitions().getStack()));
            ((AbstractTask<File, DownloadTaskDefinitions>)localTask).setResult(localSnapshotMetadataFile);
            resultSnapshots.add(localTask);
        }
            
        

        try {
            
            for (Map.Entry<String, RepositoryDefinition> entry : getDefinitions().getRepositories().entrySet()) {
                String id = entry.getKey();
                RepositoryDefinition repository = entry.getValue();
                if (repository.isReleasesEnabled()) {
                    URL url = Utils.createURL(repository.getURL(), finalArtifact);
                    
                    if (Extend.transport.isDebugEnabled()) { Extend.transport.debug(finalArtifact + ": adding download for " + url + " to " + finalFile); }

                    loader.getDownloadTasks().submitNewTask(taskGroup, new DownloadTaskDefinitions(loader, url, false, finalFile, getDefinitions().getStack()));
                }

                if (repository.isSnapshotsEnabled()) {
                    File metaData = Utils.createLocalFileName(snapshotArtifact, "maven-metadata-" + id + ".xml", loader.getLocalRepositoryFile());
                    URL url = Utils.createURL(repository.getURL(), snapshotArtifact, "maven-metadata.xml");

                    if (Extend.transport.isDebugEnabled()) { Extend.transport.debug(snapshotArtifact + ": adding download for " + url + " to " + metaData); }
                    loader.getDownloadTasks().submitNewTask(taskGroup, new DownloadTaskDefinitions(loader, url, true, metaData, getDefinitions().getStack()));
                }
            }

            while (taskGroup.hasMore()) {
                Task<File, DownloadTaskDefinitions> task = taskGroup.takeFinished();

                File file = task.getResult();
                if (Extend.transport.isDebugEnabled()) { Extend.transport.debug(finalArtifact + ": got download result for " + task.getDefinitions().getURL() + "; file=" + file); }

                if (file != null) {
                    if (!task.getDefinitions().isSnapshot()) {
                        if (Extend.debug.isInfoEnabled()) { Extend.debug.info(finalArtifact + ": it is final artifact so stopping everything else."); }
                        
                        // Final file. We need to stop all other tasks as we have found
                        // what we were looking for!
                        taskGroup.cancelAll();
                        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(finalArtifact + ": ... continuing download of final artifact."); }
                        return ((DownloadTask)task).continueDownload();
                    } else {
                        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(finalArtifact + ": ... got snapshot."); }
                        resultSnapshots.add(task);
                    }
                } else if (task.getDefinitions().isSnapshot()) {
                    // Result is null and requested file is not there.
                    // So, let's remove destination file (if exists)
                    File destinationFile = task.getDefinitions().getDestinationFile(); 
                    
                    if (destinationFile.exists() && !destinationFile.getName().endsWith("-local.xml")) {
                        if (!destinationFile.delete()) {
                            Extend.info.warn("Cannot delete " + destinationFile.getAbsolutePath() + " @ " + Utils.stackToString(getDefinitions().getStack()));
                        } else {
                            Extend.debug.debug("Failed to download file so deleting failed snapshot file: " + destinationFile.getAbsolutePath());
                        }
                    }
//                } else {
//                    URL url = task.getDefinitions().getURL();
//                    if ((url != null) && !loader.getFailedURLs().containsKey(url)) {
//                        loader.getFailedURLs().put(task.getDefinitions().getURL(), System.currentTimeMillis());
//                    }
                }
            }
            loader.getDownloadTasks().releaseGroup(taskGroup);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        // So - here we have all snapshots downloaded (or not) and local snapshot added.
        // Let's find which one is the latest!
        if (!resultSnapshots.isEmpty()) {
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(snapshotArtifact + ": snapshot metadata downloaded."); }

            String version = snapshotArtifact.getVersion();
            String snapshotVersion = null;

            boolean localCopy = false;
            long latest = 0;
            Task<File, DownloadTaskDefinitions> selectedTask = null;
            
            for (Task<File, DownloadTaskDefinitions> task : resultSnapshots) {
                
                MavenMetadata mavenMetadata = new MavenMetadata();
                try {
                    XMLProcessor processor = new XMLProcessor(task.getDefinitions().getDestinationFile());
                    processor.setStartObject(mavenMetadata);
                    processor.process();
                } catch (Exception e3) {
                    throw new RuntimeException(e3);
                }
                
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(snapshotArtifact + ": processing " + task.getDefinitions().getDestinationFile()); }

                if (mavenMetadata.getVersioning() != null) {
                   String lastUpdated = mavenMetadata.getVersioning().getLastUpdated();
                   
                   try {
                       long l = Long.parseLong(lastUpdated);
                       if (l > latest) {
                           Snapshot snapshot = mavenMetadata.getVersioning().getSnapshot();
                           if (snapshot != null) {
                               if (snapshot.isLocalCopy() || task.getDefinitions().getDestinationFile().getName().endsWith("-local.xml")) {
                                   if (snapshotFile.exists()) {
                                       latest = l;
                                       localCopy = true;

                                       if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(snapshotArtifact + ": selected local copy"); }
                                   } else {
                                       if (Extend.debug.isWarnEnabled()) { Extend.debug.warn(snapshotArtifact + ": local copy but snapshot doesn't exist! file=" + snapshotFile); }
                                   }
                               } else if ((snapshot.getBuildNumber() != null)
                                               && (snapshot.getTimestamp() != null)) {

                                   snapshotVersion = version.substring(0, version.length() - 8) + snapshot.getTimestamp() + "-" + snapshot.getBuildNumber();
                                   latest = l;
                                   localCopy = false;
                                   selectedTask = task;
                                   if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(snapshotArtifact + ": selected " + selectedTask.getDefinitions().getURL() + " as " + snapshotVersion); }
                               } else {
                                   if (Extend.debug.isWarnEnabled()) { Extend.debug.warn(snapshotArtifact + ": Strange. Not a local copy and doesn't have build number & timestamp! Will try just SNAPSHOT"); }
                                   latest = l;
                                   snapshotVersion = version;
                                   localCopy = false;
                                   selectedTask = task;
                              }
                           
                           }
                       } else {
                           if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(snapshotArtifact + ": older artifact; " + l + "<=" + latest); }
                       }
                   } catch (NumberFormatException ignore) {
                       if (Extend.debug.isWarnEnabled()) { Extend.debug.warn(snapshotArtifact + ": bad number '" + lastUpdated + "'", ignore); }
                   }
                } else {
                    if (Extend.debug.isWarnEnabled()) { Extend.debug.warn(snapshotArtifact + ": doesn't contain versioning " + task.getDefinitions().getDestinationFile()); }
                }
            }
            
            if (localCopy) {
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(snapshotArtifact + ": local copy - file=" + snapshotFile.getAbsolutePath()); }
                return snapshotFile;
            }
            
            if (snapshotVersion != null) {
                String newFileName = snapshotArtifact.getArtifactId() + "-" + snapshotVersion + "." + snapshotArtifact.getType();
                File file = Utils.createLocalFileName(snapshotArtifact, newFileName, loader.getLocalRepositoryFile());
                if (!file.exists()) {

                    // Download file
                    try {
                        URI uri = selectedTask.getDefinitions().getURL().toURI();
                        uri = uri.resolve(newFileName);
                        
                        URL url = uri.toURL();
                        
                        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(snapshotArtifact + ": file doesn't already exist. Downloading" + url + " ; file=" + file.getAbsolutePath()); }

                        Task<File, DownloadTaskDefinitions> task = loader.getDownloadTasks().executeNewTask(new DownloadTaskDefinitions(loader, url, true, file, getDefinitions().getStack()));

                        return task.getResult();
                    } catch (URISyntaxException e) {
                        Extend.info.warn("Exception while trying to convert URL to URI " + selectedTask.getDefinitions().getURL() + " @ " + Utils.stackToString(getDefinitions().getStack()), e);
                        return null;
                    } catch (IOException e) {
                        Extend.transport.warn("Exception while trying to download  " + newFileName + " @ " + Utils.stackToString(getDefinitions().getStack()), e);
                        return null;
                    }
                }

                if (!snapshotFile.exists() 
                        || (file.length() != snapshotFile.length())
                        || (file.lastModified() != snapshotFile.lastModified())) {

                    if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(snapshotArtifact + ": copying file " + file.getAbsolutePath() + " to " + snapshotFile.getAbsolutePath()); }
                    Utils.copyFile(file, snapshotFile);
                }
                
                return snapshotFile;
             }
        }
        if (snapshotFile.exists()) {
            return snapshotFile;
        }
        return null;
    }

    public String toString() {
        return "LoadModuleTask[" + getDefinitions().getArtifact() + "]";
    }
}
