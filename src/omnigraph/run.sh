#!/bin/sh

INDIR=/export/projects/enriched-topic-modeling/src/process/data/test
OUTDIR=/export/projects/enriched-topic-modeling/src/process/data/test

#sh process.sh seg $INDIR $OUTDIR
#sh process.sh tok $INDIR $OUTDIR
#sh process.sh dep $INDIR $OUTDIR
#sh process.sh sem $INDIR $OUTDIR
sh process.sh omn $INDIR $OUTDIR 10K
