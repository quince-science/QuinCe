from io import BytesIO
import pandas as pd
import numpy as np
from Preprocessor import Preprocessor
from netCDF4 import Dataset

class AddSalinityPreprocessor(Preprocessor):
  SALINITY_FILE = "salinity_data/woa18_seasonal_surface_salinity.nc"
  SALIINITY_VAR = "salinity"

  def get_name():
    return "Add Fixed Salinity"

  def preprocess(self, data):
    dataframe = pd.read_csv(data, sep="\t")
    indices = self.make_indices(dataframe)
    dataframe = dataframe.assign(Salinity=self.get_salinity(indices)["Salinity"])
    return dataframe.to_csv(sep="\t", index=False).encode("utf-8")

  def make_indices(self, dataframe):
  	indices = pd.DataFrame(index = dataframe.index.values, columns=["lon", "lat", "season"])
  	indices["lon"] = dataframe["longitude"].apply(lambda x: np.nan if np.isnan(x) else (round((x + 180) * 4)) - 1)
  	indices["lat"] = dataframe["latitude"].apply(lambda x: np.nan if np.isnan(x) else (round((x + 90) * 4)) - 1)
  	indices["season"] = dataframe["PC Date"].apply(lambda x: self.get_season(x))
  	return indices

  def get_season(self, date):
    season = -1
    month = int(date[3:5])

    if month in [12, 1, 2]:
      season = 0
    elif month in [3, 4, 5]:
      season = 1
    elif month in [6, 7, 8]:
      season = 2
    elif month in [9, 10, 11]:
      season = 3

    return season

  def get_salinity(self, lookups):
    nc = Dataset(self.SALINITY_FILE, mode="r")

    salinities = pd.DataFrame(index = lookups.index.values, columns=["Salinity"])
    for index, row in lookups.iterrows():
    	salinities.loc[index,"Salinity"] = self.read_salinity(nc, row["lon"], row["lat"], row["season"])

    return salinities

  def read_salinity(self, nc, lon, lat, season):
    salinity = "NaN"

    if not np.isnan(lon) and not np.isnan(lat) and not np.isnan(season):
      salinity = nc.variables[self.SALIINITY_VAR][int(season), int(lat), int(lon)]

    return salinity