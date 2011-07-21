package no.uka.findmyapp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

public class JSONFetcher{

  private String proxyHost = "";
  private String proxyPort = "";
  private boolean useProxy = false;

  public JSONFetcher(String proxyHost, String proxyPort) {
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
    this.useProxy = true;

  }

  public JSONFetcher() {

  }

  public String getWebFile(String urlString) throws MalformedURLException, IOException, IllegalArgumentException
  {
	  if (this.useProxy) {
		  Properties systemProperties = System.getProperties();
		  systemProperties.setProperty("http.proxyHost", this.proxyHost);
		  systemProperties.setProperty("http.proxyPort", this.proxyPort);
	  }
 
      HttpClient httpclient = new DefaultHttpClient();
      String responseBody = "";

      HttpGet httpget = new HttpGet(urlString);

      System.out.println("executing request " + httpget.getURI());

      // Create a response handler
      ResponseHandler<String> responseHandler = new BasicResponseHandler();
      responseBody = httpclient.execute(httpget, responseHandler);

      httpclient.getConnectionManager().shutdown();

      return responseBody;
    
  }
 }