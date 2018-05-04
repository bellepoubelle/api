package fr.bellepoubelle.api.misc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.WebApplicationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages access to the properties file. Implements the singleton pattern.
 * 
 * @author Jürgen Hecht
 *
 */
public class PropertyManager {

	private static Logger logger = LogManager.getLogger(PropertyManager.class);

	/**
	 * Singleton-object of the PropertyManager class.
	 */
	private static PropertyManager instance;

	/**
	 * Path of the configuration file.
	 */
	private final static String CONFIG_PATH = "/var/lib/bellepoubelle/";

	/**
	 * Name of the configuration file.
	 */
	private final static String CONFIG_FILE = "bellepoubelle.properties";

	/**
	 * Remembers whether the reading of configuration properties failed.
	 */
	private static boolean configReadFailed = false;

	/**
	 * Read the GetSeated and Log4j configuration properties.
	 */
	private static Properties properties = new Properties();

	private PropertyManager() {
		InputStream in = null;
		try {
			in = new FileInputStream(CONFIG_PATH + CONFIG_FILE);
			properties.load(in);
		} catch (IOException e) {
			configReadFailed = true;
			logger.error(CONFIG_FILE + " reading failed.");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					configReadFailed = true;
					logger.error(CONFIG_FILE + " reading failed.");
				}
			}
		}
	}

	/**
	 * Implementation of {@code getInstance()} as defined in the singleton
	 * pattern.
	 * 
	 * @return the current instance of {@code PropertyManager}
	 * @throws PropertyException
	 */
	public static PropertyManager getInstance() {
		if (instance == null) {
			instance = new PropertyManager();
		}
		return instance;
	}

	/**
	 * Returns the value associated with the given {@code key} from the
	 * properties file.
	 * 
	 * @param key
	 * @return the value associated with {@code key}
	 */
	public String getProperty(String key) {
		if (configReadFailed || properties == null) {
			throw new WebApplicationException(500);
		}
		return properties.getProperty(key).toString();
	}
}
