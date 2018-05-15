package org.fc.brewchain.bcapi.url.tcp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;

@Component
@Instantiate
@Provides(specifications = { URLStreamHandlerService.class }, properties = {
		@StaticServiceProperty(name = "url.handler.protocol", type = "java.lang.String", value = "tcp") })
public class HandlerService extends AbstractURLStreamHandlerService implements URLStreamHandlerService {

	@Override
	public URLConnection openConnection(URL u) throws IOException {
		return null;
	}

}
