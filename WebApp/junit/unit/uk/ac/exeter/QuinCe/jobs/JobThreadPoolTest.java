package unit.uk.ac.exeter.QuinCe.jobs;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

import uk.ac.exeter.QuinCe.jobs.InvalidThreadCountException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.jobs.JobThreadPool;
import uk.ac.exeter.QuinCe.jobs.JobThreadPoolNotInitialisedException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class JobThreadPoolTest extends BaseJobTest {
	
	@Test(expected=JobThreadPoolNotInitialisedException.class)
	public void getPoolNotInitialised() throws Exception {
		JobThreadPool.getInstance();
	}
	
	@Test(expected=InvalidThreadCountException.class)
	public void getPoolZero() throws Exception {
		initThreadPool(0);
	}
	
	@Test(expected=InvalidThreadCountException.class)
	public void getPoolNegative() throws Exception {
		initThreadPool(-1);
	}
	
	@Test(expected=MissingParamException.class)
	public void getThreadMissingJob() throws Exception {
		initThreadPool(1);
		JobThreadPool.getInstance().getJobThread(null);
	}
	
	@Test
	public void initPoolGood() throws Exception {
		initThreadPool(2);
		assertEquals(2, JobThreadPool.getInstance().getAvailableThreads());
	}
	
	@Test
	public void getThreadOneOfOne() throws Exception {
		initThreadPool(1);
		long jobID = createTestJob(1);
		Job job = JobManager.getNextJob(getDataSource(), null);
		JobThread thread = JobThreadPool.getInstance().getJobThread(job);
		assertEquals("JOB_" + jobID, thread.getName());
		assertEquals(0, JobThreadPool.getInstance().getAvailableThreads());
	}
		
	@Test
	public void getThreadTwoOfOne() throws Exception {
		initThreadPool(1);
		createTestJob(1);
		createTestJob(1);
		Job job = JobManager.getNextJob(getDataSource(), null);
		JobThread thread1 = JobThreadPool.getInstance().getJobThread(job);
		JobThread thread2 = JobThreadPool.getInstance().getJobThread(job);
		
		assertNotNull(thread1);
		assertNull(thread2);
	}
	
	@Test
	public void getThreadTwoOfTwo() throws Exception {
		initThreadPool(2);
		createTestJob(1);
		createTestJob(1);
		Job job1 = JobManager.getNextJob(getDataSource(), null);
		Job job2 = JobManager.getNextJob(getDataSource(), null);
		JobThread thread1 = JobThreadPool.getInstance().getJobThread(job1);
		JobThread thread2 = JobThreadPool.getInstance().getJobThread(job2);
		
		assertNotNull(thread1);
		assertNotNull(thread2);
	}
	
	@Test
	public void testThreadReuse() throws Exception {
		initThreadPool(1);
		createTestJob(1);
		createTestJob(1);
		
		Job job1 = JobManager.getNextJob(getDataSource(), null);
		Job job2 = JobManager.getNextJob(getDataSource(), null);

		JobThread thread1 = JobThreadPool.getInstance().getJobThread(job1);
		assertNull(JobThreadPool.getInstance().getJobThread(job2));
		thread1.start();
		
		JobThread thread2 = JobThreadPool.getInstance().getJobThread(job2);
		assertNotNull(thread2);
	}
	
	@Test
	public void instantThreadTwoOfThree() throws Exception {
		initThreadPool(3);
		long job1ID = createTestJob(1);
		long job2ID = createTestJob(1);
		long job3ID = createTestJob(1);
		
		Job job1 = JobManager.getJob(getDataSource(), null, job1ID);
		Job job2 = JobManager.getJob(getDataSource(), null, job2ID);
		Job job3 = JobManager.getJob(getDataSource(), null, job3ID);

		JobThread thread1 = JobThreadPool.getInstance().getJobThread(job1);
		assertNotNull(thread1);
		
		JobThread instantThread = JobThreadPool.getInstance().getInstantJobThread(job2);
		assertNotNull(instantThread);
		
		JobThread thread3 = JobThreadPool.getInstance().getJobThread(job3);
		assertNotNull(thread3);
	}
	
	@Test
	public void instantThreadThreeOfTwo() throws Exception {
		initThreadPool(2);
		long job1ID = createTestJob(1);
		long job2ID = createTestJob(1);
		long job3ID = createTestJob(1);

		Job job1 = JobManager.getJob(getDataSource(), null, job1ID);
		Job job2 = JobManager.getJob(getDataSource(), null, job2ID);
		Job job3 = JobManager.getJob(getDataSource(), null, job3ID);

		JobThread thread1 = JobThreadPool.getInstance().getJobThread(job1);
		assertNotNull(thread1);

		JobThread instantThread = JobThreadPool.getInstance().getInstantJobThread(job2);
		assertNotNull(instantThread);
		
		JobThread thread3 = JobThreadPool.getInstance().getJobThread(job3);
		assertNull(thread3);
	}
	
	@Test
	public void instantThreadOverflow() throws Exception {
		initThreadPool(2);
		long job1ID = createTestJob(1);
		long job2ID = createTestJob(1);
		long job3ID = createTestJob(1);
		
		Job job1 = JobManager.getJob(getDataSource(), null, job1ID);
		Job job2 = JobManager.getJob(getDataSource(), null, job2ID);
		Job job3 = JobManager.getJob(getDataSource(), null, job3ID);

		JobThread thread1 = JobThreadPool.getInstance().getJobThread(job1);
		assertNotNull(thread1);
		
		JobThread thread2 = JobThreadPool.getInstance().getJobThread(job2);
		assertNotNull(thread2);

		JobThread instantThread = JobThreadPool.getInstance().getInstantJobThread(job3);
		assertNotNull(instantThread);
	}
	
	@Test
	public void testInstantNotOverflowReturnedToPool() throws Exception {
		initThreadPool(2);
		long job1ID = createTestJob(1);
		long job2ID = createTestJob(1);
		long job3ID = createTestJob(1);
		
		Job job1 = JobManager.getJob(getDataSource(), null, job1ID);
		Job job2 = JobManager.getJob(getDataSource(), null, job2ID);
		Job job3 = JobManager.getJob(getDataSource(), null, job3ID);

		JobThreadPool.getInstance().getJobThread(job1);
		JobThread instantThread = JobThreadPool.getInstance().getInstantJobThread(job2);
		instantThread.start();
		
		JobThread thread3 = JobThreadPool.getInstance().getJobThread(job3);
		assertNotNull(thread3);
	}
	
	@Test
	public void testInstantOverflowNotReturnedToPool() throws Exception {
		initThreadPool(1);
		long job1ID = createTestJob(1);
		long job2ID = createTestJob(1);
		long job3ID = createTestJob(1);
		
		Job job1 = JobManager.getJob(getDataSource(), null, job1ID);
		Job job2 = JobManager.getJob(getDataSource(), null, job2ID);
		Job job3 = JobManager.getJob(getDataSource(), null, job3ID);

		JobThreadPool.getInstance().getJobThread(job1);
		JobThread instantThread = JobThreadPool.getInstance().getInstantJobThread(job2);
		instantThread.start();
		
		JobThread thread3 = JobThreadPool.getInstance().getJobThread(job3);
		assertNull(thread3);
	}
	


	private void initThreadPool(int maxThreads) throws Exception {
		JobThreadPool.initialise(maxThreads);
	}
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		JobThreadPool.destroy();
	}
}
