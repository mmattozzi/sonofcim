package sonofcim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

public class DeliciousLinkSaver {

	protected String user;
	protected HttpClient httpClient;
	protected ScheduledThreadPoolExecutor threadPoolExecutor;
	
	protected final String DELICIOUS_PREFIX = "https://api.del.icio.us/v1/posts/add?";
	
	public DeliciousLinkSaver(String user, String password) {
		this.user = user;
		httpClient = new HttpClient();
		httpClient.getParams().setAuthenticationPreemptive(true);
		Credentials defaultcreds = new UsernamePasswordCredentials(user, password);
		httpClient.getState().setCredentials(
				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), defaultcreds);
		
		threadPoolExecutor = new ScheduledThreadPoolExecutor(1);
	}
	
	public void saveLink(String link, String sender) {
		LinkSaverThread linkSaverThread = new LinkSaverThread(link, sender);
		threadPoolExecutor.submit(linkSaverThread);
	}
	
	class LinkSaverThread implements Runnable {

		protected Pattern htmlTitlePattern = Pattern.compile("<title>([^<]*)</title>", Pattern.CASE_INSENSITIVE);
		
		protected String link;
		protected String sender;
		
		public LinkSaverThread(String link, String sender) {
			this.link = link;
			this.sender = sender;
			this.sender = this.sender.replaceAll("_", "");
		}
		
		public void run() {
			String encodedLink;
			try {
				encodedLink = URLEncoder.encode(link, "utf-8");
			} catch (UnsupportedEncodingException e2) {
				e2.printStackTrace();
				return;
			}
			String linkTitle = encodedLink;
			
			GetMethod findTitleGet = new GetMethod(link);
			try {
				httpClient.executeMethod(findTitleGet);
				if (findTitleGet.getStatusCode() == 200) {
					Matcher htmlTitleMatcher = htmlTitlePattern.matcher(findTitleGet.getResponseBodyAsString());
					if (htmlTitleMatcher.find()) {
						linkTitle = htmlTitleMatcher.group(1);
						linkTitle = URLEncoder.encode(linkTitle, "utf-8");
					}
				}
			} catch (HttpException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			try {
				
				String url = DELICIOUS_PREFIX + "url=" + encodedLink + "&description=" + linkTitle + "&tags=" + sender;
				System.out.println("Calling: " + url);
				GetMethod getMethod = new GetMethod(url);
				getMethod.addRequestHeader("User-Agent", "sonofcim");
				httpClient.executeMethod(getMethod);
				System.out.println("Got response code " + getMethod.getStatusCode() + " from delicious");
				System.out.println("Delicious response: " + getMethod.getResponseBodyAsString());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (HttpException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}
