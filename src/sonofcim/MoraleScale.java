package sonofcim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.joda.time.DateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.DecisionTreeTrainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class MoraleScale {

	protected HashSet<String> positiveWords = new HashSet<String>();
	protected HashSet<String> negativeWords = new HashSet<String>();
	
	protected HashMap<String, Class> trainingMethods = new HashMap<String, Class>();
	
	protected float positiveWordCount = 1;
	protected float negativeWordCount = 1;
	
	protected HashSet<String> people = new HashSet<String>();
	protected HashMap<String, Float> positivePersonScore = new HashMap<String, Float>();
	protected HashMap<String, Float> negativePersonScore = new HashMap<String, Float>();
	
	protected HashMap<String, String> personLastLabel = new HashMap<String, String>();
	protected HashMap<String, Integer> personStreak = new HashMap<String, Integer>();
	
	protected int day = new DateTime().getDayOfMonth();
	
	protected String lastLabel = "";
	protected int consecutiveStatements = 0;
	boolean reportTrend = true;
	
	DecimalFormat decimalFormat = new DecimalFormat("#0.00");
	
	JdbcTemplate jdbcTemplate;
	Pipe instancePipe;
	Classifier classifier;
	
	public MoraleScale(JdbcTemplate jdbcTemplate) {
        HttpClient httpClient = new HttpClient();

        try {
            GetMethod get = new GetMethod("http://cloud.github.com/downloads/mmattozzi/sonofcim/positive_words.txt");
            httpClient.executeMethod(get);
			loadWords(positiveWords, get.getResponseBodyAsStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
            GetMethod get = new GetMethod("http://cloud.github.com/downloads/mmattozzi/sonofcim/negative_words.txt");
            httpClient.executeMethod(get);
			loadWords(negativeWords, get.getResponseBodyAsStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.jdbcTemplate = jdbcTemplate;
		
		 // Create the pipeline that will take as input {data = File, target = String for classname}
        // and turn them into {data = FeatureVector, target = Label}
        instancePipe = new SerialPipes (new Pipe[] {
                new Target2Label (),                                                      // Target String -> class label
                new Input2CharSequence (),                                // Data File -> String containing contents
                new CharSequence2TokenSequence (),  // Data String -> TokenSequence
                new TokenSequenceLowercase (),            // TokenSequence words lowercased
                new TokenSequenceRemoveStopwords (),// Remove stopwords from sequence
                new TokenSequence2FeatureSequence(),// Replace each Token with a feature index
                new FeatureSequence2FeatureVector(),// Collapse word order into a "feature vector"
                new PrintInputAndTarget(),
        });
        
        trainingMethods.put("decision-tree", DecisionTreeTrainer.class);
        trainingMethods.put("max-entropy", MaxEntTrainer.class);
        trainingMethods.put("naive-bayes", NaiveBayesTrainer.class);
	}

	public static void loadWords(Collection<String> wordSet, InputStream inputStream) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line = reader.readLine();
			while (line != null) {
				wordSet.add(line.toLowerCase());
				line = reader.readLine();
			}
			System.out.println("MoraleScale: Added " + wordSet.size() + " words.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected String evaluateMorale(String mesg, String sender) {
		sender = sender.replaceAll("_", "");
		
		if (! people.contains(sender)) people.add(sender);
		
		Classification classification = classifier.classify(instancePipe.instanceFrom(new Instance(mesg, "", "eval", mesg)));
        String label = classification.getLabeling().getBestLabel().toString();
        Double value = classification.getLabeling().getBestValue();
        if (value >= .6) {
	        if (label.equals("good")) {
	        	positiveWordCount += 1;
	        	addToSenderMorale(sender, positivePersonScore, 1);
	        } else {
	        	negativeWordCount += 1;
	        	addToSenderMorale(sender, negativePersonScore, 1);
	        }
	    } 
        
        String personStreak = addToPersonMoraleStreak(sender, label);
		calculateMorale();
		
		if (reportTrend) {
			return checkTrend(label) + (personStreak != null ? " " + personStreak : "");
		} else {
			return null;
		}
	}
	
	private String addToPersonMoraleStreak(String sender, String label) {
		if (personLastLabel.containsKey(sender)) {
			int streak = personStreak.get(sender);
			String lastLabel = personLastLabel.get(sender);
			if (lastLabel.equals(label)) {
				streak++;
				personStreak.put(sender, streak);
				if (streak % 10 == 0 && streak > 19) {
					return sender + " has trended " + label + " for " + streak + " statements. Morale = [" + calculateMorale() + "]";
				} else {
					return null;
				}
			} else {
				personStreak.put(sender, 1);
				personLastLabel.put(sender, label);
				return null;
			}
		} else {
			personLastLabel.put(sender, label);
			personStreak.put(sender, 1);
			return null;
		}
	}

	public void setReportTrend(boolean reportTrend) {
		reportTrend = reportTrend;
	}
	
	protected String checkTrend(String label) {
		if (label.equals(lastLabel)) {
			consecutiveStatements++;
		} else {
			lastLabel = label;
			consecutiveStatements = 1;
		}
		if (consecutiveStatements > 19 && consecutiveStatements % 10 == 0) {
			return "Morale has trended " + label + " for the last " + consecutiveStatements + " statements.";
		} else {
			return "";
		}
	}
	
	protected void addToSenderMorale(String sender, HashMap<String, Float> map, float value) {
		if (map.containsKey(sender)) {
			map.put(sender, map.get(sender) + value);
		} else {
			map.put(sender, 1 + value);
		}
	}
	
	protected void reset() {
		positiveWordCount = 1;
		negativeWordCount = 1;
		positivePersonScore.clear();
		negativePersonScore.clear();
	}
	
	protected float calculateMorale() {
		int currentDayOfMonth = new DateTime().getDayOfMonth();
		if (day != currentDayOfMonth) {
			day = currentDayOfMonth;
			System.out.println("Resetting day.");
			reset();
		}
		return 100f * (positiveWordCount / (positiveWordCount + negativeWordCount));
	}
	
	protected String getMorale() {
		return "Morale is at: " + calculateMorale() + "%";
	}

	public void train(String technique) {
		final InstanceList ilist = new InstanceList (instancePipe);
		
		jdbcTemplate.query("SELECT nick, message, classification, id FROM messages WHERE classification != 'neutral'", new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				ilist.addThruPipe(new Instance(rs.getString("nick") + " " + rs.getString("message"), 
						rs.getString("classification"), rs.getString("id"), rs.getString("message")));
			}			
		});
		
		ClassifierTrainer trainer = null;
		Class trainingMethod = trainingMethods.get(technique);
		if (trainingMethod == null) {
			trainingMethod = trainingMethods.get("naive-bayes");
		}
		try {
			trainer = (ClassifierTrainer) trainingMethod.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
        classifier = trainer.train(ilist);
        System.out.println("Finished training.");
	}

	public String evaluate(String mesg) {
		mesg = trimEndWhitespace(mesg);
		if (people.contains(mesg)) {
			float score = getPositivePersonScore(mesg);
			String res = "";
			if (score > .5) {
				res = "good " + score;
			} else if (score == .5) {
				res = "neutral 0.5";
			} else {
				res = "bad " + (1 - score);
			}
			return res + " [evaluated as person]";
		} else {
			Classification classification = classifier.classify(instancePipe.instanceFrom(new Instance(mesg, "", "eval", mesg)));
	        String res = classification.getLabeling().getBestLabel() + ": " + classification.getLabeling().getBestValue() + " [evaluated as phrase]";
	        return res;
		}
	}

	public String trimEndWhitespace(String s) {
		while (s.endsWith(" ")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}
	
	public float getPositivePersonScore(String name) {
		if (positivePersonScore.containsKey(name)) {
			if (negativePersonScore.containsKey(name)) {
				float score = positivePersonScore.get(name) / (positivePersonScore.get(name) + negativePersonScore.get(name));
				return score;
			} else {
				return 1f;
			}
		} else {
			if (negativePersonScore.containsKey(name))
				return 0f;
			else
				return .5f;
		}
	}
	
	public String getTrainingTechniques() {
		return trainingMethods.keySet().toString();
	}

	public String getMoraleRankings() {
		
		List<PersonScore> scores = new ArrayList<PersonScore>();
		
		for (String name : people) {
			float score = getPositivePersonScore(name);
			PersonScore p = new PersonScore(name, score);
			scores.add(p);
		}
		
		String result = "";
		
		Collections.sort(scores);
		
		for (PersonScore p : scores) {
			result += "_" + p + ", ";
		}
		
		return result.substring(0, result.length() - 2);
	}
	
	public class PersonScore implements Comparable<PersonScore>{

		protected String name;
		protected float score;
		
		public PersonScore(String name, float score) {
			this.name = name;
			this.score = score;
		}
		
		public int compareTo(PersonScore o) {
			float f = this.score - o.score;
			if (f == 0) return 0;
			else if (f > 0) return -1;
			else return 1;
		}
		
		@Override
		public String toString() {
			return name + " " + decimalFormat.format(score);
		}
	}
}
