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
package org.abstracthorizon.extend.server.deployment;

/**
 * Deployment exception
 *
 * @author Daniel Sendula
 */
public class DeploymentException extends RuntimeException {

    /**
     * Constructor
     */
    public DeploymentException() {
    }

    /**
     * Constructor
     *
     * @param msg message
     */
    public DeploymentException(String msg) {
        super(msg);
    }

    /**
     * Constructor
     *
     * @param cause cuase
     */
    public DeploymentException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor
     *
     * @param msg message
     * @param cause cause
     */
    public DeploymentException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
