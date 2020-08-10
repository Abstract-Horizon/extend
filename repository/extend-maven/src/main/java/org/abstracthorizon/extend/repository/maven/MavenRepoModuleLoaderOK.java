/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import org.abstracthorizon.extend.repository.maven.pom.Artifact;
import org.abstracthorizon.extend.repository.maven.pom.Dependencies;
import org.abstracthorizon.extend.repository.maven.pom.Dependency;
import org.abstracthorizon.extend.repository.maven.pom.Exclusions;
import org.abstracthorizon.extend.repository.maven.pom.MavenMetadata;
import org.abstracthorizon.extend.repository.maven.pom.POM;
import org.abstracthorizon.extend.repository.maven.pom.Settings;
import org.abstracthorizon.extend.repository.maven.pom.Snapshot;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.service.AbstractServiceModuleLoader;
import org.abstracthorizon.extend.server.deployment.support.AbstractModule;
import org.abstracthorizon.extend.server.deployment.support.ProvisionalModule;
import org.abstracthorizon.extend.server.support.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module loader that loads files from maven style repository
 *
 * @author Daniel Sendula
 */
public class MavenRepoModuleLoaderOK extends AbstractServiceModuleLoader {

    /** Default buffer size - 10K */
    public static final int DEFAULT_BUFFER_SIZE = 10240;

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(MavenRepoModuleLoaderOK.class);

    /** POM cache */
    protected Map<String, POM> pomCache = new HashMap<String, POM>();

    /** Repositories */
    protected Map<String, URL> repositories = new HashMap<String, URL>();

    /** Download buffer size */
    protected int bufferSize = DEFAULT_BUFFER_SIZE;

    /** Local repository path */
    protected File localRepository;

    /** Local path separator char */
    private char separator = File.separatorChar;

    /** Settings */
    private Settings settings = new Settings();
    
    /**
     * Constructor
     */
    public MavenRepoModuleLoaderOK() {
        localRepository = new File(System.getProperty("user.home"), ".m2/repository");
        try {
            // TODO setting up urls lasts too long. Why?
            repositories.put("central", new URL("http://repo1.maven.org/maven2"));
            repositories.put("abstracthorizon", new URL("http://repository.abstracthorizon.org/maven2/abstracthorizon"));
            repositories.put("abstracthorizon.snapshot", new URL("http://repository.abstracthorizon.org/maven2/abstracthorizon.snapshot"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        File settingsRepository = new File(System.getProperty("user.home"), ".m2/settings.xml");
        if (settingsRepository.exists()) {
            XMLProcessor processor = new XMLProcessor(settingsRepository);
            processor.setStartObject(settings);
            try {
                processor.process();
                
                if (settings.getLocalRepository() != null) {
                    localRepository = new File(settings.getLocalRepository());
                    logger.info("Setting local repository to " + localRepository.getAbsolutePath());
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        logger.debug("Using following repositories:");
        for (Map.Entry<String, URL> e : repositories.entrySet()) {
            logger.debug("    " + e.getKey() + " as " + e.getValue());
        }
        logger.debug("Using local cache at " + localRepository.getAbsolutePath());
    }

    /**
     * Sets repositories
     * @param repositories repositories
     */
    public void setRepositories(Map<String, URL> repositories) {
        this.repositories = repositories;
    }

    /**
     * Returns repositories
     * @return repositories
     */
    public Map<String, URL> getRepositories() {
        return repositories;
    }

    /**
     * Stop method removes URLResolver from &quot;DefaultURLResolver&quot;.
     */
    public void stop() {
        super.stop();

        pomCache.clear();
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
                Artifact artifact = parseArtifact(file.substring(6));
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
            Artifact artifact = parseArtifact(file.substring(6));
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
        if (!(moduleId instanceof Artifact)) {
            moduleId = new Artifact(moduleId);
        }
        try {
            return loadAndDeploy((Artifact)moduleId,  new HashSet<Artifact>(), new Stack<Artifact>());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Module loadAndDeploy(Artifact artifact, Set<Artifact> excludes, Stack<Artifact> stack) throws IOException {
        
        if (artifact instanceof Dependency) {
            artifact = new Artifact(artifact);
        }
        
        String originalId = artifact.getFullId();
        Artifact originalArtifact = new Artifact(artifact);

        if (logger.isDebugEnabled()) {
            logger.debug("Loading module " + originalId);
        }

        // boolean updatedType = false;

        DeploymentManager deploymentManager = getDeploymentManager();
        Module existing = deploymentManager.getDeployedModules().get(artifact);
        if ((existing != null) && !(existing instanceof ProvisionalModule)) {
            return existing;
        }

        POM pom = null;
        try {
            // Try to load pom
            pom = loadPom(artifact.toPOMArtifact(), stack);
        } catch (IOException e) {
            // If that fails try jar file directly
            try {
                Artifact a = artifact;
                if (a.getType() == null) {
                    a = new Artifact(artifact);
                    a.setType("jar");
                }
                File localFile = createLocalFile(a);
                if (!localFile.exists()) {
                    downloadFile(a, localFile, stack);
                }
            } catch (IOException e2) {
                // If that fails try to change the version to -SNAPSHOT
                if (!artifact.isSnapshot()) {
                    Artifact snapshotArtifact = artifact.toSnapshotArtifact();
                    Module m = loadAndDeploy(snapshotArtifact, excludes, stack);
                    if (m != null) {

                        if (!getDeploymentManager().getDeployedModules().containsKey(artifact)) {
                            getDeploymentManager().getDeployedModules().put(artifact, m);
                        }
                        if (artifact.getType() == null) {
                            try {
                                POM snapshotPOM = loadPom(snapshotArtifact.toPOMArtifact(), stack);
                                artifact.setType(snapshotPOM.getType());
                                if (artifact.getType() == null) {
                                    artifact.setType("jar");
                                }

                                if (!getDeploymentManager().getDeployedModules().containsKey(artifact)) {
                                    getDeploymentManager().getDeployedModules().put(artifact, m);
                                }
                            } catch (IOException ignore) {
                            }
                        }
                        return m;
                    } else {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        if (artifact.getType() == null) {
            artifact.setType(pom.getPackaging());
            if (artifact.getType() == null) {
                artifact.setType("jar");
            }
           // updatedType = true;

            Module m = deploymentManager.getDeployedModules().get(artifact);
            if (m != null) {
                deploymentManager.getDeployedModules().put(originalArtifact, m);
                logger.info("  Found full spec artifact " + originalArtifact + ".");
                return m;
            }
            if (artifact.isSnapshot()) {
                Artifact artifact2 = artifact.toNonSnapshotArtifact();
                
                m = deploymentManager.getDeployedModules().get(artifact2);
                if (m != null) {
                    deploymentManager.getDeployedModules().put(originalArtifact, m);
                    logger.info("  Found final version of artifact " + artifact2.toString() + ".");
                    return m;
                }
            }

        }
        if (artifact.isSnapshot()) {
            Artifact artifact2 = artifact.toNonSnapshotArtifact();
            
            Module m = deploymentManager.getDeployedModules().get(artifact2);
            if (m != null) {
                deploymentManager.getDeployedModules().put(originalArtifact, m);
                logger.info("  Found final version of artifact " + artifact2.toString() + ".");
                return m;
            }
        }

        File localFile = createLocalFile(artifact);
        if (!localFile.exists()) {
            downloadFile(artifact, localFile, stack);
        }

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
//                if (updatedType) {
////                    URI updatedIdURI = new URI("repo:maven:" + artifact.getFullId());
//                    if (deploymentManager.getDeployedModules().containsKey(artifact)) {
//                        throw new RuntimeException("Overwriting existing module!");
//                    }
//                    deploymentManager.getDeployedModules().put(artifact, module);
//                }
            if (pom != null) {
                String type = pom.getPackaging();
                if ((type == null) || !type.equals("war")) {
                    stack.push(artifact);
                    try {
                        processDependencies(pom, module, excludes, stack);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        throw e;
                    }
                    stack.pop();
                }
            }
            return module;
        }

        return null;
    }

    public POM loadPom(Artifact pomArtifact, Stack<Artifact> stack) throws FileNotFoundException {

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
            if (logger.isDebugEnabled()) {
                logger.debug("Loading pom " + pomId);
            }

            File pomFile = createLocalFile(pomArtifact);
            if (!pomFile.exists()) {
                downloadFile(pomArtifact, pomFile, stack);
            }

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
                POM parentPom = loadPom(pom.getParent(), stack);
                stack.pop();
                pom.setParentPOM(parentPom);
            }


            HashMap<String, String> properties = new HashMap<String, String>();
            collectProperties(properties, pom);
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

    protected void collectProperties(Map<String, String> properties, POM pom) {
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

    public void processDependencies(POM pom, Module module, Set<Artifact> excludes, Stack<Artifact> stack) throws IOException {
        long started = 0;
        if (logger.isDebugEnabled()) {
            logger.debug("  Processing dependencies for " + module.getModuleId());
            started = System.currentTimeMillis();
        }
        Dependencies dependencies = pom.getDependencies();
        if (dependencies != null) {
            for (Dependency dependency : dependencies.getDependencies()) {
    //              if ((dependency.getVersion() == null) && (pom.getParentPOM() != null)) {
    //                  updateDependency(pom, dependency);
    //              }
                if (pom.getParentPOM() != null) {
                    updateDependency(pom, dependency);
                }

                String scope = dependency.getScope();
                if (((scope == null)
                        || scope.equals("runtime")
                        || scope.equals("compile"))
                    && !excludesContain(excludes, dependency)) {

                    if (logger.isDebugEnabled()) {
                        logger.debug("    Dependency " + dependency.getFullId() + " as " + scope);
                    }

                    Set<Artifact> innerExcludes = excludes;
                    if (dependency.getExclusions() != null) {
                        innerExcludes = new HashSet<Artifact>();
                        innerExcludes.addAll(excludes);
                        innerExcludes.addAll(dependency.getExclusions().getExclusions());
                    }

//                    String groupId = dependency.getGroupId();
//                    String artifactId = dependency.getArtifactId();
//                    String version = dependency.getVersion();
//                    String type = dependency.getType();
//                    String classifier = dependency.getClassifier();
//                    try {
                        try {
                            stack.push(pom);
                            POM dependendPom = loadPom(dependency.toPOMArtifact(), stack);
                            stack.pop();
                            if (dependency.getType() == null) {
                                dependency.setType(dependendPom.getPackaging());
                            }
                        } catch (FileNotFoundException ein) {
                            // Cannot load pom
                            if (dependency.getType() == null) {
                                dependency.setType("jar");
                            }
                        }

                        URI moduleURI = null;
                        try {
                            moduleURI = new URI("repo:maven:" + dependency.getFullId());
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }

                        Module m = null;
                        if (!deploymentManager.getDeployedModules().containsKey(dependency)) {

                            stack.push(pom);
                            try {
                                m = loadAndDeploy(dependency, innerExcludes, stack);
                            } catch (FileNotFoundException ignore) {
                            }
                            stack.pop();
                            if (m != null) {
                                if (!deploymentManager.getDeployedModules().containsValue(m)) {
                                    deploymentManager.deploy(dependency, m);
                                } else if (!deploymentManager.getDeployedModules().containsKey(dependency)) {
                                    Artifact a = new Artifact(dependency);
                                    deploymentManager.getDeployedModules().put(a, m);
                                } else {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Module " + m.getModuleId() + " at " + dependency + " already exists");
                                    }
                                }
//                            } else {
//                                if (logger.isDebugEnabled()) {
//                                    logger.debug("    " + dependency);
//                                }
//
//                                m = getDeploymentManager().loadAndDeploy(moduleURI);
                            }
                        } else {
                            m = deploymentManager.getDeployedModules().get(dependency);
                        }
                        if (m == null) {
                            if (!"true".equalsIgnoreCase(dependency.getOptional())) {
                                throw new FileNotFoundException("Couldn't load " + moduleURI + "; " + stackToString(stack));
                            }
                        } else {
                            module.getDependsOn().add(m);
                            m.getDependOnThis().add(module);
                        }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("  Processed dependencies for " + module.getModuleId() + "(" + Long.toString(System.currentTimeMillis() - started) + "ms)");
        }
    }

    public void updateDependency(POM pom, Dependency dependency) {
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

    public File createLocalFile(Artifact artifact) {
        StringBuffer fileName = new StringBuffer();
        fileName.append(artifact.getGroupId().replace('.', separator)).append(separator);
        fileName.append(artifact.getArtifactId()).append(separator);
        fileName.append(artifact.getVersion()).append(separator);
        fileName.append(artifact.getArtifactId()).append('-').append(artifact.getVersion());
        if (artifact.getClassifier() != null) {
            fileName.append('-').append(artifact.getClassifier());
        }
        fileName.append('.').append(artifact.getType());

        File localFile = new File(localRepository, fileName.toString());
        return localFile;
    }

    public File createLocalFile(Artifact artifact, String file) {
        StringBuffer fileName = new StringBuffer();
        fileName.append(artifact.getGroupId().replace('.', separator)).append(separator);
        fileName.append(artifact.getArtifactId()).append(separator);
        fileName.append(artifact.getVersion()).append(separator);
        fileName.append(file);

        File localFile = new File(localRepository, fileName.toString());
        return localFile;
    }

    public void downloadFile(Artifact artifact, File toFile, Stack<Artifact> stack) throws FileNotFoundException {
        for (Map.Entry<String, URL> entry : repositories.entrySet()) {
            try {
                downloadFile(entry.getValue(), artifact, toFile, stack);
                return;
            } catch (FileNotFoundException e1) {
                if (artifact.isSnapshot()) {
                    try {
                        File file = createLocalFile(artifact, "maven-metadata-" + entry.getKey() + ".xml");
                        // TODO - we do not want to download file each time!

                        URL url = URLUtils.add(entry.getValue(), artifact.getGroupId().replace('.', '/') + '/' + artifact.getArtifactId() + '/' + artifact.getVersion() + "/maven-metadata.xml");
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

//                                Artifact altArtifact = new Artifact(artifact);
                                String version = artifact.getVersion();
                                String tempVersion = version.substring(0, version.length() - 8) + snapshot.getTimestamp() + "-" + snapshot.getBuildNumber();
//                                altArtifact.setVersion(tempVersion);
                                File tempFile = createLocalFile(artifact);
                                if (!tempFile.exists()) {
                                    downloadFile(entry.getValue(), artifact, tempVersion, tempFile, stack);
                                }

                                if (!toFile.equals(tempFile)) {
                                    copyFile(tempFile, toFile);
                                }
                                return;
                            }
                        }
                    } catch (FileNotFoundException i2) {
                    } catch (UnsupportedEncodingException e2) {
                        throw new RuntimeException(e2);
                    } catch (MalformedURLException e2) {
                        throw new RuntimeException(e2);
                    }
                }
            }
        }
        throw new FileNotFoundException("Cannot download artifact " + artifact.getFullId() + "; " + stackToString(stack));
    }

    public void downloadFile(URL url, Artifact artifact, File toFile, Stack<Artifact> stack) throws FileNotFoundException {
        downloadFile(url, artifact, artifact.getVersion(), toFile, stack);
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
            if (logger.isDebugEnabled()) {
                logger.info("Downloading from " + finalURL.toString());
            }
            downloadFile(finalURL, toFile, stack);
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void downloadFile(URL url, File file, Stack<Artifact> stack) throws FileNotFoundException {
        try {
            InputStream input = url.openStream();
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
        } catch (IOException e) {
//            e.printStackTrace();
            throw new FileNotFoundException("Cannot download artifact " + url + "; " + stackToString(stack));
            // throw new FileNotFoundException(url.toString());
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
                return true;
            } catch (IOException exc) {
                return false;
            }
        } else {
            return true;
        }
    }
    
    protected String stackToString(Stack<Artifact> stack) {
        StringBuffer r = new StringBuffer();
        
        r.append("\n");
        r.append("Stack:\n");
        for (Artifact a : stack) {
            r.append(a.getFullId()).append("\n");
        }
        r.append("\n");
        
        return r.toString();
    }
}