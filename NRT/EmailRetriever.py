from DataRetriever import DataRetriever

class EmailConfiguration(DataRetriever):

  def __init__(self, configuration=None):
    super().__init__()
    if configuration is None:
      self.configuration["Server"] = None
      self.configuration["User"] = None
      self.configuration["Password"] = None
    else:
      self.configuration = configuration

  @staticmethod
  def get_type():
    return "Email"
