package org.fc.brewchain.url.tcp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerSetter;

public class Handler extends URLStreamHandler  {

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		return null;
	}

}
