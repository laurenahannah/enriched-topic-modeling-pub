# Purpose
The python script generateSH.py will generate bash scripts for each company within each sector. Those bash scripts are created in the same format as the run.sh bash script in the Omnigraph creation pipeline: "process". 

All the bash scripts created are the same except for the input directory, output directory, and Omnigraph names. These 3 variables are different for each company within each sector. 


# How to generate bash script for each company
change directory to the current folder that contains the Python script generateSH.py and the csv data file (e.g. toyData_sp500_tickers_by_sector.csv).

Then type the command: 

```sh
$ python generateSH.py
```

The current directory will include a new directory named "src" that contains each sector sub-directory (gics10, gics15, etc.). Each sector sub-directory would contain one bash script (.sh) files for each company in the sector. 


# How to run all the newly-generated bash scripts for each sector
First, put the runall.sh file under the following network directory: 
/export/home/username(e.g. rz2331)/bin/runall

In network, change directory to a sector's folder, e.g. ./src/gics10

Then, type the following command:
```sh
runall
```