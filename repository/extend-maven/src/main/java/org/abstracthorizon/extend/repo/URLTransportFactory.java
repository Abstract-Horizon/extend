package org.abstracthorizon.extend.repo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class URLTransportFactory implements TransportFactory {

    @Override
    public Transport transport(URI uri) {
        Transport transport;
        try {
            if (uri.getPath().endsWith("/")) {
                transport = new URLTransport(uri.toURL());
            } else {
                URI u = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath() + "/", uri.getQuery(), uri.getFragment());
                transport = new URLTransport(u.toURL());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        
        return transport;
    }

}
