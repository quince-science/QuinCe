import traceback
from datetime import datetime
from contextlib import closing
import sqlite3
from slack_sdk import WebClient
import toml
from modules.FileRetriever import FileRetrieverFactory
from modules.Splitter import Splitter


# Database details
DB_FILE = 'file_splitter.sqlite'
DB_TABLE = 'station'

# Global Slack Config
_SLACK_CONFIG = None


def post_slack_msg(message):
    """
    Post a message to Slack
    :param message: The message
    :return: Nothing
    """
    client = WebClient(token=_SLACK_CONFIG['api_token'])
    client.chat_postMessage(channel='#' + _SLACK_CONFIG['workspace'], text=f'{message}')
    # print(message)


def main():
    """
    Main method
    :return: Nothing
    """

    # Load the configuration file
    # Make sure there's a Slack entry, but after that the config
    # is processed separately.
    with open("config.toml", "r") as config_file:
        config = toml.loads(config_file.read())

    if '_slack' not in config:
        print('Missing Slack config')
        exit()

    global _SLACK_CONFIG
    _SLACK_CONFIG = config['_slack']

    with sqlite3.connect(DB_FILE) as db:
        # Make sure the database is set up
        init_db(db)

        for station in config.keys():
            if not station.startswith('_'):
                print(f'Processing station {station}')
                process_station(db, station, config[station])


def process_station(db, name, config):
    """
    Process a station
    :param db: Database connection
    :param name: Station name
    :param config: Station configuration
    :return: Nothing
    """
    # noinspection PyBroadException
    try:
        # Get the last file processed previously
        last_processed_file, last_processed_date = get_last_processed_file(db, name)

        # Retrieve all the new files from the source
        retriever = FileRetrieverFactory.get_retriever(name, config['source'])
        file_list = retriever.get_files(last_processed_file, last_processed_date)

        for file_id, filename in file_list:
            try:
                # Initialise the splitter
                splitter = Splitter(config['splitter'])

                file_content = retriever.get_file(file_id)
                splitter.set_data(filename, file_content)

                # Write the output files
                splitter.write_output(config['output_location'])
            except Exception:
                post_slack_msg(f'Error processing {filename}:\n{traceback.format_exc()}')

        # Store the details of the last file
        last_file_id = file_list[-1]
        filename, modification_date = retriever.get_file_details(last_file_id)
        record_last_file(db, name, filename, modification_date)

    except Exception:
        post_slack_msg(f'Error processing station {name}:\n{traceback.format_exc()}')


def record_last_file(db, station, file, file_date):
    if not station_exists(db, station):
        with closing(db.cursor()) as c:
            c.execute('INSERT INTO station VALUES (?, ?, ?)', [station, file, int(file_date.timestamp())])
    else:
        with closing(db.cursor()) as c:
            c.execute('UPDATE station SET last_file=?, last_file_time=? WHERE name=?',
                      [file, int(file_date.timestamp()), station])


def station_exists(db, station):
    with closing(db.cursor()) as c:
        c.execute(f'SELECT count(*) FROM station WHERE name="{station}"')
        return c.fetchone()[0] > 0


def get_last_processed_file(db, station):
    """
    Get the name of the last file processed for the given station
    :param db: Database connection
    :param station: The station name
    :return: The last processed filename
    """
    with closing(db.cursor()) as c:
        c.execute(f'SELECT last_file, last_file_time FROM station WHERE name = "{station}"')
        record = c.fetchone()
        if record is None:
            return None, None
        else:
            return record[0], datetime.utcfromtimestamp(record[1])


def init_db(db):
    """
    Make sure the database has the required structure
    :param db: Database connection
    :return: Nothing
    """
    with closing(db.cursor()) as c:
        c.execute(f'SELECT count(*) FROM sqlite_master WHERE type="table" AND name="{DB_TABLE}"')
        has_table = c.fetchone()[0] > 0

    if not has_table:
        with closing(db.cursor()) as c:
            c.execute('''CREATE TABLE station (
                name TEXT PRIMARY KEY,
                last_file TEXT,
                last_file_time INTEGER
            )''')


if __name__ == '__main__':
    main()
