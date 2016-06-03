#!/bin/sh

#INDIR=/export/projects/enriched-topic-modeling/src/process/data/test
#OUTDIR=/export/projects/enriched-topic-modeling/src/process/data/test
INDIR=/export/home/hs2703/pub/data/plain
OUTDIR=/export/home/hs2703/pub/data/graph

cd 

sh process.sh seg $INDIR $OUTDIR
sh process.sh tok $INDIR $OUTDIR
sh process.sh dep $INDIR $OUTDIR
sh process.sh sem $INDIR $OUTDIR
sh process.sh omn $INDIR $OUTDIR sample

