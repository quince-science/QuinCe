from abc import ABCMeta, abstractmethod

class Preprocessor(metaclass=ABCMeta):

  @staticmethod
  @abstractmethod
  def get_name():
    raise NotImplementedError("get_name not implemented")

  @abstractmethod
  def preprocess(self, data):
    raise NotImplementedError("preprocess not implemented")
