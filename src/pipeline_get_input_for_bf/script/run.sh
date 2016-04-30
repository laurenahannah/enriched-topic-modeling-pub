#!/bin/bash

DIR=$(dirname $0)
INPUT=$1
H=$2
MODIFIED_GRAPH=$(mktemp /tmp/raylin.remove00.XXXXXX)
#${INPUT}.remove00
#output from 1st python
EXPLORER_IN=$(mktemp /tmp/raylin.explorer_in.XXXXXX)
#${INPUT}.explorer_in
#output from 1st python
EXPLORER_OUT=$(mktemp /tmp/raylin.explorer_out.XXXXXX)
#${INPUT}.explorer_out
#output from java
FEATURE_OUT=$3
python ${DIR}/prepare_graph_forexplorer.py ${INPUT} ${MODIFIED_GRAPH} ${EXPLORER_IN} && \
java -classpath ${DIR}/../bin edu.columbia.ccls.util.GraphFeatureExplorer ${EXPLORER_IN} ${EXPLORER_OUT} ${H} && \
python ${DIR}/match_doc_sen.py ${MODIFIED_GRAPH} ${EXPLORER_OUT} ${FEATURE_OUT} && \
rm -rf ${MODIFIED_GRAPH} ${EXPLORER_IN} ${EXPLORER_OUT}
