from DataRetriever import DataRetriever
import EmailRetriever, FileRetriever

# Factory for DataRetriever instances

# Get the list of retriever types
def get_retriever_types():
  result = []

  for clazz in DataRetriever.__subclasses__():
    result.append(clazz.get_type())

  return result

# Ask the user to select a retriever type
def ask_retriever_type():
  entries = []

  for clazz in DataRetriever.__subclasses__():
    entries.append(clazz.get_type())


  selected = -1
  while selected == -1:
    print("Select source for NRT files:")

    for i in range(0, len(entries)):
      print("  %d. %s" % (i + 1, entries[i]))

    try:
      selection = input("Selection: ")
      selection = int(selection)
      if selection > 0 and selection <= len(entries):
        selected = selection
    except ValueError:
      pass

  return entries[selected - 1]

# Get an empty Retriever instance of the specified type
def get_new_instance(retriever_type):
  result = None

  for clazz in DataRetriever.__subclasses__():
    if clazz.get_type() == retriever_type:
      result = clazz()
      break

  if result is None:
    raise ValueError("Cannot find retriever of type %s" % (retriever_type, ))

  return result
