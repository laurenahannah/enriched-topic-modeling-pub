"""
Created on Sat Mar 5 2016
Match document_ID and sentence_ID in input_graph.txt, to each sentence's feature output.
Example lines in output file 'modified_psuedo_features.txt':
    0 0 11268-238-1028:2 9742-232-788:1 7383-167-607:1 8539-380-1028:1 5858-238-1028:2 4661-1028:2 6278-104-634:1 6747-238-1028:2 10807-167-607:1, ...
    # document0, sentence0
    9 2 10329-168-811:1 11249-88-544:1 8527-168-811:1 10807-168-811:1 11135-168-811:1 8542-168-811:1 8356-238-1028:1 9323-168-811:1, ...
    # document9, sentence2
@author: Ruilin_Zhong
"""
import sys


def match(input_file, feature_file, write_output):
    doc_id = []
    sen_id = []
    f = open(input_file, 'r')
    for line in f:
        line_list = line.split()
        doc_id.append(line_list[0]) # list of string for document ID
        sen_id.append(line_list[1]) # list of string for sentene ID in each document

    content = []
    outf = open(feature_file, 'r')
    for i, l in enumerate(outf):
        l_list = l.split()
        new_line = doc_id[i]+' '+sen_id[i]+' '+" ".join(l_list[1:])
        content.append(new_line)

    with open(write_output, mode="wb") as outfile:
        for item in content:
                outfile.write(item + '\n')   # '\n' writes each string on different lines



if __name__ == "__main__":

    input_file = sys.argv[1] #'10K_2016-03-13graph_remove00.txt'
    feature_file = sys.argv[2] #'feature_3nodes_10kfull.txt'
    write_output = sys.argv[3] #'BFfull10K_3features.txt'

    match(input_file, feature_file, write_output)