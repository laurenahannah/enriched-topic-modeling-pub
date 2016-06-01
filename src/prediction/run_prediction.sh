
# environment that contains MALLET R scripts and prediction scripts
# ~/enriched-topic-modeling/src
ENVIRONMENT=`~/enriched-topic-modeling-pub/src/prediction`

# base stopword list for MALLET
BASE_STOPWORDS=stopwordlist.txt

if [ $# -lt 3 -o $# -gt 4 ]; then
   echo "USAGE: `basename "${0}"` <task> <input-dir> <output-dir> [<output-graph-name>]"
   echo "task=	tok : tokenize and part of speech tag"
   echo "	dep : dependency parse"
   echo	"	sem : semafor parse"
   echo	"	omn : generate omnigraph"
   exit 1
fi

# Absolute path to one GICS sector directory
# for each company COMP there will be a directory GRAPH_DIR/COMP that contains the followings
# 	GRAPH_DIR/COMP/*.seg : line segmented original text files
# 	GRAPH_DIR/COMP/omnigraph : directory that contains .graph, .graph.nodeid, .linear, etc.
GICS_GRAPH_DIR=$1
TEMP_DIR=$2
MALLET_DIR=$3


if [ ! -d "${TEMP_DIR}" ]; then
	mkdir "${TEMP_DIR}"
else
	rm -r "${TEMP_DIR}"
fi

if [ ! -d "${MALLET_DIR}" ]; then
	mkdir "${MALLET_DIR}"
else
	rm -r "${MALLET_DIR}"
fi

cd ${GICS_GRAPH_DIR}
for COMP in *; do
	mkdir ${TEMP_DIR}/$COMP
	cp $COMP/omnigraph ${TEMP_DIR}/$COMP
	
	mkdir ${MALLET_DIR}/$COMP
	mkdir ${MALLET_DIR}/$COMP/plain_docs
	mkdir ${MALLET_DIR}/$COMP/docs_features
	mkdir ${MALLET_DIR}/$COMP/features
	cp $COMP/*.seg 
done

python $ENVIRONMENT/get_features_wrapper_gics.py --data_trunk ${TEMP_DIR} --mallet_trunk ${MALLET_DIR}

COMPANIES=*
for COMP in *; do
	python $ENVIRONMENT/make_stopwords.py ${MALLET_DIR}/$COMP/plain_docs/ ${BASE_STOPWORDS} ${MALLET_DIR}/$COMP/stopwordlist_append.txt
	Rscript $ENVIRONMENT/mallet_r_wrapper.R ${MALLET_DIR}/$COMP/ ${MALLET_DIR}/$COMP/
done


Rscript store_R2.R ${MALLET_DIR}/

#Rscript boosted_omnimixture.R ${MALLET_DIR}/ ${MALLET_DIR}/

Rscript rf_omnimixture.R ${MALLET_DIR}/ ${MALLET_DIR}/

