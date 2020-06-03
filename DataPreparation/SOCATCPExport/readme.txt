Maren Kjos Karlsen 20200603

General info for data preparation in relation with 2019 Data upload
Relevant platforms:
- 26NA Nuka Arctica
- 48MB PALOMA (L2 not currently exported due to FOS format restrictions)
- 58GS G.O.Sars
- (65DK) France-Brazil

L2 and source raw files exported.
L2 files produced through SOCAT

Data preparation steps:
- Reformat L2 files to assimilate QuinCe Export format. 
  This was done help consistency and take advantage of the export format 
  already in use by ICOS OTC at the Carbon Portal. See SOCAT_CP_conversion.py 
  for further detail.

- SOCAT citation and DOIs for original L2 files provided by csv-file 
  <SOCAT OTC citations.csv>. Ingested and written to DB by ingest_citations.py
  for easy handling.

- Linked/relevant source files listed in csv file within each platform-subfolder
  Information ingested and stored in DB by L0_links.py 
  Nuka_L0 identifies links and stores information in DB for Nuka Arctica.

- metadata.py iterates through directories and processes any L2 files, as 
  well as the linked source files. Metadata information relating to start- and
  enddate, number of rows and hashsums are retrieved/generated and written to DB

- export.py iterates through the DB-export table and exports any unuploaded 
  files. Connection to the Carbon Portal is created, a metadata string is 
  compiled and the metadata and datafile is exported to the Carbon Portal.



