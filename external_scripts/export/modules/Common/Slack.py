'''
Local QuinCe module
Contains functions related to API-calls towards QuinCe

Maren K. Karlsen 2020.10.29
'''
import toml
import logging
from slack_sdk import WebClient


logging.getLogger('slack_sdk').setLevel(logging.WARNING)

with open('config_slack.toml') as f: CONFIG = toml.load(f)

def post_slack_msg(message,status=False):
  if status: workspace = 'err_workspace'
  else: workspace = 'rep_workspace'

  client = WebClient(token=CONFIG['slack']['api_token'])
  client.chat_postMessage(channel='#'+CONFIG['slack'][workspace], text=f'{message}')


def slack_export_report(destination,platform_name,dataset,successful_upload,err_msg):
  slack_msg = platform_name + ' : ' + dataset['name'] + ' - ' + destination + ' - '
  if successful_upload == 0: 
    slack_msg += 'Export failed. ' + str(err_msg)
  elif successful_upload == 1: slack_msg += 'Successfully exported.'
  elif successful_upload == 2: slack_msg += 'No new data.'
  else: slack_msg += 'Something went wrong. Check log.'
  post_slack_msg(slack_msg)
