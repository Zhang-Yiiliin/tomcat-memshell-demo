import com.sun.tools.attach.VirtualMachine;

public class Attach {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Attach <pid> <agent.jar> [agentArgs]");
            System.exit(1);
        }
        String pid = args[0];
        String agentJar = args[1];
        String agentArgs = args.length >= 3 ? args[2] : null;

        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            if (agentArgs == null)
                vm.loadAgent(agentJar);
            else
                vm.loadAgent(agentJar, agentArgs);
        } finally {
            vm.detach();
        }
    }
}