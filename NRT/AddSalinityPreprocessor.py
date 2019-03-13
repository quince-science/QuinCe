from Preprocessor import Preprocessor

class AddSalinityPreprocessor(Preprocessor):
  def get_name():
    return "Add Fixed Salinity"

  def preprocess(self, data):
    return data