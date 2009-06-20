package sonofcim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class MovieQuoter {

	private String yahooBossApiKey = null;
	private final String resourcePath = "http://boss.yahooapis.com/ysearch/web/v1/";
	protected XPathExpression xpathExpression = null;
	protected Pattern urlPattern = Pattern.compile("<url>.*(tt\\d{7}).*</url>");
	protected Random random = new Random();
	
	public MovieQuoter(String apiKey) {
		this.yahooBossApiKey = apiKey;
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		
		//xpath.setNamespaceContext(new SimpleNamespaceContext("yans", "urn:yahoo:answers"));
		
		try {
			xpathExpression = xpath.compile("//url");
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
	}
	
	public String getQuote(String movie) {
		try {
			URL url = new URL(resourcePath + URLEncoder.encode(movie, "utf-8") + 
					"?appid=" + yahooBossApiKey + "&format=xml" );
			
			String imdbId = null;
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = null;
			while ( (line = reader.readLine()) != null) {
				Matcher m = urlPattern.matcher(line);
				if (m.find()) {
					imdbId = m.group(1);
					break;
				}
			}
			reader.close();
			
			if (imdbId == null) {
				System.err.println("No imdb id found for " + movie);
				return null;
			}
			
			List<String> quotes = new ArrayList<String>();
			
			HttpClient client = new HttpClient();
			GetMethod get = new GetMethod("http://www.imdb.com/title/" + imdbId + "/quotes");
			client.executeMethod(get);
			
			reader = new BufferedReader(new InputStreamReader(get.getResponseBodyAsStream()));
			while ( (line = reader.readLine()) != null) {			
				if (line.startsWith("<b>") && line.endsWith("</b>:")) {
					String qLine = reader.readLine();
					String brLine = reader.readLine();
					if (brLine.equals("<br>")) {
						quotes.add(qLine);
					}
				}
			}
			reader.close();
			
			return quotes.get( Math.abs(random.nextInt()) % quotes.size() );
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
