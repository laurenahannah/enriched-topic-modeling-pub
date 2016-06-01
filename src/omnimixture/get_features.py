# -*- coding: utf-8 -*-
"""
Created on Fri Jan 29 22:58:25 2016

@author: lhannah

This script is used to do all pre and post processing to append features to
data.

Inputs:
    - directory of documents
    - directory of linearized features
    - list of omnigraph feature names
    - max_phrase_len, the length of the longest omnigraph feature
    - min_count, the smallest count for features

Outputs:
    - directory of documents appended with features
    - directory of documents with only features

"""

from __future__ import division

import sys
from argparse import ArgumentDefaultsHelpFormatter, ArgumentParser
from collections import defaultdict
from numpy import abs, exp, log, log2, percentile, power, zeros, arange, random, histogram, cumsum, isfinite
from pandas import DataFrame, Series
from pandas.io.parsers import read_csv
from scipy.special import gammaln
from shutil import copyfile
import glob
import copy
#import matplotlib.pyplot as plt
import csv
from spyderlib.utils.iofuncs import load_dictionary, save_dictionary

from IPython import embed


##############################################################################
# UTILITY FUNCTIONS
#####################################
# From David B-H Stackoverflow

def variablesfilter():
    from spyderlib.widgets.dicteditorutils import globalsfilter
    from spyderlib.plugins.variableexplorer import VariableExplorer
    from spyderlib.baseconfig import get_conf_path, get_supported_types

    data = globals()
    settings = VariableExplorer.get_settings()

    get_supported_types()
    data = globalsfilter(data,                   
                         check_all=True,
                         filters=tuple(get_supported_types()['picklable']),
                         exclude_private=settings['exclude_private'],
                         exclude_uppercase=settings['exclude_uppercase'],
                         exclude_capitalized=settings['exclude_capitalized'],
                         exclude_unsupported=settings['exclude_unsupported'],
                         excluded_names=settings['excluded_names']+['settings','In'])
    return data
    
def saveglobals(filename):
    data = variablesfilter()
    save_dictionary(data,filename)

# SAVE:
#savepath = 'test.spydata'

#saveglobals(savepath)

# LOAD:
#globals().update(load_dictionary(fpath)[0])
#data = load_dictionary(fpath)
#phrases = set()
#for idx in xrange(1,(longest_phrase+1)):
#    print(idx)
#    phrases[idx] = counts[idx]['ngram']

#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
# From 

def file_type(filename):
    magic_dict = {
    "\x1f\x8b\x08": "gz",
    "\x42\x5a\x68": "bz2",
    "\x50\x4b\x03\x04": "zip"
    }

    max_len = max(len(x) for x in magic_dict)
    
    with open(filename) as f:
        file_start = f.read(max_len)
    for magic, filetype in magic_dict.items():
        if file_start.startswith(magic):
            return filetype
    return "txt"
#++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
###############################################################################
    
###############################################################################
# BAYES FACTOR FEATURE SELECTION
def bfc(a, b, c, d, n, a_plus_b, a_plus_c, alpha, alpha_sum):
    """
    Function for computing Bayes factors conditional on n
    """

    num = (gammaln(n + alpha_sum) +
           4 * gammaln(alpha) +
           gammaln(a_plus_b + 2 * alpha - 1.0) +
           gammaln(c + d + 2 * alpha - 1.0) +
           gammaln(a_plus_c + 2 * alpha - 1.0) +
           gammaln(b + d + 2 * alpha - 1.0) +
           2 * gammaln(alpha_sum - 2.0))
    den = (gammaln(alpha_sum) +
           sum([gammaln(alpha + x) for x in [a, b, c, d]]) +
           2 * gammaln(n + alpha_sum - 2.0) +
           4 * gammaln(2 * alpha - 1.0))

    return exp(num - den)


def bfu(a, b, c, d, n, a_plus_b, a_plus_c, alpha, alpha_sum, beta):
    """
    Function for computing Bayes factors unconditional on n
    """

    num = (log(1.0 + 1.0 / beta) +
           gammaln(n + alpha_sum - 1.0) +
           4 * gammaln(alpha) +
           gammaln(a_plus_b + 2 * alpha - 1.0) +
           gammaln(c + d + 2 * alpha - 1.0) +
           gammaln(a_plus_c + 2 * alpha - 1.0) +
           gammaln(b + d + 2 * alpha - 1.0) +
           2 * gammaln(alpha_sum - 2.0))
    den = (gammaln(alpha_sum - 1.0) +
           sum([gammaln(alpha + x) for x in [a, b, c, d]]) +
           2 * gammaln(n + alpha_sum - 2.0) +
           4 * gammaln(2 * alpha - 1.0))

    return exp(num - den)


def csy(a, b, c, d, n, a_plus_b, a_plus_c):
    """
    Function for computing chi-squared tests with a Yates correction
    """

    num = n * power(abs(a * d - b * c) - n / 2.0, 2)
    den = ((a_plus_b) * (c + d) * (a_plus_c) * (b + d))

    return num / den

def recursive_summary(l, phrases, test, selection, dist, 
                      max_phrase_len, min_phrase_count,counts):
    """
    Pseudo code:
    - inputs: state file, phrase list from lower levels
    - outputs: phrase list for level (l) (phrases stored in dict of dicts)
    - structure:
        ~ make data frame of: n-gram, prefix, suffix, center, count (all)
        ~ iterrows over data frame: a = all, a+b = sum(suffix), a+c = sum(prefix), n = sum 
        ~ use testing algorithms as implemented
        ~ store accepted phrases in dict
      - THIS IS A SUBROUTINE IN summarize_topics
    """
    topics = DataFrame()
    topics['prob'] = 1   
    num_topics = 1
    # Make ngrams for level l
    
    print >> sys.stderr, 'Selecting %d-gram phrases...' % l
    total_phrases = 0
    phrase_and_score = {}
    if l == 1:
        # If unigrams, there is no previous inputs
        # All unigrams are accepted by default        
        ngram = dict()
        for _, row in state.iterrows():
            if row['word'] in ngram.keys():
                ngram[row['word']] += 1
            else:
                ngram[row['word']] = 1
            
        phrases[l] = set([key for key, val in ngram.items() if val >= min_phrase_count])
        scores =  [None]
        total_phrases = len(phrases[l])
        phrase_and_score = dict()
    else:
        # counts[l] is an input...
        # Use tuple keys to make the data frame ngrams
        # Store:
        #   - ngram, its first n-1 words as 'prefix', last n-1 as 'suffix', first word, last, and center
        #   - counts for each topic under topic numbers
        #   - counts for the number of times in the same topic under 'same'
        #   - topic ignorant counts under 'all'
        ngrams = DataFrame.from_records([[' '.join(x), ' '.join(x[:-1]),
                                              ' '.join(x[1:]), x[0],
                                              x[-1], ' '.join(x[1:-1])] + y.tolist()
                                             for x, y in counts[l].items()],
                                            columns=(['ngram', 'prefix',
                                                      'suffix', 'first', 'last', 'center'] +
                                                     range(num_topics) +
                                                     ['same', 'all']))
        n = ngrams['all'].sum()
        counts[l] = ngrams
        
        # Do significance testing
        if test == bfu or test == bfc:
            # If using Bayes factors, set prior parameters
            alpha = 1.0
            alpha_sum = 4 * alpha
            beta = alpha_sum / n
        
        # Precompute the total number of subphrase occurances
        prefix_cache = ngrams.groupby('prefix')['all'].sum()
        suffix_cache = ngrams.groupby('suffix')['all'].sum()
        center_cache = ngrams.groupby('center')['all'].sum()
        first_cache = ngrams.groupby('first')['all'].sum()
        last_cache = ngrams.groupby('last')['all'].sum()
        # Store resulting scores in a list
        scores = len(ngrams) * [None]
        phrase_and_score = list()
        # See if both prefix and suffix are phrases
        for idx, row in ngrams[ngrams['prefix'].isin(phrases[l-1]) &
                               ngrams['suffix'].isin(phrases[l-1]) &
                               (ngrams['all'] >= min_phrase_count)].iterrows():
                                   
            # Do testing for two contingency tables:
            #   - 'first' and 'suffix'
            #   - 'prefix' and 'last'
            a = row['all']
            # Compute values for 'first' vs 'suffix'
            a_plus_b = suffix_cache[row['suffix']]
            a_plus_c = first_cache[row['first']]
            b = a_plus_b - a
            c = a_plus_c - a
            d = n - a_plus_b - c
            # Update test arguments for first test
            args1 = [a, b, c, d, n, a_plus_b, a_plus_c]
            # Compute values for 'prefix' vs 'last'
            a_plus_b = last_cache[row['last']]
            a_plus_c = prefix_cache[row['prefix']]
            b = a_plus_b - a
            c = a_plus_c - a
            d = n - a_plus_b - c
            # Update test arguments for second test
            args2 = [a, b, c, d, n, a_plus_b, a_plus_c]
            # Update parameters to include prior values if Bayes factors
            if test == bfu:
                args1 += [alpha, alpha_sum, beta]
                args2 += [alpha, alpha_sum, beta]
            elif test == bfc:
                args1 += [alpha, alpha_sum]
                args2 += [alpha, alpha_sum]
           
            # Send error if the wrong number of arguments, otherwise run tests
            # For all tests, total score is the less significant value
           
            if test == bfu:
                if min(len(args1),len(args2)) < 9:
                    print >> sys.stderr, args1
                    print >> sys.stderr, args2
                scores[idx] = max(test(*args1), test(*args2))
            elif test == bfc:
                if min(len(args1),len(args2)) < 9:
                    print >> sys.stderr, args1
                    print >> sys.stderr, args2
                scores[idx] = max(test(*args1), test(*args2))
            else:
                scores[idx] = min(test(*args1), test(*args2))
    
        ngrams['score'] = scores
        # Make mask based on test thresholds
        if test == bfu or test == bfc:
            keep = ngrams['score'] <= (1.0 / 10)
        else:
            keep = ngrams['score'] > 10.83
        # Can include further selection requirements---default is 'none'
        if selection == 'none':
            phrases[l] = set(ngrams[keep]['ngram'])
        else:
            if l == 2:
                phrases[l] = dict(ngrams[keep].set_index('ngram')['score'])
            else:
                m = 2 if selection == 'bigram' else l-1
                if test == bfu or test == bfc:
                    tmp = set([k for k, v in phrases[m].items()
                               if v <= percentile(sorted(phrases[m].values(),
                                                         reverse=True),
                                                  (1.0 - 1.0 / 2**l) * 100)])
                else:
                    tmp = set([k for k, v in phrases[m].items()
                               if v >= percentile(sorted(phrases[m].values()),
                                                  (1.0 - 1.0 / 2**l) * 100)])
                if selection == 'bigram':
                    keep &= Series([all([' '.join(bigram) in tmp for bigram in
                                         zip(words, words[1:])]) for words in
                                    [ngram.split() for ngram in
                                     ngrams['ngram']]])
                            #phrases: set vs dict?
                    phrases[l] = set(ngrams[keep]['ngram'])
                else:
                    keep &= (ngrams['prefix'].isin(tmp) &
                             ngrams['suffix'].isin(tmp))
                    phrases[l] = dict(ngrams[keep].set_index('ngram')['score'])
        
        # Next line is only keeping ngram and counts
        phrase_and_score = zip(ngrams[keep]['ngram'], ngrams[keep]['score'])
        total_phrases = len(phrases[l])
        ngrams.drop(['prefix', 'suffix', 'center','score'], axis=1, inplace=True)
    
    # If new phrases were added, phrases_added is set to True
    phrases_added = False
    if total_phrases > 0:
        phrases_added = True
        
    
    # Return phrases, scores, and whether new phrases are added; stored in a list
    output_list = list()  #output_list is a list
    output_list.append(phrases) # First output is phrases
    output_list.append(scores)  # Second output is scores
    output_list.append(phrases_added)   # Is at least one new phrase added ??
    output_list.append(phrases[l])  # Phrases  ??
    output_list.append(counts)  # Counts ??
    output_list.append(phrase_and_score) # Phrase and score list for graphics/diagnostics ??
    return output_list



def get_features_bf(filenames, test, selection, dist, max_phrase_len,
                     min_phrase_count):
    """
    Generate phrase list from topics
    
    Input: 
        - filenames: either local .txt or .gz Mallet output
        - test: bfc (Bayes factor conditional), bfu (BF unconditional), or csy (chi2 Yates)
        - selection: choose 'none'
        - dist: choose 'empirical'
        - max_phrase_len: fail safe maximum phrase length, usually 50
        - min_phrase_count: minimal phrase count for phrase selection, usually 5 or 10
    """
    read_input_file = read_linearization_to_counts(filenames,max_phrase_len,min_phrase_count)
    # read_input_file is a list of length 2,
    # read_input_file[0] gives dict of one entry: {1, set of all unigrams}
    # read_input_file[1] gives dict of all multi-grams
    topics = DataFrame()
    topics['prob'] = 1

    num_topics = len(topics)
    # Store phrases in a dictionary where length of ngram is the key, value is set of ngrams
    

    print >> sys.stderr, 'Creating candidate n-grams...'
    # Call recursive_summary(l,state, phrases, test, selection, dist, max_phrase_len, min_phrase_count)
    # Also need to check if there are still feasible phrases
    keep_going = True
    l = 2 # phrase length---we already read in single phrases
    longest_phrase = 1
    # Store counts in a dictionary where phrase length is key, value is dict
    # that stores counts for phrases in each specific topic, overall in same 
    # topic, and overall without topic info
    
    phrases = read_input_file[0]   #  gives dict of one entry: {1, set of all unigrams}
    counts = read_input_file[1]    # dict of multigrams
    bf_score_dict = dict()
    
    while keep_going:
        # Test to see if we should keep going
        print l
        if (l <= 2):
            keep_going = True
        else: # Test to see if overlap in phrases
            phrase_level = phrases[l-1]
            phrase_df = DataFrame.from_records([[x.rsplit(' ',1)[0], 
                                                   x.split(' ',1)[1]] for x in 
                                                   phrase_level], columns = (['beginning', 'ending']))
            intersect_list = list(set(phrase_df['beginning']) & set(phrase_df['ending']))
            if (len(intersect_list) == 0):
                keep_going = False
                break
            elif (l > max_phrase_len):
                keep_going = False
                break
        # Call recursive_summary using previous inputs
        recursive_output = recursive_summary(l,phrases, test, selection, dist, 
                                         max_phrase_len, min_phrase_count,counts)
        phrases = recursive_output[0]
        scores = recursive_output[1]
        new_phrases = recursive_output[3]
        counts = recursive_output[4]
        # Store BF scores according to phrase for plotting/debugging
        phrase_and_score = recursive_output[5]
        for idx in xrange(len(phrase_and_score)):
            phrase_value = phrase_and_score[idx][0]
            phrase_score = phrase_and_score[idx][1]
            bf_score_dict[phrase_value] = phrase_score
        if recursive_output[2]:
            longest_phrase = l
        l = l + 1
    
    output_list = list()
    output_list.append(phrases)
    output_list.append(longest_phrase) #in each doc??
    output_list.append(bf_score_dict)  # dictionary?
    output_list.append(counts) # in each doc??
    
    return output_list


def read_linearization_to_counts(filename,max_phrase_len,min_phrase_count):
    # Read in the linearized file
    # Contents should be one line per sentence:
    #   doc number  feature1-feature2:count
    num_topics = 1 # one dummy topic for ease
    # Instantiate counts, phrases
    counts = dict([(ell, defaultdict(lambda: zeros(num_topics + 2, dtype=int))) for ell in xrange(1, max_phrase_len + 1)])
    phrases = dict()
    unigram_dict = dict() #Store word/count values
    # Read in file    
    with open(filename) as f:
        for line in f:
            line_list = line.split()
            # Parse list
            # 3 options: sentence number (single digit), unigram (digit:count), n-gram (digit-digit:count)
            for item in line_list:
                # Break count from items
                if ':' in item:
                    val_temp = item.split(':')
                    count_temp = val_temp[1]
                    feature_mashup = val_temp[0]
                    feature_list_temp = feature_mashup.split('-')
                    # List could have one feature or multiples
                    l = len(feature_list_temp)
                    if (l==1): # single word
                        if feature_list_temp[0] in unigram_dict.keys():
                            unigram_dict[feature_list_temp[0]] += int(count_temp)
                        else:
                            unigram_dict[feature_list_temp[0]] = int(count_temp)
                    else:  # starts from counts[2]
                        counts[l][tuple(feature_list_temp)][num_topics + 1] += int(count_temp)
                        counts[l][tuple(feature_list_temp)][num_topics] += int(count_temp)
                        counts[l][tuple(feature_list_temp)][0] += int(count_temp)
    # Zip unigram_dict into phrases[1]
    phrases[1] = set([key for key, val in unigram_dict.items() if val >= min_phrase_count])
    
    output_list = list()
    output_list.append(phrases)
    output_list.append(counts)
    return(output_list) #output_list is a list of length 2,
    # outputlist[0] gives dict of one entry: {1, set of all unigrams}
    # outputlist[1] gives dict of all multi-grams
 
###############################################################################
 
 
###############################################################################
# CLEAN FEATURE NAMES
   
def replace_stopwords(input_filename, output_filename):
    # Replace stuff that will cause problems with MALLET
    replace_dict = { ' ' : '_',
                     '.' : '_',
                     '0' : 'zero',
                     '1' : 'one',
                     '2' : 'two',
                     '3' : 'three',
                     '4' : 'four',
                     '5' : 'five',
                     '6' : 'six',
                     '7' : 'seven',
                     '8' : 'eight',
                     '9' : 'nine',
                     "'" : '_',
                     '`' : '_',
                     '/' : '_'
    }
    # Make a new output list, with one entry per line
    with open(output_filename,'w') as out_file:
        # Make sure file is empty, in case of overwrites
        out_file.seek(0)
        out_file.truncate()
        # Read in input_file line by line
        with open(input_filename,'r') as in_file:
            for line in in_file:
                # See if we need to replace items in replace dict
                line_temp = copy.copy(line) # Shallow copy
                for item_old, item_new in replace_dict.iteritems():
                    line_temp = line_temp.replace(item_old,item_new)
                # Write the line
                out_file.write("%s" % line_temp)
            # Close it down
        in_file.close()
    out_file.close()
    
def clean_feature_names(feature_key):
    # Run replace_stopwords to make a new feature_key
    # Saving the output to file
    # Read file in and make a list of the output
    new_filename = 'feature_key.txt'
    replace_stopwords(feature_key,new_filename)
    
    feature_names = list()
    with open(new_filename, 'r') as in_file:
        # Remove \n and such from lines
        feature_names = in_file.read().splitlines()
    return feature_names
###############################################################################
    
###############################################################################
# UPDATE COUNTS FOR FEATURES USING BAYES FACTORS
def rectify_counts(features, input_filename, output_filename,max_phrase_len):
    # Inputs:
    #   - features  : a dictionary of features from get_features.py
    #   - input_filename    : path to the feature explorer output
    #   - output_filename   : filename/path for output
    # Outputs:
    #   - feature_output        : raw text file of the same type as input file

    # Read in the linearized file
    # Contents should be one line per sentence:
    #   doc number  feature1-feature2:count
    num_topics = 1 # one dummy topic for ease
    
    # Open output file
    with open(output_filename,'w') as out_file:
        # Make sure file is empty, in case of overwrites
        out_file.seek(0)
        out_file.truncate()
        # Read in file  
        full_write = ""
        with open(input_filename,'r') as in_file:
            for line in in_file:
                line_list = line.split()
                # Parse list
                # 3 options: sentence number (single digit), unigram (digit:count), n-gram (digit-digit:count)
                # IMPORTANT: Redo counts for each sentence  
                # Make a dictionary, counts, that has phrase length as key
                # and a dictionary as an item
                writeable = ""
                counts = dict()
                for l in xrange(1,max_phrase_len+1):
                    counts[l] = dict()
                            
                for item in line_list:
                    # Break count from items
                    # First value needs to be stored again
                    
                    
                    if ':' not in item:
                        # Write down the number
                        writeable += str(item) + " "
                    else: # Scrape some features
                        val_temp = item.split(':')
                        count_temp = int(val_temp[1])
                        feature_mashup = val_temp[0]
                        feature_list_temp = feature_mashup.split('-')
                        # List could have one feature or multiples
                        l = len(feature_list_temp)
                        if tuple(feature_list_temp) in counts[l].keys():
                            # we are good, just update values
                            counts[l][tuple(feature_list_temp)] += count_temp
                        else: # Instantiate entry for that key
                            counts[l][tuple(feature_list_temp)] = count_temp
                # Now to tease out which features are actually there
                
                # Start with longest phrases and work backwards
                for l in xrange(max_phrase_len,1,-1):
                    # Loop through longest phrases and deprecate subphrases
                    # Don't do unigrams because there are no subphrases
                
                    # Kill key value if no phrases of this length are selected
                    if l not in features.keys():
                        del counts[l]
                    else:
                        for key, val in counts[l].items():
                            # Join since l > 1
                            phrase = ' '.join(str(x) for x in key)
                            if phrase in features[l]:
                                # Deprecate lower counts, but keep non-negative
                                counts = deprecate_counts(counts,key,val,l)
                            else:
                                # Remove entry
                                del counts[l][key]
                # Write kept phrase and counts to file
                for l in xrange(1,max_phrase_len+1):
                    if l in counts.keys():
                        counts_temp = counts[l]
                        for key, val in counts_temp.items():
                            if l == 1:
                                phrase = str(key[0])
                            else:
                                phrase = '-'.join(str(x) for x in key)
                            if val > 0:
                                # Make into writeable form
                                writeable += phrase + ':' + str(val) + ' '
                        
                writeable += '\n'
                full_write += writeable
                
        in_file.close()
        out_file.seek(0)
        out_file.write(full_write)
    out_file.close()
    
def deprecate_counts(counts,key,val,l):
    # Deprecate counts of all subphrases
    if l == 1:
        # no subphrases
        return counts
    else:
        # recursion to subphrases
        counts_small = counts[l-1]
        prefix = key[:-1]
        suffix = key[1:]
        counts_small[prefix] -= val
        counts_small[prefix] = max(0,counts_small[prefix])
        counts_small[suffix] -= val
        counts_small[suffix] = max(0,counts_small[suffix])
        counts[l-1] = counts_small
        counts = deprecate_counts(counts,prefix,val,l-1)
        counts = deprecate_counts(counts,suffix,val,l-1)
        return counts

######################
# Make clean version to replace rectify_counts()
# Add input for .graph file
#
# Idea:
#   1. align .graph and linearized feature file by producing two dictionaries
#       between (doc, line) and .graph line number
#   2. read in .graph file and output file that is: doc sen f1-f2-f3:v1 f4:v2... \n
#   3. for each sentence, use graph structure and bf features to get valid subgraphs

def linearization_align(linearization):
    # Inputs:
    #   - linearization:    linearization filename
    #   - graph:            .graph filename
    # Ouputs:
    #   - out_list:    dict in sentence words to (doc, sen)
    out_list = dict()
    # read in linearization file, store set of single words in as keys in dict
    with open(linearization,'r') as linear:
        # read in each line
        for line in linear:
            line_list = line.split()
            set_temp = list()
            val_temp = tuple(line_list[0:2])
            for item in line_list:
                if ':' in item and '-' not in item:
                    # We have a singleton
                    item_temp = item.split(':')
                    set_temp.append(item_temp[0])
            set_temp.sort()
            set_temp = ":".join(set_temp)
            if set_temp not in out_list.keys():
                # Add val_temp to set of values for keys
                out_list[set_temp] = list()
            out_list[set_temp].append(val_temp)
    linear.close()
    return(out_list)

def rectify_counts_graph(phrases, input_linear, input_graph, output_filename,max_phrase_len):
    # Inputs:
    #   - features  : a dictionary of features from get_features.py
    #   - input_linear      : path to the feature explorer linearization output
    #   - input_graph       : path to the feature explorer graph output
    #   - output_filename   : filename/path for output
    # Outputs:
    #   - feature_output        : raw text file of the same type as input file

    # Read in the linearized file
    # Contents should be one line per sentence:
    #   doc number  feature1-feature2:count
    num_topics = 1 # one dummy topic for ease

    # Get a raw dictionary for words in sentence to linearization (doc, sen)
    words2linear = linearization_align(input_linear)

    # Open output file
    with open(output_filename,'w') as out_file:
        # Make sure file is empty, in case of overwrites
        out_file.seek(0)
        out_file.truncate()
        # Read in file
        full_write = ""
        with open(input_graph,'r') as in_file:
            # for debugging:
            #head = [next(in_file) for x in xrange(10)]
            for line in in_file:
                # Split by ';' to get local node 2 feature in first part
                #   local node adjacency in second part
                line_list_graph = line.split(';')
                local_node2feature = line_list_graph[0]
                local_node2feature = local_node2feature.split() # Store as list like ['1:20',...]
                raw_adjacency = line_list_graph[1]
                raw_adjacency = raw_adjacency.split() # Store as list like ['1:20',...]
                node_vals = list()
                for item in raw_adjacency:
                    i_list = item.split(':')
                    node_vals.append(int(i_list[0]))
                num_nodes = max(node_vals) + 1
                #print(num_nodes)
                # Make an adjaceny matrix as list of lists
                adjacency_matrix = [0]*num_nodes
                reverse_adjacency_matrix = [0]*num_nodes
                for idx in xrange(num_nodes):
                    #print(idx)
                    adjacency_matrix[idx] = [0]*num_nodes
                    reverse_adjacency_matrix[idx] = [0]*num_nodes
                for item in raw_adjacency:
                    i_temp = item.split(':')
                    item_in = int(i_temp[0])
                    item_out = int(i_temp[1])
                    adjacency_matrix[item_in][item_out] = 1
                # Make a local2features dict
                single_words = list()
                node2feature_dict = dict()
                for item in local_node2feature:
                    if ':' in item:
                        i_temp = item.split(':')
                        local_node = int(i_temp[0])
                        feature_name = i_temp[1]
                        single_words.append(feature_name)
                        node2feature_dict[local_node] = feature_name
                single_words = list(set(single_words))
                single_words.sort()
                single_words = ":".join(single_words)
                # Crawl graph to get feature counts
                input_set = dict()
                input_set['possible'] = list(xrange(num_nodes))
                for idx, item in enumerate(input_set['possible']):
                    input_set['possible'][idx] = [item]
                input_set['final'] = list()
                trace_list = []
                trace_list = crawl_graph(phrases, adjacency_matrix, reverse_adjacency_matrix, input_set,
                                         node2feature_dict, max_phrase_len)
                trace_set = trace_list['final'] # set of all local paths... need to convert to features for printing
                # make instances into tuples, then set
                trace_set = [tuple(x) for x in trace_set]
                trace_set = set(trace_set)                
                # Find doc, sen
                if single_words in words2linear.keys():
                    # Look up values, take the first for now
                    key_tuple = words2linear[single_words][0]
                    writeable = ""
                    writeable += str(key_tuple[0]) + " " + str(key_tuple[1])
                    # Make a dict of all phrases (keys) and counts (values)
                    phrase_dict = dict()
                    for node_list in trace_set:
                        # convert to phrases
                        phrase_temp = []
                        for node in node_list:
                            phrase_temp.append(str(node2feature_dict[node]))
                        phrase_temp = "-".join(phrase_temp)
                        #print(phrase_temp)
                        if phrase_temp in phrase_dict.keys():
                            phrase_dict[phrase_temp] += 1
                        else:
                            phrase_dict[phrase_temp] = 1
                    #print(phrase_dict)
                    for phrase, val in phrase_dict.iteritems():
                        writeable += " " + phrase + ":" + str(val)
                    writeable += "\n"
                    full_write += writeable
                        
        in_file.close()
        out_file.seek(0)
        out_file.write(full_write)
    out_file.close()                


def crawl_graph(phrases, adjacency_matrix, reverse_adjacency_matrix, input_set, node2feature_dict, max_phrase_len):
    # Inputs:
    #   - features:     set of features from BF
    #   - adjacency_matrix: adjacency matrix
    #   - reverse_adjacency_matrix: reverse linkages for computational efficiency
    #   - input_set:    dictionary to store parts of final features in 'possible' and feature traces in 'final'
    #   - node2feature_dict:   looks up node name as key and returns feature name
    #   - max_phrase_len:   longest graph trace
    # Outputs:
    #   - output_set: with same structure as input_set
    output_set = dict()
    output_set['possible'] = list()
    output_set['final'] = list(input_set['final'])
    num_nodes = len(adjacency_matrix)
    # Go through 'possible' inputs and determine if each should be added to 
    # final or if extensions should be added to 'possible'
    
    final_process = False
    if len(input_set['possible']) == 0:
        final_process = True
    else:
        l = len(input_set['possible'][0])
        if l >= max_phrase_len:
            final_process = True
            output_set['final'].extend(input_set['possible'])
    if not final_process:
        l += 1
        for item in input_set['possible']:
            possible_temp = list()
            # Get a list of all node extensions
            is_possible = False
            # Search possible additions
            for idx in xrange(num_nodes):
                # Search end additions
                node_path = list(item)
                #print(node_path)
                last_val = item[-1]
                if adjacency_matrix[last_val][idx] > 0:
                    node_path.extend([idx])
                    # Check against phrase list
                    feature_temp_list = []
                    #print(node_path)
                    for node in node_path:
                        # Get feature number
                        feature_temp_list.append(node2feature_dict[node])
                    #print('feature_temp_list: ')
                    #print(feature_temp_list)
                    feature_temp = " ".join(feature_temp_list)
                    #print('feature_temp: ' + feature_temp)
                    if feature_temp in phrases[l]:
                        # Add to possible features
                        is_possible = True
                        possible_temp.append(node_path)
                        output_set['possible'].append(node_path)
                node_path = []
                first_val = item[0]
                if reverse_adjacency_matrix[first_val][idx] > 0:
                    item_temp = list(item)
                    node_path = item_temp.insert(0,idx)
                    feature_temp_list = []
                    for node in node_path:
                        # Get feature number
                        feature_temp_list.append(node2feature_dict[node])
                    #print('feature_temp_list: ')
                    #print(feature_temp_list)
                    feature_temp = " ".join(feature_temp_list)
                    #print('feature_temp: ' + feature_temp)
                    
                    if feature_temp in phrases[l]:
                        # Add to possible features
                        is_possible = True
                        print('added: ' + str(node_path))
                        possible_temp.append(node_path)
                        output_set['possible'].append(node_path)
            # Add the results to the appropriate parts of the output
            if not is_possible:
                output_set['final'].append(item)
        # Now call graph_crawl again to make this bigger
        output_set = crawl_graph(phrases, adjacency_matrix, reverse_adjacency_matrix, output_set, node2feature_dict, max_phrase_len)
    #else: # Process for final output---convert list to set to remove duplicates
        #print(input_set['possible'])        
        #print(output_set['final'])
        #output_set['final'] = set(output_set['final'])
        
    return(output_set)
            
                
                    
                    

######################

def make_mallet_folders(input_trunk_path,feature_doc_list,feature_names,doc_names):
    # Inputs:
    #   - set of input documents (may be empty)
    #   - corrected counts for features for a doc list
    #   - document that has a set of names for the features (line = num)
    #   - document that has path for each document number (line = num)
    # Outputs:
    #   - creates three sets of files in folders   
    
    # Make a feature number to feature name mapping
    feature_name_dict = dict()
    line_counter = 0
    with open(feature_names,'r') as f_names:
        for line in f_names:
            feature_name_dict[line_counter] = line.rstrip()
            line_counter += 1
    f_names.close()
    
    
    # Read in all of the feature_doc_list and store in a dict by document number
    feature_set_dict = dict()
    with open(feature_doc_list,'r') as f_list:
        for line in f_list:
            # Get document number, which is the first value
            line_list = line.split()
            doc = int(line_list[0])
            feature_set = [x for x in line_list if ":" in x]
            if doc in feature_set_dict.keys():
                # append feature_set to existing list
                feature_set_old = feature_set_dict[doc]
                feature_set.extend(feature_set_old)
            feature_set_dict[doc] = feature_set
    f_list.close()
    
    # Now read in documents and make 2 copies: 
    #   - just features
    #   - features + text
    
    # Read in each document
    with open(doc_names,'r') as d_list:
        for idx, line in enumerate(d_list):
            # idx is the document number
            # line is the name of the document
            
            # Pull the features for document idx
            # Make a list of them for appending
            append_value = ""
            if idx in feature_set_dict.keys():
                # We have more stuff to append
                for item in feature_set_dict[idx]:
                    # Break it up
                    feature_val = item.split(":")
                    feature_mash = feature_val[0]
                    val = feature_val[1]
                    feature_list = feature_mash.split("-")
                    for f_idx, f in enumerate(feature_list):
                        feature_list[f_idx] = feature_name_dict[int(f)]
                    feature_mash = "-".join(str(x) for x in feature_list)
                    for i in xrange(1,int(val)+1):
                        append_value += feature_mash + " "
            # Make docs with just append_value, and orig + append_value
            with open(input_trunk_path + "/features/" + line.rstrip(),'w') as f:
                f.write(append_value)
            f.close()
            # Append to document
            copyfile(input_trunk_path+"/plain_docs/"+line.rstrip(), input_trunk_path + 
                        "/docs_features/"+line.rstrip())
            with open(input_trunk_path + "/docs_features/"+line.rstrip(),'a') as f:
                f.write(append_value)
            f.close()
    d_list.close()
    
#    # Move everything in those folders into csv's for easy use of shell script
#    names_list = ["/features","/plain_docs","/docs_features"]
#    for name in names_list:
#        with open(input_trunk_path + "/csv" + name + ".csv",'w') as f:
#            # Read in all files
#            contents = ""
#            file_list = glob.glob(input_trunk_path + name + "/*.txt")
#            for idx, file_name in enumerate(file_list):
#                # Make first entry a document number
#                contents += "doc"+str(idx) + ","
#                with open(file_name, 'r') as txt:
#                    for line in txt:
#                        # Read and strip new line
#                        contents += " " + line.rstrip()
#                txt.close()
#                # File has been read in, add \n between documents
#                contents += "\n"
#            # Save results as csv
#            f.write(contents)
#        f.close()
                        
            
    
                
                
                
            
        
###############################################################################    

def get_features_wrapper(input_docs, linearization, input_graph, feature_key, 
                         doc_key, test, selection, dist, max_phrase_len, 
                         min_phrase_count):
    # Run:
    #   1. get_features_bf
    #   2. clean_feature_names
    #   3. append_features
    
    #################################
    # Part 1
    feature_list = get_features_bf(linearization, test, selection, dist, max_phrase_len,
                     min_phrase_count)
    features = feature_list[0]
    #################################
    
    
    #################################
    # Part 2
    # Save cleaned feature key to file, return list of feature names in dict
    omnigraph_names = clean_feature_names(feature_key)
    #################################
    
    
    #################################
    # Part 3
    # Change the counts and add feature names to the document
    # Do this for ONE DOCUMENT AT A TIME
    # Note: results are currently approximate, but close due to using single docs
    corrected_count_filename = 'corrected_linearization.txt'
    # The output is saved to a file with the same format as the linearized features
    # Old version:
    #rectify_counts(features,linearization,corrected_count_filename,max_phrase_len)
    # New version:
    rectify_counts_graph(features,linearization,input_graph,corrected_count_filename,max_phrase_len)
    # Read in stuff and write outputs for MALLET
    make_mallet_folders(input_docs,corrected_count_filename,feature_key,doc_key)
    # Turn the files into csv's with one doc per line so that shell script can be reused
    # Save into a new folder in MALLET/csv
    
                

def main():
    
    tests = {
        'bayes-conditional': bfc,
        'bayes-unconditional': bfu,
        'chi-squared-yates': csy
    }
    
    p = ArgumentParser(formatter_class=ArgumentDefaultsHelpFormatter)
    
    p.add_argument('--input_docs', type=str, metavar='<filename>', required=True,
                   help='folder of input documents')
    p.add_argument('--linearization', type=str, metavar='<filename>', required=True,
                   help='file of linearized omnigraph features')
    p.add_argument('--input_graph', type=str, metavar='<filename>', required=True,
                   help='file of plain text omnigraph features')
    p.add_argument('--feature_key', type=str, metavar='<filename>', required=True,
                   help='list of omnigraph feature names')
    p.add_argument('--doc_key', type=str, metavar='<filename>', required=True,
                   help='list of document names')
    p.add_argument('--test', metavar='<test>', required=True,
                   choices=['bayes-conditional', 'bayes-unconditional',
                            'chi-squared-yates'],
                   help='hypothesis test for phrase generation')
    p.add_argument('--selection', metavar='<selection>', required=True,
                   choices=['none', 'bigram', 'n-1-gram'],
                   help='additional selection criterion')
    p.add_argument('--dist', metavar='<dist>', required=True,
                   choices=['average-posterior', 'empirical', 'prior'],
                   help='distribution over topics')
    p.add_argument('--max-phrase-len', type=int, metavar='<max-phrase-len>',
                   default=3, help='maximum phrase length')
    p.add_argument('--min-phrase-count', type=int,
                   metavar='<min-phrase-count>',
                   default=15, help='minimum phrase count')

    args = p.parse_args()
    # Example values:
    #   - input_docs = '../MALLET'
    #   - linearization = '../data/March_07_test/feature_3nodes_debug.txt'
    #   - input_graph = '../data/March_07_test/10K_2016-03-07graph_remove00_explorer.txt'
    #   - feature_key = '../data/March_07_test/new_test_graph_nodeid.txt'
    #   - doc_key = '../data/March_07_test/doc_numbers.txt'
    #   - test = 'bayes-conditional'
    #   - selection = 'none'
    #   - dist = 'empirical'
    #   - max-phrase-len = 3
    #   - min-phrase-count = 15

    try:
        get_features_wrapper(args.input_docs, args.linearization, args.input_graph,
                             args.feature_key,args.doc_key, tests[args.test], 
                                args.selection, args.dist, 
                                args.max_phrase_len, args.min_phrase_count)
    except AssertionError:
        p.print_help()
    



if __name__ == "__main__":
    main()
    


















        