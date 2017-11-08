#####################################################################################
### Function for creating STD plots
###########################

### Input folder must contain raw data file(s) (needs to have the same format and standards).
### Output folder will contain multiple plots (per file) showing how the standard
### measurements of each dataset.

### Function has 7 inputparamters:
### - date_col
### - time_col
### - dt_format
### - CO2_col
### - run_type_col
### - std_names
### - std_val


##-----------------------------------------------------------------------------

## rm(list = ls())


date_col <- 1
time_col <- 1
dt_format <- "%d/%m/%Y %H:%M:%S"            # e.g. "%d/%m/%y %H:%M:%S"
CO2_col <- 12
run_type_col <- 3
std_names <- c("STD1","STD2","STD3","STD4")
std_val <- c(0,248.82,358.98,501.25)


#------------------------------------------------------------------------------




  input_dir<- "input"
  output_dir<-"output"
  
  # List all files in input directory
  input_files <- list.files(input_dir)
  
  
  # Loop through the input files
  for (file_loop in 1:length(input_files)) {
     
    # Get the path to file and read the data 
    in_file <- paste(input_dir, "/", input_files[file_loop], sep="")
    data <- read.table(in_file,header=T, sep = "\t", strip.white=TRUE, fileEncoding="UTF8")
    
    # Identify date and time
    # (The if statement is related to whether there are one or two date/time columns in raw file)
    if (date_col == time_col) {
        date.time <- as.POSIXct(data[,date_col], tz="UTC", format=dt_format)
    } else {
        date.time <- as.POSIXct(paste(data[,date_col], data[,time_col]), tz="UTC", format=dt_format)          
    }
    
    # Add the date time column to the data frame
    data$date.time <- date.time
    
    ## Make the plots as output
    out_file <- paste(output_dir, "/", input_files[file_loop], sep="")
    
    
    # Loop through the different standards
    for (i in 1:length(std_val)) {                                                                                              
      
      # Make subset of dataset with only one standard
      type <- std_names[i]                                                                                  
      data_sub <- data[data[,run_type_col]==type,]                                                                 
      
      # Make output path and filename 
      filename <- paste((paste((sub("^([^.]*).*", "\\1", out_file)), type, sep="_")), "jpg", sep=".")
      
      # Make the plot:
      jpeg(filename)
        
         # Make a max and min limit to use for plot (so that all values are seen on plot)
         min_range <- min(data_sub[,CO2_col], std_val[i])                                                     
         max_range <- max(data_sub[,CO2_col], std_val[i])                                                     
        
         # Make plot name
         plot_name <-paste((sub("^([^.]*).*", "\\1", input_files[file_loop])), type, sep="_")
         
         # Ploting the CO2 measurements (from standard) agaings time
         plot(data_sub$date.time, data_sub[,CO2_col], xlab="Date", ylab="CO2", main=plot_name, ylim = c(min_range, max_range))   
        
         # Adding a line on the standard value
         abline(std_val[i],0,col="red")                                                                                
      
      dev.off()
    }
  
  
  }
