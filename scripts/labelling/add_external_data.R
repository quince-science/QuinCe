library(ncdf4)

# File locations
INPUT_DIR <- "input"
OUTPUT_DIR <- "output"
DATA_DIR <- "ecmwf"

# External data details
PARAMETERS <- c("SST", "SLP")
DATA_VARS <- c("sst", "msl")
DATA_OPERATOR <- c("minus", "divide")
DATA_OPERAND <- c(273.15, 100)
EXTERNAL_COL_HEADERS <- c("ECMWF SST", "ECMWF SLP")

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

	# Loop through each parameter
	for (param_loop in 1:length(PARAMETERS)) {
		cat("\r", in_file, PARAMETERS[param_loop], "                       ")
		current_year <- 0
		external_lons <- NULL
		external_lats <- NULL
		external_times <- NULL
		external_data <- NULL


		# Process each row of the data
		for (row in 1:nrow(data)) {

			if (row %% 100 == 0) {
				cat("\r", in_file, PARAMETERS[param_loop], "Row", row, "of", nrow(data), "    ")
			}

			# Make sure we have the right external data loaded
			date <- as.POSIXlt(data[["Date"]][row], "%Y-%m-%d %H:%M:%S", tz="UTC")
			date$year <- date$year + 1900

			longitude <- as.double(data[["Longitude"]][row])
			if (longitude < 0) {
				longitude <- 180 + (180 - abs(longitude))
			}
			latitude <- as.numeric(data[["Latitude"]][row])


			if (date$year != current_year) {
				current_year <- date$year
				external_file <- paste(DATA_DIR, "/", PARAMETERS[param_loop], "_", current_year, ".nc", sep="")
				nc <- nc_open(external_file)
				external_lons <- as.double(ncvar_get(nc, "longitude"))
				external_lats <- ncvar_get(nc, "latitude")
				external_times <- ncvar_get(nc, "time")
				external_times <- as.POSIXlt("1900-01-01 00:00:00", tz="UTC") + (external_times * 3600)
				external_data <- ncvar_get(nc, DATA_VARS[param_loop])
				nc_close(nc)
			}

			lon_index <- which(external_lons >= longitude)[1] - 1
			lat_index <- tail(which(external_lats >= latitude), 1)
			time_index <- tail(which(external_times <= date), 1)

			external_value <- external_data[lon_index, lat_index, time_index]
			if (length(external_value) > 0) {

				if (!is.na(external_value)) {
					operator <- DATA_OPERATOR[param_loop]
					if (operator == "minus") {
						external_value <- external_value - DATA_OPERAND[param_loop]
					} else if (operator == "divide") {
						external_value <- external_value / DATA_OPERAND[param_loop]
					}
				}

				data[[original_column_count + param_loop]][row] <- external_value
			}
		}
	}

	# Write out the data
	out_file <- paste(OUTPUT_DIR, "/", input_files[file_loop], sep="")
	write.csv(data, file=out_file, row.names=FALSE, fileEncoding="UTF8")
}

cat("\n")