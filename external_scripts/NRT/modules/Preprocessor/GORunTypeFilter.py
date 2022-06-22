from modules.Preprocessor import Preprocessor
import pandas as pd

VALID_RUN_TYPES = 'EQU.*|ATM.*|STD.*|GO TO SLEEP|WAKE UP|FILTER|SHUT DOWN'


class GORunTypeFilter(Preprocessor.Preprocessor):
    def __init__(self, logger, configuration):
        super().__init__(logger, configuration)

    @staticmethod
    def get_type():
        return "GO Run Type Filter"

    def test_configuration(self):
        return True

    def preprocess(self, data):
        df = pd.read_csv(data, sep="\t", dtype='str')
        valid_lines = df['Type'].str.contains(VALID_RUN_TYPES)
        return df[valid_lines].to_csv(sep="\t", index=False, na_rep='NaN').encode("utf-8")

    @staticmethod
    def get_processed_filename(filename):
        return filename

    def has_config(self):
        return False
