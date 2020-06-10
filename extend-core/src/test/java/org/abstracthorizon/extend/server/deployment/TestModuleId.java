/*
 * Copyright (c) 2008 Creative Sphere Limited.
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

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author Daniel Sendula
 */
public class TestModuleId {
    
    @Test
    public void testModuleIdCreation1() {
        ModuleId m1 = new ModuleId("artifactonly");
        Assert.assertEquals(":artifactonly:", m1.toString());
        Assert.assertEquals("artifactonly", m1.getArtifactId());
        Assert.assertNull(m1.getGroupId());
        Assert.assertNull(m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation2() {
        ModuleId m1 = new ModuleId(":artifactonly");
        Assert.assertEquals(":artifactonly:", m1.toString());
        Assert.assertEquals("artifactonly", m1.getArtifactId());
        Assert.assertNull(m1.getGroupId());
        Assert.assertNull(m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation3() {
        ModuleId m1 = new ModuleId(":artifactonly::");
        Assert.assertEquals(":artifactonly:", m1.toString());
        Assert.assertEquals("artifactonly", m1.getArtifactId());
        Assert.assertNull(m1.getGroupId());
        Assert.assertNull(m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation4() {
        ModuleId m1 = new ModuleId(":artifactonly:::");
        Assert.assertEquals(":artifactonly:", m1.toString());
        Assert.assertEquals("artifactonly", m1.getArtifactId());
        Assert.assertNull(m1.getGroupId());
        Assert.assertNull(m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation5() {
        ModuleId m1 = new ModuleId("group:artifact");
        Assert.assertEquals("group:artifact:", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertNull(m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation6() {
        ModuleId m1 = new ModuleId("group:artifact:");
        Assert.assertEquals("group:artifact:", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertNull(m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation7() {
        ModuleId m1 = new ModuleId("group:artifact::");
        Assert.assertEquals("group:artifact:", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertNull(m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation8() {
        ModuleId m1 = new ModuleId("group:");
        Assert.assertEquals("group::", m1.toString());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertNull(m1.getArtifactId());
        Assert.assertNull(m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation9() {
        ModuleId m1 = new ModuleId("group:artifact:version");
        Assert.assertEquals("group:artifact:version", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertEquals("version", m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation10() {
        ModuleId m1 = new ModuleId("group::version");
        Assert.assertEquals("group::version", m1.toString());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertEquals("version", m1.getVersion());
        Assert.assertNull(m1.getArtifactId());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation11() {
        ModuleId m1 = new ModuleId("group:artifact:version:");
        Assert.assertEquals("group:artifact:version", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertEquals("version", m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation12() {
        ModuleId m1 = new ModuleId("group:artifact:version::");
        Assert.assertEquals("group:artifact:version", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertEquals("version", m1.getVersion());
        Assert.assertNull(m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation13() {
        ModuleId m1 = new ModuleId("group:artifact:version:type");
        Assert.assertEquals("group:artifact:version:type", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertEquals("version", m1.getVersion());
        Assert.assertEquals("type", m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation14() {
        ModuleId m1 = new ModuleId("group:artifact:version:type:");
        Assert.assertEquals("group:artifact:version:type", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertEquals("version", m1.getVersion());
        Assert.assertEquals("type", m1.getType());
        Assert.assertNull(m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation15() {
        ModuleId m1 = new ModuleId("group:artifact:version:type:classifier");
        Assert.assertEquals("group:artifact:version:type:classifier", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertEquals("version", m1.getVersion());
        Assert.assertEquals("type", m1.getType());
        Assert.assertEquals("classifier", m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation16() {
        ModuleId m1 = new ModuleId("group:artifact:version:type:classifier:");
        Assert.assertEquals("group:artifact:version:type:classifier", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertEquals("version", m1.getVersion());
        Assert.assertEquals("type", m1.getType());
        Assert.assertEquals("classifier", m1.getClassifier());
    }

    @Test
    public void testModuleIdCreation17() {
        ModuleId m1 = new ModuleId("group:artifact:version::classifier:");
        Assert.assertEquals("group:artifact:version::classifier", m1.toString());
        Assert.assertEquals("artifact", m1.getArtifactId());
        Assert.assertEquals("group", m1.getGroupId());
        Assert.assertEquals("version", m1.getVersion());
        Assert.assertEquals("classifier", m1.getClassifier());
        Assert.assertNull(m1.getType());
    }
}
