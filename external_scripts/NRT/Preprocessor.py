import os
from abc import abstractmethod

from ConfigurableItem import ConfigurableItem


class Preprocessor(ConfigurableItem):

    def __init__(self, logger, configuration=None):
        super().__init__(configuration)
        self.logger = logger

    @staticmethod
    @abstractmethod
    def get_type():
        raise NotImplementedError("get_name not implemented")

    @abstractmethod
    def preprocess(self, data):
        raise NotImplementedError("preprocess not implemented")

    @staticmethod
    def get_processed_filename(filename):
        split = os.path.splitext(filename)
        return f'{split[0]}.preprocessed{split[1]}'
