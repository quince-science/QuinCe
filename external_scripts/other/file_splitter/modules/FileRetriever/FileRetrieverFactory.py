"""
Factory for getting FileRetriever instances
"""
from modules.FileRetriever.GoogleDriveRetriever import GoogleDriveRetriever


def get_retriever(name, config):
    """
    Get the file retriever defined in the specified configuration
    :param name: The station name
    :param config: The retriever configuration
    :return: The retriever
    """
    retriever_type = config['type']
    if retriever_type == 'GoogleDrive':
        return GoogleDriveRetriever(name, config)
    else:
        raise ValueError(f'Unrecognised FileRetriever {retriever_type}')
