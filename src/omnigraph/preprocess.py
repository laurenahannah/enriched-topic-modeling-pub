import os,glob,sys,ConfigParser,nltk.data
import traceback
import codecs

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

    try:
        #with codecs.open(file, encoding='utf-8', mode='r') as doc:
        with open(file, 'r') as doc:
            content = doc.read().decode('utf-8', 'ignore')
            lines = '\n'.join(sent_detector.tokenize(content.strip()))
            writefile = open(OUTDIR + "/" + base + '.seg', 'w')
            writefile.write(lines.encode('utf-8', 'ignore'))
            writefile.close()
        print "Successfully segmented file %s", file
    except: 
        print "Failed for file %s", file
        traceback.print_exc() 
print("DONE")


