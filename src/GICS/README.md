# Purpose:
This script is used to re-organize news sentences for each company by the news-releasing date. 

For example, if the directory named with the sector number (e.g. gics35) has 3 companies with tickers 'ABC, 'HSP', and 'MRK', and each company folder contains txt files of single news sentences, then running this script would generate a new "result" folder under the sector directory (e.g. under gics35), where the "result" directory contains 3 sub-directories named by each company's ticker: 'ABC', 'HSP', and 'MRK'. In each company's result directory, there are .txt files, each containing all news sentences on a specific date that mentioned this company's ticker. 

# Input:
- [directory with sub-directories of txt files] A gicsxx folder where xx is the sector gics number.
       gicsxx folder should contain subdirectories named with
       each company's ticker in this sector, and each company's folder should contain ".txt' files
       each '.txt' file contains one news sentence about this company on a specific date.

# Output:
- A folder named as 'result-datetime' in the gicsxx directory. Within 'result-datetime',
    there are subfolders each named with the company's ticker. Within each company ticker's sub folder,
    there're .txt files, each contains all the news about this company on a specific date.

# How to run:
1. Make sure that this main.py script is in the gicsxx directory.

2. In terminal, change directory to this gicsxx directory.

3. Parse argument to run this script: 

```sh
$ python main.py
```


