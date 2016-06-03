# -*- coding: utf-8 -*-
"""
Created on Fri Apr 22 14:20:42 2016

@author: lhannah

make_stopword_list.py reads in: 1. a set of plain text files, and an existing 
stopword file to make a new file tailored to the corpus.
"""

from os import listdir
from os.path import isfile, join
from pandas import DataFrame
from numpy import abs, exp, log, log2, percentile, power, zeros
import operator
from collections import defaultdict
import sys

def make_stopword_list(text_folder,stopword_list,output_name):
    # Inputs:
    #   - text_folder:  a folder with corpus in individual plain text files
    #   - stopword_list:    an existing list of stopwords
    #   - output_name:  name where new list will be stored
    # Outputs:
    #   - a new .txt file saved under output_name

    # Make a new dictionary where words will be assigned to each doc
    onlyfiles = [f for f in listdir(text_folder) if isfile(join(text_folder, f))]
    num_files = len(onlyfiles)
    
    counts = defaultdict(lambda: zeros(num_files+1, dtype = int))
    
    # Punctuation to remove for data cleaning
    punctuation_list = ['.',',','/','?',']','[','{','}', '(', ')', "'", '"', ':', ';', '<', '>', "\ "]
    
    # Loop through files and add counts
    for idx, f in enumerate(onlyfiles):
        filename = join(text_folder, f)
        with open(filename, 'r') as in_file:
            # Loop through items and add counts
            for line in in_file:
                words = line.split()
                # Remove punctuation
                words = [''.join(c for c in s if c not in punctuation_list) for s in words]
                for word in words:
                    counts[word][idx] += 1
                    counts[word][num_files] += 1
                    
    # Find words with high counts and non-informative distributions
    tf_stopwords = dict()
    for key, val in counts.iteritems():
        word = key
        # Get a set of doc counts greater than 1
        count_vals = [ell for ell in counts[word] if ell > 0]
        num_docs = len(count_vals) - 1
        # Use normalized term frequency for thresholds
        tf_stopwords[word] = log((float(num_files - num_docs)+.5)/(float(num_docs)+.5))
        #if tf_stopwords[word] < 3:
        #    print((word, num_docs,num_files))
    # Sort tf_stopwords and print out the first 100 values

    sorted_tf = sorted(tf_stopwords.items(), key=operator.itemgetter(1))
    # Make a threshold
    thresh = 3    
    truncated_tf = [ell for ell in sorted_tf if ell[1] <= thresh]
    # Write the output
    writeable = ""
    
    with open(output_name,'w') as out_file:
        with open(stopword_list,'r') as stop_file:
            for line in stop_file:
                if "\n" in line:
                    out_file.write(line)
                else:
                    out_file.write(line + "\n")
        # Now add in new words
        for item in truncated_tf:
            out_file.write(item[0] + "\n")
    
    out_file.close()
            
if __name__ == "__main__":
    print(sys.argv[1])
    text_folder = sys.argv[1]
    stopword_list = sys.argv[2]
    output_name = sys.argv[3] 
    make_stopword_list(text_folder,stopword_list,output_name)
