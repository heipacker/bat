package com.dlmu.agent.test;

import com.sun.tools.attach.*;
import com.sun.tools.attach.spi.AttachProvider;
import sun.tools.attach.LinuxVirtualMachine;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by fupan on 16-4-2.
 */
public class TestAgent {

    private static Instrumentation instrumentation() {
        ClassLoader mainAppLoader = ClassLoader.getSystemClassLoader();
        try {
            Class<?> javaAgentClass = mainAppLoader.loadClass("com.dlmu.agent.AgentTest");
            Method method = javaAgentClass.getDeclaredMethod("instrumentation", new Class[0]);
            return (Instrumentation) method.invoke(null, new Object[0]);
        } catch (Throwable e) {
            System.out.println("can not get agent class" + e.getStackTrace());
            return null;
        }
    }

    /**
     * 第一个种， 程序启动是attach
     * VM options: -javaagent:/home/fupan/IdeaProjects/agentTest/agent-jar/target/agent-jar-1.0-SNAPSHOT.jar
     * 第二种， 程序启动后attach
     * attachAgent();
     *
     * @param args
     */
    public static void main(String[] args) {
        //程序启动时attach得把这个注释掉
        attachAgent();

        System.out.println("run test agent");
        Instrumentation instrumentation = instrumentation();
        ClassTransformerSupport.addCode(instrumentation);
    }

    private String jarFilePath = "/home/fupan/IdeaProjects/agentTest/agent-jar/target/agent-jar-1.0-SNAPSHOT.jar";

    private static void attachAgent() {
        TestAgent testAgent = new TestAgent();
        testAgent.loadAgent();
    }

    void loadAgent() {
        VirtualMachine vm;

        if (AttachProvider.providers().isEmpty()) {
            String vmName = System.getProperty("java.vm.name");

            if (vmName.contains("HotSpot")) {
                vm = getVirtualMachineImplementationFromEmbeddedOnes();
            } else {
                String helpMessage = getHelpMessageForNonHotSpotVM(vmName);
                throw new IllegalStateException(helpMessage);
            }
        } else {
            vm = attachToRunningVM();
        }

        loadAgentAndDetachFromRunningVM(vm);
    }

    private static AttachProvider ATTACH_PROVIDER = new AttachProvider() {
        @Override
        public String name() {
            return null;
        }

        @Override
        public String type() {
            return null;
        }

        @Override
        public VirtualMachine attachVirtualMachine(String s) throws AttachNotSupportedException, IOException {
            return null;
        }

        @Override
        public List<VirtualMachineDescriptor> listVirtualMachines() {
            return null;
        }
    };

    private static VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes() {
        Class<? extends VirtualMachine> vmClass = findVirtualMachineClassAccordingToOS();
        Class<?>[] parameterTypes = {AttachProvider.class, String.class};
        String pid = getProcessIdForRunningVM();

        try {
            // This is only done with Reflection to avoid the JVM pre-loading all the XyzVirtualMachine classes.
            Constructor<? extends VirtualMachine> vmConstructor = vmClass.getConstructor(parameterTypes);
            VirtualMachine newVM = vmConstructor.newInstance(ATTACH_PROVIDER, pid);
            return newVM;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoClassDefFoundError e) {
            throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
        } catch (UnsatisfiedLinkError e) {
            throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
        }
    }

    private static Class<? extends VirtualMachine> findVirtualMachineClassAccordingToOS() {
        if (File.separatorChar == '\\') {
//            return WindowsVirtualMachine.class;
        }

        String osName = System.getProperty("os.name");

        if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
            return LinuxVirtualMachine.class;
        } else if (osName.startsWith("Mac OS X")) {
//            return BsdVirtualMachine.class;
        } else if (osName.startsWith("Solaris")) {
//            return SolarisVirtualMachine.class;
        }

        throw new IllegalStateException("Cannot use Attach API on unknown OS: " + osName);
    }

    private static String getProcessIdForRunningVM() {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        return nameOfRunningVM.substring(0, p);
    }

    private String getHelpMessageForNonHotSpotVM(String vmName) {
        String helpMessage = "To run on " + vmName;

        if (vmName.contains("J9")) {
            helpMessage += ", add <IBM SDK>/lib/tools.jar to the runtime classpath (before jmockit), or";
        }

        return helpMessage + " use -javaagent:" + jarFilePath;
    }

    private static VirtualMachine attachToRunningVM() {
        String pid = getProcessIdForRunningVM();

        try {
            return VirtualMachine.attach(pid);
        } catch (AttachNotSupportedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadAgentAndDetachFromRunningVM(VirtualMachine vm) {
        try {
            vm.loadAgent(jarFilePath, null);
            vm.detach();
        } catch (AgentLoadException e) {
            throw new IllegalStateException(e);
        } catch (AgentInitializationException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
