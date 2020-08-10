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
package org.abstracthorizon.extend.server.dynamic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repository.maven.MavenRepoModuleLoader;
import org.abstracthorizon.extend.repository.maven.ParallelRepositoryModuleLoader;
import org.abstracthorizon.extend.repository.maven.RepositoryLoader;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleLoader;
import org.abstracthorizon.extend.server.deployment.support.JarModuleLoader;
import org.abstracthorizon.extend.server.deployment.support.ModuleClassLoader;
import org.abstracthorizon.extend.server.support.Dump;
import org.abstracthorizon.extend.server.support.EnhancedMap;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Bootstrap class that sets up Extend through maven style repository
 *
 * @author Daniel Sendula
 */
public class Bootstrap {

    public static final String DEFAULT_VERSION = "1.2";

    public static final String BOOTSTRAP_MODULE = "repo:maven:org.abstracthorizon.extend.bootstrap:extend-bootstrap-second-stage:${version}:jar";

    protected static String bootstrap = BOOTSTRAP_MODULE;

    protected static boolean debug = false;

    protected static boolean quick = false;

    protected static boolean parallel = false;

    public static void main(String[] args) throws Exception {

        System.out.println("Bootstrapping Extend...");

        ArrayList<String> repositories = new ArrayList<String>();

        ArrayList<String> originalArguments = new ArrayList<String>();
        originalArguments.addAll(Arrays.asList(args));

        File folder = new File("").getCanonicalFile();

        if (args.length > 0) {
            int i = 0;
            int p = 0;
            while (i < args.length) {
                if (args[i].equals("-b") || args[i].equals("--bootstrap")) {
                    i++;
                    if (i == args.length) {
                        throw new IllegalArgumentException("Expected bootstrap module location after -b or --bootstrap switch");
                    }
                    bootstrap = args[i];
                    originalArguments.remove(p);
                    originalArguments.remove(p);
                } else if (args[i].equals("-r") || args[i].equals("--repository")) {
                    i++;
                    if (i == args.length) {
                        throw new IllegalArgumentException("Expected repository url name after -r or --repository switch");
                    }
                    repositories.add(args[i]);
                } else if (args[i].equals("-q") || args[i].equals("--quick")) {
                    i++;
                    quick = true;
                } else if (args[i].equals("-p") || args[i].equals("--loader")) {
                    i++;
                    if (i == args.length) {
                        throw new IllegalArgumentException("Expected version number after -v or --version switch");
                    }
                    String loader = args[i];
                    parallel = "parallel".equalsIgnoreCase(loader);
                } else if (args[i].equals("-v") || args[i].equals("--version")) {
                    i++;
                    if (i == args.length) {
                        throw new IllegalArgumentException("Expected version number after -v or --version switch");
                    }
                    String version = args[i];
                    if (bootstrap.contains("${version}")) {
                        bootstrap = bootstrap.replace("${version}", version);
                    } else {
                        throw new IllegalArgumentException("For version switch to work bootstrap module's location must include '${verison}' substring or bootstrap module must not be added (-b or --bootstrap switch).");
                    }
                    originalArguments.remove(p);
                    originalArguments.remove(p);
                } else if (args[i].equals("-d") || args[i].equals("--directory")) {
                    i++;
                    if (i == args.length) {
                        throw new IllegalArgumentException("Expected folder name after -f or --folder switch");
                    }
                    // TODO setting folder twice - is that allowed?
                    folder = new File(args[i]);
                } else if (args[i].equals("--debug") || args[i].equals("-X")) {
                    debug = true;
                }
                i++;
                p++;
            }
        }

        File log4jFile = new File(folder, "config/log4j.xml");
        if (log4jFile.exists()) {
            DOMConfigurator.configureAndWatch(log4jFile.getAbsolutePath(), 10000);
            if (debug) {
                System.out.println("  Configured log4j from " + log4jFile.getAbsolutePath());
            }
        }


        DeploymentManagerStub deploymentManager = new DeploymentManagerStub();

        RepositoryLoader repositoryLoader;
        if (parallel) {
            System.out.println("  ** Using parallel loader");
            repositoryLoader = new ParallelRepositoryModuleLoader();
        } else {
            System.out.println("  ** Using serial loader");
            repositoryLoader = new MavenRepoModuleLoader();
        }
        // RepositoryLoader repositoryLoader = new MavenRepoModuleLoader();
        repositoryLoader.setDeploymentManager(deploymentManager);
        repositoryLoader.setCheckSnapshotVersions(false);
        deploymentManager.getModuleLoaders().add(repositoryLoader);

        int ri = 0;
        for (String r : repositories) {
            repositoryLoader.addRepository("anonymous_" + ri, new URL(r), true, true);
            ri++;
        }
        repositoryLoader.start();

        JarModuleLoader jarModuleLoader = new JarModuleLoader();
        jarModuleLoader.setDeploymentManager(deploymentManager);
        jarModuleLoader.getExtensions().add(".jar");
        deploymentManager.getModuleLoaders().add(jarModuleLoader);

        if (bootstrap.contains("${version}")) {
            bootstrap = bootstrap.replace("${version}", DEFAULT_VERSION);
        }

        URI supportCoreURI = new URI(bootstrap);
        Module module = deploymentManager.load(supportCoreURI);
        deploy(supportCoreURI, module, deploymentManager);

        fixSpring(deploymentManager);

        String mainClassString = null;
        ClassLoader cl = module.getClassLoader();
        Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
        URL manifestURL = null;
        String moduleLocation = module.getOriginalLocation().toString();
        while ((manifestURL == null) && manifests.hasMoreElements()) {
            URL url = manifests.nextElement();
            String urlString = url.toString().substring(4);
            if (urlString.startsWith(moduleLocation)) {
                manifestURL = url;
            }
        }

        if (manifestURL != null) {
            InputStream manifestInputStream = manifestURL.openStream();
            if (manifestInputStream != null) {
                try {
                    Manifest manifest = new Manifest(manifestInputStream);
                    mainClassString = manifest.getMainAttributes().getValue("Main-Class");
                } finally {
                    manifestInputStream.close();
                }
            }
        }
        if (mainClassString == null) {
            throw new IllegalArgumentException("Bootstrap module '" + bootstrap + "' must have main class defined in META-INF/MANIFEST.MF file");
        }

        ArrayList<String> arguments = new ArrayList<String>();

        if (quick) {
            arguments.add("-q");
        }

        String initialJar = obtainInitialJar();
        arguments.add("-ij");
        arguments.add(initialJar);

        String initialVersion = obtainInitialVersion();
        if ((initialVersion != null) && (initialVersion.length() > 0)) {
            arguments.add("-iv");
            arguments.add(initialVersion);
        }

        for (Module m : deploymentManager.getDeployedModules().values()) {
            arguments.add("-ad");
            arguments.add(m.getModuleId().toString());
        }

        arguments.addAll(originalArguments);

        String[] argumentsArray = new String[arguments.size()];
        argumentsArray = arguments.toArray(argumentsArray);

        updateModuleClassLoaders(deploymentManager);

        if (debug) {
            System.out.println("Starting with ==================================================================");
            Dump.outputCore(new PrintWriter(new OutputStreamWriter(System.out)), deploymentManager);
            System.out.println("================================================================================");
        }
        if (debug) {
            System.out.println("Failed URLS ====================================================================");
            Map<URL, Long> failedURLS = repositoryLoader.getFailedURLs();
            for (URL url : failedURLS.keySet()) {
                System.out.println(url);
            }
            System.out.println("================================================================================");
        }
        if (debug) {
            System.out.println("Succedded URLS ====================================================================");
            Map<URL, Long> succeddedURLS = repositoryLoader.getSucceddedURLs();
            for (URL url : succeddedURLS.keySet()) {
                System.out.println(url);
            }
            System.out.println("================================================================================");
        }
        Class<?> mainClass = cl.loadClass(mainClassString);
        mainClass.getMethod("main", new Class[]{args.getClass()}).invoke(null, new Object[]{argumentsArray});
    }

    public static void deploy(URI uri, Module module, DeploymentManager deploymentManager) {
        ModuleId moduleId = module.getModuleId();
        if (!deploymentManager.getDeployedModules().containsKey(moduleId)) {
            try {
                deploymentManager.deploy(moduleId, module);
            } catch (Exception e) {
                Extend.info.warn("Caught exception trying to deploy module " + moduleId + ".", e);
            }
        }
    }

    protected static void updateModuleClassLoaders(DeploymentManager deploymentManager) {
        for (Module m : deploymentManager.getDeployedModules().values()) {
            ClassLoader c = m.getClassLoader();
            while ((c != null) && !(c instanceof ModuleClassLoader)) {
                c = c.getParent();
            }
            if (c != null) {
                ModuleClassLoader mcl = (ModuleClassLoader)c;
                mcl.setUserThreadClassLoader(false);
                if (debug) { System.out.println("Updated module " + mcl.getModule().getModuleId()); }
            }
        }
    }

    // TODO this needs to go - or at least to pick the right values by itself!
    protected static void fixSpring(DeploymentManager deploymentManager) throws URISyntaxException {
        ModuleId springCoreModuleId = new ModuleId("org.springframework:spring-core:2.5.6:jar");

        Module springCore = deploymentManager.getDeployedModules().get(springCoreModuleId);

        ModuleId springBeansModuleId = new ModuleId("org.springframework:spring-beans:2.5.6:jar");
        Module springBeans = deploymentManager.getDeployedModules().get(springBeansModuleId);

        ModuleId springContextModuleId = new ModuleId("org.springframework:spring-context:2.5.6:jar");
        Module springContext = deploymentManager.getDeployedModules().get(springContextModuleId);

        springCore.getDependOnThis().add(springBeans);
        springCore.getDependOnThis().add(springContext);
        springBeans.getDependOnThis().add(springContext);
        springBeans.getDependsOn().add(springCore);
        springContext.getDependsOn().add(springCore);
        springContext.getDependsOn().add(springBeans);

        Module jclOverSlf4j = deploymentManager.getDeployedModules().get(new ModuleId("org.slf4j:jcl-over-slf4j:1.5.6:jar"));
        jclOverSlf4j.getDependOnThis().add(springCore);
        jclOverSlf4j.getDependOnThis().add(springBeans);
        jclOverSlf4j.getDependOnThis().add(springContext);

        springCore.getDependsOn().add(jclOverSlf4j);
        springBeans.getDependsOn().add(jclOverSlf4j);
        springContext.getDependsOn().add(jclOverSlf4j);
    }


    public static HashMap<String, String> processManifestFile(BufferedReader input) throws IOException {
        HashMap<String, String> res = new HashMap<String, String>();
        String key = null;
        String line = input.readLine();
        while (line != null) {
            if ((key != null) && (line.length() > 0) && Character.isWhitespace(line.charAt(0))) {
                res.put(key, res.get(key) + line.substring(1).trim());
            } else {
                int i = line.indexOf(':');
                if (i > 0) {
                    key = line.substring(0, i).trim();
                    String value = line.substring(i + 1).trim();
                    res.put(key, value);
                }
            }

            line = input.readLine();
        }

        return res;
    }

    public static class DeploymentManagerStub implements DeploymentManager {

        protected EnhancedMap<ModuleId, Module> deployedModules = new EnhancedMap<ModuleId, Module>();

        protected LinkedHashSet<ModuleLoader> moduleLoaders = new LinkedHashSet<ModuleLoader>();

        @Override
        public void create(Module module) {
            module.create();
        }

        @Override
        public void deploy(ModuleId moduleId, Module module) {
            deployedModules.put(moduleId, module);
            create(module);
            if ((module.getState() == Module.CREATED) || (module.getState() == Module.WAITING_ON_CREATE)) {
                start(module);
            }
            if (debug) { System.out.println("Deployed " + moduleId); }
        }

        @Override
        public void destroy(Module module) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public Module loadAndDeploy(URI uri) {
            ModuleId moduleId = toModuleId(uri);
            Module m = deployedModules.get(moduleId);
            if (m == null) {
                m = load(uri);
                if (m != null) {
                    moduleId = m.getModuleId();
                    if (!deployedModules.containsValue(m)) {
                        try {
                            deploy(moduleId, m);
                        } catch (Exception e) {
                            Extend.info.warn("Caught exception trying to deploy module " + moduleId + ".", e);
                        }
                    } else if (!deployedModules.containsKey(moduleId)) {
                        deployedModules.put(moduleId, m);
                    }
                }
            }
            return m;
        }

        @Override
        public EnhancedMap<ModuleId, Module> getDeployedModules() {
            return deployedModules;
        }

        @Override
        public Set<ModuleLoader> getModuleLoaders() {
            return moduleLoaders;
        }

        @Override
        public void redeploy(Module module) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void setModuleLoaders(Set<ModuleLoader> moduleLoaders) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void start(Module module) {
            module.start();
            if (debug) { System.out.println("Started " + module.getModuleId()); }
        }

        @Override
        public void stop(Module module) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void undeploy(Module module) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean canLoad(URI url) {
            for (ModuleLoader loader : moduleLoaders) {
                if (loader.canLoad(url)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Translates URI to moduleId
         * @param uri uri
         * @return module id or <code>null</code>
         */
        @Override
        public ModuleId toModuleId(URI uri) {
            String path = uri.getPath();
            if (path != null) {
                ModuleId moduleId = new ModuleId();
                moduleId.setArtifactId(path);
                return moduleId;
            } else {
                return null;
            }
        }

        @Override
        public Module load(URI uri) {
            Module module = null;
            for (ModuleLoader loader : moduleLoaders) {
                if (loader.canLoad(uri)) {
                    module = loader.load(uri);
                    return module;
                }
            }
            return module;
        }

        @Override
        public Module loadAs(URI uri, ModuleId moduleId) {
            Module module = null;
            for (ModuleLoader loader : moduleLoaders) {
                if (loader.canLoad(uri)) {
                    module = loader.loadAs(uri, moduleId);
                    return module;
                }
            }
            return module;
        }
    }

    public static String obtainInitialJar() throws MalformedURLException, UnsupportedEncodingException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        String className = Bootstrap.class.getName().replace('.', '/') + ".class";
        URL root = contextClassLoader.getResource(className);
        if ("jar".equals(root.getProtocol())) {
            String file = URLDecoder.decode(root.getFile(), "UTF-8");
            //if (!file.startsWith("file")) {
            //    throw new RuntimeException("Cannot handle protocol from where this jar is loaded; " + root);
            //}
            int i = file.lastIndexOf('!');
            file = file.substring(0, i);
            i = file.lastIndexOf("/");
            if (i >= 0) {
                file = file.substring(i + 1);
            }
            return file;
        } else {
        return "extend.jar";
        }
    }

    public static String obtainInitialVersion() {
        try {
            ClassLoader cl = Bootstrap.class.getClassLoader();
            String name = Bootstrap.class.getName().replace('.', '/') + ".class";
            URL url = cl.getResource(name);
            String urlString = url.toString();
            urlString = urlString.substring(0, urlString.length() - name.length());
            Enumeration<URL> e = cl.getResources("META-INF/Extend-Version");
            while (e.hasMoreElements()) {
                URL u = e.nextElement();
                String s = u.toString();
                if (s.startsWith(urlString)) {
                    InputStream is = u.openStream();
                    try {
                        Manifest manifest = new Manifest(is);
                        return manifest.getMainAttributes().getValue("Version");
                    } finally {
                        is.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
