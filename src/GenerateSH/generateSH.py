import sys
import os
from itertools import islice
from glob import glob
from shutil import rmtree
from copy import deepcopy


def main(filename, head_gics, head_ticker):
    c = []
    c.append('#!/bin/sh')
    c.append(' ')
    c.append('bin_home=/export/projects/enriched-topic-modeling/src/process')

    dir = os.getcwd() # dir is the current directory, generateSH folder
    pattern = os.path.join(dir, 'src*')
    for folder in glob(pattern):
        if not os.path.isdir(folder):
            continue
        rmtree(folder)
    # remove existing src folder in generateSH directory

    dir_for_sh_scripts = os.path.join(dir, 'src')
    os.makedirs(dir_for_sh_scripts, 0755) # create new directory named e.g. './src'
    # create new directory named './src'
    # so far still in current dir (dir, i.e. generateSH)

    f = open(filename,'r')
    for headline in islice(f, 0, 1): # look at the first line in file, header
            # headl = 'ticker,gics,sector,company,address'
            headline_list = headline.strip().split(',')  # .strip() removes '\n' at end of string
            gics_ind = headline_list.index(head_gics) # get gics's index
            ticker_ind = headline_list.index(head_ticker) # get ticker's index
    for line in f:
        line_list = line.split(',')
        gicsnum = line_list[gics_ind] # 10, 15, 20, 25, 30, 35, 45, 50, 55
        ticker = line_list[ticker_ind] # e.g. 'IBM'
        content_indir = 'INDIR=/export/projects/enriched-topic-modeling/experiment/input-texts/gics'+gicsnum+'/'+ticker
        content_outdir = 'OUTDIR=/export/projects/enriched-topic-modeling/experiment/graphs/gics'+gicsnum+'/'+ticker
        content_omnigraphname = 'omnigraph_name='+ticker
        content = deepcopy(c)
        content.append(content_indir)
        content.append(content_outdir)
        content.append(content_omnigraphname)
        content.append(' ')
        content.append('cd ${bin_home}')
        content.append(' ')
        content.append('sh process.sh seg $INDIR $OUTDIR')
        content.append('sh process.sh tok $INDIR $OUTDIR')
        content.append('sh process.sh dep $INDIR $OUTDIR')
        content.append('sh process.sh sem $INDIR $OUTDIR')
        content.append('sh process.sh omn $INDIR $OUTDIR ${omnigraph_name}')
        dir_for_current_gics = os.path.join(dir_for_sh_scripts, 'gics'+gicsnum)
        if not os.path.exists(dir_for_current_gics):
            os.makedirs(dir_for_current_gics)
        # next, write items in content (list) line by line to a .sh file and save .sh file in the directory dir_for_current_gics
        outfilename = ticker + '.sh'
        with open(os.path.join(dir_for_current_gics, outfilename), 'wb') as output_file:
            for item in content:
                output_file.write('%s\n' % item)



if __name__ == "__main__":

    #main(sys.argv[1], sys.argv[2], sys.argv[3])
    main('toyData_sp500_tickers_by_sector.csv', 'gics', 'ticker')

# sys.argv[1] = filename, i.e. sys.argv[1] = 'toyData_sp500_tickers_by_sector.csv' # filename for ticker&sector info
# sys.argv[2] = head_gics, i.e. sys.argv[2] = 'gics'
# sys.argv[3] = head_ticker, i.e. sys.argv[3] = 'ticker'