"""
File retriever for Google Drive sources

Requires a Credentials file from a Google Service Account to be configured.
"""
import io
import json
from dateutil import parser
from modules.FileRetriever import FileRetriever
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaIoBaseDownload


# If modifying these scopes, delete the file token.json.
SCOPES = ["https://www.googleapis.com/auth/drive.metadata.readonly", "https://www.googleapis.com/auth/drive.readonly"]


class GoogleDriveRetriever(FileRetriever.FileRetriever):
    def __init__(self, station_name, config):
        super().__init__(station_name, config)
        self.service = build("drive", "v3", credentials=self._authenticate())

    def get_files(self, last_file, last_file_date):
        file_query = f'"{self.config["folder_id"]}" in parents and mimeType = "text/plain"'
        if last_file_date is not None:
            file_query += f' and modifiedTime > "{last_file_date.isoformat()}Z"'

        finished = False
        next_page_token = None
        file_ids = []

        while not finished:
            file_list = (self.service.files()
                         .list(orderBy='modifiedTime', q=file_query, pageSize=1000, pageToken=next_page_token,
                               supportsAllDrives=True, includeItemsFromAllDrives=True)
                         .execute())

            for found_file in file_list['files']:
                file_ids.append([found_file['id'], found_file['name']])

            if 'nextPageToken' in file_list:
                next_page_token = file_list['nextPageToken']
            else:
                finished = True

        return file_ids

    def get_file(self, file_id):
        request = self.service.files().get_media(fileId=file_id)
        content = io.BytesIO()
        downloader = MediaIoBaseDownload(content, request)
        done = False
        while not done:
            status, done = downloader.next_chunk()

        return content.getvalue().decode('utf-8')

    def get_file_details(self, file_id):
        file_details = (self.service.files().get(fileId=file_id, supportsAllDrives=True, fields='name, modifiedTime').execute())
        return file_details['name'], parser.parse(file_details['modifiedTime'])

    def _authenticate(self):
        with open(self.config['credentials_file']) as cred_file:
            cred_json = json.load(cred_file)

        return service_account.Credentials.from_service_account_info(cred_json)
