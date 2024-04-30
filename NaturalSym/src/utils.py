import config
def is_inputvar(ivar): #input1, input1_d1, input1_P1_d1i
    return ivar.startswith("input")
def is_rowvar(ivar): #input1, input1_P1
    return is_inputvar(ivar) and ("_d" not in ivar) and (not ivar.endswith("i"))
def is_componentvar(ivar): #input1_P1_d2
    return is_inputvar(ivar) and ("_d" in ivar) and (not ivar.endswith("i"))
def get_pure_inputvar(ivar): # input1_P2_d3i -> input1_P2
    if "_d" in ivar: return ivar.split("_d")[0]
    elif ivar.endswith("i"): return ivar[:-1]
    else: return ivar
def get_inputid(ivar):
    # return int(ivar.split("_")[0][len("input"):]) - 1
    return ivar.split("_")[0]
def get_partid(ivar):
    if ivar.endswith("i"):
        return int(ivar.split("_d")[1][:-1])
    return int(ivar.split("_d")[1])