
# environment that contains MALLET R scripts and prediction scripts
# ~/enriched-topic-modeling/src
ENVIRONMENT=`~/enriched-topic-modeling-pub/src/prediction`

# All output files from MALLET stored in subdirectories in RESULTS_DIR
# Example: ‘results/CCI/docs_features_doc_topics.csv’
# 	company = CCI
#	file = docs_features_doc_topics.csv
RESULTS_DIR=$1
LABEL_FILE=$2

Rscript store_R2.R ${RESULTS_DIR}/

Rscript rf_omnimixture.R ${RESULTS_DIR}/ ${LABEL_FILE}

