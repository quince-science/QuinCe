from Preprocessor import Preprocessor

class NonePreprocessor(Preprocessor):
  def get_name():
    return "None"

    def preprocess(self, data):
      return data