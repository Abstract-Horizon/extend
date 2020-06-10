/*
 * Copyright (c) 2007 Creative Sphere Limited.
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
package org.abstracthorizon.extend.server.deployment;

import java.io.File;
import java.util.NoSuchElementException;

/**
 * Module identification.
 *
 * @author Daniel Sendula
 */
public class ModuleId {

    public static final String JAR_TYPE = "jar";
    
    protected String groupId;

    protected String artifactId;

    protected String version;

    protected String type;

    protected String classifier;

    protected String stringRepresentation;
    
    public ModuleId() {
    }

    public ModuleId(String moduleId) {
        try {
            parseModuleIdString(this, moduleId);
        } catch (NoSuchElementException e) {
            throw e;
        }
    }
    
    public ModuleId(ModuleId copy) {
        this.groupId = copy.groupId;
        this.artifactId = copy.artifactId;
        this.version = copy.version;
        this.type = copy.type;
        this.classifier = copy.classifier;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        stringRepresentation = null;
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        stringRepresentation = null;
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        stringRepresentation = null;
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        stringRepresentation = null;
        this.type = type;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        stringRepresentation = null;
        this.classifier = classifier;
    }

    public String toString() {
        if (stringRepresentation == null) {
            updateStringRepresentation();
        }
        return stringRepresentation;
    }
    
    protected void updateStringRepresentation() {
        StringBuffer res = new StringBuffer();
        if (groupId != null) {
            res.append(groupId);
        }
        res.append(":");
        if (artifactId != null) {
            res.append(artifactId);
        }
        res.append(":");
        if (version != null) {
            res.append(version);
        }
        if (type != null) {
            res.append(":");
            res.append(type);
        }
        if (classifier != null) {
            if (type == null) {
                res.append(":");
            }
            res.append(":");
            res.append(classifier);
        }
        if (res.length() == 0) {
            res.append("unknown@" + System.identityHashCode(this));
        }
        stringRepresentation = res.toString();
    }
    
    public int hashCode() {
        if (stringRepresentation == null) {
            updateStringRepresentation();
        }
        return stringRepresentation.hashCode();
    }

    public String getShortId() {
        StringBuffer res = new StringBuffer();
        res.append(groupId).append(':').append(artifactId).append(':').append(version);
        return res.toString();
    }

    public String getFullId() {
        StringBuffer res = new StringBuffer();
        res.append(groupId).append(':').append(artifactId).append(':').append(version);
        if (type != null) {
            res.append(':').append(type);
            if (classifier != null) {
                res.append(':').append(classifier);
            }
        } else if (classifier != null) {
            res.append(':').append(':').append(classifier);
        }
        return res.toString();
    }

    public boolean equals(Object o) {
        if (o instanceof ModuleId) {
            ModuleId other = (ModuleId)o;
            
            String type = getType();
            if (type == null) {
                type = JAR_TYPE;
            }
            String otype = other.getType();
            if (otype == null) {
                otype = JAR_TYPE;
            }
            
            boolean res = (((other.groupId == groupId) || ((groupId != null) && groupId.equals(other.groupId)))
                    && ((other.artifactId == artifactId) || ((artifactId != null) && artifactId.equals(other.artifactId)))
                    && ((other.version == version) || ((version != null) && version.equals(other.version)))
                    && ((otype == type) || ((type != null) && type.equals(otype)))
                    && ((other.classifier == classifier) || ((classifier != null) && classifier.equals(other.classifier))));
            return res;
        } else if (o instanceof String) {
            return (getFullId().equals(o));
        }
        return false;
    }

    public static ModuleId parseModuleIdString(String moduleId) {
        ModuleId res = new ModuleId();
        parseModuleIdString(res, moduleId);
        return res;
    }
    
    public static ModuleId parseModuleIdString(ModuleId moduleId, String s) {
        int i = s.indexOf(':');
        if (i < 0) {
            moduleId.setArtifactId(s);
            return moduleId;
        }
        String p1 = s.substring(0, i).trim();
        if (p1.length() > 0) {
            moduleId.setGroupId(p1);
        }
        int j = s.indexOf(':', i + 1);
        if (j < 0) {
            p1 = s.substring(i + 1).trim();
            if (p1.length() > 0) {
                moduleId.setArtifactId(p1);
            }
            return moduleId;
        }
        p1 = s.substring(i + 1, j).trim();
        if (p1.length() > 0) {
            moduleId.setArtifactId(p1);
        }
        i = j + 1;
        j = s.indexOf(':', i);
        if (j < 0) {
            p1 = s.substring(i).trim();
            if (p1.length() > 0) {
                moduleId.setVersion(p1);
            }
            return moduleId;
        }
        p1 = s.substring(i, j).trim();
        if (p1.length() > 0) {
            moduleId.setVersion(p1);
        }
        
        i = j + 1;
        j = s.indexOf(':', i);
        if (j < 0) {
            p1 = s.substring(i).trim();
            if (p1.length() > 0) {
                moduleId.setType(p1);
            }
            return moduleId;
        }
        p1 = s.substring(i, j);
        if (p1.length() > 0) {
            moduleId.setType(p1);
        }

        i = j + 1;
        j = s.indexOf(':', i);
        if (j < 0) {
            p1 = s.substring(i).trim();
            if (p1.length() > 0) {
                moduleId.setClassifier(p1);
            }
            return moduleId;
        }
        p1 = s.substring(i, j);
        if (p1.length() > 0) {
            moduleId.setClassifier(p1);
        }
        
//        
//        StringTokenizer tokenizer = new StringTokenizer(s, ":");
//        moduleId.setGroupId(tokenizer.nextToken());
//        moduleId.setArtifactId(tokenizer.nextToken());
//        moduleId.setVersion(tokenizer.nextToken());
//        if (tokenizer.hasMoreTokens()) {
//            moduleId.setType(tokenizer.nextToken());
//        }
//        if (tokenizer.hasMoreTokens()) {
//            moduleId.setClassifier(tokenizer.nextToken());
//        }

        return moduleId;
    }
    
    public static ModuleId createModuleIdFromFileName(String name) {
        if (name == null) {
            return null;
        }
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        int q = name.lastIndexOf("/");
        if (q >= 0) {
            name = name.substring(q + 1);
        }
        ModuleId moduleId = new ModuleId();
        int i = name.indexOf('#');
        if (i >= 0) {
            String s1 = name.substring(0, i);
            int j = name.indexOf('#', i + 1);
            if (j >= 0) {
                String s2 = name.substring(i + 1, j);
                int k = name.indexOf('#', j + 1);
                if (k >= 0) {
                    String s3 = name.substring(j + 1, k);
                    int l = name.indexOf('#', k + 1);
                    if (l >= 0) {
                        String s4 = name.substring(k + 1, l);
                        moduleId.setGroupId(s1);
                        moduleId.setArtifactId(s2);
                        moduleId.setVersion(s3);
                        moduleId.setClassifier(s4);
                        String s5 = name.substring(l + 1);
                        if (s5.startsWith(".")) {
                            s5 = s5.substring(1);
                        }
                        moduleId.setType(s5);
                    } else {
                        moduleId.setGroupId(s1);
                        moduleId.setArtifactId(s2);
                        moduleId.setVersion(s3);
                        String s5 = name.substring(k + 1);
                        if (s5.startsWith(".")) {
                            s5 = s5.substring(1);
                        }
                        moduleId.setType(s5);
                    }
                } else {
                    moduleId.setGroupId(s1);
                    moduleId.setArtifactId(s2);
                    String s5 = name.substring(j + 1);
                    if (s5.startsWith(".")) {
                        s5 = s5.substring(1);
                    }
                    moduleId.setType(s5);
                }
            } else {
                moduleId.setArtifactId(s1);
                String s5 = name.substring(i + 1);
                if (s5.startsWith(".")) {
                    s5 = s5.substring(1);
                }
                moduleId.setType(s5);
            }
        } else {
            i = name.indexOf('.');
            if (i >= 0) {
                String s1 = name.substring(0, i);
                String s5 = name.substring(i + 1);
                moduleId.setArtifactId(s1);
                moduleId.setType(s5);
            } else {
                moduleId.setArtifactId(name);
            }
        }
        return moduleId;
    }

    public static ModuleId createModuleIdFromFileName(File file) {
        return ModuleId.createModuleIdFromFileName(file.getName());
    }

    
}
