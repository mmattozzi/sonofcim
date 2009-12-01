package sonofcim;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbcp.BasicDataSource;
import org.jibble.pircbot.PircBot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class IrcBot extends PircBot {

	protected YahooAnswerer yahooAnswerer = null;
	protected Pattern helloPattern = null;
	protected LunchChooser lunchChooser = new LunchChooser();
	protected MovieQuoter movieQuoter = null;
	protected MoraleScale moraleScale;
    protected UserQuoter userQuoter;
	
	protected JdbcTemplate jdbcTemplate;
	protected SimpleJdbcTemplate simpleJdbcTemplate;
	
	protected Pattern quoteRequest = Pattern.compile("!q (.*)");
	protected Pattern linkPattern = Pattern.compile("(https?://\\S*)");
	protected Pattern userQuoteRequest = Pattern.compile("!do (.*)");
    protected Pattern messageRequest = Pattern.compile("!msg (.*)");

	protected DeliciousLinkSaver deliciousLinkSaver = null;
	
	public IrcBot(Properties props) {
		this.setName(props.getProperty("sonofcim.nick"));
		movieQuoter = new MovieQuoter(props.getProperty("sonofcim.yahoo.boss.apikey"));
		helloPattern = Pattern.compile("^(.*\\s)*(hi|hello|hola|hey)(\\s.*)*$", Pattern.CASE_INSENSITIVE);
		
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUsername(props.getProperty("sonofcim.db.user"));
		dataSource.setPassword(props.getProperty("sonofcim.db.password"));
		dataSource.setUrl(props.getProperty("sonofcim.db.url"));

		jdbcTemplate = new JdbcTemplate(dataSource);
		simpleJdbcTemplate = new SimpleJdbcTemplate(jdbcTemplate);
		moraleScale = new MoraleScale(jdbcTemplate);
		moraleScale.train(null);

        userQuoter = new UserQuoter(simpleJdbcTemplate);
        
		String deliciousUser = props.getProperty("sonofcim.delicious.user");
		String deliciousPass = props.getProperty("sonofcim.delicious.password");
		deliciousLinkSaver = new DeliciousLinkSaver(deliciousUser, deliciousPass);

        yahooAnswerer = new YahooAnswerer(props.getProperty("sonofcim.yahoo.apikey"), simpleJdbcTemplate);

	}
	
	@Override
	public void onJoin(String channel, String sender, String login, String hostname) {
		if (! sender.equals(this.getName())) {
			System.out.println(sender + " has joined.");
			
		}
	}
	
	@Override
	public void onMessage(String channel, String sender,
            String login, String hostname, String message) {
		
		if (! sender.equals(this.getName())) {
			
			Matcher linkMatcher = linkPattern.matcher(message);
			if (linkMatcher.find()) {
				deliciousLinkSaver.saveLink(linkMatcher.group(1), sender);
			}
			
			boolean directlyAddressed = false;
			if (message.contains(this.getName())) {
				directlyAddressed = true;
				System.out.println("Got directly addressed by " + sender);
			}
			
			if (directlyAddressed) {
				if (message.contains("lunch")) {
					sendMessage(channel, "you should eat at " + lunchChooser.getLunch());
					return;
				}
				String messageFiltered = message;
				if (directlyAddressed) messageFiltered = messageFiltered.replace(this.getName(), "");
				String answer = yahooAnswerer.getAnswer(messageFiltered, directlyAddressed, sender);
				if (answer != null) {
					answer = answer.replaceAll("\n", " ");
					sendMessage(channel, answer);
				}
			}
			
			if (message.startsWith("!q")) {
				Matcher m = quoteRequest.matcher(message);
				if (m.matches()) {
					String movie = m.group(1);
					if (movie != null) {
						System.out.println("Got quote request for " + movie);
						String msg = movieQuoter.getQuote(movie);
						if (msg != null) sendMessage(channel, msg);
					}
				}
			}

            if (message.startsWith("!do")) {
                Matcher m = userQuoteRequest.matcher(message);
                if (m.matches()) {
                    String user = m.group(1);
                    if (user != null) {
                        String msg = userQuoter.getQuote(user);
                        if (msg != null) sendMessage(channel, msg);
                    }
                }
            }

            if (message.startsWith("!msg")) {
                Matcher m = messageRequest.matcher(message);
                if (m.matches()) {
                    String user = m.group(1);
                    if (user != null) {
                        String msg = userQuoter.getMessage(user);
                        if (msg != null) sendMessage(channel, msg);
                    }
                }
            }

            if (message.equals("!aevans")) {
                sendMessage(channel, userQuoter.getQuote("aevans"));
            }

			if (message.equals("!morale")) {
				sendMessage(channel, moraleScale.getMorale());
			} else if (message.equals("!morale trend on")) {
				moraleScale.setReportTrend(true);
			} else if (message.equals("!morale trend off")) {
				moraleScale.setReportTrend(false);
			} else if (message.equals("!morale rankings")) {
				sendMessage(channel, moraleScale.getMoraleRankings());
			} else if (message.equals("!morale retrain ?")) {
				sendMessage(channel, moraleScale.getTrainingTechniques());
			} else if (message.startsWith("!morale retrain")) {
				long start = System.currentTimeMillis();
				if (message.length() > 16) {
					moraleScale.train(message.substring(16));
				} else {
					moraleScale.train(null);
				}
				long finish = System.currentTimeMillis();
				sendMessage(channel, "Retrained classifier in " + (finish - start) + " ms.");
			} else if (message.startsWith("!morale ")) {
				sendMessage(channel, moraleScale.evaluate(message.substring(8)));
			}
			
			if (! message.startsWith("!")) {
				String s = moraleScale.evaluateMorale(message, sender);
				if (s != null) {
					sendMessage(channel, s);
				}
			}
			
			if (! sender.startsWith("cimmy") && ! message.startsWith("!")) {
				simpleJdbcTemplate.update("INSERT INTO messages (nick, message, classification) VALUES (?, ?, ?)", sender.replaceAll("_", ""), message, "neutral");
			}
		}
	}

	protected boolean isSimpleHello(String channel, String sender, String message) {
		Matcher matcher = helloPattern.matcher(message);
		if (matcher.find()) {
			sendMessage(channel, "hello " + sender + ", ask a question");
			return true;
		} else {
			return false;
		}
	}

}
