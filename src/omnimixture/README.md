Omnimixture uses BayesFactor on the Omnigraph representation of documents (file structure produced by Omnigraph generating system) in order to run toppic modelling using MALLET librar and to produce omnimixture representation of the original documents.

    -inputs:
    - OMNIGRAPH_OUTDIR: Absolute path to Omnigraph output directory
 		GRAPH_DIR/*.seg : line segmented original text files
 		GRAPH_DIR/omnigraph : directory that contains .graph, .graph.nodeid, .linear, etc.
    - OMNIGRAPH_NAME: Name of the graph files e.g. GR if /OMNIGRAPH_OUTDIR/omnigraph/GR.graph
    - MALLET_OUTDIR: Absolute path to the directory where the Omnimixture files will be produced
    -outputs: omnimixture document representations

Run omnimixture script with the following options.
```sh
$ omnimixture.sh <OMNIGRAPH_OUTDIR> <OMNIGRAPH_NAME> <MALLET_OUTDIR>
```
