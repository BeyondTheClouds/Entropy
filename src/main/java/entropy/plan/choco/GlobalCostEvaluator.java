package entropy.plan.choco;

import choco.kernel.solver.variables.integer.IntDomainVar;

/**
 * generates a global cost from a model, that is, a formal representation of the cost of a model.
 */
public interface GlobalCostEvaluator {

    /**
     * produces a formal expression of the global cost of a model
     * 
     * @param model the model to produce the cost of
     * @param rp the solver to add constraints into
     * @return the expression of the added formal cost of the model
     */
    public IntDomainVar getGlobalCost(ReconfigurationProblem model,
            ChocoCustomRP rp);
}
