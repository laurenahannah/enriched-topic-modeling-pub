ENVIRONMENT=~/enriched-topic-modeling-pub/src
# runs
TEXT_INDIR=../data/Rueters/gics15/$COMP
OMNIGRAPH_OUTDIR=../data/gics15_graph/$COMP
MALLET_OUTDIR=../data/gics15_mallet/$COMP
BASE_STOPWORD_FILE=./omnimixture/stopwordlist.txt
LABEL_FILE=../data/label_SP500_01012006_11302013.csv

cd ${TEXT_INDIR}
for COMP in *; do
    COMP=$1

    cd ${ENVIRONMENT}/omnigraph
    sh process.sh seg ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP
    sh process.sh tok ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP
    sh process.sh dep ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP
    sh process.sh sem ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP
    sh process.sh omn ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP $COMP


    cd ${ENVIRONMENT}/omnimixture
    sh omnimixture.sh ${OMNIGRAPH_OUTDIR} $COMP ${MALLET_OUTDIR} ${BASE_STOPWORD_FILE}
done

cd ${ENVIRONMENT}/prediction
sh prediction.sh ${MALLET_OUTDIR} ${LABEL_FILE}