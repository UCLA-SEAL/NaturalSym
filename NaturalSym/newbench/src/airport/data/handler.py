import random

# http://ourairports.com/data/airports.csv
def csv_reader(line):
    inside_quote = False
    current_str = ""
    component_list = []
    for c in line.strip():
        if c == "\"":
            inside_quote = not inside_quote
            current_str += c
        elif c == ",":
            if inside_quote: current_str += c
            else:
                component_list.append(current_str)
                current_str = ""
        else:
            current_str += c
    
    component_list.append(current_str)
    return component_list

with open("airports.csv", "r") as f:
  content = []
  for l in f.readlines():
    row = [c.strip("\"") for c in csv_reader(l.strip("\n"))]
    content.append(row)
header = content[0]
print(header)
content = random.sample(content[1:], 500)

table1 = []
table2 = []
idx = 1
for l in content:
  t = dict(zip(header, l))
  t["ident"] = str(idx)
  idx += 1
  t["latitude_deg"] = "{:.2f}".format(float(t["latitude_deg"]))
  t["longitude_deg"] = "{:.2f}".format(float(t["longitude_deg"]))
  t1 = [t["ident"], t["elevation_ft"], t["iso_region"], t["latitude_deg"], t["longitude_deg"]]
  t2 = [t["ident"], t["type"]]

  isvalid = True
  for t in t1: 
    if len(t) == 0: isvalid = False
  for t in t2:
    if len(t) == 0: isvalid = False
  if isvalid:
    assert t1[2][2] == "-"
    table1.append(",".join(t1))
    table2.append(",".join(t2))
  # ident, elevation_ft, iso_region, gps_coordinates
  # ident, type
import numpy as np
def gua(data):
  # Calculate the mean (μ) and standard deviation (σ) of the data
  print("Guassian({}, {}) form {} to {}".format(np.mean(data), \
                                                np.std(data), \
                                                np.min(data), \
                                                np.max(data) ))
  
with open("input1.csv", "w") as f:
  f.write("\n".join(table1))
with open("input2.csv", "w") as f:
  f.write("\n".join(table2))
print("elevation")
print(gua([int(i.split(",")[1]) for i in table1]))