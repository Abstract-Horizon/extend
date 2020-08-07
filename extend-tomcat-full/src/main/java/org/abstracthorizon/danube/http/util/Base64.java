/*
 * Copyright (c) 2004-2020 Creative Sphere Limited.
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
package org.abstracthorizon.danube.http.util;

/**
 * This class encodes and decodes strings from/to Base64
 *
 * @author Daniel Sendula
 */
public class Base64 {
    
    /**
     * This method encodes given string
     * @param s string to be encoded
     * @return encoded string
     */
    public static String encode(String s) {
        if (s == null) {
            return s;
        }
        StringBuffer res = new StringBuffer();
        int len = s.length();
        int i = 0;
        for (; i + 3 <= len; i = i + 3) {
            int b1 = s.charAt(i);
            int b2 = s.charAt(i+1);
            int b3 = s.charAt(i+2);
            
            res.append(sixToEight[(b1 & 0xfc) >> 2]);
            res.append(sixToEight[((b1 & 0x3) << 4) | ((b2 & 0xf0) >> 4)]);
            res.append(sixToEight[(b2 & 0xf) << 2 | ((b3 & 0xc0) >> 6)]);
            res.append(sixToEight[b3 & 0x3f]);
        }
        if ((i + 2) == len) {
            int b1 = s.charAt(i);
            int b2 = s.charAt(i+1);
            int b3 = 0;
            res.append(sixToEight[(b1 & 0xfc) >> 2]);
            res.append(sixToEight[((b1 & 0x3) << 4) | ((b2 & 0xf0) >> 4)]);
            res.append(sixToEight[(b2 & 0xf) << 2 | ((b3 & 0xc0) >> 6)]);
            res.append('=');
        } else if ((i + 1) == len) {
            int b1 = s.charAt(i);
            int b2 = 0;
            res.append(sixToEight[(b1 & 0xfc) >> 2]);
            res.append(sixToEight[((b1 & 0x3) << 4) | ((b2 & 0xf0) >> 4)]);
            res.append('=');
            res.append('=');
        }
        
        return res.toString();
    }
    
    /**
     * This method decodes given string
     * @param s string to be decoded
     * @return decoded string
     */
    public static String decode(String s) {
        if (s == null) {
            return s;
        }
        StringBuffer res = new StringBuffer();
        int j = 0;
        char c = ' '; 
        for (int i = 0; i < s.length(); i++) {
            char x = s.charAt(i);
            if (x == '=') {
                
                if (j == 0) {
                    return null;
                } else if (j == 1) {
                    return null;
                } else if (j == 2) {
                    return res.toString();
                } else if (j == 3) {
                    return res.toString();
                }
            } else {
                int b = eightToSix[x];
                if (b == -1) {
                    return null;
                } 
                if (j == 0) {
                    c = (char)(b << 2);
                } else if (j == 1) {
                    c = (char)(((b & 0x30) >> 4) | c);
                    res.append(c);
                    c = (char)((b & 0xf) << 4);
                } else if (j == 2) {
                    c = (char)(((b & 0x3c) >> 2) | c);
                    res.append(c);
                    c = (char)((b & 0x3) << 6);
                } else {
                    c = (char)(b | c);
                    res.append(c);
                }
                j = j + 1;
                if (j == 4) {
                    j = 0;
                }
            }
        }
        
        return res.toString();
    }
    
    /** Cached encoding char array */
    protected static char[] sixToEight;
    
    /** Cached decoding int array */
    protected static int[] eightToSix;

    static {
    
       sixToEight = new char[64];

       eightToSix = new int[256];

       for (int i=0; i<=25; i++) {
           sixToEight[i] = (char)('A'+i);
       }

       for (int i=0; i<=25; i++) {
           sixToEight[i+26] = (char)('a'+i);
       }

       for (int i=0; i<=9; i++) {
           sixToEight[i+52] = (char)('0'+i);
       }
       
       sixToEight[62] = '+';
       sixToEight[63] = '/';

       for (int i=0; i<256; i++) {
           eightToSix[i] = -1;
       }

       for ( int i=0; i<64; i++ ) {
           eightToSix[sixToEight[i]] = i;
       }

       eightToSix['='] = -2;
    }
}
