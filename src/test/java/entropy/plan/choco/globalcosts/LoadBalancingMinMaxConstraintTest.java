package entropy.plan.choco.globalcosts;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import entropy.configuration.*;
import entropy.plan.PlanException;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsSimpleBP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.vjob.VJob;

public class LoadBalancingMinMaxConstraintTest {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
            .getLogger(LoadBalancingMinMaxConstraintTest.class);

    public static int NODECPUCAPA = 1000;
    public static int NODERAMCAPA = 1000;

    /**
     * generate a simple config of nodes and waiting VMs.
     * 
     * @param nbNodes number of nodes
     * @param nbVMPerNode number of VMs to generate, per node
     * @param vmPcCPU percentage of CPU used on a node, per VM
     * @param vmPcRAM percentage of RAM used on a node, per VM
     * @return an homogeneous config fulfilling those requirements.<br />
     *         The nodes have {@value #NODECPUCAPA} CPU capacity and {@value #NODECPUCAPA} ram
     *         capacity, 1 CPU core.
     */
    public static SimpleConfiguration makeSimpleConfiguration(int nbNodes,
            int nbVMPerNode, int vmPcCPU, int vmPcRAM) {
        SimpleConfiguration ret = new SimpleConfiguration();
        for (int i = 0; i < nbNodes; i++) {
            SimpleNode n = new SimpleNode("n" + i, 1, NODECPUCAPA, NODERAMCAPA);
            ret.addOnline(n);
        }
        int vmLoadCPU = vmPcCPU * 10;
        int vmLoadRAM = vmPcRAM * 10;
        for (int i = 0; i < nbVMPerNode * nbNodes; i++) {
            SimpleVirtualMachine svm = new SimpleVirtualMachine("VM" + i, 1,
                    vmLoadCPU, vmLoadRAM);
            ret.addWaiting(svm);
        }
        return ret;
    }

    @Test(groups = "unit", dataProvider = "simpleExecData")
    public void simpleExec(int nbNodes, int nbVmsPerNode, int pcCPUPerVM,
            int pcRAMPerVM) throws PlanException {
        Configuration cfg = makeSimpleConfiguration(nbNodes, nbVmsPerNode,
                pcCPUPerVM, pcRAMPerVM);
        ChocoCustomRP solver = generateClassSolver();
        List<VJob> vjobs = new ArrayList<VJob>();
        entropy.plan.TimedReconfigurationPlan result = solver.compute(cfg,
                cfg.getAllVirtualMachines(),
                new SimpleManagedElementSet<VirtualMachine>(),
                new SimpleManagedElementSet<VirtualMachine>(),
                new SimpleManagedElementSet<VirtualMachine>(),
                cfg.getAllNodes(), new SimpleManagedElementSet<Node>(), vjobs);
        Configuration dest = result.getDestination();
        // each node should have pcCPUPerVM*nbVmsPerNode % load
        logger.debug("objective load is " + nbVmsPerNode * pcCPUPerVM * 10
                + "out of " + NODECPUCAPA);
        for (Node n : dest.getAllNodes()) {
            int load = 0;
            for (VirtualMachine vm : dest.getRunnings(n)) {
                load += vm.getCPUDemand();
            }
            Assert.assertEquals(load, nbVmsPerNode * pcCPUPerVM * 10,
                    "end configuration : " + dest);
        }
    }

    /**
     * @return a list of [nodes number, VMs per node number, percentage CPU per VM, percentage RAM
     *         per VM]
     */
    @DataProvider(name = "simpleExecData")
    public Object[][] simpleExecData() {
        return new Object[][] { { 2, 2, 10, 10 }, { 10, 3, 20, 10 } };
    }

    /** generate a correct solver for the tests methods */
    public static ChocoCustomRP generateClassSolver() {
        ChocoCustomRP solver = new ChocoCustomRP(new MockDurationEvaluator(1,
                2, 3, 4, 5, 6, 7, 8, 0));
        ChocoLogging.setVerbosity(Verbosity.SOLUTION);
        solver.setPackingConstraintClass(new SatisfyDemandingSlicesHeightsSimpleBP());
        solver.setRepairMode(false);
        solver.setGlobalCostEvaluator(LoadBalancingMinMaxCost.INSTANCE);
        return solver;
    }

}
