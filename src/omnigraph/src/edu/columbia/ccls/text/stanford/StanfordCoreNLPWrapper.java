package edu.columbia.ccls.text.stanford;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class StanfordCoreNLPWrapper {
	
	public static Properties props;
	public static StanfordCoreNLP pipeline;
	
	static {
		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		//props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		pipeline = new StanfordCoreNLP(props);
		System.out.println("loaded");
	}
	
	public static String getParseTree(String text) {
		StringBuilder parseTree = new StringBuilder();
		
		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);
		
		// run all Annotators on this text
		pipeline.annotate(document);
		
		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		for(CoreMap sentence: sentences) {
			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
			parseTree.append(tree);
		}
		
		return parseTree.toString();
	}
	
	public static void run() {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		
		// read some text in the text variable
		String text = "Stanford University is located in California. It is a great university."; // Add your text here!
		//text = "Crisis_Group 's President and CEO has been , since July 2009 , Louise_Arbour , former UN_High_Commissioner_for_Human_Rights and Chief_Prosecutor for the International_Criminal_Tribunals for the former Yugoslavia and for Rwanda .";
		//text = "When you see earnings hold up in a weakening economy, that allows stocks to keep their momentum and suggests these companies could really advance when the economy picks up.";
		System.out.println("text: " + text);
		
		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);
		
		// run all Annotators on this text
		pipeline.annotate(document);
		
		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		for(CoreMap sentence: sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				System.out.println("word: " + word);
				
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				System.out.println("pos: " + pos);
				
				// this is the NER label of the token
				String ne = token.get(NamedEntityTagAnnotation.class);
				System.out.println("ne: " + ne);
			}
			
			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
			System.out.println("tree: " + tree);
			
			// this is the Stanford dependency graph of the current sentence
			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			System.out.println("dependencies: " + dependencies);
		}
		
		// This is the coreference link graph
		// Each chain stores a set of mentions that link to each other,
		// along with a method for getting the most representative mention
		// Both sentence and token offsets start at 1!
		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		
		System.out.println(graph);

	}

	public static void run2() {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		
		// read some text in the text variable
		String text = "Stanford University is located in California. It is a great university."; // Add your text here!
		System.out.println("text: " + text);
		
		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);
		
		// run all Annotators on this text
		pipeline.annotate(document);
		
		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		for(Iterator<CoreMap> itSent = sentences.iterator(); itSent.hasNext(); ) {
			CoreMap sentence = itSent.next();
			
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (Iterator<CoreLabel> it = sentence.get(TokensAnnotation.class).iterator(); it.hasNext(); ) {
				CoreLabel token = it.next();
				
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				System.out.println("word: " + word);
				token.set(TextAnnotation.class, "1");
				
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				System.out.println("pos: " + pos);
				
				// this is the NER label of the token
				String ne = token.get(NamedEntityTagAnnotation.class);
				System.out.println("ne: " + ne);
			}
			
			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
			String s = toStringBuilder(tree, new StringBuilder(), true).toString();
			System.out.println("tree: " + s);
			
			// this is the Stanford dependency graph of the current sentence
			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			System.out.println("dependencies: " + dependencies);
		}
		
		// This is the coreference link graph
		// Each chain stores a set of mentions that link to each other,
		// along with a method for getting the most representative mention
		// Both sentence and token offsets start at 1!
		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);

	}
	
	public static StringBuilder toStringBuilder(Tree tree, StringBuilder sb, boolean printOnlyLabelValue) {
	    if (tree.isLeaf()) {
	      if (tree.label() != null) {
	        if(printOnlyLabelValue) {
	          //sb.append(tree.label().value());
	        	sb.append("1");
	        } else {
	          sb.append(tree.label());
	        }
	      }
	      return sb;
	    } else {
	      sb.append('(');
	      if (tree.label() != null) {
	        if (printOnlyLabelValue) {
	          if (tree.value() != null) {
	            sb.append(tree.label().value());
	          }
	          // don't print a null, just nothing!
	        } else {
	          sb.append(tree.label());
	        }
	      }
	      Tree[] kids = tree.children();
	      if (kids != null) {
	        for (Tree kid : kids) {
	          sb.append(' ');
	          toStringBuilder(kid, sb, printOnlyLabelValue);
	        }
	      }
	      return sb.append(')');
	    }
	  }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		StanfordCoreNLPWrapper.run();
		System.out.println("done.");

	}

}
