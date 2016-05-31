package edu.columbia.ccls.finance;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import edu.columbia.ccls.exception.StockPriceNotFoundException;
import edu.columbia.ccls.modeler.SemGraphModeler;
import edu.columbia.ccls.modeler.SemGraphModeler.Task;
import edu.columbia.ccls.util.Tools;

public class StockPrice {
	
	private static HashMap<String, HashMap<String, Double>> tickerDateStockPriceMap = null;
	private static HashMap<String, HashMap<String, Double>> gicsDateStockPriceMap = null;
	private static final String DATE_FORMAT = "YYYY-MM-DD";
	
	private static void loadTickerDateStockPrice() {
		if (tickerDateStockPriceMap == null) {
			tickerDateStockPriceMap = new HashMap<String, HashMap<String,Double>>();
		}
		
		String[] rows = Tools.read(DataSource.SP500_STOCK_PRICE_FILENAME).split("\n");
		for (int i = 1; i < rows.length; i++) {
			String[] cols = rows[i].split(",");
			String ticker = cols[0];
			String date = cols[1];
			double stockPrice = Double.parseDouble(cols[7]);
			
			if (!tickerDateStockPriceMap.containsKey(ticker)) {
				tickerDateStockPriceMap.put(ticker, new HashMap<String, Double>());
			}
			tickerDateStockPriceMap.get(ticker).put(date, stockPrice);
		}
	}
	
	public static void loadGicsDateStockPrice() {
		if (gicsDateStockPriceMap == null) {
			gicsDateStockPriceMap = new HashMap<String, HashMap<String,Double>>();
		}
		
		if (tickerDateStockPriceMap == null) {
			loadTickerDateStockPrice();
		}
		
		HashMap<String, HashMap<String, LinkedList<Double>>> gicsDateStockPriceListMapMap = new HashMap<String, HashMap<String,LinkedList<Double>>>();
		for (String ticker : tickerDateStockPriceMap.keySet()) {
			String gics = Sector.getGicsByTicker(ticker);
			
			HashMap<String, Double> dateStockPriceMap = tickerDateStockPriceMap.get(ticker);
			for (String date : dateStockPriceMap.keySet()) {
				double tickerStockPrice = dateStockPriceMap.get(date);
				
				if (!gicsDateStockPriceListMapMap.containsKey(gics)) {
					gicsDateStockPriceListMapMap.put(gics, new HashMap<String, LinkedList<Double>>());
				}
				if (!gicsDateStockPriceListMapMap.get(gics).containsKey(date)) {
					gicsDateStockPriceListMapMap.get(gics).put(date, new LinkedList<Double>());
				}
				gicsDateStockPriceListMapMap.get(gics).get(date).add(tickerStockPrice);
			}
		}
		
		for (String gics : gicsDateStockPriceListMapMap.keySet()) {
			if (!gicsDateStockPriceMap.containsKey(gics)) {
				gicsDateStockPriceMap.put(gics, new HashMap<String, Double>());
			}
			
			for (String date : gicsDateStockPriceListMapMap.get(gics).keySet()) {
				double sum = 0.0;
				for (Double tickerStockPrice : gicsDateStockPriceListMapMap.get(gics).get(date)) {
					sum += tickerStockPrice;
				}
				double average = sum / gicsDateStockPriceListMapMap.get(gics).get(date).size();
				
				gicsDateStockPriceMap.get(gics).put(date, average);
			}
		}
	}
	
	private static double getStockPricePresentDate(String ticker, String dateStr) throws StockPriceNotFoundException {
		if (tickerDateStockPriceMap == null) {
			loadTickerDateStockPrice();
		}
		
		if (!tickerDateStockPriceMap.containsKey(ticker)) {
			throw new StockPriceNotFoundException("Exception: stock price not found for ticker " + ticker + ".");
		}
		
		Calendar presentDate = Tools.stringToCalendar(dateStr);
		int limitPreviousDay = 7;
		while (!tickerDateStockPriceMap.get(ticker).containsKey(Tools.CalendarToString(presentDate, DATE_FORMAT)) && limitPreviousDay-- > 0) {
			presentDate.add(Calendar.DAY_OF_YEAR, -1);
		}
		if (limitPreviousDay == -1) {
			throw new StockPriceNotFoundException("Exception: stock price not found for the present date of " + dateStr + ".");
		}
		
		return tickerDateStockPriceMap.get(ticker).get(Tools.CalendarToString(presentDate, DATE_FORMAT));
	}
	
	private static double getStockPricePreviousDate(String ticker, String dateStr) throws StockPriceNotFoundException {
		if (tickerDateStockPriceMap == null) {
			loadTickerDateStockPrice();
		}
		
		if (!tickerDateStockPriceMap.containsKey(ticker)) {
			throw new StockPriceNotFoundException("Exception: stock price not found for ticker " + ticker + ".");
		}
		
		Calendar previousDate = Tools.stringToCalendar(dateStr);
		int limitPreviousDay = 7;
		do {
			previousDate.add(Calendar.DAY_OF_YEAR, -1);
		} while (!tickerDateStockPriceMap.get(ticker).containsKey(Tools.CalendarToString(previousDate, DATE_FORMAT)) && limitPreviousDay-- > 0);
		if (limitPreviousDay == -1) {
			throw new StockPriceNotFoundException("Exception: stock price not found for the previous date of " + dateStr + ".");
		}
		
		return tickerDateStockPriceMap.get(ticker).get(Tools.CalendarToString(previousDate, DATE_FORMAT));
	}
	
	private static double getGicsStockPricePreviousDate(String gics, String dateStr) throws StockPriceNotFoundException {
		if (gicsDateStockPriceMap == null) {
			loadGicsDateStockPrice();
		}
		
		if (!gicsDateStockPriceMap.containsKey(gics)) {
			throw new StockPriceNotFoundException("Exception: stock price not found for ticker " + gics + ".");
		}
		
		Calendar previousDate = Tools.stringToCalendar(dateStr);
		int limitPreviousDay = 7;
		do {
			previousDate.add(Calendar.DAY_OF_YEAR, -1);
		} while (!gicsDateStockPriceMap.get(gics).containsKey(Tools.CalendarToString(previousDate, DATE_FORMAT)) && limitPreviousDay-- > 0);
		if (limitPreviousDay == -1) {
			throw new StockPriceNotFoundException("Exception: stock price not found for the previous date of " + dateStr + ".");
		}
		
		return gicsDateStockPriceMap.get(gics).get(Tools.CalendarToString(previousDate, DATE_FORMAT));
	}
	
	private static double getStockPriceNextDate(String ticker, String dateStr) throws StockPriceNotFoundException {
		if (tickerDateStockPriceMap == null) {
			loadTickerDateStockPrice();
		}
		
		if (!tickerDateStockPriceMap.containsKey(ticker)) {
			throw new StockPriceNotFoundException("Exception: stock price not found for ticker " + ticker + ".");
		}
		
		Calendar nextDate = Tools.stringToCalendar(dateStr);
		int limitNextDay = 7;
		do {
			nextDate.add(Calendar.DAY_OF_YEAR, 1);
		} while (!tickerDateStockPriceMap.get(ticker).containsKey(Tools.CalendarToString(nextDate, DATE_FORMAT)) && limitNextDay-- > 0);
		if (limitNextDay == -1) {
			throw new StockPriceNotFoundException("Exception: stock price not found for the next date of " + dateStr + ".");
		}
		
		return tickerDateStockPriceMap.get(ticker).get(Tools.CalendarToString(nextDate, DATE_FORMAT));
	}

	private static double getGicsStockPriceNextDate(String gics, String dateStr) throws StockPriceNotFoundException {
		if (gicsDateStockPriceMap == null) {
			loadGicsDateStockPrice();
		}
		
		if (!gicsDateStockPriceMap.containsKey(gics)) {
			throw new StockPriceNotFoundException("Exception: stock price not found for ticker " + gics + ".");
		}
		
		Calendar nextDate = Tools.stringToCalendar(dateStr);
		int limitNextDay = 7;
		do {
			nextDate.add(Calendar.DAY_OF_YEAR, 1);
		} while (!gicsDateStockPriceMap.get(gics).containsKey(Tools.CalendarToString(nextDate, DATE_FORMAT)) && limitNextDay-- > 0);
		if (limitNextDay == -1) {
			throw new StockPriceNotFoundException("Exception: stock price not found for the next date of " + dateStr + ".");
		}
		
		return gicsDateStockPriceMap.get(gics).get(Tools.CalendarToString(nextDate, DATE_FORMAT));
	}
	
	public static double getStockPriceChangeNextToPrevious(String ticker, String dateStr) throws StockPriceNotFoundException {
		double stockPricePreviousDate = getStockPricePreviousDate(ticker, dateStr);
		double stockPriceNextDate = getStockPriceNextDate(ticker, dateStr);
		//System.out.println(stockPricePreviousDate);
		//System.out.println(stockPriceNextDate);
		double change = Math.log(stockPriceNextDate / stockPricePreviousDate);
		
		return change;
	}
	
	public static double getGicsStockPriceChangeNextToPrevious(String gics, String dateStr) throws StockPriceNotFoundException {
		double gicsStockPricePreviousDate = getGicsStockPricePreviousDate(gics, dateStr);
		double gicsStockPriceNextDate = getGicsStockPriceNextDate(gics, dateStr);
		//System.out.println(stockPricePreviousDate);
		//System.out.println(stockPriceNextDate);
		double change = Math.log(gicsStockPriceNextDate / gicsStockPricePreviousDate);
		
		return change;
	}
	
	
	
	private static double getStockPricePresentDateOrBefore(String ticker, Calendar date) throws StockPriceNotFoundException {
		if (tickerDateStockPriceMap == null) {
			loadTickerDateStockPrice();
		}
		
		if (!tickerDateStockPriceMap.containsKey(ticker)) {
			throw new StockPriceNotFoundException("Exception: stock price not found for ticker " + ticker + ".");
		}
		
		int limitPreviousDay = 7;
		while (!tickerDateStockPriceMap.get(ticker).containsKey(Tools.CalendarToString(date, DATE_FORMAT)) && limitPreviousDay-- > 0) {
			date.add(Calendar.DAY_OF_YEAR, -1);
		}
		if (limitPreviousDay == -1) {
			throw new StockPriceNotFoundException("Exception: stock price not found for the present date of " + Tools.CalendarToString(date) + ".");
		}
		
		return tickerDateStockPriceMap.get(ticker).get(Tools.CalendarToString(date, DATE_FORMAT));
	}
	
	private static double getStockPricePresentDateOrAfter(String ticker, Calendar date) throws StockPriceNotFoundException {
		if (tickerDateStockPriceMap == null) {
			loadTickerDateStockPrice();
		}
		
		if (!tickerDateStockPriceMap.containsKey(ticker)) {
			throw new StockPriceNotFoundException("Exception: stock price not found for ticker " + ticker + ".");
		}
		
		int limitNextDay = 7;
		while (!tickerDateStockPriceMap.get(ticker).containsKey(Tools.CalendarToString(date, DATE_FORMAT)) && limitNextDay-- > 0) {
			date.add(Calendar.DAY_OF_YEAR, 1);
		}
		if (limitNextDay == -1) {
			throw new StockPriceNotFoundException("Exception: stock price not found for the present date of " + Tools.CalendarToString(date) + ".");
		}
		
		return tickerDateStockPriceMap.get(ticker).get(Tools.CalendarToString(date, DATE_FORMAT));
	}
	
	public static double getStockPriceLogReturn(String ticker, String dateStr, int referenceLag, int duration) throws StockPriceNotFoundException {
		// we need to subtract 1 because the oftenly the news of the day (before market close) is reflected by the price of the day (at the market close) comparing to the previous day
		// when referenceLag=0, duration=1, if the news comes out before the market close, we use the market closing price of the day compare to the price of the previous day
		referenceLag = referenceLag - 1;	 
		
		Calendar startDate = Tools.stringToCalendar(dateStr);
		startDate.add(Calendar.DAY_OF_YEAR, referenceLag);
		
		Calendar endDate = Tools.stringToCalendar(dateStr);
		endDate.add(Calendar.DAY_OF_YEAR, referenceLag + duration);
		
		//System.out.print(Tools.CalendarToString(startDate) + " to " + Tools.CalendarToString(endDate));
		
		double stockPriceStartDate = getStockPricePresentDateOrBefore(ticker, startDate);
		double stockPriceEndDate = getStockPricePresentDateOrAfter(ticker, endDate);
		//System.out.print("\t" + stockPriceStartDate);
		//System.out.print("\t" + stockPriceEndDate);
		double logReturn = Math.log(1.0 * stockPriceEndDate / stockPriceStartDate);
		//System.out.print("\t" + logReturn);
		
		return logReturn;
	}
	
	public static void test(String ticker, String dateStr) {
		double stockPricePresentDate = Double.NaN;
		try {
			stockPricePresentDate = getStockPricePresentDate(ticker, dateStr);
		} catch (StockPriceNotFoundException e) {
			System.out.println(e.getMessage());
		}
		
		double stockPricePreviousDate = Double.NaN;
		try {
			stockPricePreviousDate = getStockPricePreviousDate(ticker, dateStr);
		} catch (StockPriceNotFoundException e) {
			System.out.println(e.getMessage());
		}
		
		double stockPriceNextDate = Double.NaN;
		try {
			stockPriceNextDate = getStockPriceNextDate(ticker, dateStr);
		} catch (StockPriceNotFoundException e) {
			System.out.println(e.getMessage());
		}
		
		System.out.println("present date: " + stockPricePresentDate);
		System.out.println("previous date: " + stockPricePreviousDate);
		System.out.println("next date: " + stockPriceNextDate);
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String ticker = "IBM";
		String gics = Sector.getGicsByTicker(ticker);
		
//		String dateStr = "2012-09-05";
//		try {
//			//double change = getStockPriceChangeNextToPrevious(ticker, dateStr);
//			//System.out.println("next date - previous date: " + change);
//			
//			double gicsChange = getGicsStockPriceChangeNextToPrevious(gics, dateStr);
//			System.out.println("GICS" + gics + ": next date - previous date: " + gicsChange);
//		} catch (StockPriceNotFoundException e) {
//			// TODO Auto-generated catch block
//			System.out.println(e.getMessage());
//		}
		
		
		// get daily price label
		String startDateStr = "2007-01-01-16-00-00";
		String endDateStr = "2013-11-31-16-00-00";
		Calendar calendarFixed = Calendar.getInstance();
		//Task task = Task.PRICE_POLARITY;
		//Task task = Task.PRICE_FOLLOW_MARKET;
		//Task task = Task.PRICE_CHANGE_RELATIVE_TO_MARKET;
		Task task = Task.PRICE_POLARITY_RELATIVE_TO_MARKET;
		
		TreeMap<String, String> dailyPriceLabel = SemGraphModeler.getDailyPriceLabel(ticker, startDateStr, endDateStr, calendarFixed, task);
		int countPos = 0;
		int countNeg = 0;
		for (String dateStr : dailyPriceLabel.keySet()) {
			String label = dailyPriceLabel.get(dateStr);
			System.out.println(dateStr + ": " + label);
			if (label.equals("1")) {
				countPos++;
			} else {
				countNeg++;
			}
		}
		System.out.println("pos: " + countPos);
		System.out.println("neg: " + countNeg);
		
				
		System.out.println("StockPrice done.");

	}

}
