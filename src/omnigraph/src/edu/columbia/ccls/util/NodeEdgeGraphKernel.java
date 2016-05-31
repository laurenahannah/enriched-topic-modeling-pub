package edu.columbia.ccls.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.javatuples.Quartet;

public class NodeEdgeGraphKernel extends GraphKernel {
	
	private TreeMap<String, Double> nodeKernelStringValueMap = new TreeMap<String, Double>();
	private float[][][][][] nodeKernels = null;
	private TreeMap<Integer, HashMap<Quartet<Integer, Integer, Integer, Integer>, Double>> pQuartetKernelMapMap = null;
	
	private int currentP = 0;
	private TreeSet<String> currentMultisetLabelSet = null;
	private StringBuilder currentRStringBuilder = new StringBuilder();
	private StringBuilder currentSStringBuilder = new StringBuilder();
	private String[][] attributes = null;
	private boolean uniformWeights = true;
	private float deNodeWeight = 0.9f;
	private float oeNodeWeight = 0.4f;
	private float fnNodeWeight = 0.8f;
	private float ftNodeWeight = 0.7f;
	private float feNodeWeight = 0.6f;
	private float liNodeWeight = 0.5f;
	private float deFeEdgeWeight = 0.9f;
	private float oeFeEdgeWeight = 0.4f;
	private float fnFnEdgeWeight = 0.8f;
	private float ftFnEdgeWeight = 0.7f;
	private float feFnEdgeWeight = 0.6f;
	private float liFeEdgeWeight = 0.5f;
	private ArrayList<Float> kernelWeightList = null;
	
	public NodeEdgeGraphKernel(ArrayList<Graph> fragmentList) {
		super(fragmentList);
		// TODO Auto-generated constructor stub
	}

	public NodeEdgeGraphKernel(ArrayList<Graph> fragmentList,
			String[] nodeLabels, String[] dataLabels) {
		super(fragmentList, nodeLabels, dataLabels);
		// TODO Auto-generated constructor stub
	}

	public NodeEdgeGraphKernel(ArrayList<Graph> fragmentList,
			String[] nodeLabels) {
		super(fragmentList, nodeLabels);
		// TODO Auto-generated constructor stub
	}
	
	public ArrayList<Float> getKernelWeightList() {
		return kernelWeightList;
	}

	public void setKernelWeightList(ArrayList<Float> kernelWeightList) {
		while (kernelWeightList.size() < h) {
			kernelWeightList.add(0f);
		}
		this.kernelWeightList = kernelWeightList;
	}

	public boolean isUniformWeights() {
		return uniformWeights;
	}

	public void setUniformWeights(boolean uniformWeights) {
		this.uniformWeights = uniformWeights;
	}

	public float getDeNodeWeight() {
		return deNodeWeight;
	}

	public void setDeNodeWeight(float deNodeWeight) {
		this.deNodeWeight = deNodeWeight;
	}

	public float getOeNodeWeight() {
		return oeNodeWeight;
	}

	public void setOeNodeWeight(float oeNodeWeight) {
		this.oeNodeWeight = oeNodeWeight;
	}

	public float getFnNodeWeight() {
		return fnNodeWeight;
	}

	public void setFnNodeWeight(float fnNodeWeight) {
		this.fnNodeWeight = fnNodeWeight;
	}

	public float getFtNodeWeight() {
		return ftNodeWeight;
	}

	public void setFtNodeWeight(float ftNodeWeight) {
		this.ftNodeWeight = ftNodeWeight;
	}

	public float getFeNodeWeight() {
		return feNodeWeight;
	}

	public void setFeNodeWeight(float feNodeWeight) {
		this.feNodeWeight = feNodeWeight;
	}

	public float getLiNodeWeight() {
		return liNodeWeight;
	}

	public void setLiNodeWeight(float liNodeWeight) {
		this.liNodeWeight = liNodeWeight;
	}

	public float getDeFeEdgeWeight() {
		return deFeEdgeWeight;
	}

	public void setDeFeEdgeWeight(float deFeEdgeWeight) {
		this.deFeEdgeWeight = deFeEdgeWeight;
	}

	public float getOeFeEdgeWeight() {
		return oeFeEdgeWeight;
	}

	public void setOeFeEdgeWeight(float oeFeEdgeWeight) {
		this.oeFeEdgeWeight = oeFeEdgeWeight;
	}

	public float getFnFnEdgeWeight() {
		return fnFnEdgeWeight;
	}

	public void setFnFnEdgeWeight(float fnFnEdgeWeight) {
		this.fnFnEdgeWeight = fnFnEdgeWeight;
	}

	public float getFtFnEdgeWeight() {
		return ftFnEdgeWeight;
	}

	public void setFtFnEdgeWeight(float ftFnEdgeWeight) {
		this.ftFnEdgeWeight = ftFnEdgeWeight;
	}

	public float getFeFnEdgeWeight() {
		return feFnEdgeWeight;
	}

	public void setFeFnEdgeWeight(float feFnEdgeWeight) {
		this.feFnEdgeWeight = feFnEdgeWeight;
	}

	public float getLiFeEdgeWeight() {
		return liFeEdgeWeight;
	}

	public void setLiFeEdgeWeight(float liFeEdgeWeight) {
		this.liFeEdgeWeight = liFeEdgeWeight;
	}

	private void computeNodeEdgeGraphKernelByRecursion() {
		kernels = new double[h + 1 + 1][fragmentList.size()][fragmentList.size()];	// the last one is a linear combination of the kernels
		multisetIdLabelMap = new TreeMap<String, String>();
		multisetLabelIdMap = new TreeMap<String, String>();
		multisetLabelDocListMap = new TreeMap<String, LinkedList<Integer>>();	// Note: It's not the true frequency!
		multisetIdStepMap = new TreeMap<String, Integer>();
		attributes = new String[fragmentList.size()][];
		
		// maybe we don't need this!!!!!!!!
		// for vector space
		ArrayList<TreeMap<String, Double>> multisetLabelValueMapList = new ArrayList<TreeMap<String,Double>>();
		for (int idx = 0; idx < fragmentList.size(); idx++) {
			multisetLabelValueMapList.add(idx, new TreeMap<String, Double>());
			attributes[idx] = new String[fragmentList.get(idx).getAttributes().size()];
			fragmentList.get(idx).getAttributes().toArray(attributes[idx]);
		}
		// end
		
		//for (int p = 0; p <= h; p++) {
		for (currentP = 0; currentP <= h; currentP++) {
			currentMultisetLabelSet = new TreeSet<String>();
			System.out.println("step = " + currentP);
			double[][] kernel = new double[fragmentList.size()][fragmentList.size()];
			for (int graphAId = 0; graphAId < fragmentList.size(); graphAId++) {
				System.out.print(graphAId + " ");
				for (int graphBId = 0; graphBId < graphAId; graphBId++) {
					kernel[graphAId][graphBId] = kernel[graphBId][graphAId];
				}
				for (int graphBId = graphAId; graphBId < fragmentList.size(); graphBId++) {
					double krnl = 0.0;
					for (int r = 0; r < fragmentList.get(graphAId).getGraph().size(); r++) {
						for (int s = 0; s < fragmentList.get(graphBId).getGraph().size(); s++) {
							krnl += computeNodeKernelValueRecursion(currentP, graphAId, graphBId, r, s);
						}
					}
					kernel[graphAId][graphBId] = krnl;
				}
			}
			
			for (int i = 0; i < kernel.length; i++) {
				for (int j = 0; j < kernel[0].length; j++) {
					//System.out.println(i + "," + j + ":" + kernel[i][j]);
					kernels[currentP][i][j] = kernel[i][j];
				}
			}
			System.out.println();
			if (DEBUG) {
				System.out.println("currentMultisetLabelSet: " + currentMultisetLabelSet);
			}
			for (String multisetLabel : currentMultisetLabelSet) {
				String multisetId = String.valueOf(multisetIdLabelMap.size());
				multisetLabelIdMap.put(multisetLabel, multisetId);
				multisetIdLabelMap.put(multisetId, multisetLabel);
				multisetIdStepMap.put(multisetId, currentP);
			}
			if (DEBUG) {
				System.out.println("multisetLabelIdMap: " + multisetLabelIdMap);
				System.out.println("multisetIdLabelMap: " + multisetIdLabelMap);
				System.out.println("multisetIdStepMap: " + multisetIdStepMap);
			}
		}
		
		for (int i = 0; i < kernels[0].length; i++) {
			for (int j = 0; j < kernels[0][0].length; j++) {
				kernels[kernels.length - 1][i][j] = 0f;
				for (int p = 0; p < kernelWeightList.size(); p++) {
					kernels[kernels.length - 1][i][j] += kernelWeightList.get(p) * kernels[p][i][j];
				}
			}
		}
	}
	
	private double computeNodeKernelValueRecursion(int p, int graphAId, int graphBId, int r, int s) {
		double nodeKernel = nodeKernel(graphAId, graphBId, r, s);
		if (nodeKernel == 0) {
			return 0;
		}
		
		if (p > 0 && nodeKernel != 0) {
			double sum = 0;
			for (int rNeighbor : fragmentList.get(graphAId).getGraph().get(r)) {
				for (int sNeighbor : fragmentList.get(graphBId).getGraph().get(s)) {
					if (graphAId != graphBId) {
						if (p == currentP) {
							currentRStringBuilder.setLength(0);
							//currentRStringBuilder.append(fragmentList.get(graphAId).getAttributes().get(r));
							currentRStringBuilder.append(attributes[graphAId][r]);
							currentSStringBuilder.setLength(0);
							//currentSStringBuilder.append(fragmentList.get(graphBId).getAttributes().get(s));
							currentSStringBuilder.append(attributes[graphBId][s]);
						}
						//currentRStringBuilder.append("-" + fragmentList.get(graphAId).getAttributes().get(rNeighbor));
						currentRStringBuilder.append("-").append(attributes[graphAId][rNeighbor]);
						//currentSStringBuilder.append("-" + fragmentList.get(graphBId).getAttributes().get(sNeighbor));
						currentSStringBuilder.append("-").append(attributes[graphBId][sNeighbor]);
					}
					double k = edgeKernel(graphAId, graphBId, r, rNeighbor, s, sNeighbor) * computeNodeKernelValueRecursion(p - 1, graphAId, graphBId, rNeighbor, sNeighbor);
					sum += k;
					if (k != 0 && p == 1 && graphAId != graphBId) {
						String rMultisetLabel = currentRStringBuilder.toString();
						if (!multisetLabelDocListMap.containsKey(rMultisetLabel)) {
							multisetLabelDocListMap.put(rMultisetLabel, new LinkedList<Integer>());
						}
						multisetLabelDocListMap.get(rMultisetLabel).add(graphAId);
						currentMultisetLabelSet.add(rMultisetLabel);
						
						String sMultisetLabel = currentSStringBuilder.toString();
						if (!multisetLabelDocListMap.containsKey(sMultisetLabel)) {
							multisetLabelDocListMap.put(sMultisetLabel, new LinkedList<Integer>());
						}
						multisetLabelDocListMap.get(sMultisetLabel).add(graphBId);
						currentMultisetLabelSet.add(sMultisetLabel);
					}
					if (graphAId != graphBId && p != currentP) {
						currentRStringBuilder.setLength(currentRStringBuilder.lastIndexOf("-"));
						currentSStringBuilder.setLength(currentSStringBuilder.lastIndexOf("-"));
					}
				}
			}
			nodeKernel *= sum;
		}
		
		if (currentP == 0 && p == 0 && nodeKernel != 0 && graphAId != graphBId) {
			String rMultisetLabel = fragmentList.get(graphAId).getAttributes().get(r);
			if (!multisetLabelDocListMap.containsKey(rMultisetLabel)) {
				multisetLabelDocListMap.put(rMultisetLabel, new LinkedList<Integer>());
			}
			multisetLabelDocListMap.get(rMultisetLabel).add(graphAId);
			currentMultisetLabelSet.add(rMultisetLabel);
			
			String sMultisetLabel = fragmentList.get(graphBId).getAttributes().get(s);
			if (!multisetLabelDocListMap.containsKey(sMultisetLabel)) {
				multisetLabelDocListMap.put(sMultisetLabel, new LinkedList<Integer>());
			}
			multisetLabelDocListMap.get(sMultisetLabel).add(graphBId);
			currentMultisetLabelSet.add(sMultisetLabel);
		}
		
		return nodeKernel;
	}
	
	private void computeNodeEdgeGraphKernelByQuartet() {
		kernels = new double[h + 1][fragmentList.size()][fragmentList.size()];
		multisetIdLabelMap = new TreeMap<String, String>();
		multisetLabelIdMap = new TreeMap<String, String>();
		multisetLabelDocListMap = new TreeMap<String, LinkedList<Integer>>();
		multisetIdStepMap = new TreeMap<String, Integer>();
		
		// maybe we don't need this!!!!!!!!
		// for vector space
		ArrayList<TreeMap<String, Double>> multisetLabelValueMapList = new ArrayList<TreeMap<String,Double>>();
		for (int idx = 0; idx < fragmentList.size(); idx++) {
			multisetLabelValueMapList.add(idx, new TreeMap<String, Double>());
		}
		// end
		
		pQuartetKernelMapMap = new TreeMap<Integer, HashMap<Quartet<Integer,Integer,Integer,Integer>,Double>>();
		
		for (int p = 0; p <= h; p++) {
			System.out.println("step = " + p);
			HashMap<Quartet<Integer,Integer,Integer,Integer>,Double> quartetHashMap = new HashMap<Quartet<Integer,Integer,Integer,Integer>, Double>();
			for (int graphAId = 0; graphAId < fragmentList.size(); graphAId++) {
				System.out.print(graphAId + " ");
				for (int graphBId = 0; graphBId < fragmentList.size(); graphBId++) {
					for (int r = 0; r < fragmentList.get(graphAId).getGraph().size(); r++) {
						for (int s = 0; s < fragmentList.get(graphBId).getGraph().size(); s++) {
							Quartet<Integer, Integer, Integer, Integer> quartet = new Quartet<Integer, Integer, Integer, Integer>(graphAId, graphBId, r, s);
							double nodeKernel = computeNodeKernelQuartetValue(p, quartet);
							quartetHashMap.put(quartet, nodeKernel);
						}
					}
				}
			}
			pQuartetKernelMapMap.put(p, quartetHashMap);
			System.out.println();
		}
		
		for (int p = 0; p <= h; p++) {
			double[][] kernel = new double[fragmentList.size()][fragmentList.size()];
			for (int graphAId = 0; graphAId < fragmentList.size(); graphAId++) {
				for (int graphBId = 0; graphBId < graphAId; graphBId++) {
					kernel[graphAId][graphBId] = kernel[graphBId][graphAId];
				}
				for (int graphBId = graphAId; graphBId < fragmentList.size(); graphBId++) {
					double krnl = 0.0;
					for (int r = 0; r < fragmentList.get(graphAId).getGraph().size(); r++) {
						for (int s = 0; s < fragmentList.get(graphBId).getGraph().size(); s++) {
							Quartet<Integer, Integer, Integer, Integer> quartet = new Quartet<Integer, Integer, Integer, Integer>(graphAId, graphBId, r, s);
							if (pQuartetKernelMapMap.get(p).containsKey(quartet)) {
								krnl += pQuartetKernelMapMap.get(p).get(quartet);
							}
						}
					}
					kernel[graphAId][graphBId] = krnl;
				}
			}
			
			for (int i = 0; i < kernel.length; i++) {
				for (int j = 0; j < kernel[0].length; j++) {
					kernels[p][i][j] = kernel[i][j];
				}
			}
		}
	}
	
	private void computeNodeEdgeGraphKernelByArray() {
		kernels = new double[h + 1][fragmentList.size()][fragmentList.size()];
		multisetIdLabelMap = new TreeMap<String, String>();
		multisetLabelIdMap = new TreeMap<String, String>();
		multisetLabelDocListMap = new TreeMap<String, LinkedList<Integer>>();
		multisetIdStepMap = new TreeMap<String, Integer>();
		
		// maybe we don't need this!!!!!!!!
		// for vector space
		ArrayList<TreeMap<String, Double>> multisetLabelValueMapList = new ArrayList<TreeMap<String,Double>>();
		for (int idx = 0; idx < fragmentList.size(); idx++) {
			multisetLabelValueMapList.add(idx, new TreeMap<String, Double>());
		}
		// end
		
		nodeKernels = new float[h + 1][fragmentList.size()][fragmentList.size()][][];
		
		for (int p = 0; p <= h; p++) {
			System.out.println("step = " + p);
			for (int graphAId = 0; graphAId < fragmentList.size(); graphAId++) {
				System.out.print(graphAId + " ");
				for (int graphBId = 0; graphBId < fragmentList.size(); graphBId++) {
					nodeKernels[p][graphAId][graphBId] = new float[fragmentList.get(graphAId).getGraph().size()][fragmentList.get(graphBId).getGraph().size()];
					for (int r = 0; r < fragmentList.get(graphAId).getGraph().size(); r++) {
						for (int s = 0; s < fragmentList.get(graphBId).getGraph().size(); s++) {
							double nodeKernel = computeNodeKernelValue(p, graphAId, graphBId, r, s);
							nodeKernels[p][graphAId][graphBId][r][s] = (float)nodeKernel;
						}
					}
				}
			}
			System.out.println();
		}
		
		for (int p = 0; p <= h; p++) {
			double[][] kernel = new double[fragmentList.size()][fragmentList.size()];
			for (int graphAId = 0; graphAId < fragmentList.size(); graphAId++) {
				for (int graphBId = 0; graphBId < graphAId; graphBId++) {
					kernel[graphAId][graphBId] = kernel[graphBId][graphAId];
				}
				for (int graphBId = graphAId; graphBId < fragmentList.size(); graphBId++) {
					double krnl = 0.0;
					for (int r = 0; r < fragmentList.get(graphAId).getGraph().size(); r++) {
						for (int s = 0; s < fragmentList.get(graphBId).getGraph().size(); s++) {
							krnl += nodeKernels[p][graphAId][graphBId][r][s];
						}
					}
					kernel[graphAId][graphBId] = krnl;
				}
			}
			
			for (int i = 0; i < kernel.length; i++) {
				for (int j = 0; j < kernel[0].length; j++) {
					kernels[p][i][j] = kernel[i][j];
				}
			}
		}
	}
	
	private void computeNodeEdgeGraphKernelByString() {
		kernels = new double[h + 1][fragmentList.size()][fragmentList.size()];
		multisetIdLabelMap = new TreeMap<String, String>();
		multisetLabelIdMap = new TreeMap<String, String>();
		multisetLabelDocListMap = new TreeMap<String, LinkedList<Integer>>();
		multisetIdStepMap = new TreeMap<String, Integer>();
		
		// maybe we don't need this!!!!!!!!
		// for vector space
		ArrayList<TreeMap<String, Double>> multisetLabelValueMapList = new ArrayList<TreeMap<String,Double>>();
		for (int idx = 0; idx < fragmentList.size(); idx++) {
			multisetLabelValueMapList.add(idx, new TreeMap<String, Double>());
		}
		// end
		
		for (int p = 0; p <= h; p++) {
			System.out.println("step = " + p);
			
			for (int graphAId = 0; graphAId < fragmentList.size(); graphAId++) {
				System.out.print(graphAId + " ");
				for (int graphBId = 0; graphBId < fragmentList.size(); graphBId++) {
					for (int r = 0; r < fragmentList.get(graphAId).getGraph().size(); r++) {
						for (int s = 0; s < fragmentList.get(graphBId).getGraph().size(); s++) {
							String nodeKernelString = getNodeKernelString(p, graphAId, graphBId, r, s);
							double nodeKernel = computeNodeKernelStringValue(nodeKernelString);
							nodeKernelStringValueMap.put(nodeKernelString, nodeKernel);							
						}
					}
				}
			}
			System.out.println();
		}
		
		for (int p = 0; p <= h; p++) {
			double[][] kernel = new double[fragmentList.size()][fragmentList.size()];
			for (int graphAId = 0; graphAId < fragmentList.size(); graphAId++) {
				for (int graphBId = 0; graphBId < graphAId; graphBId++) {
					kernel[graphAId][graphBId] = kernel[graphBId][graphAId];
				}
				for (int graphBId = graphAId; graphBId < fragmentList.size(); graphBId++) {
					double krnl = 0.0;
					for (int r = 0; r < fragmentList.get(graphAId).getGraph().size(); r++) {
						for (int s = 0; s < fragmentList.get(graphBId).getGraph().size(); s++) {
							String nodeKernelString = getNodeKernelString(p, graphAId, graphBId, r, s); 
							krnl += nodeKernelStringValueMap.get(nodeKernelString);
						}
					}
					kernel[graphAId][graphBId] = krnl;
				}
			}
			
			for (int i = 0; i < kernel.length; i++) {
				for (int j = 0; j < kernel[0].length; j++) {
					kernels[p][i][j] = kernel[i][j];
				}
			}
		}
	}
	
	private String getNodeKernelString(int p, int graphAId, int graphBId, int r, int s) {
		return p + "," + graphAId + "," + graphBId + "," + r + "," + s;
	}
	
	private double computeNodeKernelQuartetValue(int p, Quartet<Integer, Integer, Integer, Integer> quartet) {
		if (p == 0) {
			return nodeKernel(quartet.getValue0(), quartet.getValue1(), quartet.getValue2(), quartet.getValue3());
		}
		
		double nodeKernel = nodeKernel(quartet.getValue0(), quartet.getValue1(), quartet.getValue2(), quartet.getValue3());
		if (nodeKernel == 0) {
			return 0;
		}
		double sum = 0;
		for (int rNeighbor : fragmentList.get(quartet.getValue0()).getGraph().get(quartet.getValue2())) {
			for (int sNeighbor : fragmentList.get(quartet.getValue1()).getGraph().get(quartet.getValue3())) {
				sum += edgeKernel(quartet.getValue0(), quartet.getValue1(), quartet.getValue2(), rNeighbor, quartet.getValue3(), sNeighbor) * pQuartetKernelMapMap.get(p - 1).get(new Quartet<Integer, Integer, Integer, Integer>(quartet.getValue0(), quartet.getValue1(), rNeighbor, sNeighbor));
			}
		}
		nodeKernel *= sum;
		
		return nodeKernel;
	}
	
	private double computeNodeKernelValue(int p, int graphAId, int graphBId, int r, int s) {
		if (p == 0) {
			return nodeKernel(graphAId, graphBId, r, s);
		}
		
		double nodeKernel = nodeKernel(graphAId, graphBId, r, s);
		if (nodeKernel == 0) {
			return 0;
		}
		double sum = 0;
		for (int rNeighbor : fragmentList.get(graphAId).getGraph().get(r)) {
			for (int sNeighbor : fragmentList.get(graphBId).getGraph().get(s)) {
				sum += edgeKernel(graphAId, graphBId, r, rNeighbor, s, sNeighbor) * nodeKernels[p - 1][graphAId][graphBId][rNeighbor][sNeighbor];
			}
		}
		nodeKernel *= sum;
		
		return nodeKernel;
	}
	
	private double computeNodeKernelStringValue(String nodeKernelString) {
		String[] items = nodeKernelString.split(",");
		int p = Integer.parseInt(items[0]);
		int graphAId = Integer.parseInt(items[1]);
		int graphBId = Integer.parseInt(items[2]);
		int r = Integer.parseInt(items[3]); // graph a node id
		int s = Integer.parseInt(items[4]); // graph b node id
		
		if (p == 0) {
			return nodeKernel(graphAId, graphBId, r, s);
		}
		
		double nodeKernel = nodeKernel(graphAId, graphBId, r, s);
		if (nodeKernel == 0) {
			return 0;
		}
		double sum = 0;
		for (int rNeighbor : fragmentList.get(graphAId).getGraph().get(r)) {
			for (int sNeighbor : fragmentList.get(graphBId).getGraph().get(s)) {
				String nodeKernelStringPMinus1 = getNodeKernelString(p - 1, graphAId, graphBId, rNeighbor, sNeighbor);
				sum += edgeKernel(graphAId, graphBId, r, rNeighbor, s, sNeighbor) * nodeKernelStringValueMap.get(nodeKernelStringPMinus1);
			}
		}
		nodeKernel *= sum;
		
		return nodeKernel;
	}
	
	private double kroneckerDelta(String a, String b) {
		return a.equals(b) ? 1.0 : 0.0;
	}
	
	private double nodeWeight(String nodeLabel) {
		if (uniformWeights) {
			return 1.0;
		}
		
		nodeLabel = nodeLabels[Integer.parseInt(nodeLabel)];
		if (isDesignatedEntityNode(nodeLabel)) {
			return deNodeWeight;
		} else if (isOtherEntityNode(nodeLabel)) {
			return oeNodeWeight;
		} else if (isFrameNode(nodeLabel)) {
			return fnNodeWeight;
		} else if (isFrameTargetNode(nodeLabel)) {
			return ftNodeWeight;
		} else if (isFrameElementNode(nodeLabel)) {
			return feNodeWeight;
		} else if (isLexicalItemNode(nodeLabel)) {	// words
			return liNodeWeight;
		} else {
			//System.out.println("Unidentified node type");
			return 1.0;
		}
	}
	
	private double edgeWeight(String fromNodeLabel, String toNodeLabel) {
		if (uniformWeights) {
			return 1.0;
		}
		
		fromNodeLabel = nodeLabels[Integer.parseInt(fromNodeLabel)];
		toNodeLabel = nodeLabels[Integer.parseInt(toNodeLabel)];
		if (
			isDesignatedEntityNode(fromNodeLabel) && isFrameElementNode(toNodeLabel)
			|| isFrameElementNode(fromNodeLabel) && isDesignatedEntityNode(toNodeLabel)
		) {
			return deFeEdgeWeight;
		} else if (isFrameNode(fromNodeLabel) && isFrameNode(toNodeLabel)) {
			return fnFnEdgeWeight;
		} else if (
			isFrameTargetNode(fromNodeLabel) && isFrameNode(toNodeLabel)
			|| isFrameNode(fromNodeLabel) && isFrameTargetNode(toNodeLabel)
		) {
			return ftFnEdgeWeight;
		} else if (
			isFrameElementNode(fromNodeLabel) && isFrameNode(toNodeLabel)
			|| isFrameNode(fromNodeLabel) && isFrameElementNode(toNodeLabel)
		) {
			return feFnEdgeWeight;
		} else if (
			isLexicalItemNode(fromNodeLabel) && isFrameElementNode(toNodeLabel)
			|| isFrameElementNode(fromNodeLabel) && isLexicalItemNode(toNodeLabel)
		) {
			return liFeEdgeWeight;
		} else if (
			isOtherEntityNode(fromNodeLabel) && isFrameElementNode(toNodeLabel)
			|| isFrameElementNode(fromNodeLabel) && isOtherEntityNode(toNodeLabel)
		) {
			return oeFeEdgeWeight;
		} else {
			//System.out.println("Unidentified edge type");
			return 1.0;
		}
	}
	
	private boolean isDesignatedEntityNode(String label) {
		return label.equals("DESIGNATED_ENTITY") ? true : false;
	}
	
	private boolean isOtherEntityNode(String label) {
		return label.equals("OTHER_ENTITY") ? true : false;
	}
	
	private boolean isFrameNode(String label) {
		return label.startsWith("FRAME_NAME-") ? true : false;
	}
	
	private boolean isFrameTargetNode(String label) {
		return label.startsWith("FRAME_TARGET-") ? true : false;
	}
	
	private boolean isFrameElementNode(String label) {
		return label.startsWith("FRAME_ELEMENT-") ? true : false;
	}
	
	private boolean isLexicalItemNode(String label) {
		return label.startsWith("LEXICAL_ITEM-") ? true : false;
	}
	
//	private double nodeKernel(int graphAId, int graphBId, int r, int s) {
//		String rLabel = fragmentList.get(graphAId).getAttributes().get(r);
//		String sLabel = fragmentList.get(graphBId).getAttributes().get(s);
//		return kroneckerDelta(rLabel, sLabel) * nodeWeight(rLabel);
//	}
	private double nodeKernel(int graphAId, int graphBId, int r, int s) {
//		String rLabel = fragmentList.get(graphAId).getAttributes().get(r);
//		String sLabel = fragmentList.get(graphBId).getAttributes().get(s);
//		return rLabel.equals(sLabel) ? nodeWeight(rLabel) : 0;
		return attributes[graphAId][r].equals(attributes[graphBId][s]) ? nodeWeight(attributes[graphAId][r]) : 0;
	}
	
//	private double edgeKernel(int graphAId, int graphBId, int r, int rNeighbor, int s, int sNeighbor) {
//		String rLabel = fragmentList.get(graphAId).getAttributes().get(r);
//		String rNeighborLabel = fragmentList.get(graphAId).getAttributes().get(rNeighbor);
//		
//		String sLabel = fragmentList.get(graphBId).getAttributes().get(s);
//		String sNeighborLabel = fragmentList.get(graphBId).getAttributes().get(sNeighbor);
//		
//		return kroneckerDelta(rLabel, sLabel) * kroneckerDelta(rNeighborLabel, sNeighborLabel) * edgeWeight(rLabel, rNeighborLabel);
//	}
	private double edgeKernel(int graphAId, int graphBId, int r, int rNeighbor, int s, int sNeighbor) {
//		String rLabel = fragmentList.get(graphAId).getAttributes().get(r);
//		String rNeighborLabel = fragmentList.get(graphAId).getAttributes().get(rNeighbor);
//		
//		String sLabel = fragmentList.get(graphBId).getAttributes().get(s);
//		String sNeighborLabel = fragmentList.get(graphBId).getAttributes().get(sNeighbor);
//		
//		return rLabel.equals(sLabel) && rNeighborLabel.equals(sNeighborLabel) ? edgeWeight(rLabel, rNeighborLabel) : 0;
		return attributes[graphAId][r].equals(attributes[graphBId][s]) && attributes[graphAId][rNeighbor].equals(attributes[graphBId][sNeighbor]) ? edgeWeight(attributes[graphAId][r], attributes[graphAId][rNeighbor]) : 0;
	}
	
	private void computeNodeEdgeGraphKernel0() {
		kernels = new double[h + 1][fragmentList.size()][fragmentList.size()];
		multisetIdLabelMap = new TreeMap<String, String>();
		multisetLabelIdMap = new TreeMap<String, String>();
		multisetLabelDocListMap = new TreeMap<String, LinkedList<Integer>>();
		multisetIdStepMap = new TreeMap<String, Integer>();
		
		// maybe we don't need this!!!!!!!!
		// for vector space
		ArrayList<TreeMap<String, Double>> multisetLabelValueMapList = new ArrayList<TreeMap<String,Double>>();
		for (int idx = 0; idx < fragmentList.size(); idx++) {
			multisetLabelValueMapList.add(idx, new TreeMap<String, Double>());
		}
		// end
		
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
					//double d = dotProduct(attributeValueMaps.get(i), attributeValueMaps.get(j));
					double d = similarity(attributeValueMaps.get(i), attributeValueMaps.get(j), h);
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
	
	protected double similarity(Map<String, Integer> a, Map<String, Integer> b, int h) {
		double sum = 0;
		
		for (String aKey : a.keySet()) {
			String aNodeLabel = recoverNodeLabelOneStep(aKey, h); // maybe we only need recoverNodeLabelOneStep!!!!!!!
//			System.out.println("recovered label for " + aKey + ": " + aNodeLabel);
			if (!aNodeLabel.contains("->")) { // single node
				for (String bKey : b.keySet()) {
					String bNodeLabel = recoverNodeLabelOneStep(bKey, h);
//					System.out.println("recovered label for " + bKey + ": " + bNodeLabel);
					if (!bNodeLabel.contains("->")) { // single node
						if (aNodeLabel.equals(bNodeLabel)) {
							double nodeWeight = 1; //0.8;
							int aKeyFreq = a.get(aKey);
							int bKeyFreq = b.get(bKey);
							double nodeKernel = aKeyFreq * bKeyFreq * nodeWeight;
							double w = nodeKernel;
							sum += w;
						}
					}
				}
			} else { // have neighbors
				String aFromNode = aNodeLabel.replaceAll("^\\(", "").replaceAll("\\)$", "").replaceAll("\\->.+", "");
				String[] aToNodes = aNodeLabel.replaceAll("^\\(", "").replaceAll("\\)$", "").replaceAll(".+?\\->", "").split(",");
				
				for (String aToNode : aToNodes) {
					String aPartialPath = aFromNode + "->" + aToNode;
//					System.out.println("aPartialPath: " + aPartialPath);
					String aFromNodeType = "frame";
					String aToNodeType = "frame";
					String aEdgeType = "frame-frame";
					
					for (String bKey : b.keySet()) {
						String bNodeLabel = recoverNodeLabelOneStep(bKey, h);
//						System.out.println("recovered label for " + bKey + ": " + bNodeLabel);
						
						String bFromNode = bNodeLabel.replaceAll("^\\(", "").replaceAll("\\)$", "").replaceAll("\\->.+", "");
						if (!bFromNode.equals(aFromNode)) {
							continue;
						}
						String[] bToNodes = bNodeLabel.replaceAll("^\\(", "").replaceAll("\\)$", "").replaceAll(".+?\\->", "").split(",");
						for (String bToNode : bToNodes) {
							String bPartialPath = bFromNode + "->" + bToNode;
//							System.out.println("bPartialPath: " + bPartialPath);
							String bFromNodeType = "frame";
							String bToNodeType = "frame";
							String bEdgeType = "frame-frame";
							
							if (aPartialPath.equals(bPartialPath)) {
								double fromNodeWeight = 1; //0.8;
								double edgeWeight = 1; //0.7;
								double toNodeWeight = 1; //0.8;
								int aKeyFreq = a.get(aKey);
								int bKeyFreq = b.get(bKey);
								double fromNodeKernel = fromNodeWeight;
								double toNodeKernel = toNodeWeight;
								double edgeKernel = edgeWeight;
								double w = aKeyFreq * bKeyFreq * fromNodeKernel * edgeKernel * toNodeKernel;
								sum += w;
							}
						}
					}
				}
			}
		}
		
		return sum;
	}
	
	protected String recoverNodeLabel(String multisetId, int h) {
		String[] multisetLabels = multisetIdLabelMap.get(multisetId).split("\\-");
		StringBuilder sb = new StringBuilder();
		for (String multisetLabel : multisetLabels) {
			String nodeLabel = nodeLabels[Integer.parseInt(multisetLabel)];
			sb.append(nodeLabel + "->");
		}
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}
	
	
	/**
	 * @param args
	 */
	public static void main0(String[] args) {
		// TODO Auto-generated method stub
		
		String graphFilename = null;
		String nodeLabelFilename = null;
		String graphLabelFilename = null;
		int h = 1;
		String libsvmFilename = null;
		String multisetLabelFilename = null;
		boolean normalization = false;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--graph")) {
				graphFilename = args[++i];
			} else if (args[i].equals("--node_label")) {
				nodeLabelFilename = args[++i];
			} else if (args[i].equals("--graph_label")) {
				graphLabelFilename = args[++i];
			} else if (args[i].equals("--h")) {
				h = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--out")) {
				libsvmFilename = args[++i];
			} else if (args[i].equals("--out_multiset_label")) {
				multisetLabelFilename = args[++i];
			} else if (args[i].equals("--normalization")) {
				++i;
				normalization = args[i].startsWith("y") || args[i].startsWith("Y") ? true : false;
			}
		}
		
		if (graphFilename == null 
			|| nodeLabelFilename == null
			|| graphLabelFilename == null
			|| h < 0
			|| libsvmFilename == null
		) {
			System.out.println("Usage: java NodeEdgeGraphKernel --graph <graph_file> --node_label <node_label_file> --graph_label <data_instance_label_file> --h <step_size> --out <output_file_in_libsvm_format> [(optional) --out_multiset_label <output_file_for_multiset_label_features>]\n");
			System.out.println("E.g. : java NodeEdgeGraphKernel --graph ../data/toy.graph --node_label ../data/toy.graph.node --graph_label ../data/toy.label --h 1 --out ../data/toy.data --out_multiset_label ../data/toy.multiset_labels\n");
			System.out.println("E.g. : java NodeEdgeGraphKernel --graph ../data/toy.graph --node_label ../data/toy.graph.node --graph_label ../data/toy.label --h 1 --out ../data/toy.data\n");
			System.exit(0);
		}
		
		ArrayList<Graph> graphList = loadGraphsFromFile(graphFilename);
		String[] nodeLabels = Tools.read(nodeLabelFilename).split("\n");
		String[] graphLabels = Tools.read(graphLabelFilename).split("\n");
		for (int i = 0; i < graphLabels.length; i++) {
			graphLabels[i] = graphLabels[i].replaceAll(".*,", "");
		}
		
		NodeEdgeGraphKernel gk = new NodeEdgeGraphKernel(graphList, nodeLabels, graphLabels);
		gk.setH(h);
		gk.setNormalization(normalization);
		//gk.computeNodeEdgeGraphKernelByString();
		//gk.computeNodeEdgeGraphKernelByArray();
		gk.computeNodeEdgeGraphKernelByRecursion();
		//gk.computeNodeEdgeGraphKernelByQuartet();
		if (gk.isNormalization()) {
			gk.normalize();
		}
		gk.writeLibsvmFormat(libsvmFilename, h);
		if (multisetLabelFilename != null) {
			gk.writeMultisetLabelDocListMap(multisetLabelFilename);
		}
		
		System.out.println("NodeEdgeGraphKernel done.");

	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		String name = null;  // "semgraph_gics45_IBM_forestsent_frame_dep_dir";
//		String dir = null; // "/Users/xie/Programs/workspace/nlp4fe/output/fragment_modeler/45";
		String graphFilename = null;
		//filename = "/Users/xie/Programs/workspace/nlp4fe/output/fragment_modeler/45/semgraph_gics45_YHOO_forestsent_frame_dep_dir.graph";
		String nodeLabelFilename = null;
		String graphLabelFilename = null;
		//String attributeFilename = dir + "/" + name + "_attributes_nek.tsv";
		String libsvmFilename = null;
		//String svmlightFilename = dir + "/" + name + "_nek.data";
		String svmlightFilename = null;
		String attributeFilename = null;
		//String arffFilename = dir + "/" + name + "_nek.arff";
		String featureFilename = null; // dir + "/" + name + "_multisetLabelFreqMap_nek.tsv";
		int h = 3;
		boolean normalization = false;
		boolean uniformWeights = true;
		float deNodeWeight = 0.9f;
		float oeNodeWeight = 0.4f;
		float fnNodeWeight = 0.8f;
		float ftNodeWeight = 0.7f;
		float feNodeWeight = 0.6f;
		float liNodeWeight = 0.5f;
		float deFeEdgeWeight = 0.9f;
		float oeFeEdgeWeight = 0.4f;
		float fnFnEdgeWeight = 0.8f;
		float ftFnEdgeWeight = 0.7f;
		float feFnEdgeWeight = 0.6f;
		float liFeEdgeWeight = 0.5f;
		ArrayList<Float> kernelWeightList = new ArrayList<Float>();
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--graph")) {
				graphFilename = args[++i];
			} else if (args[i].equals("--node_label")) {
				nodeLabelFilename = args[++i];
			} else if (args[i].equals("--graph_label")) {
				graphLabelFilename = args[++i];
			} else if (args[i].equals("--out_libsvm")) {
				libsvmFilename = args[++i];
			} else if (args[i].equals("--out_svmlight")) {
				svmlightFilename = args[++i];
			} else if (args[i].equals("--out_feature")) {
				featureFilename = args[++i];
			} else if (args[i].equals("--h")) {
				h = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--normalization")) {
				++i;
				normalization = args[i].startsWith("y") || args[i].startsWith("Y") ? true : false;
			} else if (args[i].equals("--uniform_weights")) {
				++i;
				uniformWeights = args[i].startsWith("y") || args[i].startsWith("Y") ? true : false;
			} else if (args[i].equals("--de_node_weight")) {
				deNodeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--oe_node_weight")) {
				oeNodeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--fn_node_weight")) {
				fnNodeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--ft_node_weight")) {
				ftNodeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--fe_node_weight")) {
				feNodeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--li_node_weight")) {
				liNodeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--de_fe_edge_weight")) {
				deFeEdgeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--oe_fe_edge_weight")) {
				oeFeEdgeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--fn_fn_edge_weight")) {
				fnFnEdgeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--ft_fn_edge_weight")) {
				ftFnEdgeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--fe_fn_edge_weight")) {
				feFnEdgeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].equals("--li_fe_edge_weight")) {
				liFeEdgeWeight = Float.parseFloat(args[++i]);
			} else if (args[i].matches("\\-\\-kernel_weight_\\d+")) {
				int kernelNum = Integer.parseInt(args[i].replaceAll("\\-\\-kernel_weight_", ""));
				while (kernelWeightList.size() <= kernelNum) {
					kernelWeightList.add(0f);
				}
				kernelWeightList.set(kernelNum, Float.parseFloat(args[++i]));
			}
		}
		while (kernelWeightList.size() < h) {
			kernelWeightList.add(0f);
		}
		
		String[] nodeLabels = Tools.read(nodeLabelFilename).split("\n");
		String[] graphLabels = Tools.read(graphLabelFilename).split("\n");
		for (int i = 0; i < graphLabels.length; i++) {
			graphLabels[i] = graphLabels[i].replaceAll(".*,", "");
		}
		
		ArrayList<Graph> fragmentList = loadGraphsFromFile(graphFilename);
		
		NodeEdgeGraphKernel gk = new NodeEdgeGraphKernel(fragmentList, nodeLabels, graphLabels);
		gk.setH(h);
		gk.setNormalization(normalization);
		gk.setUniformWeights(uniformWeights);
		gk.setDeNodeWeight(deNodeWeight);
		gk.setOeNodeWeight(oeNodeWeight);
		gk.setFnNodeWeight(fnNodeWeight);
		gk.setFtNodeWeight(ftNodeWeight);
		gk.setFeNodeWeight(feNodeWeight);
		gk.setLiNodeWeight(liNodeWeight);
		gk.setDeFeEdgeWeight(deFeEdgeWeight);
		gk.setOeFeEdgeWeight(oeFeEdgeWeight);
		gk.setFnFnEdgeWeight(fnFnEdgeWeight);
		gk.setFtFnEdgeWeight(ftFnEdgeWeight);
		gk.setFeFnEdgeWeight(feFnEdgeWeight);
		gk.setLiFeEdgeWeight(liFeEdgeWeight);
		gk.setKernelWeightList(kernelWeightList);
		gk.computeNodeEdgeGraphKernelByRecursion();
		if (gk.isNormalization()) {
			gk.normalize();
		}
		for (int hh = 0; hh <= h; hh++) {
			String libsvmHFilename = libsvmFilename.replaceAll("\\.(.*?)", "_h" + hh + ".$1");
			gk.writeLibsvmFormat(libsvmHFilename, hh);
		}
		String libsvmHFilename = libsvmFilename.replaceAll("\\.(.*?)", "_hWeighted.$1");
		gk.writeLibsvmFormat(libsvmHFilename, h + 1);
		gk.writeMultisetLabelDocListMap(featureFilename);
//		gk.writeArffFormat(arffFilename);
		
		// has not been verified
//		attributeFilename = svmlightFilename + ".attributes"; 
//		gk.writeSvmLightFormat(svmlightFilename, attributeFilename);
		
		System.out.print("done NodeEdgeGraphKernel");
		for (String arg : args) {
			System.out.print(" " + arg);
		}
		System.out.println();

	}

}
