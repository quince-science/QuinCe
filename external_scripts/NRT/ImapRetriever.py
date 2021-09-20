import email
import logging
import traceback
from email.header import decode_header

from imapclient import IMAPClient

from DataRetriever import DataRetriever

'''
Extract a filename, handling encoded filenames as required
'''


def _extract_filename(filename):
    result = filename

    if decode_header(filename)[0][1] is not None:
        result = decode_header(filename)[0][0].decode(decode_header(filename)[0][1])

    return result


class ImapRetriever(DataRetriever):

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
        self.imap_conn = None

        # For storing the current message ID during processing
        self.current_id = None

        # Message ID list
        self.message_ids = []

        # Current message index
        self.current_index = None

    @staticmethod
    def get_type():
        return "IMAP Email"

    # Check that the configuration works
    def test_configuration(self):

        config_ok = True

        # Connect
        try:
            self.imap_conn = IMAPClient(host=self.configuration["Server"],
                                        port=self.configuration["Port"])

        except IMAPClient.Error:
            self.log(logging.CRITICAL, "Cannot connect to IMAP server: "
                     + traceback.format_exc())
            config_ok = False

        # Authenticate
        try:
            if self.imap_conn is not None:
                self.imap_conn.login(self.configuration["User"],
                                     self.configuration["Password"])

                # Check folders
                if not self.imap_conn.folder_exists(self.configuration["Source Folder"]):
                    self.log(logging.CRITICAL, "Source Folder does not exist")
                    config_ok = False

                if not self.imap_conn.folder_exists(self.configuration["Downloaded Folder"]):
                    self.log(logging.CRITICAL, "Downloaded Folder does not exist")
                    config_ok = False

        except IMAPClient.Error:
            self.log(logging.CRITICAL, "Cannot log in to IMAP server: "
                     + traceback.format_exc())
            config_ok = False

        # Shut everything down
        self.shutdown()

        return config_ok

    # Initialise a connection to the IMAP server
    def startup(self):
        result = True

        try:
            # Log in to the mail server
            self.imap_conn = IMAPClient(host=self.configuration["Server"],
                                        port=self.configuration["Port"])
            self.imap_conn.login(self.configuration["User"],
                                 self.configuration["Password"])

            # Get the list of messages we can process
            self.imap_conn.select_folder(self.configuration["Source Folder"])
            self.message_ids = self.imap_conn.search(["NOT", "DELETED"])
            self.current_index = -1

        except IMAPClient.Error:
            self.log(logging.CRITICAL, "Cannot log in to IMAP server: "
                     + traceback.format_exc())
            result = False

        return result

    # Shutdown the server connection
    def shutdown(self):
        if self.imap_conn is not None:
            try:
                self.imap_conn.logout()
            except IMAPClient.Error:
                # Don't care
                pass

        self.imap_conn = None

    # Get the next message and extract its attachment
    def _retrieve_next_file_set(self):

        try:
            file_found = False
            self.current_index = self.current_index + 1
            while not file_found and self.current_index < len(self.message_ids):
                self.log(logging.DEBUG, "Processing email ID " +
                         str(self.message_ids[self.current_index]))

                message_content = self.imap_conn.fetch(
                    self.message_ids[self.current_index], "RFC822")[self.message_ids[self.current_index]][b"RFC822"]

                message = email.message_from_bytes(message_content)

                for part in message.walk():
                    content_disposition = part.get("Content-Disposition")
                    if content_disposition is not None and \
                            content_disposition.startswith("attachment"):
                        filename = _extract_filename(part.get_filename())
                        self.log(logging.DEBUG, "Extracting attachment " + filename)
                        contents = part.get_payload(decode=True)
                        self._add_file(filename, contents)
                        file_found = True

                if not file_found:
                    self.imap_conn.move(self.message_ids[self.current_index],
                                        self.configuration["Downloaded Folder"])

                    self.current_index = self.current_index + 1

        except IMAPClient.Error:
            self.log(logging.ERROR, "Failed to retrieve next file: "
                     + traceback.format_exc())

    # The file(s) have been processed successfully;
    # clean them up accordingly
    def _cleanup_success(self):
        try:
            self.log(logging.DEBUG, "Moving email "
                     + str(self.message_ids[self.current_index]) + " to downloaded folder")

            self.imap_conn.move(self.message_ids[self.current_index],
                                self.configuration["Downloaded Folder"])
        except IMAPClient.Error:
            self.log(logging.ERROR, "Failed to move email after processing: "
                     + traceback.format_exc())

    # The file(s) were not processed successfully;
    # clean them up accordingly
    def _cleanup_fail(self):
        # We don't do anything - we'll try to process
        # the mail again next time round
        pass

    # The file(s) were not processed
    def _cleanup_not_processed(self):
        # No action required
        pass
