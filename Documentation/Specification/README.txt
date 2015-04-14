To generate an HTML version of the specification document:

pandoc -s -S -5 -c specification.css -o Specification.html Specification.txt

For a LaTeX-based PDF:

pandoc -s Specification.txt -o Specification.pdf
