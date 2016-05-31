package edu.columbia.ccls.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class Tools {
	private static final int BSIZE = 1024 * 1024;
	
	public static String read(String fileName) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(new FileReader(new File(fileName).getAbsoluteFile()));
			try {
				String s;
				while ((s = in.readLine()) != null) {
					sb.append(s);
					sb.append("\n");
				}
			} finally {
				in.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb.toString();
	}
	
	public static void write(String fileName, String text) {
		try {
			PrintWriter out = new PrintWriter(new File(fileName).getAbsoluteFile());
			try {
				out.print(text);
			} finally {
				out.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void append(String fileName, String text) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(new File(fileName).getAbsoluteFile(), true));
			try {
				out.print(text);
			} finally {
				out.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String readFile(String fileName) {
		String content = "";
		try {
			FileChannel fc = new FileInputStream(fileName).getChannel();
			ByteBuffer buff = ByteBuffer.allocate(BSIZE);
			fc.read(buff);
			buff.flip();
			while (buff.hasRemaining())
				content += (char)buff.get();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return content;
	}
	
	public static void writeFile(String fileName, String text) {
		try {
			FileChannel fc = new FileOutputStream(fileName).getChannel();
			fc.write(ByteBuffer.wrap(text.getBytes()));
			fc.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static int[] getTopFromArray(int i[], int topNum, int threshold) {
		int[] topIndex = new int[topNum];
		int index = 0;
		//int t=0;
		int mini=-100; 
		int num=0;
		boolean flag=true;
		
		for (int j = 0; j < topNum; j++)
			topIndex[j] = -1;
		
		w:
		while(flag) {
			int maxvalue=0; 
			int count=0; 
			for(int j=0;j<i.length;j++) {//find max num
				if(maxvalue<=i[j]) {
					maxvalue=i[j];
				} 
			}//for 
			
			if (maxvalue < threshold)
				break w;
			
			//System.out.println("the top "+(++t)+"number: "+maxvalue); 
			for(int k=0;k<i.length;k++) { 
				if(maxvalue==i[k]) {
					//System.out.println("the top "+t+"number index: "+k);// find max location
					topIndex[index++] = k;
					if (index == topNum)
						break w;
					count++;
					i[k]=mini;
				}       
			}
			num=num+count;
			//System.out.println("the top "+t+"number freq: "+count); // find max num freq
			if(topNum-num==0) flag=false;
		}
		
		return topIndex;
	}
	
	public static String[] dirList(String dirname) {
		// TODO Auto-generated method stub
		File path = new File(dirname); //new File(".");
		String[] list;
		list = path.list();
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
		for (int i = 0; i < list.length; i++)
			list[i] = dirname + "/" + list[i];
		
		return list;
	}
	
	public static boolean createDir(String dir) {
		boolean success = (new File(dir)).mkdirs();
		
		return success;
	}
	
	public static boolean deleteDir(String dir) {
		File directory = new File(dir);
		return deleteDir1(directory);
	}
	
	public static boolean deleteDir1(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir1(new File(dir, children[i]));
				if (!success) { 
					return false;
				}
			}
		} // The directory is now empty so delete it
		
		return dir.delete();
	} 
	
	public static void copy(String from, String to) {
		Tools.write(to, Tools.read(from));
	}
	
	public static String currentTime() {
		String DATE_FORMAT_NOW = "yyyy-MM-dd-HH-mm-ss";
		
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}
	
	public static Calendar stringToCalendar(String dateStr) {
		String[] dateStrArray = dateStr.split("\\D");
		int year = 0, month = 0, day = 0, hrs = 0, min = 0, sec = 0;
		if (dateStrArray.length >= 3) {
			year = Integer.parseInt(dateStrArray[0]);
			month = Integer.parseInt(dateStrArray[1]) - 1;
			day = Integer.parseInt(dateStrArray[2]);
			if (dateStrArray.length >= 6) {
				hrs = Integer.parseInt(dateStrArray[3]);
				min = Integer.parseInt(dateStrArray[4]);
				sec = Integer.parseInt(dateStrArray[5]);
			} else {
				hrs = min = sec = 0;
			}
		}
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, day, hrs, min, sec);
		
		return calendar;
	}
	
	public static Calendar stringToCalendar(String dateStr, Calendar fixed) {
		String[] dateStrArray = dateStr.split("\\D");
		int year = 0, month = 0, day = 0, hrs = 0, min = 0, sec = 0;
		if (dateStrArray.length >= 3) {
			year = Integer.parseInt(dateStrArray[0]);
			month = Integer.parseInt(dateStrArray[1]) - 1;
			day = Integer.parseInt(dateStrArray[2]);
			if (dateStrArray.length >= 6) {
				hrs = Integer.parseInt(dateStrArray[3]);
				min = Integer.parseInt(dateStrArray[4]);
				sec = Integer.parseInt(dateStrArray[5]);
			} else {
				hrs = min = sec = 0;
			}
		}
		
		Calendar calendar = (Calendar) fixed.clone();
		calendar.set(year, month, day, hrs, min, sec);
		
		return calendar;
	}
	
	public static String CalendarToString(Calendar date) {
		String year = String.valueOf(date.get(Calendar.YEAR));
		String month = String.valueOf(date.get(Calendar.MONTH) + 1);
		month = month.length() == 1 ? "0" + month : month;
		String day = String.valueOf(date.get(Calendar.DAY_OF_MONTH));
		day = day.length() == 1 ? "0" + day : day;
		String hour = String.valueOf(date.get(Calendar.HOUR_OF_DAY));
		hour = hour.length() == 1 ? "0" + hour : hour;
		String minute = String.valueOf(date.get(Calendar.MINUTE));
		minute = minute.length() == 1 ? "0" + minute : minute;
		String second = String.valueOf(date.get(Calendar.SECOND));
		second = second.length() == 1 ? "0" + second : second;
		return year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
	}
	
	public static String CalendarToString(Calendar date, String format) {
		String dateStr = null;
		if (format.equals("YYYY-MM-DD")) {
			int y = date.get(Calendar.YEAR);
			int m = date.get(Calendar.MONTH) + 1;
			int d = date.get(Calendar.DAY_OF_MONTH);
			String yStr = String.valueOf(y);
			String mStr = m < 10 ? "0" + String.valueOf(m) : String.valueOf(m);
			String dStr = d < 10 ? "0" + String.valueOf(d) : String.valueOf(d);
			dateStr = yStr + "-" + mStr + "-" + dStr;
			//System.out.println(dateStr);
		}
		return dateStr;
	}
	
	public static <T, U> LinkedHashMap<T, U> sortMapByValue(Map<T, U> map) {
		SortMapByValueComparator<T, U> bvc = new SortMapByValueComparator<T, U>(map);
		TreeMap<T, U> mapSorted = new TreeMap<T, U>(bvc);
		for (T s : map.keySet()) {
			mapSorted.put(s, map.get(s));
		}
		
		LinkedHashMap<T, U> LHMap = new LinkedHashMap<T, U>();
		for (Map.Entry<T, U> e : mapSorted.entrySet()) {
			LHMap.put(e.getKey(), e.getValue());
		}
		
		return LHMap;
	}
}


class DirFilter implements FilenameFilter {
	private Pattern pattern;
	public DirFilter(String regex) {
		pattern = Pattern.compile(regex);
	}
	public boolean accept(File dir, String name) {
		return pattern.matcher(name).matches();
	}
}

class SortMapByValueComparator<T, U> implements Comparator<Object> {
	Map<T, U> base;
	
	public SortMapByValueComparator(Map<T, U> base) {
		this.base = base;
	}
	
	public int compare(Object obj0, Object obj1) {
		if (base.get(obj0) instanceof Double) {
			return ((Double) base.get(obj0)).compareTo((Double) base.get(obj1)) > 0 ? -1 : 1;
		} else if (base.get(obj0) instanceof Integer) {
			return ((Integer) base.get(obj0)).compareTo((Integer) base.get(obj1)) > 0 ? -1 : 1;
		} else if (base.get(obj0) instanceof String) {
			return ((String) base.get(obj0)).compareTo((String) base.get(obj1)) > 0 ? 1 : -1;
		} else {
			return -1;
		}
	}
}


