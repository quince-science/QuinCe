'''
Local QuinCe module
Contains functions related to API-calls towards QuinCe

Maren K. Karlsen 2020.10.29
'''
import logging 
import urllib
import base64
import toml
import json
import sys
import os
import re
import io
from slacker import Slacker

from xml.etree import ElementTree as ET
from zipfile import ZipFile

with open('config_slack.toml') as f: CONFIG = toml.load(f)


def post_slack_msg(message,status=0):
  if status == 1: workspace = 'err_workspace'
  else: workspace = 'rep_workspace' 

  slack = Slacker(CONFIG['slack']['api_token'])
  slack.chat.post_message('#'+CONFIG['slack'][workspace],f'{message}')


def slack_export_report(destination,platform_name,dataset,successful_upload,err_msg):
  slack_msg = platform_name + ' : ' + dataset['name'] + ' - ' + destination + ' - '
  if successful_upload == 0: 
    slack_msg += 'Export failed. ' + str(err_msg)
  elif successful_upload == 1: slack_msg += 'Successfully exported.'
  elif successful_upload == 2: slack_msg += 'No new data.'
  else: slack_msg += 'Something went wrong. Check log.'
  post_slack_msg(slack_msg)
       