import json
import logging
import os
import urllib.error

import toml
from tabulate import tabulate

import PreprocessorFactory
import RetrieverFactory
import nrtdb
import nrtftp
# Local modules
import quince

'''
Extract the list of IDs from a set of instruments
'''


def get_ids(instruments):
    result = []
    for instrument in instruments:
        result.append(instrument["id"])
    return result


'''
Draw the table of instruments
'''


def make_instrument_table(instruments, ids, show_type):
    if show_type:
        table_data = [["ID", "Name", "Owner", "Type", "Preprocessor"]]
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
                                   "None" if instrument["type"] is None
                                   else instrument["type"],
                                   instrument["preprocessor"]])
            else:
                table_data.append([instrument["id"],
                                   instrument["name"],
                                   instrument["owner"]])

    print(tabulate(table_data, headers="firstrow"))


#######################################################

def main():
    ftp_conn = None
    db_conn = None

    try:
        # Blank logger - sends everything to /dev/null
        logging.basicConfig(filename=os.devnull)
        logger = logging.getLogger('configure_nrt')
        logger.setLevel(level=10)

        with open("config.toml", "r") as config_file:
            config = toml.loads(config_file.read())

        print("Connecting to FTP servers...")
        ftp_conn = nrtftp.connect_ftp(config["FTP"])
        db_conn = nrtdb.get_db_conn(config["Database"]["location"])

        print("Getting QuinCe instruments...")
        quince_instruments = quince.get_instruments(config)

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

            command = input("\nEnter instrument ID to configure, or Q to quit: ").lower()

            if command == "q":
                finish = True
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

                        print()

                        change = input("Change configuration (y/n)? ").lower()

                        if change == "y":
                            new_type = RetrieverFactory.ask_retriever_type()
                            if new_type is None:
                                nrtdb.store_configuration(db_conn, instrument["id"], None, None)
                            else:
                                if new_type != instrument["type"]:
                                    retriever = RetrieverFactory.get_new_instance(new_type)

                                config_ok = False
                                while not config_ok:
                                    print()
                                    config_ok = retriever.enter_configuration()

                                print()
                                preprocessor = PreprocessorFactory.ask_preprocessor()
                                nrtdb.store_configuration(db_conn, instrument["id"], retriever, preprocessor)
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
