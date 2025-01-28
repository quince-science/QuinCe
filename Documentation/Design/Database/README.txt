To generate an HTML version of the database_tables document:

pandoc -s -f markdown+smart -t html5 --mathml --toc -c ../design.css -o Database_Tables.html Database_Tables.md
