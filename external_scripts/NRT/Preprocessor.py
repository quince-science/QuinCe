import os
from abc import ABCMeta, abstractmethod


class Preprocessor(metaclass=ABCMeta):

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
