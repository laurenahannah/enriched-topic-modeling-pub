
if [ $# -lt 4 -o $# -gt 4 ]; then
   echo "USAGE: `basename "${0}"` <OMNIGRAPH_OUTDIR> <OMNIGRAPH_NAME> <MALLET_OUTDIR>"
   echo "	OMNIGRAPH_OUTDIR: Absolute path to Omnigraph output directory"
   echo "	OMNIGRAPH_NAME: Name of the graph files e.g. GR if /OMNIGRAPH_OUTDIR/omnigraph/GR.graph"
   echo	"	MALLET_DIR: Absolute path to the directory where the Omnimixture files will be produced"
   echo	"	BASE_STOPWORDS: base stopword list for MALLET"
   exit 1
fi

#   - OMNIGRAPH_OUTDIR: Absolute path to Omnigraph output directory
# 		GRAPH_DIR/*.seg : line segmented original text files
# 		GRAPH_DIR/omnigraph : directory that contains .graph, .graph.nodeid, .linear, etc.
#	- OMNIGRAPH_NAME: Name of the graph files e.g. GR if /OMNIGRAPH_OUTDIR/omnigraph/GR.graph
#   - MALLET_OUTDIR: Absolute path to the directory where the Omnimixture files will be produced

OMNIGRAPH_OUTDIR=$1
OMNIGRAPH_NAME=$2
MALLET_DIR=$3
BASE_STOPWORDS=$4

echo "OMNIGRAPH_OUTDIR="${OMNIGRAPH_OUTDIR}
echo "OMNIGRAPH_NAME="${OMNIGRAPH_NAME}
echo "MALLET_DIR="${MALLET_OUTDIR}
echo "BASE_STOPWORDS="${BASE_STOPWORDS}

if [ ! -d "${MALLET_DIR}" ]; then
	mkdir "${MALLET_DIR}"
else
	rm -r "${MALLET_DIR}"
	mkdir "${MALLET_DIR}"
fi
mkdir ${MALLET_DIR}/plain_docs
mkdir ${MALLET_DIR}/docs_features
mkdir ${MALLET_DIR}/features

cp ${OMNIGRAPH_OUTDIR}/*.seg ${MALLET_DIR}/plain_docs

# Bayes Factor
python get_features_wrapper.py --data_trunk ${OMNIGRAPH_OUTDIR} --graph_name ${OMNIGRAPH_NAME} --mallet_trunk ${MALLET_DIR}
  
python make_stopwords.py ${MALLET_DIR}/plain_docs/ ${BASE_STOPWORDS} ${MALLET_DIR}/stopwordlist_append.txt
Rscript mallet_r_wrapper.R ${MALLET_DIR}/ ${MALLET_DIR}/


