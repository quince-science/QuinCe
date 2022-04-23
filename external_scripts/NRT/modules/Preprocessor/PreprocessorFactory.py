from modules.Preprocessor.AddSalinityPreprocessor import AddSalinityPreprocessor
from modules.Preprocessor.NonePreprocessor import NonePreprocessor
from modules.Preprocessor.SQLiteExtractor import SQLiteExtractor


def _get_retriever_classes():
    return [
        NonePreprocessor,
        AddSalinityPreprocessor,
        SQLiteExtractor
    ]


# Get the list of preprocessor types
def get_preprocessor_names():
    result = []

    for clazz in _get_retriever_classes():
        result.append(clazz.get_type())

    return result


# Ask the user to select a preprocessor
def ask_preprocessor_type():
    entries = get_preprocessor_names()

    selected = -1
    while selected == -1:
        print("Select preprocessor:")

        for i in range(0, len(entries)):
            print("  %d. %s" % (i, entries[i]))

        try:
            selection = input("Selection: ")
            selection = int(selection)
            if -1 < selection <= len(entries):
                selected = selection
        except ValueError:
            pass

    return entries[selected]


def get_instance(preprocessor_type, logger, configuration):
    result = None

    for clazz in _get_retriever_classes():
        if clazz.get_type() == preprocessor_type:
            result = clazz(logger, configuration)
            break

    if result is None:
        raise ValueError("Cannot find retriever of type %s" % (preprocessor_type,))

    return result


def get_new_instance(preprocessor_type):
    result = None

    for clazz in _get_retriever_classes():
        if clazz.get_type() == preprocessor_type:
            result = clazz(None, None)
            break

    if result is None:
        raise ValueError("Cannot find retriever of type %s" % (preprocessor_type,))

    return result
