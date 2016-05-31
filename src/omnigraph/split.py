import os, sys


if len(sys.argv) != 5:
	print "usage: " + sys.argv[0] + " <input dep file> <input sem file> <output dep path> <output sem path>" 
	exit(1)

# all paths should be absolute
depfile = sys.argv[1] 
semfile = sys.argv[2] 
single_depfile_path = sys.argv[3]
single_semfile_path = sys.argv[4]

with open(depfile, 'r') as f:
	
	string = f.read()
	dep_parses = string.split("\n\n")

	counter = 0
	for parse in dep_parses:
		counter += 1
		if counter == len(dep_parses):
			continue
		sys.stdout.write(str(counter)+' ')
		one_sentence_dep_parse = open(single_depfile_path+'/all'+str(counter)+'.txt', 'w')
		one_sentence_dep_parse.write(parse)
		one_sentence_dep_parse.close()
	print ' '


	
with open(semfile, 'r') as f:
	
	string = f.read()
	dep_parses = string.split("<sentence ID")
	
	counter = -1

	for parse in dep_parses:
		counter += 1
		if counter == 0:
			continue
		sys.stdout.write(str(counter)+' ')
		one_sentence_dep_parse = open(single_semfile_path+'/all'+str(counter)+'.txt', 'w')
		one_sentence_dep_parse.write("<sentence ID"+parse)
		one_sentence_dep_parse.close()
	print ' '
	





