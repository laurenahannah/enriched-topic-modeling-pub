package edu.columbia.ccls.modeler;
import java.util.*;
import java.io.*;
//import SemGraphModeler;
import edu.columbia.ccls.modeler.SemGraphModeler;
import edu.columbia.ccls.util.Graph;
public class GenerateGraph {

	public GenerateGraph(){

	}
	public static void main(String[] args) throws IOException {
		String path = args[0];
		File folder = new File(path);
		File[] list = folder.listFiles();
		String[] filenames = new String[list.length];
		for (int i = 0; i < list.length; i++) {
			filenames[i] = list[i].getName();
		}
		SemGraphModeler modeler = new SemGraphModeler();
		Graph g = modeler.getMergedFragment(filenames);
		PrintWriter pw = new PrintWriter(new File("result.graph"));
//		ArrayList<ArrayList<Integer>> values = g.getGraph();
//		for (int i = 0; i < values.size(); i++) {
//			ArrayList<Integer> line = values.get(i);
//			
//		}
		pw.write(g.toStringOneLine());
		pw.close();

	}

}
