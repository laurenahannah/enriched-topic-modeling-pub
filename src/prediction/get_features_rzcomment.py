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
            with open(input_trunk_path + "/features/omnigraph" + str(idx) + ".txt",'w') as f:
                f.write(append_value)
            f.close()
            # Append to document
            copyfile(input_trunk_path+"/plain_docs/"+line.rstrip(), input_trunk_path + 
                        "/docs_features/"+line.rstrip())
            with open(input_trunk_path + "/docs_features/"+line.rstrip(),'a') as f:
                f.write(append_value)
            f.close()
    d_list.close()
    
    # Move everything in those folders into csv's for easy use of shell script
    names_list = ["/features","/plain_docs","/docs_features"]
    for name in names_list:
        with open(input_trunk_path + "/csv" + name + ".csv",'w') as f:
            # Read in all files
            contents = ""
            file_list = glob.glob(input_trunk_path + name + "/*.txt")
            for idx, file_name in enumerate(file_list):
                # Make first entry a document number
                contents += "doc"+str(idx) + ","
                with open(file_name, 'r') as txt:
                    for line in txt:
                        # Read and strip new line
                        contents += " " + line.rstrip()
                txt.close()
                # File has been read in, add \n between documents
                contents += "\n"
            # Save results as csv
            f.write(contents)
        f.close()
                        
            
    
                
                
                
            
        
###############################################################################    

def get_features_wrapper(input_docs, linearization, feature_key, doc_key, test, 
                         selection, dist, max_phrase_len, min_phrase_count):
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
    rectify_counts(features,linearization,corrected_count_filename,max_phrase_len)
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
                   help='folder of input documents')    # path of the folder 
    p.add_argument('--linearization', type=str, metavar='<filename>', required=True,
                   help='folder of linearized omnigraph features')     # boyi;s output of features
    p.add_argument('--feature_key', type=str, metavar='<filename>', required=True,
                   help='list of omnigraph feature names')      # newtestgraphnodeid.txt
    p.add_argument('--doc_key', type=str, metavar='<filename>', required=True,
                   help='list of document names')        # doc_numberscore.txt from hooshmond
    p.add_argument('--test', metavar='<test>', required=True,
                   choices=['bayes-conditional', 'bayes-unconditional',
                            'chi-squared-yates'],        # 'bayes-conditional'
                   help='hypothesis test for phrase generation')
    p.add_argument('--selection', metavar='<selection>', required=True,
                   choices=['none', 'bigram', 'n-1-gram'],   # 'none'
                   help='additional selection criterion')       
    p.add_argument('--dist', metavar='<dist>', required=True,       
                   choices=['average-posterior', 'empirical', 'prior'],
                   help='distribution over topics')    # 'empirical'
    p.add_argument('--max-phrase-len', type=int, metavar='<max-phrase-len>',
                   default=3, help='maximum phrase length')  # 3
    p.add_argument('--min-phrase-count', type=int,
                   metavar='<min-phrase-count>',
                   default=15, help='minimum phrase count')      # 15 smaller then more longer graph feature 

    args = p.parse_args()

    try:
        get_features_wrapper(args.input_docs, args.linearization, args.feature_key,
                             args.doc_key, tests[args.test], args.selection, args.dist, 
                                args.max_phrase_len, args.min_phrase_count)
    except AssertionError:
        p.print_help()
    



if __name__ == "__main__":
    main()






# put the following in the same foler:
# 1) all 4819 raw text documents needed to be appended
# 2) Hooshmand data: 10K.graph, that ONE FILE has document_ID, sentence_ID, graph_data (need to send it to Boyi's graph feature explorer .java to get features)
# 3) Boyi's output of feature_3nodes.txt, BUT!!! this is the modified version, with Doc_ID Sent_ID feature_lists_and_counts
# 4) Hooshmand data: doc_numberscore.txt 
# 5) Hooshmand data: newtestgraphnodeid.txt




# CCLS network to get data:
# Hooshmand RAW DOCUMENTS: data/edgar/10K (n=291)
# Hooshmand GRAPH: data/edgar/10K-proc/10K.graph, 10K.
# WHERE MY CODE AND LAUREN'S CODE GO: rc/preprocessing/omnigraph  (INCLUDING BOYI'S CODE????)



















