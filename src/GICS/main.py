"""
Created on Sun Apr 17 2016
@author: raylinz
Purpose:
This script is used to create a csv file that records each company's ticker, date, and label that indicates the
up-down daily movement of this company's stock daily closing prices.
Inputs:
    1. [directory with sub-directories of txt files] A gicsxx folder where xx is the sector gics number.
       gicsxx folder should contain subdirectories named with
       each company's ticker in this sector, and each company's folder should contain ".txt' files
       each '.txt' file contains one news sentence about this company on a specific date.

Outputs:
    1. A folder named with 'result' in the gicsxx directory. Within 'result',
    there are subfolders each named with the company's ticker. Within each company ticker's sub folder,
    there're .txt files, each contains all the news about this company on a specific date.

How-to-run:
    1. Make sure that this main.py script is in the gicsxx directory.
    2. In terminal, change directory to this gicsxx directory.
    3. Parse argument to run this script: python main.py
"""


import os
import sys
from glob import glob
from shutil import rmtree
from itertools import islice
import re



def main():
    dir = os.getcwd() # dir is the current directory, gics folder
    s = re.search('([^/]+$)',  dir).group(0) # regular expression to get sector name after last slash
    sector = s.upper() # GICSxx, the capitalized sector name

    pattern = os.path.join(dir, 'result*')
    for folder in glob(pattern):
        if not os.path.isdir(folder):
            continue
        rmtree(folder)
    # remove existing result folders in gics directory


    
    dir_for_result_folder = os.path.join(dir, 'result')
    os.makedirs(dir_for_result_folder, 0755) # create new directory named 'result'


    foldername_list = []
    for foldername in os.listdir('.'): # foldername is the string of the name for a folder
        if not (foldername.endswith('.py') or foldername.startswith('result') or foldername.startswith('.')):
            # foldername.startswith('.') are the names for all hidden files
            foldername_list.append(foldername)
            # foldername_list is a list that contains the names of all company's folders,
            # e.g. ['ABC', 'AMGN', 'HSP', 'MRK', 'PKI', 'WLP']



    for compname in foldername_list: # compname is the string of a company's name, e.g. 'AMGN'
        dir_current_company = os.path.join(dir, compname)
        # e.g. dir_current_company = /Users/raylin/Google Drive/GoldmanResearch/code/cleanApr14/NewCorpus/gics35/compname
        os.chdir(dir_current_company) # change directory to current company's folder
        datedic = {} # dictionary to store all dates for this company. Key = date, value = list of sentences.
        # e.g. {'2007-10-22': ['S&P500 went up.', 'oh yeah!']}
        for newsfilename in os.listdir(dir_current_company):
            if newsfilename.endswith('.txt'):
                #newsfilename is a string of a news file in current company's directory
                list_newsfilename = list(newsfilename)
                str_date = ''.join(list_newsfilename[31:41])
                line = open(newsfilename).read()
                if not datedic.has_key(str_date):
                    datedic[str_date] = [] # key to store each sentence for this date for this company
                    datedic[str_date].append(line)
                else: #this date already recorded some sentence
                    datedic[str_date].append(line)
        dir_for_result_company = os.path.join(dir_for_result_folder, compname)
        # directory name for current company's result folder.
        os.makedirs(dir_for_result_company, 0755)
        # create directory for current company's result folder.
        os.chdir(dir_for_result_company) # change directory to current company's result folder

        for date, content in datedic.iteritems():
            filename = compname + '_SP500_' + sector + '_COMPANY' + '_' + date + '.txt'
            outf = open(filename,'w+')
            for item in datedic[date]:
                outf.write("%s\n" % item)




if __name__ == "__main__":

    main()




