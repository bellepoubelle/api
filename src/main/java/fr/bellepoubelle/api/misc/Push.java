package fr.bellepoubelle.api.misc;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;

public class Push {

    private static Logger logger = LogManager.getLogger(Push.class);
    private static PropertyManager config = PropertyManager.getInstance();
    
    private static String SERVER_KEY = config.getProperty("push.fcm.key");
    private static String PATH_TO_P12_CERT = config.getProperty("push.ios.certificate.path");
    private static String CERT_PASSWORD = config.getProperty("push.ios.certificate.password");

    public static void toFCM(String deviceToken, String title, String message) throws Exception {
		String pushMessage = "{\"data\":{\"title\":\"" + title + "\",\"message\":\"" + message + "\"},\"to\":\""
				+ deviceToken + "\"}";
        
        // Create connection to send FCM Message request.
        URL url = new URL("https://fcm.googleapis.com/fcm/send");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "key=" + SERVER_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // Send FCM message content.
        OutputStream outputStream = conn.getOutputStream();
        outputStream.write(pushMessage.getBytes());

        logger.debug(conn.getResponseCode());
        logger.debug(conn.getResponseMessage());
    }
    
	public static void toIOS(String deviceToken, String title, String message) {
		logger.debug("send iOS Push notification");
		ApnsService service = APNS.newService().withCert(PATH_TO_P12_CERT, CERT_PASSWORD).withSandboxDestination().build();
		String payload = APNS.newPayload().alertTitle(title).alertBody(message).sound("default").build();
		service.push(deviceToken, payload);
	}
}
