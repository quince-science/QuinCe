# Run a preprocessor on a file
import sys
from io import BytesIO
from modules.Preprocessor import PreprocessorFactory


def main():
    if len(sys.argv) != 2:
        print("Usage: preprocess_file.py <file>")
        exit()

    preprocessor = PreprocessorFactory.get_new_instance(PreprocessorFactory.ask_preprocessor_type())

    preprocessor.enter_configuration()

    in_file = open(sys.argv[1], "rb")
    in_bytes = in_file.read()
    in_file.close()
    out_bytes = preprocessor.preprocess(BytesIO(in_bytes))

    out_file = open(sys.argv[1] + ".processed", "wb")
    out_file.write(out_bytes)
    out_file.close()


if __name__ == '__main__':
    main()
