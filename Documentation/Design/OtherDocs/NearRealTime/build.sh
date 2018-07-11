#!/bin/bash

# NOTE: This needs the mermaid-filter for Pandoc (https://github.com/raghur/mermaid-filter)
pandoc -f markdown+smart+multiline_tables+footnotes -F mermaid-filter --toc -o ~/temp/NRT.pdf NearRealTime.md
