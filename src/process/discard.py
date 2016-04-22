import os, sys

seg_filename = sys.argv[1]
threshold = int(sys.argv[2])

segf = open(seg_filename, 'r')
seg_lines = segf.readlines()
segf.close()


print "Discarding sentences larger than " + str(threshold) + " ..."
print seg_filename

out_seg = open(seg_filename, 'w')

i = 0
for seg_line in seg_lines:
	i += 1
	if len(seg_line) < threshold:
		out_seg.write(seg_line)
	else:
		sys.stdout.write(str(i) + ":(" + str(len(seg_line)) + ") ")

print 'done!'
	
out_seg.close()

