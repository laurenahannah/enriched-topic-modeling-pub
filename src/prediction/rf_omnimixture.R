##############################
# Lauren A. Hannah
# 05/11/16
#
# Modified by Lauren A. Hannah
# 06/1/16
# Modification: cleaned for use in public folder
#
# Purpose: use random forests on different inputs
##############################
require(rpart)
require(randomForest)
require(caret)
require(caretEnsemble)

###############################
# Label data and make covariates
#
# Loop through directories
args = commandArgs(trailingOnly=TRUE)
path_results_high_level= args[1]
label_file= args[2]
#path_results_high_level = "results/"
#label_path = sprintf("%s%s", path_results_high_level,label_file)
label_path = label_file
dir_temp = dir(path=path_results_high_level,all.files=FALSE,recursive=FALSE)
#print(dir_temp)

# Preprocess for caret---want to make custom random forest models
##########
# OmniMixture
rfOmni <- list(type= "Classification", library = "randomForest", loop = NULL)
prm <- data.frame(parameter = c("mtry"), class = rep("integer",1), label = c("mtry"))
rfOmni$parameters <- prm
rfOmniGrid <- function(x, y, len = NULL, search = "grid"){
	p <- length(x[1,])/2
	out <- data.frame(mtry = floor(sqrt(p)))
	out
}
rfOmni$grid <- rfOmniGrid
rfOmniFit <- function(x, y, wts, param, lev, last, weights, classProbs, ...){
	p <- length(x[1,])/2
	x_omni <- as.matrix(x[,1:p])
	randomForest(x = x_omni, y = as.factor(y), mtry = param$mtry)
}
rfOmni$fit <- rfOmniFit
rfOmniPred <- function(modelFit, newdata, preProc = NULL, submodels = NULL){
	if (!is.matrix(newdata)) newdata <- as.matrix(newdata)
	predict(modelFit, newdata)
}
rfOmni$predict <- rfOmniPred

rfOmniProb <- function(modelFit, newdata, preProc = NULL, submodels = NULL){
	if (!is.matrix(newdata)) newdata <- as.matrix(newdata)
	predict(modelFit, newdata, type = "probabilities")
}
rfOmni$prob <- rfOmniProb
##########
# Plain Text
rfPlain <- list(type= "Classification", library = "randomForest", loop = NULL)
prm <- data.frame(parameter = c("mtry"), class = rep("integer",1), label = c("mtry"))
rfPlain$parameters <- prm
rfPlainGrid <- function(x, y, len = NULL, search = "grid"){
	p <- length(x[1,])/2
	out <- data.frame(mtry = floor(sqrt(p)))
	out
}
rfPlain$grid <- rfPlainGrid
rfPlainFit <- function(x, y, wts, param, lev, last, weights, classProbs, ...){
	p <- length(x[1,])/2
	v_low = p+1
	v_hi = 2*p
	x_plain <- as.matrix(x[,v_low:v_hi])
	randomForest(x = x_plain, y = as.factor(y), mtry = param$mtry)
}
rfPlain$fit <- rfPlainFit
rfPlainPred <- function(modelFit, newdata, preProc = NULL, submodels = NULL){
	if (!is.matrix(newdata)) newdata <- as.matrix(newdata)
	predict(modelFit, newdata)
}
rfPlain$predict <- rfPlainPred

rfPlainProb <- function(modelFit, newdata, preProc = NULL, submodels = NULL){
	if (!is.matrix(newdata)) newdata <- as.matrix(newdata)
	predict(modelFit, newdata, type = "probabilities")
}
rfPlain$prob <- rfPlainProb

# Preprocess for caret---want to make custom tree models
##########
# OmniMixture
rpartOmni <- list(type= "Classification", library = "rpart", loop = NULL)
prm <- data.frame(parameter = c("mtry"), class = rep("integer",1), label = c("mtry"))
rpartOmni$parameters <- prm
rpartOmniGrid <- function(x, y, len = NULL, search = "grid"){
	p <- length(x[1,])/2
	out <- data.frame(mtry = floor(sqrt(p)))
	out
}
rpartOmni$grid <- rpartOmniGrid
rpartOmniFit <- function(x, y, wts, param, lev, last, weights, classProbs, ...){
	p <- length(x[1,])/2
	x_omni <- x[,1:p]
	my_df = as.data.frame(x_omni)
	my_df$y = as.factor(y)
	rpart(formula = y ~ ., data = my_df)

}
rpartOmni$fit <- rpartOmniFit
rpartOmniPred <- function(modelFit, newdata, preProc = NULL, submodels = NULL){
	if (!is.data.frame(newdata)) newdata <- as.data.frame(newdata)
	predict(modelFit, newdata)
}
rpartOmni$predict <- rpartOmniPred

rpartOmniProb <- function(modelFit, newdata, preProc = NULL, submodels = NULL){
	if (!is.data.frame(newdata)) newdata <- as.data.frame(newdata)
	predict(modelFit, newdata, type = "probabilities")
}
rpartOmni$prob <- rpartOmniProb
##########
# Plain Text
rpartPlain <- list(type= "Classification", library = "rpart", loop = NULL)
prm <- data.frame(parameter = c("mtry"), class = rep("integer",1), label = c("mtry"))
rpartPlain$parameters <- prm
rpartPlainGrid <- function(x, y, len = NULL, search = "grid"){
	p <- length(x[1,])/2
	out <- data.frame(mtry = floor(sqrt(p)))
	out
}
rpartPlain$grid <- rfPlainGrid
rpartPlainFit <- function(x, y, wts, param, lev, last, weights, classProbs, ...){
	p <- length(x[1,])/2
	v_low = p+1
	v_hi = 2*p
	x_plain <- x[,v_low:v_hi]
	my_df = as.data.frame(x_plain)
	my_df$y = as.factor(y)
	rpart(formula = y ~ ., data = my_df)
}
rpartPlain$fit <- rpartPlainFit
rpartPlainPred <- function(modelFit, newdata, preProc = NULL, submodels = NULL){
	if (!is.data.frame(newdata)) newdata <- as.data.frame(newdata)
	predict(modelFit, newdata)
}
rpartPlain$predict <- rpartPlainPred

rpartPlainProb <- function(modelFit, newdata, preProc = NULL, submodels = NULL){
	if (!is.data.frame(newdata)) newdata <- as.data.frame(newdata)
	predict(modelFit, newdata, type = "probabilities")
}
rpartPlain$prob <- rpartPlainProb


############


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
all_mis = vector(mode = 'numeric', length = 0)
plain_omni_cor = vector(mode = 'numeric', length = 0)
plain_all_cor = vector(mode = 'numeric', length = 0)
omni_all_cor = vector(mode = 'numeric', length = 0)

# Store n (number of testing docs) for each data set, along with misclassification




counter = 1
for (ticks in dir_temp){
	print(ticks)
	path_results = sprintf("%s%s",path_results_high_level,ticks)
	# Ticker symbol
	ticker = sprintf("%s_SP500",ticks)
	# Break date for training/testing
	break_date = as.Date("2012-01-01")
	# Read all data
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
	if (length(company_id) == 0){
		print("No documents found for:")
		print(ticker)
		print("Either: not in S&P500 entire time or symbol does not work with grep(). Run latter manually.")
		next
	}
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
		
		# Someone did not include movements
		# for non-trading days... even though there might be news on them
		#
		# To fix: pull out the next closest day
		if (length(date_id) == 0){ #No movement recorded
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
		if (is.na(record$Label)){
			change_vec[idx] = 0
			change_direction[idx] = 0
		}else if (record$Label == "no movement"){
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
	
	y_temp = label_df$change
	y_temp[y_temp == 0] = "no_move"
	y_temp[y_temp == 1] = "move"
	y_temp = as.factor(y_temp)
	y = y_temp[!test_id]
	y_test = y_temp[test_id]
	
	# Run boosted model
	all_df = cbind(omni,plain,y_temp)
	names(all_df) = c("o1", "o2", "o3", "o4", "o5", "o6", "o7", "o8", "o9", "o10", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "y")
	training = all_df[!test_id,]
	testing = all_df[test_id,]
	
	# Plain
	x = plain[!test_id,]
	x_test = plain[test_id,]
	
	# run caret
	set.seed(997)
	CVfolds <- 10
	CVrepeats <- 10
	indexPreds <- createMultiFolds(training$y, CVfolds, CVrepeats)
	ctrl <- trainControl(method = "repeatedcv", repeats = CVrepeats, 
					number = CVfolds, returnResamp = "all", 
					savePredictions = "all", 
					index = indexPreds) 
							
	set.seed(862)

	omniForest = train(y ~ . , data = training, 
					method = rfOmni,
					trControl = ctrl)
					
	plainForest = train(y ~ . , data = training, 
					method = rfPlain,
					trControl = ctrl)
	
	allForest = train(y ~ . , data = training, 
 					method = "rf",
 					trControl = ctrl)
 	
 	my_models = list(omni_rf = omniForest, plain_rf = plainForest, 
 					rf = allForest)
 	
 	class(my_models) = "caretList"
 	
 	# Get model correlation
 	
 	cor_mat = modelCor(resamples(my_models))
	
	# Predict on test set
	my_pred = predict(my_models, newdata = testing)
	
	# Record
	docs_name[counter] = ticks
	num_docs[counter] = length(y_test)
	plain_mis[counter] = sum(my_pred[,2] != as.factor(testing$y))
	omni_mis[counter] = sum(my_pred[,1] != as.factor(testing$y))
	all_mis[counter] = sum(my_pred[,3] != as.factor(testing$y))
	plain_omni_cor[counter] = cor_mat[1,2]
	plain_all_cor[counter] = cor_mat[1,3]
	omni_all_cor[counter] = cor_mat[2,3]
	
	out_df_temp = data.frame(doc = docs_name, num_tot = num_docs, plain_mis = plain_mis, omni_mis = omni_mis, all_mis = all_mis, plain_omni_cor = plain_omni_cor, plain_all_cor = plain_all_cor, omni_all_cor = omni_all_cor)
	print(out_df_temp)
	
	counter = counter + 1
}
out_df = data.frame(doc = docs_name, num_tot = num_docs, plain_mis = plain_mis, omni_mis = omni_mis, all_mis = all_mis, plain_omni_cor = plain_omni_cor, plain_all_cor = plain_all_cor, omni_all_cor = omni_all_cor)

filename = sprintf("%s/rf_results.csv",path_results_high_level)
write.csv(out_df,filename,row.names = F)


