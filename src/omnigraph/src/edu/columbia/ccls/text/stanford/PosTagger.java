package edu.columbia.ccls.text.stanford;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class PosTagger {
	
	private static Properties props = null;
	private static StanfordCoreNLP pipeline = null;
	
	public static LinkedList<String[]> extractWordPos(String text) {
		LinkedList<String[]> wordPosList = new LinkedList<String[]>();
		
		if (PosTagger.props == null) {
			PosTagger.props = new Properties();
			PosTagger.props.put("annotators", "tokenize, ssplit, pos");
			PosTagger.pipeline = new StanfordCoreNLP(PosTagger.props);
		}
		
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		for(CoreMap sentence: sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				String[] wordPos = new String[2];
				
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				wordPos[0] = word;
				
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				wordPos[1] = pos;
				
				wordPosList.add(wordPos);
			}
		}
		
		return wordPosList;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String text = "Stanford University is located in California. It is a great university."; // Add your text here!
		System.out.println(text);
		LinkedList<String[]> wordPosList = PosTagger.extractWordPos(text);
		
		for (String[] wordPos : wordPosList) {
			System.out.println(wordPos[0] + ": " + wordPos[1]);
		}

	}

}
