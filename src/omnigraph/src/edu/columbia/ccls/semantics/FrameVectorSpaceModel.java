package edu.columbia.ccls.semantics;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.columbia.ccls.text.CorpusAgent;
import edu.columbia.ccls.text.stanford.PosTagger;
import edu.columbia.ccls.util.Tools;

public class FrameVectorSpaceModel extends CorpusAgent {
	
	public static final String SRL_RAW_ANNOTATION_PATTERN = " *?<annotationSet ID=\"\\d+\" " +
			"frameName=\"(.*?)\">.*?" +
			"<layer ID=\"\\d+\" name=\"Target\">.*?" +
			"<label ID=\"\\-?\\d+\" end=\"(\\d+)\" name=\"Target\" start=\"(\\d+)\"/>.*?" +
			"</layer>.*?" +
			"(<layer ID=\"\\d+\" name=\"FE\">.*?" +
			//"(<label ID=\"\\-?\\d+\" name=\".*?\" chunk=\".*?\"/>)*?.*?" +
			"</layer>).*?</annotationSet>";
	public static final String SRL_ANNOTATION_PATTERN = " *?<annotationSet ID=\"\\d+\" " +
			"frameName=\"(.*?)\">.*?" +
			"<layer ID=\"\\d+\" name=\"Target\">.*?" +
			"<label ID=\"\\-?\\d+\" name=\"Target\" chunk=\"(.*?)\"/>.*?" +
			"</layer>.*?" +
			"(<layer ID=\"\\d+\" name=\"FE\">.*?" +
			//"(<label ID=\"\\-?\\d+\" name=\".*?\" chunk=\".*?\"/>)*?.*?" +
			"</layer>).*?</annotationSet>";
	public static final String SRL_RAW_ANNOTATION_FE_PATTERN = "<label ID=\"\\-?\\d+\" end=\"(\\d+)\" name=\"(.*?)\" start=\"(\\d+)\"/>";
	public static final String SRL_ANNOTATION_FE_PATTERN = "<label ID=\"\\-?\\d+\" name=\"(.*?)\" chunk=\"(.*?)\"/>";
	public static final String FRAME_FEATURE_PREFIX = "FRAME_FEATURE-";
	public static final String PRIOR_POLARITY_FEATURE_PREFIX = "PRIOR_POLARITY_FEATURE-";
	public static final double DAL_PLEASANTNESS_MEAN = 1.839958774;
	public static final double DAL_ACTIVATION_MEAN = 1.847899428;
	public static final double DAL_IMAGERY_MEAN = 1.934568749;
	public static final double DAL_PLEASANTNESS_SD = 0.440015477;
	public static final double DAL_ACTIVATION_SD = 0.3943928;
	public static final double DAL_IMAGERY_SD = 0.634407312;
	public static final double DAL_MEAN = 1.874142317;
	public static final double DAL_SD = 0.502355579;
	
	protected Double thresholdFrame;
	protected Double limitFrame;
	protected boolean useFrame = true;
	protected boolean useFrameTarget = false;
	protected boolean useFrameElement = false;
	protected boolean useBow = false;
	protected boolean useBof = true;
	protected boolean useTof = false;	// tree of frame
	protected String semCorpusDir = null;
	protected LinkedList<String> semDocList = null;
	protected boolean usePriorPolarity = false;
	protected String dalFilename = null;
	protected TreeMap<String, Double[]> wordDalMap = null;
	
	protected void loadSemDocList() {
		this.semDocList = new LinkedList<String>();
		if (this.docListFilename == null) {
			String[] docs = new File(this.semCorpusDir).list();
			for (String doc : docs) {
				semDocList.add(this.semCorpusDir + "/" + doc);
			}
		} else {
			String[] rows = Tools.read(this.docListFilename).split("\n");
			for (String row : rows) {
				String[] cols = row.split(",");
				String filename = this.semCorpusDir + "/" + cols[0];
				if (!new File(filename).exists()) {
					filename += ".txt";
				}
				if (new File(filename).exists()) {
					semDocList.add(filename);
				}
			}
		}
	}
	
	public LinkedHashMap<Integer, Object> getDocumentData(String filename) {
		return getDocumentDataSrl(filename);
	}
	
	public LinkedList<String> getDirectDocList() {
		return this.semDocList;
	}
	
	protected String[] preSelectFeatures() {
		String[] features = null;
		
		String[] featuresFrame = null;
		if (this.useFrame) {
			HashMap<String, Integer> frameFeatureFreqMap = new HashMap<String, Integer>();
			
			for (String filename : this.semDocList) {
				System.out.println(filename);
				String content = Tools.read(filename);
				
				Matcher m = Pattern.compile(SRL_ANNOTATION_PATTERN, Pattern.MULTILINE | Pattern.DOTALL).matcher(content);
				while (m.find()) {
					//System.out.println(m.group());
					String frame = FRAME_FEATURE_PREFIX + m.group(1);
					String frameTarget = frame + "-" + m.group(2).toLowerCase();
					ArrayList<String> frameElements = new ArrayList<String>();
					Matcher mFE = Pattern.compile(SRL_ANNOTATION_FE_PATTERN, Pattern.MULTILINE | Pattern.DOTALL).matcher(m.group(3));
					while (mFE.find()) {
						frameElements.add(frame + "-" + mFE.group(1));
					}
					
					if (useFrame) {
						if (frame != null) {
							if (frameFeatureFreqMap.containsKey(frame)) {
								frameFeatureFreqMap.put(frame, frameFeatureFreqMap.get(frame) + 1);
							} else {
								frameFeatureFreqMap.put(frame, 1);
							}
						}
					}
					
					if (useFrameTarget) {
						if (frameTarget != null) {
							if (frameFeatureFreqMap.containsKey(frameTarget)) {
								frameFeatureFreqMap.put(frameTarget, frameFeatureFreqMap.get(frameTarget) + 1);
							} else {
								frameFeatureFreqMap.put(frameTarget, 1);
							}
						}
					}
					
					if (useFrameElement) {
						for (String fe : frameElements) {
							if (fe != null) {
								if (frameFeatureFreqMap.containsKey(fe)) {
									frameFeatureFreqMap.put(fe, frameFeatureFreqMap.get(fe) + 1);
								} else {
									frameFeatureFreqMap.put(fe, 1);
								}
							}
						}
					}
				}
			}
			
			featuresFrame = selectFeatures(Tools.sortMapByValue(frameFeatureFreqMap), this.thresholdFrame, this.limitFrame);
		} else {
			featuresFrame = new String[0];
		}
		
		String[] featuresNGram = null;
		if (this.useBow) {
			featuresNGram = super.preSelectFeatures();
		} else {
			featuresNGram = new String[0];
		}
		
		String[] featuresPriorPolarity = null;
		if (this.usePriorPolarity) {
			featuresPriorPolarity = new String[12];
			featuresPriorPolarity[0] = PRIOR_POLARITY_FEATURE_PREFIX + "All_Pleasantness";
			featuresPriorPolarity[1] = PRIOR_POLARITY_FEATURE_PREFIX + "All_Activation";
			featuresPriorPolarity[2] = PRIOR_POLARITY_FEATURE_PREFIX + "All_Imagery";
			featuresPriorPolarity[3] = PRIOR_POLARITY_FEATURE_PREFIX + "VB_Pleasantness";
			featuresPriorPolarity[4] = PRIOR_POLARITY_FEATURE_PREFIX + "VB_Activation";
			featuresPriorPolarity[5] = PRIOR_POLARITY_FEATURE_PREFIX + "VB_Imagery";
			featuresPriorPolarity[6] = PRIOR_POLARITY_FEATURE_PREFIX + "JJ_Pleasantness";
			featuresPriorPolarity[7] = PRIOR_POLARITY_FEATURE_PREFIX + "JJ_Activation";
			featuresPriorPolarity[8] = PRIOR_POLARITY_FEATURE_PREFIX + "JJ_Imagery";
			featuresPriorPolarity[9] = PRIOR_POLARITY_FEATURE_PREFIX + "RB_Pleasantness";
			featuresPriorPolarity[10] = PRIOR_POLARITY_FEATURE_PREFIX + "RB_Activation";
			featuresPriorPolarity[11] = PRIOR_POLARITY_FEATURE_PREFIX + "RB_Imagery";
		} else {
			featuresPriorPolarity = new String[0];
		}
		
		features = new String[featuresFrame.length + featuresNGram.length + featuresPriorPolarity.length];
		int idx = 0;
		for (String f : featuresFrame) {
			features[idx++] = f;
		}
		for (String f : featuresNGram) {
			features[idx++] = f;
		}
		for (String f : featuresPriorPolarity) {
			features[idx++] = f;
		}
		
		return features;
	}
	
	public LinkedHashMap<Integer, Object> getDocumentDataSrl(String filename) {
		LinkedHashMap<Integer, Object> data = new LinkedHashMap<Integer, Object>();
		TreeNode rootTreeNode = new TreeNode("TARGET_OBJECT", new ArrayList<TreeNode>());
		
		HashMap<String, Double> attributeFreq = new HashMap<String, Double>();
		
		if (this.useTof || this.useFrame) {
			String content = Tools.read(filename);
			Matcher m = Pattern.compile(SRL_ANNOTATION_PATTERN, Pattern.MULTILINE | Pattern.DOTALL).matcher(content);
			while (m.find()) {
				//System.out.println(m.group());
				String frame = FRAME_FEATURE_PREFIX + m.group(1);
				String frameTarget = frame + "-" + m.group(2).toLowerCase();
				ArrayList<String> frameElements = new ArrayList<String>();
				ArrayList<String> frameElementChunks = new ArrayList<String>();
				Matcher mFE = Pattern.compile(SRL_ANNOTATION_FE_PATTERN, Pattern.MULTILINE | Pattern.DOTALL).matcher(m.group(3));
				while (mFE.find()) {
					frameElements.add(frame + "-" + mFE.group(1));
					frameElementChunks.add(mFE.group(2));
				}
				
				if (useFrame) {
					if (attributeFreq.containsKey(frame)) {
						attributeFreq.put(frame, attributeFreq.get(frame) + 1);
					} else {
						attributeFreq.put(frame, 1.0);
					}
				}
				
				if (useFrameTarget) {
					if (attributeFreq.containsKey(frameTarget)) {
						attributeFreq.put(frameTarget, attributeFreq.get(frameTarget) + 1);
					} else {
						attributeFreq.put(frameTarget, 1.0);
					}
				}
				
				if (useFrameElement) {
					for (String fe : frameElements) {
						if (attributeFreq.containsKey(fe)) {
							attributeFreq.put(fe, attributeFreq.get(fe) + 1);
						} else {
							attributeFreq.put(fe, 1.0);
						}
					}
				}
				
				if (this.useTof && this.useFrame) {
					TreeNode frameTN = null;
					if (this.attributeIndexMap.containsKey(frame)) {
						String frameRaw = frame.replaceAll(FRAME_FEATURE_PREFIX, "");
						frameTN = new TreeNode(frameRaw, new ArrayList<TreeNode>());
						
						// children is null for leaf node
						if (this.attributeIndexMap.containsKey(frameTarget)) {
							String frameTargetRaw = frameTarget.replaceAll(frame + "\\-", "");
							TreeNode targetLeafTN = new TreeNode(frameTargetRaw, null);
							TreeNode targetTN = new TreeNode("Target", new ArrayList<TreeNode>());
							targetTN.getChildren().add(targetLeafTN);
							
							frameTN.getChildren().add(targetTN);
						}
						
						for (int i = 0; i < frameElements.size(); i++) {
							//TreeNode feLeafTN = new TreeNode(frameElementChunks.get(i), null);
							// children is empty but not null, for non-leaf node
							if (this.attributeIndexMap.containsKey(frameElements.get(i))) {
								String feRaw = frameElements.get(i).replaceAll(frame + "\\-", "");
								TreeNode feTN = new TreeNode(feRaw, new ArrayList<TreeNode>());
								
								frameTN.getChildren().add(feTN);
							}
						}
					}
					
					// find root
					for (int i = 0; i < frameElements.size(); i++) {
						if (this.attributeIndexMap.containsKey(frameElements.get(i))) {
							String chunk = frameElementChunks.get(i);
							String targetObject = filename.replaceAll(".*_(.*?_SP500_GICS\\d+_COMPANY).*", "$1");
							//System.out.println(targetObject);
							if (chunk.contains(targetObject)) {
								String feRaw = frameElements.get(i).replaceAll(frame + "\\-", "");
								TreeNode feTN = new TreeNode(feRaw, new ArrayList<TreeNode>());
								if (frameTN != null) {
									feTN.getChildren().add(frameTN);
								}
								
								rootTreeNode.getChildren().add(feTN);
							}
						}
					}
				}
			}
			
			if (this.useTof) {
				data.put(-1, rootTreeNode.toString());
			}
		}
		
		if (this.usePriorPolarity) {
			String origFilename = this.corpusDir + "/" + filename.replaceAll(".*/", "");
			LinkedList<String[]> wordPosList = PosTagger.extractWordPos(Tools.read(origFilename));
			double allPleasantness = 0.0;
			double allActivation = 0.0;
			double allImagery = 0.0;
			double vbPleasantness = 0.0;
			double vbActivation = 0.0;
			double vbImagery = 0.0;
			double jjPleasantness = 0.0;
			double jjActivation = 0.0;
			double jjImagery = 0.0;
			double rbPleasantness = 0.0;
			double rbActivation = 0.0;
			double rbImagery = 0.0;
			
			int allCount = 0;
			int vbCount = 0;
			int jjCount = 0;
			int rbCount = 0;
			for (String[] wordPos : wordPosList) {
				String word = wordPos[0].toLowerCase();
				String pos = wordPos[1];
				if (this.wordDalMap.containsKey(word)) {
					Double[] pai = this.wordDalMap.get(word);	// pleasantness, activation, imagery
					
					allPleasantness += pai[0];
					allActivation += pai[1];
					allImagery += pai[2];
					allCount++;
					
					if (pos.startsWith("VB")) {
						vbPleasantness += pai[0];
						vbActivation += pai[1];
						vbImagery += pai[2];
						vbCount++;
					}
					
					if (pos.startsWith("JJ")) {
						jjPleasantness += pai[0];
						jjActivation += pai[1];
						jjImagery += pai[2];
						jjCount++;
					}
					
					if (pos.startsWith("RB")) {
						rbPleasantness += pai[0];
						rbActivation += pai[1];
						rbImagery += pai[2];
						rbCount++;
					}
				}
			}
			
			if (allCount != 0) {
				allPleasantness /= allCount;
				allActivation /= allCount;
				allImagery /= allCount;
			}
			
			if (vbCount != 0) {
				vbPleasantness /= vbCount;
				vbActivation /= vbCount;
				vbImagery /= vbCount;
			}
			
			if (jjCount != 0) {
				jjPleasantness /= jjCount;
				jjActivation /= jjCount;
				jjImagery /= jjCount;
			}
			
			if (rbCount != 0) {
				rbPleasantness /= rbCount;
				rbActivation /= rbCount;
				rbImagery /= rbCount;
			}
			
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "All_Pleasantness", allPleasantness);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "All_Activation", allActivation);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "All_Imagery", allImagery);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "VB_Pleasantness", vbPleasantness);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "VB_Activation", vbActivation);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "VB_Imagery", vbImagery);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "JJ_Pleasantness", jjPleasantness);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "JJ_Activation", jjActivation);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "JJ_Imagery", jjImagery);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "RB_Pleasantness", rbPleasantness);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "RB_Activation", rbActivation);
			attributeFreq.put(PRIOR_POLARITY_FEATURE_PREFIX + "RB_Imagery", rbImagery);
		}
		
		if (this.useBow) {
			String origFilename = this.corpusDir + "/" + filename.replaceAll(".*/", "");
			TreeMap<String, Integer> dataUnigram = getDocumentFrequentUnigrams(origFilename, attributeIndexMap.keySet());
			TreeMap<String, Integer> dataBigram = getDocumentFrequentBigrams(origFilename, attributeIndexMap.keySet());
			TreeMap<String, Integer> dataTrigram = getDocumentFrequentTrigrams(origFilename, attributeIndexMap.keySet());
			
			HashMap<String, Integer> termFreqMap = new HashMap<String, Integer>();
			termFreqMap.putAll(dataUnigram);
			termFreqMap.putAll(dataBigram);
			termFreqMap.putAll(dataTrigram);
			
			for (String term : termFreqMap.keySet()) {
				attributeFreq.put(term, 1.0 * termFreqMap.get(term));
			}
			
		}
		
		for (Integer idx : indexAttributeMap.keySet()) {
			String attribute = indexAttributeMap.get(idx);
			if (this.useTfidf && this.attributeIdfMap != null) {
				if (attributeFreq.containsKey(attribute)) {
					double tfidf = attributeFreq.get(attribute) * this.attributeIdfMap.get(attribute);
					if (tfidf != 0.0) {
						data.put(idx, tfidf);
					}
				}
			} else {
				if (attributeFreq.containsKey(attribute) && attributeFreq.get(attribute) != 0) {
					data.put(idx, attributeFreq.get(attribute));
				}
			}
		}
		
		return data;
	}
	
	
	public void build() {
		// if output dir doesn't exist, create it
		if (!(new File(this.outDir).exists())) {
			Tools.createDir(outDir);
		}
		
		// load doc list, only these docs will be used
		if (this.semDocList == null) {
			loadSemDocList();
		}
		if ((this.useBow || this.usePriorPolarity) && this.docList == null) {
			loadDocList();
		}
		
		if (this.usePriorPolarity) {
			setWordDalMap();
		}
		
		// load document labels
		if (this.docLabelMap == null || this.labelSet == null) {
			loadDocLabelMap();
		}
		
		if (attributesFilename == null) {
			String[] attributes = preSelectFeatures();
			
			setAttributeIndexMap(attributes);
			setIndexAttributeMap(attributes);
		} else {
			String[] attributes = Tools.read(attributesFilename).split("\n");
			setAttributeIndexMap(attributes);
			setIndexAttributeMap(attributes);
		}
		
		StringBuilder sbAttributes = new StringBuilder();
		for (Integer index : this.indexAttributeMap.keySet()) {
			sbAttributes.append(this.indexAttributeMap.get(index) + "\n");
		}
		this.attributesFilename = outDir + "/" + name + "_attributes.txt";
		Tools.write(this.attributesFilename, sbAttributes.toString());
		
		// this step should be done after we find the attributes
		if (this.useTfidf && this.attributeIdfMap == null) {
			this.setAttributeIdfMap();
		}
		
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
			sbArffHeader.append("@ATTRIBUTE '" + indexAttributeMap.get(index).replaceAll("\\'", "\\\\'") + "' NUMERIC\n");
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
		
		for (String filename : this.semDocList) {
			StringBuilder sbData = new StringBuilder();
			StringBuilder sbDat = new StringBuilder();
			StringBuilder sbArffDat = new StringBuilder();
			
			System.out.println(filename);
			Tools.append(outDocFilename, filename + "\n");
			
			//LinkedHashMap<Integer, Object> data = getDocumentData(filename, wordRegex);
			LinkedHashMap<Integer, Object> data = getDocumentDataSrl(filename);
			
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
			
			if (this.useTof) {
				String treeString = (String) data.get(-1);
				data.remove(-1);
				
				sbData.append(" |BT|");
				sbData.append(treeString);
				sbData.append(" |ET|");
				
				sbData.append(" |BV|");
			}
			
			DecimalFormat df = new DecimalFormat("#.###");
	        for (Integer idx : data.keySet()) {
				sbData.append(" " + (idx + 1) + ":" + df.format(data.get(idx)));
			}
			
			if (this.useTof) {
				sbData.append(" |EV|");
			}
			
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			// to compensate for the bug of SVM Light
			if (data.size() == 0 && !this.useTof) {
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
			
			Tools.append(outDataFilename, sbData.toString());
			Tools.append(outDatFilename, sbDat.toString());
			Tools.append(outArffFilename, sbArffDat.toString());
		}
	}

	public boolean isUseFrame() {
		return useFrame;
	}

	public void setUseFrame(boolean useFrame) {
		this.useFrame = useFrame;
	}

	public boolean isUseFrameTarget() {
		return useFrameTarget;
	}

	public void setUseFrameTarget(boolean useFrameTarget) {
		this.useFrameTarget = useFrameTarget;
	}

	public boolean isUseFrameElement() {
		return useFrameElement;
	}

	public void setUseFrameElement(boolean useFrameElement) {
		this.useFrameElement = useFrameElement;
	}
	
	public boolean isUseBow() {
		return useBow;
	}

	public void setUseBow(boolean useBow) {
		this.useBow = useBow;
	}
	
	public String getSemCorpusDir() {
		return semCorpusDir;
	}

	public void setSemCorpusDir(String semCorpusDir) {
		this.semCorpusDir = semCorpusDir;
	}

	public boolean isUseBof() {
		return useBof;
	}

	public void setUseBof(boolean useBof) {
		this.useBof = useBof;
	}

	public boolean isUseTof() {
		return useTof;
	}

	public void setUseTof(boolean useTof) {
		this.useTof = useTof;
	}

	public Double getThresholdFrame() {
		return thresholdFrame;
	}

	public void setThresholdFrame(Double thresholdFrame) {
		this.thresholdFrame = thresholdFrame;
	}

	public Double getLimitFrame() {
		return limitFrame;
	}

	public void setLimitFrame(Double limitFrame) {
		this.limitFrame = limitFrame;
	}

	public boolean isUsePriorPolarity() {
		return usePriorPolarity;
	}

	public void setUsePriorPolarity(boolean usePriorPolarity) {
		this.usePriorPolarity = usePriorPolarity;
	}

	public String getDalFilename() {
		return dalFilename;
	}

	public void setDalFilename(String dalFilename) {
		this.dalFilename = dalFilename;
	}

	public TreeMap<String, Double[]> getWordDalMap() {
		return wordDalMap;
	}

	public void setWordDalMap(TreeMap<String, Double[]> wordDalMap) {
		this.wordDalMap = wordDalMap;
	}
	
	public void setWordDalMap() {
		this.wordDalMap = new TreeMap<String, Double[]>();
		String[] lines = Tools.read(this.dalFilename).split("\n");
		for (String line : lines) {
			String[] cols = line.split("\\s+");
			if (cols.length != 4) {
				System.out.println("error in dal file.");
				System.exit(0);
			}
			String word = cols[0];
			Double[] dal = new Double[3];
			dal[0] = (Double.parseDouble(cols[1]) - DAL_PLEASANTNESS_MEAN) / DAL_PLEASANTNESS_SD;
			dal[1] = (Double.parseDouble(cols[2]) - DAL_ACTIVATION_MEAN) / DAL_ACTIVATION_SD;
			dal[2] = (Double.parseDouble(cols[3]) - DAL_IMAGERY_MEAN) / DAL_IMAGERY_SD;
			
			this.wordDalMap.put(word, dal);
		}
		
		for (String word : this.wordDalMap.keySet()) {
			Double[] dal = this.wordDalMap.get(word);
			//System.out.print(word + "\t");
			for (double s : dal) {
				//System.out.print(s + "\t");
			}
			//System.out.println();
		}
		System.out.println("load " + wordDalMap.size() + " words.");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String name = null;
		String wordRegex = CorpusAgent.WORD_REGEX;
		String semCorpusDir = null;
		String corpusDir = null;
		String stopwordsFilename = null;
		Double thresholdFrame = Double.MIN_VALUE;
		Double limitFrame = Double.MAX_VALUE;
		Double thresholdUnigram = null;
		Double limitUnigram = Double.MAX_VALUE;
		Double thresholdBigram = null;
		Double limitBigram = Double.MAX_VALUE;
		Double thresholdTrigram = null;
		Double limitTrigram = Double.MAX_VALUE;
		String outDir = null;
		boolean caseSensitive = false;
		String labelFilename = null;
		String attributesFilename = null;
		String docListFilename = null;
		boolean useFrame = true;
		boolean useFrameTarget = false;
		boolean useFrameElement = false;
		boolean useBow = false;
		boolean useBof = true;
		boolean useTof = false;	// tree of frame
		String attributeIdfFilename = null;
		boolean useTfidf = false;
		boolean usePriorPolarity = false;
		String dalFilename = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--name")) {
				name = args[++i];
			} else if (args[i].equals("--corpus_dir")) {
				corpusDir = args[++i];
			} else if (args[i].equals("--sem_corpus_dir")) {
				semCorpusDir = args[++i];
			} else if (args[i].equals("--out_dir")) {
				outDir = args[++i];
			} else if (args[i].equals("--label")) {
				labelFilename = args[++i];
			} else if (args[i].equals("--attributes")) {
				attributesFilename = args[++i];
			} else if (args[i].equals("--docs")) {
				docListFilename = args[++i];
			} else if (args[i].equals("--use_frame")) {
				useFrame = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--use_frame_target")) {
				useFrameTarget = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--use_frame_element")) {
				useFrameElement = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--word_regex")) {
				wordRegex = args[++i];
			} else if (args[i].equals("--stopwords")) {
				stopwordsFilename = args[++i];
			} else if (args[i].equals("--threshold_frame")) {
				thresholdFrame = Double.parseDouble(args[++i]);
			} else if (args[i].equals("--limit_frame")) {
				limitFrame = Double.parseDouble(args[++i]);
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
			} else if (args[i].equals("--case_sensitive")) {
				caseSensitive = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--use_bow")) {
				useBow = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--use_bof")) {
				useBof = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--use_tof")) {
				useTof = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--idf")) {
				attributeIdfFilename = args[++i];
			} else if (args[i].equals("--use_tfidf")) {
				useTfidf = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--use_prior_polarity")) {
				usePriorPolarity = args[++i].startsWith("y") ? true : false;
			} else if (args[i].equals("--dal")) {
				dalFilename = args[++i];
			}
		}
		
		FrameVectorSpaceModel fvsm = new FrameVectorSpaceModel();
		fvsm.setName(name);
		fvsm.setSemCorpusDir(semCorpusDir);
		fvsm.setOutDir(outDir);
		fvsm.setLabelFilename(labelFilename);
		fvsm.setAttributesFilename(attributesFilename);
		fvsm.setDocListFilename(docListFilename);
		fvsm.setUseFrame(useFrame);
		fvsm.setUseFrameTarget(useFrameTarget);
		fvsm.setUseFrameElement(useFrameElement);
		fvsm.setUseBow(useBow);
		fvsm.setUseBof(useBof);
		fvsm.setUseTof(useTof);
		fvsm.setThresholdFrame(thresholdFrame);
		fvsm.setLimitFrame(limitFrame);
		
		// prior polarity
		fvsm.setUsePriorPolarity(usePriorPolarity);
		fvsm.setDalFilename(dalFilename);
		
		// for super class
		fvsm.setCorpusDir(corpusDir);
		fvsm.setWordRegex(wordRegex);
		fvsm.setStopwords(stopwordsFilename);
		fvsm.setThresholdUnigram(thresholdUnigram);
		fvsm.setLimitUnigram(limitUnigram);
		fvsm.setThresholdBigram(thresholdBigram);
		fvsm.setLimitBigram(limitBigram);
		fvsm.setThresholdTrigram(thresholdTrigram);
		fvsm.setLimitTrigram(limitTrigram);
		fvsm.setCaseSensitive(caseSensitive);
		fvsm.setAttributeIdfFilename(attributeIdfFilename);
		fvsm.setUseTfidf(useTfidf);
		
		
		fvsm.build();
		
		System.out.println("FrameVectorSpaceModel done.");

	}

}

