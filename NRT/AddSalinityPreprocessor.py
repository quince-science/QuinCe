from io import BytesIO
import pandas as pd
from Preprocessor import Preprocessor

class AddSalinityPreprocessor(Preprocessor):
  def get_name():
    return "Add Fixed Salinity"

  def preprocess(self, data):
    dataframe = pd.read_csv(data, sep="\t")
    dataframe = dataframe.assign(Salinity=35)
    return dataframe.to_csv(sep="\t", index=False).encode("utf-8")
