package edu.columbia.ccls.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Graph implements Comparable<Graph> {
	
	private ArrayList<String> attributes;
	private ArrayList<ArrayList<Integer>> graph; 
	
	public Graph(ArrayList<String> attributes) {
		this.attributes = attributes;
		graph = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < this.attributes.size(); i++) {
			graph.add(new ArrayList<Integer>());
		}
	}
	
	public Graph(String oneLine) {
		String[] sections = oneLine.split(";");
		String attributeStr = sections[0].trim();
		String adjacentMatrixStr = sections.length == 2 ? sections[1].trim() : null;
		
		attributes = new ArrayList<String>();
		String[] idNamePairs = attributeStr.split("\\s+");
		for (String idNamePair : idNamePairs) {
			String[] pair = idNamePair.split(":");
			int id = Integer.parseInt(pair[0]);
			String name = pair[1];
			this.attributes.add(id, name);
		}
		
		graph = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < this.attributes.size(); i++) {
			graph.add(new ArrayList<Integer>());
		}
		
		if (adjacentMatrixStr != null) {
			String[] nodePairs = adjacentMatrixStr.split("\\s+");
			for (String nodePair : nodePairs) {
				String[] pair = nodePair.split(":");
				int from = Integer.parseInt(pair[0]);
				int to = Integer.parseInt(pair[1]);
				graph.get(from).add(to);
			}
		}
		
		for (ArrayList<Integer> list : graph) {
			Collections.sort(list);
		}
	}
	
	public int hashCode() {
		int hashcode = 1;
		
		for (String attribute : attributes) {
			hashcode += attribute.hashCode();
		}
		
		return hashcode;
	}
	
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Graph)) {
			return false;
		}
		
		Graph fragment = (Graph) obj;
		if (!fragment.attributes.equals(this.attributes)) {
			return false;
		}
		
		for (int i = 0; i < fragment.attributes.size(); i++) {
			if (fragment.graph.get(i).size() != this.graph.get(i).size()) {
				return false;
			}
			for (int j = 0; j < fragment.graph.get(i).size(); j++) {
				if (!fragment.graph.get(i).get(j).equals(this.graph.get(i).get(j))) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public void setAttributes(ArrayList<String> attributes) {
		this.attributes = attributes;
	}

	public ArrayList<String> getAttributes() {
		return attributes;
	}

	public ArrayList<ArrayList<Integer>> getGraph() {
		return graph;
	}

	public void setGraph(ArrayList<ArrayList<Integer>> graph) {
		this.graph = graph;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if (this.attributes.size() <= 2) {
			for (String attribute : this.attributes) {
				sb.append("\t" + attribute);
			}
			sb.append("\n");
			
			for (int i = 0; i < this.attributes.size(); i++) {
				sb.append(attributes.get(i));
				int index = 0;
				for (Integer idx : this.graph.get(i)) {
					for (; index < idx; index++) {
						sb.append("\t0");
					}
					sb.append("\t1");
					index++;
				}
				for (; index < this.attributes.size(); index++) {
					sb.append("\t0");
				}
				sb.append("\n");
			}
		} else {
			for (int i = 0; i < this.attributes.size(); i++) {
				sb.append(i + ":" + this.attributes.get(i) + "\n");
			}
			for (int i = 0; i < this.graph.size(); i++) {
				sb.append(i + ":");
				for (int j = 0; j < this.graph.get(i).size(); j++) {
					sb.append(" " + this.graph.get(i).get(j));
				}
				sb.append("\n");
			}
		}
		
		
		return sb.toString();
	}
	
	public String toStringOneLine() {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < this.attributes.size(); i++) {
			sb.append(i + ":" + this.attributes.get(i));
			if (i < this.attributes.size() - 1) {
				sb.append(" ");
			} else {
				sb.append(";");
			}
		}
		for (int i = 0; i < this.graph.size(); i++) {
			for (int j = 0; j < this.graph.get(i).size(); j++) {
				sb.append(" " + i + ":" + this.graph.get(i).get(j));
			}
		}
		
		return sb.toString();
	}
	
	@Override
	public int compareTo(Graph o) {
		// TODO Auto-generated method stub
		return this.toStringOneLine().compareTo(o.toStringOneLine());
	}
	
	public static Graph merge(List<Graph> fragments, Set<String> uniqueAttributeSet, boolean keepIndexSuffix) {
		if (fragments.isEmpty()) {
			return null;
		}
		
		ArrayList<String> attributes = new ArrayList<String>();
		for (int i = 0; i < fragments.size(); i++) {
			//for (String attribute : fragments.get(i).getAttributes()) {
			for (int j = 0; j < fragments.get(i).getAttributes().size(); j++) {
				String attribute = fragments.get(i).getAttributes().get(j) + "-" + i + "_" + j;
				attributes.add(attribute);
			}
		}
		
		// remove the duplicated attribute if it needs to be unique
		for (String uniqueAttribute : uniqueAttributeSet) {
			LinkedList<Integer> toRemove = new LinkedList<Integer>();
			for (int i = attributes.size() - 1; i >= 0; i--) {
				if (attributes.get(i).replaceAll("\\-\\d+_\\d+$", "").equals(uniqueAttribute)) {
					toRemove.add(i);
				}
			}
			// remove all then add one without index suffix
			for (int i : toRemove) {
				attributes.remove(i);
			}
			if (!toRemove.isEmpty()) {
				attributes.add(uniqueAttribute);
			}
		}
		
		Collections.sort(attributes);
		
		TreeMap<String, Integer> attributeIndexMap = new TreeMap<String, Integer>();
		for (int i = 0; i < attributes.size(); i++) {
			attributeIndexMap.put(attributes.get(i), i);
		}
		
		Graph fragment = new Graph(attributes);
		
		for (int i = 0; i < fragments.size(); i++) {
			Graph f = fragments.get(i);
			for (Integer from = 0; from < f.graph.size(); from++) {
				String fromAttribute = f.getAttributes().get(from);
				if (!uniqueAttributeSet.contains(fromAttribute)) {
					fromAttribute += "-" + i + "_" + from;
				}
				Integer fromNew = attributeIndexMap.get(fromAttribute);
				for (Integer to : f.graph.get(from)) {
					String toAttribute = f.getAttributes().get(to);
					if (!uniqueAttributeSet.contains(toAttribute)) {
						toAttribute += "-" + i + "_" + to;
					}
					Integer toNew = attributeIndexMap.get(toAttribute);
					fragment.getGraph().get(fromNew).add(toNew);
				}
			}
		}
		// sort the ajacency list
		for (ArrayList<Integer> list : fragment.getGraph()) {
			Collections.sort(list);
		}
		
		// remove index suffix if not necessary
		if (!keepIndexSuffix) {
			for (int i = 0; i < fragment.getAttributes().size(); i++) {
				fragment.getAttributes().set(i, fragment.getAttributes().get(i).replaceAll("\\-\\d+_\\d+$", ""));
			}
		}
		
		return fragment;
	}
	
	public void removeNodes(Set<String> nodes) {
		ArrayList<String> attributesNew = new ArrayList<String>();
		for (String attribute : this.attributes) {
			if (!nodes.contains(attribute)) {
				attributesNew.add(attribute);
			}
		}
		if (attributesNew.isEmpty()) {
			this.attributes = attributesNew;
			this.graph = new ArrayList<ArrayList<Integer>>();
			return;
		}
		Collections.sort(attributesNew);
		HashMap<Integer, Integer> oldNewIdxMap = new HashMap<Integer, Integer>();
		int newIdx = 0;
		for (int oldIdx = 0; oldIdx < this.attributes.size(); oldIdx++) {
			if (newIdx < attributesNew.size() && this.attributes.get(oldIdx).equals(attributesNew.get(newIdx))) {
				oldNewIdxMap.put(oldIdx, newIdx);
				newIdx++;
			}
		}
		
		ArrayList<ArrayList<Integer>> graphNew = new ArrayList<ArrayList<Integer>>();
		for (int oldIdx = 0; oldIdx < this.attributes.size(); oldIdx++) {
			if (oldNewIdxMap.containsKey(oldIdx)) {
				ArrayList<Integer> oldAl = this.graph.get(oldIdx);
				ArrayList<Integer> newAl = new ArrayList<Integer>();
				for (Integer oIdx : oldAl) {
					if (oldNewIdxMap.containsKey(oIdx)) {
						newAl.add(oldNewIdxMap.get(oIdx));
					}
				}
				
				graphNew.add(newAl);
			}
		}
		this.attributes = attributesNew;
		this.graph = graphNew;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String[] atts0 = {"a", "b", "c"};
		String[] atts1 = {"a", "b", "c"};
		ArrayList<String> attributes0 = new ArrayList<String>();
		ArrayList<String> attributes1 = new ArrayList<String>();
		for (String a : atts0) {
			attributes0.add(a);
		}
		for (String a : atts1) {
			attributes1.add(a);
		}
		
		Graph fragment0 = new Graph(attributes0);
		Graph fragment1 = new Graph(attributes1);
		
		fragment0.getGraph().get(1).add(2);
		fragment1.getGraph().get(1).add(2);
		fragment1.getGraph().get(1).add(1);
		
		System.out.println(fragment0);
		System.out.println(fragment1);
		
		System.out.println(fragment0.toStringOneLine());
		System.out.println(fragment1.toStringOneLine());
		
		System.out.println(fragment0.equals(fragment1));

		TreeSet<String> uniqueAttributeSet = new TreeSet<String>();
		uniqueAttributeSet.add("a");
		uniqueAttributeSet.add("b");
		ArrayList<Graph> fList = new ArrayList<Graph>();
		fList.add(fragment0);
		fList.add(fragment1);
		boolean keepIndexSuffix = false;
		Graph fragment2 = Graph.merge(fList, uniqueAttributeSet, keepIndexSuffix);
		System.out.println(fragment2);
	}

	
}
