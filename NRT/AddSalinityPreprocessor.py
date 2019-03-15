import pandas as pd
from Preprocessor import Preprocessor

class AddSalinityPreprocessor(Preprocessor):
  def get_name():
    return "Add Fixed Salinity"

  def preprocess(self, data):
    dataframe = pd.read_csv(data, sep="\t")
    dataframe = dataframe.assign(Salinity=35)
    return pd.to_csv(dataframe, sep="\t")
