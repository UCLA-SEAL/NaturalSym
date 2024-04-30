import re, sys, os

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

import numpy as np
def gua(data):
    # Calculate the mean (μ) and standard deviation (σ) of the data
    print("Guassian({}, {}) form {} to {}".format(np.mean(data), np.std(data), np.min(data), np.max(data) ))
if __name__ == "__main__":
    results = []
    os.chdir(os.path.dirname(os.path.abspath(sys.argv[0])))
    years = []
    with open("movies.txt", "r") as f:
        for l in f.readlines():
            ls = csv_reader(l.strip())
            #for (i, e) in zip(range(len(ls)), ls):
            #    print(i, e)
            #print(ls, len(ls))
            assert (len(ls) == 14)
            ratings = "" if ls[7] == "" else str(int(eval(ls[7])))
            if "min" not in ls[6]: continue
            if ls[2] == "": continue
            duration = eval(ls[6].strip().split(" ")[0])
            duration_f = "{:02d}:{:02d}".format(duration // 60, duration % 60)
            year = ls[2]
            years.append(int(eval(year)))
            res = [ls[1], year, ls[3].split(",")[0].strip("\"\n "), ratings, duration_f]
            valid = True
            for x in res:
                if x == "": valid = False
                if "," in x: valid = False
                if "\"" in x: valid = False
            if valid: results.append(",".join(res))
    with open("input1.csv", "w") as f:
        f.writelines("\n".join(results))
    
    print("movie rating")
    gua([int(i.split(",")[3]) for i in results])