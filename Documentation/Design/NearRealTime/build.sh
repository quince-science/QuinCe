#!/bin/bash

# Mermaid diagrams need to be built first
#mmdc -w 1000 -i data_flow.mermaid -o data_flow.png

pandoc -f markdown+smart+multiline_tables+footnotes --toc -o ~/temp/NRT.pdf NearRealTime.md
