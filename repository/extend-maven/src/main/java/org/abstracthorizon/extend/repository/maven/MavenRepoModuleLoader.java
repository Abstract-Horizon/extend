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
package org.abstracthorizon.extend.repository.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repository.maven.pom.Artifact;
import org.abstracthorizon.extend.repository.maven.pom.Dependencies;
import org.abstracthorizon.extend.repository.maven.pom.Dependency;
import org.abstracthorizon.extend.repository.maven.pom.MavenMetadata;
import org.abstracthorizon.extend.repository.maven.pom.POM;
import org.abstracthorizon.extend.repository.maven.pom.Repositories;
import org.abstracthorizon.extend.repository.maven.pom.Repository;
import org.abstracthorizon.extend.repository.maven.pom.RepositoryDefinition;
import org.abstracthorizon.extend.repository.maven.pom.Settings;
import org.abstracthorizon.extend.repository.maven.pom.Snapshot;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.service.AbstractServiceModuleLoader;
import org.abstracthorizon.extend.server.deployment.support.AbstractModule;
import org.abstracthorizon.extend.server.deployment.support.ProvisionalModule;
import org.abstracthorizon.extend.server.support.URLUtils;

/**
 * Module loader that loads files from maven style repository
 *
 * @author Daniel Sendula
 */
public class MavenRepoModuleLoader extends AbstractServiceModuleLoader implements RepositoryLoader {

    /** Default buffer size - 10K */
    public static final int DEFAULT_BUFFER_SIZE = 10240;

    /** POM cache */
    protected Map<String, POM> pomCache = new HashMap<String, POM>();

    /** Repositories */
    protected Map<String, RepositoryDefinition> repositories = new HashMap<String, RepositoryDefinition>();

    /** Download buffer size */
    protected int bufferSize = DEFAULT_BUFFER_SIZE;

    /** Settings */
    private Settings settings = new Settings();

    /** Settings file */
    private File settingsFile = new File(System.getProperty("user.home"), ".m2/settings.xml");

    /** Local repository */
    private File localRepository = null;

    /** Cache of failed URLs. Map points to timestamp when failure happend so it can timeout */
    private Map<URL, Long> failedURLS = new HashMap<URL, Long>();

    /** Cache of succedded URLs. Map points to timestamp when failure happend so it can timeout */
    private Map<URL, Long> succeddedURLS = new HashMap<URL, Long>();

    /** When failed URLs should timeout - another try is allowed. */
    private long failedTimeout = 1000*60*2; // 2 minutes

    /** URL read timeout */
    private int readTimeout = 30000; // 30 seconds

    /** URL connect timeout */
    private int connectTimeout = 30000; // 30 seconds

    /** Should snapshot artifacts be checked for the latest or not */
    private boolean checkSnapshotVersions = true;

    /** Should repository definitions from poms override already defined. */
    private boolean allowRepositoryOverride = true;

    /**
     * Constructor
     */
    public MavenRepoModuleLoader() {
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
    @Override
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
    @Override
    public void addRepository(String id, URL url, boolean releases, boolean snapshots) {
        RepositoryDefinition def = new RepositoryDefinition(id, url, releases, snapshots);
        getRepositories().put(id, def);
    }

    /**
     * Sets local repository file.
     * @param localRepository local repository file
     */
    public void setLocalRepository(File localRepository) {
        this.localRepository = localRepository;
    }

    /**
     * Return local repository file.
     * @return local repository file
     */
    public File getLocalRepository() {
        return localRepository;
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
    @Override
    public Map<URL, Long> getFailedURLs() {
        return failedURLS;
    }

    /**
     * Returns succedded urls map.
     * @return succedded urls map
     */
    @Override
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
    @Override
    public boolean isCheckSnapshotVersions() {
        return checkSnapshotVersions;
    }

    /**
     * Should artifact with snapshot version be checked afainst repositories latest or
     * only local to be used (if exsits).
     * @param checkSnapshotVersions
     */
    @Override
    public void setCheckSnapshotVersions(boolean checkSnapshotVersions) {
        this.checkSnapshotVersions = checkSnapshotVersions;
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
    @Override
    public void start() {
        super.start();

        if ((settingsFile != null) && settingsFile.exists()) {
            XMLProcessor processor = new XMLProcessor(settingsFile);
            processor.setStartObject(settings);
            try {
                processor.process();

                if (settings.getLocalRepository() != null) {
                    setLocalRepository(new File(settings.getLocalRepository()));
                    Extend.info.info("Setting local repository to " + getLocalRepository().getAbsolutePath());
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        Extend.info.debug("Using following repositories:");
        for (Map.Entry<String, RepositoryDefinition> e : repositories.entrySet()) {
            Extend.info.debug("    " + e.getKey() + " as " + e.getValue());
        }
        if (getLocalRepository() == null) {
            File localM2Repository = new File(".m2/repository");
            if (localM2Repository.exists()) {
                setLocalRepository(localM2Repository);
            } else {
                setLocalRepository(new File(System.getProperty("user.home"), ".m2/repository"));
            }
            Extend.debug.info("Using default local repository as " + getLocalRepository().getAbsolutePath());
        }
        if (!getLocalRepository().exists()) {
            if (!getLocalRepository().mkdirs()) {
                throw new RuntimeException("Cannot create local repository " + localRepository);
            }
        }

        try {
            // TODO setting up urls lasts too long. Why?
            if (repositories.size() == 0) {
                addRepository("central", new URL("https://repo1.maven.org/maven2"), true, false);
                addRepository("abstracthorizon", new URL("http://repository.abstracthorizon.org/maven2/abstracthorizon"), true, false);
                addRepository("abstracthorizon.snapshot", new URL("http://repository.abstracthorizon.org/maven2/abstracthorizon.snapshot"), false, true);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        for (RepositoryDefinition repo : repositories.values()) {
            Extend.debug.info("  Using repository " + repo.getURL().toString() + " : " + (repo.isReleasesEnabled() ? "releases " : "") + (repo.isSnapshotsEnabled() ? "snapshots " : ""));
        }

        Extend.info.debug("Using local cache at " + localRepository.getAbsolutePath());

    }

    /**
     * Stop method removes URLResolver from &quot;DefaultURLResolver&quot;.
     */
    @Override
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
    @Override
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

    @Override
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
    @Override
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
    @Override
    public Module loadAs(URI uri, ModuleId moduleId) {
        if (!(moduleId instanceof Artifact)) {
            moduleId = new Artifact(moduleId);
        }
        try {
            return loadAndDeploy((Artifact)moduleId, new HashSet<Artifact>(), false, repositories, new Stack<Artifact>());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * Loads and deploys artifact.
     *
     * @param artifact artifact id to be deployed
     * @param excludes list of excludes while deploying artifact
     * @param optional if <code>true</code> this method won't return IOException but <code>null</code> as result
     * @param repositories list of repositories to be used
     * @param stack path to this artifact
     * @return module or <code>null</code> in case of optional and failure to load the module.
     * @throws IOException thrown only if not optional
     */
    public Module loadAndDeploy(Artifact artifact, Set<Artifact> excludes, boolean optional, Map<String, RepositoryDefinition> repositories, Stack<Artifact> stack) throws IOException {
        Artifact originalArtifact = artifact; // Save original reference

        if (artifact.isSnapshot() && checkSnapshotVersions) {
            // If it is snapshot and final version exists - ignore snapshot!
            artifact = artifact.toNonSnapshotArtifact();
            try {
                Module m = loadAndDeployExact(artifact, excludes, repositories, stack);
                deploymentManager.getDeployedModules().put(artifact, m);
                return m;
            } catch (IOException ignore) {
                // If it was snapshot and final version doesn't exist - ignore error!
            }
        }
        try {
            if (artifact.isSnapshot()) {
                artifact = artifact.toNonSnapshotArtifact();
            }
            Module m = loadAndDeployExact(artifact, excludes, repositories, stack);
            return m;
        } catch (IOException e) {
            if (!artifact.isSnapshot()) {
                // Artifact wasn't snapshot - so we will try snapshot now, as maybe we haven't released final version yet...
                artifact = artifact.toSnapshotArtifact();
                try {
                    Module m = loadAndDeployExact(artifact, excludes, repositories, stack);
                    deploymentManager.getDeployedModules().put(originalArtifact, m);
                    return m;
                } catch (IOException e2) {
                    if (optional) {
                        return null;
                    } else {
                        throw e2;
                    }
                }
            } else {
                // Artifact was snapshot and that failed - so we just re-throw exception
                throw e;
            }
        }
    }


    public Module loadAndDeployExact(Artifact artifact, Set<Artifact> excludes, Map<String, RepositoryDefinition> repositories, Stack<Artifact> stack) throws IOException {
        String originalId = artifact.getFullId();
        Artifact originalArtifact = artifact;
        if (artifact instanceof Dependency) {
            // Dependency has ugly to string + it is not place for dependecy class to act as artifact id in deployment
            artifact = new Artifact(artifact);
        }

        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("Loading module " + originalId);
        }

        DeploymentManager deploymentManager = getDeploymentManager();
        Module existing = deploymentManager.getDeployedModules().get(artifact);
        if ((existing != null) && !(existing instanceof ProvisionalModule)) {
            return existing;
        }

        POM pom = null;
        try {
            // Try to load pom
            pom = loadPom(artifact.toPOMArtifact(), repositories, stack);
        } catch (IOException ignore) {
        }

        if (artifact.getType() == null) {
            if (pom != null) {
                artifact.setType(pom.getPackaging());
            }
        }
        if (artifact.getType() == null) {
            artifact.setType("jar");
        }

        Module m = deploymentManager.getDeployedModules().get(artifact);
        if (m != null) {
            // Asked artifact was without type and now us adding type from pom or just setting it to 'jar' yield results
            deploymentManager.getDeployedModules().put(originalArtifact, m);
            Extend.info.info("  Found full spec artifact " + originalArtifact + ".");
            return m;
        }

        File localFile = getArtifactFile(artifact, stack);

        URI localURI = localFile.toURI();

        if (deploymentManager.canLoad(localURI)) {
            Module module = deploymentManager.loadAs(localURI, artifact);
            if (module instanceof AbstractModule) {
                AbstractModule abstractModule = (AbstractModule)module;
                abstractModule.setModuleId(artifact);
            }
            deploymentManager.deploy(artifact, module);

            // TODO check if early deploy is allowed
            // We are deploying local file as repo file as well
            deploymentManager.getDeployedModules().put(originalArtifact, module);

            if (pom != null) {
                String type = pom.getPackaging();
                if ((type == null) || !type.equals("war")) {
                    stack.push(artifact);
                    processDependencies(pom, module, excludes, repositories, stack);
                    stack.pop();
                }
            }
            return module;
        }

        throw new FileNotFoundException("Cannot load module " + originalId);
    }

    public POM loadPom(Artifact pomArtifact, Map<String, RepositoryDefinition> repositories, Stack<Artifact> stack) throws FileNotFoundException {

        if (!"pom".equals(pomArtifact.getType())) {
            if (pomArtifact.getType() == null) {
                pomArtifact = pomArtifact.toPOMArtifact();
            } else {
                // TODO What do we do here?
                throw new RuntimeException("Tried to load " + pomArtifact + " as POM");
            }
        }
        String pomId = pomArtifact.getShortId();

        POM pom = pomCache.get(pomId);
        if (pom == null) {
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Loading pom " + pomId);
            }

            File pomFile = getArtifactFile(pomArtifact, stack);

            pom = new POM();

            try {
                XMLProcessor processor = new XMLProcessor(pomFile);
                processor.setStartObject(pom);
                processor.process();
            } catch (Exception e) {
                // TODO
                throw new RuntimeException(e);
            }

            if (pom.getParent() != null) {
                stack.push(pom);
                POM parentPom = loadPom(pom.getParent(), repositories, stack);
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

            pomCache.put(pomId, pom);
        }
        return pom;
    }

    public void processDependencies(POM pom, Module module, Set<Artifact> excludes, Map<String, RepositoryDefinition> repositories, Stack<Artifact> stack) throws IOException {
        long started = 0;
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("  Processing dependencies for " + module.getModuleId());
            started = System.currentTimeMillis();
        }

        repositories = Utils.updateRepositories(pom, repositories, isAllowRepositoryOverride());

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

                    if (Extend.info.isDebugEnabled()) {
                        Extend.info.debug("    Dependency " + dependency.getFullId() + " as " + scope);
                    }

                    Set<Artifact> innerExcludes = excludes;
                    if (dependency.getExclusions() != null) {
                        innerExcludes = new HashSet<Artifact>();
                        innerExcludes.addAll(excludes);
                        innerExcludes.addAll(dependency.getExclusions().getExclusions());
                    }

                    try {
                        if (dependency.getType() == null) {
                            stack.push(pom);
                            POM dependendPom = loadPom(dependency.toPOMArtifact(), repositories, stack);
                            stack.pop();
                            dependency.setType(dependendPom.getPackaging());
                        }
                    } catch (FileNotFoundException ein) {
                    }
                    if (dependency.getType() == null) {
                        dependency.setType("jar");
                    }

                    Module m = null;
                    stack.push(pom);
                    try {
                        m = loadAndDeploy(dependency, innerExcludes, "true".equalsIgnoreCase(dependency.getOptional()), repositories, stack);
//                    } catch (FileNotFoundException ex) {
//                        if (!"true".equalsIgnoreCase(dependency.getOptional())) {
//                            throw ex;
//                        }
                    } finally {
                        stack.pop();
                    }
                    if (m != null) {
                        module.getDependsOn().add(m);
                        m.getDependOnThis().add(module);
                    }
                }
            }
        }
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("  Processed dependencies for " + module.getModuleId() + "(" + Long.toString(System.currentTimeMillis() - started) + "ms)");
        }
    }


    public static Map<String, URL> updateRepositories(POM pom, Map<String, URL> repositories) throws MalformedURLException {
        boolean old = true;
        while (pom != null) {
            Repositories rreps = pom.getRepositories();
            if (rreps != null) {
                List<Repository> reps = rreps.getRepositories();
                if (reps != null) {
                    for (Repository repository : reps) {
                        if (!repositories.containsKey(repository.getId())) {
                            if (old) {
                                Map<String, URL> newRepositories = new HashMap<String, URL>();
                                newRepositories.putAll(repositories);
                                repositories = newRepositories;
                                old = false;
                            }
                            repositories.put(repository.getId(), new URL(repository.getUrl()));
                        }
                    }
                }
            }
            pom = pom.getParentPOM();
        }

        return repositories;
    }

    public File getArtifactFile(Artifact artifact, Stack<Artifact> stack) throws FileNotFoundException {
        File file = Utils.createLocalArtifactFileName(artifact, artifact.getVersion(), getLocalRepository());
        if (!file.exists()) {
            return downloadFile(artifact, file, repositories, stack);
        }

        if (artifact.isSnapshot()) {
            try {
                return downloadFile(artifact, file, repositories, stack);
            } catch (FileNotFoundException ignore) {
            }
        }
        return file;
    }

    public File downloadFile(Artifact artifact, File toFile, Map<String, RepositoryDefinition> repositories, Stack<Artifact> stack) throws FileNotFoundException {
        for (Map.Entry<String, RepositoryDefinition> entry : repositories.entrySet()) {
            if (artifact.isSnapshot()) {
                try {
                    File file = Utils.createLocalFileName(artifact, "maven-metadata-" + entry.getKey() + ".xml", getLocalRepository());

                    URL url = URLUtils.add(entry.getValue().getURL(), artifact.getGroupId().replace('.', '/') + '/' + artifact.getArtifactId() + '/' + artifact.getVersion() + "/maven-metadata.xml");
                    downloadFile(url, file, stack);

                    MavenMetadata mavenMetadata = new MavenMetadata();
                    try {
                        XMLProcessor processor = new XMLProcessor(file);
                        processor.setStartObject(mavenMetadata);
                        processor.process();
                    } catch (Exception e3) {
                        throw new RuntimeException(e3);
                    }
                    if ((mavenMetadata.getVersioning() != null)
                            && (mavenMetadata.getVersioning().getSnapshot() != null)) {

                        Snapshot snapshot = mavenMetadata.getVersioning().getSnapshot();
                        if ((snapshot.getBuildNumber() != null)
                                && (snapshot.getTimestamp() != null)) {

                            String version = artifact.getVersion();
                            String tempVersion = version.substring(0, version.length() - 8) + snapshot.getTimestamp() + "-" + snapshot.getBuildNumber();

                            File tempFile = Utils.createLocalArtifactFileName(artifact, tempVersion, getLocalRepository());
                            if (!tempFile.exists()) {
                                downloadFile(entry.getValue().getURL(), artifact, tempVersion, tempFile, stack);
                                updateFileTimestamp(tempFile, snapshot.getTimestamp());
                            }

                            if (!toFile.equals(tempFile) && tempFile.lastModified() >= toFile.lastModified()) {
                                Utils.copyFile(tempFile, toFile);
                            }
                            return toFile;
                        }
                    }
                } catch (FileNotFoundException i2) {
                } catch (UnsupportedEncodingException e2) {
                    throw new RuntimeException(e2);
                } catch (MalformedURLException e2) {
                    throw new RuntimeException(e2);
                }
            }

            try {
                downloadFile(entry.getValue().getURL(), artifact, artifact.getVersion(), toFile, stack);
                return toFile;
            } catch (FileNotFoundException e1) {
            }
        }
        throw new FileNotFoundException("Cannot download artifact " + artifact.getFullId() + "; " + Utils.stackToString(stack));
    }

    private void updateFileTimestamp(File file, String timestampString) {
        if (file.exists()) {
            try {
                Date date = new SimpleDateFormat("yyyyMMdd.HHmmss").parse(timestampString);
                long timestamp = date.getTime();
                file.setLastModified(timestamp);
            } catch (ParseException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public void downloadFile(URL url, Artifact artifact, String altVersion, File toFile, Stack<Artifact> stack) throws FileNotFoundException {
        try {
            StringBuffer path = new StringBuffer();
            path.append(artifact.getGroupId().replace('.', '/')).append('/').append(artifact.getArtifactId()).append('/').append(artifact.getVersion()).append('/');
            path.append(artifact.getArtifactId()).append('-').append(altVersion);
            if (artifact.getClassifier() != null) {
                path.append('-').append(artifact.getClassifier());
            }
            path.append('.').append(artifact.getType());
            URL finalURL = URLUtils.add(url, path.toString());

            // TODO
            if (Extend.info.isDebugEnabled()) {
                Extend.info.info("Downloading from " + finalURL.toString());
            }
            downloadFile(finalURL, toFile, stack);
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void downloadFile(URL url, File file, Stack<Artifact> stack) throws FileNotFoundException {
        Long when = failedURLS.get(url);
        if ((when == null) || ((System.currentTimeMillis() - when.longValue()) > getFailedTimeout())) {
            long now = 0;
            if (Extend.transport.isDebugEnabled()) {
                now = System.currentTimeMillis();
                Extend.transport.debug("    Downloading \"" + url + "\"...");
            }
            try {
                URLConnection urlConnection = url.openConnection();

                urlConnection.setConnectTimeout(getConnectTimeout());
                urlConnection.setReadTimeout(getReadTimeout());

                InputStream input = urlConnection.getInputStream();
                try {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        if (!parent.mkdirs()) {
                            throw new RuntimeException("Cannot create directory " + parent.getAbsolutePath());
                        }
                    }
                    FileOutputStream output = new FileOutputStream(file);
                    try {
                        byte[] buffer = new byte[bufferSize];
                        int r = input.read(buffer);
                        while (r > 0) {
                            output.write(buffer, 0, r);
                            r = input.read(buffer);
                        }
                    } finally {
                        output.close();
                    }
                } finally {
                    input.close();
                }
                if (Extend.transport.isDebugEnabled()) {
                    Extend.transport.debug("    Downloading \"" + url + "\" succedded. (" + (System.currentTimeMillis() - now) + ")");
                }
                succeddedURLS.put(url, new Long(System.currentTimeMillis()));
            } catch (IOException e) {
                if (Extend.transport.isDebugEnabled()) {
                    Extend.transport.debug("    Downloading \"" + url + "\" failed. (" + (System.currentTimeMillis() - now) + ")");
                }
                Extend.info.debug(" * Added url to the blacklist " + url);
                failedURLS.put(url, new Long(System.currentTimeMillis()));
                throw new FileNotFoundException("Cannot download artifact " + url + "; " + Utils.stackToString(stack));
            }
        } else {
            if (Extend.transport.isDebugEnabled()) {
                Extend.transport.debug("        Skipped blacklisted \"" + url + "\"");
            }
            Extend.info.debug(" * prevented loading " + url);
            throw new FileNotFoundException("Prevented downloading artifact (alraedy failed before) " + url + "; " + Utils.stackToString(stack));
        }
    }

    protected void downloadFile(URL url, StringBuilder result, Stack<Artifact> stack) throws FileNotFoundException {
        Long when = failedURLS.get(url);
        if ((when == null) || ((System.currentTimeMillis() - when.longValue()) > getFailedTimeout())) {
            long now = 0;
            if (Extend.transport.isDebugEnabled()) {
                now = System.currentTimeMillis();
                Extend.transport.debug("    Downloading \"" + url + "\"...");
            }
            try {
                URLConnection urlConnection = url.openConnection();

                urlConnection.setConnectTimeout(getConnectTimeout());
                urlConnection.setReadTimeout(getReadTimeout());

                InputStream input = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(input);
                try {
                    char[] buffer = new char[bufferSize];
                    int r = reader.read(buffer);
                    while (r > 0) {
                        result.append(buffer, 0, r);
                        r = reader.read(buffer);
                    }
                } finally {
                    input.close();
                }
                if (Extend.transport.isDebugEnabled()) {
                    Extend.transport.debug("    Downloading \"" + url + "\" succedded. (" + (System.currentTimeMillis() - now) + ")");
                }
                succeddedURLS.put(url, new Long(System.currentTimeMillis()));
            } catch (IOException e) {
                if (Extend.transport.isDebugEnabled()) {
                    Extend.transport.debug("    Downloading \"" + url + "\" failed. (" + (System.currentTimeMillis() - now) + ")");
                }
                Extend.info.debug(" * Added url to the blacklist " + url);
                failedURLS.put(url, new Long(System.currentTimeMillis()));
                throw new FileNotFoundException("Cannot download artifact " + url + "; " + Utils.stackToString(stack));
            }
        } else {
            if (Extend.transport.isDebugEnabled()) {
                Extend.transport.debug("        Skipped blacklisted \"" + url + "\"");
            }
            Extend.info.debug(" * prevented loading " + url);
            throw new FileNotFoundException("Prevented downloading artifact (alraedy failed before) " + url + "; " + Utils.stackToString(stack));
        }
    }

    protected void writeFile(StringBuilder buffer, File file, Stack<Artifact> stack) throws FileNotFoundException {
        File parent = file.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                throw new RuntimeException("Cannot create directory " + parent.getAbsolutePath());
            }
        }
        try {
            FileWriter output = new FileWriter(file);
            try {
                output.write(buffer.toString());
            } finally {
                output.close();
            }
        } catch (IOException e) {
            if (Extend.transport.isDebugEnabled()) {
                Extend.transport.debug("    Writing \"" + file.getAbsolutePath() + "\" failed.");
            }
            throw new FileNotFoundException("Cannot write file " + file.getAbsolutePath() + "; " + Utils.stackToString(stack));
        }
    }
}