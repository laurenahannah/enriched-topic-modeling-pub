##################################
# Lauren Hannah
# 4/21/16
# Goal: make a function that loads in two topic matrices
# and returns generalized R^2 and average adjusted R^2
##################################

topic_generalized_R2 <- function(topic_mat1, topic_mat2){
	# Inputs:
	#	- topic_mat1:	matrix with n rows (documents), n_topic columns (topics)
	#	- topic_mat2:	same dimension as topic_mat2, but different fit
	# Outputs:
	#	- generalized_R2:	generalized R^2 value
	
	dim_vec = dim(topic_mat1)
	n = dim_vec[1]
	n_topic = dim_vec[2]
	
	# Remove the last columns of each to make topic proportions independent-ish
	if (n_topic > 1){
		t_mat1 = topic_mat1[,1:(n_topic-1)]
		t_mat2 = topic_mat2[,1:(n_topic-1)]
	}else{
		t_mat1 = topic_mat1
		t_mat2 = topic_mat2
	}
	t_mat1 = as.matrix(t_mat1)
	t_mat2 = as.matrix(t_mat2)
	##################
	# Fit linear model
	
	# Get linear model
	lin.model = lm(t_mat2 ~ t_mat1)
	#print(lin.model)
	# Get linear coefficients
	beta_mat = lin.model$coef
	#print(beta_mat)
	# Get residuals
	res_mat = lin.model$residuals
	# Get variances
	sig_lin_vec = apply(lin.model$residuals,2,var)
	# Get log likelihood
	linear_log_like = log_like_gsn(t_mat1, t_mat2, beta_mat, sig_lin_vec)
	###################
	
	###################
	# Fit intercept model
	intercept_vec = colMeans(t_mat2)
	beta_intercept = rbind(intercept_vec, mat.or.vec(n_topic - 1, n_topic-1))
	sig_int_vec = apply(t_mat2,2,var)
	intercept_log_like = log_like_gsn(t_mat1, t_mat2, beta_intercept, sig_int_vec)
	###################
	
	# Get generalized R^2
	
	#print(linear_log_like)
	#print(intercept_log_like)
	
	generalized_R2 = 1 - (exp(intercept_log_like - linear_log_like))^(2/n)
	
	####################
	# Get average adjusted R^2
	adj_R2_vec = rep(0,n_topic-1)
	d = n_topic-1
	for (idx in 1:(n_topic-1)){
		ss_tot = mean((t_mat2[,idx]-mean(t_mat2[,idx]))^2)
		ss_res = mean(lin.model$res[,idx]^2)
		#print(c(ss_res, ss_tot))
		adj_R2_vec[idx] = max(1 - (ss_res/ss_tot)*(n - 1)/(n - d - 1),0)
	}
	#print(adj_R2_vec)
	adj_R2 = mean(adj_R2_vec)
	out_list = list(2)
	out_list[['generalized_R2']] = generalized_R2
	out_list[['adjusted_R2']] = adj_R2
	return(out_list)
}

log_like_gsn = function(x_mat, y_mat, beta_mat, sigma2_vec){
	# Inputs:
	#	- x_mat:	matrix of n x p covariates
	#	- y_mat:	matrix of n x d responses
	#	- beta_mat:	matrix of (p+1) x d linear coefficients (include intercept)
	#	- sigma2_vec:	vector of d variances
	# Outputs:
	#	- log_like:	log likelihood of model
	# Fits a Gaussian log likelihood to linear model with p covariates and d responses
	
	# Get sizes
	x_dim = dim(x_mat)
	y_dim = dim(y_mat)
	n = x_dim[1]
	p = x_dim[2]
	d = y_dim[2]
	
	x_aug = cbind(rep(1,n), x_mat) # Add in constant for intercept
	
	# Do computations
	out_vec = rep(0,n)
	for (idx in 1:d){
		out_vec = out_vec - log(rep(2*pi*sigma2_vec[idx]))/2 - (y_mat[,idx] - x_aug %*% beta_mat[,idx])^2/(2*sigma2_vec[idx])
	}
	log_like = sum(out_vec)
	return(log_like)
}