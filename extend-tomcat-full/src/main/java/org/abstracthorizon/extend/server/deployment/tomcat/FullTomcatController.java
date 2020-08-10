package org.abstracthorizon.extend.server.deployment.tomcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.abstracthorizon.danube.http.util.Base64;
import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleLoader;
import org.abstracthorizon.extend.server.deployment.support.AbstractModule;
import org.abstracthorizon.extend.server.support.ArchiveUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FullTomcatController {

    public static final Logger logger = LoggerFactory.getLogger(FullTomcatController.class);
    
    private DeploymentManager deploymentManager;
    private File catalinaHome;
    private URI tomcatVersionURI;
    private boolean controlTomcatStop = true;
    private boolean forceControlTomcatStop = false;
    private boolean forceControlTomcatStopSet = false;
    private boolean restartTomcatOnDeploy = false;
    private boolean forceRestartTomcatOnDeploy = false;
    private boolean forceRestartTomcatOnDeploySet = false;
    private int serverShutdownPort = 8005;
    private int forceServerShutdownPort = -1;
    private String serverShutdownInterfaceAddress = "localhost";
    private String forceServerShutdownInterfaceAddres = null;
    private String serverShutdownValue = "SHUTDOWN";
    private String forceServerShutdownValue = null;
    
    private int serverManagerPort = -1;
    private int forceServerManagerPort;
    private String serverManagerInterfaceAddress = "localhost";
    private String forceServerManagerInterfaceAddres = null;

    private boolean deployUsingManager = false;
    private boolean forceDeployUsingManager = false;
    private boolean forceDeployUsingManagerSet = false;
    
    private String collectedRunningHostName = "localhost";
    private int collectedRunningPort = -1;
    private boolean collectedOnlyOnSSL = false;
    
    private String managerUser = null;
    private String managerPassword = null;
    
    public static final int CONSOLE_OUTPUT_TIMEOUT = 10000;
    private int consoleOutputTimeout = CONSOLE_OUTPUT_TIMEOUT;
    
    public static final int STARTUP_TIMEOUT = 30000;
    private int startupTimeout = STARTUP_TIMEOUT;

    public static final int DELAY_AFTER_SHUTDOWN = 1000;
    private int delayAfterShutdown = DELAY_AFTER_SHUTDOWN;

    private int overridePort = -1;
    private int overrideShutdownPort = -1;
    
    public static final String DEPLOYED_MODULES_EXTEND_DESCRIPTOR = "extend-deployed-modules.txt";
    
    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }
    
    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }
    
    public File getCatalinaHome() {
        return catalinaHome;
    }
    
    public void setCatalinaHome(File catalinaHome) {
        this.catalinaHome = catalinaHome;
    }

    public URI getTomcatArtifact() {
        return tomcatVersionURI;
    }
    
    public void setTomcatArtifact(URI tomcatVersion) {
        this.tomcatVersionURI = tomcatVersion;
    }
    
    public boolean isControlTomcatStop() {
        if (forceControlTomcatStopSet) {
            return forceControlTomcatStop;
        }
        return controlTomcatStop;
    }

    public void setControlTomcatStop(boolean controlTomcatStop) {
        this.forceControlTomcatStop = controlTomcatStop;
        forceControlTomcatStop = true;
    }

    public boolean isRestartTomcatOnDeploy() {
        if (forceRestartTomcatOnDeploySet) {
            return forceRestartTomcatOnDeploy;
        }
        return restartTomcatOnDeploy;
    }

    public void setRestartTomcatOnDeploy(boolean restartTomcatOnDeploy) {
        this.forceRestartTomcatOnDeploy = restartTomcatOnDeploy;
        forceRestartTomcatOnDeploySet = true;
    }

    
    public int getServerShutdownPort() {
        if (forceServerShutdownPort > 0) {
            return forceServerShutdownPort;
        }
        return serverShutdownPort;
    }

    public void setServerShutdownPort(int forceServerShutdownPort) {
        this.forceServerShutdownPort = forceServerShutdownPort;
    }

    public String getServerShutdownInterfaceAddress() {
        if (forceServerShutdownInterfaceAddres != null) {
            return forceServerShutdownInterfaceAddres;
        }
        return serverShutdownInterfaceAddress;
    }

    public void setServerShutdownInterfaceAddress(String serverShutdownInterfaceAddress) {
        this.serverShutdownInterfaceAddress = serverShutdownInterfaceAddress;
    }

    public String getServerShudownValue() {
        if (forceServerShutdownValue != null) {
            return forceServerShutdownValue;
        }
        return serverShutdownValue;
    }
    
    public void setServerShutdownValue(String serverShutdownValue) {
        this.forceServerShutdownValue = serverShutdownValue;
    }
    
    public int getServerManagerPort() {
        if (forceServerManagerPort > 0) {
            return forceServerManagerPort;
        }
        return serverManagerPort;
    }

    public void setServerManagerPort(int forceServerManagerPort) {
        this.forceServerManagerPort = forceServerManagerPort;
    }

    public String getServerManagerInterfaceAddress() {
        if (forceServerManagerInterfaceAddres != null) {
            return forceServerManagerInterfaceAddres;
        }
        return serverManagerInterfaceAddress;
    }

    public void setServerManagerInterfaceAddress(String serverInterfaceAddress) {
        this.serverManagerInterfaceAddress = serverInterfaceAddress;
    }

    public boolean isDeployUsingManager() {
        if (forceDeployUsingManagerSet) {
            return forceDeployUsingManager;
        }
        return deployUsingManager;
    }
    
    public void setDeployUsingManager(boolean deployUsingManager) {
        this.forceDeployUsingManager = deployUsingManager;
        forceDeployUsingManagerSet = true;
    }

    public String getManagerUser() {
        return managerUser;
    }

    public void setManagerUser(String managerUser) {
        this.managerUser = managerUser;
    }

    public String getManagerPassword() {
        return managerPassword;
    }

    public void setManagerPassword(String managerPassword) {
        this.managerPassword = managerPassword;
    }

    public int getConsoleOutputTimeout() {
        return consoleOutputTimeout;
    }

    public void setConsoleOutputTimeout(int consoleOutputTimeout) {
        this.consoleOutputTimeout = consoleOutputTimeout;
    }
    
    public int getStartupTimeout() {
        return startupTimeout;
    }
    
    public void setStartupTimeout(int startupTimeout) {
        this.startupTimeout = startupTimeout;
    }
    
    public int getDelayAfterShutdown() {
        return delayAfterShutdown;
    }
    
    public void setDelayAfterShutdown(int delayAfterShutdown) {
        this.delayAfterShutdown = delayAfterShutdown;
    }
    
    public int getOverridePort() {
        return overridePort;
    }
    
    public void setOverridePort(int port) {
        this.overridePort = port;
    }
    
    public int getOverrideShutdownPort() {
        return overrideShutdownPort;
    }
    
    public void setOverrideShutdownPort(int port) {
        this.overrideShutdownPort = port;
    }
    
    public void create() throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
        if (catalinaHome == null) {
            throw new NullPointerException("Catalina Home must be populated");
        }
        if (deploymentManager == null) {
            throw new NullPointerException("DeploymentManager must be supplied");
        }
        // Ensure tomcat is downloaded and unpacked
        File configDir = new File(getCatalinaHome(), "conf");
        if (!getCatalinaHome().exists() || !configDir.exists()) {
            ModuleLoader tempModuleLoader = new ZipFileModuleLoader(".zip");
            getDeploymentManager().getModuleLoaders().add(tempModuleLoader);
            Module module;
            try {
                module = getDeploymentManager().load(getTomcatArtifact());
                if (module == null) {
                    throw new RuntimeException(new FileNotFoundException("Cannot obtain tomcat zip file: " + tomcatVersionURI.toString()));
                }
            } finally {
                getDeploymentManager().getModuleLoaders().remove(tempModuleLoader);
            }
            URL workingLocationURL = module.getWorkingLocation();
            File workingLocationFile;
            try {
                workingLocationFile = new File(URLDecoder.decode(workingLocationURL.getFile(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            if (!getCatalinaHome().exists() && !getCatalinaHome().mkdirs()) {
                throw new RuntimeException(new IOException("Cannot create directory for catalina home: " + catalinaHome.getAbsolutePath()));
            }
            ArchiveUtils.unpackArchive(workingLocationFile, catalinaHome, true);

            String os = System.getProperty("os.name");
            boolean onWindows = false;
            if (os.startsWith("Windows")) {
                onWindows = true;
            }
            if (!onWindows) {
                File binDir = new File(getCatalinaHome(), "bin");
                if (binDir.exists()) {
                    File[] files = binDir.listFiles();
                    for (File f : files) {
                        if (f.getName().endsWith(".sh")) {
                            Runtime.getRuntime().exec("chmod u+x " + f.getAbsolutePath());
                        }
                    }
                }
            }
        }
        collectTomcatInformation();
    }

    public void start() throws IOException {
        Extend.info.debug("Starting instance of tomcat at " + getServerManagerInterfaceAddress() + ":" + getServerManagerPort());
        if (!isRunning()) {
            removeExtendWars(); // just in case extend was terminated prematurely
            startTomcat();
            Extend.info.info("Started instance of tomcat at " + getServerManagerInterfaceAddress() + ":" + getServerManagerPort());
        } else {
            Extend.info.info("Instance of tomcat at " + getServerManagerInterfaceAddress() + ":" + getServerManagerPort() + " is already running.");
        }
    }

    public void stop() {
        if (isControlTomcatStop()) {
            try {
                stopTomcat();
            } catch (IOException e) {
                Extend.info.error("Could not stop tomcat", e);
            }
            removeExtendWars(); // After stopping tomcat, remove all modules extend deployed to tomcat
        }
    }

    public void deploy(FullTomcatWebApplicationContext module) throws IOException {
        String contextPath = module.getContextPath();
        if (contextPath.startsWith("/")) {
            contextPath = contextPath.substring(1);
        }
        
        Map<String, ModuleId> existingModules = readDeployedModulesExtendDescriptor();
        
        ModuleId existingModuleId = existingModules.get(contextPath);
        if (existingModuleId != null) {
            if (existingModuleId.equals(module.getModuleId())){
                module.setDeployedHostName(collectedRunningHostName);
                module.setDeployedPort(collectedRunningPort);
                module.setDeployedOnOnlySSL(collectedOnlyOnSSL);
                return; // Already deployed
            } else {
                Extend.info.info("Undeploying existing module " + existingModuleId + " from context path " + contextPath);
                undeploy(contextPath, existingModuleId, null);
            }
        }
        
        if (isRestartTomcatOnDeploy()) {
            stopTomcat();
            startTomcat();
        }
        // String contextPath = module.getModuleId().getArtifactId();
        if (deployUsingManager) {
            
            URL managerDeployURL = new URL("http", getServerManagerInterfaceAddress(),
                    getServerManagerPort(), "/manager/deploy?path=/" + contextPath + "&war=" + module.getWorkingLocation().toString());
            HttpURLConnection connection = (HttpURLConnection)managerDeployURL.openConnection();
            
            String authString = getManagerUser() + ":" + getManagerPassword();
            String authStringEncoded = Base64.encode(authString);
            
            connection.setRequestProperty("Authorization", "Basic " + authStringEncoded);
            
            int statusCode = connection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                org.abstracthorizon.extend.Extend.info.debug("Deployed " + module.getModuleId() + " to context path=" + contextPath + " from working location=" + module.getWorkingLocation());    

                module.setDeployedHostName(collectedRunningHostName);
                module.setDeployedPort(collectedRunningPort);
                module.setDeployedOnOnlySSL(collectedOnlyOnSSL);

                addDeployedModuleToExtendDescriptor(contextPath, module.getModuleId());
            } else {
                String statusMessage = connection.getResponseMessage();
                throw new IOException("Failed to deploy " + module.getModuleId() + " to context path=" + contextPath + " from working location=" + module.getWorkingLocation() + "\nResponse code: " + statusCode + "\n Response message: " + statusMessage);
            }
        } else {
            
            File webapps = new File(getCatalinaHome(), "webapps");
            if (!webapps.exists()) {
                throw new FileNotFoundException("War (file) deployment: Cannot webapps dir is not present! " + webapps.getAbsolutePath());
            }
            File tomcatTemp = new File(getCatalinaHome(), "temp");
            if (!tomcatTemp.exists()) {
                if (!tomcatTemp.mkdirs()) {
                    tomcatTemp = getCatalinaHome();
                }
            }
            String destWarFileName = contextPath + ".war"; 
            File tempDeployFile = new File(tomcatTemp, destWarFileName);
            
            OutputStream os = new FileOutputStream(tempDeployFile);
            try {
                InputStream is = module.getWorkingLocation().openStream();
                try {
                    ArchiveUtils.copy(is, os);
                } finally {
                    is.close();
                }
            } finally {
                os.close();
            }
            File destWarFile = new File(webapps, destWarFileName);
            if (destWarFile.exists()) {
                if (!destWarFile.delete()) {
                    throw new IOException("War (file) deployment: Failed deleting old war file " + destWarFile.getAbsolutePath());
                }
            }
            
            if (!tempDeployFile.renameTo(destWarFile)) {
                throw new IOException("War (file) deployment: Failed renaming " + tempDeployFile.getAbsolutePath() + " to " + destWarFileName);
            }
            org.abstracthorizon.extend.Extend.info.debug("Deployed (file) " + module.getModuleId() + " to context path=" + contextPath + " from working location=" + module.getWorkingLocation());    

            module.setDeployedHostName(collectedRunningHostName);
            module.setDeployedPort(collectedRunningPort);
            module.setDeployedOnOnlySSL(collectedOnlyOnSSL);
            
            addDeployedModuleToExtendDescriptor(contextPath, module.getModuleId());
        }
    }

    public void undeploy(FullTomcatWebApplicationContext module) throws IOException {
        String contextPath = module.getModuleId().getArtifactId();
        ModuleId moduleId = module.getModuleId();
        URL workingLocation = module.getWorkingLocation();
        undeploy(contextPath, moduleId, workingLocation);
    }

    public void undeploy(String contextPath, ModuleId moduleId, URL workingLocation) throws IOException {
        if (deployUsingManager) {
            
            URL managerDeployURL = new URL("http", getServerManagerInterfaceAddress(),
                    getServerManagerPort(), "/manager/undeploy?path=/" + contextPath);
            HttpURLConnection connection = (HttpURLConnection)managerDeployURL.openConnection();
            
            String authString = getManagerUser() + ":" + getManagerPassword();
            String authStringEncoded = Base64.encode(authString);
            
            connection.setRequestProperty("Authorization", "Basic " + authStringEncoded);
            
            int statusCode = connection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                if (workingLocation != null) {
                    org.abstracthorizon.extend.Extend.info.debug("Undeployed (manager) " + moduleId + " to context path=" + contextPath + " from working location=" + workingLocation);
                } else {
                    org.abstracthorizon.extend.Extend.info.debug("Undeployed (manager) " + moduleId + " to context path=" + contextPath);
                }

                removeDeployedModuleToExtendDescriptor(contextPath);
            } else {
                String statusMessage = connection.getResponseMessage();
                if (workingLocation != null) {
                    throw new IOException("War (manager) undeployment: Failed undeploying " + moduleId + " to context path=" + contextPath + " from working location=" + workingLocation + "\nResponse code: " + statusCode + "\n Response message: " + statusMessage);
                } else {
                    throw new IOException("War (manager) undeployment: Failed undeploying " + moduleId + " to context path=" + contextPath + "\nResponse code: " + statusCode + "\n Response message: " + statusMessage);
                }
            }
        } else {
            String destWarFileName = contextPath + ".war";
            
            File webapps = new File(getCatalinaHome(), "webapps");
            if (!webapps.exists()) {
                throw new FileNotFoundException("War (file) undeployment: Cannot webapps dir is not present! " + webapps.getAbsolutePath());
            }
            File destWarFile = new File(webapps, destWarFileName);
            if (destWarFile.exists()) {
                if (!destWarFile.delete()) {
                    throw new IOException("War (file) undeployment: Failed deleting old war file " + destWarFile.getAbsolutePath());
                }
            }
            if (workingLocation != null) {
                org.abstracthorizon.extend.Extend.info.debug("Undeployed (file) " + moduleId + " to context path=" + contextPath + " from working location=" + workingLocation);
            } else {
                org.abstracthorizon.extend.Extend.info.debug("Undeployed (file) " + moduleId + " to context path=" + contextPath);
            }

            removeDeployedModuleToExtendDescriptor(contextPath);
        }
    }

    public boolean isRunning() {
        if (getServerManagerPort() > 0) {
            try {
                Socket socket = new Socket(getServerManagerInterfaceAddress(), getServerManagerPort());
                socket.close();
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }
    
    protected void startTomcat() throws IOException {
        String os = System.getProperty("os.name");
        boolean onWindows = false;
        if (os.startsWith("Windows")) {
            onWindows = true;
        }

        String startScriptName = "startup.sh";
        if (onWindows) {
            startScriptName = "startup.bat";
        }

        boolean hasJDK = true;
        File jdkHomeDir = null;
        File jreHomeDir = new File(System.getProperty("java.home"));
        File bin = new File(jreHomeDir, "bin");
        File javac = new File(bin, "javac");
        if (!javac.exists()) {
            jdkHomeDir = jreHomeDir.getParentFile();
            bin = new File(jdkHomeDir, "bin");
            javac = new File(bin, "javac");
            hasJDK = javac.exists();
        } else {
            jdkHomeDir = jreHomeDir;
        }
        
        HashMap<String, String> envMap = new HashMap<String, String>();
        envMap.putAll(System.getenv());
        envMap.put("CATALINA_HOME", getCatalinaHome().getAbsolutePath());
        if (hasJDK) {
            if (!envMap.containsKey("JAVA_HOME")) {
                envMap.put("JAVA_HOME", jdkHomeDir.getAbsolutePath());
            }
        } else {
            if (!envMap.containsKey("JRE_HOME")) {
                envMap.put("JRE_HOME", jreHomeDir.getAbsolutePath());
            }
        }
        
        if (onWindows) {
            if (!envMap.containsKey("OS")) {
                envMap.put("OS", "Windows_NT");
            }
            if (!envMap.containsKey("SystemRoot")) {
                envMap.put("SystemRoot", "c:\\windows");
            }
        }
        
        
        String[] env = new String[envMap.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            env[i] = entry.getKey() + "=" + entry.getValue();
            i++;
        }

        File binDir = new File(getCatalinaHome(), "bin");
        File startupScriptFile = new File(binDir, startScriptName);
        // Process process = Runtime.getRuntime().exec(new String[]{startupScriptFile.getAbsolutePath()}, env);

        ArrayList<String> startCommands = new ArrayList<String>();
        if (onWindows) {
            startCommands.add("cmd");
            startCommands.add("start");
            startCommands.add("/c");
        } else {
            startCommands.add("sh");
        }
        startCommands.add(startupScriptFile.getAbsolutePath());
        
        ProcessBuilder processBuilder = new ProcessBuilder(startCommands);
        processBuilder.environment().putAll(envMap);
        
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        StringWriter result = new StringWriter();
        InputStream inputStream = process.getInputStream();
        byte[] buf = new byte[10240];
        boolean loop = true;
        
        int exitValue = -9999;
        long now = System.currentTimeMillis();
        while (loop) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignore) {
            }
            int available = inputStream.available();
            if (available > 0) {
                int r = inputStream.read(buf, 0, available);
                if (r > 0) {
                    String s = new String(buf, 0, r);
                    result.write(s);
                    if (logger.isDebugEnabled()) {
                        logger.debug(s);
                    }
                } else {
                    loop = false;
                }
            } else {
                try {
                    exitValue = process.exitValue();
                    loop = false;
                } catch (IllegalThreadStateException ignore) {
                }
            }
            long deltaTime = System.currentTimeMillis() - now;
            if ((getConsoleOutputTimeout() > 0) && (deltaTime > getConsoleOutputTimeout())) {
                loop = false;
                // throw new IOException("Wait process timed out after " + deltaTime + "ms");
            }
        }

        if (exitValue == 0) {
            logger.info("Successfully executed " + startScriptName);
            if (logger.isDebugEnabled()) {
                logger.debug("Env: ");
                for (String s : env) {
                    logger.debug(s);
                }
                logger.debug(result.toString());
            }

        } else if (exitValue == -9999) {
            Extend.info.debug("Tomcat startup script is still running; script " + startScriptName);
            if (logger.isDebugEnabled()) {
                logger.debug("Env: ");
                for (String s : env) {
                    logger.debug(s);
                }
                logger.debug(result.toString());
            }
        } else {
            if (onWindows && exitValue == 1) {
                Extend.info.warn("Windows cmd returned exit value 1 running script " + startScriptName);
                // Windows may have exit value 1... Strange...
            } else {
                StringBuffer envString = new StringBuffer();
                for (String s : env) {
                    envString.append(s).append('\n');
                }
                throw new IOException("Failed to start " + startScriptName + ";\nExit value: " + exitValue + "\nOutput:\n" + result.toString() + "\n\nEnvironment: " + envString.toString());
            }
        }
        
        now = System.currentTimeMillis();
        boolean started = false;
        while (!started && System.currentTimeMillis() - now < getStartupTimeout()) {
            try {
                Socket socket = new Socket(getServerShutdownInterfaceAddress(), getServerShutdownPort());
                socket.close();
                started = true;
            } catch (IOException ignore) {
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }
        }
        if (!started) {
            Extend.info.warn("Tomcat didn't start within specified timeout of " + getStartupTimeout() + "ms or shutdown port " + getServerShutdownInterfaceAddress() + ":" + getServerShutdownPort() + " is not enabled.");
        }
    }
    
    protected void stopTomcat() throws UnknownHostException, IOException {
        try {
            Socket socket = new Socket(getServerShutdownInterfaceAddress(), getServerShutdownPort());
            OutputStream os = socket.getOutputStream();
            os.write(getServerShudownValue().getBytes());
            os.write("\n".getBytes());
            os.flush();
            os.close();
            socket.close();
            if (getDelayAfterShutdown() > 0) {
                try {
                    Thread.sleep(getDelayAfterShutdown());
                } catch (InterruptedException ignore) {
                }
            }
        } catch (ConnectException e) {
            Extend.info.warn("Failed to connect tomcat on port " + getServerShutdownInterfaceAddress() + ":" + getServerShutdownPort() + " to shut it down. "
                    + "Tomcat instance on " + getServerManagerInterfaceAddress() + ":" + getServerManagerPort());
            throw e;
        }
    }
    
    protected void removeExtendWars() {
        try {
            File webappsDir = new File(getCatalinaHome(), "webapps");
            Map<String, ModuleId> deployedWars = readDeployedModulesExtendDescriptor();
            
            File[] webappsFiles = webappsDir.listFiles();
            for (File webapp : webappsFiles) {
                String name = webapp.getName();
                if (name.endsWith(".war")) {
                    name = name.substring(0, name.length() - 4);
                }
                
                if (deployedWars.containsKey(name)) {
                    delete(webapp);
                }
            }
            
            deployedWars.clear();
            writeDeployedModulesExtendDescriptor(deployedWars);
        } catch (IOException e) {
            Extend.info.warn("Failed to remove previously deployed war files from tomcat; ", e);
        }
    }

    protected void addDeployedModuleToExtendDescriptor(String warName, ModuleId moduleId) throws IOException {
        Map<String, ModuleId> deployedWars = readDeployedModulesExtendDescriptor();
        deployedWars.put(warName, moduleId);
        writeDeployedModulesExtendDescriptor(deployedWars);
    }

    protected void removeDeployedModuleToExtendDescriptor(String warName) throws IOException {
        Map<String, ModuleId> deployedWars = readDeployedModulesExtendDescriptor();
        deployedWars.remove(warName);
        writeDeployedModulesExtendDescriptor(deployedWars);
    }
    
    protected Map<String, ModuleId> readDeployedModulesExtendDescriptor() throws IOException {
        Map<String, ModuleId> res = new LinkedHashMap<String, ModuleId>();
        File deployedModulesExtendDescriptorFile = new File(getCatalinaHome(), DEPLOYED_MODULES_EXTEND_DESCRIPTOR);
        if (deployedModulesExtendDescriptorFile.exists()) {
            FileReader reader = new FileReader(deployedModulesExtendDescriptorFile);
            try {
                @SuppressWarnings("resource")
                BufferedReader in = new BufferedReader(reader);
                String line = in.readLine();
                while (line != null) {
                    line = line.trim();
                    if (!line.startsWith("#")) {
                        int i = line.indexOf('=');
                        if (i > 0) {
                            String warName = line.substring(0, i);
                            String moduleIdString = line.substring(i + 1);
                            ModuleId moduleId = ModuleId.parseModuleIdString(moduleIdString);
                            res.put(warName, moduleId);
                        }
                    }
                    line = in.readLine();
                }
            } finally {
                reader.close();
            }
        }
        return res;
    }

    protected void writeDeployedModulesExtendDescriptor(Map<String, ModuleId> deployedWars) throws IOException {
        File deployedModulesExtendDescriptorFile = new File(getCatalinaHome(), DEPLOYED_MODULES_EXTEND_DESCRIPTOR);
        FileWriter writer = new FileWriter(deployedModulesExtendDescriptorFile);
        try {
            for (Map.Entry<String, ModuleId> entry : deployedWars.entrySet()) {
                writer.write(entry.getKey().toString());
                writer.write('=');
                writer.write(entry.getValue().toString());
                writer.write('\n');
            }
        } finally {
            writer.close();
        }
    }
    
    protected void delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (!f.getName().equals(".") && !f.getName().equals("..")) {
                    delete(f);
                }
            }
        }
        file.delete();
    }

    
    
    protected void collectTomcatInformation() throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        File configDir = new File(getCatalinaHome(), "conf");
        File serverDotXml = new File(configDir, "server.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(serverDotXml);
        String shutdownPort = extractProperty("/Server/@port", doc);
        String shutdownValue = extractProperty("/Server/@shutdown", doc);
        if (shutdownPort != null) {
            try {
                serverShutdownPort = Integer.parseInt(shutdownPort);
            } catch (NumberFormatException ignore) {
            }
        }
        if (shutdownValue != null) {
            serverShutdownValue = shutdownValue;
        }

        XPath xpath = XPathFactory.newInstance().newXPath();
        
        XPathExpression expr = xpath.compile("/Server/Service/Connector");
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() < 1) {
            throw new IOException("Couldn't find node '/Server/Service/Connector' in server.xml");
        }
        
        String sslPortString = null;
        String sslAddressString = null;
        String normalPortString = null;
        String normalAddressString = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node portNode = nodes.item(i).getAttributes().getNamedItem("port"); 
            Node protocolNode = nodes.item(i).getAttributes().getNamedItem("protocol"); 
            Node addressNode = nodes.item(i).getAttributes().getNamedItem("address"); 
            Node secureNode = nodes.item(i).getAttributes().getNamedItem("SSLEnabled"); 
            if (portNode != null && protocolNode != null && protocolNode.getTextContent().startsWith("HTTP/")) {
                if (secureNode != null && secureNode.getTextContent().equalsIgnoreCase("true") && sslPortString != null) {
                    sslPortString = portNode.getTextContent();
                    if (addressNode != null) {
                        sslAddressString = addressNode.getTextContent();
                    } else {
                        sslAddressString = "localhost";
                    }
                } else {
                    normalPortString = portNode.getTextContent();
                    if (addressNode != null) {
                        normalAddressString = addressNode.getTextContent();
                    } else {
                        normalAddressString = "localhost";
                    }
                }
            }
        }
        if (normalPortString == null && sslPortString == null) {
            throw new IOException("server.xml doesn't have /Server/Service/Connector with HTTP service defined");
        }
        Integer normalPort = null;
        Integer sslPort = null;
        
        boolean setNormalPort = false;
        if (normalPortString != null) {
            try {
                normalPort = Integer.parseInt(normalPortString);
                // deployUsingManager = checkManagerWorking(normalPort, normalAddressString);
                deployUsingManager = checkManagerDeployed();
                if (deployUsingManager) {
                    serverManagerPort = normalPort;
                    serverManagerInterfaceAddress = normalAddressString;
                    setNormalPort = true;
                }
            } catch (NumberFormatException ignore) {
            }
        }
        if (sslPortString != null) {
            try {
                sslPort = Integer.parseInt(sslPortString);
            } catch (NumberFormatException ignore) {
            }
        }
        if (!setNormalPort) {
            forceDeployUsingManagerSet = true;
            forceDeployUsingManager = false;
            if (sslPort != null) {
                collectedOnlyOnSSL = true;
                collectedRunningPort = sslPort;
                collectedRunningHostName = sslAddressString;
            }
        } else {
            collectedOnlyOnSSL = false;
            collectedRunningPort = normalPort;
            collectedRunningHostName = normalAddressString;
        }
        if (overridePort > 0 && normalPort != null && overridePort != normalPort) {
            updatePort(normalPort, overridePort);
            collectedRunningPort = overridePort;
            if (setNormalPort) {
                serverManagerPort = overridePort;
            }
            if (deployUsingManager) {
                serverManagerPort = overridePort;
            }
        }
        if (overrideShutdownPort > 0 && overrideShutdownPort != serverShutdownPort) {
            updatePort(serverShutdownPort, overrideShutdownPort);
            serverShutdownPort = overrideShutdownPort;
        }
        updateTomcatUsersXml();
    }
    
    protected boolean checkManagerDeployed() {
        File manager = new File(getCatalinaHome(), "webapps/manager");
        return manager.exists();
    }
    
    protected boolean checkManagerWorking(int port, String address) {
        try {
            URL url = new URL("http", address, port, "/manager/serverinfo");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return true;
            }
        } catch (Exception ignore) {
        }
        return false;
    }
    
    protected void updateTomcatUsersXml() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        File conf = new File(getCatalinaHome(), "conf");
        File tomcatUsersDotXml = new File(conf, "tomcat-users.xml");
        if (tomcatUsersDotXml.exists()) {
            boolean roleFound = false;
            boolean userFound = false;
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(tomcatUsersDotXml);
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            XPathExpression exprRole = xpath.compile("/tomcat-users/role/@rolename");
            Object resultRole = exprRole.evaluate(doc, XPathConstants.NODESET);
            NodeList nodesRole = (NodeList)resultRole;
            for (int i = 0; i < nodesRole.getLength(); i++) {
                String roleName = nodesRole.item(0).getTextContent();
                if ("manager-script".equals(roleName)
                        || "manager".equals(roleName)) {
                    roleFound = true;
                }
            }

            XPathExpression exprUser = xpath.compile("/tomcat-users/user");
            Object resultUser = exprUser.evaluate(doc, XPathConstants.NODESET);
            NodeList nodesUser = (NodeList)resultUser;
            for (int i = 0; i < nodesUser.getLength(); i++) {
                Node userNode = nodesUser.item(0);
                if (managerUser != null) {
                    Node usernameNode = userNode.getAttributes().getNamedItem("username");
                    if (usernameNode != null && managerUser.equals(usernameNode.getTextContent())) {
                        Node passwordNode = userNode.getAttributes().getNamedItem("password");
                        if (passwordNode != null) {
                            managerPassword = passwordNode.getTextContent();
                        }
                        userFound = true;
                    }
                } else {
                    Node usernameNode = userNode.getAttributes().getNamedItem("username");
                    Node passwordNode = userNode.getAttributes().getNamedItem("password");
                    Node rolesNode = userNode.getAttributes().getNamedItem("roles");
                    if (usernameNode != null && passwordNode != null && rolesNode != null) {
                        String[] roles = rolesNode.getTextContent().split(",");
                        for (String role : roles) {
                            if (role.equals("manager") || role.equals("manager-script")) {
                                managerUser = usernameNode.getTextContent();
                                managerPassword = passwordNode.getTextContent();
                                userFound = true;
                                roleFound = true;
                            }
                        }
                    }
                }
            }
            
            String EOL = System.getProperty("line.separator");
            
            if (!roleFound || !userFound) {
                if (managerUser == null) {
                    managerUser = "manager-user";
                }
                if (managerPassword == null) {
                    managerPassword = "123specialmanageruserpassword@!";
                }
                StringWriter tomcatUsersContent = new StringWriter();
                FileReader fileReader = new FileReader(tomcatUsersDotXml);
                try {
                    @SuppressWarnings("resource")
                    BufferedReader in = new BufferedReader(fileReader);
                    String line = in.readLine();
                    while (line != null) {
                        if (!roleFound && line.trim().equals("<tomcat-users>")) {
                            tomcatUsersContent.append(line).append(EOL);
                            tomcatUsersContent.append("  <role rolename=\"manager-script\"/>").append(EOL);
                        } else if (!userFound && line.trim().equals("</tomcat-users>")) {
                            tomcatUsersContent.append("  <user username=\"" + managerUser + "\" password=\"" + managerPassword + "\" roles=\"manager-script\"/>").append(EOL);
                            tomcatUsersContent.append(line).append(EOL);
                        } else {
                            tomcatUsersContent.append(line).append(EOL);
                        }
                        line = in.readLine();
                    }
                } finally {
                    fileReader.close();
                }
                
                FileWriter fileWriter = new FileWriter(tomcatUsersDotXml);
                try {
                    fileWriter.write(tomcatUsersContent.toString());
                } finally {
                    fileWriter.close();
                }
            }

        } else {
            throw new FileNotFoundException("Cannot find conf/tomcat-users.xml; full path=" + tomcatUsersDotXml.getAbsolutePath());
        }
    }
    
    protected void updatePort(int fromPort, int toPort) throws IOException {
        File configDir = new File(getCatalinaHome(), "conf");
        File serverDotXml = new File(configDir, "server.xml");
        StringBuilder serverXML = new StringBuilder();
        char[] buf = new char[10240];
        FileReader reader = new FileReader(serverDotXml);
        try {
            int r = reader.read(buf);
            while (r > 0) {
                serverXML.append(buf, 0, r);
                r = reader.read(buf);
            }
        } finally {
            reader.close();
        }
        String fromString = "\"" + fromPort + "\"";
        String toString = "\"" + toPort + "\"";
        int i = serverXML.indexOf(fromString);
        if (i > 0) {
            serverXML.replace(i, i + fromString.length(), toString);
        }
        FileWriter writer = new FileWriter(serverDotXml);
        try {
            writer.write(serverXML.toString());
        } finally {
            writer.close();
        }
    }
    
    protected String extractProperty(String proprtyPath, Document doc) throws XPathExpressionException, IOException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        XPathExpression expr = xpath.compile(proprtyPath);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() < 1) {
            throw new IOException("Couldn't find node '" + proprtyPath + "' in server.xml");
        }
        return nodes.item(0).getTextContent();
    }

    public static class ZipFileModuleLoader implements ModuleLoader {

        private String fileName;
        
        public ZipFileModuleLoader(String fileName) {
            this.fileName = fileName;
        }
        
        public boolean canLoad(URI uri) {
            if (uri.getPath() != null && uri.getPath().endsWith(fileName)) {
                // Simple catch statement for zip files 
                return true;
            }
            return false;
        }

        public ModuleId toModuleId(URI uri) {
            return ModuleId.createModuleIdFromFileName(uri.getPath());
        }

        public Module load(URI uri) {
            return loadAs(uri, ModuleId.createModuleIdFromFileName(uri.getPath()));
        }

        public Module loadAs(URI uri, ModuleId moduleId) {
            AbstractModule module = new AbstractModule() {
                
            };
            try {
                module.setLocation(uri.toURL());
                module.setModuleId(moduleId);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            return module;
        }
        
    }

    
    
}
