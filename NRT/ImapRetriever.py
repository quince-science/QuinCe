from DataRetriever import DataRetriever

class ImapConfiguration(DataRetriever):

  def __init__(self, configuration=None):
    super().__init__()
    if configuration is None:
      self.configuration["Server"] = None
      self.configuration["Port"] = None
      self.configuration["User"] = None
      self.configuration["Password"] = None
      self.configuration["Source Folder"] = None
      self.configuration["Downloaded Folder"] = None
    else:
      self.configuration = configuration

  @staticmethod
  def get_type():
    return "IMAP Email"

  def _test_configuration(self):
    pass
