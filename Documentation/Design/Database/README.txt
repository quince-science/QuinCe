To generate an HTML version of the database_tables document:

pandoc -s -f markdown+smart -t html5 --mathml --toc -c design.css -o Database_Tables.html Database_Tables.txt

For a LaTeX-based PDF:

pandoc -s Database_Tables.txt -o Database_Tables.pdf

For a Word document:

pandoc -s -S Database_Tables.txt -o Database_Tables.docx
