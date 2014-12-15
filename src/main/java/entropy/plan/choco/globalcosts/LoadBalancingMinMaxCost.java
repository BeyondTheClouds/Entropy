package entropy.plan.choco.globalcosts;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Node;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.choco.GlobalCostEvaluator;
import entropy.plan.choco.ReconfigurationProblem;

public class LoadBalancingMinMaxCost implements GlobalCostEvaluator {
    @SuppressWarnings("unused")
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
    .getLogger(LoadBalancingMinMaxCost.class);

    @Override
    public IntDomainVar getGlobalCost(ReconfigurationProblem model,
            ChocoCustomRP rp) {
        int max = 0;
        for (Node o : model.getNodes()) {
            if (o.getCPUCapacity() > max) {
                max = o.getCPUCapacity();
            }
        }
        IntDomainVar ret = model.createBoundIntVar("loadBalancingCost", 0, max);
        IntDomainVar maxLoad = model.max(model.getUsedCPUs());
        IntDomainVar minLoad = model.min(model.getUsedCPUs());
        System.err.println("max load : " + maxLoad);
        System.err.println("min load : " + minLoad);
        // ret = maxload-minload. Use plus untill
        // IntDomainVar ReconfigurationProblem.minus(IntDomainVar, IntDomainVar)
        // is implemented
        model.post(model.eq(model.plus(ret, minLoad), maxLoad));
        return ret;
    }

    public static final LoadBalancingMinMaxCost INSTANCE = new LoadBalancingMinMaxCost();
}
