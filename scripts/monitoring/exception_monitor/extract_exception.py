# This script looks for exceptions in the Tomcat log
# Any exceptions it finds are emailed to selected email addresses

import os
import toml
from slack_sdk import WebClient
import requests

# Constants
LAST_LINE_FILE = 'lastLine.txt'
SEARCHING_MODE = 0
EXCEPTION_MODE = 1


# The last line we processed is stored in a file.
# These are two functions to load and store it
def load_last_line():
    last_line = -1

    if os.path.isdir(LAST_LINE_FILE):
        print('Last line file is a directory!')
        quit()
    elif os.path.isfile(LAST_LINE_FILE):
        if not os.access(LAST_LINE_FILE, os.R_OK | os.W_OK):
            print('Cannot access last line file for read/write')
            quit()
        else:
            try:
                with open(LAST_LINE_FILE, 'r') as llf:
                    last_line = int(llf.read())
            except (TypeError, ValueError):
                print('Last line file does not contain an integer')
                quit()

    return last_line


def save_last_line(last_line):
    if (os.path.isfile(LAST_LINE_FILE)
            and not os.access(LAST_LINE_FILE, os.W_OK)):

        print('Cannot access last line file for writing')
        quit()
    else:
        with open(LAST_LINE_FILE, "w") as llf:
            llf.write(str(last_line))


# Read the log file and return it as an array of lines
def get_file_lines(log_file):
    contents = None
    lines = None

    if not os.access(log_file, os.R_OK):
        print('Log file does not exist or cannot be read')
        quit()
    else:
        file = open(log_file, 'r')
        contents = file.read()
        file.close()

    if contents is not None:
        lines = contents.splitlines()

    return lines


def post_slack_msg(config, message):
    client = WebClient(token=config['slack']['api_token'])
    client.chat_postMessage(channel='#'+config['slack']['workspace'], text=f'{message}')


def post_telegram_msg(config, message):
    url = f"https://api.telegram.org/bot{config['telegram']['token']}/sendMessage?chat_id={config['telegram']['chat_id']}&text=EXCEPTION MONITOR: {message}"
    r = requests.get(url)


def main(config):
    last_line = load_last_line()
    exceptions = []

    # Load the log file
    log_data = get_file_lines(config['log']['file'])

    # If the last line is larger than the file, we start again
    if last_line > len(log_data):
        last_line = -1

    # Work our way through the file
    current_line = last_line + 1
    mode = SEARCHING_MODE
    exception = None

    while current_line < len(log_data):

        line = log_data[current_line]

        # Currently processing an exception
        if mode == EXCEPTION_MODE:
            # Continued exceptions start with "at" or
            # "Caused by" (trimmed strings)
            trimmed = line.lstrip()
            if trimmed.startswith('at') or trimmed.startswith('Caused by'):
                exception.append(line)
            else:
                # Only add the exception if it's not a broken pipe
                if not any('Broken pipe' in line for line in exception):
                    exceptions.append('\n'.join(exception))
                mode = SEARCHING_MODE

        else:  # We are searching for an exception
            # If we see the word Exception, start a new Exception
            if line.find('Exception') > -1:
                exception = [line]
                mode = EXCEPTION_MODE

        # Go to the next line
        current_line += 1

    # Tidy up the last exception, if that was the last thing in the file
    if mode == EXCEPTION_MODE:
        if not any('Broken pipe' in line for line in exception):
            exceptions.append('\n'.join(exception))

    if len(exceptions) > 0:

        # Send an HTTP POST with the exceptions we found
        error_log = f'Exceptions found in {config["log"]["file"]}'

        message_destination = config['messages']['destination']
        if message_destination == 'slack':
            for exception in exceptions:
                error_log = error_log + "\n\n"
                error_log = error_log + exception

            post_slack_msg(config, error_log)
        elif message_destination == 'telegram':
            for exception in exceptions:
                post_telegram_msg(config, exception)
        else:
            print('UNRECOGNISED MESSAGE DESTINATION')
            print(error_log)

    # Store the last processed line ready for next time
    save_last_line(current_line - 1)


#######################################################
# Here we go
if __name__ == '__main__':
    with open('config.toml', 'r') as config_file:
        config = toml.load(config_file)

    main(config)
