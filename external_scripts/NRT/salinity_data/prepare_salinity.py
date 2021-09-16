# Prepare a cut-down version of the World Ocean Atlas 2018 salinity
# data to use with the AddSalinityPreprocessor.

# Input files are 0.25° seasonal files for the years 2005-2017,
# available from https://www.nodc.noaa.gov/cgi-bin/OC5/woa18/woa18.pl

# Files are:
#
# woa18_A5B7_s13_04.nc - Winter (DJF) = Season 1
# woa18_A5B7_s14_04.nc - Spring (MAM) = Season 2
# woa18_A5B7_s15_04.nc - Summer (JJA) = Season 3
# woa18_A5B7_s16_04.nc - Winter (SON) = Season 4
#
#
# Output is a single netCDF file, containing the surface data for the full grid
# and four time steps.
import os
import netCDF4

WINTER_FILE = "woa18_A5B7_s13_04.nc"
SPRING_FILE = "woa18_A5B7_s14_04.nc"
SUMMER_FILE = "woa18_A5B7_s15_04.nc"
AUTUMN_FILE = "woa18_A5B7_s16_04.nc"
IN_VAR = "s_an"

OUTPUT_FILE = "woa18_seasonal_surface_salinity.nc"


def main():
    if not init_check():
        print("Initialisation check failed.")
        exit()

    init_output_file()
    add_season(WINTER_FILE, 0)
    add_season(SPRING_FILE, 1)
    add_season(SUMMER_FILE, 2)
    add_season(AUTUMN_FILE, 3)


# Initialisation check
def init_check():
    check_result = True

    if not file_exists(WINTER_FILE):
        check_result = False
    if not file_exists(SPRING_FILE):
        check_result = False
    if not file_exists(SUMMER_FILE):
        check_result = False
    if not file_exists(SPRING_FILE):
        check_result = False

    return check_result


# See if a file exists
def file_exists(file):
    exists = True

    if not os.path.isfile(file):
        print("Missing file %s" % file)
        exists = False

    return exists


def init_output_file():
    # Get spatial dimensions from input file
    nc = netCDF4.Dataset(WINTER_FILE, mode="r")
    lons = nc.variables["lon"][:]
    lats = nc.variables["lat"][:]
    nc.close()

    nc = netCDF4.Dataset(OUTPUT_FILE, format="NETCDF4_CLASSIC", mode="w")

    nc.createDimension("lon", len(lons))
    lon_var = nc.createVariable("lon", "f", "lon", fill_value=-999)
    lon_var.units = "degrees_east"

    nc.createDimension("lat", len(lats))
    lat_var = nc.createVariable("lat", "f", "lat", fill_value=-999)
    lat_var.units = "degrees_north"

    nc.createDimension("time", 4)
    time_var = nc.createVariable("time", "i", "time", fill_value=-1)
    time_var.units = "season"
    time_var.long_name = "season"

    nc.createVariable("salinity", "d", ("time", "lat", "lon"), fill_value=-999)

    lon_var[:] = lons
    lat_var[:] = lats
    time_var[:] = [1, 2, 3, 4]

    nc.close()


def add_season(season_file, season):
    nc = netCDF4.Dataset(season_file, mode="r")
    values = nc.variables[IN_VAR][0, 0, :, :]
    nc.close()

    nc = netCDF4.Dataset(OUTPUT_FILE, mode="a")
    nc.variables["salinity"][season, :, :] = values
    nc.close()


if __name__ == '__main__':
    main()
