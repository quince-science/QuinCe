from DataRetriever import DataRetriever

class FileConfiguration(DataRetriever):

  # Constructor. Preset configuration is optional
  def __init__(self, configuration=None):
    super().__init__()
    if configuration is None:
      self.configuration["Source Dir"] = None
      self.configuration["Success Dir"] = None
      self.configuration["Failure Dir"] = None
    else:
      self.configuration = configuration

  @staticmethod
  def get_type():
    return "File"
