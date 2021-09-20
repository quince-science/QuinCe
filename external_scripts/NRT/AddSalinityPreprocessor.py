import pandas as pd
import numpy as np
from Preprocessor import Preprocessor
from netCDF4 import Dataset


def get_surrounding_cells(lon, lat, step, lon_max, lat_max):
    result = []

    if step == 0:
        result.append([lon, lat])
    else:
        # Loop through the latitudes from (lat - step) to (lat + step)
        for y in range(step * -1, step):
            cell_y = lat + y

            # Make sure we haven't fallen off the world
            if 0 <= cell_y < lat_max:

                # For the top and bottom rows of the step grid, add all horizontal cells
                if abs(y) == step:
                    for x in range(step * -1, step):
                        cell_x = lon + x
                        if 0 <= cell_x < lon_max:
                            result.append([cell_x, cell_y])
                else:
                    # For all other rows, just add the left and right edges
                    cell_x = lon - step
                    if 0 <= cell_x < lon_max:
                        result.append([cell_x, cell_y])

                    cell_x = lon + step
                    if 0 <= cell_x < lon_max:
                        result.append([cell_x, cell_y])

    return result


def get_season(date):
    season = -1
    month = int(date[3:5])

    if month in [12, 1, 2]:
        season = 0
    elif month in [3, 4, 5]:
        season = 1
    elif month in [6, 7, 8]:
        season = 2
    elif month in [9, 10, 11]:
        season = 3

    return season


def make_indices(dataframe):
    indices = pd.DataFrame(index=dataframe.index.values, columns=["lon", "lat", "season"])
    indices["lon"] = dataframe["longitude"].apply(lambda x: np.nan if np.isnan(x) else (round((x + 180) * 4)) - 1)
    indices["lat"] = dataframe["latitude"].apply(lambda x: np.nan if np.isnan(x) else (round((x + 90) * 4)) - 1)
    indices["season"] = dataframe["PC Date"].apply(lambda x: get_season(x))
    return indices


class AddSalinityPreprocessor(Preprocessor):

    SALINITY_FILE = "salinity_data/woa18_seasonal_surface_salinity.nc"
    SALINITY_VAR = "salinity"

    @staticmethod
    def get_type():
        return "Add Fixed Salinity"

    def preprocess(self, data):
        dataframe = pd.read_csv(data, sep="\t")
        indices = make_indices(dataframe)
        dataframe = dataframe.assign(Salinity=self.get_salinity(indices)["Salinity"])
        return dataframe.to_csv(sep="\t", index=False).encode("utf-8")

    def get_salinity(self, lookups):
        nc = Dataset(self.SALINITY_FILE, mode="r")
        lon_max = nc.variables["lon"].shape[0]
        lat_max = nc.variables["lat"].shape[0]

        salinities = pd.DataFrame(index=lookups.index.values, columns=["Salinity"])
        for index, row in lookups.iterrows():
            salinities.loc[index, "Salinity"] = self.read_salinity(nc, row["lon"], row["lat"], row["season"], lon_max,
                                                                   lat_max)

        return salinities

    def read_salinity(self, nc, lon, lat, season, lon_max, lat_max):
        salinity = None

        if np.isnan(lon) or np.isnan(lat) or np.isnan(season):
            salinity = "NaN"
        else:
            search_step = -1
            while salinity is None:
                search_step = search_step + 1
                search_cells = get_surrounding_cells(int(lon), int(lat), search_step, lon_max, lat_max)

                for (search_lon, search_lat) in search_cells:
                    salinity_value = nc.variables[self.SALINITY_VAR][int(season), search_lat, search_lon]
                    if not salinity_value.mask:
                        salinity = salinity_value
                        break

        return salinity
