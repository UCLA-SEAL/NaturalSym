from scipy.io import arff
import pandas as pd

import numpy as np
def gua(data):
  # Calculate the mean (μ) and standard deviation (σ) of the data
  print("Guassian({}, {}) form {} to {}".format(np.mean(data), \
                                                np.std(data), \
                                                np.min(data), \
                                                np.max(data) ))
  
data = arff.loadarff('dataset_31_credit-g.arff')
df = pd.DataFrame(data[0])
df.to_csv("credit.txt", encoding="utf-8", index=False)

content = []
with open("credit.txt", "r") as f:
    for l in f.readlines():
        comps = []
        for c in l.strip().split(","):
            if c.startswith("b\'") and c.endswith("\'"): c = eval(c).decode('utf-8')
            comps.append(c)
        content.append(comps)
    
header = content[0]
content = content[1:]

print(header)
'''
for k in range(len(heads)):
    ls = []
    ls.append(heads[k])
    for samples in refined[1:5 ]:
        ls.append(samples.split(",")[k])
    print(ls)
'''

def toInt(s): return str(int(float(s)))

# ["duration_months", "credit_amount", "purpose", "job"]

table1 = []
for credit in content:
    t = dict(zip(header, credit))
    #for k, v in t.items(): print(k, v)
    t1 = [toInt(t["duration"]), toInt(t["credit_amount"]), t["purpose"], t["job"]]
    # print(t1)
    isvalid = True
    for t in t1: 
        if len(t) == 0: isvalid = False
    if isvalid:
        table1.append(",".join(t1))

with open("input1.csv", "w") as f:
    f.write("\n".join(table1))

print("credit_amount")
gua([int(i.split(",")[1]) for i in table1])