# -*- coding: utf-8 -*-
"""
Created on Sun May  8 21:31:27 2016

@author: lhannah

# Copies files with a given name from directory1 into directory2 
"""
def copy_file_from_directory(old_dir,new_dir,file_list):
    with open(file_list,'r') as f_list:
        for line in f_list:
            copyfile(old_dir + line.rstrip(), new_dir + line.rstrip())
