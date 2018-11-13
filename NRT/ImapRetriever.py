from DataRetriever import DataRetriever
from imapclient import IMAPClient
import email
from email.header import decode_header
import traceback, logging

class ImapConfiguration(DataRetriever):

  def __init__(self, instrument_id, logger, configuration=None):
    super().__init__(instrument_id, logger)
    if configuration is None:
      self.configuration["Server"] = None
      self.configuration["Port"] = None
      self.configuration["User"] = None
      self.configuration["Password"] = None
      self.configuration["Source Folder"] = None
      self.configuration["Downloaded Folder"] = None
    else:
      self.configuration = configuration

    # For storing the current IMAP connection
    self.imapconn = None

    # For storing the current message ID during processing
    self.current_id = None

  @staticmethod
  def get_type():
    return "IMAP Email"

  # Check that the configuration works
  def test_configuration(self):

    config_ok = True
    logged_in = False

    # Connect
    try:
      self.imapconn = IMAPClient(host=self.configuration["Server"],
        port=self.configuration["Port"])

    except:
      self.log(logging.CRITICAL, "Cannot connect to IMAP server: " + traceback.format_exc())
      config_ok = False

    # Authenticate
    try:
      if self.imapconn is not None:
        self.imapconn.login(self.configuration["User"],
          self.configuration["Password"])

        logged_in = True

        # Check folders
        if not self.imapconn.folder_exists(self.configuration["Source Folder"]):
          self.log(logging.CRITICAL, "Source Folder does not exist")
          config_ok = False

        if not self.imapconn.folder_exists(self.configuration["Downloaded Folder"]):
          self.log(logging.CRITICAL, "Downloaded Folder does not exist")
          config_ok = False

    except:
      self.log(logging.CRITICAL, "Cannot log in to IMAP server: " + traceback.format_exc())
      config_ok = False

    # Shut everything down
    self.shutdown()

    return config_ok

  # Initialise a connection to the IMAP server
  def startup(self):
    result = True

    try:
      # Log in to the mail server
      self.imapconn = IMAPClient(host=self.configuration["Server"],
          port=self.configuration["Port"])
      self.imapconn.login(self.configuration["User"],
            self.configuration["Password"])

      # Get the list of messages we can process
      self.imapconn.select_folder(self.configuration["Source Folder"])
      self.message_ids = self.imapconn.search(["NOT", "DELETED"])
      self.current_index = -1

    except:
      self.log(logging.CRITICAL, "Cannot log in to IMAP server: "
        + traceback.format_exc())
      result = False

    return result

  # Shutdown the server connection
  def shutdown(self):
    if self.imapconn is not None:
      try:
        if logged_in:
          self.imapconn.logout()

        self.imapconn.shutdown()
      except:
        # Don't care
        pass

    self.imapconn = None


  # Get the next message and extract its attachment
  def _retrieve_next_file(self):

    try:
      file_found = False
      self.current_index = self.current_index + 1
      while not file_found and self.current_index < len(self.message_ids):

        message_content = self.imapconn.fetch(self.message_ids[self.current_index], "RFC822") \
              [self.message_ids[self.current_index]][b"RFC822"]

        message = email.message_from_bytes(message_content)

        for part in message.walk():
          content_disposition = part.get("Content-Disposition")
          if content_disposition is not None and \
              content_disposition.startswith("attachment"):

            filename = self._extract_filename(part.get_filename())
            contents = part.get_payload(decode=True)
            self._add_file(filename, contents)
            file_found = True

        if not file_found:
          self.imapconn.move(self.message_ids[self.current_index],
            self.configuration["Downloaded Folder"])

          self.current_index = self.current_index + 1

    except:
      self.log(logging.ERROR, "Failed to retrieve next file: "
        + traceback.format_exc())

  # Extract a filename, handling encoded filenames as required
  def _extract_filename(self, filename):
    result = filename

    if decode_header(filename)[0][1] is not None:
      result = decode_header(filename)[0][0].decode(decode_header(filename)[0][1])

    return result

  # The file(s) have been processed successfully;
  # clean them up accordingly
  def _cleanup_success(self):
    try:
      self.imapconn.move(self.message_ids[self.current_index],
        self.configuration["Downloaded Folder"])
    except:
      self.log(logging.ERROR, "Failed to move email after processing: "
        + traceback.format_exc())

  # The file(s) were not processed successfully;
  # clean them up accordingly
  def _cleanup_fail(self):
    # We don't do anything - we'll try to process
    # the mail again next time round
    pass
