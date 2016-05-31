package edu.columbia.ccls.finance;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.opencsv.CSVReader;

public class Sector {
	
	private static HashMap<String, TreeSet<String>> gicsTickers = null;
	private static TreeMap<String, String> tickerGicsMap = null;
	private static TreeMap<String, String> tickerCompanyNameMap = null;
	
	public static final String[] GICS_ARRAY = {"10", "15", "20", "25", "30", "35", "40", "45", "50", "55"};
	
	private static Object lock = new Object();
	
	private static String[] getCandidateNames(String name) {
		LinkedList<String> stack = new LinkedList<String>();
		
		// Corp., Inc., Co., Ltd.
//		if (name.endsWith(".")) {
//			name = name.substring(0, name.length() - 1);
//		}
		name = name.trim();
		String[] tokens = name.split("\\s+");
		String candidate = tokens[0];
		if (tokens.length == 1) {
			stack.push(candidate);
		} else {
			if (
				candidate.equals("Verizon")
				|| candidate.equals("Sprint")
			) {
				stack.push(candidate);
			}
		}
		
		if (tokens.length == 2 
			&& (
				tokens[1].equals("Corp.")
				|| tokens[1].equals("Corp")
				|| tokens[1].equals("Corporation")
				|| tokens[1].equals("Inc.")
				|| tokens[1].equals("Inc")
				|| tokens[1].equals("Incorporation")
				|| tokens[1].equals("Co.")
				|| tokens[1].equals("Co")
				|| tokens[1].equals("Company")
				|| tokens[1].equals("Ltd.")
				|| tokens[1].equals("Ltd")
				|| tokens[1].equals("Limited")
			)
		) {
			stack.push(tokens[0]);
		}
		
		for (int i = 1; i < tokens.length; i++) {
			candidate = tokens[0] + " ";
			for (int j = 1; j <= i; j++) {
				//System.out.println(tokens[j]);
				candidate += tokens[j] + " ";
			}
			candidate = candidate.trim();
			
			if (candidate.equals("International Business")
				|| candidate.equals("Public Service")
				|| candidate.equals("United States")
				|| candidate.equals("American Electric")
			) {
				continue;
			}
			
			stack.push(candidate);
		}
		
		if (tokens[tokens.length - 1].equals("Corp")) {
			stack.push(candidate.replaceAll(" Corp", " Corp."));
			stack.push(candidate.replaceAll(" Corp", " Corporation"));
		} else if (tokens[tokens.length - 1].equals("Corporation")) {
			stack.push(candidate.replaceAll(" Corporation", " Corp"));
			stack.push(candidate.replaceAll(" Corporation", " Corp."));
		} else if (tokens[tokens.length - 1].equals("Inc")) {
			stack.push(candidate.replaceAll(" Inc", " Inc."));
			stack.push(candidate.replaceAll(" Inc", " Incorporation"));
		}  else if (tokens[tokens.length - 1].equals("Incorporation")) {
			stack.push(candidate.replaceAll(" Incorporation", " Inc"));
			stack.push(candidate.replaceAll(" Incorporation", " Inc."));
		} else if (tokens[tokens.length - 1].equals("Co")) {
			stack.push(candidate.replaceAll(" Co", " Co."));
			stack.push(candidate.replaceAll(" Co", " Company"));
		} else if (tokens[tokens.length - 1].equals("Company")) {
			stack.push(candidate.replaceAll(" Company", " Co"));
			stack.push(candidate.replaceAll(" Company", " Co."));
		} else if (tokens[tokens.length - 1].equals(" Ltd")) {
			stack.push(candidate.replaceAll(" Ltd", " Ltd."));
			stack.push(candidate.replaceAll(" Ltd", " Limited"));
		} else if (tokens[tokens.length - 1].equals(" Limited")) {
			stack.push(candidate.replaceAll(" Limited", " Ltd"));
			stack.push(candidate.replaceAll(" Limited", " Ltd."));
		} else {
			stack.push(candidate + " Inc");
			stack.push(candidate + " Co");
			stack.push(candidate + " Corp");
			stack.push(candidate + " Ltd");
			
			stack.push(candidate + " Inc.");
			stack.push(candidate + " Co.");
			stack.push(candidate + " Corp.");
			stack.push(candidate + " Ltd.");
			
			stack.push(candidate + " Incorporation");
			stack.push(candidate + " Company");
			stack.push(candidate + " Corporation");
			stack.push(candidate + " Limited");
		}
		
		if (name.contains("International Business Machines")) {
			stack.push("IBM");
		}
		
		String[] candidates = new String[stack.size()];
		int idx = 0;
		for (String c : stack) {
			candidates[idx++] = c;
		}
		
		return candidates;
	}
	
	private static HashMap<String, String> loadNameTickerMap(String gics) {
		HashMap<String, String> nameTickerMap = new HashMap<String, String>();
		TreeSet<String> tickers = Sector.getTickersByGics(gics);
		for (String ticker : tickers) {
			String companyName = Sector.getCompanyNameByTicker(ticker);
			String[] candidates = Sector.getCandidateNames(companyName);
			for (String candidate : candidates) {
				nameTickerMap.put(candidate, ticker);
			}
		}
		return nameTickerMap;
	}
	
	public static HashMap<String, LinkedList<String>> loadTickerNamesMap(String gics) {
		if (!gics.equals("55")) {
			System.out.println("not the gics I want");
		}
		System.out.println("here here herea: gics=" + gics);
		HashMap<String, LinkedList<String>> tickerNamesMap = new HashMap<String, LinkedList<String>>();
		TreeSet<String> tickers = Sector.getTickersByGics(gics);
//		int limit = 1000;
//		while (tickers == null && limit-- > 0) {
//			tickers = Sector.getTickersByGics(gics);
//			try {
//				Thread.sleep(1000);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
		for (String ticker : tickers) {
			tickerNamesMap.put(ticker, new LinkedList<String>());
			String companyName = Sector.getCompanyNameByTicker(ticker);
			String[] candidates = getCandidateNames(companyName);
			for (String candidate : candidates) {
				tickerNamesMap.get(ticker).add(candidate);	// need to keep the order (from full name -> short name)
			}
		}
		//System.out.println(tickerNamesMap);
		return tickerNamesMap;
	}
	
	public static HashMap<String, LinkedList<String>> loadTickerNamesMap() {
		HashMap<String, LinkedList<String>> tickerNamesMap = new HashMap<String, LinkedList<String>>();
		TreeSet<String> tickers = Sector.getSp500TickerSet();
//		int limit = 1000;
//		while (tickers == null && limit-- > 0) {
//			tickers = Sector.getTickersByGics(gics);
//			try {
//				Thread.sleep(1000);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
		for (String ticker : tickers) {
			tickerNamesMap.put(ticker, new LinkedList<String>());
			String companyName = Sector.getCompanyNameByTicker(ticker);
			String[] candidates = getCandidateNames(companyName);
			for (String candidate : candidates) {
				tickerNamesMap.get(ticker).add(candidate);	// need to keep the order (from full name -> short name)
			}
		}
		//System.out.println(tickerNamesMap);
		return tickerNamesMap;
	}
	
	private static void loadGicsTickers() {
		gicsTickers = new HashMap<String, TreeSet<String>>();
		tickerGicsMap = new TreeMap<String, String>();
		tickerCompanyNameMap = new TreeMap<String, String>();
		try {
			CSVReader reader = new CSVReader(new FileReader(DataSource.SP500_TICKERS_BY_SECTOR_FILENAME));
			String[] nextLine;
			nextLine = reader.readNext();	// skip the header
		    while ((nextLine = reader.readNext()) != null) {
		    	String ticker = nextLine[0];
		    	String gics = nextLine[1];
		    	if (!gicsTickers.containsKey(gics)) {
		    		gicsTickers.put(gics, new TreeSet<String>());
		    	}
		        gicsTickers.get(gics).add(ticker);
		        tickerGicsMap.put(ticker, gics);
		        
		        String companyName = nextLine[3];
		        tickerCompanyNameMap.put(ticker, companyName);
		    }
		    reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static TreeSet<String> getTickersByGics(String gics) {
		//if (gicsTickers == null) {
		synchronized (lock) {
			if (gicsTickers == null || !gicsTickers.containsKey(gics)) {
				loadGicsTickers();
			}
		}
		
		return gicsTickers.get(gics);
	}
	
	public static String getGicsByTicker(String ticker) {
		synchronized (lock) {
			if (tickerGicsMap == null) {
				loadGicsTickers();
			}
		}
		
		return tickerGicsMap.get(ticker);
	}
	
	public static String[] getSp500Tickers() {
		synchronized (lock) {
			if (tickerGicsMap == null) {
				loadGicsTickers();
			}
		}
		
		String[] tickers = new String[tickerGicsMap.keySet().size()];
		int idx = 0;
		for (String ticker : tickerGicsMap.keySet()) {
			tickers[idx++] = ticker;
		}
		return tickers;
	}
	
	public static TreeSet<String> getSp500TickerSet() {
		synchronized (lock) {
			if (tickerGicsMap == null) {
				loadGicsTickers();
			}
		}
		
		TreeSet<String> set = new TreeSet<String>();
		set.addAll(tickerGicsMap.keySet());
		return set;
	}
	
	public static String getCompanyNameByTicker(String ticker) {
		synchronized (lock) {
			if (tickerCompanyNameMap == null || !tickerCompanyNameMap.containsKey(ticker)) {
				loadGicsTickers();
			}
		}
		
		return tickerCompanyNameMap.get(ticker);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String gics = "10";
		TreeSet<String> tickers = Sector.getTickersByGics(gics);
		System.out.println(tickers);
		
		String ticker = "T";
		String g = Sector.getGicsByTicker(ticker);
		System.out.println(g);
		String[] names = Sector.getCandidateNames(Sector.getCompanyNameByTicker(ticker));
		for (String name : names) {
			System.out.println(name);
		}
		
		System.out.println(loadTickerNamesMap(gics));
		
		System.out.println("Sector done.");

	}

}
