package edu.columbia.ccls.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class Tokenizer {
	
	private static Properties props = null;
	private static StanfordCoreNLP pipeline = null;
	
	/*
	static {
		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		pipeline = new StanfordCoreNLP();
	}
	*/
	
	/**
	 * 
	 * @param text
	 * @param caseSensitive
	 * @param regex
	 * @param useSNC
	 * @param useLemma
	 * @return
	 */
	public static ArrayList<String> extractTokens(String text, boolean caseSensitive, String regex, boolean useSCN, boolean useLemma) {
		ArrayList<String> tokens = new ArrayList<String>();
		
		if (useSCN) {	// use Stanford NLP Core
			// create an empty Annotation just with the given text
			text = text.replaceAll("\\s{5,}", "\n");
			Annotation document = new Annotation(text);
			
			// run all Annotators on this text
			if (Tokenizer.props == null) {
				Tokenizer.props = new Properties();
				Tokenizer.props.put("annotators", "tokenize, ssplit, pos, lemma");
				Tokenizer.pipeline = new StanfordCoreNLP();
			}
			pipeline.annotate(document);
			
			List<CoreLabel> tokensAnnotation = document.get(TokensAnnotation.class);
			
			for(CoreLabel token: tokensAnnotation) {
				String t = useLemma? token.lemma() : token.word();
				
				// case sensitive or not
				if (!caseSensitive) {
					t = t.toLowerCase();
				}
				
				// check whether it matches the given regular expression
				if (regex == null) {
					tokens.add(t);
				} else if (t.matches(regex)) {
					tokens.add(t);
				}
			}
		} else {	// do not use Stanford NLP Core
			Matcher m = Pattern.compile(regex).matcher(text);
			
			while (m.find()) {
				String t = m.group(0);
				if (!caseSensitive) {
					t = t.toLowerCase();
				}
				tokens.add(t);
			}
		}
		
		
		return tokens;
	}

}
