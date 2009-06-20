package sonofcim;

import java.util.Random;

public class LunchChooser {

	protected Random random = new Random();
	protected String lunchPlaces[] = 
		{"ralph's", "mama's", "potbellys", "corner bakery", "liberty place",
		 "q'doba", "tampopo", "gia pronto", "la scala's", "cosi"};
			
	public String getLunch() {
		int randNum = random.nextInt(lunchPlaces.length);
		return lunchPlaces[randNum];
	}
	
}
