package fr.bellepoubelle.api.rest;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/rest/v1.0")
public class RestApplication extends Application {
	
	private static Logger logger = LogManager.getLogger(RestApplication.class);

	@PostConstruct
	public void initializeSystem() {
		// setup Log4j 2
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        File file = new File("/var/lib/bellepoubelle/log4j2.xml");
        // this will force a reconfiguration
        context.setConfigLocation(file.toURI());
		logger.debug("Logger initialized.");
	}
}