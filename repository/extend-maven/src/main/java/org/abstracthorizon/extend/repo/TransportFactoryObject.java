package org.abstracthorizon.extend.repo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;

public class TransportFactoryObject {
    
    private static TransportFactory urlTransportFactory = new TransportFactory() {

        private HashMap<URI, Transport> permanentCache = new HashMap<URI, Transport>();
        
        public Transport transport(URI uri) {
            Transport transport = permanentCache.get(uri);
            if (transport == null) {
                if (uri.getScheme() == "file") {
                    transport = new FileTransport(new File(uri.getPath()));
                } else {
                    try {
                        transport = new URLTransport(uri.toURL());
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                permanentCache.put(uri, transport);
            }
            return transport;
        }
    };
    
    public static TransportFactory urlTransportFactory() {
        return urlTransportFactory;
    }
    
    public static TransportFactory defaultTransportFactory() {
        return urlTransportFactory;
    }

}
