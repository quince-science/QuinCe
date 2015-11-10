package uk.ac.exeter.QuinCe.web.Instrument;

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
	 * The number of columns in the input file
	 */
	private int columnCount = 0;
	
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
		
		for (int i = 0; i <= 5; i++) {
			if (terminate) {
				break;
			}
			progress = i * 20;
			System.out.println("Ex " + progress);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// Don't care
			}
		}
		
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
