from DataRetriever import DataRetriever
from imapclient import IMAPClient
import sys

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

    config_ok = True
    imapconn = None
    logged_in = False

    # Connect
    try:
      imapconn = IMAPClient(host=self.configuration["Server"],
        port=self.configuration["Port"])

    except:
      print("Cannot connect to IMAP server: " + str(sys.exc_info()[0]))
      config_ok = False

    # Authenticate
    try:
      if imapconn is not None:
        imapconn.login(self.configuration["User"],
          self.configuration["Password"])

        logged_in = True

        # Check folders
        if not imapconn.folder_exists(self.configuration["Source Folder"]):
          print("Source Folder does not exist")
          config_ok = False

        if not imapconn.folder_exists(self.configuration["Downloaded Folder"]):
          print("Downloaded Folder does not exist")
          config_ok = False

    except:
      print("Cannot log in to IMAP server: " + str(sys.exc_info()[0]))
      config_ok = False

    # Shut everything down
    if imapconn is not None:
      try:
        if logged_in:
          imapconn.logout()

        imapconn.shutdown()
      except:
        # Don't care
        pass

    return config_ok
