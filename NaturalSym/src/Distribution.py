import numpy as np
import scipy.stats

class Distribution:
    def __init__(self) -> None:
        self.instances = []
        pass
    # sample N inputs
    def samples(self, N):
        # np.random.seed(0)
        return list(set([self.sample() for i in range(N)]))
    def samples_unseived(self, N):
        # np.random.seed(0)
        return [self.sample() for i in range(N)]
    def getL(self): return None
    def getR(self): return None
    def isSpecified(self): return True

class Discrete(Distribution):
    def __init__(self, instances) -> None:
        super().__init__()
        self.instances = instances
        self.N = len(self.instances)
    def sample(self):
        random_idx = np.random.randint(0, self.N)
        return self.instances[random_idx]
    
class DiscreteStr(Distribution):
    def __init__(self, instances, p) -> None:
        super().__init__()
        self.instances = instances
        self.p = p
    def sample(self):
        return np.random.choice(self.instances, size=1, p=self.p)[0]
    
class Uniform(Distribution):
    def __init__(self, l, r) -> None:
        self.L = l
        self.R = r
    def sample(self):
        return str(int(np.random.uniform(self.L, self.R)))
    def getL(self): return self.L
    def getR(self): return self.R
    
class Gaussian(Distribution):
    def __init__(self, mu, sigma) -> None:
        self.mu = mu
        self.sigma = sigma
    def sample(self):
        return str(int(np.random.normal(self.mu, self.sigma, 1)[0]))
    def pdf(self, x):
        return (1 / (np.sqrt(2 * np.pi) * self.sigma)) * np.exp(-((x - self.mu)**2) / (2 * self.sigma**2))
    def logpdf(self, x):
        return np.log(1 / (np.sqrt(2 * np.pi) * self.sigma)) + (-((x - self.mu)**2) / (2 * self.sigma**2))

class TruncatedGaussian(Gaussian):
    def __init__(self, mu, sigma, L, R) -> None:
        super().__init__(mu, sigma)
        self.L = L
        self.R = R
        assert L <= R
    def sample(self):
        while True:
            val = int(np.random.normal(self.mu, self.sigma, 1)[0])
            if val >= self.L and val <= self.R: return str(val)
    def getL(self): return self.L
    def getR(self): return self.R
            
class TruncatedGaussianFloat(Gaussian):
    def __init__(self, mu, sigma, L, R) -> None:
        super().__init__(mu, sigma)
        self.L = L
        self.R = R
    def sample(self):
        while True:
            val = int(np.random.normal(self.mu, self.sigma, 1)[0])
            if val >= self.L and val <= self.R: return str(val)

class DateTime(Distribution):
    def __init__(self) -> None:
        super().__init__()
    def sample(self):
        return "{}-{}".format(np.random.randint(0, 24), np.random.randint(0, 60))
    
class SciPy(Distribution):
    def __init__(self, dist_name, *args) -> None:
        super().__init__()
        self.dist = getattr(scipy.stats, dist_name)(*args)
        self.args = args
    def sample(self):
        return str(int(self.dist.rvs(size=1)[0]))

class NotSpecified(Distribution):
    def __init__(self, *args) -> None:
        super().__init__()
    def isSpecified(self): return False