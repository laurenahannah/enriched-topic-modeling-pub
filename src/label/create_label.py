"""
Created on Thu Apr 14 2016
@author: raylinz
Purpose:
This script is used to create a csv file that records each company's ticker, date, and label that indicates the
up-down daily movement of this company's stock daily closing prices.
Inputs:
    1. [.csv file] file for each company's stock daily price data, e.g. stock_price_SP500_01012006_11302013.csv
    2. [.csv file] file for each company's ticker and gics information, e.g. sp500_tickers_by_sector.csv
    3. [str] name of the file for each company's stock daily price data, e.g. 'stock_price_SP500_01012006_11302013.csv'
    4. [str] column name for company ticker in company's stock daily price data, e.g. 'Ticker'
    5. [str] column name for date in company's stock daily price data, e.g. 'Date'
    6. [str] column name for daily adjusted closing price in company's stock daily price data, e.g. 'Adj Close'
    7. [str] name of the file for each company's ticker and gics information, e.g. 'sp500_tickers_by_sector.csv'
    8. [str] column name for company ticker in the file for each company's ticker and gics info, e.g. 'ticker'
    9. [str] column name for company GICS in the file for each company's ticker and gics info, e.t. 'gics'
    10. [str] name of the output csv file whose header =  Company,Date,Label, e.g. 'label_SP500_01012006_11302013.csv'

Outputs:
    - a csv file with header =  Company,Date,Label, in the working directory
How-to-run:
    1. Make sure that the following three files are in the same folder/working directory:
    - 'stock_price_SP500_01012006_11302013.csv' (the file for each company's stock daily price data)
    - 'sp500_tickers_by_sector.csv' (the file for each company's ticker and gics information)
    - create_label.py (this current python script)
    2. In terminal, change directory to the current folder
    3. Parse argument to run this script. An example argument would be:
    python create_label.py 'stock_price_SP500_01012006_11302013.csv' 'Ticker' 'Date' 'Adj Close' 'sp500_tickers_by_sector.csv' 'ticker' 'gics' 'label_SP500_01012006_11302013.csv'
"""

import sys
import collections
from itertools import islice


def main(filename, headname_ticker, headname_date, headname_adjClosePrice, filename_ticker, head_ticker, head_gics, output_filename):
    dic = {}
    f = open(filename,'r')
    for headline in islice(f, 0, 1): # look at the first line in file, header
        # headline = 'Ticker,Date,Open,High,Low,Close,Volume,Adj Close'
        head_list = headline.strip().split(',')  # .strip() removes '\n' at end of string
        ticker_ind = head_list.index(headname_ticker)
        date_ind = head_list.index(headname_date)
        adjprice_ind = head_list.index(headname_adjClosePrice)
    for line in f: # islice already omit line0 that contains header info
        line_list = line.split(',')
        # line_list is a list of strings, e.g. ['A','2006-01-03','33.40','33.58','32.82','33.50','3796200','31.00']
        if not dic.has_key(line_list[ticker_ind]):
            dic_date2price = {} # create dictionary to store date and adj_price
            dic_date2price[line_list[date_ind]] = float(line_list[adjprice_ind].strip())
            # .strip() removes '\n' at the end of the adj price
            dic[line_list[ticker_ind]] = dic_date2price
        else: # already read in this company
            dic[line_list[ticker_ind]][line_list[date_ind]] = float(line_list[adjprice_ind].strip())
            # add date and adj_price to dic_date2price
    for k, v in dic.iteritems():
        od = collections.OrderedDict(sorted(v.items(), key=lambda t: t[0]))
        # od is an ordered dictionary, sorted by key (date) from small to large
        dic[k] = od
    # dic is a dictionary of ordered dictionary, len(dic) = 493 companies,
    # dic's value is ticker (string) of each company,
    # dic's key is a orderedDic, OD, where key = date (string) and value = adj price (float) of this ticker on that day, e.g. {'2006-01-03': 31.0}
    # each OD, len(OD) = 1979, coz 1979 trading days

    # next, read in ticker and gics info from 'sp500_tickers_by_sector.csv'
    f_ticker = open(filename_ticker,'r')
    t = {}
    for headl in islice(f_ticker, 0, 1): # look at the first line in file, header
        # headl = 'ticker,gics,sector,company,address'
        headl_list = headl.strip().split(',')  # .strip() removes '\n' at end of string
        tick_ind = headl_list.index(head_ticker)
        gics_ind = headl_list.index(head_gics)
    for l in f_ticker: # islice already omit line0
        l_list = l.split(',')
        t[l_list[tick_ind]] = l_list[gics_ind]
    # now t is a dictionary whose key = company ticker (str), value is company's gics num (str)

    all_label = []
    #crete all_label to store each company's date and label info
    for ticker, date2price in dic.iteritems():
        list_label = [] # empty list to store each day's label in order
        list_adj_price = date2price.values() # date order will be preserved
        for i, price in enumerate(list_adj_price):
            if i == 0:
                list_label.append('NA')
            else:
                ratio = price/list_adj_price[i-1]
                if ratio >= 1.02:
                    list_label.append('+')
                elif ratio <= 0.98:
                    list_label.append('-')
                else:
                    list_label.append('no movement')
        list_date = date2price.keys() # a list of date in order from small to large
        # Next, modify company's ticker name to the following format:
        # IBM_SP500_GICS45_COMPANY: only 'IBM'(ticker) and 'GICS45' changes
        comp_name = ticker + '_SP500_GICS' + t[ticker] + '_COMPANY'
        ### Next, write the string: 'company_name, date, label
        for i in range(len(list_date)):
            outline = comp_name + ',' + list_date[i] + ',' + list_label[i]
            all_label.append(outline)
    # all_label is a list of strings (948976 strings)
    # Finally sort the list all_label by company_name, and date
    all_label.sort()
    output_head = 'Company,Date,Label'
    all_label.insert(0, output_head)
    # write to new csv
    outf = open(output_filename,'w+')
    for item in all_label:
        outf.write("%s\n" % item)
 



if __name__ == "__main__":

    main(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8])
    # filename = sys.argv[1]
    # # name of the file for each company's stock daily price data
    # # e.g.: sys.argv[1] = 'stock_price_SP500_01012006_11302013.csv'
    # headname_ticker = sys.argv[2]
    # # column name for company ticker in argv[1], i.e. the file for each company's stock daily price data
    # # e.g.: sys.argv[2] = 'Ticker'
    # headname_date = sys.argv[3]
    # # column name for date in argv[1], i.e. the file for each company's stock daily price data
    # # e.g.: sys.argv[3] = 'Date'
    # headname_adjClosePrice = sys.argv[4]
    # # column name for daily adjusted closing price in argv[1], i.e. the file for each company's stock daily price data
    # # e.g.: sys.argv[4] = 'Adj Close'
    # filename_ticker = sys.argv[5]
    # # name of the file for each company's ticker and gics information
    # # e.g.: sys.argv[5] = 'sp500_tickers_by_sector.csv'
    # head_ticker = sys.argv[6]
    # # column name for company ticker in argv[5], i.e. the file for each company's ticker and gics info
    # # e.g. sys.argv[6] = 'ticker'
    # head_gics = sys.argv[7]
    # # column name for company GICS in argv[5], i.e. the file for each company's ticker and gics info
    # # e.g. sys.argv[7] = 'gics'
    # output_filename = [8]
    # # name of the output csv file (header = Company,Date,Label)
    # # e.g. sys.argv[9] = 'label_SP500_01012006_11302013'












