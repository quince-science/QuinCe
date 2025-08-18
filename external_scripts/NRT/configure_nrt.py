import json
import logging
import os
import urllib.error

import toml
from tabulate import tabulate

# Local modules
from modules.Preprocessor import PreprocessorFactory
from modules.Retriever import RetrieverFactory
import quince
import nrtdb
import nrtftp


def get_ids(instruments):
    """
    Extract the list of IDs from a set of instruments
    """
    result = []
    for instrument in instruments:
        result.append(instrument["id"])
    return result


def make_instrument_table(instruments, ids, show_type):
    """
    Draw the table of instruments
    """
    if show_type:
        table_data = [["ID", "Name", "Owner", "Type", "Preprocessor", "Check Hours", "Paused"]]
    else:
        table_data = [["ID", "Name", "Owner"]]

    for instrument in instruments:
        draw = True
        if ids is not None and instrument["id"] not in ids:
            draw = False

        if draw:
            if show_type:
                table_data.append([instrument["id"],
                                   instrument["name"],
                                   instrument["owner"],
                                   "None" if instrument["type"] is None else instrument["type"],
                                   instrument["preprocessor"],
                                   instrument["check_hours"],
                                   instrument["paused"]])
            else:
                table_data.append([instrument["id"],
                                   instrument["name"],
                                   instrument["owner"]])

    print(tabulate(table_data, headers="firstrow"))


def ask_check_hours(existing):
    """
    Ask the user to specify which hours the NRT source should be checked.
    """
    new_value = None

    print()
    while new_value is None:
        input_value = input("Check hours (comma separated) %s: " % existing).strip()
        if input_value == "":
            if existing is not None:
                new_value = existing
        else:
            parsed_value = parse_check_hours(input_value)
            if parsed_value is not None:
                new_value = parsed_value

    return new_value


def parse_check_hours(hours):
    ok = True

    entry_list = hours.split(",")

    hour_list = []

    try:
        for e in entry_list:
            hour = int(e)
            if 0 <= hour <= 23:
                hour_list.append(hour)
    except ValueError:
        ok = False

    if not ok:
        return None
    else:
        # Sort/unique
        list(dict.fromkeys(hour_list)).sort()
        return hour_list

def pause_instrument(db_conn):
    id = input("\nEnter instrument ID to toggle pause status: ")

    instrument_id = None
    try:
        instrument_id = int(id)
    except ValueError:
        pass

    if instrument_id is not None:
        nrtdb.toggle_pause(db_conn, instrument_id)


#######################################################

def main():
    ftp_conn = None
    db_conn = None
    quince_instruments = None

    try:
        # Blank logger - sends everything to /dev/null
        logging.basicConfig(filename=os.devnull)
        logger = logging.getLogger('configure_nrt')
        logger.setLevel(level=10)

        with open("config.toml", "r") as config_file:
            config = toml.loads(config_file.read())

        db_conn = nrtdb.get_db_conn(config["Database"]["location"])

        print("Connecting to NRT Upload server...")
        try:
            ftp_conn = nrtftp.connect_ftp(config["FTP"])
        except Exception as e:
            print(f"Could not connect to server: {e}")
            exit()

        print("Getting QuinCe instruments...")
        try:
            quince_instruments = quince.get_instruments(config)
        except Exception as e:
            print(f"Could not retrieve information from QuinCe: {e}")
            exit()

        print("Getting NRT instruments...")
        nrt_instruments = nrtdb.get_instruments(db_conn)

        # Check that NRT and QuinCe are in sync
        quince_ids = get_ids(quince_instruments)
        nrt_ids = get_ids(nrt_instruments)

        # Remove old instruments from NRT
        orphaned_ids = list(set(nrt_ids) - set(quince_ids))
        if len(orphaned_ids) > 0:
            print("The following instruments are no longer in QuinCe and will be removed:\n")
            make_instrument_table(nrt_instruments, orphaned_ids, False)
            go = input("Enter Y to proceed, or anything else to quit: ")
            if not go.lower() == "y":
                exit()
            else:
                nrtdb.delete_instruments(db_conn, orphaned_ids)

        # Add new instruments from QuinCe
        new_ids = list(set(quince_ids) - set(nrt_ids))
        if len(new_ids) > 0:
            print("The following instruments are new in QuinCe and will be added:\n")
            make_instrument_table(quince_instruments, new_ids, False)
            go = input("Enter Y to proceed, or anything else to quit: ")
            if not go.lower() == "y":
                exit()
            else:
                nrtdb.add_instruments(db_conn, quince_instruments, new_ids)
                nrtftp.add_instruments(ftp_conn, config["FTP"], new_ids)

        # Main configuration loop
        finish = False
        while not finish:
            print()

            instruments = nrtdb.get_instruments(db_conn)
            make_instrument_table(instruments, None, True)

            command = input("\nEnter instrument ID to configure, P to pause/unpause and instrument, or Q to quit: ").lower()

            if command == "q":
                finish = True
            elif command == "p":
                pause_instrument(db_conn)
            else:
                instrument_id = None
                try:
                    instrument_id = int(command)
                except ValueError:
                    pass

                if instrument_id is not None:
                    instrument = nrtdb.get_instrument(db_conn, instrument_id)
                    if instrument is not None:
                        retriever = None

                        print()
                        print("Current configuration for instrument %d (%s):" %
                              (instrument["id"], instrument["name"]))
                        print()

                        print("TYPE: %s" % (instrument["type"]))
                        if instrument["type"] is not None and instrument["config"] is not None:
                            retriever = RetrieverFactory.get_instance(instrument["type"],
                                                                      instrument["id"], logger,
                                                                      json.loads(instrument["config"]))
                            retriever.print_configuration()
                            print()

                            print("PREPROCESSOR: %s" % instrument["preprocessor"])
                            if instrument["preprocessor"] is not None and instrument["preprocessor_config"] is not None:
                                preprocessor = PreprocessorFactory.get_instance(
                                    instrument["preprocessor"], logger, json.loads(instrument["preprocessor_config"]))
                                preprocessor.print_configuration()
                                print()

                        print()

                        change = input("Change configuration (y/n)? ").lower()

                        if change == "y":
                            new_type = RetrieverFactory.ask_retriever_type()
                            if new_type is None:
                                nrtdb.store_configuration(db_conn, instrument["id"], None, None, None)
                            else:
                                if new_type != instrument["type"]:
                                    retriever = RetrieverFactory.get_new_instance(new_type)

                                config_ok = not retriever.has_config()
                                while not config_ok:
                                    print()
                                    config_ok = retriever.enter_configuration()

                                new_preprocessor_type = PreprocessorFactory.ask_preprocessor_type()

                                if new_preprocessor_type != instrument["preprocessor"]:
                                    preprocessor = PreprocessorFactory.get_new_instance(new_preprocessor_type)
                                else:
                                    existing_config = "{}"
                                    if "preprocessor_config" in instrument \
                                            and instrument["preprocessor_config"] is not None:
                                        existing_config = instrument["preprocessor_config"]

                                    preprocessor = PreprocessorFactory.get_instance(new_preprocessor_type, logger,
                                                                                    json.loads(existing_config))

                                print()
                                config_ok = preprocessor.enter_configuration()
                                while not config_ok:
                                    print()
                                    config_ok = preprocessor.enter_configuration()

                                instrument["check_hours"] = ask_check_hours(instrument["check_hours"])

                                nrtdb.store_configuration(db_conn, instrument["id"], retriever, preprocessor,
                                                          instrument["check_hours"])
    except urllib.error.HTTPError as e:
        print("%s %s" % (e.code, e.reason))
    except urllib.error.URLError as e:
        print(e)
    finally:
        if db_conn is not None:
            nrtdb.close(db_conn)

        if ftp_conn is not None:
            ftp_conn.close()


if __name__ == '__main__':
    main()
