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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repository.concurent.TaskGroup;
import org.abstracthorizon.extend.repository.maven.pom.Artifact;
import org.abstracthorizon.extend.repository.maven.pom.POM;
import org.abstracthorizon.extend.repository.maven.pom.RepositoryDefinition;
import org.abstracthorizon.extend.repository.maven.pom.Settings;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.service.AbstractServiceModuleLoader;
import org.abstracthorizon.extend.server.deployment.support.ProvisionalModule;

/**
 * Module loader that loads files from maven style repository. This loader
 * will try to parallelise downloads as much as possible.
 *
 * @author Daniel Sendula
 */
public class ParallelRepositoryModuleLoader extends AbstractServiceModuleLoader implements RepositoryLoader {

    /** Default buffer size - 10K */
    public static final int DEFAULT_BUFFER_SIZE = 10240;

    /** POM cache */
    protected Map<String, POM> pomCache = new HashMap<String, POM>();

    /** Repositories */
    protected Map<String, RepositoryDefinition> repositories = new HashMap<String, RepositoryDefinition>();

    /** Download buffer size */
    protected int bufferSize = DEFAULT_BUFFER_SIZE;

    /** Local path separator char */
    static char separator = File.separatorChar;

    /** Settings */
    private Settings settings = new Settings();
    
    /** Settings file */
    private File settingsFile = new File(System.getProperty("user.home"), ".m2/settings.xml");
    
    /** Local repository */
    private File localRepositoryFile = new File(System.getProperty("user.home"), ".m2/repository");

    /** Cache of failed URLs. Map points to timestamp when failure happend so it can timeout */
    protected Map<URL, Long> failedURLS = new HashMap<URL, Long>();

    /** Cache of succedded URLs. Map points to timestamp when failure happend so it can timeout */
    private Map<URL, Long> succeddedURLS = new HashMap<URL, Long>();
    
    /** When failed URLs should timeout - another try is allowed. */
    private long failedTimeout = 1000*60*2; // 2 minutes
    
    /** URL read timeout */
    private int readTimeout = 2000; // 30 seconds
    
    /** URL connect timeout */
    private int connectTimeout = 2000; // 30 seconds
    
    /** Should snapshot artifacts be checked for the latest or not */
    private boolean checkSnapshotVersions = true;
    
    /** Executor to be used with load and deploy. */
    private ExecutorService loadModuleExecutor;
    
    /** Executor to be used for downloading. */
    private ExecutorService downloadExecutor;
    
    /** Download tasks manager. */
    protected DownloadTasks downloadTasks;

    /** LoadModules tasks manager */
    protected LoadModuleTasks loadModuleTasks;
    
    /** Should repository definitions from poms override already defined. */
    private boolean allowRepositoryOverride = true;

    /**
     * Constructor
     */
    public ParallelRepositoryModuleLoader() {
    }

    /**
     * Sets repositories
     * @param repositories repositories
     */
    public void setRepositories(Map<String, RepositoryDefinition> repositories) {
        this.repositories = repositories;
    }

    /**
     * Returns repositories
     * @return repositories
     */
    public Map<String, RepositoryDefinition> getRepositories() {
        return repositories;
    }

    /**
     * Adds repository.
     * @param id repository id
     * @param url url
     * @param releases releases enabled
     * @param shapshots snapshots enabled
     */
    public void addRepository(String id, URL url, boolean releases, boolean snapshots) {
        RepositoryDefinition def = new RepositoryDefinition(id, url, releases, snapshots);
        getRepositories().put(id, def);
    }
    
    /**
     * Sets settings file.
     * @param settingsFile settings file
     */
    public void setSettingsFile(File settingsFile) {
        this.settingsFile = settingsFile;
    }
    
    /**
     * Return settings file.
     * @return settings file
     */
    public File getSettingsFile() {
        return settingsFile;
    }
    
    /**
     * Sets when failed URLs cache entries could be removed (URLs re-tried).
     * @param failedTimeout timeout
     */
    public void setFailedTimeout(long failedTimeout) {
        this.failedTimeout = failedTimeout;
    }
    
    /**
     * Returns when failed URLs cache entries could be removed (URLs re-tried).
     * @return timeout
     */
    public long getFailedTimeout() {
        return failedTimeout;
    }
    
    /**
     * Returns failed urls map.
     * @return failed urls map
     */
    public Map<URL, Long> getFailedURLs() {
        return failedURLS;
    }
    
    /**
     * Returns succedded urls map.
     * @return succedded urls map
     */
    public Map<URL, Long> getSucceddedURLs() {
        return succeddedURLS;
    }
    
    /**
     * Sets URL read timeout.
     * @param readTimeout read timeout
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    /**
     * Returns URL read timeout.
     * @return readTimeout read timeout
     */
    public int getReadTimeout() {
        return readTimeout;
    }
    
    /**
     * Sets URL connect timeout.
     * @param connectTimeout connect timeout
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    /**
     * Returns URL connect timeout.
     * @return connectTimeout connect timeout
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }
    
    /**
     * Should artifact with snapshot version be checked afainst repositories latest or
     * only local to be used (if exsits).
     * @return checkSnapshotVersions
     */
    public boolean isCheckSnapshotVersions() {
        return checkSnapshotVersions;
    }
    
    /**
     * Should artifact with snapshot version be checked afainst repositories latest or
     * only local to be used (if exsits).     
     * @param checkSnapshotVersions
     */
    public void setCheckSnapshotVersions(boolean checkSnapshotVersions) {
        this.checkSnapshotVersions = checkSnapshotVersions;
    }
    
    /**
     * Returns local repository directory.
     * @return local repository directory
     */
    protected File getLocalRepositoryFile() {
        return localRepositoryFile;
    }
    
    /**
     * Returns LoadModuleTasks instance.
     * @return LoadModuleTasks instance
     */
    protected LoadModuleTasks getLoadModuleTasks() {
        return loadModuleTasks;
    }
    
    /**
     * Returns DownloadTasks instance.
     * @return DownloadTasks instance
     */
    protected DownloadTasks getDownloadTasks() {
        return downloadTasks;
    }
    
    /**
     * POM Cache
     * @return POM cache
     */
    protected Map<String, POM> getPOMCache() {
        return pomCache;
    }
    
    /**
     * Should snapshot artifacts be checked for the latest or not. 
     * @return flag
     */
    public boolean isAllowRepositoryOverride() {
        return allowRepositoryOverride;
    }
    
    /**
     * Set should snapshot artifacts be checked for the latest or not.
     * Default is <code>true<code>.
     * 
     * @param allowRepositoryOverride flag
     */
    public void setAllowRepositoryOverride(boolean allowRepositoryOverride) {
        this.allowRepositoryOverride = allowRepositoryOverride;
    }
    
    /**
     * Method that sets up this object.
     */
    public void start() {
        super.start();

        ThreadFactory threadFactory = null;
        
        if ((loadModuleExecutor == null) || (downloadExecutor == null)) {
            threadFactory = new ThreadFactory() {
    
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    return thread;
                }
                
            };
        }        
        int min = 2;
        int max = 48;

        if (loadModuleExecutor == null) {
            loadModuleExecutor = new ThreadPoolExecutor(min, max, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
        }
        if (downloadExecutor == null) {
            downloadExecutor = new ThreadPoolExecutor(min, max, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
        }
        
        downloadTasks = new DownloadTasks(this, downloadExecutor);
        
        loadModuleTasks = new LoadModuleTasks(this, loadModuleExecutor);
        if (getRepositories().isEmpty()) {
            // Define initial values
            try {
                // TODO setting up urls lasts too long. Why?
                addRepository("central", new URL("http://repo1.maven.org/maven2"), true, false);
                addRepository("abstracthorizon", new URL("http://repository.abstracthorizon.org/maven2/abstracthorizon"), true, false);
                addRepository("abstracthorizon.snapshot", new URL("http://repository.abstracthorizon.org/maven2/abstracthorizon.snapshot"), false, true);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        if ((settingsFile != null) && settingsFile.exists()) {
            XMLProcessor processor = new XMLProcessor(settingsFile);
            processor.setStartObject(settings);
            try {
                processor.process();
                
                if (settings.getLocalRepository() != null) {
                    localRepositoryFile = new File(settings.getLocalRepository());
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        Extend.info.debug("Using following repositories:");
        for (Map.Entry<String, RepositoryDefinition> e : repositories.entrySet()) {
            StringBuilder s = new StringBuilder();
            s.append("    ").append(e.getKey()).append(" as ").append(e.getValue().getURL());
            if (e.getValue().isReleasesEnabled() || e.getValue().isSnapshotsEnabled()) {
                s.append(" for ");
                if (e.getValue().isReleasesEnabled()) {
                    s.append("releases");
                    if (e.getValue().isSnapshotsEnabled()) {
                        s.append(" and snapshots.");
                    }
                } else if (e.getValue().isSnapshotsEnabled()) {
                    s.append("snapshots.");
                }
            } else {
                s.append(" - ERROR! No releases or snapshots defined for repository!");
            }
            Extend.info.debug(s.toString());
        }
        if (!localRepositoryFile.exists()) {
            if (!localRepositoryFile.mkdirs()) {
                throw new RuntimeException("Cannot create local repository " + localRepositoryFile);
            }
        }
        Extend.info.debug("Using local cache at " + localRepositoryFile.getAbsolutePath());
        
    }
    
    /**
     * Stop method removes URLResolver from &quot;DefaultURLResolver&quot;.
     */
    public void stop() {
        super.stop();

        pomCache.clear();
    }

    public void destroy() {
    }
    
    /**
     * This method checks if URI protocol is &quot;repo&quot; and file starts with &quot;maven&quot;. Also
     * rest of the file must have at least two &quot;:&quot; for group id, artifact id and version. Type
     * is assumed as jar and classifier can be empty.
     * @param uri URI
     * @return <code>true</code> if URI protocol is &quot;repo&quot; and file starts with &quot;maven&quot;.
     */
    public boolean canLoad(URI uri) {
        String f = uri.getSchemeSpecificPart();
        String scheme = uri.getScheme();
        if ((scheme != null) && scheme.equals("repo") && (f != null) && f.startsWith("maven:")) {
            f = f.substring(6);
            int i = f.indexOf(':');
            if (i > 0) {
                i = f.indexOf(':', i + 1);
                if (i > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public ModuleId toModuleId(URI uri) {
        if (canLoad(uri)) {
            String file = uri.getSchemeSpecificPart();
            if (file != null) {
                Artifact artifact = Utils.parseArtifact(file.substring(6));
                return artifact;
            }
        }
        return null;
    }
    
    /**
     * Loads a file from maven style repository
     * @param uri URI
     */
    public Module load(URI uri) {
        String file = uri.getSchemeSpecificPart();
        if (file != null) {
            Artifact artifact = Utils.parseArtifact(file.substring(6));
            return loadAs(uri, artifact);
        } else {
            return null;
        }
    }

    /**
     * Loads a file from maven style repository
     * @param uri URI
     * @param moduleId module id
     */
    public Module loadAs(URI uri, ModuleId moduleId) {
        Extend.info.debug("Loading module " + uri + " as " + moduleId);
        
        Artifact originalArtifact = null;
        if (!(moduleId instanceof Artifact)) {
            originalArtifact = new Artifact(moduleId);
        } else {
            originalArtifact = (Artifact)moduleId;
        }
        
        TaskGroup<Module, LoadModuleTaskDefinitions> taskGroup = loadModuleTasks.newTaskGroup();

        Stack<Artifact> stack = new Stack<Artifact>();
        stack.add(originalArtifact);

        
        
        Artifact finalArtifact = new Artifact(originalArtifact.toNonSnapshotArtifact());
        
        Module m = getDeploymentManager().getDeployedModules().get(finalArtifact);
        if (m instanceof ProvisionalModule) {
            m = null;
        }
        if ((m == null) && (finalArtifact.getType() == null)) {
            POM dependencyPom = getPOMCache().get(finalArtifact.getShortId());
            if (dependencyPom != null) {
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(originalArtifact.getFullId() + ": type is null but found POM, so setting type from the POM (start)."); }
                
                finalArtifact.setType(dependencyPom.getPackaging());
                if (finalArtifact.getType() == null) {
                    if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(originalArtifact.getFullId() + ": POM had no packaging - setting default 'jar' (start)."); }
                    finalArtifact.setType("jar");
                }
                m = getDeploymentManager().getDeployedModules().get(finalArtifact);
            }
        }
        if (m != null) {
            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(originalArtifact.getFullId() + ": found module at the start."); }
            return m;
        }
        
        
        
        LoadModuleTask mainTask = (LoadModuleTask)loadModuleTasks.submitNewTask(taskGroup,
                new LoadModuleTaskDefinitions(finalArtifact, new HashSet<Artifact>(), false, getRepositories(), stack,
                taskGroup));

        ArrayList<LoadModuleTask> failedTasks = new ArrayList<LoadModuleTask>();
        
        // boolean mainDone = false;
        // boolean more = true;
        
        synchronized (taskGroup) {
            while (taskGroup.hasMore()) {
                try {
                    if (Extend.debug.isDebugEnabled()) {
                        Extend.debug.debug("Waiting on pendng  " + taskGroup.getPending().values()); 
                        Extend.debug.debug("    and on waiting " + mainTask.getWaitingOn());
                    }
                    
                    LoadModuleTask task = (LoadModuleTask)taskGroup.takeFinished();
                    if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(task.getDefinitions().getArtifact() + ": finished. Processing..."); }
                    // if (task == mainTask) {
                    //    mainDone = true;
                    // }
                    if ((task.getResult() == null) && !task.getDefinitions().isOptional()) {
                        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(task.getDefinitions().getArtifact() + ": finished. Failed - added to list of failed tasks."); }
                        failedTasks.add(task);
                    } else {
                        updateParents(task);
                    }
                
                } catch (InterruptedException e) {
                    // TODO - this should never ever happen!
                    throw new RuntimeException(e);
                }
                
                // more = taskGroup.hasMore();
                
//                synchronized (mainTask) {
//                    more = !mainDone || !mainTask.getWaitingOn().isEmpty() || !taskGroup.hasMore();
//                }
//
//                if (!taskGroup.hasMore()) {
//                    more = false;
//                }
            }
        }
        
        loadModuleTasks.releaseGroup(taskGroup);
        
        if (!failedTasks.isEmpty()) {
            StringWriter msg = new StringWriter();
            PrintWriter out = new PrintWriter(msg);
            for (LoadModuleTask task : failedTasks) {
                out.print("Failed to load ");
                out.print(task.getDefinitions().getArtifact());
                if (task.getException() != null) {
                    out.println(": ");
                    task.getException().printStackTrace(out);
                }
                out.println(Utils.stackToString(task.getDefinitions().getStack()));
            }
            out.flush();
            throw new RuntimeException(msg.toString());
        } else if (!mainTask.getWaitingOn().isEmpty()) {
            throw new RuntimeException("Failed to load due to errors in " + mainTask.getWaitingOn());
        }
        
        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(mainTask.getDefinitions().getArtifact() + ": Updating status of main task..."); }
        Module module = mainTask.getResult();

        for (Module mod : mainTask.getDependsOn()) {
            if (mod == module) {
                throw new RuntimeException("Cannot make dependency on itself " + mod.getModuleId());
            }
            module.getDependsOn().add(mod);
            mod.getDependOnThis().add(module);
        }
        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(mainTask.getDefinitions().getArtifact() + ": updated."); }
        
        return mainTask.getResult();
    }
    
    protected void updateParents(LoadModuleTask task) {
        if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(task.getDefinitions().getArtifact() + ": Updating status..."); }

        
        Map<LoadModuleTaskDefinitions, LoadModuleTask> taskWaitingOn = task.getWaitingOn();
        Map<LoadModuleTaskDefinitions, LoadModuleTask> taskWaitingOnThis = task.getWaitingOnThis();
        synchronized (task) {
            if (!taskWaitingOnThis.isEmpty()) {
                if (taskWaitingOn.isEmpty()) {
                    Artifact artifact = task.getDefinitions().getFinalArtifact();
                    ModuleId moduleId = new ModuleId(artifact);
                    Module module = task.getResult();

                    for (LoadModuleTask parent : taskWaitingOnThis.values()) {
                        if (module != null) {
                            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(task.getDefinitions().getArtifact() + ": updating dependencies."); }
    
                            for (Module m : task.getDependsOn()) {
                                if (m == module) {
                                    throw new RuntimeException("Cannot make dependency on itself " + m.getModuleId());
                                }
                                module.getDependsOn().add(m);
                                m.getDependOnThis().add(module);
                            }
        
                            deploymentManager.deploy(moduleId, module);
        
                            synchronized (parent) { 
                                parent.getDependsOn().add(module);
                            }
                        } else if (!task.getDefinitions().isOptional()) {
                            if (Extend.debug.isWarnEnabled()) { Extend.debug.warn(task.getDefinitions().getArtifact() + ": No module downloaded - won't update dependencies."); }
                        }
                        loadModuleTasks.complete(task);
                        
                        Map<LoadModuleTaskDefinitions, LoadModuleTask> parentWaitingOn = parent.getWaitingOn();
                        synchronized (parent) {
                            parentWaitingOn.remove(task.getDefinitions());
                            if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(task.getDefinitions().getArtifact() + ": removed 'waiting for' on parent's module " + parent.getDefinitions().getArtifact()); }
                            if (parentWaitingOn.isEmpty()) {
                                updateParents(parent);
                            }
                        }
                    }
                    if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(task.getDefinitions().getArtifact() + ": updated."); }
                } else {
                    if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(task.getDefinitions().getArtifact() + ": still other modules we are waiting for...!"); }
                }
            } else {
                if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(task.getDefinitions().getArtifact() + ": main task (no parent) - ignoring it!"); }
            }
        }
    }
    
}