package uk.ac.exeter.QuinCe.web.Instrument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import uk.ac.exeter.QuinCe.jobs.Job;

public class SampleFileExtractor implements Runnable {

	/**
	 * The bean that contains the uploaded file,
	 * and is also where the extracted data should be stored.
	 */
	private NewInstrumentBean sourceBean;
	
	/**
	 * The progress of the extraction
	 */
	private int progress = 0;
	
	/**
	 * The status of the extraction process
	 */
	private String status = Job.WAITING_STATUS;
	
	/**
	 * A flag to indicate if the thread should be stopped early.
	 */
	private boolean terminate = false;
	
	/**
	 * Simple constructor.
	 * @param sourceBean The source bean that contains the file to be extracted
	 */
	public SampleFileExtractor(NewInstrumentBean sourceBean) {
		this.sourceBean = sourceBean;
	}
	
	/**
	 * Perform the extraction
	 */
	@Override
	public void run() {

		status = Job.RUNNING_STATUS;
		
		long fileSize = sourceBean.getFile().getSize();
		long bytesProcessed = 0;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(sourceBean.getFile().getInputstream()));		
			String line;
			int lineCount = 0;
			while ((line = in.readLine()) != null) {
				if (terminate) {
					// TODO DO SOMETHING
					break;
				}
				
				String[] splitLine = line.split(sourceBean.getSeparator());
				lineCount++;
				if (lineCount == 1) {
					//TODO Check if count is one. If it is, the split char is wrong
					
					sourceBean.setSampleFileColumnCount(splitLine.length);
				} else {
					if (sourceBean.getSampleFileColumnCount() != splitLine.length) {
						// TODO ERROR
						break;
					}
				}
				
				sourceBean.addSampleFileLine(splitLine);
				bytesProcessed += line.getBytes(StandardCharsets.UTF_8).length;
				progress = (int) (((double) bytesProcessed / (double) fileSize) * 100);
			}
		} catch (Exception e) {
			// TODO Don't know yet.
		}
		
		progress = 100;
		status = Job.FINISHED_STATUS;
		
	}

	/**
	 * Retrieve the current progress of the extraction
	 * @return The current progress of the extraction
	 */
	protected int getProgress() {
		return progress;
	}
	
	/**
	 * Retrieve the current state of the extraction job
	 * @return The current state of the extraction job
	 */
	protected String getStatus() {
		return status;
	}
	
	protected void terminate() {
			terminate = true;
	}
}
