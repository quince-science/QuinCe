library(ncdf4)

wd <- "D:/MyFiles/Projects/ICOS/Labelling/Labelling Step 2/Data_checks/external_data"
setwd(wd)

# File locations
INPUT_DIR <- "input"
OUTPUT_DIR <- "output"
ECMWF_DIR <- "ecmwf"
EN4_DIR <- "en4"


# External data details
EXTERNAL_COL_HEADERS <- c("ECMWF SST", "ECMWF SLP", "EN4 SSS", "ATM CO2")

ECMWF_PARAMS <- c("SST", "SLP")
ECMWF_VARS <- c("sst", "msl")
ECMWF_OPERATOR <- c("minus", "divide")
ECMWF_OPERAND <- c(273.15, 100)


# Get the files in the input directory
input_files <- list.files(INPUT_DIR)

for (file_loop in 1:length(input_files)) {
	# Load the data
	in_file <- paste(INPUT_DIR, "/", input_files[file_loop], sep="")
	data <- read.csv(in_file,header=T, fileEncoding="UTF8")
	original_column_count <- ncol(data)

	# Add the columns for the external data
	for (i in 1:length(EXTERNAL_COL_HEADERS)) {
		data[,EXTERNAL_COL_HEADERS[i]] <- NA
	}

	# ECMWF
	for (ecmwf_loop in 1:length(ECMWF_PARAMS)) {
		cat("\r", in_file, ECMWF_PARAMS[ecmwf_loop], "                       ")
		current_year <- 0
		external_lons <- NULL
		external_lats <- NULL
		external_times <- NULL
		external_data <- NULL


		# Process each row of the data
		for (row in 1:nrow(data)) {

			if (row %% 100 == 0) {
				cat("\r", in_file, ECMWF_PARAMS[ecmwf_loop], "Row", row, "of", nrow(data), "    ")
			}

			# Make sure we have the right external data loaded
			date <- as.POSIXlt(data[["Date"]][row], "%Y-%m-%d %H:%M:%S", tz="UTC")
			date$year <- date$year + 1900
			
      longitude <- as.double(data[["Longitude"]][row])
			if (!is.na(longitude)) {
        
			if (longitude < 0) {
				longitude <- 180 + (180 - abs(longitude))
			}
			latitude <- as.numeric(data[["Latitude"]][row])


			if (date$year != current_year) {
				current_year <- date$year
				external_file <- paste(ECMWF_DIR, "/", ECMWF_PARAMS[ecmwf_loop], "_", current_year, ".nc", sep="")
				nc <- nc_open(external_file)
				external_lons <- as.double(ncvar_get(nc, "longitude"))
				external_lats <- ncvar_get(nc, "latitude")
				external_times <- ncvar_get(nc, "time")
				external_times <- as.POSIXlt("1900-01-01 00:00:00", tz="UTC") + (external_times * 3600)
				external_data <- ncvar_get(nc, ECMWF_VARS[ecmwf_loop])
				nc_close(nc)
			}

			lon_index <- which(external_lons >= longitude)[1] - 1
			lat_index <- tail(which(external_lats >= latitude), 1)
			time_index <- tail(which(external_times <= date), 1)

			external_value <- external_data[lon_index, lat_index, time_index]
			if (length(external_value) > 0) {

				if (!is.na(external_value)) {
					operator <- ECMWF_OPERATOR[ecmwf_loop]
					if (operator == "minus") {
						external_value <- external_value - ECMWF_OPERAND[ecmwf_loop]
					} else if (operator == "divide") {
						external_value <- external_value / ECMWF_OPERAND[ecmwf_loop]
					}
				}

				data[[original_column_count + ecmwf_loop]][row] <- external_value
			}
		 }
		}
	}

  
	# EN4 SS
	current_year <- 0
	for (row in 1:nrow(data)) {

		if (row %% 100 == 0) {
			cat("\r", in_file, "SSS Row", row, "of", nrow(data), "    ")
		}

		# Make sure we have the right external data loaded
		date <- as.POSIXlt(data[["Date"]][row], "%Y-%m-%d %H:%M:%S", tz="UTC")
		date$year <- date$year + 1900

    
		longitude <- as.double(data[["Longitude"]][row])
		if (!is.na(longitude)) {
		
		if (longitude < 0) {
			longitude <- 180 + (180 - abs(longitude))
		}
		latitude <- as.numeric(data[["Latitude"]][row])

		if (date$year != current_year) {
			current_year <- date$year
			external_file <- paste(EN4_DIR, "/", "SSS_", current_year, ".nc", sep="")
			nc <- nc_open(external_file)
			external_lons <- as.double(ncvar_get(nc, "lon"))
			external_lats <- ncvar_get(nc, "lat")
			external_data <- ncvar_get(nc, "salinity")
			nc_close(nc)
		}

		lon_index <- which(external_lons >= longitude)[1] - 1
		lat_index <- which(external_lats >= latitude)[1]

		external_value <- external_data[lon_index, lat_index, date$mon]
		if (length(external_value) > 0 && !is.na(external_value)) {
			data[[original_column_count + 3]][row] <- external_value
		}
	}
	}
	# Write out the data
	out_file <- paste(OUTPUT_DIR, "/", input_files[file_loop], sep="")
	write.csv(data, file=out_file, row.names=FALSE, fileEncoding="UTF8")
}

cat("\n")