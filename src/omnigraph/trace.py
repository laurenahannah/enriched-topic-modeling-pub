
import os, sys
import difflib

#out_path = '/export/projects/enriched-topic-modeling/src/preptest/data/test'
#omni_path = '/export/projects/enriched-topic-modeling/src/preptest/data/test/omnigraph' 
#omni_name = '10K'

if len(sys.argv) != 4:
	print "wrong usage!"
	print sys.argv[0] + " <sentence-per-line-.seg-path> <omnigraph-path> <omnigraph-file-name>"
	exit(1)

out_path = sys.argv[1]
omni_path = sys.argv[2]
omni_name = sys.argv[3]

graph_doc = omni_path + '/'+omni_name+'.doc'
graph_file = omni_path + '/'+omni_name+'.graph'

f = open(graph_doc, 'r')
corr_sem_files = f.readlines()
f.close()


sentences = []
for sem_file in corr_sem_files:

	with open(sem_file.replace('\n', ''), 'r') as f:
		sentence = f.read()
		sentence = sentence[sentence.index('<text>')+len('<text>'):sentence.index('</text>')]	
		sentences.append(sentence)


doc_id = []
origfilename = []
origlinenumber = []
origsentences = []
onlyfiles = [f for f in os.listdir(out_path) if os.path.isfile(os.path.join(out_path, f))]
for filename in onlyfiles:
	if filename.endswith('.seg'):
		doc_id.append(filename)	
		with open(out_path+'/'+filename, 'r') as f:
			lines = f.readlines()
			for i in range(0, len(lines)):	
				origsentences.append(lines[i])
				origfilename.append(filename)
				origlinenumber.append(i)


f = open(graph_file, 'r')
graphs = f.readlines()
f.close()


docid_file = open(omni_path+'/'+omni_name+'.docid', 'w')
#new_graph_file = open(omni_path+'/'+omni_name+'.graph', 'w')
new_graph_file = open(omni_path+'/'+omni_name+'.newgraph', 'w')

for doc in doc_id:
	docid_file.write(doc+'\n')
docid_file.close()

offset = len(origsentences) - len(sentences) + 1
for i in range(0, len(sentences)):
	dif_scores = []
	for o in range(0, offset):
		dif_ratio = difflib.SequenceMatcher(None, origsentences[i+o], sentences[i]).ratio()
		dif_scores.append(dif_ratio)
		#sys.stdout.write(str(dif_ratio) + " ")
	index = i + dif_scores.index(max(dif_scores))
	docid = doc_id.index(origfilename[index])
	fname = out_path + "/" + origfilename[index]
	linen = origlinenumber[index]
	new_graph_file.write(str(docid) + " " + str(linen) + " " + str(graphs[i]))



