import pickle
import numpy as np
import scipy.optimize
from scipy.signal import savgol_filter
import sys
import xarray as xr
import scipy.optimize
from scipy.signal import savgol_filter
from scipy.interpolate import interp1d
from sklearn.base import BaseEstimator, ClassifierMixin
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.pipeline import make_pipeline, Pipeline
from keras.models import load_model
import itertools

class TwoStepWrapperClassifier(BaseEstimator, ClassifierMixin):
    def __init__(self, mixedClassifier,
                        distinctClassifier):
        self.mixedClassifier = mixedClassifier
        self.distinctClassifier = distinctClassifier
        

    def splitMixed(self, X, y):
        comb = np.concatenate([X, y], axis=1)
        distinct = comb[comb[:, comb.shape[1]-1] != 1, :]
        distinct[:, -1] = np.where(distinct[:, -1] == 2, 1, 0)
        mixed = comb 
        mixed[:, -1] = np.where(mixed[:, -1] == 1, 1, 0)
        return mixed[:, : -1], mixed[:, -1], distinct[:, : -1], distinct[:, -1]
    
    def fit(self, X, y):
        mixed_X, mixed_y, distinct_X, distinct_y = self.splitMixed(X, y)
        self.mixedClassifier.fit(mixed_X, mixed_y)
        self.distinctClassifier.fit(distinct_X[:, :-1], distinct_y)
     
    def predict(self, X):
        predictMixed = self.mixedClassifier.predict(X)
        predictDistinct = self.distinctClassifier.predict(X[:, :-1])
        return np.where(predictMixed == 1, predictMixed, predictDistinct * 2)

def interpolate(data : xr.Dataset, interpolSize):
    # интерполировать
    timeInterpol = np.linspace(0, (60*166)/1500, num=interpolSize, endpoint=True)
    global timestep 
    timestep = timeInterpol[1]
    dataInterpol = xr.Dataset({
        "time" : (
            ("row"),
            timeInterpol,
        )
    }, )
    for var in data.data_vars.keys():
        if(var == "time"):
            continue
        
        feat = data[var]
        time = data["time"]
        f = interp1d(time, feat)
        featInterpol = f(timeInterpol)
        dataInterpol[var] = xr.DataArray(featInterpol, dims=("row"))
    return dataInterpol

def toDistances(data):
    return xr.Dataset(
        {
            "dist21" : (
                ("row"),
                np.sqrt(np.square(data.x2 - data.x1) + np.square(data.y2 - data.y1) + np.square(data.z2 - data.z1)).data,
            ),
            "dist32" : (
                ("row"),
                np.sqrt(np.square(data.x3 - data.x2) + np.square(data.y3 - data.y2) + np.square(data.z3 - data.z2)).data,
            ),
            "dist31" : (
                ("row"),
                np.sqrt(np.square(data.x3 - data.x1) + np.square(data.y3 - data.y1) + np.square(data.z3 - data.z1)).data,
            )
        }
    )

def toDistancesSmooth(data):
    return xr.Dataset(
        {
            "dist21" : (
                ("row"),
                savgol_filter(np.sqrt(np.square(data.x2 - data.x1) + np.square(data.y2 - data.y1) + np.square(data.z2 - data.z1)).data, 15, 5),
            ),
            "dist32" : (
                ("row"),
                savgol_filter(np.sqrt(np.square(data.x3 - data.x2) + np.square(data.y3 - data.y2) + np.square(data.z3 - data.z2)).data, 15, 5),
            ),
            "dist31" : (
                ("row"),
                savgol_filter(np.sqrt(np.square(data.x3 - data.x1) + np.square(data.y3 - data.y1) + np.square(data.z3 - data.z1)).data, 15, 5),
            )
        }
    )

def sinusoid(t, A, w, p, c):  return A * np.sin(w*t + p) + c

def approximateSinusoid(y, timestep):
    t = np.array([i*timestep for i in range(len(y))])
    y = np.array(y)
    F_y = abs(np.fft.fft(y))
    ff = np.fft.fftfreq(len(t), (t[1]-t[0]))   
    freq_init = abs(ff[np.argmax(F_y[1:])+1])  
    amp_init = np.std(y) * 2.**0.5
    offset_init = np.mean(y)
    init = np.array([amp_init, 2.*np.pi*freq_init, 0., offset_init])

    popt = scipy.optimize.curve_fit(sinusoid, t, y, p0=init, maxfev=10000)
    A, w, p, c = popt
    return {"amp": A, "omega": w, "phase": p, "offset": c}

def toSinParams(data : xr.Dataset):
    dataNew = xr.Dataset({
    }, )

    for var in data.data_vars.keys():
        if(var == "time"):
            continue
        feat = data[var]
        smooth = savgol_filter(feat, 15, 5)
        sinParams = approximateSinusoid(smooth, timestep)
        for k in sinParams:
            key = var+"_"+k
            dataNew[key] = xr.DataArray(sinParams[k], dims=())
    dataNew['omega_12_13'] = dataNew['dist21_omega'] / dataNew['dist31_omega']
    dataNew['omega_12_32'] = dataNew['dist21_omega'] / dataNew['dist32_omega']
    dataNew['offset_23_13'] = dataNew['dist32_offset']**2 / dataNew['dist31_offset']**2
    dataNew['offset_12_13'] = dataNew['dist21_offset']**4 / dataNew['dist31_offset']**4
    dataNew['offset_12_23'] = dataNew['dist21_offset']**4 / dataNew['dist32_offset']**4
    dataNew['amp_23_13'] = dataNew['dist32_amp'] / dataNew['dist31_amp']
    dataNew['amp_12_13'] = dataNew['dist21_amp'] / dataNew['dist31_amp']
    dataNew['amp_12_23'] = dataNew['dist21_amp'] / dataNew['dist32_amp']
    return dataNew

def preprocessForComplexModel(raw: xr.Dataset):
    return raw.pipe(interpolate, 166).pipe(toDistances).pipe(toSinParams)

def preprocessForCNN(raw: xr.Dataset):
    return raw.pipe(interpolate, 166).pipe(toDistancesSmooth)

def permuteConcat(a, _2d = False):
    conc = []
    for idx in list(itertools.permutations([0, 1, 2])):
        i = np.array(idx)
        i = np.tile(i, (len(a), 1) if _2d else (len(a), 1, 1))
        conc.append(np.take_along_axis(a,i,axis=-1))
    return np.concatenate(conc, axis=-1)

def main():
    rawData = sys.argv[1].split(',')
    data = []
    for item in rawData:
        word = item.split(' ')
        data.append(word)    
    data = np.array(data, dtype='float')
    data[:, 0] = data[:, 0] - data[0, 0]
    data = xr.Dataset(
        {
            "time" : (
                ("row"),
                data[:, 0],
            ),
            "x1" : (
                ("row"),
                data[:, 1],
            ),
            "y1" : (
                ("row"),
                data[:, 2],
            ),
            "z1" : (
                ("row"),
                data[:, 3],
            ),
            "x2" : (
                ("row"),
                data[:, 4],
            ),
            "y2" : (
                ("row"),
                data[:, 5],
            ),
            "z2" : (
                ("row"),
                data[:, 6],
            ),
            "x3" : (
                ("row"),
                data[:, 7],
            ),
            "y3" : (
                ("row"),
                data[:, 8],
            ),
            "z3" : (
                ("row"),
                data[:, 9],
            ),
        }
    )
    
    if sys.argv[2] == '0':
        with open("TwoStep_GridSeached_77.pkl", "rb") as f:
            params = preprocessForComplexModel(data).to_array().to_numpy().reshape(1, -1)
            model = pickle.load(f)
            predicted = model.predict(params)
            return predicted[0]
    else:
        model = load_model('CNN_model_0.8.h5')
        params = preprocessForCNN(data).to_array().to_numpy().transpose()
        params = permuteConcat(params, True)
        return np.argmax(model.predict(np.expand_dims(params, axis = 0)))

if __name__ == "__main__":
    try:
        sys.exit(int(main()))
    except Exception as e:
        print(e)
        sys.exit(-1)
    

