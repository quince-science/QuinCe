from DataRetriever import DataRetriever

class FileConfiguration(DataRetriever):

  def __init__(self):
    super().__init__()

  @staticmethod
  def get_type():
    return "File"
