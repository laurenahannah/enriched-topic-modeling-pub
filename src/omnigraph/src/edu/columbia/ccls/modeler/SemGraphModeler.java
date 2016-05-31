package edu.columbia.ccls.modeler;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.columbia.ccls.exception.StockPriceNotFoundException;
import edu.columbia.ccls.finance.Sector;
import edu.columbia.ccls.finance.StockPrice;
import edu.columbia.ccls.semantics.FrameVectorSpaceModel;
import edu.columbia.ccls.semantics.TreeNode;
import edu.columbia.ccls.util.Graph;
import edu.columbia.ccls.util.Tools;

public class SemGraphModeler {
	
	public static final String WORD_REGEX = "\\b[A-Za-z][A-Za-z0-9\\-_&]+\\b";
	
	public static final double DAL_PLEASANTNESS_MEAN = 1.839958774;
	public static final double DAL_ACTIVATION_MEAN = 1.847899428;
	public static final double DAL_IMAGERY_MEAN = 1.934568749;
	public static final double DAL_PLEASANTNESS_SD = 0.440015477;
	public static final double DAL_ACTIVATION_SD = 0.3943928;
	public static final double DAL_IMAGERY_SD = 0.634407312;
	public static final double DAL_MEAN = 1.874142317;
	public static final double DAL_SD = 0.502355579;
	public static final double DAL_SCALE = 0.5;
	
	public enum MergeRule {
		SINGLE_FRAGMENT,
		FORREST_OF_FRAME_FRAGMENTS,
		FORREST_OF_SENTENCE_FRAGMENTS
	}
	
	public enum FeatureSpace {
		FRAME_DE_ONLY,
		FRAME,
		FRAME_BOW,
		FRAME_DE_DEP,
		FRAME_BOW_DE_DEP,
		FRAME_DEP,
		FRAME_BOW_DEP,
		FRAME_TOPIC,
		FRAME_DEP_TOPIC,
		FRAME_DEP_TOPIC_DAL,
		FRAME_DE_DEP_TOPIC_DAL,
		FRAME_DE_DEP_TOPIC,
		FRAME_BOW_TOPIC,
		FRAME_BOW_TOPIC_DAL,
		FRAME_BOW_DEP_TOPIC,
		FRAME_BOW_DEP_TOPIC_DAL,
		FRAME_BOW_DE_DEP_TOPIC_DAL,
		FRAME_BOW_DE_DEP_TOPIC,
		FRAME_DAL,
		FRAME_DEP_DAL,
		FRAME_DE_DEP_DAL,
		FRAME_BOW_DAL,
		FRAME_BOW_DEP_DAL,
		FRAME_BOW_DE_DEP_DAL,
		DEP
	}
	
	public enum Task {
		PRICE_CHANGE,
		PRICE_POLARITY,
		PRICE_FOLLOW_MARKET,
		PRICE_CHANGE_RELATIVE_TO_MARKET,
		PRICE_POLARITY_RELATIVE_TO_MARKET
	}
	
	private MergeRule mergeRule = MergeRule.FORREST_OF_SENTENCE_FRAGMENTS;
	private FeatureSpace featureSpace = FeatureSpace.FRAME_DEP; //FeatureSpace.FRAME_DE_ONLY;
	private boolean isDirected = true;
	
	private String ticker;
	private String gics;
	private String datasetDir;
	private String startDateStr;
	private String endDateStr;
	private String name;
	private String outDir;
	private int minFreq;
	private int limit;
	private Task task;
	
	private Calendar calendarFixed = Calendar.getInstance();
	private HashSet<String> stopwordSet = new HashSet<String>();
	private String tmDocsFilename;
	private String tmWordsFilename;
	private String tmTopicAssignmentFilename;
	private HashMap<String, HashMap<String, Integer>> docWordTopicMapMap = new HashMap<String, HashMap<String,Integer>>();
	private String dalFilename;
	private TreeMap<String, String[]> wordDalMap = null;
	
	public SemGraphModeler() {
		
	}
	
	public void setMergeRule(String mergeRuleStr) {
		if (mergeRuleStr.equals("SINGLE")) {
			this.mergeRule = MergeRule.SINGLE_FRAGMENT;
		} else if (mergeRuleStr.equals("FFRAME")) {
			this.mergeRule = MergeRule.FORREST_OF_FRAME_FRAGMENTS;
		} else if (mergeRuleStr.equals("FSENT")) {
			this.mergeRule = MergeRule.FORREST_OF_SENTENCE_FRAGMENTS;
		} else {
			System.out.println("Incorrect merge rule setting.");
			System.exit(0);
		}
	}
	
	public void setFeatureSpace(String featureSpaceStr) {
		if (featureSpaceStr.equals("FRAME_DE_ONLY")) {
			this.featureSpace = FeatureSpace.FRAME_DE_ONLY;
		} else if (featureSpaceStr.equals("FRAME")) {
			this.featureSpace = FeatureSpace.FRAME;
		} else if (featureSpaceStr.equals("FRAME_BOW")) {
			this.featureSpace = FeatureSpace.FRAME_BOW;
		} else if (featureSpaceStr.equals("FRAME_DEP")) {
			this.featureSpace = FeatureSpace.FRAME_DEP;
		} else if (featureSpaceStr.equals("FRAME_DE_DEP")) {
			this.featureSpace = FeatureSpace.FRAME_DE_DEP;
		} else if (featureSpaceStr.equals("FRAME_BOW_DE_DEP")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_DE_DEP;
		} else if (featureSpaceStr.equals("FRAME_BOW_DEP")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_DEP;
		} else if (featureSpaceStr.equals("FRAME_TOPIC")) {
			this.featureSpace = FeatureSpace.FRAME_TOPIC;
		} else if (featureSpaceStr.equals("FRAME_DEP_TOPIC")) {
			this.featureSpace = FeatureSpace.FRAME_DEP_TOPIC;
		} else if (featureSpaceStr.equals("FRAME_DEP_TOPIC_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_DEP_TOPIC_DAL;
		} else if (featureSpaceStr.equals("FRAME_DE_DEP_TOPIC_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_DE_DEP_TOPIC_DAL;
		} else if (featureSpaceStr.equals("FRAME_DE_DEP_TOPIC")) {
			this.featureSpace = FeatureSpace.FRAME_DE_DEP_TOPIC;
		} else if (featureSpaceStr.equals("FRAME_BOW_TOPIC")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_TOPIC;
		} else if (featureSpaceStr.equals("FRAME_BOW_TOPIC_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_TOPIC_DAL;
		} else if (featureSpaceStr.equals("FRAME_BOW_DEP_TOPIC")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_DEP_TOPIC;
		} else if (featureSpaceStr.equals("FRAME_BOW_DEP_TOPIC_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL;
		} else if (featureSpaceStr.equals("FRAME_BOW_DE_DEP_TOPIC_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL;
		} else if (featureSpaceStr.equals("FRAME_BOW_DE_DEP_TOPIC")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_DE_DEP_TOPIC;
		} else if (featureSpaceStr.equals("FRAME_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_DAL;
		} else if (featureSpaceStr.equals("FRAME_DEP_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_DEP_DAL;
		} else if (featureSpaceStr.equals("FRAME_DE_DEP_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_DE_DEP_DAL;
		} else if (featureSpaceStr.equals("FRAME_BOW_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_DAL;
		} else if (featureSpaceStr.equals("FRAME_BOW_DEP_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_DEP_DAL;
		} else if (featureSpaceStr.equals("FRAME_BOW_DE_DEP_DAL")) {
			this.featureSpace = FeatureSpace.FRAME_BOW_DE_DEP_DAL;
		} else if (featureSpaceStr.equals("DEP")) {
			this.featureSpace = FeatureSpace.DEP;
		} else {
			System.out.println("Incorrect feature space setting.");
			System.exit(0);
		}
	}
	
	public void setIsDirected(boolean isDirected) {
		this.isDirected = isDirected;
	}
	
	public static String[] findFilenames(String ticker, String gics, String datasetDir) {
		//hooshmand
		//String dir = datasetDir + "/fulltext_sen_rlv_sendoc_semraw";
		String dir = datasetDir + "/sem";
		String[] filenames = new File(dir).list();
		for (int i = 0; i < filenames.length; i++) {
			filenames[i] = dir + "/" + filenames[i];
		}
		return filenames;
	}
	
	public static HashSet<String> findTargetObjectSet(String content) {
		HashSet<String> targetObjectSet = new HashSet<String>();
		Matcher m = Pattern.compile("[A-Za-z]+_[Ss][Pp]500_[Gg][Ii][Cc][Ss]\\d+_[Cc][Oo][Mm][Pp][Aa][Nn][Yy]").matcher(content);
		while (m.find()) {
			targetObjectSet.add(m.group());
		}
		return targetObjectSet;
	}
	
	private HashMap<String, HashMap<String, String>> getDepInfoMap(String depFilename) {
		HashMap<String, HashMap<String, String>> depInfoMap = new HashMap<String, HashMap<String,String>>();
		
		String[] lines = Tools.read(depFilename).split("\n");
		for (String line : lines) {
			String[] cols = line.split("\t");
			String index = cols[0];
			String token = cols[1];
			String headIndex = cols[6];
			String type = cols[7];
			HashMap<String, String> infoMap = new HashMap<String, String>();
			infoMap.put("index", index);
			infoMap.put("token", token);
			infoMap.put("head_index", headIndex);
			infoMap.put("dep_type", type);
			depInfoMap.put(index, infoMap);
		}
		return depInfoMap;
	}
	
	public TreeSet<Graph> getDepFragments(String depFilename) {
		TreeSet<Graph> fragmentSet = new TreeSet<Graph>();
		
		HashMap<String, HashMap<String, String>> depInfoMap = getDepInfoMap(depFilename);
		
		HashSet<String> targetObjectSet = findTargetObjectSet(Tools.read(depFilename));
		HashSet<String> uniqueAttributeSet = new HashSet<String>();
		
		ArrayList<Graph> fragmentsToMerge = new ArrayList<Graph>();
		for (String index : depInfoMap.keySet()) {
			HashMap<String, String> infoMap = depInfoMap.get(index);
			String headIndex = infoMap.get("head_index");
			String depType = infoMap.get("dep_type");
			uniqueAttributeSet.add(index);
			uniqueAttributeSet.add(headIndex);
			//uniqueAttributeSet.add(depType);	// added on 08Jan2015, not sure
			
			TreeSet<String> attributeSet = new TreeSet<String>();
			attributeSet.add(index);
			attributeSet.add(headIndex);
			attributeSet.add(depType);
			ArrayList<String> attributes = new ArrayList<String>();
			for (String a : attributeSet) {
				attributes.add(a);
			}
			// fragment to represent the graph structure of this frame
			Graph fragment = new Graph(attributes);
			HashMap<String, Integer> attributeIndexMap = new HashMap<String, Integer>();
			for (int i = 0; i < attributes.size(); i++) {
				attributeIndexMap.put(attributes.get(i), i);
			}
			
			// add connectivity
			ArrayList<TreeSet<Integer>> connectivity = new ArrayList<TreeSet<Integer>>();
			for (int i = 0; i < attributes.size(); i++) {
				connectivity.add(new TreeSet<Integer>());
			}
			
			int dependentAttributeIndex = attributeIndexMap.get(index);
			int headAttributeIndex = attributeIndexMap.get(headIndex);
			int depTypeAttributeIndex = attributeIndexMap.get(depType);
			if (this.isDirected) {
				connectivity.get(dependentAttributeIndex).add(depTypeAttributeIndex);
				connectivity.get(depTypeAttributeIndex).add(headAttributeIndex);
			} else {
				connectivity.get(dependentAttributeIndex).add(depTypeAttributeIndex);
				connectivity.get(depTypeAttributeIndex).add(dependentAttributeIndex);
				connectivity.get(depTypeAttributeIndex).add(headAttributeIndex);
				connectivity.get(headAttributeIndex).add(depTypeAttributeIndex);
			}
			for (int i = 0; i < connectivity.size(); i++) {
				for (int j : connectivity.get(i)) {
					fragment.getGraph().get(i).add(j);
				}
			}
			// end adding connectivity
			
			fragmentsToMerge.add(fragment);
		}
		
		//uniqueAttributeSet.addAll(targetObjectSet); // commented out on 09Jan15, don't need to merge by designated companies
		boolean keepIndexSuffix = false;
		Graph sentenceFragment = Graph.merge(fragmentsToMerge, uniqueAttributeSet, keepIndexSuffix);
		ArrayList<String> attributes = sentenceFragment.getAttributes();
		for (int i = 0; i < attributes.size(); i++) {
			if (attributes.get(i).matches("\\d+")) {
				String newAttribute = attributes.get(i).equals("0") ? "ROOT" : depInfoMap.get(attributes.get(i)).get("token");
				if (newAttribute.matches("[a-z]+_sp500_gics\\d+_company")) {
					newAttribute = newAttribute.toUpperCase();
				} else if (newAttribute.equals("ROOT")) {
					newAttribute = "SENTENCE_ENTRNACE";
				} else {
					newAttribute = "LEXICAL_ITEM-" + newAttribute;
				}
				
				attributes.set(i, newAttribute);
			} else {
				String newAttribute = "DEP_TYPE-" + attributes.get(i);
				attributes.set(i, newAttribute);
			}
		}
		fragmentsToMerge.clear();
		fragmentsToMerge.add(sentenceFragment);
		sentenceFragment = Graph.merge(fragmentsToMerge, new HashSet<String>(), keepIndexSuffix);
		
		// remove punctuation and stop words
		HashSet<String> nodesToRemove = new HashSet<String>();
		for (String attribute : sentenceFragment.getAttributes()) {
			String token = attribute.replaceAll("LEXICAL_ITEM-", "");
			if (!token.matches(WORD_REGEX)) {
				nodesToRemove.add(attribute);
			}
			if (stopwordSet.contains(token)) {
				nodesToRemove.add(attribute);
			}
		}
		sentenceFragment.removeNodes(nodesToRemove);
		// end removing nodes
		
		fragmentSet.add(sentenceFragment);
		
		return fragmentSet;
	}
	
	public TreeSet<Graph> getFragments(String content, HashMap<String, String> dependentHeadMap, HashMap<String, Integer> wordTopicMap) {
		TreeSet<Graph> fragmentSet = new TreeSet<Graph>();
		
		HashSet<String> targetObjectSet = findTargetObjectSet(content);
		TreeSet<String> frameSet = new TreeSet<String>();
		TreeSet<String> frameElementSet = new TreeSet<String>();
		
		String[] lines = content.split("\n");
		for (int iLine = 0; iLine < lines.length; iLine++) {
			if (lines[iLine].matches("\\s*<sentence ID=\"\\d+\">")) {
				//TreeNode rootTreeNode = new TreeNode("TARGET_OBJECT", new ArrayList<TreeNode>());
				
				StringBuilder sbSentence = new StringBuilder();
				sbSentence.append(lines[iLine]);
				sbSentence.append("\n");
				while (!lines[++iLine].matches("\\s*</sentence>")) {
					sbSentence.append(lines[iLine]);
					sbSentence.append("\n");
				}
				sbSentence.append(lines[iLine]);
				
				// for dep relations
				HashMap<String, LinkedList<String>> positionHeadListMap = new HashMap<String, LinkedList<String>>();
				HashMap<String, String> positionFrameNameMap = new HashMap<String, String>();
				// for removing frames lower than the frames that contain designated objects
				HashSet<String> positionsContainDE = new HashSet<String>();
				HashMap<Graph, String> fragmentPositionMap = new HashMap<Graph, String>();
				
				// For a parse for a sentence
				ArrayList<Graph> fragmentsToMerge = new ArrayList<Graph>();
				Matcher mEntry = Pattern.compile("( *<sentence ID=\"\\d+\">)(.*?)( *</sentence>)", Pattern.MULTILINE | Pattern.DOTALL).matcher(sbSentence.toString());
				if (mEntry.find()) {
					String entryStartLine = mEntry.group(1);
					String entryEndLine = mEntry.group(3);
					String entry = mEntry.group(2);
					String textLine = "";
					String text = "";
					Matcher mText = Pattern.compile(" *<text>(.*)</text>").matcher(entry);
					if (mText.find()) {
						textLine = mText.group();
						text = mText.group(1);
						text = text.replaceAll("&amp;", "&");
					}
					Matcher mAnnotationSets = Pattern.compile(" *(<annotationSets>.*</annotationSets>|<annotationSets/>)", Pattern.MULTILINE | Pattern.DOTALL).matcher(entry);
					if (mAnnotationSets.find()) {
						String annotationSets = mAnnotationSets.group();
						
						Matcher m = Pattern.compile(FrameVectorSpaceModel.SRL_RAW_ANNOTATION_PATTERN, Pattern.MULTILINE | Pattern.DOTALL).matcher(annotationSets);
						// Iterate each frame
						while (m.find()) {
							//System.out.println(m.group());
							// extract the frame name
							String frame = FrameVectorSpaceModel.FRAME_FEATURE_PREFIX + m.group(1);
							
							// extract the target word (i.e. lexical unit)
							int targetStart = Integer.parseInt(m.group(3));
							int targetEnd = Integer.parseInt(m.group(2));
							// adjust the offset of semafor
							while (text.charAt(targetStart) == ' '
//									|| (targetEnd + 1 != text.length() && text.charAt(targetEnd + 1) == ' ')
							) {
								targetStart++;
								targetEnd++;
							}
							String targetText = text.substring(targetStart, targetEnd + 1).toLowerCase();
							if (targetText.matches("[a-z]+_sp500_gics\\d+_company")) {
								targetText = targetText.toUpperCase();
							}
							String frameTarget = frame + "-" + targetText;
							String frameTargetPosition = targetStart == 0 ? "1" : String.valueOf(text.substring(0, targetStart).split(" ").length + 1);
							
							// extract the frame elements and the text span for each frame element
							ArrayList<String> frameElements = new ArrayList<String>();
							ArrayList<String> frameElementChunks = new ArrayList<String>();
							Matcher mFE = Pattern.compile(FrameVectorSpaceModel.SRL_RAW_ANNOTATION_FE_PATTERN, Pattern.MULTILINE | Pattern.DOTALL).matcher(m.group(4));
							while (mFE.find()) {
								int feStart = Integer.parseInt(mFE.group(3));
								int feEnd = Integer.parseInt(mFE.group(1));
								String fe = mFE.group(2);
								while (text.charAt(feStart) == ' '
//										|| (feEnd + 1 != text.length() && text.charAt(feEnd + 1) == ' ')
								) {
									feStart++;
									feEnd++;
								}
								String chunk = text.substring(feStart, feEnd + 1).toLowerCase();
								frameElements.add(frame + "-" + fe);
								frameElementChunks.add(chunk);
							}
							
							// define a node for the frame name
							TreeNode frameTN = null;
							String frameRaw = "FRAME_NAME-" + frame.replaceAll(FrameVectorSpaceModel.FRAME_FEATURE_PREFIX, "");
							frameTN = new TreeNode(frameRaw, new ArrayList<TreeNode>());
							
							// define a node for "Target" (a symbolic node) 
							// and a node for the target word (i.e. lexical unit)
							// children is null for leaf node
							String frameTargetRaw = "FRAME_TARGET-" + frameTarget.replaceAll(frame + "\\-", "");
							TreeNode targetLeafTN = new TreeNode(frameTargetRaw, null);
							TreeNode targetTN = new TreeNode("Target", new ArrayList<TreeNode>());
							targetTN.getChildren().add(targetLeafTN);
							
							// attach the symbolic "Target" node to the frame name node
							frameTN.getChildren().add(targetTN);
							
							// create nodes for frame elements, and attach them to the frame name node
							for (int i = 0; i < frameElements.size(); i++) {
								//TreeNode feLeafTN = new TreeNode(frameElementChunks.get(i), null);
								// children is empty but not null, for non-leaf node
								String feRaw = "FRAME_ELEMENT-" + frameElements.get(i).replaceAll(frame + "\\-", "");
								TreeNode feTN = new TreeNode(feRaw, new ArrayList<TreeNode>());
								
								frameTN.getChildren().add(feTN);
							}
							
							// for dependency relations, in order to find frame name by token position
							positionFrameNameMap.put(frameTargetPosition, frameRaw);
							
							Graph fragment = null;
							if (featureSpace.equals(FeatureSpace.FRAME_DE_ONLY)) {
								// find roots and create a root node for each of the designated entity
								// This is how it's done:
								// For each designated entity, iterate all the frame elements,
								// if a frame element mentions the designated entity,
								// create a node for the frame element, 
								// attach the frame name node to the newly created frame element node,
								// attach the newly created frame element node to the root node
								TreeMap<String, TreeNode> rootTreeNodeMap = new TreeMap<String, TreeNode>();
								for (String targetObject : targetObjectSet) {
									for (int i = 0; i < frameElements.size(); i++) {
										String chunk = frameElementChunks.get(i);
										
										//System.out.println(targetObject);
										if (chunk.contains(targetObject.toLowerCase())) {
											String feRaw = "FRAME_ELEMENT-" + frameElements.get(i).replaceAll(frame + "\\-", "");
											TreeNode feTN = new TreeNode(feRaw, new ArrayList<TreeNode>());
											if (frameTN != null) {
												feTN.getChildren().add(frameTN);
											}
											
											if (!rootTreeNodeMap.containsKey(targetObject)) {
												rootTreeNodeMap.put(targetObject, new TreeNode(targetObject, new ArrayList<TreeNode>()));
											}
											rootTreeNodeMap.get(targetObject).getChildren().add(feTN);
										}
									}
								}
								
								////////
								
								if (rootTreeNodeMap.isEmpty()) {
									continue;
								}
								
								//System.out.println(text);
								
								// to create a graph
								// for the nodes of the graph
								// define a set of attributes for the graph
								TreeSet<String> attributeSet = new TreeSet<String>();
								
								// create a node for frame name + target
								String target = null;
								for (TreeNode tn : frameTN.getChildren()) {
									if (tn.getValue().equals("Target")) {
										target = tn.getChildren().get(0).getValue();
										break;
									}
								}
								String frameAndTarget = frameTN.getValue() + "-" + target;
								attributeSet.add(frameAndTarget);
								frameSet.add(frameAndTarget);
								
								// create nodes for the designated entity and frame elements
								for (String targetObject : rootTreeNodeMap.keySet()) {
									TreeNode rootTreeNode = rootTreeNodeMap.get(targetObject);
									//System.out.println(rootTreeNode);
									attributeSet.add(targetObject);
									for (TreeNode feTN : rootTreeNode.getChildren()) {
										String fe = feTN.getValue();
										attributeSet.add(fe);
										frameElementSet.add(fe);
									}
								}
								
								// create attribute list for fragment
								ArrayList<String> attributes = new ArrayList<String>();
								for (String a : attributeSet) {
									attributes.add(a);
								}
								
								// fragment to represent the graph structure of this frame
								fragment = new Graph(attributes);
								
								HashMap<String, Integer> attributeIndexMap = new HashMap<String, Integer>();
								for (int i = 0; i < attributes.size(); i++) {
									attributeIndexMap.put(attributes.get(i), i);
								}
								
								// for the edges of the graph
								// it still starts from the nodes of the designated objects
								ArrayList<TreeSet<Integer>> connectivity = new ArrayList<TreeSet<Integer>>();
								for (int i = 0; i < attributes.size(); i++) {
									connectivity.add(new TreeSet<Integer>());
								}
								
								for (String targetObject : rootTreeNodeMap.keySet()) {
									int targetObjectIndex = attributeIndexMap.get(targetObject);
									
									TreeNode rootTreeNode = rootTreeNodeMap.get(targetObject);
									for (TreeNode feTN : rootTreeNode.getChildren()) {
										int feIndex = attributeIndexMap.get(feTN.getValue());
										int frameIndex = attributeIndexMap.get(frameAndTarget);
										
										if (this.isDirected) {
											// from designated entity to frame element
											connectivity.get(targetObjectIndex).add(feIndex);
											// from frame element to frame name
											connectivity.get(feIndex).add(frameIndex);
										} else {
											// undirected graph (bi-direction)
											connectivity.get(targetObjectIndex).add(feIndex);
											connectivity.get(feIndex).add(frameIndex);
											
											connectivity.get(feIndex).add(targetObjectIndex);
											connectivity.get(frameIndex).add(feIndex);
										}
									}
								}
								for (int i = 0; i < connectivity.size(); i++) {
									for (int j : connectivity.get(i)) {
										fragment.getGraph().get(i).add(j);
									}
								}
								
							} else {	// for feature space that's not only for designated entities
								// to create a graph
								// for the nodes of the graph
								// define a set of attributes for the graph
								TreeSet<String> attributeSet = new TreeSet<String>();
								
								// frame name node
								attributeSet.add(frameRaw);
								
								// target node (lexical unit)
								attributeSet.add(frameTargetRaw);
								
								// frame element nodes
								for (int i = 0; i < frameElements.size(); i++) {
									String feRaw = "FRAME_ELEMENT-" + frameElements.get(i).replaceAll(frame + "\\-", "");
									attributeSet.add(feRaw);
									
									String chunks = frameElementChunks.get(i);
									String[] tokens = chunks.split(" ");
									for (String token : tokens) {
										if (token.matches("[a-z]+_sp500_gics\\d+_company")) {
											token = token.toUpperCase();
										}
										
										// for bow nodes
										if ((featureSpace.equals(FeatureSpace.FRAME_BOW)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC_DAL) 
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_DAL)
											)
											&& token.matches(WORD_REGEX)
											&& !stopwordSet.contains(token)) {
											//attributeSet.add(token);
											String lexicalItem = "LEXICAL_ITEM-" + token;
											attributeSet.add(lexicalItem);
										}
										
										// for topic nodes
										if ((featureSpace.equals(FeatureSpace.FRAME_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC)
											)
											&& token.matches(WORD_REGEX)
											&& !stopwordSet.contains(token)
										) {
											if (wordTopicMap.containsKey(token)) {
												String topic = "TOPIC-" + wordTopicMap.get(token);
												attributeSet.add(topic);
											}
										}
										
										// for dal nodes
										if ((featureSpace.equals(FeatureSpace.FRAME_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
											)
											&& token.matches(WORD_REGEX)
											&& !stopwordSet.contains(token)
										) {
											if (wordDalMap.containsKey(token)) {
												for (String d : wordDalMap.get(token)) {
													String dal = "DAL-" + d;
													attributeSet.add(dal);
												}
											}
										}
										
										for (String targetObject : targetObjectSet) {
											if (targetObject.equals(token)) {
												attributeSet.add(targetObject); 
											}
										}
									}
								}
								
								if (attributeSet.contains("yhoo_sp500_gics45_company")) {
									System.out.println("here");
								}
								
								ArrayList<String> attributes = new ArrayList<String>();
								for (String a : attributeSet) {
									attributes.add(a);
								}
								// fragment to represent the graph structure of this frame
								fragment = new Graph(attributes);
								
								HashMap<String, Integer> attributeIndexMap = new HashMap<String, Integer>();
								for (int i = 0; i < attributes.size(); i++) {
									if (attributes.get(i).equals(" announce")) {
										System.out.println("here");
									}
									attributeIndexMap.put(attributes.get(i), i);
								}
								
								// for the edges of the graph
								// it still starts from the nodes of the designated objects
								ArrayList<TreeSet<Integer>> connectivity = new ArrayList<TreeSet<Integer>>();
								for (int i = 0; i < attributes.size(); i++) {
									connectivity.add(new TreeSet<Integer>());
								}
								
								// setup the connectivity
								
								// frame name node and target node
								int fIndex = attributeIndexMap.get(frameRaw);
								int ftIndex = attributeIndexMap.get(frameTargetRaw);
								if (this.isDirected) {
									// from frame target to frame name
									connectivity.get(ftIndex).add(fIndex);
								} else {
									connectivity.get(fIndex).add(ftIndex);
									connectivity.get(ftIndex).add(fIndex);									
								}
								
								// for dep relation, add what is it pointed by
//								if (!positionHeadListMap.containsKey(frameTargetPosition)) {
//									positionHeadListMap.put(frameTargetPosition, new LinkedList<String>());
//								}
								// no matter what, create the new entry, in case there's multiple frames (but seems unlikely)
								positionHeadListMap.put(frameTargetPosition, new LinkedList<String>());
								String dependent = frameTargetPosition;
								String head = dependentHeadMap.get(dependent);
								while (!head.equals("0")) {
									positionHeadListMap.get(frameTargetPosition).add(head);
									dependent = head;
									head = dependentHeadMap.get(dependent);
								}
								
								// frame element nodes
								for (int i = 0; i < frameElements.size(); i++) {
									String feRaw = "FRAME_ELEMENT-" + frameElements.get(i).replaceAll(frame + "\\-", "");
									int feIndex = attributeIndexMap.get(feRaw);
									if (this.isDirected) {
										// from frame element to frame name
										connectivity.get(feIndex).add(fIndex);
									} else {
										connectivity.get(fIndex).add(feIndex);
										connectivity.get(feIndex).add(fIndex);
									}
									
									String chunks = frameElementChunks.get(i);
									String[] tokens = chunks.split(" ");
									for (String token : tokens) {
										// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
										// AFTER WE HAVE DE DEP, THIS NEEDS TO BE MODIFIED TO MAKE IT MORE CONSISTENT
										// MAYBE NO MATTER WHAT, WE HAVE TO EXECUTE THE CURRENT else CLAUSE FOR DE
										// (HAVE ATTEMPTED)
										
										if (token.matches("[a-z]+_sp500_gics\\d+_company")) {
											token = token.toUpperCase();
										}
										
										// for bow nodes
										if ((featureSpace.equals(FeatureSpace.FRAME_BOW)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC_DAL) 
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_DAL)
											)
											&& token.matches(WORD_REGEX)
											&& !stopwordSet.contains(token)
										) {
											//int tokenIndex = attributeIndexMap.get(token);
											String lexicalItem = "LEXICAL_ITEM-" + token;
											int tokenIndex = attributeIndexMap.get(lexicalItem);
											if (this.isDirected) {
												// from token to frame element
												connectivity.get(tokenIndex).add(feIndex);
											} else {
												connectivity.get(feIndex).add(tokenIndex);
												connectivity.get(tokenIndex).add(feIndex);
											}
										}
										
										// for topic nodes
										if ((featureSpace.equals(FeatureSpace.FRAME_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC)
											)
											&& token.matches(WORD_REGEX)
											&& !stopwordSet.contains(token)
										) {
											if (wordTopicMap.containsKey(token)) {
												String topic = "TOPIC-" + wordTopicMap.get(token);
												int topicIndex = attributeIndexMap.get(topic);
												if (this.isDirected) {
													connectivity.get(topicIndex).add(feIndex);
												} else {
													connectivity.get(feIndex).add(topicIndex);
													connectivity.get(topicIndex).add(feIndex);
												}
											}
										}
										
										// for dal nodes
										if ((featureSpace.equals(FeatureSpace.FRAME_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
											)
											&& token.matches(WORD_REGEX)
											&& !stopwordSet.contains(token)
										) {
											if (wordDalMap.containsKey(token)) {
												for (String d : wordDalMap.get(token)) {
													String dal = "DAL-" + d;
													int dalIndex = attributeIndexMap.get(dal);
													if (this.isDirected) {
														connectivity.get(dalIndex).add(feIndex);
													} else {
														connectivity.get(feIndex).add(feIndex);
														connectivity.get(feIndex).add(feIndex);
													}
												}
											}
										}
										
										// only when the token is the designated entity 
										for (String targetObject : targetObjectSet) {
											if (targetObject.equals(token)) {
												int deIndex = attributeIndexMap.get(targetObject);
												if (this.isDirected) {
													// from the designated entity to frame element
													connectivity.get(deIndex).add(feIndex);
												} else {
													connectivity.get(feIndex).add(deIndex);
													connectivity.get(deIndex).add(feIndex);
												}
												
												positionsContainDE.add(frameTargetPosition);
											}
										}
									}
								}
								for (int i = 0; i < connectivity.size(); i++) {
									for (int j : connectivity.get(i)) {
										fragment.getGraph().get(i).add(j);
									}
								}
							}
							
							
							// this part of the code limits the features to distinguish same features in sentence frames
							// such that only two company co-occur in the same frame, then they have connection in the graph
							
							switch (this.mergeRule) {
								case FORREST_OF_SENTENCE_FRAGMENTS:
									if (fragment != null) {
										fragmentsToMerge.add(fragment);
										fragmentPositionMap.put(fragment, frameTargetPosition);
									}
									break;
								default:
									// for FORREST_OF_FRAME_FRAGMENTS and SINGLE_FRAGMENT
//									if (fragment != null) {
//										fragmentsToMerge.add(fragment);
//									}
									break;
							}
							// end
							
							//System.out.println(fragment);
						} // end of Iterate each frame
					}
				} // end of a parse of a sentence
				
				switch (this.mergeRule) {
					case FORREST_OF_SENTENCE_FRAGMENTS:
						// before we merge all the frame fragments
						// for FRAME_DE_DEP
						HashSet<String> positionsToKeep = new HashSet<String>();
						if (featureSpace.equals(FeatureSpace.FRAME_DE_DEP)) {
							// remove the frames that are below the DE frames in the dependency relation
							LinkedList<Integer> fragmentIdxToRemove = new LinkedList<Integer>(); // should remove in reverse order
							for (int i = 0; i < fragmentsToMerge.size(); i++) {
								Graph fragment = fragmentsToMerge.get(i);
								String fragmentPosition = fragmentPositionMap.get(fragment);
								boolean isInDEHeadList = false;
								for (String position : positionsContainDE) {
//									if (isInDEHeadList) {
//										break;
//									}
									if (fragmentPosition.equals(position)) {
										isInDEHeadList = true;
										positionsToKeep.add(fragmentPosition);
//										break;
									}
									LinkedList<String> headList = positionHeadListMap.get(position);
									for (String head : headList) {
										if (fragmentPosition.equals(head)) {
											isInDEHeadList = true;
											positionsToKeep.add(fragmentPosition);
//											break;
										}
									}
								}
								if (!isInDEHeadList) {
									fragmentIdxToRemove.push(i);
								}
							}
							// remove fragments from those with larger indices
							while (!fragmentIdxToRemove.isEmpty()) {
								int i = fragmentIdxToRemove.pop();
								fragmentsToMerge.remove(i);
							}
						}
						
						// merge all the fragments of frames into one fragment for a sentence
						// supplying targetObject set so that frames will be merged through targetObjects
						boolean keepIndexSuffix = false;
						Graph sentenceFragment = Graph.merge(fragmentsToMerge, targetObjectSet, keepIndexSuffix);
						
						if (sentenceFragment == null) {
							break;
						}
						
						HashMap<String, Integer> attributeIndexMap = new HashMap<String, Integer>();
						ArrayList<String> attributes = sentenceFragment.getAttributes();
						for (int i = 0; i < attributes.size(); i++) {
							attributeIndexMap.put(attributes.get(i), i);
						}

						// SOONER OR LATER IT SHOULD BE INDEPENDENT OF THE mergeRule!!!!
						if (featureSpace.equals(FeatureSpace.FRAME_DEP)
							|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP)
							|| featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC)
							|| featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC_DAL)
							|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC_DAL)
							|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC)
							|| featureSpace.equals(FeatureSpace.FRAME_DEP_DAL)
							|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_DAL)
							|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP)
							|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP)
							|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC)
							|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC)
							|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_DAL)
							|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_DAL)
							|| featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL)
							|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
						) {
							// adding dependencies for all targets (target positions)
							for (String position : positionHeadListMap.keySet()) {
								if ((featureSpace.equals(FeatureSpace.FRAME_DE_DEP) 
									|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC)
									|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC_DAL)
									|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_DAL)
									|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP)
									|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC)
									|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_DAL)
									|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
									)
									&& !positionsToKeep.contains(position)) {
									continue;
								}
								String dependentFrame = positionFrameNameMap.get(position);
								// how to deal with multiple frames of the same token? currently only using the first frame
								
								String headFrame = null;
								LinkedList<String> headList = positionHeadListMap.get(position);
								for (String head : headList) {
									if (positionFrameNameMap.containsKey(head)) {
										if ((featureSpace.equals(FeatureSpace.FRAME_DE_DEP) 
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_DE_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_DAL)
											|| featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
											)
											&& !positionsToKeep.contains(head)) {
											continue;
										}
										
										headFrame = positionFrameNameMap.get(head);
										break;
									}
								}
								
								if (headFrame != null) {
									int dependentFrameIndex = attributeIndexMap.get(dependentFrame);
									int headFrameIndex = attributeIndexMap.get(headFrame);
									
									// add connectivity
									if (this.isDirected) {
										// from dependent to head
										sentenceFragment.getGraph().get(dependentFrameIndex).add(headFrameIndex);
									} else {
										sentenceFragment.getGraph().get(dependentFrameIndex).add(headFrameIndex);
										sentenceFragment.getGraph().get(headFrameIndex).add(dependentFrameIndex);
									}
								}
							}
						}
						
						fragmentSet.add(sentenceFragment);
						
						break;
					default:
						// for FORREST_OF_FRAME_FRAGMENTS and SINGLE_FRAGMENT
						break;
				}
				
			}	// end of a sentence
		}
		
		return fragmentSet;
	}
	
	private TreeMap<Graph, Integer> getFragmentDocFreqMap(String[] filenames) {
		TreeMap<Graph, Integer> fragmentDocFreqMap = new TreeMap<Graph, Integer>();
		
		for (String filename : filenames) {
			System.out.println(filename);

			String content = Tools.read(filename);
			
			//String depFilename = filename.replaceAll("fulltext_sen_rlv_sendoc_semraw", "fulltext_sen_rlv_sendoc_dep");
			String depFilename = filename.replaceAll("/sem/", "/dep/");
			HashMap<String, String> dependentHeadMap = getDependentHeadMap(depFilename);
			
			HashMap<String, Integer> wordTopicMap = this.docWordTopicMapMap.get(filename.replaceAll(".*/", ""));
			
			TreeSet<Graph> fragmentSet = getFragments(content, dependentHeadMap, wordTopicMap);
			
			for (Graph fragment : fragmentSet) {
				if (fragmentDocFreqMap.containsKey(fragment)) {
					fragmentDocFreqMap.put(fragment, fragmentDocFreqMap.get(fragment) + 1);
				} else {
					fragmentDocFreqMap.put(fragment, 1);
				}
			}
		}
		
		return fragmentDocFreqMap;
	}
	
	private HashMap<String, String> getDependentHeadMap(String depFilename) {
		HashMap<String, String> dependentHeadMap = new HashMap<String, String>();
		
		String[] lines = Tools.read(depFilename).split("\n");
		for (String line : lines) {
			String[] cols = line.split("\t");
			String index = cols[0];
			String headIndex = cols[6];
			dependentHeadMap.put(index, headIndex);
		}
		
		return dependentHeadMap;
	}
	
	public Graph getMergedFragment(String[] filenames) {
		ArrayList<Graph> fragmentList = new ArrayList<Graph>();
		TreeSet<String> uniqueAttributeSet = new TreeSet<String>();
		for (String filename : filenames) {
//			if (filename.contains("2008-10-01")) {
//				System.out.println("here");
//			}
			System.out.println(filename);
			String content = Tools.read(filename);

			//TODO	Hooshmand
			//	Change Hardcoded semafor/dependencies into given
			
			//String depFilename = filename.replaceAll("fulltext_sen_rlv_sendoc_semraw", "fulltext_sen_rlv_sendoc_dep");
			String depFilename = filename.replaceAll("/sem/", "/dep/");
			
			HashMap<String, Integer> wordTopicMap = this.docWordTopicMapMap.get(filename.replaceAll(".*/", ""));
			
			TreeSet<Graph> fragmentSet = null;
			if (this.featureSpace.equals(FeatureSpace.DEP)) {
				fragmentSet = getDepFragments(depFilename);
			} else {
				HashMap<String, String> dependentHeadMap = getDependentHeadMap(depFilename);
				fragmentSet = getFragments(content, dependentHeadMap, wordTopicMap);
			}
			
			for (Graph fragment : fragmentSet) {
				fragmentList.add(fragment);
				for (String attribute : fragment.getAttributes()) {
					if (attribute.matches("[A-Z]+_SP500_GICS\\d+_COMPANY")) {
						uniqueAttributeSet.add(attribute);
					}
				}
			}
		}
		
		Graph fragment = null;
		boolean keepIndexSuffix = false;
		switch (this.mergeRule) {
			case SINGLE_FRAGMENT:
				fragment = Graph.merge(fragmentList, uniqueAttributeSet, keepIndexSuffix);
				System.out.println("NULL FRAGMENT!");
				break;
			default:
				// default is for FORREST_OF_FRAME_FRAGMENTS and FORREST_OF_SENTENCE_FRAGMENTS
				// do not merge by companies
				fragment = Graph.merge(fragmentList, new TreeSet<String>(), keepIndexSuffix);
				break;
		}
		
		return fragment;
	}


	public static TreeMap<String, Double> getDailyPriceChange(String ticker, String startDateStr, String endDateStr, Calendar calendarFixed) {
		TreeMap<String, Double> dailyPriceChange = new TreeMap<String, Double>();
		
		Calendar date = Tools.stringToCalendar(startDateStr, calendarFixed);
		Calendar endDate = Tools.stringToCalendar(endDateStr, calendarFixed);
		while (date.before(endDate) || date.equals(endDate)) {
			String dateStr = Tools.CalendarToString(date);
			try {
				Double logChange = StockPrice.getStockPriceChangeNextToPrevious(ticker, dateStr.replaceAll(" 16:00:00", ""));
				System.out.println(dateStr + ": " + logChange);
				dailyPriceChange.put(dateStr, logChange);
			} catch (StockPriceNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			date.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		return dailyPriceChange;
	}

	
	public static TreeMap<String, Double> getGicsDailyPriceChange(String gics, String startDateStr, String endDateStr, Calendar calendarFixed) {
		TreeMap<String, Double> gicsDailyPriceChange = new TreeMap<String, Double>();
		
		Calendar date = Tools.stringToCalendar(startDateStr, calendarFixed);
		Calendar endDate = Tools.stringToCalendar(endDateStr, calendarFixed);
		while (date.before(endDate) || date.equals(endDate)) {
			String dateStr = Tools.CalendarToString(date);
			try {
				Double logChange = StockPrice.getGicsStockPriceChangeNextToPrevious(gics, dateStr.replaceAll(" 16:00:00", ""));
				System.out.println(dateStr + ": " + logChange);
				gicsDailyPriceChange.put(dateStr, logChange);
			} catch (StockPriceNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			date.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		return gicsDailyPriceChange;
	}


	public static TreeMap<String, String> getDailyPriceLabel(String ticker, String startDateStr, String endDateStr, Calendar calendarFixed, Task task) {
		TreeMap<String, String> dailyPriceLabel = new TreeMap<String, String>();
		TreeMap<String, Double> dailyPriceChange = getDailyPriceChange(ticker, startDateStr, endDateStr, calendarFixed);
		
		TreeMap<String, Double> gicsDailyPriceChange = null;
		String gics = Sector.getGicsByTicker(ticker);
		if (task.equals(Task.PRICE_FOLLOW_MARKET)
			|| task.equals(Task.PRICE_CHANGE_RELATIVE_TO_MARKET)
			|| task.equals(Task.PRICE_POLARITY_RELATIVE_TO_MARKET)) {
			gicsDailyPriceChange = getGicsDailyPriceChange(gics, startDateStr, endDateStr, calendarFixed);
		}
		
		for (String dateStr : dailyPriceChange.keySet()) {
			double logChange = dailyPriceChange.get(dateStr);
			if (task.equals(Task.PRICE_CHANGE)) {
				if (logChange > Math.log(1.02) || logChange < Math.log(0.98)) {
					String label = "1";
					dailyPriceLabel.put(dateStr, label);
				} else {
					String label = "-1";
					dailyPriceLabel.put(dateStr, label);
				}
			} else if (task.equals(Task.PRICE_POLARITY)) {
				// positive label if price increase more than 2%, negative label if price decrease more than 2%
				if (logChange > Math.log(1.02)) {
					String label = "1";
					dailyPriceLabel.put(dateStr, label);
				} else if (logChange < Math.log(0.98)) {
					String label = "-1";
					dailyPriceLabel.put(dateStr, label);
				}
			} else if (task.equals(Task.PRICE_FOLLOW_MARKET)) {
				double gicsLogChange = gicsDailyPriceChange.get(dateStr);
				if (logChange > Math.log(1.02) || logChange < Math.log(0.98)) {
					if (Math.signum(logChange) == Math.signum(gicsLogChange)) {
						String label = "1";
						dailyPriceLabel.put(dateStr, label);
					} else {
						String label = "-1";
						dailyPriceLabel.put(dateStr, label);
					}
				}
			} else if (task.equals(Task.PRICE_CHANGE_RELATIVE_TO_MARKET)) {
				double gicsLogChange = gicsDailyPriceChange.get(dateStr);
				System.out.println("date: " + dateStr);
				System.out.println("comp price change: " + logChange + ", " + Math.exp(logChange) + ", " + (Math.exp(logChange) - 1));
				System.out.println("gics price change: " + gicsLogChange + ", " + Math.exp(gicsLogChange) + ", " + (Math.exp(gicsLogChange) - 1));
				if (Math.abs(Math.exp(logChange) - Math.exp(gicsLogChange)) > 0.02) {
					String label = "1";
					dailyPriceLabel.put(dateStr, label);
				} else {
					String label = "-1";
					dailyPriceLabel.put(dateStr, label);
				}
			} else if (task.equals(Task.PRICE_POLARITY_RELATIVE_TO_MARKET)) {
				double gicsLogChange = gicsDailyPriceChange.get(dateStr);
				System.out.println("date: " + dateStr);
				System.out.println("comp price change: " + logChange + ", " + Math.exp(logChange) + ", " + (Math.exp(logChange) - 1));
				System.out.println("gics price change: " + gicsLogChange + ", " + Math.exp(gicsLogChange) + ", " + (Math.exp(gicsLogChange) - 1));
				if (Math.exp(logChange) > Math.exp(gicsLogChange) + 0.02) {
					String label = "1";
					dailyPriceLabel.put(dateStr, label);
				} else if (Math.exp(logChange) < Math.exp(gicsLogChange) - 0.02) {
					String label = "-1";
					dailyPriceLabel.put(dateStr, label);
				}
			}
		}
		
		return dailyPriceLabel;
	}
	
	private LinkedHashMap<Graph, Integer> getFragmentDocFreqMapSorted(String ticker, String gics, String datasetDir) {
		String[] filenames = findFilenames(ticker, gics, datasetDir);
		TreeMap<Graph, Integer> fragmentDocFreqMap = getFragmentDocFreqMap(filenames);
		LinkedHashMap<Graph, Integer> fragmentFreqMapSorted = Tools.sortMapByValue(fragmentDocFreqMap);
		
		StringBuilder sb = new StringBuilder();
		for (Graph fragment : fragmentFreqMapSorted.keySet()) {
			System.out.println(fragmentFreqMapSorted.get(fragment) + "\t" + fragment.toStringOneLine());
			sb.append(fragmentFreqMapSorted.get(fragment) + "\t" + fragment.toStringOneLine() + "\n");
		}
		System.out.println("total fragments with doc freq: " + fragmentDocFreqMap.size());
		Tools.write("fragment_features_AAPL_2010", sb.toString());
		
		return fragmentFreqMapSorted;
	}
	
	private ArrayList<String> getFragmentFeatures(String ticker, String gics, String datasetDir, int minFreq) {
		ArrayList<String> fragmentFeatures = new ArrayList<String>();
		
		LinkedHashMap<Graph, Integer> fragmentDocFreqMapSorted = getFragmentDocFreqMapSorted(ticker, gics, datasetDir);
		for (Graph fragment : fragmentDocFreqMapSorted.keySet()) {
			int freq = fragmentDocFreqMapSorted.get(fragment);
			//
			if (freq >= minFreq) {
				fragmentFeatures.add(fragment.toStringOneLine());
			}
		}
		
		Collections.sort(fragmentFeatures);
		return fragmentFeatures;
	}
	
	// currently the price is not based on 16:00:00 but only the date !!!!!!!!!!
	public static TreeMap<String, ArrayList<String>> getDateDocListMap(String[] filenames) {
		TreeMap<String, ArrayList<String>> dateDocListMap = new TreeMap<String, ArrayList<String>>();
		for (int i = 0; i < filenames.length; i++) {
			Matcher m = Pattern.compile(".*?(\\d{4}\\-\\d{2}\\-\\d{2}).*?").matcher(filenames[i]);
			if (m.find()) {
				String date = m.group(1);
				date += " 16:00:00";
				if (!dateDocListMap.containsKey(date)) {
					dateDocListMap.put(date, new ArrayList<String>());
				}
				dateDocListMap.get(date).add(filenames[i]);
			}
		}
		return dateDocListMap;
	}

	// Hooshmand:
	// Returns the a String[] with the names of semafor files in @DatasetDir
	public String[] getSemFiles(){

		//String semaforDir = getDatasetDir()+"/fulltext_sen_rlv_sendoc_semraw";
		String semaforDir = getDatasetDir()+"/sem";
		File folder = new File(semaforDir);
		File[] listofFiles = folder.listFiles();
		
		String[] filenames = new String[listofFiles.length];

		for (int i = 0; i < listofFiles.length; i++){
			if( listofFiles[i].isFile() && listofFiles[i].getName().endsWith(".txt")){

				filenames[i] = semaforDir + "/" + listofFiles[i].getName();
				
			}
		}

		return filenames;
	}

	public void run() {
		// for topic features
		if (this.featureSpace.equals(FeatureSpace.FRAME_TOPIC)
			|| this.featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC)
			|| this.featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
		) {
			setDocWordTopicMapMap();
		}
		
		// for dal features
		if (this.featureSpace.equals(FeatureSpace.FRAME_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_DEP_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_DEP_TOPIC_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_DE_DEP_TOPIC_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_DE_DEP_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_TOPIC_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_DEP_TOPIC_DAL)
			|| this.featureSpace.equals(FeatureSpace.FRAME_BOW_DE_DEP_TOPIC_DAL)
		) {
			setWordDalMap();
		}
		
		if (!new File(outDir).exists()) {
			Tools.createDir(outDir);
		}
		String outDocFilename = outDir + "/" + name + ".doc";
		String outGraphFilename = outDir + "/" + name + ".graph";
		String outGraphNodeLabelFilename = outGraphFilename + ".nodeid";
		
		Tools.write(outDocFilename, "");
		Tools.write(outGraphFilename, "");

		LinkedList<Graph> fragmentMergedList = new LinkedList<Graph>();
		// docList is written into the file.doc
		LinkedList<String> docList = new LinkedList<String>();

		String[] semFilenames = getSemFiles();

		for (String filename : semFilenames){
			if (filename != null){
				System.out.println("Hooshmand version:"+filename);
				//getMergedFragments only accepts String[]
				String[] filenames = {filename};	

				Graph fragmentMerged = getMergedFragment(filenames);
				if (fragmentMerged == null) {

					continue;
				}
				docList.add(filename);
				fragmentMergedList.add(fragmentMerged);
				
			}
		}
		
		// remove nodes that have a low frequency (doc freq)
		HashMap<String, Integer> nodeFreqMap = new HashMap<String, Integer>();
		for (Graph f : fragmentMergedList) {
			for (String attribute : f.getAttributes()) {
				if (!nodeFreqMap.containsKey(attribute)) {
					nodeFreqMap.put(attribute, 1);
				} else {
					nodeFreqMap.put(attribute, nodeFreqMap.get(attribute) + 1);
				}
			}
		}
		LinkedHashMap<String, Integer> nodeFreqMapSorted = Tools.sortMapByValue(nodeFreqMap);
		HashSet<String> nodesToRemove = new HashSet<String>();
		int count = 0;
		for (String node : nodeFreqMapSorted.keySet()) {
			if (++count > limit || nodeFreqMapSorted.get(node) < minFreq) {
				nodesToRemove.add(node);
			}
		}
		for (Graph f : fragmentMergedList) {
			f.removeNodes(nodesToRemove);
		}
		// end remove nodes
		
		// collect node labels
		TreeSet<String> nodeLabelSet = new TreeSet<String>();
		for (Graph f : fragmentMergedList) {
			for (String attribute : f.getAttributes()) {
				nodeLabelSet.add(attribute);
			}
		}
		
		// convert the node string into node id
		TreeMap<String, Integer> nodeLabelIdMap = new TreeMap<String, Integer>();
		StringBuilder sbNodes = new StringBuilder();
		int id = 0;
		for (String nodeLabel : nodeLabelSet) {
			nodeLabelIdMap.put(nodeLabel, id++);	// id starts with 1
			sbNodes.append(nodeLabel + "\n");
		}
		Tools.write(outGraphNodeLabelFilename, sbNodes.toString());
		
		for (int i = 0; i < fragmentMergedList.size(); i++) {
			Graph f = fragmentMergedList.get(i);
			if (f.getAttributes().isEmpty()) {
				continue;
			}
			
			ArrayList<String> ids = new ArrayList<String>();
			for (String attribute : f.getAttributes()) {
			
				ids.add(String.valueOf(nodeLabelIdMap.get(attribute)));
			}
			f.setAttributes(ids);
			
			Tools.append(outGraphFilename, f.toStringOneLine() + "\n");
			Tools.append(outDocFilename, docList.get(i) + "\n");
		}
		
	}
	
	protected ArrayList<String> removeDuplicateSentences(ArrayList<String> filenameList) {
		ArrayList<String> newFilenameList = new ArrayList<String>();
		Set<String> sentences = new HashSet<String>();
		for (String filename : filenameList) {
			String sentenceFilename = filename.replaceAll("fulltext_sen_rlv_sendoc_semraw", "fulltext_sen_rlv_sendoc");
			//String sentenceFilename = filename.replaceAll("/sem/", "/dep/");
			String sentence = Tools.read(sentenceFilename);
			if (!sentences.contains(sentence)) {
				newFilenameList.add(filename);
				sentences.add(sentence);
			}
		}
		return newFilenameList;
	}
	
	public MergeRule getMergeRule() {
		return mergeRule;
	}

	public void setMergeRule(MergeRule mergeRule) {
		this.mergeRule = mergeRule;
	}

	public FeatureSpace getFeatureSpace() {
		return featureSpace;
	}

	public void setFeatureSpace(FeatureSpace featureSpace) {
		this.featureSpace = featureSpace;
	}

	public boolean isDirected() {
		return isDirected;
	}

	public void setDirected(boolean isDirected) {
		this.isDirected = isDirected;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public String getGics() {
		return gics;
	}

	public void setGics(String gics) {
		this.gics = gics;
	}

	public String getDatasetDir() {
		return datasetDir;
	}

	public void setDatasetDir(String datasetDir) {
		this.datasetDir = datasetDir;
	}

	public String getStartDateStr() {
		return startDateStr;
	}

	public void setStartDateStr(String startDateStr) {
		this.startDateStr = startDateStr;
	}

	public String getEndDateStr() {
		return endDateStr;
	}

	public void setEndDateStr(String endDateStr) {
		this.endDateStr = endDateStr;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOutDir() {
		return outDir;
	}

	public void setOutDir(String outDir) {
		this.outDir = outDir;
	}

	public int getMinFreq() {
		return minFreq;
	}

	public void setMinFreq(int minFreq) {
		this.minFreq = minFreq;
	}
	
	public int getLimit() {
		return limit;
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}

	public Calendar getCalendarFixed() {
		return calendarFixed;
	}

	public void setCalendarFixed(Calendar calendarFixed) {
		this.calendarFixed = calendarFixed;
	}

	public Task getTask() {
		return task;
	}
	
	public void setTask(String taskStr) {
		if (taskStr == null) {
			this.task = Task.PRICE_POLARITY;
		}
		
		if (taskStr.equals("chg")) {
			this.task = Task.PRICE_CHANGE;
		} else if (taskStr.equals("pol")) {
			this.task = Task.PRICE_POLARITY;
		} else if (taskStr.equals("mkt")) {
			this.task = Task.PRICE_FOLLOW_MARKET;
		} else if (taskStr.equals("chg_rel_mkt")) {
			this.task = Task.PRICE_CHANGE_RELATIVE_TO_MARKET;
		} else if (taskStr.equals("pol_rel_mkt")) {
			this.task = Task.PRICE_POLARITY_RELATIVE_TO_MARKET;
		} else {
			this.task = Task.PRICE_POLARITY;
		}
	}

	public void setTask(Task task) {
		this.task = task;
	}
	
	public HashSet<String> getStopwordSet() {
		return stopwordSet;
	}
	
	public void setStopwordSet(String stopwordFilename) {
		if (stopwordFilename == null) {
			return;
		}
		
		String[] stopwords = Tools.read(stopwordFilename).split("\n");
		for (String s : stopwords) {
			this.stopwordSet.add(s);
		}
	}
	
	public String getDalFilename() {
		return dalFilename;
	}

	public void setDalFilename(String dalFilename) {
		this.dalFilename = dalFilename;
	}

	public TreeMap<String, String[]> getWordDalMap() {
		return wordDalMap;
	}

	public void setWordDalMap() {
		this.wordDalMap = new TreeMap<String, String[]>();
		String[] lines = Tools.read(this.dalFilename).split("\n");
		for (String line : lines) {
			String[] cols = line.split("\\s+");
			if (cols.length != 4) {
				System.out.println("error in dal file.");
				System.exit(0);
			}
			
			String word = cols[0];
			String[] dal = new String[3];
			
			if ((Double.parseDouble(cols[1]) - DAL_PLEASANTNESS_MEAN) >= DAL_SCALE * DAL_PLEASANTNESS_SD) {
				dal[0] = "Pleasant";
			} else if ((DAL_PLEASANTNESS_MEAN - Double.parseDouble(cols[1])) >= DAL_SCALE * DAL_PLEASANTNESS_SD) {
				dal[0] = "Unpleasant";
			} else {
				dal[0] = "NeutralPleasantness";
			}
			
			if ((Double.parseDouble(cols[2]) - DAL_ACTIVATION_MEAN) >= DAL_SCALE * DAL_ACTIVATION_SD) {
				dal[1] = "Active";
			} else if ((DAL_ACTIVATION_MEAN - Double.parseDouble(cols[2])) >= DAL_SCALE * DAL_ACTIVATION_SD) {
				dal[1] = "Passive";
			} else {
				dal[1] = "NeutralActivation";
			}
			
			if ((Double.parseDouble(cols[3]) - DAL_IMAGERY_MEAN) >= DAL_SCALE * DAL_IMAGERY_SD) {
				dal[2] = "EasyToImagine";
			} else if ((DAL_IMAGERY_MEAN - Double.parseDouble(cols[3])) >= DAL_SCALE * DAL_IMAGERY_SD) {
				dal[2] = "HardToImagine";
			} else {
				dal[2] = "NeutralImagery";
			}
			
			this.wordDalMap.put(word, dal);
		}
	}

	public void setDocWordTopicMapMap() {
		docWordTopicMapMap = new HashMap<String, HashMap<String,Integer>>();
		String[] docs = Tools.read(tmDocsFilename).split("\n");
		for (int i = 0; i < docs.length; i++) {
			docs[i] = docs[i].replaceAll(".*/", "");
		}
		String[] words = Tools.read(tmWordsFilename).split("\n");
		
		String[] rows = Tools.read(tmTopicAssignmentFilename).split("\n");
		for (int i = 1; i < rows.length; i++) {
			String[] cols = rows[i].split("\t");
			String doc = docs[Integer.parseInt(cols[0])];
			String word = words[Integer.parseInt(cols[1])];
			Integer topic = Integer.parseInt(cols[2]);
			
			if (!docWordTopicMapMap.containsKey(doc)) {
				docWordTopicMapMap.put(doc, new HashMap<String, Integer>());
			}
			docWordTopicMapMap.get(doc).put(word, topic);
		}
	}

	public String getTmDocsFilename() {
		return tmDocsFilename;
	}

	public void setTmDocsFilename(String tmDocsFilename) {
		this.tmDocsFilename = tmDocsFilename;
	}

	public String getTmWordsFilename() {
		return tmWordsFilename;
	}

	public void setTmWordsFilename(String tmWordsFilename) {
		this.tmWordsFilename = tmWordsFilename;
	}

	public String getTmTopicAssignmentFilename() {
		return tmTopicAssignmentFilename;
	}

	public void setTmTopicAssignmentFilename(String tmTopicAssignmentFilename) {
		this.tmTopicAssignmentFilename = tmTopicAssignmentFilename;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String gics = null;	// "45";
		String ticker = null;	// "IBM";
		String datasetDir = null;	// "/Users/xie/Dataset/reuters";
		String startDateStr = null; 	//"2007-01-01-16-00-00";
		String endDateStr = null;	// "2012-08-31-16-00-00";
		String outDir = null;	// "output/fragment_modeler/" + gics;
		int minFreq = 1;
		int limit = Integer.MAX_VALUE;
		String mergeRuleStr = null;	// "FORREST_OF_SENTENCE_FRAGMENTS";
		String featureSpaceStr = null;	// "FRAME_DE_ONLY";
		boolean isDirected = true;
		String taskStr = null;
		String stopwordFilename = null;
		
		String tmDocsFilename = null;
		String tmWordsFilename = null;
		String tmTopicAssignmentFilename = null;
		
		String dalFilename = null;
		
		String name = null;	// "fragment_forestsent_frame_dep_" + ticker + "_" + startDateStr.replaceAll("\\s", "_").replaceAll("\\:", "-") + "_" + endDateStr.replaceAll("\\s", "_").replaceAll("\\:", "-");
	
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--name")) {
				name = args[++i];
			} else if (args[i].equals("--dataset_dir")) {
				datasetDir = args[++i];
			} else if (args[i].equals("--out_dir")) {
				outDir = args[++i];
			} else if (args[i].equals("--min_freq")) {
				minFreq = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--limit")) {
				limit = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--merge_rule")) {
				mergeRuleStr = args[++i];
			} else if (args[i].equals("--feature_space")) {
				featureSpaceStr = args[++i];
			} else if (args[i].equals("--is_directed")) {
				isDirected = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--task")) {
				taskStr = args[++i];
			} else if (args[i].equals("--stopwords")) {
				stopwordFilename = args[++i];
			} else if (args[i].equals("--tm_topic_assignment")) {
				tmTopicAssignmentFilename = args[++i];
			} else if (args[i].equals("--tm_docs")) {
				tmDocsFilename = args[++i];
			} else if (args[i].equals("--tm_words")) {
				tmWordsFilename = args[++i];
			} else if (args[i].equals("--dal")) {
				dalFilename = args[++i];
			}
		}
		
		/*	
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--name")) {
				name = args[++i];
			} else if (args[i].equals("--gics")) {
				gics = args[++i];
			} else if (args[i].equals("--ticker")) {
				ticker = args[++i];
			} else if (args[i].equals("--dataset_dir")) {
				datasetDir = args[++i];
			} else if (args[i].equals("--start_date")) {
				startDateStr = args[++i];
			} else if (args[i].equals("--end_date")) {
				endDateStr = args[++i];
			} else if (args[i].equals("--out_dir")) {
				outDir = args[++i];
			} else if (args[i].equals("--min_freq")) {
				minFreq = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--limit")) {
				limit = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--merge_rule")) {
				mergeRuleStr = args[++i];
			} else if (args[i].equals("--feature_space")) {
				featureSpaceStr = args[++i];
			} else if (args[i].equals("--is_directed")) {
				isDirected = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--task")) {
				taskStr = args[++i];
			} else if (args[i].equals("--stopwords")) {
				stopwordFilename = args[++i];
			} else if (args[i].equals("--tm_topic_assignment")) {
				tmTopicAssignmentFilename = args[++i];
			} else if (args[i].equals("--tm_docs")) {
				tmDocsFilename = args[++i];
			} else if (args[i].equals("--tm_words")) {
				tmWordsFilename = args[++i];
			} else if (args[i].equals("--dal")) {
				dalFilename = args[++i];
			}
		}
		*/
		
		SemGraphModeler semGraphModeler = new SemGraphModeler();
		semGraphModeler.setMergeRule(mergeRuleStr);
		semGraphModeler.setFeatureSpace(featureSpaceStr);
		semGraphModeler.setIsDirected(isDirected);
		//semGraphModeler.setTicker(ticker);
		//semGraphModeler.setGics(gics);
		semGraphModeler.setDatasetDir(datasetDir);
		//semGraphModeler.setStartDateStr(startDateStr);
		//semGraphModeler.setEndDateStr(endDateStr);
		semGraphModeler.setOutDir(outDir);
		semGraphModeler.setMinFreq(minFreq);
		semGraphModeler.setLimit(limit);
		semGraphModeler.setName(name);
		//semGraphModeler.setTask(taskStr);
		semGraphModeler.setStopwordSet(stopwordFilename);
		semGraphModeler.setTmDocsFilename(tmDocsFilename);
		semGraphModeler.setTmWordsFilename(tmWordsFilename);
		semGraphModeler.setTmTopicAssignmentFilename(tmTopicAssignmentFilename);
		semGraphModeler.setDalFilename(dalFilename);
		semGraphModeler.run();
		
		System.out.print("done SemGraphModeler");
		for (String arg : args) {
			System.out.print(" " + arg);
		}
		System.out.println();

	}

}
