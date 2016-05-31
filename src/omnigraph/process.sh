
# input/output dir should be passed on as an argument
source /export/projects/enriched-topic-modeling/src/process/config

if [ $# -lt 3 -o $# -gt 4 ]; then
   echo "USAGE: `basename "${0}"` <task> <input-dir> <output-dir> [<output-graph-name>]"
   echo "task=	tok : tokenize and part of speech tag"
   echo "	dep : dependency parse"
   echo	"	sem : semafor parse"
   echo	"	omn : generate omnigraph"
   exit 1
fi

# task = seg : segment the raw text
#	 tok : tokenize and part of speech tag
#	 dep : dependency parse
#	 sem : semafor parse
task=${1}
INDIR=${2}
OUTDIR=${3}

# Creating a dir for output files
if [ ! -d "${OUTDIR}" ]; then
   mkdir "${OUTDIR}"
fi

if [ ! -d "${OUTDIR}/tmp" ]; then
   mkdir "${OUTDIR}/tmp"
fi
TEMP_DIR=${OUTDIR}/tmp

# turns raw txt files into txt files where each line contains a sentence
# concatenate all the sentence per line .seg files into a single all.seg
if [ ${task} = "seg" ]; then
   echo "Performing tokenizing and part of speach tagging on "${dataset_dir}
   python preprocess.py $INDIR $OUTDIR $NLTK

   echo "Creating single input file..."
   cat ${OUTDIR}/*.seg >> ${TEMP_DIR}/all.seg
   python discard.py ${TEMP_DIR}/all.seg 1700
   echo "Done creating a single input file!"

   echo	"finished tokenizing and parg of speech tagging" 
fi


INPUT_FILE=${TEMP_DIR}/all.seg
CLEAN_INPUT=${TEMP_DIR}/all
echo 'hello '${CLEAN_INPUT}
grep -v '^\s*$' ${INPUT_FILE} > ${CLEAN_INPUT}.input
INPUT_FILE=${CLEAN_INPUT}

# The MST dependency parser assumes (hard-wired) that there is a temp
# directory "tmp" under its home directory, so we want to make sure
# that this directory exists.
if [ ! -d "${MST_PARSER_HOME}/tmp" ]; then
   mkdir "${MST_PARSER_HOME}/tmp"
   REMOVE_DOT_TMP=1
else
   REMOVE_DOT_TMP=0
fi

# Setting the library binary files path
#CLASSPATH=".:${SEMAFOR_HOME}/lib/semafor-deps.jar"
CLASSPATH="./bin:${SEMAFOR_HOME}/lib/semafor-deps.jar"

#TOKENIZE & PART OF SPEECH TAG
#
if [ ${task} = "tok" ]; then

   rm -f ${INPUT_FILE}.tokenized
   if [ "${GOLD_TARGET_FILE}" == "null" ]
   then
       echo "**********************************************************************"
       echo "Tokenizing file: ${INPUT_FILE}"
       sed -f ${SEMAFOR_HOME}/scripts/tokenizer.sed ${INPUT_FILE}.input > ${INPUT_FILE}.tokenized
       echo "Finished tokenization."
       echo "**********************************************************************"
       echo
       echo
   else
       echo "**********************************************************************"
       echo "Gold target file provided, not tokenizing input file."
       cat ${INPUT_FILE}.input > ${INPUT_FILE}.tokenized
       echo "**********************************************************************"
       echo 
       echo
   fi

   echo "**********************************************************************"
   echo "Part-of-speech tagging tokenized data...."
   rm -f ${INPUT_FILE}.pos.tagged
   cd ${SEMAFOR_HOME}/scripts/jmx
   ./mxpost tagger.project < ${INPUT_FILE}.tokenized > ${INPUT_FILE}.pos.tagged
   echo "Finished part-of-speech tagging."
   echo "**********************************************************************"
   echo
   echo
 
fi

#DEPENDENCY PARSE
#
if [ ${task} = "dep" ]; then
   echo "Performing dependency parsing "
   rm -f ${INPUT_FILE}.conll.input
   rm -f ${INPUT_FILE}.dep
   if [ "$MST_MODE" != "server" ]
   then
    echo "**********************************************************************"
    echo "Preparing the input for MST Parser..."
    cd ${SEMAFOR_HOME}
    ${JAVA_HOME_BIN}/java \
	-classpath ${CLASSPATH} \
	edu.cmu.cs.lti.ark.fn.data.prep.CoNLLInputPreparation \
	${INPUT_FILE}.pos.tagged ${INPUT_FILE}.conll.input

    echo "Dependency parsing the data..."
    cd ${MST_PARSER_HOME}
    ${JAVA_HOME_BIN}/java -classpath ".:./lib/trove.jar:./lib/mallet-deps.jar:./lib/mallet.jar" \
	-Xms8g -Xmx8g mst.DependencyParser \
	test separate-lab \
	model-name:${MODEL_DIR}/wsj.model \
	decode-type:proj order:2 \
	test-file:${INPUT_FILE}.conll.input \
	output-file:${INPUT_FILE}.dep \
	format:CONLL
    echo "Finished dependency parsing."
    echo "**********************************************************************"
    echo
    echo
   fi
fi


#SEMAFORE PARSE
#
if [ ${task} = "sem" ]; then
   echo "Performing semafore parsing "

   if [ "${AUTO_TARGET_ID_MODE}" == "relaxed" ]
   then 
    RELAXED_FLAG=yes
   else
    RELAXED_FLAG=no
   fi

   if [ "${USE_GRAPH_FILE}" == "yes" ]
   then
    GRAPH_FILE=${MODEL_DIR}/sparsegraph.gz
   else
    GRAPH_FILE=null
   fi

   ALL_LEMMA_TAGS_FILE=${INPUT_FILE}.all.lemma.tags

   echo "**********************************************************************"
   echo "Performing frame-semantic parsing"
   cd ${SEMAFOR_HOME}
   ${JAVA_HOME_BIN}/java \
    -classpath ${CLASSPATH} \
    -Xms4g -Xmx4g \
    edu.cmu.cs.lti.ark.fn.parsing.ParserDriver \
    mstmode:${MST_MODE} \
    mstserver:${MST_MACHINE} \
    mstport:${MST_PORT} \
    posfile:${INPUT_FILE}.pos.tagged \
    test-parsefile:${INPUT_FILE}.dep \
    stopwords-file:${SEMAFOR_HOME}/stopwords.txt \
    wordnet-configfile:${SEMAFOR_HOME}/file_properties.xml \
    fnidreqdatafile:${MODEL_DIR}/reqData.jobj \
    goldsegfile:${GOLD_TARGET_FILE} \
    userelaxed:${RELAXED_FLAG} \
    testtokenizedfile:${INPUT_FILE}.tokenized \
    idmodelfile:${MODEL_DIR}/idmodel.dat \
    alphabetfile:${MODEL_DIR}/parser.conf \
    framenet-femapfile:${MODEL_DIR}/framenet.frame.element.map \
    eventsfile:${INPUT_FILE}.events.bin \
    spansfile:${INPUT_FILE}.spans \
    model:${MODEL_DIR}/argmodel.dat \
    useGraph:${GRAPH_FILE} \
    frameelementsoutputfile:${INPUT_FILE}.fes \
    alllemmatagsfile:${ALL_LEMMA_TAGS_FILE} \
    requiresmap:${MODEL_DIR}/requires.map \
    excludesmap:${MODEL_DIR}/excludes.map \
    decoding:${DECODING_TYPE}

   end=`wc -l ${INPUT_FILE}.tokenized`
   end=`expr ${end% *}`
   echo "Producing final XML document:"
   ${JAVA_HOME_BIN}/java -classpath ${CLASSPATH} \
    -Xms4g -Xmx4g \
    edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML \
    testFEPredictionsFile:${INPUT_FILE}.fes \
    startIndex:0 \
    endIndex:${end} \
    testParseFile:${ALL_LEMMA_TAGS_FILE} \
    testTokenizedFile:${INPUT_FILE}.tokenized \
    outputFile:${INPUT_FILE}.xml


   echo "Finished frame-semantic parsing."
   echo "**********************************************************************"
   echo
   echo
fi


if [ ${task} = "omn" ]; then

   DEPDIR=${OUTDIR}/dep
   SEMDIR=${OUTDIR}/sem
   OMNDIR=${OUTDIR}/omnigraph

   if [ ! -d "${DEPDIR}" ]; then
      mkdir "${DEPDIR}"
   else
      rm -r ${DEPDIR}/*
   fi

   if [ ! -d "${SEMDIR}" ]; then
      mkdir "${SEMDIR}"
   else
      rm -r ${SEMDIR}/*
   fi

   if [ ! -d "${OMNDIR}" ]; then
      mkdir "${OMNDIR}"
   else
      rm -r ${OMNDIR}/*
   fi

   python split.py ${INPUT_FILE}.dep ${INPUT_FILE}.xml ${DEPDIR} ${SEMDIR}

   graph_output_name=${4}

   # dataset_dir should have dependency parses in dep/ and their corresponding semafore parses in sem/
   javac -cp lib/javatuples-1.2.jar:lib/stanford-corenlp-2012-07-09.jar:lib/opencsv-3.4.jar src/edu/columbia/ccls/*/*.java src/edu/columbia/ccls/text/stanford/*.java
   java -Xmx4g -cp ${omnigraph_workspace}/src:'/lib/opencsv-3.4.jar' edu.columbia.ccls.modeler.SemGraphModeler --name ${graph_output_name} --dataset_dir ${OUTDIR} --out_dir ${OMNDIR} --min_freq ${omnigraph_min_freq} --limit ${omnigraph_limit} --merge_rule ${omnigraph_merge_rule} --feature_space ${omnigraph_feature_space} --is_directed ${omnigraph_is_directed}

   python trace.py ${OUTDIR} ${OMNDIR} ${graph_output_name}
fi

REMOVE_DOT_TMP=0
# clean up
if [ ${REMOVE_DOT_TMP} = 1 ]; then
   /bin/rm -rf "${MST_PARSER_HOME}/tmp"
fi


