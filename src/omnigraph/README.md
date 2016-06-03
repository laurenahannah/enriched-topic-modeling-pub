
# Omnigraph

### Part 1: Create Omnigraph

	INPUTS:
	- <INDIR>: plain text file with .txt extension. Each .txt file will be treated as a document
 
	OUTPUTS:
	- <OUTDIR>/*.seg: One sentence per line files that correspond to the original plain text files
	- <OUTDIR>/dep/: individual sentences dependency parses
	- <OUTDIR>/sem/: individual sentences frame semantic parses
	- <OUTDIR>/omnigraph/<name-of-graph>.graph: Omnigraph file where each file is the graph representation of a sentence in the corpus
	- <OUTDIR>/omnigraph/<name-of-graph>.graph.nodeid: Mapping from node ID's to semantic roles
	- <OUTDIR>/omnigraph/<name-of-graph>.docid: Mapping from graphs to their corresponding documents/lines
	- <OUTDIR>/omnigraph/<name-of-graph>.doc: Mapping from graphs to their corresponding frame semantic and depency parses


STEPS to run process in order to get omnigraph files from plain .txt files:

1) The config file contains environment variables that has to be set (e.g. SEMAFOR_HOME). The config file contains comments explaining the options.

2) Run process.h with the following options

```sh
$  sh process.sh <task> <INDIR> <OUTDIR> [<name-of-graph>]
```


	- <task> = seg : segment the raw text
	-	       tok : tokenize and part of speech tag
	-	       dep : dependency parse
	-	       sem : semafor parse
	-	       omn : generating omnigraph
	- <INDIR>: absolute path that contains the plain .txt files
	- <OUTDIR>: absolute path to the place that all the intermediate and resulting files from the pipeline is saved
	- <name-of-graph>: name of the output graph files


a)
```sh
$ sh process.sh seg <INDIR> <OUTDIR>
```

The first steps creates a one to one mapping from original .txt files to .seg files which contain a sentence per each line.
For example, for INDIR/1.txt there would be a OUTDIR/1.seg

b)
```sh
$ sh process.sh tok <INDIR> <OUTDIR>
$ sh process.sh dep <INDIR> <OUTDIR>
$ sh process.sh sem <INDIR> <OUTDIR>
```

These three command create the temporary tokenization, part of speech tagging, dependency parses and frame semantic parses that are necessary for generating the Omnigraph. 

```sh
$ sh process.sh omn <INDIR> <OUTDIR> <name-of-graph>
```
This final command creates the Omnigraph files.

----------------------------------

### Part 2: Linearize Omnigraph Features

# README 
## Purpose
This pipeline takes Omnigraph graph data and gives output of a file that specifies feature names and each feature's count for each sentence.
The final output of this pipeline is a .txt file ready to be used as the input document for get_features.py.

## Directories
The directory of this pipeline, pipeline_get_input_for_bf, in CCLS network is as follows:
/export/projects/enriched-topic-modeling/data/edgar/pipeline_get_input_for_bf

In “pipeline_get_input_for_bf”, there are 4 sub-directories: 

- data: contains the .graph data provided by Hooshmand (10K_2016-03-13.graph)
- “script": contains 2 .py python script and 1 bash script (prepare_graph_forexplorer.py, match_doc_sen.py, run.sh)
- “bin”: contains all the java classes needed for configuring and running Boyi’s GraphFeatureExplorer.java
- “output”: the directory that we wish to put the final output of feature counts (i.e. input file used in Lauren’s BF script). Before running the bash script run.sh, the “output” directory is empty. 

## How to Run
To run this small pipeline, we need to cd into this directory “pipeline_parse_graph_get_bf”:

```sh 
$ cd /export/projects/enriched-topic-modeling/data/edgar/pipeline_get_input_for_bf
```
Then, type the following command line:

```sh
$ ./linearization/linearize.sh ./data/10K_2016-03-13.graph 2 ./output/bf_input
```
Note that the above command line takes 3 input arguments for the bash script run.sh:

- 10K_2016-03-13.graph: this is the filename of full 10K data provided by Hooshmand
- integer “2": this integer specifies maximum number of nodes in feature. For any nonnegative integer n, maximum number of nodes in feature = n + 1. So in this argument, we specify that n = 2 i.e. maximum number of nodes in feature is 3.
- bf_input: this is the filename of the final output document that contains doc_ID, sentence_ID, feature names, and feature’s count. The file bf_input is ready to be passed to Lauren’s get_features.py (BF code).

The bash script run.sh will generate intermediary files and store them temporarily in a “temp” folder in the system. Once the bash script finishes up running, it will remove all temporary intermediary files, and only gives the final output “bf_input” in the “output” directory. 
