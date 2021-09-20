from Preprocessor import Preprocessor


class NonePreprocessor(Preprocessor):

    @staticmethod
    def get_type():
        return "None"

    def preprocess(self, data):
        return data.getvalue()

    @staticmethod
    def get_processed_filename(filename):
        return filename
