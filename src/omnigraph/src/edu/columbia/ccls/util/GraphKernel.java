package edu.columbia.ccls.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class GraphKernel {
	
	protected ArrayList<Graph> fragmentList = null;
	protected String[] nodeLabels = null;
	private String[] dataLabels = null;
	private boolean normalization = false;
	
	protected ArrayList<TreeMap<Integer, Double>> multisetIdValueMapList = null;
	
	protected int h = 0;
	protected double[][][] kernels = null;
	protected TreeMap<String, String> multisetIdLabelMap = null;
	protected TreeMap<String, String> multisetLabelIdMap = null;
	protected TreeMap<String, LinkedList<Integer>> multisetLabelDocListMap = null;
	protected TreeMap<String, Integer> multisetIdStepMap = null;
	
	protected boolean DEBUG = false;
	
	public GraphKernel(ArrayList<Graph> fragmentList) {
		this.fragmentList = fragmentList;
	}
	
	public GraphKernel(ArrayList<Graph> fragmentList, String[] nodeLabels) {
		this.fragmentList = fragmentList;
		this.nodeLabels = nodeLabels;
	}
	
	public GraphKernel(ArrayList<Graph> fragmentList, String[] nodeLabels, String[] dataLabels) {
		this.fragmentList = fragmentList;
		this.nodeLabels = nodeLabels;
		this.dataLabels = dataLabels;
	}
	
	public int getH() {
		return h;
	}

	public void setH(int h) {
		this.h = h;
	}

	public boolean isNormalization() {
		return normalization;
	}
	
	public void setNormalization(boolean normalization) {
		this.normalization = normalization;
	}
	
	protected void normalize() {
		for (int p = 0; p < kernels.length; p++) {
			for (int i = 0; i < kernels[p].length; i++) {
				for (int j = 0; j < kernels[p][i].length; j++) {
					if (i != j) {
						double bigger = kernels[p][i][i] > kernels[p][j][j] ? kernels[p][i][i] : kernels[p][j][j];
						kernels[p][i][j] = bigger != 0 ? kernels[p][i][j] / bigger : 0;
					}
				}
			}
			for (int i = 0; i < kernels[p].length; i++) {
				kernels[p][i][i] = 1;
			}
		}
	}

	private void computeWLKernel() {
		kernels = new double[h + 1][fragmentList.size()][fragmentList.size()];
		multisetIdLabelMap = new TreeMap<String, String>();
		multisetLabelIdMap = new TreeMap<String, String>();
		multisetLabelDocListMap = new TreeMap<String, LinkedList<Integer>>();
		multisetIdStepMap = new TreeMap<String, Integer>();
		
		// for vector space
		ArrayList<TreeMap<String, Double>> multisetLabelValueMapList = new ArrayList<TreeMap<String,Double>>();
		for (int idx = 0; idx < fragmentList.size(); idx++) {
			multisetLabelValueMapList.add(idx, new TreeMap<String, Double>());
		}
		
		for (int step = 0; step <= h; step++) {
			if (DEBUG) {
				System.out.println("step = " + step);
			}
			System.out.println("step = " + step);
			
			double[][] kernel = new double[fragmentList.size()][fragmentList.size()];
			TreeSet<String> multisetLabelSet = new TreeSet<String>();
			
			// for each node in G_1, ..., G_N
			if (DEBUG) {
				System.out.println("for each node in G_1, ..., G_N");
			}
			for (int idx = 0; idx < fragmentList.size(); idx++) {
				Graph fragment = fragmentList.get(idx);
				
				ArrayList<String> attributesNew = new ArrayList<String>();
				ArrayList<String> attributes = fragment.getAttributes();
				ArrayList<ArrayList<Integer>> graph = fragment.getGraph();
				// for each node in a G
				if (DEBUG) {
					System.out.println("for each node in a G");
				}
				for (int i = 0; i < graph.size(); i++) {
					// multiset-label determination
					if (DEBUG) {
						System.out.println("multiset-label determination");
					}
					LinkedList<String> multiset = new LinkedList<String>();
					if (step == 0) {
						// do nothing for neighbors
					} else {
						// create multiset by finding neighbors
						for (Integer j : graph.get(i)) {
							multiset.add(attributes.get(j));
//							if (attributes.get(j).equals("546")) {
//								System.out.println("here");
//							}
						}
					}
					
					// sort multiset
					Collections.sort(multiset);
					
					// create multiset-label
					if (DEBUG) {
						System.out.println("create multiset-label");
					}
					String multisetLabel = null;
					if (multiset.isEmpty()) {
						multisetLabel = attributes.get(i);
					} else  {
						StringBuilder sb = new StringBuilder();
						sb.append(attributes.get(i));
						for (String s : multiset) {
							sb.append("," + s);
						}
						multisetLabel = sb.toString();
						multisetLabel = multisetLabel.replaceFirst(",", "-");
//						if (multisetLabel.equals("1091-546")) {
//							System.out.println("here");
//						}
					}
					//String multisetLabel = multiset.isEmpty() ? attributes.get(i) : attributes.get(i) + "-" + multiset;
					if (DEBUG) {
						System.out.println("multisetLabel: " + multisetLabel);
					}
					
					// count the how many times this feature occurs (for all steps)
					if (!multisetLabelDocListMap.containsKey(multisetLabel)) {
						multisetLabelDocListMap.put(multisetLabel, new LinkedList<Integer>());
					}
					multisetLabelDocListMap.get(multisetLabel).add(idx);
					
					// for vector space
					TreeMap<String, Double> multisetLabelValueMap = multisetLabelValueMapList.get(idx);
					if (!multisetLabelValueMap.containsKey(multisetLabel)) {
						multisetLabelValueMap.put(multisetLabel, 1.0);
					} else {
						multisetLabelValueMap.put(multisetLabel, multisetLabelValueMap.get(multisetLabel) + 1.0);
					}
					
					attributesNew.add(i, multisetLabel);
					multisetLabelSet.add(multisetLabel);
				}
				fragment.setAttributes(attributesNew);
			}
			
			// label compression
			if (DEBUG) {
				System.out.println("label compression");
			}
			for (String multisetLabel : multisetLabelSet) {
				if (multisetLabelIdMap.containsKey(multisetLabel)) {
					// in the new round, a node isn't expanded (this often happens for directed graph)
					continue;
				}
				String multisetId = String.valueOf(multisetIdLabelMap.size());
				multisetLabelIdMap.put(multisetLabel, multisetId);
				multisetIdLabelMap.put(multisetId, multisetLabel);
				multisetIdStepMap.put(multisetId, step);
//				if (multisetId.equals("1950")) {
//					System.out.println("here");
//				}
			}
			if (DEBUG) {
				System.out.println("multisetLabelIdMap: " + multisetLabelIdMap);
				System.out.println("multisetIdLabelMap: " + multisetIdLabelMap);
			}
			
			// relabeling
			if (DEBUG) {
				System.out.println("relabeling");
			}
			for (Graph fragment : fragmentList) {
				ArrayList<String> attributes = fragment.getAttributes();
				for (int i = 0; i < attributes.size(); i++) {
					attributes.set(i, multisetLabelIdMap.get(attributes.get(i)));
				}
				if (DEBUG) {
					System.out.println(fragment.toStringOneLine());
				}
			}
			
			// calculate kernel
			ArrayList<TreeMap<String, Integer>> attributeValueMaps = new ArrayList<TreeMap<String, Integer>>();
			for (int i = 0; i < fragmentList.size(); i++) {
				TreeMap<String, Integer> a = new TreeMap<String, Integer>();
				// at this moment, attribute is already the multisetId
				for (String attribute : fragmentList.get(i).getAttributes()) {
					if (!a.containsKey(attribute)) {
						a.put(attribute, 1);
					} else {
						a.put(attribute, a.get(attribute) + 1);
					}
				}
				attributeValueMaps.add(i, a);
			}
			for (int i = 0; i < fragmentList.size(); i++) {
				System.out.print(i + " ");
				for (int j = 0; j < i; j++) {
					kernel[i][j] = kernel[j][i];
				}
				for (int j = i; j < fragmentList.size(); j++) {
					// at this moment, attribute is already the multisetId
					double d = dotProduct(attributeValueMaps.get(i), attributeValueMaps.get(j));
					if (d != 0) {
						kernel[i][j] = d;
					}
				}
			}
			System.out.println();
			if (DEBUG) {
				System.out.println("kernel:");
				for (int i = 0; i < kernel.length; i++) {
					for (int j = 0; j < kernel[0].length; j++) {
						System.out.print(kernel[i][j] + "\t");
					}
					System.out.println();
				}
				System.out.println("end kernel");
			}
			
			if (step == 0) {
				for (int i = 0; i < kernel.length; i++) {
					for (int j = 0; j < kernel[0].length; j++) {
						kernels[step][i][j] = kernel[i][j];
					}
				}
			} else if (step > 0) {
				for (int i = 0; i < kernel.length; i++) {
					for (int j = 0; j < kernel[0].length; j++) {
						kernels[step][i][j] = kernels[step - 1][i][j] + kernel[i][j];
					}
				}
			}
			if (DEBUG) {
				System.out.println("kernel:");
				for (int i = 0; i < kernels[step].length; i++) {
					for (int j = 0; j < kernels[step][0].length; j++) {
						System.out.print(kernels[step][i][j] + "\t");
					}
					System.out.println();
				}
				System.out.println("end kernel");
			}
		}
		
		for (String multisetLabel : multisetLabelDocListMap.keySet()) {
			LinkedList<Integer> docList = multisetLabelDocListMap.get(multisetLabel);
			Collections.sort(docList);
		}
		System.out.println("multisetLabelFreqMap.size(): " + multisetLabelDocListMap.size());
		
		
		multisetIdValueMapList = new ArrayList<TreeMap<Integer,Double>>();
		for (int idx = 0; idx < multisetLabelValueMapList.size(); idx++) {
			TreeMap<String, Double> multisetLabelValueMap = multisetLabelValueMapList.get(idx);
			TreeMap<Integer, Double> multisetIdValueMap = new TreeMap<Integer, Double>();
			for (String multisetLabel : multisetLabelValueMap.keySet()) {
				double value = multisetLabelValueMap.get(multisetLabel);
				int multisetId = Integer.parseInt(multisetLabelIdMap.get(multisetLabel));
				multisetIdValueMap.put(multisetId, value);
			}
			multisetIdValueMapList.add(idx, multisetIdValueMap);
		}
	}
	
	private double entropy(String[] a) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (String s : a) {
			if (!map.containsKey(s)) {
				map.put(s, 1);
			} else {
				map.put(s, map.get(s) + 1);
			}
		}
		
		return entropy(map);
	}
	
	private double entropy(HashMap<String, Integer> map) {
		double h = 0.0;
		
		int sum = 0;
		for (String s : map.keySet()) {
			sum += map.get(s);
		}
		
		for (String s : map.keySet()) {
			double p = 1.0 * map.get(s) / sum;
			h -=  p * log(p);
		}
		
		return h;
	}
	
	private double log(double a) {
		return Math.log(a) / Math.log(2);
	}
	
	protected void writeMultisetLabelDocListMap(String multisetLabelFilename) {
		System.out.print("writing multiset label freq map... ");
		
		HashMap<String, Double> multisetLabelInfoGainMap = new HashMap<String, Double>();
		HashMap<String, Double> multisetLabelGainRatioMap = new HashMap<String, Double>();
		HashMap<String, Double> multisetLabelMutualInfoMap = new HashMap<String, Double>();
		
		HashSet<Integer> negDocSet = new HashSet<Integer>();
		HashSet<Integer> posDocSet = new HashSet<Integer>();
		for (int i = 0; i < dataLabels.length; i++) {
			if (dataLabels[i].equals("-1")) {
				negDocSet.add(i);
			} else {
				posDocSet.add(i);
			}
		}
		double propNeg = 1.0 * negDocSet.size() / dataLabels.length;
		double propPos = 1.0 * posDocSet.size() / dataLabels.length;
		
		double h = entropy(dataLabels);
		for (String multisetLabel : multisetLabelDocListMap.keySet()) {
			LinkedList<Integer> docList = multisetLabelDocListMap.get(multisetLabel);
			HashSet<Integer> docSet = new HashSet<Integer>(docList);
			String[] occurs = new String[docSet.size()];
			String[] notOccurs = new String[dataLabels.length - occurs.length];
			int i = 0;
			int j = 0;
			for (int idx = 0; idx < dataLabels.length; idx++) {
				if (docSet.contains(idx)) {
					occurs[i++] = dataLabels[idx];
				} else {
					notOccurs[j++] = dataLabels[idx];
				}
			}
			double hOccur = entropy(occurs);
			double hNotOccur = entropy(notOccurs);
			double propOccur = 1.0 * occurs.length / dataLabels.length;
			double propNotOccur = 1.0 * notOccurs.length / dataLabels.length;
			double infoGain = h - (propOccur * hOccur + propNotOccur * hNotOccur);
			multisetLabelInfoGainMap.put(multisetLabel, infoGain);
			
			double intrinsicValue = - (propOccur * log(propOccur) + propNotOccur * log(propNotOccur));
			double gainRatio = infoGain / intrinsicValue;
			multisetLabelGainRatioMap.put(multisetLabel, gainRatio);
			
			HashSet<Integer> posOccurSet = (HashSet<Integer>)docSet.clone();
			posOccurSet.retainAll(posDocSet);
			double propPosOccur = 1.0 * posOccurSet.size() / dataLabels.length;
			HashSet<Integer> negOccurSet = (HashSet<Integer>)docSet.clone();
			negOccurSet.retainAll(negDocSet);
			double propNegOccur = 1.0 * negOccurSet.size() / dataLabels.length;
			HashSet<Integer> posNotOccurSet = (HashSet<Integer>)posDocSet.clone();
			posNotOccurSet.removeAll(posOccurSet);
			double propPosNotOccur = 1.0 * posNotOccurSet.size() / dataLabels.length;
			HashSet<Integer> negNotOccurSet = (HashSet<Integer>)negDocSet.clone();
			negNotOccurSet.removeAll(negOccurSet);
			double propNegNotOccur = 1.0 * negNotOccurSet.size() / dataLabels.length;
			double mutualInfo = 0.0;
			mutualInfo += propNegNotOccur == 0 ? 0 : propNegNotOccur * Math.log(propNegNotOccur / propNotOccur / propNeg);
			mutualInfo += propPosNotOccur == 0 ? 0 : propPosNotOccur * Math.log(propPosNotOccur / propNotOccur / propPos);
			mutualInfo += propNegOccur == 0 ? 0 : propNegOccur * Math.log(propNegOccur / propOccur / propNeg);
			mutualInfo += propPosOccur == 0 ? 0 : propPosOccur * Math.log(propPosOccur / propOccur / propPos);
			multisetLabelMutualInfoMap.put(multisetLabel, mutualInfo);
		}
		
		HashMap<String, Integer> multisetLabelFreqMap = new HashMap<String, Integer>();
		HashMap<String, HashMap<String, Integer>> multisetLabelLabelFreqMapMap = new HashMap<String, HashMap<String,Integer>>();
		for (String multisetLabel : multisetLabelDocListMap.keySet()) {
			LinkedList<Integer> docList = multisetLabelDocListMap.get(multisetLabel);
			multisetLabelFreqMap.put(multisetLabel, docList.size());
			HashSet<Integer> docSet = new HashSet<Integer>(docList);
			HashMap<String, Integer> labelFreqMap = new HashMap<String, Integer>();
			for (Integer idx : docSet) {
				String label = this.dataLabels[idx];
				if (!labelFreqMap.containsKey(label)) {
					labelFreqMap.put(label, 1);
				} else {
					labelFreqMap.put(label, labelFreqMap.get(label) + 1);
				}
			}
			multisetLabelLabelFreqMapMap.put(multisetLabel, labelFreqMap);
		}
		
		StringBuilder sbMultisetLabel = new StringBuilder();
		int count = 0;
		int limit = 10000;
		
		// sort by label frequency
		//LinkedHashMap<String, Integer> multisetLabelSorted = Tools.sortMapByValue(multisetLabelFreqMap);
		// sort by info gain
		//LinkedHashMap<String, Double> multisetLabelSorted = Tools.sortMapByValue(multisetLabelInfoGainMap);
		// sort by gain ratio
		//LinkedHashMap<String, Double> multisetLabelSorted = Tools.sortMapByValue(multisetLabelGainRatioMap);
		// sort by mutual info
		LinkedHashMap<String, Double> multisetLabelSorted = Tools.sortMapByValue(multisetLabelMutualInfoMap);
		
		DecimalFormat df = new DecimalFormat("#.######");
		sbMultisetLabel.append("id\tfeature\tnum_inst\tlabel_dist\tmutual_info\tinfo_gain\tgain_ratio\tfreq\tinst_list\n");
		for (String multisetLabel : multisetLabelSorted.keySet()) {
			String multisetId = multisetLabelIdMap.get(multisetLabel);
			HashSet<Integer> docSet = new HashSet<Integer>(multisetLabelDocListMap.get(multisetLabel));
//			if (multisetId.equals("61289")) {
//				System.out.println("here");
//			}
			//sbMultisetLabel.append(multisetId + "\t" + recoverNodeLabel(multisetId) + "\t" + docSet.size() + "\t" + multisetLabelLabelFreqMapMap.get(multisetLabel) + "\t" + multisetLabelInfoGainMap.get(multisetLabel) + "\t" + multisetLabelGainRatioMap.get(multisetLabel) + "\t" + multisetLabelFreqMap.get(multisetLabel) + "\t" + multisetLabelDocListMap.get(multisetLabel) + "\n");
			//sbMultisetLabel.append(multisetId + "\t" + recoverNodeLabel(multisetId, this.h) + "\t" + docSet.size() + "\t" + multisetLabelLabelFreqMapMap.get(multisetLabel) + "\t" + multisetLabelInfoGainMap.get(multisetLabel) + "\t" + multisetLabelGainRatioMap.get(multisetLabel) + "\t" + multisetLabelFreqMap.get(multisetLabel) + "\t" + multisetLabelDocListMap.get(multisetLabel) + "\n");
			TreeMap<Integer, Integer> docIdFreqMap = new TreeMap<Integer, Integer>();
			for (Integer docId : multisetLabelDocListMap.get(multisetLabel)) {
				if (docIdFreqMap.containsKey(docId)) {
					docIdFreqMap.put(docId, docIdFreqMap.get(docId) + 1);
				} else {
					docIdFreqMap.put(docId, 1);
				}
			}
			
			sbMultisetLabel.append(multisetId + "\t" + recoverNodeLabel(multisetId, this.multisetIdStepMap.get(multisetId)) + "\t" + docSet.size() + "\t" + multisetLabelLabelFreqMapMap.get(multisetLabel) + "\t" + df.format(multisetLabelMutualInfoMap.get(multisetLabel)) + "\t" + df.format(multisetLabelInfoGainMap.get(multisetLabel)) + "\t" + df.format(multisetLabelGainRatioMap.get(multisetLabel)) + "\t" + multisetLabelFreqMap.get(multisetLabel) + "\t" + docIdFreqMap.keySet() + "\n");
			if (++count >= limit) {
				break;
			}
		}
		Tools.write(multisetLabelFilename, sbMultisetLabel.toString());
		System.out.println("done");
	}
	
	protected void writeArffFormat(String arffFilename) {
		if (nodeLabels == null) {
//			System.out.println("Not initialized!");
			return;
		}
		
		System.out.print("writing arff... ");
		
		StringBuilder sbArff = new StringBuilder();
		sbArff.append("@RELATION arff\n\n");
		
		LinkedList<Integer> ids = new LinkedList<Integer>();
		for (String multisetId : multisetIdLabelMap.keySet()) {
			ids.add(Integer.parseInt(multisetId));
		}
		Collections.sort(ids);
		for (int id : ids) {
			String multisetId = String.valueOf(id);
			//String nodeLabel = recoverNodeLabel(multisetId);
			String nodeLabel = recoverNodeLabel(multisetId, this.multisetIdStepMap.get(multisetId));
			
			sbArff.append("@ATTRIBUTE '" + nodeLabel.replaceAll("\\'", "\\\\'") + "' NUMERIC\n");
		}
		sbArff.append("@ATTRIBUTE LABELOFINSTANCE {1,-1}\n");
		
		sbArff.append("\n");
		sbArff.append("@DATA\n");
		
		for (int idx = 0; idx < multisetIdValueMapList.size(); idx++) {
			sbArff.append("{");
			TreeMap<Integer, Double> multisetIdValueMap = multisetIdValueMapList.get(idx);
			for (int id : multisetIdValueMap.keySet()) {
				sbArff.append(id + " " + multisetIdValueMap.get(id) + ",");
			}
			sbArff.append(multisetLabelDocListMap.size() + " " + dataLabels[idx] + "}\n");
		}
		Tools.write(arffFilename, sbArff.toString());
		System.out.println("done");
	}
	
	protected void writeSvmLightFormat(String svmlightFilename, String attributeFilename) {
		if (nodeLabels == null) {
//			System.out.println("Not initialized!");
			return;
		}
		
		System.out.print("writing svmlight... ");
		
		StringBuilder sbMultisetLabel = new StringBuilder();
		LinkedList<Integer> ids = new LinkedList<Integer>();
		for (String multisetId : multisetIdLabelMap.keySet()) {
			ids.add(Integer.parseInt(multisetId));
		}
		Collections.sort(ids);
		for (int id : ids) {
			String multisetId = String.valueOf(id);
			//String nodeLabel = recoverNodeLabel(multisetId);
			String nodeLabel = recoverNodeLabel(multisetId, this.multisetIdStepMap.get(multisetId));
			sbMultisetLabel.append(nodeLabel + "\n");
		}
		
		Tools.write(attributeFilename, sbMultisetLabel.toString());
		
		StringBuilder sbVector = new StringBuilder();
		for (int idx = 0; idx < multisetIdValueMapList.size(); idx++) {
			sbVector.append(dataLabels[idx]);
			TreeMap<Integer, Double> multisetIdValueMap = multisetIdValueMapList.get(idx);
			for (int id : multisetIdValueMap.keySet()) {
				sbVector.append(" " + (id + 1) + ":" + multisetIdValueMap.get(id));
			}
			sbVector.append("\n");
		}
		Tools.write(svmlightFilename, sbVector.toString());
		System.out.println("done");
	}
	
	protected void writeLibsvmFormat(String libsvmFilename, int h) {
		if (nodeLabels == null) {
//			System.out.println("Not initialized!");
			return;
		}
		
		System.out.print("writing libsvm... ");
		
		DecimalFormat df = new DecimalFormat("#.##");
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < kernels[h].length; i++) {
			StringBuilder sb = new StringBuilder();
			// the id of the data instance starts with 1 for this format
			sb.append(dataLabels[i] + " 0:" + (i + 1));
			for (int j = 0; j < kernels[h][0].length; j++) {
				sb.append(" " + (j + 1) + ":" + df.format(kernels[h][i][j]));
			}
			list.add(sb.toString());
		}
		
		// write all in once
//		StringBuilder sb = new StringBuilder();
//		for (String s : list) {
//			sb.append(s);
//			sb.append("\n");
//		}
//		Tools.write(libsvmFilename, sb.toString());
		
		// write each instance at a time
		Tools.write(libsvmFilename, "");
		for (String s : list) {
			Tools.append(libsvmFilename, s);
			Tools.append(libsvmFilename, "\n");
		}
		System.out.println("done");
	}
	
//	// Problematic dealing with directed graph
//	private String recoverNodeLabel(String multisetId) {
//		String multisetLabel = multisetIdLabelMap.get(multisetId);
//		if (!multisetLabel.contains("-")) {
//			String nodeLabel = nodeLabels[Integer.parseInt(multisetLabel)];
//			if (nodeLabel.equals(",")) {
//				nodeLabel = "\\,";
//			}
//			return nodeLabel;
//		} else {
//			String label = "";
//			int dashPosition = multisetLabel.indexOf('-');
//			String fromId = multisetLabel.substring(0, dashPosition);
//			String fromLabel = recoverNodeLabel(fromId);
//			label += fromLabel + "->";
//			String[] toIds = multisetLabel.substring(dashPosition + 1).split(",");
//			for (String toId : toIds) {
//				String toLabel = recoverNodeLabel(toId);
//				label += toLabel + ",";
//			}
//			label = label.substring(0, label.length() - 1);
//			return "(" + label + ")";
//		}
//	}
	
	protected String recoverNodeLabel(String multisetId, int h) {
		String multisetLabel = multisetIdLabelMap.get(multisetId);
		if (!multisetLabel.contains("-")) {
			if (h == 0) {
				String nodeLabel = nodeLabels[Integer.parseInt(multisetLabel)];
				if (nodeLabel.equals(",")) {
					nodeLabel = "\\,";
				}
				return nodeLabel;
			} else {
				String label = recoverNodeLabel(multisetLabel, h - 1);
				return "(" + label + ")";
			}
		} else {
			String label = "";
			int dashPosition = multisetLabel.indexOf('-');
			String fromId = multisetLabel.substring(0, dashPosition);
			String fromLabel = recoverNodeLabel(fromId, h - 1);
			label += fromLabel + "->";
			String[] toIds = multisetLabel.substring(dashPosition + 1).split(",");
			for (String toId : toIds) {
				String toLabel = recoverNodeLabel(toId, h - 1);
				label += toLabel + ",";
			}
			label = label.substring(0, label.length() - 1);
			return "(" + label + ")";
		}
	}
	
	protected String recoverNodeLabelOneStep(String multisetId, int h) {
		String multisetLabel = multisetIdLabelMap.get(multisetId);
		if (!multisetLabel.contains("-")) {
			if (h == 0) {
				String nodeLabel = nodeLabels[Integer.parseInt(multisetLabel)];
				if (nodeLabel.equals(",")) {
					nodeLabel = "\\,";
				}
				return nodeLabel;
			} else {
				String label = multisetLabel;
				return "(" + label + ")";
			}
		} else {
			String label = "";
			int dashPosition = multisetLabel.indexOf('-');
			String fromId = multisetLabel.substring(0, dashPosition);
			String fromLabel = fromId;
			label += fromLabel + "->";
			String[] toIds = multisetLabel.substring(dashPosition + 1).split(",");
			for (String toId : toIds) {
				String toLabel = toId;
				label += toLabel + ",";
			}
			label = label.substring(0, label.length() - 1);
			return "(" + label + ")";
		}
	}
	
	protected double dotProduct(Map<String, Integer> a, Map<String, Integer> b) {
		double sum = 0;
//		Set<String> keySet = new HashSet<String>(a.keySet());
//		keySet.retainAll(b.keySet());
//		for (String key : keySet) {
//			sum += a.get(key) * b.get(key);
//		}
		if (a.size() < b.size()) {
			for (String key : a.keySet()) {
				if (b.containsKey(key)) {
					sum += a.get(key) * b.get(key);
				}
			}
		} else {
			for (String key : b.keySet()) {
				if (a.containsKey(key)) {
					sum += a.get(key) * b.get(key);
				}
			}
		}
		
		return sum;
	}
	
	protected static ArrayList<Graph> loadGraphsFromFile(String filename) {
		ArrayList<Graph> fragmentList = new ArrayList<Graph>();
		String[] rows = Tools.read(filename).split("\n");
		for (String row : rows) {
			Graph fragment = new Graph(row);
			fragmentList.add(fragment);
		}
		
		return fragmentList;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String name = null;  // "semgraph_gics45_IBM_forestsent_frame_dep_dir";
		String dir = null; // "/Users/xie/Programs/workspace/nlp4fe/output/fragment_modeler/45";
		int h = 3;
		boolean normalization = false;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--name")) {
				name = args[++i];
			} else if (args[i].equals("--dir")) {
				dir = args[++i];
			} else if (args[i].equals("--h")) {
				h = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--normalization")) {
				++i;
				normalization = args[i].startsWith("y") || args[i].startsWith("Y") ? true : false;
			}
		}
		
		String filename = dir + "/" + name + ".graph";
		//filename = "/Users/xie/Programs/workspace/nlp4fe/output/fragment_modeler/45/semgraph_gics45_YHOO_forestsent_frame_dep_dir.graph";
		String nodeLabelFilename = dir + "/" + name + ".graph.nodeid";
		String docLabelFilename = dir + "/" + name + ".doc";
		String attributeFilename = dir + "/" + name + "_attributes.tsv";
		String svmlightFilename = dir + "/" + name + ".data";
		String arffFilename = dir + "/" + name + ".arff";
		
		String multisetLabelFilename = dir + "/" + name + "_multisetLabelFreqMap.tsv";
		
		String[] nodeLabels = Tools.read(nodeLabelFilename).split("\n");
		String[] docLabels = Tools.read(docLabelFilename).split("\n");
		for (int i = 0; i < docLabels.length; i++) {
			docLabels[i] = docLabels[i].replaceAll(".*,", "");
		}
		
		ArrayList<Graph> fragmentList = loadGraphsFromFile(filename);
		
		GraphKernel gk = new GraphKernel(fragmentList, nodeLabels, docLabels);
		gk.setH(h);
		gk.setNormalization(normalization);
		gk.computeWLKernel();
		if (gk.isNormalization()) {
			gk.normalize();
		}
		gk.writeMultisetLabelDocListMap(multisetLabelFilename);
		gk.writeArffFormat(arffFilename);
		gk.writeSvmLightFormat(svmlightFilename, attributeFilename);
		for (int hh = 0; hh <= h; hh++) {
			String libsvmFilename = dir + "/" + name + "_h" + hh + ".libsvm";
			gk.writeLibsvmFormat(libsvmFilename, hh);
		}
		
		System.out.print("done GraphKernel");
		for (String arg : args) {
			System.out.print(" " + arg);
		}
		System.out.println();

	}

}
