To generate an HTML version of the QC_Sharing document:

pandoc -s -S -t html5 --mathml --toc -c design.css -o QC_Sharing.html QC_Sharing.txt

For a LaTeX-based PDF:

pandoc -s QC_Sharing.txt -o QC_Sharing.pdf

For a Word document:

pandoc -s -S QC_Sharing.txt -o QC_Sharing.docx
