# -*- coding: utf-8 -*-
"""
Created on Tue Mar 22 19:48:50 2016

feature_viewer: outputs sententes with your graph feature of choice from the 
MALLET topic summaries

outputs are saved as csv; for each sentence we get:
    - document name
    - sentence number
    - sentence

@author: lhannah
"""
import csv
from numpy import exp, log, log2, percentile, power, zeros, arange, matrix
from os import listdir
from os.path import isfile, join

def feature_viewer(path_list_csv,feature,topic):
    # Store the following information in a csv with paths from this folder:
    #   - input_docs
    #   - linearization
    #   - feature_key
    #   - doc_key
    #   - doc_topics
    #   - topic_words
    #   - vocab
    # feature is the item that you care about as it appears in MALLET
    
    # Plan:
    #   - compute probability for the feature in each document using MALLET outputs
    #   - threshold and get docs with high enough probability
    #   - get key between feature names and feature numbers
    #   - pull sentences in selected docs with that feature
    #   - write to output file
    
    ###########################
    # Read in data from path list
    #
    # First line: input_docs, a trunk for folder with all documents
    
    with open(path_list_csv,'r') as csvfile:
        counter = 0
        for row in csvfile:
            row = row.rstrip()
            if counter == 0:
                input_docs = "".join(row)
            elif counter == 1: # linearization
                linearization = "".join(row)
            elif counter == 2:
                feature_key = "".join(row)
            elif counter == 3:
                doc_key = "".join(row)
            elif counter == 4:
                doc_topics = "".join(row)
            elif counter == 5:
                topic_words = "".join(row)
            elif counter == 6:
                vocab = "".join(row)
            counter += 1    
    # Debugging
    #print(input_docs)
    #print(linearization)
    #print(feature_key)
    #print(doc_key)
    #print(doc_topics)
    #print(topic_words)
    #print(vocab)
    # Get list of MALLET labels for docs with high feature probability
    thresh = 0.5 # threshold for feature selection
    MALLET_doc_list = make_feature_prob(doc_topics,topic_words,vocab,feature,topic,thresh)
    print(MALLET_doc_list)    
    
    # Decide if feature is an omnigraph feature or plain text
    plain_text = True
    if "_" in feature and "-" in feature:
        plain_text = False
    # If the feature is plain text:
    #   - pull out sentences within the docs
    # If the feature is omnigraph:
    #   - use keys to map back to documents
    if plain_text:
        print("plain_text")
        make_plain_text_output(MALLET_doc_list,input_docs,feature)
    else:
        print("omnigraph")
        make_omnigraph_output(MALLET_doc_list,input_docs,linearization,feature_key,doc_key,feature)
        
    # Map the MALLET document list to the doc list---there may be some docs
    # without omnigraph 
    #MALLET_to_Omni_doc_key = MALLET_to_omni(input_docs,doc_key)
    
def make_feature_prob(doc_topics,topic_words,vocab,feature,topic_in,thresh):
    # Read in the necessary documents
    # Map the vocab file to the probabilities columns
    vocab_dict = dict()
    with open(vocab) as csvfile:
        vocab_reader = csv.reader(csvfile)
        counter = 0
        for row in vocab_reader:
            if counter > 0:
                vocab_dict["".join(row)] = counter - 1
            counter += 1
    
    feature_ind = vocab_dict[feature]
    # Read in the topic probabilities for each document
    topic_word_mat = numpy.loadtxt(open(topic_words,'rb'), delimiter = ",", skiprows = 1)
    t_w_m_shape = topic_word_mat.shape
    n_topic = t_w_m_shape[0]
    n_word = len(vocab_dict)
     
    doc_topic_mat = numpy.loadtxt(open(doc_topics,'rb'), delimiter = ",", skiprows = 1)
    d_t_m_shape = doc_topic_mat.shape
    n_doc = d_t_m_shape[0]
    
    # Make a list that stores the indicies of documents that have the feature
    out_list = []
    for doc in xrange(n_doc):
        # Compute p(t|doc,feature)
        #  \propto p(w|t) p(t|doc)
        log_pvec = [0]*n_topic
        for topic in xrange(n_topic):
            log_pvec[topic] = log(topic_word_mat[topic,feature_ind]) + log(doc_topic_mat[doc,topic])
        max_val = max(log_pvec)
        # renormalize for numerical stability
        # so that largest log probability is 0
        pvec = [0]*n_topic
        for topic in xrange(n_topic):
            log_pvec[topic] -= max_val
            pvec[topic] = exp(log_pvec[topic])
        pval = pvec[topic_in]/sum(pvec)
        # keep if probability greater than threshold
        if pval >= thresh:
            out_list.append(doc)
    return(out_list)

def make_plain_text_output(MALLET_doc_list,input_docs,feature):
    # MALLET reads in all of the files in the input_docs folder and assigns them
    # a number alphabetically
    
    # Get list of files in input_docs, store alphabetically
    files_in_dir = [f for f in listdir(input_docs) if '.txt' in f]
    files_in_dir = sorted(files_in_dir, key=str.lower)
    
    #with open(input_trunk_path + "/csv" + name + ".csv",'w')
    
    # Pull out the files from MALLET_doc_list
    for item in MALLET_doc_list:
        filename = join(input_docs,files_in_dir[item])
        print(filename)
        # Go through docs line by line
        with open(filename,'r') as doc:
            for line in doc:
                if feature.lower() in line.lower():
                    print(filename + "   :::::   " + line)
        doc.close()
        
def make_omnigraph_output(MALLET_doc_list,input_docs,linearization,feature_key,doc_key,feature):
    # This is a pain.
    
    # MALLET has a different file ordering than omnigraph, so we will make a key
    mallet_key = MALLET_key(input_docs)
    mallet_id2file = mallet_key[0]
    mallet_file2id = mallet_key[1]
    
    # Make a feature key for omnigraph as well
    omni_key = omnigraph_key(doc_key)
    omni_id2file = omni_key[0]
    omni_file2id = omni_key[1]
    
    # Use the same function to make a feature key
    f_list = omnigraph_key(feature_key)
    feature_id2plain = f_list[0]
    feature_plain2id = f_list[1]
    # Make keys lower case
    feature_plain2id = dict((k.lower(),v) for k,v in feature_plain2id.items())    
    
    # Get the raw number of the input feature
    # NEED TO STRIP COMPOUNDING AND THEN RECODE!!!!!!!  
    feature_split = feature.split('-')
    # super annoying fun fact: string put back together with -'s, which also
    # mash together parts
    frame_elements = ['lexical_item', 'frame_target', 'frame_name', 'frame_element']
    counter = 0
    feature_list = [] #store the features we care about
    for item in feature_split:
        # Look at pairs of items
        if counter < (len(feature_split) - 1):
            item2 = feature_split[counter + 1]
            if item.lower() in frame_elements and item2.lower() not in frame_elements:
                # we have a feature, so mash it back together with '-'
                feature_temp = item + '-' + item2
                feature_list.append(feature_temp)
        counter += 1
    feature_id = []
    print(feature_list)
    for item in feature_list:
        feature_id_temp = feature_plain2id[item]
        feature_id.append(str(feature_id_temp))
    feature_old = '-'.join(feature_id) # for use in linearization... le sigh...
    print(feature_old)
    
    # Go through MALLET_doc_list to get high probability documents
    omni_number_list = []
    for mallet_doc in MALLET_doc_list:
        # go from mallet number to omnigraph number
        # note: the latter might not exist
        doc_plain = mallet_id2file[mallet_doc]
        omni_number = - 1
        if doc_plain in omni_file2id.keys():
            omni_number = omni_file2id[doc_plain]
        omni_number_list.append(omni_number)
    print(omni_number_list)
    # Read in documents---linearization to find sentences, all docs to print sentence
    # Save document as key, list of sentences to print out
    sentences4print = dict()
    with open(linearization,'r') as linear:
        for line in linear:
            line_list = line.split()
            # First two elements are omni_doc and line number
            omni_doc = int(line_list[0])
            sentence = int(line_list[1])
            
            if feature_old in line and int(omni_doc) in omni_number_list:
                # Translate to document
                filename = omni_id2file[omni_doc]
                # store sentences in list attached to doc key
                # this makes reading quicker, groups documents
                if filename in sentences4print.keys():
                    sent_list = sentences4print[filename]
                    sent_list.append(sentence)
                    sentences4print[filename] = sent_list
                else:
                    sentences4print[filename] = [sentence]
    linear.close()
    print(len(sentences4print))
    for doc in sentences4print.keys():
        filename = join(input_docs,doc)
        sentences = sentences4print[doc]
        with open(filename,'r') as f:
            for idx, line in enumerate(f):
                # NOTE: in linearization, sentence counts seem to start from 1
                # Fix on 3/30/16 (temporary): add 1 to index
                # NOTE: changed back to start from 0 on 4/6/16
                if idx in sentences:
                    print(filename + "  ::::   " + str(idx) + "   :::::   " + line)
                    
                                    
    
        
def MALLET_key(input_docs):
    # MALLET reads in all of the files in the input_docs folder and assigns them
    # a number alphabetically
    # Store MALLET id to file, file to id
    
    id2file = dict()
    file2id = dict()
    
    # Get list of files in input_docs, store alphabetically
    files_in_dir = [f for f in listdir(input_docs) if '.txt' in f]
    files_in_dir = sorted(files_in_dir, key=str.lower) # MALLET order
    
    # Read
    counter = 0
    for item in files_in_dir:
        id2file[counter] = item.rstrip()
        file2id[item.rstrip()] = counter
        counter += 1
    key_list = []
    key_list.append(id2file)
    key_list.append(file2id)
    return(key_list)
    
def omnigraph_key(doc_key):
    # omnigraph uses a file key
    # Store omnigraph id to file, file to id
    
    id2file = dict()
    file2id = dict()
    
    # Read
    counter = 0
    with open(doc_key,'r') as f:
        for item in f:
            id2file[counter] = item.rstrip()
            file2id[item.rstrip()] = counter
            counter += 1
    # Store
    key_list = []
    key_list.append(id2file)
    key_list.append(file2id)
    return(key_list)