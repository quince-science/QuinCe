from DataRetriever import DataRetriever

class FileConfiguration(DataRetriever):

  # Constructor. Preset configuration is optional
  def __init__(self, configuration=None):
    super().__init__()
    if configuration is None:
      pass
    else:
      self.configuration = configuration

  @staticmethod
  def get_type():
    return "File"
