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
package org.abstracthorizon.extend.support.spring.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.support.RedeployURLScanner;
import org.abstracthorizon.extend.server.support.ArchiveUtils;
import org.abstracthorizon.extend.support.spring.deployment.AbstractApplicationContextModule;

/**
 * Service module loader. It loads &quot;service.xml&quot; file as
 * spring's application context xml configuration file.
 * TODO: explain donwloading and unpacking of archives
 *
 * @author Daniel Sendula
 */
public class ServiceModuleLoader extends SpringAbstractServiceModuleLoader {

    /** Initial buffer size */
    public static final int INITIAL_BUFFER_SIZE = 10240;

    /** Buffer for downlaod size */
    protected int bufferSize = INITIAL_BUFFER_SIZE;

    /** Redeployment URL scanner */
    protected RedeployURLScanner redeployURLScanner;

    /**
     * Empty constructor
     */
    public ServiceModuleLoader() {
    }

    /**
     * Translates URI to moduleId
     * @param uri uri
     * @return module id or <code>null</code>
     */
    public ModuleId toModuleId(URI uri) {
        if (uri.getPath() != null) {
            ModuleId moduleId = ModuleId.createModuleIdFromFileName(uri.getPath());
            return moduleId;
        } else {
            return null;
        }
    }

    /**
     * Creates {@link ServiceApplicationContextModule} and initialises it.
     * <p>This implementation checks if file or directory is supplied as URI (directory has distinct &quot;/&quot;
     * at the end of the file path). If it is directory then it has to be on filesystem - url with &quot;file&quot;
     * protocol. If it is a file then it is assimed to be jar file. If file is remote file (url's protocol is not
     * &quot;file&quot;) then file is firstly downloaded.
     *
     * @param uri url for module to be loaded from
     * @return module initialised {@link ServiceApplicationContextModule}
     */
    public Module load(URI uri) {
        ModuleId moduleId = toModuleId(uri);
        return loadAs(uri, moduleId);
    }
    

    /**
     * Creates {@link ServiceApplicationContextModule} and initialises it.
     * <p>This implementation checks if file or directory is supplied as URI (directory has distinct &quot;/&quot;
     * at the end of the file path). If it is directory then it has to be on filesystem - url with &quot;file&quot;
     * protocol. If it is a file then it is assimed to be jar file. If file is remote file (url's protocol is not
     * &quot;file&quot;) then file is firstly downloaded.
     *
     * @param uri url for module to be loaded from
     * @param moduleId module id
     * @return module initialised {@link ServiceApplicationContextModule}
     */
    public Module loadAs(URI uri, ModuleId moduleId) {
        try {
            URL originalLocation = uri.toURL();
            URL location = uri.toURL();
            String path;
            try {
                path = URLDecoder.decode(originalLocation.getFile(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            if (path.endsWith(".xml")) {
                if (!"file".equals(originalLocation.getProtocol())) {
                    throw new UnsupportedOperationException(originalLocation.getProtocol() + " is not supported by this module loader");
                }
            } else if (path.endsWith("/")) {
                // If we have directory passed in and it is not of file protocol then we do not know how to handle it
                if (!"file".equals(originalLocation.getProtocol())) {
                    throw new UnsupportedOperationException(originalLocation.getProtocol() + " is not supported by this module loader");
                }
            } else {
                // If we have a file passed in we will assume it is archive file:
                // if file is stored remotely (i.e. not on url with file prefix) then we will download it first.
                // Once accessible file we will unpacked it and work from that copy.
                try {
                    File archiveFile = new File(URLDecoder.decode(originalLocation.getFile(), "UTF-8"));
                    String archiveFileName = archiveFile.getName();
                    if (!"file".equals(originalLocation.getProtocol())) {
                        File tmp = File.createTempFile("sas_", archiveFileName);
                        ArchiveUtils.downloadArchive(originalLocation, tmp, getBufferSize());
                        archiveFile = tmp;
                    }
                    File archiveDir = new File(System.getProperty("java.io.tmpdir"), archiveFileName);
                    ArchiveUtils.unpackArchive(archiveFile, archiveDir, getBufferSize());
                    location = archiveDir.toURI().toURL();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            AbstractApplicationContextModule service = createModule(location);
            if (moduleId != null) {
                service.setModuleId(moduleId);
            }
            service.setParent(root);
            service.setLocation(location);
            service.setOriginalLocation(originalLocation);

            //            String name = location.getPath();
//            int i = name.lastIndexOf('/');
//            if (i >= 0) {
//                name = name.substring(i + 1);
//            }
//            service.setName(name);
            
            preDefinitionProcessing(service);
            service.setState(Module.DEFINED);
            postDefinitionProcessing(service);
            if (redeployURLScanner != null) {
                addFilesForScanning(service);
            }
            return service;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Creates new module that is extension of {@link AbstractApplicationContextModule}
     * @param url url for the module
     * @return newly created module
     */
    protected AbstractApplicationContextModule createModule(URL url) {
        try {
            return new ServiceApplicationContextModule(toModuleId(url.toURI()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called before module is pronounced defined
     * @param service service
     */
    protected void preDefinitionProcessing(AbstractApplicationContextModule service) {
    }

    /**
     * Called after module is defined
     * @param service service
     */
    protected void postDefinitionProcessing(AbstractApplicationContextModule service) {
    }

    /**
     * Hook method that collects all important files from newly created module and
     * adds then to the URL redeployment scanner.
     *
     */
    protected void addFilesForScanning(AbstractApplicationContextModule module) {
        if (redeployURLScanner != null) {
            // TODO should this be set in case of working location only?!
            if (module.getOriginalLocation().equals(module.getWorkingLocation())) {
                URL serviceFileURL = module.getServiceFile();
                if (serviceFileURL != null) {
                    redeployURLScanner.addURL(serviceFileURL, module);
                }
            }
        }
    }

    /**
     * Returns redeploy URL scanner
     * @return redeploy URL scanner
     */
    public RedeployURLScanner getRedeployURLScanner() {
        return redeployURLScanner;
    }

    /**
     * Sets redeploy URL scanner
     * @param redeployURLScanner redeploy URL scanner
     */
    public void setRedeployURLScanner(RedeployURLScanner redeployURLScanner) {
        this.redeployURLScanner = redeployURLScanner;
    }

    /**
     * Returns buffer size for downloading
     * @return buffer size for downloading
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Sets buffer size for downloading
     * @param bufferSize buffer size for downloading
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
