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
package org.abstracthorizon.extend.support.spring.deployment;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleUtils;
import org.abstracthorizon.extend.server.deployment.support.ModuleClassLoader;
import org.abstracthorizon.extend.server.deployment.support.ProvisionalModule;
import org.abstracthorizon.extend.server.support.ClassUtils;
import org.abstracthorizon.extend.server.support.URLUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * This class is base class for spring application context modules. That means module represents
 * a kind of application context in spring terms loaded from module's location.
 *
 * @author Daniel Sendula
 */
public abstract class AbstractApplicationContextModule extends AbstractXmlApplicationContext implements Module {

    /** Module id */
    protected ModuleId moduleId;

    /** List of modules this module depends on */
    protected LinkedHashSet<Module> dependsOn = new LinkedHashSet<Module>();

    /** List of modules that depend on this module */
    protected LinkedHashSet<Module> dependOnThis = new LinkedHashSet<Module>();

    /** Internal class loader */
    protected ClassLoader internalClassLoader;

    /** Application context's class loader */
    protected ModuleClassLoader overallClassLoader;

    /** Original location this archive was unpacked from */
    protected URL originalLocation;

    /** Module's location */
    protected URL workingLocation;

    /** Application context xml file */
    protected URL serviceFile;

    /** Module's state */
    protected int state = Module.UNDEFINED;

    /** Deployment manager */
    protected DeploymentManager deploymentManager;

    /**
     * Constructor that creates overall class loader (context's class loader).
     */
    public AbstractApplicationContextModule(ModuleId moduleId) {
        overallClassLoader = new ModuleClassLoader(null, this);
        this.moduleId = moduleId;
    }

    /**
     * Sets deployment manager
     * @param deploymentManager deployment manager
     */
    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    /**
     * Returns deployment manager
     * @return deployment manager
     */
    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    /**
     * Class that tries to load beans from current context or any context this context
     * depends on. It uses {@link AbstractApplicationContextModule#getDependsOn()} list for it.
     */
    public class DeployersDefaultListableBeanFactory extends DefaultListableBeanFactory {

        /**
         * Constructor
         * @param parent parent factory
         */
        public DeployersDefaultListableBeanFactory(BeanFactory parent) {
            super(parent);
        }

        /**
         * This method tries to load bean from current factory and if fails
         * it uses {@link AbstractApplicationContextModule#getDependsOn()} list and
         * tries all contexts from there. In case of failure throws original expcetion.
         * @throws BeanException
         */
        public Object getBean(String beanName) throws BeansException {
            try {
                Object bean = super.getBean(beanName);
                return bean;
            } catch (BeansException e) {
                BeansException originalException = e;
                for (Module module : AbstractApplicationContextModule.this.getDependsOn()) {
                    if (module instanceof ApplicationContext) {
                        ApplicationContext context = (ApplicationContext)module;
                        try {
                            Object bean = context.getBean(beanName);
                            return bean;
                        } catch (BeansException ignore) {
                        }
                    }
                }
                throw originalException;
            }
        }
    }

    /**
     * This method returns config location for this {@link AbstractXmlApplicationContext}
     * instance. It uses {@link #serviceFile} for it converted to a string.
     * @return {@link #serviceFile} converted to a string.
     */
    protected String[] getConfigLocations() {
        if (serviceFile != null) {
            return new String[]{serviceFile.toString()};
        } else {
            return new String[]{};
        }
    }

    /**
     * This method invokes {@link #createInternal()} method. After that it
     * loads modules context from {@link #serviceFile} and invokes
     * {@link Module#create()} method on all dependent modules.
     */
    public void create() {
        if ((state == Module.DEFINED) || (state == Module.WAITING_ON_CREATE) || (state == Module.WAITING_ON_CREATE_TO_START)) {
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Creating module '" + moduleId + "'.");
            }

            if (!getDependsOn().isEmpty()) {
                for (Module m: getDependsOn()) {
                    int st = m.getState();
                    if ((st != Module.CREATED) && (st != Module.STARTED)) {
                        if ((state != WAITING_ON_CREATE) && (state != WAITING_ON_CREATE_TO_START))  {
                            setState(Module.WAITING_ON_CREATE);
                            if (Extend.info.isDebugEnabled()) {
                                Extend.info.debug("Set state of module '" + moduleId + "' to WAITING_ON_DEPENDENCY.");
                            }
                        }
                        if (Extend.info.isDebugEnabled()) {
                            Extend.info.debug("Creating module '" + moduleId + "' failed - module " + m.getModuleId() + " it depends on is not yet created.");
                        }
                        return;
                    }
                }
            }

            if ((overallClassLoader.getServerClassLoader() == null) && (getParent() != null)) {
                // Link overallClassLoader to server's classloader
                // Server server = (Server)getParent().getClass()getBean("Server");
                // overallClassLoader.setServerClassLoader(server.getClassLoader());
                ClassLoader classLoader = ((DefaultResourceLoader)getParent()).getClassLoader();
                overallClassLoader.setServerClassLoader(classLoader);
            }

            ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClassLoader());
            try {
                // Set thread's class loader for refreshinggetParent()

                if (Extend.info.isDebugEnabled()) {
                    Extend.info.debug("Loading context for '" + moduleId + "' module; location=" + workingLocation.toString());
                }
                try {
                    refresh();
                } catch (BeanDefinitionParsingException e) {
                    throw e;
                }

                // Repeat this since after refresh we can get dependencies we didn't have before it
                // (ApplicationContext is really read here!
                if (!getDependsOn().isEmpty()) {
                    for (Module m: getDependsOn()) {
                        int st = m.getState();
                        if ((st != Module.CREATED) && (st != Module.STARTED) && (st != Module.WAITING_ON_START)) {
                            if (state != WAITING_ON_CREATE) {
                                setState(Module.WAITING_ON_CREATE);
                                if (Extend.info.isDebugEnabled()) {
                                    Extend.info.debug("Set state of module '" + moduleId + "' to WAITING_ON_DEPENDENCY.");
                                }
                            }
                            if (Extend.info.isDebugEnabled()) {
                                Extend.info.debug("Creating module '" + moduleId + "' failed - module " + m.getModuleId() + " it depends on is not yet created.");
                            }
                            return;
                        }
                    }
                }

//                SingletonBeanRegistry beanFactory = (SingletonBeanRegistry)getBeanFactory();
//                beanFactory.registerSingleton("_self", this);
//                beanFactory.registerSingleton("_module", this);

                createInternal();

                setState(Module.CREATED);
                if (Extend.info.isDebugEnabled()) {
                    Extend.info.debug("Created module '" + moduleId + "'.");
                }
            } finally {
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }
        } else {
            if (Extend.info.isWarnEnabled()) {
                Extend.info.warn("Creating module '" + moduleId + "' failed - it is not in DEFINED or WAITING_ON_DEPENDENCY state.");
            }
        }
    }

    /**
     * This method calls {@link #startInternal()} and then calls {@link Module#start()} of all dependent modules.
     */
    public void start() {
        if (state == Module.WAITING_ON_CREATE) {
            setState(Module.WAITING_ON_CREATE_TO_START);
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Set state of module '" + moduleId + "' to WAITING_ON_CREATE_TO_START.");
            }
        } else if ((state == Module.CREATED) || (state == Module.WAITING_ON_START)) {
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Starting module '" + moduleId + "'.");
            }
            if (!getDependsOn().isEmpty()) {
                for (Module m: getDependsOn()) {
                    int st = m.getState();
                    if (st != Module.STARTED) {
                        if (state != WAITING_ON_START) {
                            setState(Module.WAITING_ON_START);
                            if (Extend.info.isDebugEnabled()) {
                                Extend.info.debug("Set state of module '" + moduleId + "' to WAITING_ON_START.");
                            }
                        }
                        if (Extend.info.isDebugEnabled()) {
                            Extend.info.debug("Starting module '" + moduleId + "' failed - module " + m.getModuleId() + " it depends on is not yet started.");
                        }
                        return;
                    }
                }
            }

            ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClassLoader());
            try {
                // Add starting of all beans that do have 'start' method
                startInternal();
                setState(Module.STARTED);
            } finally {
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Set state of module '" + moduleId + "' to STARTED.");
            }
        } else {
            if (Extend.info.isWarnEnabled()) {
                Extend.info.warn("Starting module '" + moduleId + "' failed - it is not in CREATED state.");
            }
        }
    }

    /**
     * This method calls {@link #stopInternal()} and then calls {@link Module#stop()} of all dependent modules.
     * If module's state is {@link Module#WAITING_ON_START} then only state of module is updated (to {@link Module#CREATED}.
     */
    public void stop() {
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("Stopping module '" + moduleId + "'.");
        }
        if (state == Module.STARTED) {
            ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClassLoader());
            try {
            // Stop all depended beans
                stopInternal();
                setState(Module.CREATED);
            } finally {
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Set state of module '" + moduleId + "' to CREATED.");
            }
        } else if (state == Module.WAITING_ON_START) {
            setState(Module.CREATED);
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Set state of module '" + moduleId + "' to CREATED.");
            }
        } else if (state == Module.WAITING_ON_CREATE_TO_START) {
            setState(Module.WAITING_ON_CREATE);
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Set state of module '" + moduleId + "' to WAITING_ON_CREATE.");
            }
        } else {
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Stopping module '" + moduleId + "' failed - it is not in STARTED state.");
            }
        }
    }

    /**
     * This method calls {@link #stopInternal()} and then calls {@link Module#stop()} of all dependent modules.
     * At last it closes this application context releasing resources.
     * If module's state is {@link Module#WAITING_ON_CREATE} then only state of module is updated (to {@link Module#DEFINED}.
     */
    public void destroy() {
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("Destroying module '" + moduleId + "'.");
        }
        if ((state == Module.CREATED) || (state == Module.WAITING_ON_START)) {
            ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClassLoader());
            try {
                destroyInternal();
                // Destroy all dependent beans
                close();
                setState(Module.DEFINED);
            } finally {
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Set state of module '" + moduleId + "' to DEFINED.");
            }
        } else if  ((state == Module.WAITING_ON_CREATE) || (state == Module.WAITING_ON_CREATE_TO_START)) {
            setState(Module.DEFINED);
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Set state of module '" + moduleId + "' to DEFINED.");
            }
        } else {
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Destroying module '" + moduleId + "' failed - it not in CREATED or WAITING_ON_DEPENDENCY state.");
            }
        }
    }

    /**
     * This method is to be implemented by extension. It is called when {@link #create()} method is called
     */
    protected abstract void createInternal();

    /**
     * This method is to be implemented by extension. It is called when {@link #start()} method is called
     */
    protected abstract void startInternal();

    /**
     * This method is to be implemented by extension. It is called when {@link #stop()} method is called
     */
    protected abstract void stopInternal();

    /**
     * This method is to be implemented by extension. It is called when {@link #destroy()} method is called
     */
    protected abstract void destroyInternal();

    /**
     * Sets up parser for this application context.
     * This implementation uses {@link ApplicationContextModuleXmlParser} without validation
     * with {@link #internalClassLoader}.
     */
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader xmlbeandefinitionreader) {
        xmlbeandefinitionreader.setDocumentReaderClass(ApplicationContextModuleXmlParser.class);
        xmlbeandefinitionreader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        xmlbeandefinitionreader.setBeanClassLoader(internalClassLoader);
    }


    /**
     * This method is called by parser to process dependencies it collected before
     * rest of bean information is processed by the parser.
     * @param dependencies list of dependencies (as Strings).
     * @return <code>true</code> if all dependencies are resolved positively (modules are already defined). If
     * any of modules is missing <code>false</code> is going to be returned.
     */
    protected boolean processDependencies(Dependency delegateTo, List<Dependency> dependencies) {
        // TODO check for circular depedencies

        boolean okToProceed = true;

        if (deploymentManager == null) {
            ApplicationContext parent = getParent();
            deploymentManager = (DeploymentManager)parent.getBean(DeploymentManager.DEPLOYMENT_MANAGER_DEFAULT_NAME); // TODO
        }

        if (delegateTo != null) {
            try {
                Module m = null;
                ModuleId dependencyId = null;
                URI uri = null;


                if (delegateTo.isUri()) {
                    uri = new URI(delegateTo.getValue());
                    dependencyId = deploymentManager.toModuleId(uri);
                } else {
                    dependencyId = new ModuleId(delegateTo.getValue());
                    uri = new URI("repo:maven:" + delegateTo.getValue());
                }

                m = deploymentManager.getDeployedModules().get(dependencyId);
                if (m == null) {
                    if (!delegateTo.isOptional()) {
                        m = deploymentManager.load(uri);
                        if (m != null) {
                            deploymentManager.deploy(dependencyId, m);
                        } else {
                            if (delegateTo.isUri()) {
                                m = new ProvisionalModule(uri, dependencyId);
                            } else {
                                m = new ProvisionalModule(null, dependencyId);
                            }
                            deploymentManager.deploy(dependencyId, m);
                        }
                    }
                }

                if (m != null) {
                    // TODO circular reference?!
                    getDependOnThis().add(m);
                    m.getDependsOn().add(this);
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException("Bad URI for dependecy; " + delegateTo.getValue(), e);
            }

        }

        for (Dependency dependency: dependencies) {
            try {
                Module m = null;
                ModuleId dependencyId = null;
                URI uri = null;


                if (dependency.isUri()) {
                    uri = new URI(dependency.getValue());
                    dependencyId = deploymentManager.toModuleId(uri);
                } else {
                    dependencyId = new ModuleId(dependency.getValue());
                    uri = new URI("repo:maven:" + dependency.getValue());
                }

                m = deploymentManager.getDeployedModules().get(dependencyId);
                if (m == null) {
                    if (!dependency.isOptional()) {
                        m = deploymentManager.load(uri);
                        if (m != null) {
                            deploymentManager.deploy(dependencyId, m);
                        } else {
                            if (dependency.isUri()) {
                                m = new ProvisionalModule(uri, dependencyId);
                            } else {
                                m = new ProvisionalModule(null, dependencyId);
                            }
                            deploymentManager.deploy(dependencyId, m);
                        }
                    }
                }

                if (m != null) {
                    getDependsOn().add(m);
                    m.getDependOnThis().add(this);
                    int state = m.getState();
                    if ((state != Module.CREATED) && (state != Module.STARTED) && (state != Module.WAITING_ON_START)) {
                        Extend.info.debug("Module " + getModuleId() + " cannot be started or created because it waits on " + m.getModuleId() + " which is in state " + ModuleUtils.stateAsString(m.getState()));
                        okToProceed = false;
                    }
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException("Bad URI for dependecy; " + dependency.getValue(), e);
            }
        }
        if (!okToProceed) {
            if ((state != WAITING_ON_CREATE) && (state != WAITING_ON_CREATE_TO_START)) {
                setState(Module.WAITING_ON_CREATE);
            }
        }
        return okToProceed;
    }

    /**
     * Returns original location this archive was unpacked from.
     * This value is going to be the same as {@link #getWorkingLocation()} if archive is an directory.
     * @return original location
     */
    public URL getOriginalLocation() {
        return originalLocation;
    }

    /**
     * Sets original location
     * @param originalLocation original location
     */
    public void setOriginalLocation(URL originalLocation) {
        this.originalLocation = originalLocation;
    }

    /**
     * Returns the location.
     * @return the location.
     */
    public URL getWorkingLocation() {
        return workingLocation;
    }


    /**
     * Sets the location. Location is top directory of this module. All module's
     * files are under it.
     * @param location the location to set.
     * @throws RuntimeException in case of IOException
     */
    public void setLocation(URL location) {
        this.workingLocation = location;
        try {
//            File path = new File(location.getFile());
//            String name = path.getName();
//            int i = name.lastIndexOf('.');
//            if (i >= 0) {
//                setName(name.substring(0, i));
//            } else {
//                setName(name);
//            }
            createClassLoaders();
            serviceFile = createServiceFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates service file URL
     * @return service file URL
     * @throws IOException
     */
    protected URL createServiceFile() throws IOException {
        if (workingLocation.getFile().endsWith(".xml")) {
            return workingLocation;
        } else {
            String serviceFileName = getContextFileName();
            if (serviceFileName != null) {
                URL sf = URLUtils.add(workingLocation, serviceFileName);
                if (URLUtils.exists(sf)) {
                    return sf;
                }
            }
            return null;
        }
    }

    /**
     * Returns service file url
     * @return service file url
     */
    public URL getServiceFile() {
        return serviceFile;
    }

    /**
     * This is template method for name of application context xml file.
     * This implementation returns &quot;service.xml&quot; but can be redefined by subclasses.
     * @return &quot;service.xml&quot;
     */
    protected String getContextFileName() {
        return "service.xml";
    }

    /**
     * Called when location is set for class loader to be created.
     *
     */
    protected void createClassLoaders() {
        if (workingLocation.getFile().endsWith(".xml")) {
            internalClassLoader = overallClassLoader;
        } else {
            internalClassLoader = ClassUtils.createClassLoader(overallClassLoader, workingLocation);
        }
    }

    /**
     * Return's module's class loader (top most {@link #internalClassLoader}).
     * @return module's class loader
     */
    public ClassLoader getClassLoader() {
        return internalClassLoader;
    }

    /**
     * Returns set of modules this module depends on.
     * @return set of modules this module depends on
     */
    public Set<Module> getDependsOn() {
        return dependsOn;
    }

    /**
     * Returns set of modules that depend on this module.
     * @return set of modules that depend on this module
     */
    public Set<Module> getDependOnThis() {
        return dependOnThis;
    }

    /**
     * Sets module's name
     * @param moduleId module's id
     */
    public void setModuleId(ModuleId moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * Returns module's id
     * @return module's id
     */
    public ModuleId getModuleId() {
        return moduleId;
    }

    /**
     * Returns module's name followed by colon and string representation of
     * {@link AbstractXmlApplicationContext} object.
     * @return this object as a string
     */
    public String toString() {
        return moduleId + ":" + super.toString();
    }

    /**
     * Creates {@link DeployersDefaultListableBeanFactory} instead of
     * original.
     * @return new instance of {@link DeployersDefaultListableBeanFactory}
     */
    public DefaultListableBeanFactory createBeanFactory() {
        //super.createBeanFactory();
        return new DeployersDefaultListableBeanFactory(getInternalParentBeanFactory());
    }

    /**
     * Returns module's state
     * @return module's state
     */
    public int getState() {
        return state;
    }

    /**
     * Sets module's state
     * @param state module's state
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * Returns module's state as a string
     * @return module's state as a string
     */
    public String getStateAsString() {
        return ModuleUtils.stateAsString(state);
    }

    /**
     * Returns resource for given location
     * @param location location
     * @return resource
     */
    @Override
    public Resource getResource(String location) {
        if (location.startsWith("server-")) {
            return getParent().getResource(location);
        }
        return super.getResource(location);
    }

    /**
     * Compares two modules and returns <code>true</code> if names are the same
     * @param o other object
     * @return <code>true</code> if names are the same
     */
    public boolean equals(Object o) {
        if ((o instanceof Module) && getModuleId().equals(((Module)o).getModuleId())) {
            return true;
        }
        return super.equals(o);
    }

}
