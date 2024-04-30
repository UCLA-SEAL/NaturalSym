import random

# AWS S3 https://us-east-2.console.aws.amazon.com/dataexchange/home?region=us-east-2#/products/prodview-y77x3t6zisn4w
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

with open("carvana_carvana_car_sold-2022-07.csv", "r") as f:
  content = []
  for l in f.readlines():
    row = [c.strip("\"") for c in csv_reader(l.strip("\n"))]
    content.append(row)
header = content[0]
print(header)
content = random.sample(content[1:], 500)

import numpy as np
def gua(data):
  # Calculate the mean (μ) and standard deviation (σ) of the data
  print("Guassian({}, {}) form {} to {}".format(np.mean(data), \
                                                np.std(data), \
                                                np.min(data), \
                                                np.max(data) ))
table1 = []
table2 = []
for l in content:
  t = dict(zip(header, l))
  # vehicle_id,model
  # vehicle_id,year,price,discounted,sold-date,miles
  t1 = [t["vehicle_id"], t["model"]]
  t2 = [t["vehicle_id"], t["year"], t["sold_price"], t["discounted_sold_price"], t["sold_date"], t["miles"]]

  isvalid = True
  for t in t1: 
    if len(t) == 0: isvalid = False
  for t in t2:
    if len(t) == 0: isvalid = False
  if isvalid:
    table1.append(",".join(t1))
    table2.append(",".join(t2))
  # ident, elevation_ft, iso_region, gps_coordinates
  # ident, type

with open("input1.csv", "w") as f:
  f.write("\n".join(table1))
with open("input2.csv", "w") as f:
  f.write("\n".join(table2))

#print("year")
gua([int(i.split(",")[1]) for i in table2])
#print("price")
gua([int(i.split(",")[2]) for i in table2])
#print("discount")
gua([int(i.split(",")[3]) for i in table2])
#print("miles")
gua([int(i.split(",")[-1]) for i in table2])