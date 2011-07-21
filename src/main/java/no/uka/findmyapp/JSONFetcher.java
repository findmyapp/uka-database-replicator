package no.uka.findmyapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JSONFetcher
{
  private Map<String, List<String>> responseHeader = null;
  private URL responseURL = null;
  private int responseCode = -1;
  private String MIMEtype = null;
  private String charset = null;
  private Object content = null;

  private String proxyHost = "";
  private String proxyPort = "";
  private boolean useProxy = false;

  public JSONFetcher(String proxyHost, String proxyPort) {
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
    this.useProxy = true;

    this.content = "";
  }

  public JSONFetcher() {
    this.content = "";
  }

  public String getWebFile(String urlString) throws MalformedURLException, IOException, IllegalArgumentException
  {
    if (this.useProxy) {
      Properties systemProperties = System.getProperties();
      systemProperties.setProperty("http.proxyHost", this.proxyHost);
      systemProperties.setProperty("http.proxyPort", this.proxyPort);
    }

    URL url = new URL(urlString);
    URLConnection uconn = url.openConnection();

    if (!(uconn instanceof HttpURLConnection)) {
      throw new IllegalArgumentException("URL protocol must be HTTP.");
    }
    HttpURLConnection conn = (HttpURLConnection)uconn;

    conn.setConnectTimeout(10000);
    conn.setReadTimeout(10000);
    conn.setInstanceFollowRedirects(true);
    conn.setRequestProperty("User-agent", "spider");

    conn.connect();

    this.responseHeader = conn.getHeaderFields();
    this.responseCode = conn.getResponseCode();
    this.responseURL = conn.getURL();
    int length = conn.getContentLength();
    String type = conn.getContentType();

    if (type != null) {
      String[] parts = type.split(";");
      this.MIMEtype = parts[0].trim();
      for (int i = 1; (i < parts.length) && (this.charset == null); i++) {
        String t = parts[i].trim();
        int index = t.toLowerCase().indexOf("charset=");
        if (index != -1) {
          this.charset = t.substring(index + 8);
        }
      }
    }

    InputStream stream = conn.getErrorStream();
    if (stream != null) {
      this.content = readStream(length, stream);
    }
    else if (((this.content = conn.getContent()) != null) && ((this.content instanceof InputStream))) {
      this.content = readStream(length, (InputStream)this.content);
    }

    conn.disconnect();

    return (String)this.content;
  }

  private Object readStream(int length, InputStream stream)
    throws IOException
  {
    int buflen = Math.max(1024, Math.max(length, stream.available()));
    byte[] buf = new byte[buflen];
    byte[] bytes = (byte[])null;

    for (int nRead = stream.read(buf); nRead != -1; nRead = stream.read(buf)) {
      if (bytes == null) {
        bytes = buf;
        buf = new byte[buflen];
      }
      else {
        byte[] newBytes = new byte[bytes.length + nRead];
        System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        System.arraycopy(buf, 0, newBytes, bytes.length, nRead);
        bytes = newBytes;
      }
    }
    if (this.charset == null) {
      System.out.println("null contents");
      return bytes;
    }
    try {
      return new String(bytes, this.charset);
    }
    catch (UnsupportedEncodingException localUnsupportedEncodingException) {
    }
    return bytes;
  }
}