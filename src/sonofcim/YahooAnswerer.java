package sonofcim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class YahooAnswerer {

	protected String yahooId = null;
	protected String resourcePath = "http://answers.yahooapis.com/AnswersService/V1/questionSearch";
	protected XPathExpression xpathExpression = null;
	protected HashSet<String> stopwords = new HashSet<String>();

	protected List<String> negativeWords = new ArrayList<String>();
	
	Random r = new Random();
	
	protected String[] cannedResponses = new String[]{ "my brain hurts", "I think I need to lie down", "the dude abides", "hexen thx", "pong anyone?" };
	
	public YahooAnswerer(String yahooApiKey) {
		this.yahooId = yahooApiKey;
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		
		xpath.setNamespaceContext(new SimpleNamespaceContext("yans", "urn:yahoo:answers"));
		
		try {
			xpathExpression = xpath.compile("//yans:ChosenAnswer");
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		
		try {
			MoraleScale.loadWords(stopwords, this.getClass().getResource("/stopwords.txt").openStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			MoraleScale.loadWords(negativeWords, this.getClass().getResource("/negative_words.txt").openStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getAnswer(String question, boolean forceAnswer, String sender) {
		try {
			question = question.replaceAll("(,|')", "");
			String answer = queryAnswersAPI(question);
			if (answer == null) System.out.println("Couldn't find direct answer to question");
			
			if (answer == null && forceAnswer) {
				answer = queryAnswersAPI(getKeywords(question, 2));
			}
			
			if (answer == null && forceAnswer) answer = "That's " + negativeWords.get(r.nextInt(negativeWords.size())) + ", " + sender;
			return answer;
		} catch (Exception e) {
			e.printStackTrace();
			if (forceAnswer) return cannedResponses[Math.abs(r.nextInt()) % cannedResponses.length];
			else return null;
		}
	}

	protected String getKeywords(String question, int num) {
		String keywordString = "";
		String []words = question.split(" ");
		for (int i = 0; i < num; i++)
			keywordString += " " + popLongestWord(words);
		return keywordString;
	}

	protected String popLongestWord(String[] words) {
		int bestLength = 0;
		String bestWord = "";
		int bestIndex = 0;
		for (int i = 0; i < words.length; i++) {
			if (words[i].length() > bestLength && ! stopwords.contains(words[i].toLowerCase()) ) {
				bestLength = words[i].length();
				bestWord = words[i];
				bestIndex = i;
			}
		}
		words[bestIndex] = "";
		return bestWord;
	}

	protected String queryAnswersAPI(String question) throws MalformedURLException,
			UnsupportedEncodingException, ParserConfigurationException,
			SAXException, IOException, XPathExpressionException {
		System.out.println("Checking for yahoo answer to: [" + question + "]");
		
		URL url = new URL(resourcePath + "?appid=" + yahooId + 
				"&type=resolved&query=" + URLEncoder.encode(question, "utf-8") );
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(url.openStream());
		
		NodeList nodeSet = (NodeList) xpathExpression.evaluate(doc, XPathConstants.NODESET);
		for (int i = 0;  i < nodeSet.getLength(); i++) {
			String chosenAnswer = nodeSet.item(i).getTextContent();
			if (chosenAnswer != null && ! chosenAnswer.equals("")) {
				chosenAnswer = trimAnswer(chosenAnswer);
				return chosenAnswer;
			}
		}
		return null;
	}

	protected String trimAnswer(String chosenAnswer) {
		if (chosenAnswer.length() < 400) {
			return chosenAnswer;
		} else {
			String trimmedAnswer = chosenAnswer.substring(0, 400);
			int lastPeriod = trimmedAnswer.lastIndexOf('.');
			return trimmedAnswer.substring(0, lastPeriod);
		}
	}

	
	
}
