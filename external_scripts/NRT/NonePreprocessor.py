from Preprocessor import Preprocessor


class NonePreprocessor(Preprocessor):
    def get_name(self):
        return "None"

    def preprocess(self, data):
        return data.getvalue()
