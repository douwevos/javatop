package com.github.douwevos.javatop.thread;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.douwevos.javatop.JvmService;

public class JvmThreadSampler implements Runnable {

	private final JvmService jvmService;
	private final ThreadListSnapshotObserver observer;

	private int sampleNumber;

	private Thread thread;
	
	private volatile boolean keepRunning = true;
	
	private volatile boolean pause = false;
	
	private volatile SampleMode sampleMode = SampleMode.CPU_TIME;

	private Map<Long, ThreadMonitorInfoBuilder> threadMonitorInfoMap = new ConcurrentHashMap<>();
	private List<Long> threadIdList = new ArrayList<>();

	private volatile ThreadListSnapshot threadListSnapshot;
	
	public JvmThreadSampler(JvmService jvmService, ThreadListSnapshotObserver observer) {
		this.jvmService = jvmService;
		this.observer = observer;
		thread = new Thread(this);
		thread.setName("thread-Sampler");
		thread.start();
	}
	
	public ThreadListSnapshot getThreadListSnapshot() {
		return threadListSnapshot;
	}
	
	public boolean togglePause() {
		pause = !pause;
		return pause;
	}
	
	public SampleMode getSampleMode() {
		return sampleMode;
	}
	
	public void setSampleMode(SampleMode sampleMode) {
		this.sampleMode = sampleMode;
	}
	
	@Override
	public void run() {
		long lastDumpTime = System.nanoTime();
		long dumpTime = System.nanoTime();
		boolean nextIsBadSample = false;
		while(keepRunning) {
			if (pause) {
				nextIsBadSample = true;
			}
			else {
				lastDumpTime = dumpTime;
				dumpTime = System.nanoTime();
				ThreadMXBean threadMXBeanProxy = jvmService.getThreadMXBeanProxy();

				if (threadMXBeanProxy != null) {
					if (threadMXBeanProxy.isThreadContentionMonitoringSupported() && !threadMXBeanProxy.isThreadContentionMonitoringEnabled()) {
						threadMXBeanProxy.setThreadContentionMonitoringEnabled(true);
					}
					long samplePeriodNs = nextIsBadSample ? -1 : dumpTime-lastDumpTime;
					nextIsBadSample = false;
					boolean didUpdate = false;
					ThreadInfo[] allThreads = threadMXBeanProxy.dumpAllThreads(false, false, 1);
					sampleNumber++;
					for(ThreadInfo threadInfo : allThreads) {
						Long threadId = threadInfo.getThreadId();
						long threadBlockedTime = threadInfo.getBlockedTime();
						ThreadMonitorInfoBuilder threadMonitorInfo = threadMonitorInfoMap.get(threadId);
						long threadCpuTime = threadMXBeanProxy.getThreadCpuTime(threadId);
						if (threadMonitorInfo == null) {
							threadMonitorInfo = new ThreadMonitorInfoBuilder(threadId, threadInfo.getThreadName(), threadCpuTime, threadBlockedTime);
							threadMonitorInfoMap.put(threadId, threadMonitorInfo);
							threadIdList.add(threadId);
						}
						else {
							State threadState = threadInfo.getThreadState();
							threadMonitorInfo.shiftThreadCpuTime(samplePeriodNs, threadCpuTime, threadBlockedTime, threadState);
						}
						threadMonitorInfo.setSampleNumber(sampleNumber);
						
						didUpdate = true;
					}
					
					for(Map.Entry<Long, ThreadMonitorInfoBuilder> entry : threadMonitorInfoMap.entrySet()) {
						if (entry.getValue().markDeath(sampleNumber)) {
							threadMonitorInfoMap.remove(entry.getKey());
							threadIdList.remove(entry.getKey());
						}
					}
					
					
					if (didUpdate) {
						
						List<ThreadMonitorInfo> infoList = threadIdList.stream().map(threadMonitorInfoMap::get).map(ThreadMonitorInfoBuilder::build).collect(Collectors.toList());
										
						threadListSnapshot = new ThreadListSnapshot(infoList);
						if (observer != null) {
							observer.onNewSnapshot(threadListSnapshot);
						}
					}
					
				}
			}
			
			try {

				if (sampleMode==SampleMode.CPU_TIME) {
					Thread.sleep(400);
				} else {
					Thread.sleep(100);
				}
				
			} catch (InterruptedException e) {
			}
		}
		
	}
	
	interface ThreadListSnapshotObserver {
		public void onNewSnapshot(ThreadListSnapshot snapshot);
	}
	

	public static class ThreadSample {
		public final int cpuPercentile;
		public final int blockedPercentile;
		public final State threadState;
		public ThreadSample(int cpuPercentile, int blockedPercentile, State threadState) {
			this.cpuPercentile = cpuPercentile;
			this.blockedPercentile = blockedPercentile;
			this.threadState = threadState;
		}
	}

	private static class ThreadMonitorInfoBuilder {

		protected final Long threadId;
		protected final String threadName;
		
		protected final TailListProducer<ThreadSample> samples;
		protected long cpuTime;
		protected long blockedTime;
		protected int sampleNumber;
		protected boolean alive = true;
		protected int deathCount = 0;
		
		public ThreadMonitorInfoBuilder(Long threadId, String threadName, long cputTime, long blockedTime) {
			this.threadId = threadId;
			this.threadName = threadName;
			int sampleSize = 300;
			this.samples = new TailListProducer<>(sampleSize, sampleSize);
			this.blockedTime = blockedTime;
		}

		public void setSampleNumber(int sampleNumber) {
			this.sampleNumber = sampleNumber;
		}
		
		public boolean isAlive() {
			return alive;
		}
		
		public void shiftThreadCpuTime(long samplePeriodNs, long threadCpuTime, long threadBlockedTimeMs, State threadState) {
			int cpuPercentile = 0;
			if (threadCpuTime<=0) {
				cpuPercentile = 0;
			}
			else {
				long deltaCpuTime = threadCpuTime-cpuTime;
				cpuTime = threadCpuTime;
				cpuPercentile = (int) ((deltaCpuTime*1000)/samplePeriodNs);
			}
			
			int blockedPercentile = 0;
			if (threadBlockedTimeMs>0 && threadBlockedTimeMs!=blockedTime) {
				long deltaBlockTimeNs = (threadBlockedTimeMs-blockedTime) * TimeUnit.MILLISECONDS.toNanos(1);
				blockedPercentile = (int) ((deltaBlockTimeNs * 1000)/ samplePeriodNs);
				blockedTime = threadBlockedTimeMs;
				if (deltaBlockTimeNs>0 && blockedPercentile<=0) {
					blockedPercentile = 1;
				}
			}
			
			samples.add(new ThreadSample(cpuPercentile, blockedPercentile, threadState));
		}

		public boolean markDeath(int sampleNumber) {
			if (sampleNumber == this.sampleNumber) {
				return false;
			}
			this.sampleNumber = sampleNumber;
			shiftThreadCpuTime(0, 0, 0, null);
			alive = false;
			deathCount++;
			return deathCount>samples.length();
		}

		public ThreadMonitorInfo build() {
			return new ThreadMonitorInfo(threadId, threadName, samples.produce());
//			ThreadMonitorInfo copy = new ThreadMonitorInfo(threadId, threadName, samples.length);
//			copy.cpuTime = cpuTime;
//			copy.alive = alive;
//			System.arraycopy(samples, 0, copy.samples, 0, samples.length);
//			return copy;
		}

		
	}

	public static class ThreadMonitorInfo {
		
		private final Long threadId;
		private final String threadName;
		private final List<ThreadSample> samples;
		
		public ThreadMonitorInfo(Long threadId, String threadName, List<ThreadSample> samples) {
			this.threadId = threadId;
			this.threadName = threadName;
			this.samples = samples;
		}
		
		public List<ThreadSample> getSamples() {
			return samples;
		}
		
		public Long getThreadId() {
			return threadId;
		}
		
		public String getThreadName() {
			return threadName;
		}
		
	}

	
	public static class ThreadListSnapshot {
		
		private final List<ThreadMonitorInfo> infoList;
		
		public ThreadListSnapshot(List<ThreadMonitorInfo> infoList) {
			this.infoList = infoList;
		}

		public List<ThreadMonitorInfo> getInfoList() {
			return infoList;
		}
	}

	public void stop() {
		keepRunning = false;
		if (thread != null) {
			thread.interrupt();
		}
		try {
			thread.join(500);
		} catch (InterruptedException e) {
		}
	}
	
	
	public static enum SampleMode {
		CPU_TIME,
		THREAD_STATES
	}

	
}
