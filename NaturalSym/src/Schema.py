# Schema : S | Schema,S
# S      : Discrete("string", integer) | (-2<=)Guassian(0,1)(<=10) | Uniform(0,10)
from Distribution import *
def parseSchema(schema_file):
    schema = dict()
    with open(schema_file, "r") as f:
        for row in f.readlines():
            if "=" not in row: continue
            name = row.split(":=")[0].strip()
            schema_str = row.split(":=")[1].strip()
            samplers = []
            for s in schema_str.split("|"):
                s = s.strip()
                if "Gaussian" in s:
                    L, R = None, None
                    if "<=" in s:
                        L = float(s.split("<=")[0])
                        R = float(s.split("<=")[2])
                        s = s.split("<=")[1].strip()
                    s = s.strip("Gaussian")
                    s = s.strip("(").strip(")")
                    std = float(s.split(",")[0].strip())
                    dev = float(s.split(",")[1].strip())
                    if L is not None:
                        samplers += [TruncatedGaussian(std, dev, L, R)]
                    else:
                        samplers += [Gaussian(std, dev)]
                elif "Discrete" in s:
                    s = s.strip("Discrete")
                    s = s.strip("(").strip(")")
                    ds = [d.strip("\"") for d in s.split(",")]
                    samplers += [Discrete(ds)]
                elif "Uniform" in s:
                    s = s.strip("Uniform")
                    s = s.strip("(").strip(")")
                    L = float(s.split(",")[0].strip())
                    R = float(s.split(",")[1].strip())
                    samplers += [Uniform(L, R)]
                elif "DateTime" in s:
                    samplers += [DateTime()]
                else:
                    assert "False", "unidentifiable type"
            schema[name] = samplers
    return schema

def parseSchema2(schema_file):
    schema = dict()
    with open(schema_file, "r") as f:
        for row in f.readlines():
            if "=" not in row: continue
            name = row.split(":=")[0].strip()
            schema_str = row.split(":=")[1].strip()
            samplers = []
            for s in schema_str.split("|"):
                s = s.strip()
                if "Gaussian" in s:
                    L, R = None, None
                    if "<=" in s:
                        L = float(s.split("<=")[0])
                        R = float(s.split("<=")[2])
                        s = s.split("<=")[1].strip()
                    s = s.strip("Gaussian")
                    s = s.strip("(").strip(")")
                    std = float(s.split(",")[0].strip())
                    dev = float(s.split(",")[1].strip())
                    if L is not None:
                        samplers += [TruncatedGaussian(std, dev, L, R)]
                    else:
                        samplers += [Gaussian(std, dev)]
                elif "DiscreteStr" in s:
                    s = s[len("DiscreteStr"):]
                    s = s.strip("(").strip(")")
                    ds = [d for d in s.split(",")]
                    ncnt = sum([int(":" not in d) for d in ds])
                    tot = sum([eval(d.split(":")[1]) for d in ds if ":" in d])
                    instances = [d.split(":")[0].strip("\"") for d in ds]
                    ps = [eval(d.split(":")[1]) if ":" in d else (1-tot)/ncnt for d in ds ]
                    samplers += [DiscreteStr(instances, ps)]
                elif "Discrete" in s:
                    s = s[len("Discrete"):]
                    s = s.strip("(").strip(")")
                    ds = [d.strip("\"") for d in s.split(",")]
                    samplers += [Discrete(ds)]
                elif "Uniform" in s:
                    s = s[7:]
                    s = s.strip("(").strip(")")
                    L = float(s.split(",")[0].strip())
                    R = float(s.split(",")[1].strip())
                    samplers += [Uniform(L, R)]
                elif "DateTime" in s:
                    samplers += [DateTime()]
                elif "scipy." in s:
                    s = s[6:]
                    dist_name = s.split("(")[0]
                    arg_str = s.split("(")[1].strip(")")
                    args = [eval(arg.strip()) for arg in arg_str.split(",")]
                    samplers += [SciPy(dist_name, *args)]
                else:
                    samplers += [NotSpecified()]
                #else:
                #    assert False, "unidentifiable type"
            schema[name] = samplers
    return schema

if __name__ == "__main__":
    #schema = parseSchema2("../newbench/config/movie1.config")
    schema = parseSchema2("./test.config")
    print(schema)
    for name, samplers in schema.items():
        print(name)
        for sampler in samplers:
            print(sampler.samples_unseived(10))
            # samples_unseived: not unique