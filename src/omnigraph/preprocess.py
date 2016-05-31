import os,glob,sys,ConfigParser,nltk.data


INDIR = sys.argv[1] 
OUTDIR = sys.argv[2] 
NLTK = sys.argv[3]
sent_detector = nltk.data.load(NLTK + "/" +'nltk_data/tokenizers/punkt/english.pickle')
print INDIR
print OUTDIR
print " "
print("Beginning execution sentence segmentation . . . ")
os.chdir(INDIR)
for file in glob.glob("*.txt"):
    # print file 
    base=os.path.splitext(file)[0]
    writefile = open(OUTDIR + "/" + base + '.seg', 'w')
    with open(file, 'r') as doc:
        content = doc.read()
    writefile.write('\n'.join(sent_detector.tokenize(content.strip())))
    writefile.close()
print("DONE")


