"""
Convert a QuinCe CSV output file to netCDF
"""
import argparse


def make_netcdf():
    """
    Main program function
    """
    print("Hello")


# Command line processor
if __name__ == '__main__':

    parser = argparse.ArgumentParser(description="Convert a QuinCe output file to netCDF")
    parser.add_argument("config_file", help="Configuration file")
    parser.add_argument("csv_file", help="QuinCe CSV File")

    args = parser.parse_args()

    make_netcdf()
