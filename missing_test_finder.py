"""
Locates Java files that do not have a corresponding test file.

This is fairly basic - for each x.java file in the WebApp/src
tree, looks for a corresponding xTest.java file in the
WebApp/junit tree.

There will, of course, be other tests for complex classes,
but they should all have a basic set of root tests in a class
named as above.

This exists because coverage tests can't tell us when a class
has specific tests as opposed to being touched by other tests
by coincidence.
"""
import os
from glob import glob

def strip_path(filename, root):
    result = filename.removeprefix(root)
    return result.removeprefix('/')

def get_files(path):
    files = [y for x in os.walk(path) for y in glob(os.path.join(x[0], '*.java'))]
    return [(lambda x: x.removeprefix(path + '/').removesuffix('.java'))(x) for x in files]

SOURCE_ROOT = 'WebApp/src'
TEST_ROOT = 'WebApp/junit'

# Get the lists of files
start_path = os.getcwd()
java_path = os.path.join(os.getcwd(), SOURCE_ROOT)
test_path = os.path.join(os.getcwd(), TEST_ROOT)

# Get the java source files
java_files = get_files(java_path)

# Get the test java files, and remove 'Test'.
# These should then match the source files
test_files = [x.removesuffix('Test') for x in get_files(test_path)]

for source in java_files:
    if source not in test_files:
        print(source)

