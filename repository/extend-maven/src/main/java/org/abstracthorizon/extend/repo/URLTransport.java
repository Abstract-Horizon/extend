package org.abstracthorizon.extend.repo;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class URLTransport extends AbstractURLTransport {

    private URL repositoryURL;
    
    public URLTransport(URL repositoryURL) {
        this.repositoryURL = repositoryURL;
    }
    
    protected URL resourceURL(String path) {
        try {
            return repositoryURL.toURI().resolve(path).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    
}
