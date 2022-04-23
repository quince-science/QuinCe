from modules.Retriever.ImapRetriever import ImapRetriever
from modules.Retriever.FTPRetriever import FTPRetriever
from modules.Retriever.SFTPRetriever import SFTPRetriever


# Factory for DataRetriever instances

def _get_retriever_classes():
    return [
        FTPRetriever,
        ImapRetriever,
        SFTPRetriever
    ]


# Get the list of retriever types
def get_retriever_types():
    result = []

    for clazz in _get_retriever_classes():
        result.append(clazz.get_type())

    return result


# Ask the user to select a retriever type
def ask_retriever_type():
    entries = get_retriever_types()

    selected = -1
    while selected == -1:
        print("Select source for NRT files:")
        print("  0. None")

        for i in range(0, len(entries)):
            print("  %d. %s" % (i + 1, entries[i]))

        try:
            selection = input("Selection: ")
            selection = int(selection)
            if -1 < selection <= len(entries):
                selected = selection
        except ValueError:
            pass

    result = None
    if selected > 0:
        result = entries[selected - 1]

    return result


# Get an empty Retriever instance of the specified type
def get_new_instance(retriever_type):
    result = None

    for clazz in _get_retriever_classes():
        if clazz.get_type() == retriever_type:
            result = clazz(None, None)
            break

    if result is None:
        raise ValueError("Cannot find retriever of type %s" % (retriever_type,))

    return result


def get_instance(retriever_type, instrument_id, logger, configuration):
    result = None

    for clazz in _get_retriever_classes():
        if clazz.get_type() == retriever_type:
            result = clazz(instrument_id, logger, configuration)
            break

    if result is None:
        raise ValueError("Cannot find retriever of type %s" % (retriever_type,))

    return result
