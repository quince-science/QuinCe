"""
Convert a QuinCe CSV output file to netCDF
"""
import argparse
import os.path

import yaml
import pandas as pd
from datetime import datetime
from netCDF4 import Dataset
from cftime import date2num


def make_netcdf(config, in_file):
    """
    Main program function
    """
    out_file = f"{os.path.splitext(in_file)[0]}.nc"

    quince = pd.read_csv(in_file, parse_dates=["Date/Time"], low_memory=False)
    with Dataset(out_file, "w", format="NETCDF4") as nc:

        # Set global attributes from config
        for attr in config["global_attributes"]:
            nc.setncattr(attr, config["global_attributes"][attr])

        nc.setncattr("creation_date", datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ'))

        nc.createDimension("time")
        timevar = nc.createVariable("time", "f8", ("time",))
        timevar.units = "days since 1970-01-01 00:00:00.0"

        timevar[:] = date2num(quince["Date/Time"], timevar.units)

        for column in config["columns"]:
            column_info = config["columns"][column]
            var = nc.createVariable(column_info["variable"], column_info["type"], ("time",))
            var.units = column_info["units"]
            var[:] = quince[column]


def parse_config(config_file):
    """
    Check the supplied configuration file
    :param config_file: The path to the config file
    :return: true/false if the config file is good/bad
    """
    with open(config_file, "r") as stream:
        return yaml.safe_load(stream)


def check_input_file(path):
    if not os.path.exists(path):
        print("Specified input file does not exist")
        return False

    try:
        with open(path, "r") as csv:
            return True
    except OSError as ex:
        print(f"Failed to open input file: {ex}")
        return False


# Command line processor
if __name__ == "__main__":

    parser = argparse.ArgumentParser(description="Convert a QuinCe output file to netCDF")
    parser.add_argument("config_file", help="Configuration file")
    parser.add_argument("csv_file", help="QuinCe CSV File")

    args = parser.parse_args()

    try:
        parsed_config = parse_config(args.config_file)
    except Exception as e:
        print(f"Failed to parse configuration file: {e}")
        exit()

    if not check_input_file(args.csv_file):
        exit()

    make_netcdf(parsed_config, args.csv_file)
