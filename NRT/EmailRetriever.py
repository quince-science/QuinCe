from DataRetriever import DataRetriever

class EmailConfiguration(DataRetriever):

  def __init__(self):
    super().__init__()
    self.configuration["Server"] = None
    self.configuration["User"] = None
    self.configuration["Password"] = None


  @staticmethod
  def get_type():
    return "Email"
