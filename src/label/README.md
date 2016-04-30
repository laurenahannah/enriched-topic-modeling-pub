# Purpose:
This script is used to create a csv file that records each company's ticker, date, and label that indicates the
up-down daily movement of this company's stock daily closing prices.

Label assignment follows the rules in the following table, where t represents the current date, t-1 represents the previous trading date, Pt represents day t's adjusted closing price. 

| Label | Condition |
|:-------------:|:-------------:| 
| '+'     | $$\frac{P_t}{P_{t-1}} \geq 1.02$$ |
| '-'     | $$\frac{P_t}{P_{t-1}} \leq 0.98$$ |   
| 'no movement' | $$0.98 < \frac{P_t}{P_{t-1}} < 1.02$$ | 
| 'NA'          | First trading date in record for each comapny |

# Inputs:
    1. [.csv file] file for each company's stock daily price data, e.g. stock_price_SP500_01012006_11302013.csv
    2. [.csv file] file for each company's ticker and gics information, e.g. sp500_tickers_by_sector.csv
    3. [str] name of the file for each company's stock daily price data, e.g. 'stock_price_SP500_01012006_11302013.csv'
    4. [str] column name for company ticker in company's stock daily price data, e.g. 'Ticker'
    5. [str] column name for date in company's stock daily price data, e.g. 'Date'
    6. [str] column name for daily adjusted closing price in company's stock daily price data, e.g. 'Adj Close'
    7. [str] name of the file for each company's ticker and gics information, e.g. 'sp500_tickers_by_sector.csv'
    8. [str] column name for company ticker in the file for each company's ticker and gics info, e.g. 'ticker'
    9. [str] column name for company GICS in the file for each company's ticker and gics info, e.t. 'gics'
    10. [str] name of the output csv file whose header =  Company,Date,Label, e.g. 'label_SP500_01012006_11302013.csv'

# Outputs:
    a csv file with company's daily label in the working directory, e.g. label_SP500_01012006_11302013.csv

# How to run:
1. Make sure that the following three files are in the same folder/working directory:
    - 'stock_price_SP500_01012006_11302013.csv' (the file for each company's stock daily price data)
    - 'sp500_tickers_by_sector.csv' (the file for each company's ticker and gics information)
    - create_label.py (this current python script)


2. In terminal, change directory to the current folder

3. Parse argument to run this script. An example argument would be:

```sh
$ python create_label.py 'stock_price_SP500_01012006_11302013.csv' 'Ticker' 'Date' 'Adj Close' 'sp500_tickers_by_sector.csv' 'ticker' 'gics' 'label_SP500_01012006_11302013.csv'
```

# Output Preview

Companies are ordered alphabetically.

For each company, date is ordered from small to large. 
 
| Company, | Date, | Label |
|:-------------:|:-------------:|:-------------:| 
| AAPL_SP500_GICS45_COMPANY, | 2006-01-03, | NA |
| AAPL_SP500_GICS45_COMPANY, | 2006-01-04, | no movement |
| AAPL_SP500_GICS45_COMPANY, | 2006-01-05, | no movement |
| AAPL_SP500_GICS45_COMPANY, | 2006-01-05, | no movement |
| AAPL_SP500_GICS45_COMPANY, | 2006-01-06, | +           |
| ... | ... | ... |


 
 
