package org.abstracthorizon.extend.repo;

import java.net.URI;

public interface TransportFactory {

    Transport transport(URI uri);

}
