Scripts to add external data to files exported from QuinCe.

`add_external_data.R` needs SST and SLP from ECMWF, named in the form:
`<VAR>_<YEAR>.nc`

The configuration for the ECMWF downloads is:
Time steps: 0,12,16,18
Step: 0
Resolution: 1°x1°

`summary_and_plots.R` doesn't need anything special

To run both scripts, in addition to the data directories
listed above, you need an input directory containing the
files exported from QuinCe, and an output directory where
the generated files will end up.


To run both scripts, in addition to the data directories
listed above, you need an input directory containing the
files exported from QuinCe, and an output directory where
the generated files will end up.

