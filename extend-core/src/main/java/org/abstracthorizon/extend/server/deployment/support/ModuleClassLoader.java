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
package org.abstracthorizon.extend.server.deployment.support;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.support.LocalClassLoader;

/**
 * Class loader for {@link Module}.
 * It searches for class in current class loader and then in class loaders
 * of all modules {@link Module} depends on.
 *
 * @author Daniel Sendula
 */
public class ModuleClassLoader extends ClassLoader implements LocalClassLoader {

    /** Context this class loader depends on */
    protected Module context;

    /** Top level - server's class loader */
    protected ClassLoader serverClassLoader;

    /** Should module class loader use thread's context class loader in searching for the resource */
    protected boolean useThreadContextLoader = true;

    /**
     * Constructor
     * @param parent parent class loader
     * @param context module this class loader belongs to.
     */
    public ModuleClassLoader(ClassLoader parent, Module context) {
        super(parent);
        this.context = context;
        serverClassLoader = parent;
    }

    /**
     * Returns should this class loader use context class loader or not
     * @return <code>true</code> if this class loader should use context class loader or not
     */
    public boolean getUseThreadClassLoader() {
        return useThreadContextLoader;
    }

    /**
     * Sets should this class loader use context class loader or not
     * @param useThreadContextLoader flag
     */
    public void setUserThreadClassLoader(boolean useThreadContextLoader) {
        this.useThreadContextLoader = useThreadContextLoader;
    }

    /**
     * Searches for the class in dependent modules and server's class loader at the end.
     * @param name class name
     * @throws ClassNotFoundException
     */
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Set<Module> dependencies = collectAllModules();
        for (Module module : dependencies) {
            try {
                ClassLoader cl = module.getClassLoader();
                if (cl != null) {
                    if (cl instanceof LocalClassLoader) {
                        LocalClassLoader lcl = (LocalClassLoader) cl;
                        return lcl.loadLocalClass(name);
                    } else {
                        Class<?> cls = cl.loadClass(name);
                        return cls;
                    }
                }
            } catch (ClassNotFoundException ignore) {
            } catch (NoClassDefFoundError ex) {
                throw new NoClassDefFoundError(context.getModuleId() + " - " + ex.getMessage());
            }
        }
        if (serverClassLoader != null) {
            return serverClassLoader.loadClass(name);
        } else {
            throw new ClassNotFoundException(name);
        }
    }

    /**
     * Searches for resource in dependent modules and server's class loader at the end.
     * @param name resource name
     */
    public URL findResource(String name) {
        Iterator<Module> it = collectAllModules().iterator();
        while (it.hasNext()) {
            Module c = it.next();
            ClassLoader cl = c.getClassLoader();
            if (cl != null) {
                URL url;
                if (cl instanceof LocalClassLoader) {
                    LocalClassLoader lcl = (LocalClassLoader) cl;
                    url = lcl.getLocalResource(name);
                } else {
                    url = cl.getResource(name);
                }
                if (url != null) {
                    return url;
                }
            }
        }
        if (serverClassLoader != null) {
            return serverClassLoader.getResource(name);
        } else {
            return null;
        }
    }

    /**
     * Searches for resources in dependent modules and server's class loader at the end.
     * @param name resource name
     * @return enumeration containing all URLs for given resource name
     * @throws IOException
     */
    public Enumeration<URL> findResources(String name) throws IOException {

        Enumerations<URL> enumerations = new Enumerations<URL>();

        Iterator<Module> it = collectAllModules().iterator();
        while (it.hasNext()) {
            Module c = (Module) it.next();
            ClassLoader cl = c.getClassLoader();
            if (cl != null) {
                Enumeration<URL> enumeration;
                if (cl instanceof LocalClassLoader) {
                    LocalClassLoader lcl = (LocalClassLoader)cl;
                    enumeration = lcl.getLocalResources(name);
                } else {
                    enumeration = cl.getResources(name);
                }

                enumerations.addEnumeration(enumeration);
            }
        }
        if (serverClassLoader != null) {
            Enumeration<URL> enumeration = serverClassLoader.getResources(name);
            enumerations.addEnumeration(enumeration);
        }

        return enumerations;
    }


    protected Set<Module> collectAllModules() {
        HashSet<Module> modules = new HashSet<Module>();

        if (useThreadContextLoader) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            while ((cl != null) && !(cl instanceof ModuleClassLoader)) {
                ClassLoader pcl = cl.getParent();
                if (pcl == cl) {
                    throw new RuntimeException("HERE!");
                }
                cl = pcl;
            }
            if (cl instanceof ModuleClassLoader) {
                ModuleClassLoader mcl = (ModuleClassLoader)cl;
                collectAllModules(mcl.getModule(), modules);
            }
        }

        collectAllModules(getModule(), modules);
        return modules;
    }

    protected void collectAllModules(Module module, Set<Module> modules) {
        for (Module m : module.getDependsOn()) {
            if (!modules.contains(m)) {
                modules.add(m);
                collectAllModules(m, modules);
            }
        }
    }


    /**
     * Returns module this class loader belongs to
     * @return module this class loader belongs to
     */
    public Module getModule() {
        return context;
    }

    /**
     * Returns server's class loader
     * @return server's class loader
     */
    public ClassLoader getServerClassLoader() {
        return serverClassLoader;
    }

    /**
     * Sets server's class loader
     * @param serverClassLoader server's class loader
     */
    public void setServerClassLoader(ClassLoader serverClassLoader) {
        this.serverClassLoader = serverClassLoader;
    }

    public URL getLocalResource(String name) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Enumeration<URL> getLocalResources(String name) throws IOException {
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    public Class<?> loadLocalClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    /**
     * Class that implements enumeration interface and enumerates through
     * all enumeration passed to it
     *
     * @author Daniel Sendula
     */
    protected class Enumerations<T> implements Enumeration<T> {

        /** List of enumerations */
        protected List<Enumeration<T>> enumerations = new ArrayList<Enumeration<T>>();

        /** Current iterator through enumerations list */
        protected Iterator<Enumeration<T>> iterator;

        /** Current enumeration (as obtained through iterator) */
        protected Enumeration<T> currentEnumeration;

        /**
         * Constructor
         */
        public Enumerations() {
        }

        /**
         * Adds new enumeration to the object
         * @param enumeration enumeration to be added
         */
        public void addEnumeration(Enumeration<T> enumeration) {
            enumerations.add(enumeration);
        }

        /**
         * Returns <code>true</code> if more elements are available
         * @return <code>true</code> if more elements are available
         */
        public boolean hasMoreElements() {
            if (iterator == null) {
                iterator = enumerations.iterator();
                if (!iterator.hasNext()) {
                    return false;
                } else {
                    currentEnumeration = iterator.next();
                }
            }
            while (!currentEnumeration.hasMoreElements()) {
                if (!iterator.hasNext()) {
                    return false;
                } else {
                    currentEnumeration = iterator.next();
                }
            }
            return true;
        }

        /**
         * Returns next element as would current enumeration
         * @return new element
         */
        public T nextElement() {
            return currentEnumeration.nextElement();
        }
    }
}
