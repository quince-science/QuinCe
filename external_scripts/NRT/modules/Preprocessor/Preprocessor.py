import os
import sys
import logging
from abc import abstractmethod

from ConfigurableItem import ConfigurableItem


class Preprocessor(ConfigurableItem):

    def __init__(self, logger, configuration=None):
        super().__init__(configuration)

        if logger is not None:
            self.logger = logger
        else:
            logging.basicConfig(stream=sys.stdout,
                                format="%(message)s",
                                level=logging.ERROR)
            self.logger = logging.getLogger('preprocessor')

    @staticmethod
    @abstractmethod
    def get_type():
        raise NotImplementedError("get_type not implemented")

    @abstractmethod
    def preprocess(self, data):
        raise NotImplementedError("preprocess not implemented")

    @staticmethod
    def get_processed_filename(filename):
        split = os.path.splitext(filename)
        return f'{split[0]}.preprocessed{split[1]}'
