

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


