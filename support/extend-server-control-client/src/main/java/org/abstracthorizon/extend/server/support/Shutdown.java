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

package org.abstracthorizon.extend.server.support;


/**
 * Simple class that sends {@link Control#MAGIC} to default (or specified) port on
 * local (or specified machine).
 *
 * @author Daniel Sendula
 */
public class Shutdown {

    public static void main(String[] args) throws Exception {
        // TODO
//        String host = "localhost";
//        int port = Control.DEFAULT_PORT;
//        if (args.length == 1) {
//
//            try {
//                port = Integer.parseInt(args[0]);
//            } catch (NumberFormatException e) {
//                host = args[0];
//            }
//        } else if (args.length == 2) {
//            host =  args[0];
//            try {
//                port = Integer.parseInt(args[1]);
//            } catch (NumberFormatException e) {
//                printUseage();
//            }
//        } else if (args.length > 2) {
//            printUseage();
//        }
//
//        Socket socket = new Socket(host, port);
//        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
//        out.println(Control.MAGIC);
//        out.flush();
//        try {
//            socket.close();
//        } catch (Exception ignore) {
//        }
    }

    /**
     * Prints useage (from command line) of this class.
     */
    protected static void printUseage() {
        System.out.println("Useage: java org.abstracthorizon.spring.server.support.Shutdown [<host>] [<port>]");
        System.exit(1);
    }


}
