#!/bin/bash

# mermaid.cli module (npm install mermaid.cli) is required to build mermaid files.
# Images are checked in anyway so the document can be viewed with minimum effort

#mmdc -i L1_L2_progression.mermaid -o L1_L2_progression.png
#mmdc -i L1_L2_CPversions.mermaid -o L1_L2_CPversions.png
#mmdc -i J1-4_NRT.mermaid -o J1-4_NRT.png

# Requires pandoc 2.5+ (https://pandoc.org/installing.html)

pandoc -f markdown+smart+multiline_tables+footnotes -o ~/temp/CarbonPortalExport.pdf CarbonPortalExport.md
