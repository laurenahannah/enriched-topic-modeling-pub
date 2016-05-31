# -*- coding: utf-8 -*-
"""
Created on Tue Apr 26 15:23:37 2016

@author: lhannah

Script to run get_features.py for all subfolders in a directory for GICS index

Assuming:
    - doc_key: TICKER.docid
    - feature_key: TICKER.graph.nodeid
    - linearization: TICKER.linear
    - graph_input: TICKER.graph
    - input_docs: "../MALLET/Reuters/gicsSECTOR/xxx" with xxx subfolder

"""
import get_features
import os

def gics_bf(data_trunk,mallet_trunk):
    # Inputs:
    #   - data_trunk: path to gics folder with data files
    #   - mallet_trunk:     path to folder with MALLET files
    # Outputs:
    #   - all get_features.py outputs for each subfolder in data_trunk

    # Find ticker, subfolders
    dir_list = [x[0] for x in os.walk(data_trunk)]
    # should have entries like:
    # ['../data/Reuters/gics15/',
    # '../data/Reuters/gics15/AA',
    # '../data/Reuters/gics15/APD',...
    # Use this to get a list of files
    list_of_parts = list()
    max_len = 0
    for idx, item in enumerate(dir_list):
        item_spl = item.split("/")
        if len(item_spl) > max_len:
            max_len = len(item_spl)
        list_of_parts.append(item_spl)
    # If we are at least max_len, run get_features on directory
    for idx, item in enumerate(dir_list):
        if len(list_of_parts[idx]) == max_len:
            # Good to go
            parts_temp = list_of_parts[idx]
            ticker = parts_temp[-1]
            # Use ticker value to make filenames for feature_key, etc
            input_docs = os.path.join(mallet_trunk,ticker)
            print("Input docs: " + input_docs)
            linearization = os.path.join(data_trunk, ticker, ticker + '.linear')
            print("Linearization: " + linearization)
            input_graph = os.path.join(data_trunk, ticker, ticker + '.graph')
            print("Input graph: " + input_graph)
            feature_key = os.path.join(data_trunk, ticker, ticker + '.graph.nodeid')
            print("Feature key:" + feature_key)
            doc_key = os.path.join(data_trunk, ticker, ticker + '.docid')
            print("Doc key: " + doc_key)
            selection = 'none'
            dist = 'empirical'
            max_phrase_len = 3
            min_phrase_count= 15
            get_features_wrapper(input_docs, linearization, input_graph, feature_key, 
                         doc_key, bfc, selection, dist, max_phrase_len, 
                         min_phrase_count)
        
def main():
    p = ArgumentParser(formatter_class=ArgumentDefaultsHelpFormatter)
    p.add_argument('--data_trunk', type=str, metavar='<filename>', required=True,
                   help='folder of data input (usually in data folder)')
    p.add_argument('--mallet_trunk', type=str, metavar='<filename>', required=True,
                   help='folder of document input (usually in mallet folder)') 
                   
    args = p.parse_args()
    # Example values:
    #   - data_trunk = '../data/Reuters/gics15'
    #   - mallet_trunk = '../MALLET/Reuters/gics15'
    
    try:
        gics_bf(args.data_trunk, args.mallet_trunk)
    except AssertionError:
        p.print_help()
    



if __name__ == "__main__":
    main()

