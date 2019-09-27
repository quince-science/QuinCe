from Preprocessor import Preprocessor
import NonePreprocessor, AddSalinityPreprocessor

# Factory for Preprocessor instances

# Get the list of preprocessor types
def get_preprocessor_names():
  result = []

  for clazz in Preprocessor.__subclasses__():
    result.append(clazz.get_name())

  return result

# Ask the user to select a preprocessor
def ask_preprocessor():
  entries = get_preprocessor_names()

  selected = -1
  while selected == -1:
    print("Select preprocessor:")

    for i in range(0, len(entries)):
      print("  %d. %s" % (i, entries[i]))

    try:
      selection = input("Selection: ")
      selection = int(selection)
      if selection > -1 and selection <= len(entries):
        selected = selection
    except ValueError:
      pass

  result = entries[selected]

  return result

def get_new_instance(preprocessor_type):
  result = None

  for clazz in Preprocessor.__subclasses__():
    if clazz.get_name() == preprocessor_type:
      result = clazz()
      break

  if result is None:
    raise ValueError("Cannot find retriever of type %s" % (preprocessor_type, ))

  return result
