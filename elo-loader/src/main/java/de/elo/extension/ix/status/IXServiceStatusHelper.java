package de.elo.extension.ix.status;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lucian.Dragomir on 5/21/2015.
 */
public class IXServiceStatusHelper {

    private static final String CMD_STATUS = "ix?cmd=status";
    public static final String VERSION = "Version";
    public static final String JAVA_CLIENT = "Java Client Components";

    private static final Pattern statusPattern = Pattern.compile("<tr class=\"StatusTable(.+?)>(.+?)<td>(.+?)</td>(.+?)<td>(.+?)</td>");
    private static final int statusPropertyName = 3;
    private static final int statusPropertyValue = 5;

    private static final Pattern linkPattern = Pattern.compile("<td class=\"ActionsTableCell\">(.+?)<a href=\"(.+?)\">(.+?)</a>");
    private static final int linkPropertyName = 3;
    private static final int linkPropertyValue = 2;

    private String url;

    public IXServiceStatusHelper(String url) {
        this.url = url.substring(0, url.lastIndexOf("/") + 1);
    }

    private static String getStringFromInputStream(InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    private String getIXStatusReport() {
        String response = null;
        HttpURLConnection connection = null;

        try {
            //Create connection
            connection = (HttpURLConnection) (new URL(url + CMD_STATUS)).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            response = getStringFromInputStream(connection.getInputStream());
            if (connection.getResponseCode() != 200) {
                response = null;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    public String getIXAuthenticationReport(String user, String password) {
        String response = null;
        HttpURLConnection connection = null;
        String link = null;
        Map<String, String> mapLinks = getIXStatusLinks();
        link = mapLinks.get("Test SSO Login");

        try {
            //Create connection
            String authEncoded = DatatypeConverter.printBase64Binary((user + ":" + password).getBytes("UTF-8"));

            connection = (HttpURLConnection) (new URL(link)).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setRequestProperty("Authorization", "Basic " + authEncoded);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            response = getStringFromInputStream(connection.getInputStream());
            if (connection.getResponseCode() != 200) {
                response = null;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    public Map<String, String> getIXStatusProperties() {
        Map<String, String> map = new HashMap<String, String>();
        String response = getIXStatusReport();
        if (response != null && response.length() > 0) {
            Matcher matcher = statusPattern.matcher(response);
            String propertyName;
            String propertyValue;

            while (matcher.find()) {
                propertyName = matcher.group(statusPropertyName);
                propertyValue = matcher.group(statusPropertyValue);
                if (map.containsKey(propertyName)) {
                    map.remove(propertyName);
                }
                map.put(propertyName, propertyValue);
            }
        }
        return map;
    }

    public Map<String, String> getIXStatusLinks() {
        Map<String, String> map = new HashMap<String, String>();
        String response = getIXStatusReport();
        if (response != null && response.length() > 0) {
            Matcher matcher = linkPattern.matcher(response);
            String propertyName;
            String propertyValue;

            while (matcher.find()) {
                propertyName = matcher.group(linkPropertyName);
                propertyValue = matcher.group(linkPropertyValue);
                if (map.containsKey(propertyName)) {
                    map.remove(propertyName);
                }
                map.put(propertyName, this.url + propertyValue);
            }
        }
        return map;
    }
}
