package com.github.douwevos.javatop.block;

import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.github.douwevos.javatop.JvmService;

public class JvmBlockSampler implements Runnable {

	private final JvmService jvmService;
	private final BlockListSnapshotObserver observer;

	private final List<BlockInfoMap> samples = new ArrayList<>(30);
	
	private Thread thread;
	
	private volatile boolean keepRunning = true;
	
	private volatile boolean pause = false;
	
	private volatile List<BlockInfoMap> snapshot;

	
	public JvmBlockSampler(JvmService jvmService, BlockListSnapshotObserver observer) {
		this.jvmService = jvmService;
		this.observer = observer;
		thread = new Thread(this);
		thread.setName("block-Sampler");
		thread.start();
	}
	
	public List<BlockInfoMap> getSnapshot() {
		return snapshot;
	}
	
	public boolean togglePause() {
		pause = !pause;
		return pause;
	}

	
	@Override
	public void run() {
		while(keepRunning) {
			if (!pause) {
				ThreadMXBean threadMXBeanProxy = jvmService.getThreadMXBeanProxy();

				if (threadMXBeanProxy != null) {
					if (threadMXBeanProxy.isThreadContentionMonitoringSupported() && !threadMXBeanProxy.isThreadContentionMonitoringEnabled()) {
						threadMXBeanProxy.setThreadContentionMonitoringEnabled(true);
					}
					BlockInfoMap blockInfoMap = new BlockInfoMap();

					boolean hadBlocker = false;
					
					ThreadInfo[] allThreads = threadMXBeanProxy.dumpAllThreads(true, true);
					for(ThreadInfo threadInfo : allThreads) {

						Long threadId = threadInfo.getThreadId();
						LockInfo[] lockedSynchronizers = threadInfo.getLockedSynchronizers();
						MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
						LockInfo lockInfo = threadInfo.getLockInfo();
						
						if (lockInfo == null && (lockedSynchronizers==null || lockedSynchronizers.length==0) && lockedMonitors==null) {
							continue;
						}
						
						State threadState = threadInfo.getThreadState();
						BlockClassDetails classDetails = new BlockClassDetails(threadId, threadState, threadInfo.getStackTrace());

						if (lockInfo != null && threadState == State.BLOCKED) {
							blockInfoMap.registerBlocked(lockInfo, classDetails);
							
							
							hadBlocker = true;
						}
						
						if (lockedSynchronizers!=null) {
							for(LockInfo lockInfoOwner : lockedSynchronizers) {
								blockInfoMap.registerOwner(lockInfoOwner, classDetails);
							}
						}
						
						if (lockedMonitors !=null) {
							for(MonitorInfo monitorInfo : lockedMonitors) {
								blockInfoMap.registerOwner(monitorInfo, classDetails);
							}
						}
					}

					if (!blockInfoMap.isEmpty()) {
						List<BlockInfoMap> snapshot = null;
						if (hadBlocker) {
							samples.add(blockInfoMap);
							if (samples.size()>30) {
								samples.remove(0);
							}
							snapshot = new ArrayList<>(samples);
						} else {
							snapshot = new ArrayList<>(samples);
							snapshot.add(blockInfoMap);
						}
						this.snapshot = snapshot;
						if (observer != null) {
							observer.onNewSnapshot(snapshot);
						}
						
					}
				}
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	
	
	
	interface BlockListSnapshotObserver {
		public void onNewSnapshot(List<BlockInfoMap> snapshot);
	}
	
	public static class LockKey {
		
		private final String classname;
		private final int identityHashCode;
		
		public LockKey(String classname, int identityHashCode) {
			this.classname = classname;
			this.identityHashCode = identityHashCode;
		}
		
		public LockKey(LockInfo lockInfo) {
			this.classname = lockInfo.getClassName();
			this.identityHashCode = lockInfo. getIdentityHashCode();
		}

		public String name() {
			return classname + "#"+identityHashCode;
		}
		
		@Override
		public int hashCode() {
			return Objects.hashCode(classname) + identityHashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof LockKey) {
				LockKey that = (LockKey) obj;
				return that.identityHashCode==identityHashCode
						&& Objects.equals(classname, that.classname);
			}
			return false;
		}
	}

	
	public static class BlockClassDetails {
		
		private final Long threadId;
		private final State threadState;
		private final StackTraceElement[] stackTrace;
		
		public BlockClassDetails(Long threadId, State threadState, StackTraceElement[] stackTrace) {
			this.threadId = threadId;
			this.threadState = threadState;
			this.stackTrace = stackTrace;
		}
		
		public Long getThreadId() {
			return threadId;
		}
		
		public State getThreadState() {
			return threadState;
		}
		
		public StackTraceElement[] getStackTrace() {
			return stackTrace;
		}
		
	}
	
	
	public static class BlockInfo {
		
		private final LockKey lockKey;
		private BlockClassDetails owner;
		
		private List<BlockClassDetails> blocked = new ArrayList<>();
		
		public BlockInfo(LockKey lockKey) {
			this.lockKey = lockKey;
		}

		public void addBlockedThread(int sampleNumber, BlockClassDetails classDetails) {
			blocked.add(classDetails);
		}

		public void setOwner(BlockClassDetails classDetails) {
			this.owner = classDetails;
		}
		
		
		public BlockClassDetails getOwner() {
			return owner;
		}
		
		public List<BlockClassDetails> getBlocked() {
			return blocked;
		}
		
		public int blockedCount() {
			return blocked.size();
		}
		
		public LockKey getLockKey() {
			return lockKey;
		}
		
	}

	
	
	public static class BlockInfoMap {

		private boolean hasBlocked = false;
		private final Map<LockKey, BlockInfo> map = new ConcurrentHashMap<>();

		public void registerBlocked(LockInfo lockInfo, BlockClassDetails classDetails) {
			BlockInfo blockInfo = produceBlockInfo(lockInfo);
			blockInfo.addBlockedThread(0, classDetails);
			hasBlocked = true;
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}
		
		public boolean hasBlocked() {
			return hasBlocked;
		}

		public void registerOwner(LockInfo lockInfoOwner, BlockClassDetails classDetails) {
			BlockInfo blockInfo = produceBlockInfo(lockInfoOwner);
			blockInfo.setOwner(classDetails);
		}

		private BlockInfo produceBlockInfo(LockInfo lockInfoOwner) {
			LockKey lockKey = new LockKey(lockInfoOwner);
			BlockInfo blockInfo = map.computeIfAbsent(lockKey, s -> new BlockInfo(lockKey));
			return blockInfo;
		}
		
		public Map<LockKey, BlockInfo> getMap() {
			return map;
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
	
	
}
