package sonofcim;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;

public class BotBot implements Runnable {

	protected String name;
	protected String channel;
	protected IrcBot ircBot;
	protected String server = "irc.freenode.net";
	
	public static void main(String[] args) throws NickAlreadyInUseException, IOException, IrcException {
		
		if (args.length < 1) {
			System.err.println("Use botbot.BotBot <properties file>");
			System.exit(1);
		}
		
		BotBot botBot = new BotBot(args[0]);
		Thread t = new Thread(botBot);
		t.start();
	}

	public BotBot(String propertiesFile) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(propertiesFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.name = properties.getProperty("sonofcim.nick");
		this.channel = properties.getProperty("sonofcim.channel");
		this.ircBot = new IrcBot(properties);
		this.ircBot.setVerbose(true);
	}

	public void run() {
		while (true) {
			if (! this.ircBot.isConnected()) {
				try {
					System.err.println(new Date() + " Not connected, joining...");
					join();
				} catch (NickAlreadyInUseException e) {
					this.name = this.name + "_";
					if (this.name.endsWith("___")) {
						this.name.replaceAll("_", "");
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (IrcException e) {
					e.printStackTrace();
				}
			}
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void join() throws NickAlreadyInUseException, IOException, IrcException {
		ircBot.connect(server);
		ircBot.joinChannel("#" + this.channel);
	}
}
