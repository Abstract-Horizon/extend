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
package org.abstracthorizon.extend.server.dynamic.secondstage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repository.maven.RepositoryLoader;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleLoader;
import org.abstracthorizon.extend.server.support.Dump;
import org.abstracthorizon.extend.server.support.EnhancedMap;
import org.abstracthorizon.extend.support.spring.server.SpringBasedServer;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap class that sets up Extend through maven style repository
 *
 * @author Daniel Sendula
 */
public class Bootstrap {

    /** Logger */
    protected final static Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    protected String initialVersion = "1.2";

    protected String version;

    protected File folder;

    protected HashSet<String> alreadyDeployedModules = new HashSet<String>();

    protected String initialJar = "extend.jar";

    protected DeploymentManagerStub deploymentManager;

    protected DeploymentManager finalDeploymentManager;

    protected boolean weStarted = false;

    protected Socket socket;

    protected PrintWriter out;

    protected BufferedReader in;

    protected boolean debug = false;

    protected List<String> extraRepos = new ArrayList<String>();

    protected boolean quick;

    protected String repositoryLoaderBean = "Serial";

    public Bootstrap() {
        try {
            folder = new File("").getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        Bootstrap bootstrap = new Bootstrap();
        try {
            bootstrap.processArguments(args);
        } catch (Throwable e) {
            logger.error("* Failed starting extend ", e);
            logger.debug("===========================================================================================");
            if (logger.isDebugEnabled()) {
                StringWriter dump = new StringWriter();
                if (bootstrap.finalDeploymentManager != null) {
                    Dump.outputCore(new PrintWriter(dump), bootstrap.finalDeploymentManager);
                } else {
                    Dump.outputCore(new PrintWriter(dump), bootstrap.deploymentManager);
                }
                logger.debug(dump.toString());
            }
        }
    }

//    public static void deploy(URI uri, Module module, DeploymentManager deploymentManager) {
//        if (!deploymentManager.getDeployedModules().containsKey(uri)) {
//            deploymentManager.deploy(uri, module);
//        }
//    }

    public void processArguments(String[] args) throws Exception {
        int i = 0;
        boolean switches = true;
        while (switches && (i < args.length)) {
            if (args[i].equals("-v") || args[i].equals("--version")) {
                i++;
                if (i == args.length) {
                    throw new IllegalArgumentException("Expected version number after -v or --version switch");
                }
                // TODO setting version twice - is that allowed?
                version = args[i];
            } else if (args[i].equals("-d") || args[i].equals("--directory")) {
                i++;
                if (i == args.length) {
                    throw new IllegalArgumentException("Expected folder name after -f or --folder switch");
                }
                // TODO setting folder twice - is that allowed?
                folder = new File(args[i]);
            } else if (args[i].equals("-q") || args[i].equals("--quick")) {
                i++;
                quick = true;
            } else if (args[i].equals("-r") || args[i].equals("--repository")) {
                i++;
                if (i == args.length) {
                    throw new IllegalArgumentException("Expected repository url name after -r or --repository switch");
                }
                extraRepos.add(args[i]);
            } else if (args[i].equals("-p") || args[i].equals("--loader")) {
                i++;
                if (i == args.length) {
                    throw new IllegalArgumentException("Expected version number after -v or --version switch");
                }
                repositoryLoaderBean = args[i];
            } else if (args[i].equals("-h") || args[i].equals("-?") || args[i].equals("--help")) {
                help();
            } else if (args[i].equals("-X") || args[i].equals("--debug")) {
                debug = true;
            } else if (args[i].equals("-ad") || args[i].equals("--alreadyDeployed")) {
                i++;
                if (i == args.length) {
                    throw new IllegalArgumentException(
                            "Expected module location name after -ad or --alreadyDeployed switch");
                }
                alreadyDeployedModules.add(args[i]);
            } else if (args[i].equals("-ij") || args[i].equals("--initialJar")) {
                i++;
                if (i == args.length) {
                    throw new IllegalArgumentException("Expected module location name after -ij or --initialJar switch");
                }
                initialJar = args[i];
            } else if (args[i].equals("-iv") || args[i].equals("--initialVersion")) {
                i++;
                if (i == args.length) {
                    throw new IllegalArgumentException(
                            "Expected module location name after -iv or --initialVersion switch");
                }
                initialVersion = args[i];
                if (version == null) {
                    version = initialVersion;
                }
            } else if (args[i].startsWith("-")) {
                throw new IllegalArgumentException("Unknown switch " + args[i]);
            } else {
                i--;
                switches = false;
            }
            i++;
        }
        if (version == null) {
            version = initialVersion;
        }

        File log4jFile = new File(folder, "config/log4j.xml");
        if (log4jFile.exists()) {
            DOMConfigurator.configureAndWatch(log4jFile.getAbsolutePath(), 10000);
            // System.out.println("************** configured log4j from " + log4jFile.getAbsolutePath());
        }

        logger.info("Starting Extend...");

        if (logger.isDebugEnabled()) {
            logger.debug("Arguments:");

            for (String s : args) {
                logger.debug("    " + s);
            }
        }

        socket = connectToRunningServer();
        try {
            if (socket != null) {
                OutputStream outputStream = socket.getOutputStream();
                out = new PrintWriter(new OutputStreamWriter(outputStream));
                InputStream inputStream = socket.getInputStream();
                in = new BufferedReader(new InputStreamReader(inputStream));
            }

            String command = "";
            while (i < args.length) {
                if (args[i].equals("install")) {
                    if ((i + 1 < args.length) && (args[i + 1].equals("-o") || args[i + 1].equals("--overwrite"))) {
                        install(true);
                        i++;
                    } else {
                        install(false);
                    }
                } else if (args[i].equals("shutdown") || args[i].equals("stop")) {
                    shutdown();
                    i = args.length;
                } else if (args[i].equals("start")) {
                    start();
                } else if (args[i].equals("start-module")) {
                    command = "start-module";
                } else if (args[i].equals("stop-module")) {
                    command = "stop-module";
                } else if (args[i].equals("deploy")) {
                    command = "deploy";
                } else if (args[i].equals("undeploy")) {
                    command = "undeploy";
                } else if (args[i].equals("help")) {
                // TODO ADD STATUS - to return running or not and list running modules, etc...
                    help();
                } else {
                    if (command.equals("start-module")) {
                        start(args[i]);
                    } else if (command.equals("stop-module")) {
                        stop(args[i]);
                    } else if (command.equals("deploy")) {
                        deploy(args[i]);
                    } else if (command.equals("undeploy")) {
                        undeploy(args[i]);
                    }
                }

                i++;
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    public void help() {
        try (InputStream inputStream = getClass().getResourceAsStream("help.txt")) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
                String line = in.readLine();
                while (line != null) {
                    line = processHelpLine(line);
                    System.out.println(line);
                    line = in.readLine();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String processHelpLine(String s) {
        int i = s.indexOf("${version}");
        if (i >= 0) {
            s = s.replace("${version}", initialVersion);
        }
        i = s.indexOf("${jar}");
        if (i >= 0) {
            s = s.replace("${jar}", initialJar);
        }
        return s;
    }

    public void install(boolean overwrite) throws IOException {
        if (socket != null) {
            throw new RuntimeException("Server is already running on the location " + folder.getAbsolutePath());
        }
        if (overwrite) {
            logger.info("Install --overwrite");
        } else {
            logger.info("Install");
        }

        File configFolder = new File(folder, "config");
        if (!configFolder.exists()) {
            if (!configFolder.mkdirs()) {
                throw new IOException("Cannot create " + configFolder.getAbsolutePath());
            }
        }
        //
        // File libFolder = new File(folder, "lib");
        // if (!libFolder.exists()) {
        // if (!libFolder.mkdirs()) {
        // throw new IOException("Cannot create " +
        // libFolder.getAbsolutePath());
        // }
        // }

        File deployFolder = new File(folder, "deploy");
        if (!deployFolder.exists()) {
            if (!deployFolder.mkdirs()) {
                throw new IOException("Cannot create " + deployFolder.getAbsolutePath());
            }
        }

        File serverFile = new File(configFolder, "server.xml");
        if (overwrite || !serverFile.exists()) {
            installFile(getClass().getResource("/config/server.xml"), serverFile);
        }

        File bootstrapDeployFile = new File(configFolder, "bootstrap.deploy");
        if (overwrite || !bootstrapDeployFile.exists()) {
            installFile(getClass().getResource("/config/bootstrap.deploy"), bootstrapDeployFile);
        }

        File log4jFile = new File(configFolder, "log4j.xml");
        if (overwrite || !log4jFile.exists()) {
            try {
                installFile(getClass().getResource("/config/log4j.xml"), log4jFile);
            } catch (IOException e) {
                System.out.println("*** " + e.getMessage());
            }
        }

        File dot_m2 = new File(folder, ".m2");
        if (!dot_m2.exists()) {
            if (!dot_m2.mkdirs()) {
                System.out.println("*** !!! failed to create " + dot_m2.getAbsolutePath());
            }
        }
        File repository = new File(dot_m2, "repository");
        if (!repository.exists()) {
            if (!repository.mkdirs()) {
                System.out.println("*** !!! failed to create " + repository.getAbsolutePath());
            }
        }
    }

    protected void installFile(URL url, File file) throws IOException {
        if (url != null) {
            InputStream inputStream = url.openStream();
            try {
                OutputStream outputStream = new FileOutputStream(file);
                try {
                    byte[] buf = new byte[10240];
                    int r = inputStream.read(buf);
                    while (r > 0) {
                        outputStream.write(buf, 0, r);
                        r = inputStream.read(buf);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } else {
            throw new IOException("No given source for " + file.getAbsolutePath());
        }
    }

    public void start() throws Exception {

        if (socket != null) {
            throw new RuntimeException("Server is already running on the location " + folder.getAbsolutePath());
        }

        deploymentManager = new DeploymentManagerStub();

        ModuleId bootstrapModuleId = new ModuleId();
        bootstrapModuleId.setArtifactId("internalBootstrapModule");
        Module core = new BootstrapModule(bootstrapModuleId, getClass().getClassLoader());
        for (String deployedModuleId : alreadyDeployedModules) {
            // deploymentManager.getDeployedModules().put(bootstrapModuleId, core);
            deploymentManager.getDeployedModules().put(new ModuleId(deployedModuleId), core);
        }

        SpringBasedServer springBasedServer = new SpringBasedServer(folder.toURI().toURL(), folder.toURI().toURL());
        springBasedServer.create();
        finalDeploymentManager = (DeploymentManager) springBasedServer.getServerApplicationContext().getBean(
                "DeploymentManager");
        finalDeploymentManager.getDeployedModules().values().addAll(deploymentManager.getDeployedModules().values());
        for (Map.Entry<ModuleId, Module> entry : deploymentManager.getDeployedModules().entrySet()) {
            finalDeploymentManager.getDeployedModules().put(entry.getKey(), entry.getValue());
        }

        // TODO this is ugly!
        if (extraRepos.size() > 0) {
            for (ModuleLoader ml : finalDeploymentManager.getModuleLoaders()) {
                if (ml instanceof RepositoryLoader) {
                    RepositoryLoader mrml = (RepositoryLoader) ml;
                    for (String r : extraRepos) {
                        mrml.addRepository(r, new URL(r), true, true);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Added new repository: " + r);
                        }
                    }
                    if (quick) {
                        mrml.setCheckSnapshotVersions(false);
                    }
                }
            }
        }
        RepositoryLoader repositoryLoader = (RepositoryLoader)springBasedServer.getServerApplicationContext().getBean(repositoryLoaderBean + "RepositoryModuleLoader");
        if (extraRepos.size() > 0) {
            for (String r : extraRepos) {
                repositoryLoader.addRepository(r, new URL(r), true, true);
                if (logger.isDebugEnabled()) {
                    logger.debug("Added new repository: " + r);
                }
            }
            if (quick) {
                repositoryLoader.setCheckSnapshotVersions(false);
            }
        }
        repositoryLoader.start();

        springBasedServer.start();


        weStarted = true;
        logger.info("Extend (" + version + ") is started at " + springBasedServer.getHomeLocation());
    }

    protected void fixSpring() throws URISyntaxException {
        Module springCore = deploymentManager.getDeployedModules().get(new ModuleId("org.springframework:spring-core:2.5.6:jar"));
        Module springBeans = deploymentManager.getDeployedModules().get(new ModuleId("org.springframework:spring-beans:2.5.6:jar"));
        Module springContext = deploymentManager.getDeployedModules().get(new ModuleId("org.springframework:spring-context:2.5.6:jar"));

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

    public void stop() throws Exception {
        if (socket == null) {
            throw new RuntimeException("Server is not running on the location " + folder.getAbsolutePath());
        }
        out.println("SHUTDOWN");
        out.flush();
        String line = in.readLine();
        logger.info("Stop - " + line);
    }

    protected void shutdown() throws Exception {
        stop();
    }


    protected void start(String moduleId) throws Exception {
        if (weStarted) {
            Module module = finalDeploymentManager.getDeployedModules().get(ModuleId.parseModuleIdString(moduleId));
            if (module != null) {
                try {
                    finalDeploymentManager.start(module);
                } catch (RuntimeException e) {
                    logger.error("Failed to start module " + module.getModuleId(), e);
                }
            }
        } else {
            if (socket == null) {
                throw new RuntimeException("Server is not running on the location " + folder.getAbsolutePath());
            }
            out.println("START " + moduleId);
            out.flush();
            String line = in.readLine();
            logger.info("Start " + moduleId + " - " + line);
        }
    }

    protected void stop(String moduleId) throws Exception {
        if (weStarted) {
            Module module = finalDeploymentManager.getDeployedModules().get(ModuleId.parseModuleIdString(moduleId));
            if (module != null) {
                finalDeploymentManager.stop(module);
            }
        } else {
            if (socket == null) {
                throw new RuntimeException("Server is not running on the location " + folder.getAbsolutePath());
            }
            out.println("STOP " + moduleId);
            out.flush();
            String line = in.readLine();
            logger.info("Stop " + moduleId + " - " + line);
        }
    }


    public void deploy(String location) throws Exception {
        if (weStarted) {
            try {
                URI uri = new URI(location);
                Module module = finalDeploymentManager.load(uri);
                if (module != null) {
                    ModuleId moduleId = module.getModuleId();
                    try {
                        finalDeploymentManager.deploy(moduleId, module);
                    } catch (Exception e) {
                        Extend.info.warn("Caught exception trying to deploy module " + moduleId + ".", e);
                    }
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (socket == null) {
                throw new RuntimeException("Server is not running on the location " + folder.getAbsolutePath());
            }
            out.println("DEPLOY " + location);
            out.flush();
            String line = in.readLine();
            logger.info("Deploy " + location + " - " + line);
        }
    }

    public void undeploy(String moduleId) throws Exception {
        if (weStarted) {
            Module module = finalDeploymentManager.getDeployedModules().get(ModuleId.parseModuleIdString(moduleId));
            if (module != null) {
                try {
                    finalDeploymentManager.undeploy(module);
                } catch (Exception e) {
                    Extend.info.warn("Caught exception trying to undeploy module " + moduleId + ".", e);
                }
            }
        } else {
            if (socket == null) {
                throw new RuntimeException("Server is not running on the location " + folder.getAbsolutePath());
            }
            out.println("UNDEPLOY " + moduleId);
            out.flush();
            String line = in.readLine();
            logger.info("Undeploy " + moduleId + " - " + line);
        }
    }

    protected Socket connectToRunningServer() {
        try {
            File controlFile = new File(folder, "control.port");
            if (controlFile.exists()) {
                logger.debug("Reading control file from " + controlFile.getAbsolutePath());
                FileInputStream fis = new FileInputStream(controlFile);
                try {
                    byte[] buf = new byte[10];
                    int i = fis.read(buf);
                    if (i >= 0) {
                        String s = new String(buf, 0, i);
                        int port = Integer.parseInt(s);
                        Socket socket = new Socket("127.0.0.1", port);
                        return socket;
                    }
                } finally {
                    fis.close();
                }
            } else {
                logger.debug("Control file not present; " + controlFile.getAbsolutePath());
            }
        } catch (IOException ignore) {
        } catch (NumberFormatException ignore) {
        }
        return null;
    }

    public static class BootstrapModule implements Module {

        protected ClassLoader classLoader;

        protected HashSet<Module> dependOnThis = new HashSet<Module>();

        protected HashSet<Module> dependsOn = new HashSet<Module>();

        protected ModuleId moduleId;

        public BootstrapModule(ModuleId moduleId, ClassLoader classLoader) {
            this.classLoader = new BootstrapClassLoader(classLoader);
            this.moduleId = moduleId;
        }

        @Override
        public void create() {
        }

        @Override
        public void destroy() {
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public Set<Module> getDependOnThis() {
            return dependOnThis;
        }

        @Override
        public Set<Module> getDependsOn() {
            return dependsOn;
        }

        @Override
        public ModuleId getModuleId() {
            return moduleId;
        }

        @Override
        public URL getOriginalLocation() {
            return null;
        }

        @Override
        public int getState() {
            return Module.STARTED;
        }

        @Override
        public URL getWorkingLocation() {
            return null;
        }

        @Override
        public void setState(int state) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

    }

    public static class BootstrapClassLoader extends ClassLoader {

        protected Set<ClassLoader> localClassLoaders = new HashSet<ClassLoader>();

        public BootstrapClassLoader(ClassLoader parent) {
            super(parent);
        }
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
            logger.debug("Deployed " + moduleId);
        }

        @Override
        public Module loadAndDeploy(URI uri) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void destroy(Module module) {
            throw new RuntimeException("Not implemented");
        }
//
//        public Module findModule(URI uri, boolean createProvisional) {
//            Module module = (Module) deployedModules.get(uri);
//            if ((module == null) && createProvisional) {
//                throw new RuntimeException("Not implemented");
//            }
//            return module;
//        }

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
            logger.debug("Started " + module.getModuleId());
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
}
