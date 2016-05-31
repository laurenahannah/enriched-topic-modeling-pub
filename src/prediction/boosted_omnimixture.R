##############################
# Lauren A. Hannah
# 04/22/16
#
# Purpose: use randomized subsets of features to do boosting
##############################
require(rpart)

##############################
ada_randomized = function(x, y, S, n_sub, x_test, y_test){
	# Inputs:
	#	- x:	covariates
	#	- y:	labels
	#	- S:	number of rounds
	#	- n_sub:	number of covariates in subset
	#	- x_test:	testing covariates
	#	- y_test:	testing responses
	# Outputs:
	#	- y_pred:	predicted labels for x_test
	#
	# Idea: randomly select subsets of covariates to create low correlation
	# tree classifiers each round. More low correlation, high signal covariates 
	# is better.
	dim_vec = dim(x)
	n = dim_vec[1]
	d = dim_vec[2]
	dim_vec_test = dim(x_test)
	n_test = dim_vec_test[1]
	
	# Make x's into data frames for prediction
	x = as.matrix(x)
	x_test = as.matrix(x_test)
	x_df = as.data.frame(x)
	x_test_df = as.data.frame(x_test)
	
	tree_list = list() # Store tree classifiers
	alpha_vec = rep(0,S) # Store alpha vectors
	weight_list = list() # Store weight vectors
	weight_list[[1]] = rep(1,n)/n # Start with equal weights
	cov_list = list() # Store subsets of covariates
	y_pred_mat = mat.or.vec(n_test,S)
	for (s in 1:S){
		# Select covariates
		cov_vec = sample(1:d,n_sub)
		cov_list[[s]] = cov_vec
		# Fit a tree
		weights = weight_list[[s]]
		x_temp = x_df[,cov_vec]
		x_temp$y = y
		# Due to small number of covariates, tree should be stumpy
		tree_temp = rpart(y~., data = x_temp,weights = weights,method="class")
		tree_list[[s]] = tree_temp
		# Evaluate error
		y_train_pred = predict(tree_temp,x_temp,type = "class")
		err_vec = (as.factor(y) != as.factor(y_train_pred))
		#print(c(as.factor(y[1:10]), as.factor(y_train_pred[1:10])))
		epsilon = as.numeric(err_vec %*% weights)
		#print(epsilon)
		alpha_vec[s] = .5*log((1-epsilon)/epsilon)
		# Recompute weights
		weight_temp = weights
		if (epsilon > 0){
			# Misclassified
			weight_temp[err_vec] = weight_temp[err_vec]*exp(alpha_vec[s])
			# Correctly classified
			weight_temp[!err_vec] = weight_temp[!err_vec]*exp(-alpha_vec[s])
			if (min(weight_temp) <= 0 ){
				print(alpha_vec)
				print(weight_temp)
				print(epsilon)
			}
		}
		
		# Normalize
		weight_temp = weight_temp/sum(weight_temp)
		weight_list[[s+1]] = weight_temp
		# Compute for output
		x_test_temp = x_test_df[,cov_vec]
		x_test_temp$y = y_test
		y_pred_temp = predict(tree_temp,x_test_temp,type = "class")
		#print(as.numeric(y_pred_temp[1:10]))
		#print(err_vec)
		for(j in 1:n_test){
			y_pred_mat[j,s] = as.numeric(y_pred_temp[j])
		}
		#print(alpha_vec)
	}
	# Compute predictor
	# Check to see if any perfect training classifiers
	y_levels = levels(as.factor(y))
	out_mat = mat.or.vec(n_test,length(y_levels))
	if(min(is.finite(alpha_vec)) == 0){# Some perfect classifiers
		index_vec = is.finite(alpha_vec)
		alpha_vec[index_vec] = 0
		alpha_vec[!index_vec] = 1	
	}
	#print(dim(as.numeric(y_pred_mat)))
	##print(dim(alpha_vec))
	#weighted_pred = round(as.numeric(y_pred_mat) %*% alpha_vec)
	#return(weighted_pred)
	out_list = list()
	out_list[[1]] = alpha_vec
	out_list[[2]] = y_pred_mat
	return(out_list)
}

###############################
# Label data and make covariates
#
# Loop through directories

# gets inputs from arguments 
args = commandArgs(trailingOnly=TRUE)
path_high_level= args[1]
path_results_high_level= args[2]

#path_high_level = "../MALLET/Reuters/gics50/"
#path_results_high_level = "../results/Reuters/gics50/"
dir_temp = dir(path=path_results_high_level,all.files=FALSE,recursive=FALSE)
print(dir_temp)

dir_alt = dir_temp
dir_temp = vector(mode = 'character', length = 0)
counter = 1
for (items in dir_alt){
	str_temp = strsplit(items,".",fixed=T)
	if (length(str_temp[[1]]) == 1){
		dir_temp[counter] = items
		counter = counter + 1
	}
}
print(dir_temp)

docs_name = vector(mode = 'character', length = 0)
num_docs = vector(mode = 'numeric', length = 0)
plain_mis = vector(mode = 'numeric', length = 0)
omni_mis = vector(mode = 'numeric', length = 0)
mixed_mis = vector(mode = 'numeric', length = 0)
plain_omni_mis = vector(mode = 'numeric', length = 0)
plain_mixed_mis = vector(mode = 'numeric', length = 0)
omni_mixed_mis = vector(mode = 'numeric', length = 0)
all_mis = vector(mode = 'numeric', length = 0)
# Store n (number of testing docs) for each data set, along with misclassification

# Parameters
S = 50


counter = 1
for (ticks in dir_temp){
	path_base = sprintf("%s%s",path_high_level,ticks)
	path_results = sprintf("%s%s",path_results_high_level,ticks)
	# Ticker symbol
	ticker = sprintf("%s_SP500",ticks)
	# Break date for training/testing
	break_date = as.Date("2012-01-01")
	# Read all data
	#label_path = "cleanApr14/Label/label_SP500_01012006_11302013.csv"
	label_path = "./label_SP500_01012006_11302013.csv"
	label_all = read.csv(label_path)
	# Match files with dates, ticker
	
	# Read in docs from files
	filename = sprintf("%s/plain_docs_docs.csv", path_results)
	docs = read.csv(filename,header = T) # Ends up as dataframe
	docs = as.character(docs[,1])
	
	n_docs = length(docs)
	file_vec = rep(" ", n_docs)
	change_vec = rep(0, n_docs)
	change_direction = rep(0, n_docs)
	test_id = rep(FALSE, n_docs)
	
	# Loop through files in plain_docs
	company_id = grep(ticker,label_all$Company)
	label_small = label_all[company_id,]
	for (idx in 1:n_docs){
		file_vec[idx] = docs[idx]
		# Get the date
		file_split = strsplit(file_vec[idx], "_") # should break off date at end
		date_part = file_split[[1]][length(file_split[[1]])]
		# remove .txt
		date_spl_txt = strsplit(date_part,'.',fixed=TRUE) 
		date = date_spl_txt[[1]][1]
		# Find file with that date and ticker
		date_id = which(label_small$Date == date)
		
		# OKAY---it looks like someone ignored specs and did not include movements
		# for non-trading days... even though there might be news on them
		# Not to say that I'm angry, but... I'm angry
		# Seriously, I have better things to do than cover for RAs not making
		# code to spec
		#
		# To fix: pull out the next closest day
		if (length(date_id) == 0){ #Grrrrr..... I hate bugs!
			date_as_date = as.Date(date)
			while(length(date_id) == 0){
				date_as_date = date_as_date + 1
				date_str = as.character(date_as_date)
				date_id = which(label_small$Date == date_str)
			}
		}
		if (as.Date(date) >= break_date){
			test_id[idx] = TRUE
		}
		
		record = label_small[date_id,]
		#TODO HOOSHMAND
		#print(record$Label)
		if (record$Label == "no movement"){
			change_vec[idx] = 0
			change_direction[idx] = 0
		}else{
			change_vec[idx] = 1
			if (record$Label == "+"){
				change_direction[idx] = 1
			}else{
				change_direction[idx] = -1
			}
		}
	}
	# Make a data frame with the results
	label_df = data.frame(doc = file_vec, change = change_vec, direction = change_direction, test_id = test_id)
	
	# Read data
	doc_path_list = list(3)
	doc_path_list[[1]] = "plain_docs" # Plain documents without features
	doc_path_list[[2]] = "features" # Only features
	doc_path_list[[3]] = "docs_features" # Docs with features
	filename = sprintf("%s/%s_doc_topics.csv",path_results,doc_path_list[[1]])
	plain = read.csv(filename)
	filename = sprintf("%s/%s_doc_topics.csv",path_results,doc_path_list[[2]])
	omni = read.csv(filename)
	filename = sprintf("%s/%s_doc_topics.csv",path_results,doc_path_list[[3]])
	mixed = read.csv(filename)
	
	
	# Run boosted model
	
	y_temp = label_df$change
	y = y_temp[!test_id]
	y_test = y_temp[test_id]
	
	# Plain
	x = plain[!test_id,]
	x_test = plain[test_id,]
	
	n_sub = floor(length(x[1,]))/3
	
	out_list = ada_randomized(x,y, S, n_sub, x_test, y_test)
	alpha_vec = out_list[[1]]
	pred_mat = out_list[[2]]
	pred_vec = as.factor(round(pred_mat %*% (alpha_vec/sum(alpha_vec))))
	misclassified_plain = sum(pred_vec != as.numeric(as.factor(y_test)))
	
	# Omni
	x = omni[!test_id,]
	x_test = omni[test_id,]
	
	n_sub = floor(length(x[1,]))/3
	
	out_list = ada_randomized(x,y, S, n_sub, x_test, y_test)
	alpha_vec = out_list[[1]]
	pred_mat = out_list[[2]]
	pred_vec = as.factor(round(pred_mat %*% (alpha_vec/sum(alpha_vec))))
	misclassified_omni = sum(pred_vec != as.numeric(as.factor(y_test)))
	
	# Mixed
	x = mixed[!test_id,]
	x_test = mixed[test_id,]
	
	n_sub = floor(length(x[1,]))/3
	
	out_list = ada_randomized(x,y, S, n_sub, x_test, y_test)
	alpha_vec = out_list[[1]]
	pred_mat = out_list[[2]]
	pred_vec = as.factor(round(pred_mat %*% (alpha_vec/sum(alpha_vec))))
	misclassified_mixed = sum(pred_vec != as.numeric(as.factor(y_test)))
	
	# Plain-Omni
	x = cbind(plain[!test_id,], omni[!test_id,])
	x_test = cbind(plain[test_id,], omni[test_id,])
	
	#n_sub = floor(length(x[1,]))/3
	n_sub = 3
	
	out_list = ada_randomized(x,y, S, n_sub, x_test, y_test)
	alpha_vec = out_list[[1]]
	pred_mat = out_list[[2]]
	pred_vec = as.factor(round(pred_mat %*% (alpha_vec/sum(alpha_vec))))
	misclassified_plain_omni = sum(pred_vec != as.numeric(as.factor(y_test)))
	
	# Plain-Mixed
	x = cbind(plain[!test_id,], mixed[!test_id,])
	x_test = cbind(plain[test_id,], mixed[test_id,])
	
	n_sub = floor(length(x[1,]))/3
	
	out_list = ada_randomized(x,y, S, n_sub, x_test, y_test)
	alpha_vec = out_list[[1]]
	pred_mat = out_list[[2]]
	pred_vec = as.factor(round(pred_mat %*% (alpha_vec/sum(alpha_vec))))
	misclassified_plain_mixed = sum(pred_vec != as.numeric(as.factor(y_test)))
	
	# Omni-mixed
	x = cbind(mixed[!test_id,], omni[!test_id,])
	x_test = cbind(mixed[test_id,], omni[test_id,])
	
	n_sub = floor(length(x[1,]))/3
	
	out_list = ada_randomized(x,y, S, n_sub, x_test, y_test)
	alpha_vec = out_list[[1]]
	pred_mat = out_list[[2]]
	pred_vec = as.factor(round(pred_mat %*% (alpha_vec/sum(alpha_vec))))
	misclassified_omni_mixed = sum(pred_vec != as.numeric(as.factor(y_test)))
	
	# All
	x = cbind(plain[!test_id,], omni[!test_id,], mixed[!test_id,])
	x_test = cbind(plain[test_id,], omni[test_id,], mixed[test_id,])
	
	n_sub = floor(length(x[1,]))/3
	
	out_list = ada_randomized(x,y, S, n_sub, x_test, y_test)
	alpha_vec = out_list[[1]]
	pred_mat = out_list[[2]]
	pred_vec = as.factor(round(pred_mat %*% (alpha_vec/sum(alpha_vec))))
	misclassified_all = sum(pred_vec != as.numeric(as.factor(y_test)))
	
	# Record
	docs_name[counter] = ticks
	num_docs[counter] = length(y_test)
	plain_mis[counter] = misclassified_plain
	omni_mis[counter] = misclassified_omni
	mixed_mis[counter] = misclassified_mixed
	plain_omni_mis[counter] = misclassified_plain_omni
	plain_mixed_mis[counter] = misclassified_plain_mixed
	omni_mixed_mis[counter] = misclassified_omni_mixed
	all_mis[counter] = misclassified_all
	counter = counter + 1
}
out_df = data.frame(doc = docs_name, num_tot = num_docs, plain = plain_mis, omni = omni_mis, mixed = mixed_mis, plain_omni = plain_omni_mis, plain_mixed = plain_mixed_mis, omni_mixed = omni_mixed_mis, all = all_mis)

filename = sprintf("%s/boosted_results.csv",path_results)
print(filename)
write.csv(out_df,filename,row.names = F)


