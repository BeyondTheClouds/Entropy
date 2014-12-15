package entropy.plan.choco.globalcosts;

import java.util.ArrayList;
import java.util.List;

import choco.Choco;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.choco.GlobalCostEvaluator;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ActionModel;
import entropy.plan.choco.actionModel.ActionModels;

public class PackingCost implements GlobalCostEvaluator {
    @SuppressWarnings("unused")
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
    .getLogger(PackingCost.class);

    /** singleton instance (immutable object) */
    public static final PackingCost INSTANCE = new PackingCost();

    @Override
    public IntDomainVar getGlobalCost(ReconfigurationProblem model,
            ChocoCustomRP rp) {
        IntDomainVar globalCost = model.createBoundIntVar("globalCost", 0,
                Choco.MAX_UPPER_BOUND);
        List<ActionModel> allActions = new ArrayList<ActionModel>();
        allActions.addAll(model.getVirtualMachineActions());
        allActions.addAll(model.getNodeMachineActions());
        IntDomainVar[] allCosts = ActionModels.extractCosts(allActions);
        List<IntDomainVar> varCosts = new ArrayList<IntDomainVar>();
        for (IntDomainVar c : allCosts) {
            if (c.isInstantiated() && c.getVal() == 0) {
            } else {
                varCosts.add(c);
            }
        }
        IntDomainVar[] costs = varCosts.toArray(new IntDomainVar[varCosts
                                                                 .size()]);
        // model.post(model.eq(globalCost, /*model.sum(costs)*/explodedSum(model,
        // costs, 200, true)));
        SConstraint<?> cs = model.eq(globalCost,
                rp.explodedSum(model, costs, 100, false));
        rp.costConstraints.add(cs);
        // model.post(cs);
        return globalCost;
    }
}
