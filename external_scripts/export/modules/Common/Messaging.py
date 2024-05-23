"""
Local QuinCe module
Contains functions related to API-calls towards QuinCe
"""
import toml
import logging
from slack_sdk import WebClient
import requests

logging.getLogger('export_messaging').setLevel(logging.WARNING)

with open('config_messaging.toml') as f:
    CONFIG = toml.load(f)


def post_msg(message, status=False):
    message_destination = CONFIG['messaging']['destination']

    if message_destination == 'slack':
        _post_slack_msg(message, status)
    elif message_destination == 'telegram':
        _post_telegram_msg(message)
    else:
        raise ValueError('Unrecognised message destination')


def export_report(destination, platform_name, dataset, successful_upload, err_msg):
    message = _make_export_report(destination, platform_name, dataset, successful_upload, err_msg)
    post_msg(message)


def _post_slack_msg(message, status=False):
    if status:
        workspace = 'err_workspace'
    else:
        workspace = 'rep_workspace'

    client = WebClient(token=CONFIG['slack']['api_token'])
    client.chat_postMessage(channel='#' + CONFIG['slack'][workspace], text=f'{message}')


def _make_export_report(destination, platform_name, dataset, successful_upload, err_msg):
    result = platform_name + ' : ' + dataset['name'] + ' - ' + destination + ' - '
    if successful_upload == 0:
        result += 'Export failed. ' + str(err_msg)
    elif successful_upload == 1:
        result += 'Successfully exported.'
    elif successful_upload == 2:
        result += 'No new data.'
    else:
        result += 'Something went wrong. Check log.'

    return result


def _post_telegram_msg(message):
    token = CONFIG['telegram']['token']
    chat_id = CONFIG['telegram']['chat_id']
    url = f"https://api.telegram.org/bot{token}/sendMessage?chat_id={chat_id}&text=EXPORT: {message}"
    requests.get(url)
