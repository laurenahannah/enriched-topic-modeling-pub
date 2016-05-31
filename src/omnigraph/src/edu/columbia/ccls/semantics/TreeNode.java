package edu.columbia.ccls.semantics;

import java.util.ArrayList;

public class TreeNode {
	private String value = null;
	private ArrayList<TreeNode> children = null;
	
	public TreeNode(String value, ArrayList<TreeNode> children) {
		this.value = value;
		this.children = children;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ArrayList<TreeNode> getChildren() {
		return children;
	}

	public void setChildren(ArrayList<TreeNode> children) {
		this.children = children;
	}
	
	public String toString() {
		if (value == null) {
			return "";
		}
		
		String result = null;
		if (children == null) {
			result = " " + value;
		} else if (children.size() <= 0) {
			result = " (" + value + " )";
		} else {
			result = " (" + value;
			for (int i = 0; i < children.size(); i++) {
				result += children.get(i).toString();
			}
			result += ")";
		}
		
		
		return result;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		TreeNode root = new TreeNode("TARGET_OBJECT", new ArrayList<TreeNode>());
		TreeNode leaf0 = new TreeNode("Leaf0", null);
		TreeNode leaf1 = new TreeNode("Leaf1", null);
		TreeNode leaf2 = new TreeNode("Leaf2", null);
		
		TreeNode node0 = new TreeNode("Node0", new ArrayList<TreeNode>());
		TreeNode node1 = new TreeNode("Node1", new ArrayList<TreeNode>());
		TreeNode node2 = new TreeNode("Node2", new ArrayList<TreeNode>());
		
		node0.getChildren().add(leaf0);
		node0.getChildren().add(leaf1);
		node1.getChildren().add(leaf2);
		
		root.getChildren().add(node0);
		root.getChildren().add(node1);
		root.getChildren().add(node2);
		
		String treeString = root.toString();
		System.out.println(treeString);

	}

}
