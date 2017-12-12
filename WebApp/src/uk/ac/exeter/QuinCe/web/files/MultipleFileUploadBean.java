package uk.ac.exeter.QuinCe.web.files;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.FileExistsException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.Instrument.newInstrument.FileDefinitionBuilder;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

@ManagedBean(name="fileUpload")
@ViewScoped
public class MultipleFileUploadBean extends BaseManagedBean {
	/**
	 * The data file object
	 */
	private ArrayList<UploadedFileExtended> dataFiles = new ArrayList<>();
	private String displayClass = "hidden";


	public MultipleFileUploadBean() {
		initialiseInstruments();
	}

	/**
	 * Handle the file upload and subsequent processing.
	 * @param event The file upload event
	 */
	public final void handleFileUpload(FileUploadEvent event) {
		UploadedFileExtended file = new UploadedFileExtended(event.getFile());
		dataFiles.add(file);
		setDisplayClass("");
	}

	public List<UploadedFileExtended> getUploadedFiles() {
		return dataFiles;
	}


	/**
	 * Refresh file list in frontend
	 */
	public void refreshFileList() {
		RequestContext.getCurrentInstance().update("uploadForm:fileList");
	}

	/**
	 * Extract next file in file list that is not yet extracted
	 */
	public void extractNext() {
		for (UploadedFileExtended file: dataFiles) {
			if (file.getDataFile() == null) {
				extractFile(file);
				return;
			}
		}
	}

	/**
	 * Store selected files. This moves the file(s) to the file store, and updates the database with file 
	 * info.
	 * @throws MissingParamException
	 * @throws FileExistsException
	 * @throws DatabaseException
	 */
	public void store() throws MissingParamException, FileExistsException, DatabaseException {
		for (UploadedFileExtended file: dataFiles) {
			if (file.isStore() && null != file.getDataFile()) {
				DataFileDB.storeFile(getDataSource(), getAppConfig(), file.getDataFile());
			}
		}
	}

	/**
	 * Extract and process the uploaded file's contents
	 */
	public void extractFile(UploadedFileExtended file) {
		try {
			if (null == currentFullInstrument) {
				currentFullInstrument = InstrumentDB.getInstrument(
						getDataSource(),
						getCurrentInstrument(),
						ServletUtils.getResourceManager().getSensorsConfiguration(),
						ServletUtils.getResourceManager().getRunTypeCategoryConfiguration()
				);
			}

			FileDefinitionBuilder guessedFileLayout = new FileDefinitionBuilder(currentFullInstrument.getFileDefinitions());
			guessedFileLayout.setFileContents(Arrays.asList(file.getLines()));
			guessedFileLayout.guessFileLayout();
			FileDefinition fileDefinition = currentFullInstrument.getFileDefinitions()
					.getMatchingFileDefinition(guessedFileLayout).iterator().next();
			// TODO Handle multiple matched definitions

			file.setDataFile(new DataFile(
					getAppConfig().getProperty("filestore"),
					fileDefinition,
					file.getName(),
					Arrays.asList(file.getLines())
			));

			if (file.getDataFile().getMessageCount() > 0) {
				setMessage(null, file.getName() + " could not be processed (see messages below). Please fix these problems and upload the file again.");
			} else if (
					DataFileDB.fileExistsWithDates(
							getDataSource(),
							fileDefinition.getDatabaseId(),
							file.getDataFile().getStartDate(),
							file.getDataFile().getEndDate()
					)
			) {
				// TODO This is what the front end uses to detect that the file was not processed successfully.
				//This can be improved when overlapping files are implemented instead of being rejected.
				fileDefinition = null;
				file.setDataFile(null);
				setMessage(null, "A file already exists that covers overlaps with this file. Please upload a different file.");
			}
		} catch (NoSuchElementException nose) {
			file.setDataFile(null);
			setMessage(null, "The format of " + file.getName() + " was not recognised. Please upload a different file.");
			return;
		} catch (Exception e){
			file.setDataFile(null);
			e.printStackTrace();
			setMessage(null, "The file could not be processed: " + e.getMessage());
		}
	}

	/**
	 * @return the displayClass
	 */
	public String getDisplayClass() {
		return displayClass;
	}

	/**
	 * @param displayClass the displayClass to set
	 */
	public void setDisplayClass(String displayClass) {
		this.displayClass = displayClass;
	}

	/**
	 * @return the class "hidden" if there are no datafiles yet. Otherwise returns an empty string.
	 */
	public String getStoreFileButtonClass() {
		return dataFiles.size()>0 ? "" : "hidden";
	}
}
