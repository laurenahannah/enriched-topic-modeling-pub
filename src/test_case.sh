# Path to enriched-topic-modeling-pub home
OMNIMIXTURE_HOME=~
# runs
TEXT_INDIR=${OMNIMIXTURE_HOME}/data/Reuters/gics15
OMNIGRAPH_OUTDIR=${OMNIMIXTURE_HOME}/data/gics15_graph
MALLET_OUTDIR=${OMNIMIXTURE_HOME}/data/gics15_mallet
BASE_STOPWORD_FILE=${OMNIMIXTURE_HOME}/omnimixture/stopwordlist.txt
LABEL_FILE=${OMNIMIXTURE_HOME}/data/label_SP500_01012006_11302013.csv

cd ${TEXT_INDIR}
ls
for COMP in *; do
    echo "PROCESSING "$COMP
    cd ${OMNIMIXTURE_HOME}/pub/omnigraph
    sh process.sh seg ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP
    sh process.sh tok ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP
    sh process.sh dep ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP
    sh process.sh sem ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP
    sh process.sh omn ${TEXT_INDIR}/$COMP ${OMNIGRAPH_OUTDIR}/$COMP $COMP


    cd ${OMNIMIXTURE_HOME}/pub/omnimixture
    sh omnimixture.sh ${OMNIGRAPH_OUTDIR} $COMP ${MALLET_OUTDIR}/$COMP ${BASE_STOPWORD_FILE}
done

cd ${OMNIMIXTURE_HOME}/prediction
sh prediction.sh ${MALLET_OUTDIR} ${LABEL_FILE}
