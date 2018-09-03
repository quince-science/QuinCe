from abc import ABC, abstractmethod

class DataRetriever(ABC):

	# Empty constructor
	def __init__(self):
		pass

	# Get the configuration type
	@staticmethod
	@abstractmethod
	def get_type():
		pass
