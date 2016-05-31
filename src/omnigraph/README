

STEPS to run process in order to get omnigraph files from plain .txt files:

1. The config file contains environment variables that has to be set (e.g. SEMAFOR_HOME). The config file contains comments explaining the options.

2. Run process.h with the following options

# task = seg : segment the raw text
#	 tok : tokenize and part of speech tag
#	 dep : dependency parse
#	 sem : semafor parse
#	 omn : generating omnigraph

INDIR is the absolute path that contains the raw .txt files
OUTDIR is the absolute path to the place that all the intermediate and resulting files from the pipeline is saved
name-of-graph is simply the name of the .graph omnigraph file

a)
>> sh process.sh seg <INDIR> <OUTDIR>

The first steps creates a one to one mapping from original .txt files to .seg files which contain a sentence per each line.
For example, for INDIR/1.txt there would be a OUTDIR/1.seg

b)
>> sh process.sh tok <INDIR> <OUTDIR>
>> sh process.sh dep <INDIR> <OUTDIR>
>> sh process.sh sem <INDIR> <OUTDIR>

These three command create the necessary tokenization, part of speach tags, dependency parses and semafor files 
respectively according to the following nameing
OUTDIR/tmp/all.tokenized
OUTDIR/tmp/all.pos
OUTDIR/dep/*.dep
OUTDIR/dep/*.sem

>> sh process.sh omn <INDIR> <OUTDIR> <name-of-graph>

The following files will be created:

OUTDIR/omnigraph/<name-of-graph>.graph
OUTDIR/omnigraph/<name-of-graph>.graph.nodeid
OUTDIR/omnigraph/<name-of-graph>.doc
OUTDIR/omnigraph/<name-of-graph>.docid

These files are respectively the omnigraph file, the feature id (node id) files, the mapping of each graph to its semafor parse and finally the mapping graphs to original
.seg files

