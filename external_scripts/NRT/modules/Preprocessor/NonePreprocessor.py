from modules.Preprocessor import Preprocessor


class NonePreprocessor(Preprocessor.Preprocessor):
    def __init__(self, logger, configuration):
        super().__init__(logger, configuration)

    @staticmethod
    def get_type():
        return "None"

    def test_configuration(self):
        return True

    def preprocess(self, data):
        return data.getvalue()

    @staticmethod
    def get_processed_filename(filename):
        return filename

    def has_config(self):
        return True
