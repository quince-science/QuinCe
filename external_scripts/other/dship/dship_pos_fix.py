import argparse
import os
import pandas as pd

parser = argparse.ArgumentParser(
    prog='dship_pos_fix.py',
    description='Convert positions in Polarstern DSHIP files to a format that QuinCe can use')

parser.add_argument('in_dir', help='Directory containing source files')
parser.add_argument('out_dir', help='Directory for output files')

args = parser.parse_args()

if not os.path.isdir(args.in_dir):
    exit('in_dir is not a directory')

if not os.path.isdir(args.out_dir):
    exit('out_dir is not a directory')

for file in os.listdir(args.in_dir):
    
    print(f'Processing {file}')

    file_path = os.path.join(args.in_dir, file)

    # Open the file and grab the first three lines as the file header
    header_lines = []
    with open(file_path, 'r', encoding='cp1252') as f_in:
        for i in range(3):
            header_lines.append(f_in.readline())

        # Read the rest of the file into a Pandas DataFrame
        df = pd.read_csv(f_in, sep='\t', header=None)

    # Edit the position columns
    df[1] = df[1].replace(regex=r'([0-9]+)° ([0-9.]+)\' ([NS])', value='\\3 \\1 \\2')
    df[2] = df[2].replace(regex=r'([0-9]+)° ([0-9.]+)\' ([EW])', value='\\3 \\1 \\2')

    # Write to output file
    with open(os.path.join(args.out_dir, file), 'w') as out:
        for header in header_lines:
            out.write(header)

        df.to_csv(out, sep='\t', header=None, index=False)
