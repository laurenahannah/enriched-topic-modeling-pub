# Pipeline for generating Omnimixture representation of documents

This pipeline contains three components, namely omnigraph generation, omnimixture document reconstruction, prediction task on stock price change using Reuters news corpus. Two first components are for general use and can be used on any collection of plain text documents to produce omnimixture representations. The prediction component is specifically designed for the problem that the paper addresses.

### Tech

Omnimixture uses a number of open source projects to work:

* [NLTK] - Natural Language Processing with Python
* [MST Parser] - Dependency parsing toolkit
* [SEMAFOR] - Frame semantic parser

   [NLTK]: <http://www.nltk.org/>
   [MST Parser]: <http://www.seas.upenn.edu/~strctlrn/MSTParser/MSTParser.html>
   [SEMAFOR]: <http://www.cs.cmu.edu/~ark/SEMAFOR/>
 
## Sample Usage

In order to generate Omnigraphs from a directory <TEXT_INDIR> containing plan .txt documents and creating the results in <OMNIGRAPH_OUTDIR>

```sh
TEXT_INDIR=../data/Rueters/gics15/AA
OMNIGRAPH_OUTDIR=../data/gics15_graph/AA
GRAPH_NAME=APA

cd ./omnigraph
sh process.sh seg ${TEXT_INDIR} ${OMNIGRAPH_OUTDIR}
sh process.sh tok ${TEXT_INDIR} ${OMNIGRAPH_OUTDIR}
sh process.sh dep ${TEXT_INDIR} ${OMNIGRAPH_OUTDIR}
sh process.sh sem ${TEXT_INDIR} ${OMNIGRAPH_OUTDIR}
sh process.sh omn ${TEXT_INDIR} ${OMNIGRAPH_OUTDIR} ${GRAPH_NAME}

```

In order to generate omnimixture 


```sh
cd ./omnimixture
MALLET_OUTDIR=../data/gics15_mallet/AA
BASE_STOPWORD_FILE=../omnimixture/stopwordlist.txt
sh omnimixture.sh ${OMNIGRAPH_OUTDIR} ${GRAPH_NAME} ${MALLET_OUTDIR} ${BASE_STOPWORD_FILE}
```


## Test Case

We have included partial Rueters dataset and script template (test_case.sh) to demonstrate the usage of prediction component.

