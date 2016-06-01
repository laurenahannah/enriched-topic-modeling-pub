"""
Created on Fri Mar 11 2016

Puroose: 
Parse Onigraph input document (graphname = '10K_2016-03-07graph.txt') to 
1) remove lines (graphs) starting from 0 0: those lines (Omnigraph) whose sentence counterpart couldn't be found, so we can ignore them
the graph after removing 00 lines are generated as output: modified_graph_outputname = '10K_2016-03-07graph_remove00.txt'
2) based on graph after removing 00 lines (modified_graph_outputname = '10K_2016-03-07graph_remove00.txt'), 
this script will then remove doc_ID and sentence_ID for each graph (each line), to serve as proper input for Boyi's code GraphFeatureExplorer.java
The output of step 2) is input_for_GraphFeatureExplorer = '10K_2016-03-07graph_remove00_explorer.graph'

Note: .graph document for Omnigraph must be saved as .txt format before sending to this script 

@author: Ruilin_Zhong
"""


def remove_00(graphname, modified_graph_outputname):
    graph = open(graphname, 'r')
    content = [] # for modified_graph_outputname = '10K_2016-03-07graph_remove00.txt'
    content_explorer = [] # for input_for_GraphFeatureExplorer = '10K_2016-03-07graph_remove00_explorer.graph'
    for line in graph:
        line_list = line.split() # split each line by space or tab, line_list is list of elements
        if line_list[0:2] == ['0', '0']:
            continue
        else:
            line_append = ' '.join(line_list)
            content.append(line_append)   # for modified_graph_outputname = '10K_2016-03-07graph_remove00.txt'
            line_list_explorer = line_list[2:] # get rid of doc_ID and sentence_ID
            line_explorer_append = ' '.join(line_list_explorer)
            content_explorer.append(line_explorer_append) # for input_for_GraphFeatureExplorer = '10K_2016-03-07graph_remove00_explorer.graph'

    with open(modified_graph_outputname, mode="wb") as outfile:
        for item in content:
            outfile.write(item + '\n')

    with open(input_for_GraphFeatureExplorer, mode='wb') as output:
        for stuff in content_explorer:
            output.write(stuff + '\n')



if __name__ == "__main__":

    graphname = '10K_2016-03-07graph.txt'
    modified_graph_outputname = '10K_2016-03-07graph_remove00.txt'
    input_for_GraphFeatureExplorer = '10K_2016-03-07graph_remove00_explorer.graph'

    remove_00(graphname, modified_graph_outputname)










