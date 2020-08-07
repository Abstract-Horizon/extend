package org.abstracthorizon.extend.repository.maven;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleLoader;
import org.abstracthorizon.extend.server.deployment.support.JarModuleLoader;
import org.abstracthorizon.extend.server.support.EnhancedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TryIt {

    public static Logger logger = LoggerFactory.getLogger(TryIt.class);

    public static void main(String[] args) throws Exception {

        DeploymentManager deploymentManager = new DeploymentManagerStub();


        ParallelRepositoryModuleLoader loader = new ParallelRepositoryModuleLoader();
        loader.setCheckSnapshotVersions(true);
        loader.setDeploymentManager(deploymentManager);

        JarModuleLoader jarModuleLoader = new JarModuleLoader();
        jarModuleLoader.setDeploymentManager(deploymentManager);
        jarModuleLoader.getExtensions().add(".jar");
        deploymentManager.getModuleLoaders().add(jarModuleLoader);

        URI uri = new URI("repo:maven:org.abstracthorizon.extend.support:extend-server-control-client:1.2:jar");

        loader.start();

        long now = System.currentTimeMillis();
        logger.info("Loading module: " + uri + " ============================================================================================");
        loader.load(uri);
        logger.info("Module loaded: " + uri + " ============================================================================================");
        logger.info("");
        logger.info("");
        logger.info("Lasted " + (System.currentTimeMillis() - now) + "ms");
    }


    public static class DeploymentManagerStub implements DeploymentManager {

        protected EnhancedMap<ModuleId, Module> deployedModules = new EnhancedMap<ModuleId, Module>();

        protected LinkedHashSet<ModuleLoader> moduleLoaders = new LinkedHashSet<ModuleLoader>();

        public void create(Module module) {
            module.create();
        }

        public void deploy(ModuleId moduleId, Module module) {
            deployedModules.put(moduleId, module);
            create(module);
            if ((module.getState() == Module.CREATED) || (module.getState() == Module.WAITING_ON_CREATE)) {
                start(module);
            }
            logger.debug("Deployed " + moduleId);
        }

        public void destroy(Module module) {
            throw new RuntimeException("Not implemented");
        }

        public Module loadAndDeploy(URI uri) {
            Module m = deployedModules.get(toModuleId(uri));
            if (m == null) {
                m = load(uri);
                if (m != null) {
                    ModuleId moduleId = m.getModuleId();
                    if (!deployedModules.containsValue(m)) {
                        deploy(moduleId, m);
                    } else if (!deployedModules.containsKey(moduleId)) {
                        deployedModules.put(moduleId, m);
                    }
                }
            }
            return m;
        }

        public EnhancedMap<ModuleId, Module> getDeployedModules() {
            return deployedModules;
        }

        public Set<ModuleLoader> getModuleLoaders() {
            return moduleLoaders;
        }

        public void redeploy(Module module) {
            throw new RuntimeException("Not implemented");
        }

        public void setModuleLoaders(Set<ModuleLoader> moduleLoaders) {
            throw new RuntimeException("Not implemented");
        }

        public void start(Module module) {
            module.start();
            logger.debug("Started " + module.getModuleId());
        }

        public void stop(Module module) {
            throw new RuntimeException("Not implemented");
        }

        public void undeploy(Module module) {
            throw new RuntimeException("Not implemented");
        }

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
