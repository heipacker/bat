package com.dlmu.agent;

import com.sun.tools.attach.*;
import com.sun.tools.attach.spi.AttachProvider;
import sun.tools.attach.LinuxVirtualMachine;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by fupan on 16-4-9.
 */
@Deprecated
public class AgentSupport {

    public static Instrumentation getInstrumentation() {
        Instrumentation instrumentation = doGetInstrumentation();
        if (instrumentation == null) {
            throw new IllegalStateException("The Byte Buddy agent is not initialized");
        }
        return instrumentation;
    }

    /**
     * The name of this class'S {@code instrumentation} field.
     */
    private static final String INSTRUMENTATION_FIELD_NAME = "instrumentation";

    /**
     * An indicator variable to express that no instrumentation is available.
     */
    private static final Instrumentation UNAVAILABLE = null;

    /**
     * Base for access to a reflective member to make the code more readable.
     */
    private static final Object STATIC_MEMBER = null;

    private static Instrumentation doGetInstrumentation() {
        try {
            Field declaredField = ClassLoader.getSystemClassLoader()
                    .loadClass(AgentInstaller.class.getName())
                    .getDeclaredField(INSTRUMENTATION_FIELD_NAME);
            declaredField.setAccessible(true);
            return (Instrumentation) declaredField.get(STATIC_MEMBER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return UNAVAILABLE;
    }

    /**
     *
     * @param jarFilePath
     */
    public static void loadAgent(String jarFilePath) {
        VirtualMachine vm;

        if (AttachProvider.providers().isEmpty()) {
            String vmName = System.getProperty("java.vm.name");

            if (vmName.contains("HotSpot")) {
                vm = getVirtualMachineImplementationFromEmbeddedOnes();
            } else {
                String helpMessage = getHelpMessageForNonHotSpotVM(vmName, jarFilePath);
                throw new IllegalStateException(helpMessage);
            }
        } else {
            vm = attachToRunningVM();
        }

        loadAgentAndDetachFromRunningVM(vm, jarFilePath);
    }

    private static AttachProvider EMPTY_ATTACH_PROVIDER = new AttachProvider() {
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
            VirtualMachine newVM = vmConstructor.newInstance(EMPTY_ATTACH_PROVIDER, pid);
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

    private static String getHelpMessageForNonHotSpotVM(String vmName, String jarFilePath) {
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

    private static void loadAgentAndDetachFromRunningVM(VirtualMachine vm, String jarFilePath) {
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
