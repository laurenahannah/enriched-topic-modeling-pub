# Script for running MALLET in R
# because R is easy and I am not great at shell scripting
# ... sigh ...

# Loosely based on code from Shawn Graham

# Lauren Hannah
# 3/10/16
####################################################
# Set memory for java, cuz we don't need no heap space errors
#options( java.parameters = "-Xmx8g" )
#options( java.parameters = "-Xms8g" )

# Install mallet first using 
#install.packages('mallet')
require (mallet)

################################
# Import documents
# Assumptions:
#	- each file is a document
#	- all files are in the same folder
#	- extension is .txt


# adding memory requirements for java purposes
#library(rJava)
#options( java.parameters = "-Xmx8g" )
#options( java.parameters = "-Xms8g" )

# gets inputs from arguments 
args = commandArgs(trailingOnly=TRUE)
path_base = args[1]
path_results = args[2]

# Save three paths in a list
#path_base = "../MALLET/Reuters/gics50/CCI/"
#path_results = "../results/Reuters/gics50/CCI/"
doc_path_list = list(3)
doc_path_list[[1]] = "plain_docs" # Plain documents without features
doc_path_list[[2]] = "features" # Only features
doc_path_list[[3]] = "docs_features" # Docs with features

for (path_num in 1:length(doc_path_list)){
	doc_name = doc_path_list[[path_num]]
	doc_path = sprintf("%s%s/",path_base,doc_name)
	docs = mallet.read.dir(doc_path)

	# Other housekeeping
	stopword_path = sprintf("%s%s",path_base,"stopwordlist_append.txt")
	
	
	# MALLET-ize the docs
	mallet_instances = mallet.import(docs$id, docs$text, stopword_path, token.regexp = "\\p{L}[\\p{L}\\p{P}]+\\p{L}")
	
	################################
	# Train the model
	n_topics = 10
	
	# Object trainer
	topic_model = MalletLDA(n_topics)
	# Add the docs
	topic_model$loadDocuments(mallet_instances)
	# Get model features
	vocab = topic_model$getVocabulary()
	word_freq = mallet.word.freqs(topic_model)
	# Optimize hyperparameters every 20 iterations after 200 burnin iterations
	topic_model$setAlphaOptimization(20, 200)
	
	# Train the model using 4999 burnin iterations
	topic_model$train(4999)
	
	# Pick the best topic for each token
	topic_model$maximize(10)
	
	# Get the probability for topics for each document
	# Use smoothed = T to use posterior probabilities instead of empirical
	doc_topics = mallet.doc.topics(topic_model,smoothed = T, normalized = T)
	topic_words = mallet.topic.words(topic_model, smoothed = T, normalized = T)
	
	#############################################
	# Output results
	
	# Documents:
	filename = sprintf("%s%s_docs.csv",path_results,doc_name)
	print(filename)
	write.csv(docs$id,filename, row.names = F, col.names = NA)
	
	# Vocab:
	filename = sprintf("%s%s_vocab.csv",path_results,doc_name)
	print(filename)
	write.csv(vocab,filename, row.names = F, col.names = NA)
	
	# Topics:
	filename = sprintf("%s%s_doc_topics.csv",path_results,doc_name)
	print(filename)
	write.csv(doc_topics,filename, row.names = F, col.names = NA)
	
	# Word probabilities:
	filename = sprintf("%s%s_topic_words.csv",path_results,doc_name)
	write.csv(topic_words,filename, row.names = F, col.names = NA)
	
	# Words:
	topic_labels = rep("",n_topics)
	for (topic in 1:n_topics){
		topic_labels[topic] = paste(mallet.top.words(topic_model,topic_words[topic,],num.top.words = 20)$words, collapse = " ")
	}
	filename = sprintf("%s%s_topic_labels.csv",path_results,doc_name)
	write.csv(topic_labels,filename)

}
