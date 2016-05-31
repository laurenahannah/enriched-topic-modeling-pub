package edu.columbia.ccls.text;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.columbia.ccls.semantics.FrameVectorSpaceModel;
import edu.columbia.ccls.text.stanford.StanfordCoreNLPWrapper;
import edu.columbia.ccls.util.Tools;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class CorpusAgent {
	
	public static final String WORD_REGEX = "\\b[A-Za-z][A-Za-z0-9\\-_&]+\\b";
	public String wordRegex = CorpusAgent.WORD_REGEX;	//"[A-Za-z][A-Za-z0-9\\-_]+";	//"[A-Za-z0-9_\\+\\-\\^]+";	//"[A-Za-z0-9&]+";
	
	protected static final boolean DEBUG = true;
	
	protected TreeSet<String> stopwords = null;
	protected TreeSet<String> scopewords = null;
	protected Double thresholdUnigram = Double.MIN_VALUE;
	protected double limitUnigram = Double.MAX_VALUE;
	protected Double thresholdBigram = Double.MIN_VALUE;
	protected double limitBigram = Double.MAX_VALUE;
	protected Double thresholdTrigram = Double.MIN_VALUE;
	protected double limitTrigram = Double.MAX_VALUE;
	protected String name = null;
	protected TreeMap<String, Integer> attributeIndexMap = null;
	protected TreeMap<Integer, String> indexAttributeMap = null;
	protected boolean keepSequence = false;//true;
	protected boolean keepSyntax = false;//true;
	protected boolean caseSensitive = false;
	protected boolean useLemma = true;
	protected boolean useSCN = false;
	protected String format = "lda-c";
	protected String labelFilename = null;
	protected String attributesFilename = null;
	protected String corpusDir = null;
	protected String outDir = null;
	protected String docListFilename = null;
	protected LinkedList<String> docList = null;
	protected HashMap<String, String> docLabelMap = null;
	protected TreeSet<String> labelSet = null;
	protected String attributeIdfFilename = null;
	protected TreeMap<String, Double> attributeIdfMap = null;
	protected boolean useTfidf = false;
	
	public String getWordRegex() {
		return wordRegex;
	}

	public void setWordRegex(String wordRegex) {
		this.wordRegex = wordRegex;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getThresholdUnigram() {
		return thresholdUnigram;
	}

	public void setThresholdUnigram(Double thresholdUnigram) {
		this.thresholdUnigram = thresholdUnigram;
	}

	public double getLimitUnigram() {
		return limitUnigram;
	}

	public void setLimitUnigram(double limitUnigram) {
		this.limitUnigram = limitUnigram;
	}
	
	public Double getThresholdBigram() {
		return thresholdBigram;
	}

	public void setThresholdBigram(Double thresholdBigram) {
		this.thresholdBigram = thresholdBigram;
	}

	public double getLimitBigram() {
		return limitBigram;
	}

	public void setLimitBigram(double limitBigram) {
		this.limitBigram = limitBigram;
	}
	
	public Double getThresholdTrigram() {
		return thresholdTrigram;
	}

	public void setThresholdTrigram(Double thresholdTrigram) {
		this.thresholdTrigram = thresholdTrigram;
	}

	public double getLimitTrigram() {
		return limitTrigram;
	}

	public void setLimitTrigram(double limitTrigram) {
		this.limitTrigram = limitTrigram;
	}

	public TreeSet<String> getStopwords() {
		return stopwords;
	}

	public void setStopwords(TreeSet<String> stopwords) {
		this.stopwords = stopwords;
	}
	
	public void setStopwords(String stopwordsFilename) {
		stopwords = new TreeSet<String>();
		String[] s = Tools.read(stopwordsFilename).split("\\s+");
		for (String ss : s) {
			stopwords.add(ss);
		}
	}

	public TreeSet<String> getScopewords() {
		return scopewords;
	}

	public void setScopewords(TreeSet<String> scopewords) {
		this.scopewords = scopewords;
	}
	
	public void setScopewords(String scopewordsFilename) {
		scopewords = new TreeSet<String>();
		String[] s = Tools.read(scopewordsFilename).split("\\s+");
		for (String ss : s) {
			scopewords.add(ss);
		}
	}
	
	public boolean isKeepSequence() {
		return keepSequence;
	}

	public void setKeepSequence(boolean keepSequence) {
		this.keepSequence = keepSequence;
	}

	public boolean isKeepSyntax() {
		return keepSyntax;
	}

	public void setKeepSyntax(boolean keepSyntax) {
		this.keepSyntax = keepSyntax;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public boolean isUseLemma() {
		return useLemma;
	}

	public void setUseLemma(boolean useLemma) {
		this.useLemma = useLemma;
	}

	public boolean isUseSCN() {
		return useSCN;
	}

	public void setUseSCN(boolean useSCN) {
		this.useSCN = useSCN;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getLabelFilename() {
		return labelFilename;
	}

	public void setLabelFilename(String labelFilename) {
		this.labelFilename = labelFilename;
	}
	
	public String getCorpusDir() {
		return corpusDir;
	}

	public void setCorpusDir(String corpusDir) {
		this.corpusDir = corpusDir;
	}
	
	public String getOutDir() {
		return outDir;
	}

	public void setOutDir(String outDir) {
		this.outDir = outDir;
	}
	
	public String getDocListFilename() {
		return docListFilename;
	}

	public void setDocListFilename(String docListFilename) {
		this.docListFilename = docListFilename;
	}

	public String getAttributesFilename() {
		return attributesFilename;
	}

	public void setAttributesFilename(String attributesFilename) {
		this.attributesFilename = attributesFilename;
	}

	public TreeMap<String, Integer> getAttributeIndexMap() {
		return attributeIndexMap;
	}

	public void setAttributeIndexMap(TreeMap<String, Integer> attributeIndexMap) {
		this.attributeIndexMap = attributeIndexMap;
	}

	public void setAttributeIndexMap(String[] s) {
		TreeMap<String, Integer> attributeIndexMap = new TreeMap<String, Integer>();
		
		for (int i = 0; i < s.length; i++) {
			attributeIndexMap.put(s[i], i);
		}
		
		this.attributeIndexMap = attributeIndexMap;
	}
	
	public TreeMap<Integer, String> getIndexAttributeMap() {
		return indexAttributeMap;
	}

	public void setIndexAttributeMap(TreeMap<Integer, String> indexAttributeMap) {
		this.indexAttributeMap = indexAttributeMap;
	}
	
	public void setIndexAttributeMap(String[] s) {
		TreeMap<Integer, String> indexAttributeMap = new TreeMap<Integer, String>();
		
		for (int i = 0; i < s.length; i++) {
			indexAttributeMap.put(i, s[i]);
		}
		
		this.indexAttributeMap = indexAttributeMap;
	}
	
	public String getAttributeIdfFilename() {
		return attributeIdfFilename;
	}

	public void setAttributeIdfFilename(String attributeIdfFilename) {
		this.attributeIdfFilename = attributeIdfFilename;
	}

	public TreeMap<String, Double> getAttributeIdfMap() {
		return attributeIdfMap;
	}

	public void setAttributeIdfMap(TreeMap<String, Double> attributeIdfMap) {
		this.attributeIdfMap = attributeIdfMap;
	}
	
	public LinkedList<String> getDirectDocList() {
		return this.docList;
	}
	
	public void setAttributeIdfMap() {
		System.out.println("begin set attribute idf...");
		if (this.attributeIdfFilename != null) {
			if (this.attributeIdfMap == null) {
				this.attributeIdfMap = new TreeMap<String, Double>();
			}
			String[] lines = Tools.read(this.attributeIdfFilename).split("\n");
			for (String line : lines) {
				String[] cols = line.split(",");
				String attribute = "";
				Double idf = 1.0;
				if (cols.length == 2) {
					attribute = cols[0];
					idf = Double.parseDouble(cols[1]);
				} else {
					idf = Double.parseDouble(cols[cols.length - 1]);
					for (int i = 0; i < cols.length - 1; i++) {
						attribute += cols[i];
					}
				}
				
				this.attributeIdfMap.put(attribute, idf);
			}
		} else {
			HashMap<Integer, Integer> attributeIdxDfMap = new HashMap<Integer, Integer>();
			for (Integer idx : this.indexAttributeMap.keySet()) {
				attributeIdxDfMap.put(idx, 0);
			}
			
			LinkedList<String> directDocList = this.getDirectDocList();
			for (String filename : directDocList) {
				LinkedHashMap<Integer, Object> data = getDocumentData(filename);
				for (Integer idx : data.keySet()) {
					if (idx < 0) {
						continue;
					}
					
					// assumes that it's sparse!!!!!!!
					attributeIdxDfMap.put(idx, attributeIdxDfMap.get(idx) + 1);
				}
			}
			
			if (this.attributeIdfMap == null) {
				this.attributeIdfMap = new TreeMap<String, Double>();
			}
			
			for (Integer idx : attributeIdxDfMap.keySet()) {
				double idf = Math.log(1.0 * directDocList.size() / (1.0 + attributeIdxDfMap.get(idx)));
				this.attributeIdfMap.put(this.indexAttributeMap.get(idx), idf);
				
				if (this.indexAttributeMap.get(idx).startsWith(FrameVectorSpaceModel.PRIOR_POLARITY_FEATURE_PREFIX)) {
					this.attributeIdfMap.put(this.indexAttributeMap.get(idx), 1.0);
				}
			}
			
			StringBuilder sb = new StringBuilder();
			for (String attribute : this.attributeIdfMap.keySet()) {
				sb.append(attribute + "," + this.attributeIdfMap.get(attribute) + "\n");
			}
			String idfFilename = this.outDir + "/" + this.name + "_attributes_idf.csv";
			Tools.write(idfFilename, sb.toString());
			
			LinkedHashMap<String, Double> mapSorted = Tools.sortMapByValue(this.attributeIdfMap);
			StringBuilder sbSorted = new StringBuilder();
			for (String a : mapSorted.keySet()) {
				sbSorted.append(a + "," + mapSorted.get(a) + "\n");
			}
			String idfSortedFilename = this.outDir + "/" + this.name + "_attributes_idf_sorted.csv";
			Tools.write(idfSortedFilename, sbSorted.toString());
		}
		System.out.println("end set attribute idf...");
	}

	public boolean isUseTfidf() {
		return useTfidf;
	}

	public void setUseTfidf(boolean useTfidf) {
		this.useTfidf = useTfidf;
	}

	/**
	 * Extract the terms with to their frequencies
	 * @param dir
	 */
	protected TreeMap<String, Integer> getCorpusFrequentUnigrams(String regex) {
		// use a map to record the term frequency for each term
		TreeMap<String, Integer> termFreqMap = new TreeMap<String, Integer>();
		
		if (this.thresholdUnigram == null) {
			return termFreqMap;
		}
		
		for (String filename : this.docList) {
			System.out.println("processing file: " + filename);
			
			String content = Tools.read(filename);
			/*
			//String wordRegex = "[A-Za-z](?:[A-Za-z0-9]|[.'-][A-Za-z0-9])+";//ADD 0-9 FOR NOS AS WELL
			Matcher m = Pattern.compile(regex).matcher(content);
			
			while (m.find()) {
				String term = m.group(0);
				term = term.toLowerCase();
				
				if (stopwords != null && stopwords.contains(term)) {
					continue;
				}
				
				if (scopewords != null && !scopewords.contains(term)) {
					continue;
				}
				
				if (termFreqMap.keySet().contains(term)) {
					termFreqMap.put(term, termFreqMap.get(term) + 1);
				} else {
					termFreqMap.put(term, 1);
				}
			}
			*/
			
			// use the tokenization from stanford nlp core
			ArrayList<String> terms = Tokenizer.extractTokens(content, caseSensitive, regex, useSCN, useLemma);
			for (String term : terms) {
				if (stopwords != null && stopwords.contains(term)) {
					continue;
				}
				
				if (scopewords != null && !scopewords.contains(term)) {
					continue;
				}
				
				if (termFreqMap.keySet().contains(term)) {
					termFreqMap.put(term, termFreqMap.get(term) + 1);
				} else {
					termFreqMap.put(term, 1);
				}
			}
		}
		System.out.println("loaded all terms.");
		
		if (DEBUG) {
			System.out.println("in getCorpusFrequentUnigrams, selected features:");
			for (String term : termFreqMap.keySet()) {
				System.out.println(term + ":" + termFreqMap.get(term));
			}
		}
		
		return termFreqMap;
	}
	
	/**
	 * Extract the bigrams with to their frequencies
	 * @param dir
	 */
	protected TreeMap<String, Integer> getCorpusFrequentBigrams(String regex) {
		// use a map to record the term frequency for each term
		TreeMap<String, Integer> termFreqMap = new TreeMap<String, Integer>();
		
		if (this.thresholdBigram == null) {
			return termFreqMap;
		}
		
		for (String filename : this.docList) {
			System.out.println("processing file: " + filename);
			
			String content = Tools.read(filename);
			
			// use the tokenization from stanford nlp core
			ArrayList<String> tokens = Tokenizer.extractTokens(content, caseSensitive, regex, useSCN, useLemma);
			
			if (tokens.size() < 2) {
				continue;
			}
			ArrayList<String> terms = new ArrayList<String>();
			String previous = tokens.get(0);
			for (int i = 1; i < tokens.size(); i++) {
				String current = tokens.get(i);
				
				// stop word list contains the individual word
				if (stopwords != null && (stopwords.contains(previous) || stopwords.contains(current))) {
					previous = current;
					continue;
				}
				
				if (scopewords != null && (!scopewords.contains(previous) || !scopewords.contains(current))) {
					previous = current;
					continue;
				}
				
				String bigram = previous + " " + current;
				terms.add(bigram);
				previous = current;
			}
			
			for (String term : terms) {
				// the stop word list contains the bigram
				if (stopwords != null && stopwords.contains(term)) {
					continue;
				}
				
				if (scopewords != null && !scopewords.contains(term)) {
					continue;
				}
				
				if (termFreqMap.keySet().contains(term)) {
					termFreqMap.put(term, termFreqMap.get(term) + 1);
				} else {
					termFreqMap.put(term, 1);
				}
			}
		}
		System.out.println("loaded all terms.");
		
		if (DEBUG) {
			System.out.println("in getCorpusFrequentBigrams, selected features:");
			for (String term : termFreqMap.keySet()) {
				System.out.println(term + ":" + termFreqMap.get(term));
			}
		}
		
		return termFreqMap;
	}
	
	/**
	 * Extract the trigrams with to their frequencies
	 * @param dir
	 */
	protected TreeMap<String, Integer> getCorpusFrequentTrigrams(String regex) {
		// use a map to record the term frequency for each term
		TreeMap<String, Integer> termFreqMap = new TreeMap<String, Integer>();
		
		if (this.thresholdTrigram == null) {
			return termFreqMap;
		}
		
		for (String filename : this.docList) {
			System.out.println("processing file: " + filename);
			
			String content = Tools.read(filename);
			
			// use the tokenization from stanford nlp core
			ArrayList<String> tokens = Tokenizer.extractTokens(content, caseSensitive, regex, useSCN, useLemma);
			
			if (tokens.size() < 3) {
				continue;
			}
			ArrayList<String> terms = new ArrayList<String>();
			String previousPrevious = tokens.get(0);
			String previous = tokens.get(1);
			for (int i = 2; i < tokens.size(); i++) {
				String current = tokens.get(i);
				
				// stop word list contains the individual word
				if (stopwords != null && (stopwords.contains(previousPrevious) || stopwords.contains(previous) || stopwords.contains(current))) {
					previous = current;
					continue;
				}
				
				if (scopewords != null && (!scopewords.contains(previousPrevious) || !scopewords.contains(previous) || !scopewords.contains(current))) {
					previous = current;
					continue;
				}
				
				String trigram = previousPrevious + " " + previous + " " + current;
				terms.add(trigram);
				previousPrevious = previous;
				previous = current;
			}
			
			for (String term : terms) {
				// the stop word list contains the bigram
				if (stopwords != null && stopwords.contains(term)) {
					continue;
				}
				
				if (scopewords != null && !scopewords.contains(term)) {
					continue;
				}
				
				if (termFreqMap.keySet().contains(term)) {
					termFreqMap.put(term, termFreqMap.get(term) + 1);
				} else {
					termFreqMap.put(term, 1);
				}
			}
		}
		System.out.println("loaded all terms.");
		
		if (DEBUG) {
			System.out.println("in getCorpusFrequentTrigrams, selected features:");
			for (String term : termFreqMap.keySet()) {
				System.out.println(term + ":" + termFreqMap.get(term));
			}
		}
		
		return termFreqMap;
	}
	
	/**
	 * Rank the terms according to their frequencies
	 * @param dir
	 */
	protected LinkedHashMap<String, Integer> rankCorpusFrequentUnigrams(String regex) {
		return Tools.sortMapByValue(getCorpusFrequentUnigrams(regex));
	}
	
	/**
	 * Rank the bigrams according to their frequencies
	 * @param dir
	 */
	protected LinkedHashMap<String, Integer> rankCorpusFrequentBigrams(String regex) {
		return Tools.sortMapByValue(getCorpusFrequentBigrams(regex));
	}
	
	/**
	 * Rank the trigrams according to their frequencies
	 * @param dir
	 */
	protected LinkedHashMap<String, Integer> rankCorpusFrequentTrigrams(String regex) {
		return Tools.sortMapByValue(getCorpusFrequentTrigrams(regex));
	}
	
	/**
	 * Rank the terms according to their frequencies
	 * @param dir
	 */
	protected TreeMap<String, Integer> getDocumentFrequentUnigrams(String filename, String regex) {
		// use a map to record the term frequency for each term
		TreeMap<String, Integer> termFreqMap = new TreeMap<String, Integer>();
		
		String content = Tools.read(filename);
		
		//String wordRegex = "[A-Za-z](?:[A-Za-z0-9]|[.'-][A-Za-z0-9])+";//ADD 0-9 FOR NOS AS WELL
		Matcher m = Pattern.compile(regex).matcher(content);
		
		while (m.find()) {
			String term = m.group(0);
			term = term.toLowerCase();
			
			if (stopwords != null && stopwords.contains(term)) {
				continue;
			}
			
			if (scopewords != null && !scopewords.contains(term)) {
				continue;
			}
			
			if (termFreqMap.keySet().contains(term)) {
				termFreqMap.put(term, termFreqMap.get(term) + 1);
			} else {
				termFreqMap.put(term, 1);
			}
		}
		
		return termFreqMap;
	}
	
	/**
	 * Rank the terms according to their frequencies
	 * @param dir
	 */
	protected TreeMap<String, Integer> getDocumentFrequentUnigrams(String filename, Set<String> attributeSpace) {
		// use a map to record the term frequency for each term
		TreeMap<String, Integer> termFreqMap = new TreeMap<String, Integer>();
		
		String content = Tools.read(filename);
		
		//String wordRegex = "[A-Za-z](?:[A-Za-z0-9]|[.'-][A-Za-z0-9])+";//ADD 0-9 FOR NOS AS WELL
		Matcher m = Pattern.compile(this.wordRegex).matcher(content);
		
		while (m.find()) {
			String term = m.group(0);
			term = term.toLowerCase();
			
			if (!attributeSpace.contains(term)) {
				continue;
			}
			
			if (termFreqMap.keySet().contains(term)) {
				termFreqMap.put(term, termFreqMap.get(term) + 1);
			} else {
				termFreqMap.put(term, 1);
			}
		}
		
		return termFreqMap;
	}
	
	
	protected TreeMap<String, Integer> getDocumentFrequentBigrams(String filename, String regex) {
		// use a map to record the term frequency for each term
		TreeMap<String, Integer> termFreqMap = new TreeMap<String, Integer>();
		
		String content = Tools.read(filename);
		
		// use the tokenization from stanford nlp core
		ArrayList<String> tokens = Tokenizer.extractTokens(content, caseSensitive, regex, useSCN, useLemma);
		
		if (tokens.size() < 2) {
			return termFreqMap;
		}
		ArrayList<String> terms = new ArrayList<String>();
		String previous = tokens.get(0);
		for (int i = 1; i < tokens.size(); i++) {
			String current = tokens.get(i);
			
			// stop word list contains the individual word
			if (stopwords != null && (stopwords.contains(previous) || stopwords.contains(current))) {
				previous = current;
				continue;
			}
			
			if (scopewords != null && (!scopewords.contains(previous) || !scopewords.contains(current))) {
				previous = current;
				continue;
			}
			
			String bigram = previous + " " + current;
			terms.add(bigram);
			previous = current;
		}
		
		for (String term : terms) {
			// the stop word list contains the bigram
			if (stopwords != null && stopwords.contains(term)) {
				continue;
			}
			
			if (scopewords != null && !scopewords.contains(term)) {
				continue;
			}
			
			if (termFreqMap.keySet().contains(term)) {
				termFreqMap.put(term, termFreqMap.get(term) + 1);
			} else {
				termFreqMap.put(term, 1);
			}
		}
		
		return termFreqMap;
	}
	
	protected TreeMap<String, Integer> getDocumentFrequentBigrams(String filename, Set<String> attributeSpace) {
		// use a map to record the term frequency for each term
		TreeMap<String, Integer> termFreqMap = new TreeMap<String, Integer>();
		
		String content = Tools.read(filename);
		
		// use the tokenization from stanford nlp core
		ArrayList<String> tokens = Tokenizer.extractTokens(content, caseSensitive, this.wordRegex, useSCN, useLemma);
		
		if (tokens.size() < 2) {
			return termFreqMap;
		}
		ArrayList<String> terms = new ArrayList<String>();
		String previous = tokens.get(0);
		for (int i = 1; i < tokens.size(); i++) {
			String current = tokens.get(i);
			
			// stop word list contains the individual word
			if (stopwords != null && (stopwords.contains(previous) || stopwords.contains(current))) {
				previous = current;
				continue;
			}
			
			if (scopewords != null && (!scopewords.contains(previous) || !scopewords.contains(current))) {
				previous = current;
				continue;
			}
			
			String bigram = previous + " " + current;
			terms.add(bigram);
			previous = current;
		}
		
		for (String term : terms) {
			if (!attributeSpace.contains(term)) {
				continue;
			}
			
			if (termFreqMap.keySet().contains(term)) {
				termFreqMap.put(term, termFreqMap.get(term) + 1);
			} else {
				termFreqMap.put(term, 1);
			}
		}
		
		return termFreqMap;
	}
	
	protected TreeMap<String, Integer> getDocumentFrequentTrigrams(String filename, String regex) {
		// use a map to record the term frequency for each term
		TreeMap<String, Integer> termFreqMap = new TreeMap<String, Integer>();
		
		String content = Tools.read(filename);
		
		// use the tokenization from stanford nlp core
		ArrayList<String> tokens = Tokenizer.extractTokens(content, caseSensitive, regex, useSCN, useLemma);
		
		if (tokens.size() < 3) {
			return termFreqMap;
		}
		ArrayList<String> terms = new ArrayList<String>();
		String previousPrevious = tokens.get(0);
		String previous = tokens.get(1);
		for (int i = 2; i < tokens.size(); i++) {
			String current = tokens.get(i);
			
			// stop word list contains the individual word
			if (stopwords != null && (stopwords.contains(previousPrevious) || stopwords.contains(previous) || stopwords.contains(current))) {
				previous = current;
				continue;
			}
			
			if (scopewords != null && (!scopewords.contains(previousPrevious) || !scopewords.contains(previous) || !scopewords.contains(current))) {
				previous = current;
				continue;
			}
			
			String trigram = previousPrevious + " " + previous + " " + current;
			terms.add(trigram);
			previousPrevious = previous;
			previous = current;
		}
		
		for (String term : terms) {
			// the stop word list contains the bigram
			if (stopwords != null && stopwords.contains(term)) {
				continue;
			}
			
			if (scopewords != null && !scopewords.contains(term)) {
				continue;
			}
			
			if (termFreqMap.keySet().contains(term)) {
				termFreqMap.put(term, termFreqMap.get(term) + 1);
			} else {
				termFreqMap.put(term, 1);
			}
		}
		
		return termFreqMap;
	}
	
	protected TreeMap<String, Integer> getDocumentFrequentTrigrams(String filename, Set<String> attributeSpace) {
		// use a map to record the term frequency for each term
		TreeMap<String, Integer> termFreqMap = new TreeMap<String, Integer>();
		
		String content = Tools.read(filename);
		
		// use the tokenization from stanford nlp core
		ArrayList<String> tokens = Tokenizer.extractTokens(content, caseSensitive, this.wordRegex, useSCN, useLemma);
		
		if (tokens.size() < 3) {
			return termFreqMap;
		}
		ArrayList<String> terms = new ArrayList<String>();
		String previousPrevious = tokens.get(0);
		String previous = tokens.get(1);
		for (int i = 2; i < tokens.size(); i++) {
			String current = tokens.get(i);
			
			// stop word list contains the individual word
			if (stopwords != null && (stopwords.contains(previousPrevious) || stopwords.contains(previous) || stopwords.contains(current))) {
				previous = current;
				continue;
			}
			
			if (scopewords != null && (!scopewords.contains(previousPrevious) || !scopewords.contains(previous) || !scopewords.contains(current))) {
				previous = current;
				continue;
			}
			
			String trigram = previousPrevious + " " + previous + " " + current;
			terms.add(trigram);
			previousPrevious = previous;
			previous = current;
		}
		
		for (String term : terms) {
			if (!attributeSpace.contains(term)) {
				continue;
			}
			
			if (termFreqMap.keySet().contains(term)) {
				termFreqMap.put(term, termFreqMap.get(term) + 1);
			} else {
				termFreqMap.put(term, 1);
			}
		}
		
		return termFreqMap;
	}
	
	/*
	public LinkedHashMap<Integer, Object> getDocumentData(String filename, String regex) {
		LinkedHashMap<Integer, Object> data = new LinkedHashMap<Integer, Object>();
		
		HashMap<String, Integer> termFreqMap = new HashMap<String, Integer>();
		String content = Tools.read(filename);
		Matcher m = Pattern.compile(regex).matcher(content);
		while (m.find()) {
			String term = m.group(0);
			term = term.toLowerCase();
			
			if (!attributeIndexMap.keySet().contains(term)) {
				continue;
			}
			
			if (termFreqMap.keySet().contains(term)) {
				termFreqMap.put(term, termFreqMap.get(term) + 1);
			} else {
				termFreqMap.put(term, 1);
			}
		}
		
		for (Integer idx : indexAttributeMap.keySet()) {
			String attribute = indexAttributeMap.get(idx);
			if (termFreqMap.containsKey(attribute) && termFreqMap.get(attribute) != 0) {
				data.put(idx, termFreqMap.get(attribute));
			}
		}
		
		return data;
	}
	*/
	
	public LinkedHashMap<Integer, Object> getDocumentData(String filename) {
		return getDocumentDataNGram(filename);
	}
	
	public LinkedHashMap<Integer, Object> getDocumentDataNGram(String filename) {
		LinkedHashMap<Integer, Object> data = new LinkedHashMap<Integer, Object>();
		
		TreeMap<String, Integer> dataUnigram = getDocumentFrequentUnigrams(filename, attributeIndexMap.keySet());
		TreeMap<String, Integer> dataBigram = getDocumentFrequentBigrams(filename, attributeIndexMap.keySet());
		TreeMap<String, Integer> dataTrigram = getDocumentFrequentTrigrams(filename, attributeIndexMap.keySet());
		
		HashMap<String, Integer> termFreqMap = new HashMap<String, Integer>();
		termFreqMap.putAll(dataUnigram);
		termFreqMap.putAll(dataBigram);
		termFreqMap.putAll(dataTrigram);
		
		for (Integer idx : indexAttributeMap.keySet()) {
			String attribute = indexAttributeMap.get(idx);
			if (termFreqMap.containsKey(attribute) && termFreqMap.get(attribute) != 0) {
				if (this.useTfidf && this.attributeIdfMap != null) {
					double tfidf = termFreqMap.get(attribute) * this.attributeIdfMap.get(attribute);
					if (tfidf != 0.0) {
						data.put(idx, tfidf);
					}
				} else {
					data.put(idx, termFreqMap.get(attribute));
				}
			}
		}
		
		return data;
	}
	
	protected LinkedList<Integer> getDocumentDataSequence(String filename, String regex) {
		LinkedList<Integer> data = new LinkedList<Integer>();
		
		String content = Tools.read(filename);
		Matcher m = Pattern.compile(regex).matcher(content);
		while (m.find()) {
			String term = m.group(0);
			term = term.toLowerCase();
			
			if (!attributeIndexMap.keySet().contains(term)) {
				continue;
			}
			
			data.add(this.attributeIndexMap.get(term));
		}
		
		return data;
	}
	
	protected String getDocumentDataTree(String filename, String regex, boolean printIndex) {
		StringBuilder sb = new StringBuilder();
		
		// read some text in the text variable
		String text = Tools.read(filename);
		//text = text.substring(0, 72000);
		text = text.replaceAll("\\s{5,}", "\n");
		System.out.println("text: " + text);
		
		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);
		
		// run all Annotators on this text
		StanfordCoreNLPWrapper.pipeline.annotate(document);
		
		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		for(CoreMap sentence: sentences) {
			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
			
			// use lemma
			if (useLemma) {
				List<CoreLabel> tokenAnnotation = sentence.get(TokensAnnotation.class);
				convertToLemmaTree(tree, tokenAnnotation.iterator());
			}
			
			//System.out.println("tree: " + tree);
			sb.append(toStringBuilder(tree, new StringBuilder(), true, printIndex, caseSensitive));
		}
		
		return sb.toString();
	}
	
	public void convertToLemmaTree(Tree tree, Iterator<CoreLabel> it) {
		if (tree.isLeaf()) {
			if (tree.label() != null) {
				CoreLabel token = null;
				while (it.hasNext()) {
					token = it.next();
					System.out.println(token.value());
					if (token.value().equals(tree.label().value())) {
						break;
					}
				}
				tree.label().setValue(token.lemma());
			}
		} else {
			Tree[] kids = tree.children();
			if (kids != null) {
				for (Tree kid : kids) {
					convertToLemmaTree(kid, it);
				}
			}
		}
	}
	
	public StringBuilder toStringBuilder(Tree tree, StringBuilder sb, boolean printOnlyLabelValue, boolean printIndex, boolean caseSensitive) {
	    if (tree.isLeaf()) {
	      if (tree.label() != null) {
	        if(printOnlyLabelValue) {
	          //sb.append(tree.label().value());
	        	String term = tree.label().value();
	        	if (!caseSensitive) {
	        		term = term.toLowerCase();
	        	}
	        	if (printIndex) {
	        		sb.append(this.attributeIndexMap.get(term) + ":1");
	        	} else {
	        		sb.append(term + ":1");
	        	}
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
	          toStringBuilder(kid, sb, printOnlyLabelValue, printIndex, caseSensitive);
	        }
	      }
	      return sb.append(')');
	    }
	  }

	
	/**
	 * Rank the terms according to their frequencies
	 * @param dir
	 */
	protected LinkedHashMap<String, Integer> rankDocumentFrequentUnigrams(String filename, String regex) {
		return Tools.sortMapByValue(getDocumentFrequentUnigrams(filename, regex));
	}
	
	/**
	 * Rank the terms according to their frequencies
	 * @param dir
	 */
	protected LinkedHashMap<String, Integer> rankDocumentFrequentUnigrams(String filename, String regex, Set<String> attributeSpace) {
		return Tools.sortMapByValue(getDocumentFrequentUnigrams(filename, attributeSpace));
	}
	
	protected static int getMapValueSum(Map<String, Integer> map) {
		int sum = 0;
		
		for (Map.Entry<String, Integer> e : map.entrySet()) {
			sum += e.getValue();
		}
		
		return sum;
	}
	
	protected static HashMap<String, Integer> getUnigramIndexMap(Map<String, Integer> map) {
		HashMap<String, Integer> unigramIndexMap = new HashMap<String, Integer>();
		
		int i = 0;
		for (String u : map.keySet()) {
			unigramIndexMap.put(u, i);
			i++;
		}
		
		return unigramIndexMap;
	}
	
	protected static String[] selectFeatures(LinkedHashMap<String, Integer> map, Double threshold, double limit) {
		ArrayList<String> featureList = new ArrayList<String>();
		String[] features = null;
		
		if (threshold == null) {
			threshold = Double.MIN_VALUE;
			//return new String[0];
		}
		
		int count = 0;
		
		for (Iterator<String> it = map.keySet().iterator(); it.hasNext(); ) {
			if (count >= limit) {
				break;
			}
			
			String key = it.next();
			int value = map.get(key);
			
			if (value < threshold) {
				break;
			}

			featureList.add(key);
			count++;
		}
		
		Collections.sort(featureList);
		features = new String[featureList.size()];
		featureList.toArray(features);
		return features;
	}
	
	protected void loadDocList() {
		this.docList = new LinkedList<String>();
		if (this.docListFilename == null) {
			String[] docs = new File(this.corpusDir).list();
			for (String doc : docs) {
				docList.add(this.corpusDir + "/" + doc);
			}
		} else {
			String[] rows = Tools.read(this.docListFilename).split("\n");
			for (String row : rows) {
				String[] cols = row.split(",");
				String filename = this.corpusDir + "/" + cols[0];
				if (!new File(filename).exists()) {
					filename += ".txt";
				}
				if (new File(filename).exists()) {
					docList.add(filename);
				}
			}
		}
	}
	
	protected String[] preSelectFeatures() {
		LinkedHashMap<String, Integer> unigramFreqMap = rankCorpusFrequentUnigrams(wordRegex);
		LinkedHashMap<String, Integer> bigramFreqMap = rankCorpusFrequentBigrams(wordRegex);
		LinkedHashMap<String, Integer> trigramFreqMap = rankCorpusFrequentTrigrams(wordRegex);
		
		System.out.println("after here");
		// output unigram frequency file
		StringBuilder unigramFreqSB = new StringBuilder();
		for (String unigram : unigramFreqMap.keySet()) {
			unigramFreqSB.append(unigram + "\t" + unigramFreqMap.get(unigram) + "\n");
		}
		Tools.write(outDir + "/" + name + "_unigram_freq.txt", unigramFreqSB.toString());
		// output bigram frequency file
		StringBuilder bigramFreqSB = new StringBuilder();
		for (String bigram : bigramFreqMap.keySet()) {
			bigramFreqSB.append(bigram + "\t" + bigramFreqMap.get(bigram) + "\n");
		}
		Tools.write(outDir + "/" + name + "_bigram_freq.txt", bigramFreqSB.toString());
		// output trigram frequency file
		StringBuilder trigramFreqSB = new StringBuilder();
		for (String trigram : trigramFreqMap.keySet()) {
			trigramFreqSB.append(trigram + "\t" + trigramFreqMap.get(trigram) + "\n");
		}
		Tools.write(outDir + "/" + name + "_trigram_freq.txt", trigramFreqSB.toString());
		
		String[] featuresUnigram = selectFeatures(unigramFreqMap, thresholdUnigram, limitUnigram);
		String[] featuresBigram = selectFeatures(bigramFreqMap, thresholdBigram, limitBigram);
		String[] featuresTrigram = selectFeatures(trigramFreqMap, thresholdTrigram, limitTrigram);
		
		/*
		TreeSet<String> featureSet = new TreeSet<String>();
		for (String feature : featuresUnigram) {
			featureSet.add(feature);
		}
		for (String feature : featuresBigram) {
			featureSet.add(feature);
		}
		for (String feature : featuresTrigram) {
			featureSet.add(feature);
		}
		String[] featuresNGram = new String[featureSet.size()];
		featureSet.toArray(featuresNGram);
		*/
		
		
		String[] featuresNGram = new String[featuresUnigram.length + featuresBigram.length + featuresTrigram.length];
		for (int i = 0; i < featuresUnigram.length; i++) {
			featuresNGram[i] = featuresUnigram[i];
		}
		for (int i = 0; i < featuresBigram.length; i++) {
			featuresNGram[featuresUnigram.length + i] = featuresBigram[i];
		}
		for (int i = 0; i < featuresTrigram.length; i++) {
			featuresNGram[featuresUnigram.length + featuresBigram.length + i] = featuresTrigram[i];
		}
		
		System.out.println("selected ngram feature size: " + featuresNGram.length);
		
		return featuresNGram;
	}
	
	protected void loadDocLabelMap() {
		this.docLabelMap = new HashMap<String, String>();
		this.labelSet = new TreeSet<String>();
		if (this.labelFilename != null) {
			String[] rows = Tools.read(this.labelFilename).split("\n");
			for (String row : rows) {
				String[] cols = row.split(",");
				if (cols.length == 1) {
					this.docLabelMap.put(cols[0], "1");
					this.labelSet.add("1");
				} else {
					this.docLabelMap.put(cols[0], cols[1]);
					this.labelSet.add(cols[1]);
				}
			}
			
			if (this.labelSet.size() < 2 && !this.labelSet.contains("0")) {
				this.labelSet.add("0");	// we assume if any instance is not in the label, its label is 0
			}
		}
	}
	
	public void load() {
//		if (!corpusDir.endsWith("/")) {
//			corpusDir += "/";
//		}
		if (this.docList == null) {
			loadDocList();
		}
		
		if (!outDir.endsWith("/")) {
			outDir += "/";
		}
		
		if (this.docLabelMap == null || this.labelSet == null) {
			loadDocLabelMap();
		}
		
		// get the unigrams for the corpus
		if (attributesFilename == null) {
			String[] featuresNGram = preSelectFeatures();
			
			setAttributeIndexMap(featuresNGram);
			setIndexAttributeMap(featuresNGram);
		} else {
			String[] attributes = Tools.read(attributesFilename).split("\n");
			setAttributeIndexMap(attributes);
			setIndexAttributeMap(attributes);
		}
		StringBuilder sbAttributes = new StringBuilder();
		for (Integer index : this.indexAttributeMap.keySet()) {
			sbAttributes.append(this.indexAttributeMap.get(index) + "\n");
		}
		//Tools.write(outDir + "/" + name + ".vocab", sbVoc.toString());
		this.attributesFilename = outDir + "/" + name + "_attributes.txt";
		Tools.write(this.attributesFilename, sbAttributes.toString());
		
		// this step should be done after we find the attributes
		if (this.useTfidf && this.attributeIdfMap == null) {
			this.setAttributeIdfMap();
		}
		
		//StringBuilder sbDat = new StringBuilder();
		String outDataFilename = outDir + "/" + name + ".data";
		String outDatFilename = outDir + "/" + name + ".dat";
		String outDocFilename = outDir + "/" + name + ".doc";
		String outArffFilename = outDir + "/" + name + ".arff";
		
		Tools.write(outDataFilename, "");
		Tools.write(outDatFilename, "");
		Tools.write(outDocFilename, "");
		
		StringBuilder sbArffHeader = new StringBuilder();
		sbArffHeader.append("@RELATION " + name + "\n\n");
		for (Integer index = 0; index < indexAttributeMap.size(); index++) {
//			if (indexAttributeMap.get(index).equals("abo")) {
//				System.out.println(sbArffHeader.toString());
//				System.exit(0);
//			}
			sbArffHeader.append("@ATTRIBUTE '" + indexAttributeMap.get(index) + "' NUMERIC\n");
		}
		
		if (labelFilename != null) {
			sbArffHeader.append("@ATTRIBUTE LABELOFINSTANCE ");
			if (this.labelSet.size() < 50) {
				sbArffHeader.append("{");
				for (String label : this.labelSet) {
					sbArffHeader.append(label + ",");
				}
				sbArffHeader.setCharAt(sbArffHeader.length() - 1, '}');
			} else {
				sbArffHeader.append("STRING");
			}
			
			sbArffHeader.append("\n\n");
		}
		sbArffHeader.append("@DATA\n");
		Tools.write(outArffFilename, sbArffHeader.toString());
		
		if (DEBUG) {
			Tools.write(outDir + "/" + name + ".dat.orig", "");
		}
		
		for (String filename : this.docList) {
			StringBuilder sbData = new StringBuilder();
			StringBuilder sbDat = new StringBuilder();
			StringBuilder sbArffDat = new StringBuilder();
			
			// for DEBUG
			StringBuilder sbDatOrig = new StringBuilder();
			
			System.out.println(filename);
			Tools.append(outDocFilename, filename + "\n");
			
			if (this.keepSequence) {
				if (this.keepSyntax) {
					boolean printIndex = true;
					sbDat.append(getDocumentDataTree(filename, wordRegex, printIndex));
					sbDat.append("\n");
					
					// for DEBUG
					if (DEBUG) {
						printIndex = false;
						sbDatOrig.append(getDocumentDataTree(filename, wordRegex, printIndex));
						sbDatOrig.append("\n");
					}
				} else {
					LinkedList<Integer> data = getDocumentDataSequence(filename, wordRegex);
					sbDat.append(data.size());
					for (Integer idx : data) {
						sbDat.append(" " + idx + ":" + 1);
					}
					sbDat.append("\n");
				}
			} else {
				//LinkedHashMap<Integer, Object> data = getDocumentData(filename, wordRegex);
				LinkedHashMap<Integer, Object> data = getDocumentData(filename);
				
				// for svmlight data format
				if (labelFilename != null) {
					String file = filename.replaceAll(".*/", "");
					if (docLabelMap.containsKey(file)) {
						sbData.append(docLabelMap.get(file));
					} else if (docLabelMap.containsKey(file.replaceAll(".txt", ""))) {
						sbData.append(docLabelMap.get(file.replaceAll(".txt", "")));
					} else {
						sbData.append("0");
					}
				}
				
				DecimalFormat df = new DecimalFormat("#.###");
				for (Integer idx : data.keySet()) {
					sbData.append(" " + (idx + 1) + ":" + df.format(data.get(idx)));
				}
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				// to compensate for the bug of SVM Light
				if (data.size() == 0) {
					sbData.append(" 1:0");
				}
				sbData.append("\n");
				
				
				// for lda data format
				DecimalFormat dfInt = new DecimalFormat("#");
				sbDat.append(data.size());
				for (Integer idx : data.keySet()) {
					sbDat.append(" " + idx + ":" + dfInt.format(data.get(idx)));
				}
				sbDat.append("\n");
				
				// for arff
				sbArffDat.append("{");
				for (Integer idx : data.keySet()) {
					sbArffDat.append(idx + " " + df.format(data.get(idx)) + ",");
				}
				if (labelFilename != null) {
					String file = filename.replaceAll(".*/", "");
					if (docLabelMap.containsKey(file)) {
						sbArffDat.append(indexAttributeMap.keySet().size() + " " + docLabelMap.get(file));
					} else if (docLabelMap.containsKey(file.replaceAll(".txt", ""))) {
						sbArffDat.append(indexAttributeMap.keySet().size() + " " + docLabelMap.get(file.replaceAll(".txt", "")));
					} else {
						sbArffDat.append(indexAttributeMap.keySet().size() + " " + "0");
					}
					sbArffDat.append(",");
				}
				if (data.keySet().size() == 0) {
					sbArffDat.append("}");
				} else {
					sbArffDat.setCharAt(sbArffDat.length() - 1, '}');
				}
				sbArffDat.append("\n");
			}
			
			LinkedHashMap<String, Integer> attributeDocumentFreqMapSorted = rankDocumentFrequentUnigrams(filename, wordRegex, attributeIndexMap.keySet());
			System.out.println(attributeDocumentFreqMapSorted);
			int length = getMapValueSum(attributeDocumentFreqMapSorted);
			System.out.println("doc length: " + length + ", unique words: " + attributeDocumentFreqMapSorted.size());
			System.out.println();
			
			Tools.append(outDataFilename, sbData.toString());
			Tools.append(outDatFilename, sbDat.toString());
			Tools.append(outArffFilename, sbArffDat.toString());
			
			// for DEBUG
			if (DEBUG) {
				Tools.append(outDir + "/" + name + ".dat.orig", sbDatOrig.toString());
			}
		}
	}
	
	public void createTest(String corpusDir, String attributeFilename, String outDir) {
		if (!corpusDir.endsWith("/")) {
			corpusDir += "/";
		}
		
		if (!outDir.endsWith("/")) {
			outDir += "/";
		}
		
		HashMap<String, String> docLabelMap = new HashMap<String, String>();
		TreeSet<String> labelSet = new TreeSet<String>();
		if (labelFilename != null) {
			String[] rows = Tools.read(labelFilename).split("\n");
			for (String row : rows) {
				String[] cols = row.split("\\W");
				if (cols.length == 1) {
					docLabelMap.put(cols[0], "1");
					labelSet.add("1");
				} else {
					docLabelMap.put(cols[0], cols[1]);
					labelSet.add(cols[1]);
				}
			}
			labelSet.add("0");	// we assume if any instance is not in the label, its label is 0
		}
		
		String[] attributes = Tools.read(attributeFilename).split("\n");
		setAttributeIndexMap(attributes);
		setIndexAttributeMap(attributes);
		
		Tools.write(outDir + name + ".dat", "");
		Tools.write(outDir + name + ".doc", "");
		Tools.write(outDir + name + "_valid.doc", "");
		
		StringBuilder sbArffHeader = new StringBuilder();
		sbArffHeader.append("@RELATION " + name + "\n\n");
		for (Integer index = 0; index < indexAttributeMap.size(); index++) {
			sbArffHeader.append("@ATTRIBUTE '" + indexAttributeMap.get(index) + "' NUMERIC\n");
		}
		
		// !!!!!!!!!!!!!!!!!!! temporarily replace for binary value
		sbArffHeader.append("@ATTRIBUTE 'POLARITY' {-1,1}\n\n");
		
		if (labelFilename != null) {
			sbArffHeader.append("@ATTRIBUTE LABELOFINSTANCE {");
			for (String label : labelSet) {
				sbArffHeader.append(label + ",");
			}
			sbArffHeader.setCharAt(sbArffHeader.length() - 1, '}');
			sbArffHeader.append("\n\n");
		}
		sbArffHeader.append("@DATA\n");
		Tools.write(outDir + name + ".arff", sbArffHeader.toString());
		
		if (DEBUG) {
			Tools.write(outDir + name + ".dat.orig", "");
		}
		
		String[] files = new File(corpusDir).list();
		System.out.println("# of files: " + files.length);
		for (String file : files) {
			if (file.startsWith(".")) {
				continue;
			}
			
			StringBuilder sbDat = new StringBuilder();
			StringBuilder sbArffDat = new StringBuilder();
			
			// for DEBUG
			StringBuilder sbDatOrig = new StringBuilder();
			
			String filename = corpusDir + file;
			System.out.println(filename);
			
			LinkedHashMap<String, Integer> attributeDocumentFreqMapSorted0 = rankDocumentFrequentUnigrams(filename, wordRegex, attributeIndexMap.keySet());
			System.out.println(attributeDocumentFreqMapSorted0);
			int length0 = getMapValueSum(attributeDocumentFreqMapSorted0);
			if (length0 == 0) {
				continue;
			}
			
			
			
			Tools.append(outDir + name + ".doc", filename + "\n");
			/*
			if (this.keepSequence) {
				LinkedHashMap<Integer, Object> data = getDocumentData(filename, wordRegex);
				sbDat.append(data.size());
				for (Integer idx : data.keySet()) {
					sbDat.append(" " + idx + ":" + data.get(idx));
				}
				sbDat.append("\n");
			} else {
				System.out.println("data keep sequence");
				LinkedList<Integer> data = getDocumentDataSequence(filename, wordRegex);
				sbDat.append(data.size());
				for (Integer idx : data) {
					sbDat.append(" " + idx + ":" + 1);
				}
				sbDat.append("\n");
			}
			*/
			if (this.keepSequence) {
				if (this.keepSyntax) {
					boolean printIndex = true;
					sbDat.append(getDocumentDataTree(filename, wordRegex, printIndex));
					sbDat.append("\n");
					
					// for DEBUG
					if (DEBUG) {
						printIndex = false;
						sbDatOrig.append(getDocumentDataTree(filename, wordRegex, printIndex));
						sbDatOrig.append("\n");
					}
				} else {
					LinkedList<Integer> data = getDocumentDataSequence(filename, wordRegex);
					sbDat.append(data.size());
					for (Integer idx : data) {
						sbDat.append(" " + idx + ":" + 1);
					}
					sbDat.append("\n");
				}
			} else {
				LinkedHashMap<Integer, Object> data = getDocumentData(filename);
				sbDat.append(data.size());
				for (Integer idx : data.keySet()) {
					sbDat.append(" " + idx + ":" + data.get(idx));
				}
				sbDat.append("\n");
				
				// for arff
				sbArffDat.append("{");
				for (Integer idx : data.keySet()) {
					//sbArffDat.append(idx + " " + data.get(idx) + ",");
					//!!!!!!!!!!!!! temporarily replace for binary value
					sbArffDat.append(idx + " 1,");
				}
				
				// !!!!!!!!!!!!!!!!!!! temporarily replace for binary value
				if (sbArffDat.charAt(sbArffDat.length() - 1) != '{') {
					sbArffDat.append(indexAttributeMap.keySet().size() + " -1,");
					Tools.append(outDir + name + "_valid.doc", filename + "\n");
				} else {	// no feature exists
					sbArffDat.append(indexAttributeMap.keySet().size() + " -1");
				}
				
				
				
				if (labelFilename != null) {
					if (docLabelMap.containsKey(file)) {
						sbArffDat.append(indexAttributeMap.keySet().size() + " " + docLabelMap.get(file));
					} else if (docLabelMap.containsKey(file.replaceAll(".txt", ""))) {
						sbArffDat.append(indexAttributeMap.keySet().size() + " " + docLabelMap.get(file.replaceAll(".txt", "")));
					} else {
						sbArffDat.append(indexAttributeMap.keySet().size() + " " + "0");
					}
					sbArffDat.append(",");
				}
				if (data.keySet().size() == 0) {
					sbArffDat.append("}");
				} else {
					sbArffDat.setCharAt(sbArffDat.length() - 1, '}');
				}
				sbArffDat.append("\n");
			}
			/*
			LinkedHashMap<String, Integer> attributeDocumentFreqMapSorted = rankDocumentFrequentUnigrams(filename, wordRegex, attributeIndexMap.keySet());
			System.out.println(attributeDocumentFreqMapSorted);
			int length = getMapValueSum(attributeDocumentFreqMapSorted);
			System.out.println("doc length: " + length + ", unique words: " + attributeDocumentFreqMapSorted.size());
			System.out.println();
			*/
			LinkedHashMap<String, Integer> attributeDocumentFreqMapSorted = rankDocumentFrequentUnigrams(filename, wordRegex, attributeIndexMap.keySet());
			System.out.println(attributeDocumentFreqMapSorted);
			int length = getMapValueSum(attributeDocumentFreqMapSorted);
			System.out.println("doc length: " + length + ", unique words: " + attributeDocumentFreqMapSorted.size());
			System.out.println();
			
			Tools.append(outDir + name + ".dat", sbDat.toString());
			Tools.append(outDir + name + ".arff", sbArffDat.toString());
			
			// for DEBUG
			if (DEBUG) {
				Tools.append(outDir + name + ".dat.orig", sbDatOrig.toString());
			}
		}
		/*
		Tools.write(outDir + name + ".dat", sbDat.toString());
		
		StringBuilder sbDoc = new StringBuilder();
		for (int i = 0; i < files.length; i++) {
			String filename = corpusDir + files[i];
			sbDoc.append(filename + "\n");
		}
		Tools.write(outDir + name + ".doc", sbDoc.toString());
		System.out.println("done test: " + outDir + name + ".doc");
		*/
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		/*
		String corpusDir = "/home/bx2109/CCLS/pyramids/DUC06/corpus-summaries/";
		String outDir = "/home/bx2109/CCLS/pyramids/DUC06/tm/";
		String name = "pyramid-summaries";
		String stopwordsFilename = "/home/bx2109/CCLS/pyramids/DUC06/tm/stopwords.txt";
		String scopewordsFilename = "/home/bx2109/CCLS/pyramids/DUC06/tm/scopewords.txt";
		double thresholdUnigram = 1.0;
		double limitUnigram = Integer.MAX_VALUE;
		
		String vocabFilename = "/home/bx2109/CCLS/pyramids/DUC06/tm/pyramid.vocab";
		*/
		
		if (args[0].equals("test")) {
			if (args.length != 5 && args.length != 6) {
				System.out.println("Usage: Java CorpusAgent test name corpusDir attributeFilename outDir");
				System.exit(0);
			}
			String name = args[1];
			String corpusDir = args[2];
			String attributeFilename = args[3];
			String outDir = args[4];
			String wordRegex = null;
			if (args.length == 6) {
				wordRegex = args[5];
			}
			
			CorpusAgent ca = new CorpusAgent();
			ca.setName(name);
			if (args.length == 6) {
				ca.setWordRegex(wordRegex);
			}
			
			Tools.createDir(outDir);

			ca.createTest(corpusDir, attributeFilename, outDir);
			
			System.out.println("done.");
			System.exit(0);
		}
		
		/* Not using this right now
		if (args.length != 7) {
			System.out.println("Usage: Java CorpusAgent name corpusDir stopwordsFile scopewordsFile thresholdUnigram limitUnigram outDir");
			System.exit(0);
		}
		*/
		
		String name = null;
		String wordRegex = null;
		String corpusDir = null;
		String stopwordsFilename = null;
		String scopewordsFilename = null;
		Double thresholdUnigram = Double.MIN_VALUE;
		Double limitUnigram = Double.MAX_VALUE;
		Double thresholdBigram = Double.MIN_VALUE;
		Double limitBigram = Double.MAX_VALUE;
		Double thresholdTrigram = Double.MIN_VALUE;
		Double limitTrigram = Double.MAX_VALUE;
		String outDir = null;
		boolean keepSequence = false;//true;
		boolean keepSyntax = false;//true;
		boolean caseSensitive = false;
		boolean useLemma = true;
		boolean useSCN = false;
		String format = "lda-c";
		String labelFilename = null;
		String attributesFilename = null;
		String docListFilename = null;
		String attributeIdfFilename = null;
		boolean useTfidf = false;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--name")) {
				name = args[++i];
			} else if (args[i].equals("--corpus_dir")) {
				corpusDir = args[++i];
			} else if (args[i].equals("--stopwords")) {
				stopwordsFilename = args[++i];
			} else if (args[i].equals("--scopewords")) {
				scopewordsFilename = args[++i];
			} else if (args[i].equals("--threshold_unigram")) {
				thresholdUnigram = Double.parseDouble(args[++i]);
			} else if (args[i].equals("--limit_unigram")) {
				limitUnigram = Double.parseDouble(args[++i]);
			} else if (args[i].equals("--threshold_bigram")) {
				thresholdBigram = Double.parseDouble(args[++i]);
			} else if (args[i].equals("--limit_bigram")) {
				limitBigram = Double.parseDouble(args[++i]);
			} else if (args[i].equals("--threshold_trigram")) {
				thresholdTrigram = Double.parseDouble(args[++i]);
			} else if (args[i].equals("--limit_trigram")) {
				limitTrigram = Double.parseDouble(args[++i]);
			} else if (args[i].equals("--out_dir")) {
				outDir = args[++i];
			} else if (args[i].equals("--format")) {
				format = args[++i];
			} else if (args[i].equals("--keep_sequence")) {
				keepSequence = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--keep_syntax")) {
				keepSyntax = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--case_sensitive")) {
				caseSensitive = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--use_lemma")) {
				useLemma = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--use_scn")) {
				useSCN = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--word_regex")) {
				wordRegex = args[++i];
			} else if (args[i].equals("--label")) {
				labelFilename = args[++i];
			} else if (args[i].equals("--attributes")) {
				attributesFilename = args[++i];
			} else if (args[i].equals("--docs")) {
				docListFilename = args[++i];
			} else if (args[i].equals("--idf")) {
				attributeIdfFilename = args[++i];
			} else if (args[i].equals("--use_tfidf")) {
				useTfidf = args[++i].startsWith("y") ? true : false;
			}
		}
		
		
		Tools.createDir(outDir);
		
		CorpusAgent ca = new CorpusAgent();
		ca.setName(name);
		ca.setCorpusDir(corpusDir);
		ca.setOutDir(outDir);
		ca.setWordRegex(wordRegex);
		if (stopwordsFilename != null) {
			ca.setStopwords(stopwordsFilename);
		}
		//ca.setScopewords(scopewordsFilename);
		ca.setThresholdUnigram(thresholdUnigram);
		ca.setLimitUnigram(limitUnigram);
		ca.setThresholdBigram(thresholdBigram);
		ca.setLimitBigram(limitBigram);
		ca.setThresholdTrigram(thresholdTrigram);
		ca.setLimitTrigram(limitTrigram);
		ca.setFormat(format);
		ca.setKeepSequence(keepSequence);
		ca.setKeepSyntax(keepSyntax);
		ca.setCaseSensitive(caseSensitive);
		ca.setUseLemma(useLemma);
		ca.setUseSCN(useSCN);
		ca.setLabelFilename(labelFilename);
		ca.setAttributesFilename(attributesFilename);
		ca.setDocListFilename(docListFilename);
		ca.setAttributeIdfFilename(attributeIdfFilename);
		ca.setUseTfidf(useTfidf);
		
		ca.load();
		
		//ca.createTest(corpusDir, vocabFilename, outDir);
		
		System.out.println("done.");

	}

}

