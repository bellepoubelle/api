package fr.bellepoubelle.api.misc;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
	
	private static Logger logger = LogManager.getLogger(LoggingFilter.class);

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		logger.debug("Request filter");
		logger.debug("Headers: " + requestContext.getHeaders());
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		logger.debug("Response filter");
		logger.debug("Headers: " + responseContext.getHeaders());
	}

}
