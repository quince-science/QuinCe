-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `calibration`
--


CREATE TABLE `calibration` (
  `instrument_id` int(11) NOT NULL,
  `type` varchar(20) NOT NULL,
  `target` varchar(45) NOT NULL,
  `deployment_date` bigint(20) NOT NULL,
  `coefficients` text NOT NULL,
  `class` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `calibration_data`
--


CREATE TABLE `calibration_data` (
  `id` int(11) NOT NULL,
  `dataset_id` int(11) NOT NULL,
  `date` bigint(20) NOT NULL,
  `run_type` varchar(45) NOT NULL,
  `use_record` tinyint(1) NOT NULL DEFAULT '1',
  `use_message` text,
  `intake_temperature` double DEFAULT NULL,
  `salinity` double DEFAULT NULL,
  `equilibrator_temperature` double DEFAULT NULL,
  `equilibrator_pressure_absolute` double DEFAULT NULL,
  `equilibrator_pressure_differential` double DEFAULT NULL,
  `atmospheric_pressure` double DEFAULT NULL,
  `xh2o` double DEFAULT NULL,
  `co2` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `dataset`
--

CREATE TABLE `dataset` (
  `id` int(11) NOT NULL,
  `instrument_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `start` bigint(20) NOT NULL,
  `end` bigint(20) NOT NULL,
  `status` tinyint(1) NOT NULL,
  `properties` text,
  `last_touched` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `dataset_data`
--

CREATE TABLE `dataset_data` (
  `id` int(11) NOT NULL,
  `dataset_id` int(11) NOT NULL,
  `date` bigint(20) NOT NULL,
  `longitude` double DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `run_type` varchar(45) NOT NULL,
  `diagnostic_values` text,
  `intake_temperature` double DEFAULT NULL,
  `salinity` double DEFAULT NULL,
  `equilibrator_temperature` double DEFAULT NULL,
  `equilibrator_pressure_absolute` double DEFAULT NULL,
  `equilibrator_pressure_differential` double DEFAULT NULL,
  `atmospheric_pressure` double DEFAULT NULL,
  `xh2o` double DEFAULT NULL,
  `co2` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `data_file`
--

CREATE TABLE `data_file` (
  `id` int(11) NOT NULL,
  `file_definition_id` int(11) NOT NULL,
  `filename` varchar(200) NOT NULL,
  `start_date` bigint(20) NOT NULL,
  `end_date` bigint(20) NOT NULL,
  `record_count` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `equilibrator_pco2`
--

CREATE TABLE `equilibrator_pco2` (
  `measurement_id` int(11) NOT NULL,
  `delta_temperature` double DEFAULT NULL,
  `true_moisture` double DEFAULT NULL,
  `ph2o` double DEFAULT NULL,
  `dried_co2` double DEFAULT NULL,
  `calibrated_co2` double DEFAULT NULL,
  `pco2_te_dry` double DEFAULT NULL,
  `pco2_te_wet` double DEFAULT NULL,
  `fco2_te` double DEFAULT NULL,
  `fco2` double DEFAULT NULL,
  `auto_flag` smallint(2) DEFAULT '-1000',
  `auto_message` text,
  `user_flag` smallint(2) DEFAULT '-1000',
  `user_message` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `file_column`
--

CREATE TABLE `file_column` (
  `file_definition_id` int(11) NOT NULL,
  `file_column` smallint(3) NOT NULL,
  `primary_sensor` tinyint(1) NOT NULL,
  `sensor_type` varchar(100) NOT NULL,
  `sensor_name` varchar(100) NOT NULL,
  `value_column` smallint(3) NOT NULL,
  `depends_question_answer` tinyint(1) NOT NULL DEFAULT '0',
  `missing_value` varchar(50) DEFAULT NULL,
  `post_calibrated` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `file_definition`
--

CREATE TABLE `file_definition` (
  `id` int(11) NOT NULL,
  `instrument_id` int(11) NOT NULL,
  `description` varchar(100) NOT NULL,
  `column_separator` varchar(1) NOT NULL,
  `header_type` tinyint(1) NOT NULL,
  `header_lines` smallint(3) DEFAULT NULL,
  `header_end_string` varchar(100) DEFAULT NULL,
  `column_header_rows` tinyint(2) NOT NULL,
  `column_count` smallint(3) NOT NULL,
  `lon_format` tinyint(1) NOT NULL DEFAULT '-1',
  `lon_value_col` smallint(3) NOT NULL DEFAULT '-1',
  `lon_hemisphere_col` smallint(3) NOT NULL DEFAULT '-1',
  `lat_format` tinyint(1) NOT NULL DEFAULT '-1',
  `lat_value_col` smallint(3) NOT NULL DEFAULT '-1',
  `lat_hemisphere_col` smallint(3) NOT NULL DEFAULT '-1',
  `date_time_col` smallint(3) NOT NULL DEFAULT '-1',
  `date_time_props` text,
  `date_col` smallint(3) NOT NULL DEFAULT '-1',
  `date_props` text,
  `hours_from_start_col` smallint(3) NOT NULL DEFAULT '-1',
  `hours_from_start_props` text,
  `jday_time_col` smallint(3) NOT NULL DEFAULT '-1',
  `jday_col` smallint(3) NOT NULL DEFAULT '-1',
  `year_col` smallint(3) NOT NULL DEFAULT '-1',
  `month_col` smallint(3) NOT NULL DEFAULT '-1',
  `day_col` smallint(3) NOT NULL DEFAULT '-1',
  `time_col` smallint(3) NOT NULL DEFAULT '-1',
  `time_props` text,
  `hour_col` smallint(3) NOT NULL DEFAULT '-1',
  `minute_col` smallint(3) NOT NULL DEFAULT '-1',
  `second_col` smallint(3) NOT NULL DEFAULT '-1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `instrument`
--

CREATE TABLE `instrument` (
  `id` int(11) NOT NULL,
  `owner` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `pre_flushing_time` int(11) DEFAULT '0',
  `post_flushing_time` int(11) DEFAULT '0',
  `minimum_water_flow` int(11) DEFAULT '-1',
  `averaging_mode` tinyint(1) NOT NULL DEFAULT '0',
  `platform_code` varchar(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `job`
--

CREATE TABLE `job` (
  `id` int(11) NOT NULL,
  `owner` int(11) DEFAULT NULL,
  `submitted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `class` text NOT NULL,
  `parameters` longtext,
  `status` enum('WAITING','RUNNING','FINISHED','ERROR','KILLED')
  NOT NULL DEFAULT 'WAITING',
  `started` timestamp NULL DEFAULT NULL,
  `ended` timestamp NULL DEFAULT NULL,
  `thread_name` varchar(50) DEFAULT NULL,
  `progress` float DEFAULT NULL,
  `stack_trace` longtext
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `run_type`
--

CREATE TABLE `run_type` (
  `file_definition_id` int(11) NOT NULL,
  `run_name` varchar(50) NOT NULL,
  `category_code` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Tabellstruktur for tabell `user`
--

CREATE TABLE `user` (
  `id` int(11) NOT NULL,
  `email` varchar(45) NOT NULL,
  `salt` varbinary(20) NOT NULL,
  `password` varbinary(45) NOT NULL,
  `firstname` varchar(30) DEFAULT NULL,
  `surname` varchar(45) DEFAULT NULL,
  `email_code` varchar(50) DEFAULT NULL,
  `email_code_time` timestamp NULL DEFAULT NULL,
  `password_code` varchar(50) DEFAULT NULL,
  `password_code_time` timestamp NULL DEFAULT NULL,
  `permissions` int(11) NOT NULL DEFAULT '0',
  `preferences` mediumtext
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `calibration`
--
ALTER TABLE `calibration`
  ADD PRIMARY KEY (`instrument_id`,`type`,`target`,`deployment_date`);

--
-- Indexes for table `calibration_data`
--
ALTER TABLE `calibration_data`
  ADD PRIMARY KEY (`id`),
  ADD KEY `DATASETDATA_DATASET_idx` (`dataset_id`);

--
-- Indexes for table `dataset`
--
ALTER TABLE `dataset`
  ADD PRIMARY KEY (`id`),
  ADD KEY `DATASET_INSTRUMENT_idx` (`instrument_id`);

--
-- Indexes for table `dataset_data`
--
ALTER TABLE `dataset_data`
  ADD PRIMARY KEY (`id`),
  ADD KEY `DATASETDATA_DATASET_idx` (`dataset_id`);

--
-- Indexes for table `data_file`
--
ALTER TABLE `data_file`
  ADD PRIMARY KEY (`id`),
  ADD KEY `DATAFILE_FILEDEFINITION_idx` (`file_definition_id`);

--
-- Indexes for table `equilibrator_pco2`
--
ALTER TABLE `equilibrator_pco2`
  ADD PRIMARY KEY (`measurement_id`);

--
-- Indexes for table `file_column`
--
ALTER TABLE `file_column`
  ADD PRIMARY KEY (`file_definition_id`,`file_column`);

--
-- Indexes for table `file_definition`
--
ALTER TABLE `file_definition`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FILEDEFINITION_INSTRUMENT_idx` (`instrument_id`);

--
-- Indexes for table `instrument`
--
ALTER TABLE `instrument`
  ADD PRIMARY KEY (`id`),
  ADD KEY `OWNER_idx` (`owner`);

--
-- Indexes for table `job`
--
ALTER TABLE `job`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_job_1_idx` (`owner`);

--
-- Indexes for table `run_type`
--
ALTER TABLE `run_type`
  ADD PRIMARY KEY (`file_definition_id`,`run_name`);

--
-- Indexes for table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email_UNIQUE` (`email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `calibration_data`
--
ALTER TABLE `calibration_data`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `dataset`
--
ALTER TABLE `dataset`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `dataset_data`
--
ALTER TABLE `dataset_data`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `data_file`
--
ALTER TABLE `data_file`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `file_definition`
--
ALTER TABLE `file_definition`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `instrument`
--
ALTER TABLE `instrument`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `job`
--
ALTER TABLE `job`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `user`
--
ALTER TABLE `user`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
--
-- Begrensninger for dumpede tabeller
--

--
-- Begrensninger for tabell `calibration`
--
ALTER TABLE `calibration`
  ADD CONSTRAINT `CALIBRATION_INSTRUMENT` FOREIGN KEY (`instrument_id`)
  REFERENCES `instrument` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Begrensninger for tabell `dataset`
--
ALTER TABLE `dataset`
  ADD CONSTRAINT `DATASET_INSTRUMENT` FOREIGN KEY (`instrument_id`)
  REFERENCES `instrument` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Begrensninger for tabell `dataset_data`
--
ALTER TABLE `dataset_data`
  ADD CONSTRAINT `DATASETDATA_DATASET` FOREIGN KEY (`dataset_id`)
  REFERENCES `dataset` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Begrensninger for tabell `data_file`
--
ALTER TABLE `data_file`
  ADD CONSTRAINT `DATAFILE_FILEDEFINITION` FOREIGN KEY (`file_definition_id`)
  REFERENCES `file_definition` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Begrensninger for tabell `equilibrator_pco2`
--
ALTER TABLE `equilibrator_pco2`
  ADD CONSTRAINT `UNDERWAYPCO2_DATASETDATA` FOREIGN KEY (`measurement_id`)
  REFERENCES `dataset_data` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Begrensninger for tabell `file_column`
--
ALTER TABLE `file_column`
  ADD CONSTRAINT `FILECOLUMN_FILEDEFINITION` FOREIGN KEY (`file_definition_id`)
  REFERENCES `file_definition` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Begrensninger for tabell `file_definition`
--
ALTER TABLE `file_definition`
  ADD CONSTRAINT `FILEDEFINITION_INSTRUMENT` FOREIGN KEY (`instrument_id`)
  REFERENCES `instrument` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Begrensninger for tabell `instrument`
--
ALTER TABLE `instrument`
  ADD CONSTRAINT `INSTRUMENT_OWNER` FOREIGN KEY (`owner`)
  REFERENCES `user` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Begrensninger for tabell `job`
--
ALTER TABLE `job`
  ADD CONSTRAINT `JOB_OWNER` FOREIGN KEY (`owner`)
  REFERENCES `user` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Begrensninger for tabell `run_type`
--
ALTER TABLE `run_type`
  ADD CONSTRAINT `RUNTYPE_FILEDEFINITION` FOREIGN KEY (`file_definition_id`)
  REFERENCES `file_definition` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;
