Omnimixture uses BayesFactor on the Omnigraph representation of documents (file structure produced by Omnigraph generating system) in order to run toppic modelling using MALLET librar and to produce omnimixture representation of the original documents.

    -inputs:
    - OMNIGRAPH_OUTDIR: Absolute path to Omnigraph output directory
 		GRAPH_DIR/*.seg : line segmented original text files
 		GRAPH_DIR/omnigraph : directory that contains .graph, .graph.nodeid, .linear, etc.
    - OMNIGRAPH_NAME: Name of the graph files e.g. GR if /OMNIGRAPH_OUTDIR/omnigraph/GR.graph
    - MALLET_OUTDIR: Absolute path to the directory where the Omnimixture files will be produced
    - BASE_STOPWORD_FILE: stop word list file for topic modeling
    -outputs: omnimixture document representations

Run omnimixture script with the following options.
```sh
$ omnimixture.sh <OMNIGRAPH_OUTDIR> <OMNIGRAPH_NAME> <MALLET_OUTDIR> <BASE_STOPWORD_FILE>
```
=======================================================================

Details:

File structure:

	BASE_FOLDER:
		- src	:	holds sources files
		- data	:	holds non-text input files, like .docid, .graph, .nodeid, etc
		- MALLET	:	holds .txt files for feeding into MALLET
			~ plain_docs	:	raw text .txt files
			~ features	:	omnigraph .txt files
			~ docs_features	:	mixed text and omnigraph
		- results	:	holds .csv results from MALLET, post processing
		
		
Omnigraph to admixture representation

(1) Use Bayes Factors to select omnigraph values

(1a) get_features_wrapper_gics.py
	Wrapper to get all subfiles based on ticker symbol; calls get_features.py

    # Inputs:
    #   - data_trunk: path to gics folder with data files
    #   - mallet_trunk:     path to folder with MALLET files
    # Outputs:
    #   - all get_features.py outputs for each subfolder in data_trunk

(1b) get_features.py
	INPUTS:
	- input_docs: folder where input .txt documents live
	- linearization: linearized features file
	- input_graph: raw .graph file
	- feature_key: .nodeid file that matches line number to feature name
	- doc_key: .docid file that matches line number to feature name
	- test: for Bayes Factor (use ‘bayes-conditional’)
	- selection: ignore, use ‘none’
	- dist: distribution for Bayes Factor
	- max-phrase-len: maximum length of omnigraph features
	- min-phrase-count: minimum number of counts for feature selection

	OUTPUTS:
	- omnigraph features: .txt files with original filenames in input_docs\features folder
	- mixed features: .txt files with original filenames in input_docs\docs_features folder

    # Example values:
    #   - input_docs = '../MALLET'
    #   - linearization = '../data/March_07_test/feature_3nodes_debug.txt'
    #   - input_graph = '../data/March_07_test/10K_2016-03-07graph_remove00_explorer.txt'
    #   - feature_key = '../data/March_07_test/new_test_graph_nodeid.txt'
    #   - doc_key = '../data/March_07_test/doc_numbers.txt'
    #   - test = 'bayes-conditional'
    #   - selection = 'none'
    #   - dist = 'empirical'
    #   - max-phrase-len = 3
    #   - min-phrase-count = 15

(2) make_stopword_list.py
	Makes a stop word list that is tailored to file because ‘IBM’ happens to be a stop word in documents that are about IBM. Who knew?

	NOTE: CURRENTLY ONLY TAKES ONE SET OF .TXT FILES AS AN INPUT, BUT WRAPPER NEEDS TO BE WRITTEN SO ALL /plain_docs, /features, /docs_features FILES CAN BE USED TO GENERATE STOP WORDS

    # Inputs:
    #   - text_folder:  a folder with corpus in individual plain text files
    #   - stopword_list:    an existing list of stopwords
    #   - output_name:  name where new list will be stored
    # Outputs:
    #   - a new .txt file saved under output_name

(3) mallet_r_wrapper.R
	Runs MALLET in R; this makes statistical data post processing easier
	This can be replaced by .jar MALLET implementation
	
	Default run: 4999 iterations; can be reduced to 999 if too slow
	
	NOTE: call before running the first time in a session to set Java memory
		library(rJava)
		options( java.parameters = "-Xmx8g" )
		options( java.parameters = "-Xms8g" )

	Runs as a script with inputs hard coded; can be changed into a function
	Inputs:
		path_base = where all .txt files are stored, like ”../MALLET/Reuters/gics50/CCI/"
		path_results = where the outputs are written in .csv form, like ”../results/Reuters/gics50/CCI/"
	Outputs:
		- Document id: filename = sprintf("%s%s_docs.csv",path_results,doc_name)
		- Vocab: filename = sprintf("%s%s_vocab.csv",path_results,doc_name)
		- Topics: filename = sprintf("%s%s_doc_topics.csv",path_results,doc_name)
		- Word probabilities: filename = sprintf("%s%s_topic_words.csv",path_results,doc_name)
