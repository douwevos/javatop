package com.github.douwevos.javatop;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class JvmService {

	private final JvmDescription jvmDescription;
	
	private ThreadMXBean threadMXBeanProxy;
	private RuntimeMXBean runtimeMXBeanProxy;
	
	
	public JvmService(JvmDescription jvmDescription) {
		this.jvmDescription = jvmDescription;
	}
	
	public void connect() {

		try {
			VirtualMachine vm = VirtualMachine.attach(jvmDescription.vmId);
			Properties agentProps = vm.getAgentProperties();
	        String address = vm.startLocalManagementAgent();
	        if (address == null) {
	            String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress";
	            address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
	        }
	        if (address != null) {
	        	JMXServiceURL jmxServiceURL = new JMXServiceURL(address);
	        	JMXConnector connector = JMXConnectorFactory.connect(jmxServiceURL);
	        	MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();
	        	threadMXBeanProxy = ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
	        	runtimeMXBeanProxy = ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
	        	
	        	
	        }
			vm.detach();
		} catch(IOException | AttachNotSupportedException e) {
		}
	}
	
	
	public ThreadMXBean getThreadMXBeanProxy() {
		return threadMXBeanProxy;
	}
	
	public RuntimeInfo getRuntimeInfo() {
		if (runtimeMXBeanProxy != null) {
			return new RuntimeInfo(jvmDescription, runtimeMXBeanProxy);
		}
		return null;
	}
	
	public static class RuntimeInfo {
		public final JvmDescription jvmDescription;
		public final TemporalAmount uptime;
		public final String name;
		public final long pid;
		public final String vmName;
		public final String vmVendor;
		public final String vmVersion;
		
		public final Map<String, String> systemProperties;
		
		public RuntimeInfo(JvmDescription jvmDescription, RuntimeMXBean bean) {
			this.jvmDescription = jvmDescription;
			uptime = Duration.ofMillis(bean.getUptime());
			name = bean.getName();
			pid = getPid(bean);
			vmName = bean.getVmName();
			vmVendor = bean.getVmVendor();
			vmVersion = bean.getVmVersion();
			systemProperties = bean.getSystemProperties();
		}
		
		private final long getPid(RuntimeMXBean bean) {
			try {
				return bean.getPid();
			} catch(Exception e) {
			}
			return -1;
		}
	}
	
	

	public static List<JvmDescription> enlistVirtualMachines() {
		List<JvmDescription> result = new ArrayList<>();
		List<VirtualMachineDescriptor> list = VirtualMachine.list();
		for(VirtualMachineDescriptor vmDesc : list) {
			boolean canAttach = vmDesc.provider()!=null;
			result.add(new JvmDescription(vmDesc.id(), vmDesc.displayName(), canAttach));
		}
		return result;
	}
	
	

	public static class JvmDescription {
		public final String vmId;
		public final String displayName;
		public final boolean canAttach;
		
		public JvmDescription(String vmId, String displayName, boolean canAttach) {
			this.vmId = vmId;
			this.displayName = displayName;
			this.canAttach = canAttach;
		}
	}
}
