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
from get_features import *
import os
from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter

def gics_bf(data_trunk, graph_name, mallet_trunk):
    # Inputs:
    #   - data_trunk: path to gics folder with data files
    #   - mallet_trunk:     path to folder with MALLET files
    # Outputs:
    #   - all get_features.py outputs for each subfolder in data_trunk

    # Good to go

    input_docs = os.path.join(mallet_trunk)
    print("Input docs: " + input_docs)
    linearization = os.path.join(data_trunk, "omnigraph", graph_name + '.linear')
    print("Linearization: " + linearization)
    input_graph = os.path.join(data_trunk, "omnigraph", graph_name + '.graph')
    print("Input graph: " + input_graph)
    feature_key = os.path.join(data_trunk, "omnigraph", graph_name + '.graph.nodeid')
    print("Feature key:" + feature_key)
    doc_key = os.path.join(data_trunk, "omnigraph", graph_name + '.docid')
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
    p.add_argument('--graph_name', type=str, metavar='<filename>', required=True,
                   help='name of omnigraph files in omnigraph output folder') 
    p.add_argument('--mallet_trunk', type=str, metavar='<filename>', required=True,
                   help='folder of document input (usually in mallet folder)') 
                   
    args = p.parse_args()
    # Example values:
    #   - data_trunk = '../data/Reuters/gics15'
    #   - mallet_trunk = '../MALLET/Reuters/gics15'
    
    try:
        gics_bf(args.data_trunk, args.graph_name, args.mallet_trunk)
    except AssertionError:
        p.print_help()
    



if __name__ == "__main__":
    main()

