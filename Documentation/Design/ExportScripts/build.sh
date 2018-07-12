#!/bin/bash

# mermain.cli module (npm install mermaid.cli) is required to build mermaid files.
# Images are checked in anyway so the document can be viewed with minimum effort

#mmdc -w 1500 -i dataset_flow.mermaid -o dataset_flow.png
#mmdc -w 1500 -i simple_script_flow.mermaid -o simple_script_flow.png

# Requires pandoc-crossref (https://github.com/lierdakil/pandoc-crossref)

pandoc -f markdown+smart+multiline_tables+footnotes -F pandoc-crossref --toc -o ~/temp/ExportScripts.pdf ExportScripts.md
