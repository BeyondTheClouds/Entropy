/*
 * Copyright (c) Fabien Hermenier
 *
 * This file is part of Entropy.
 *
 * Entropy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Entropy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */
package entropy.plan.choco;

import java.util.*;

import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.constraints.integer.IntExp;
import choco.kernel.solver.search.ISolutionPool;
import choco.kernel.solver.search.SolutionPoolFactory;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.*;
import entropy.plan.choco.actionModel.ActionModel;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.plan.choco.actionModel.slice.Slice;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSliceHeights;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsSimpleBP;
import entropy.plan.choco.constraint.sliceScheduling.SlicesPlanner;
import entropy.plan.choco.globalcosts.PackingCost;
import entropy.plan.durationEvaluator.DurationEvaluationException;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;

/**
 * A CustomizablePlannerModule based on Choco.
 * 
 * @author Fabien Hermenier
 */
public class ChocoCustomRP extends CustomizablePlannerModule {

    public List<SConstraint> costConstraints;

    /**
     * The model.
     */
    private ReconfigurationProblem model;

    private boolean repair = true;

    private List<VJob> queue;

    /**
     * Make a new plan module.
     * 
     * @param eval to evaluate the duration of the actions.
     */
    public ChocoCustomRP(DurationEvaluator eval) {
        super(eval);
        costConstraints = new LinkedList<SConstraint>();
    }

    /**
     * Get the model.
     * 
     * @return the model to express constraints.
     */
    public ReconfigurationProblem getModel() {
        return model;
    }

    @Override
    public List<SolutionStatistics> getSolutionsStatistics() {
        if (model == null) {
            return new ArrayList<SolutionStatistics>();
        }
        return model.getSolutionsStatistics();
    }

    /**
     * @return some statistics about the solving process
     */
    @Override
    public SolvingStatistics getSolvingStatistics() {
        if (model == null) {
            return SolvingStatistics.getStatisticsForNotSolvingProcess();
        }
        return model.getSolvingStatistics();
    }

    /**
     * the class to instantiate to generate the global constraint. Default is
     * SatisfyDemandingSlicesHeightsSimpleBP.
     */
    private SatisfyDemandingSliceHeights packingConstraintClass = new SatisfyDemandingSlicesHeightsSimpleBP();

    /**
     * @return the globalConstraintClass
     */
    public SatisfyDemandingSliceHeights getPackingConstraintClass() {
        return packingConstraintClass;
    }

    /**
     * @param c the globalConstraintClass to set
     */
    public void setPackingConstraintClass(SatisfyDemandingSliceHeights c) {
        packingConstraintClass = c;
    }

    @Override
    public TimedReconfigurationPlan compute(Configuration src,
            ManagedElementSet<VirtualMachine> run,
            ManagedElementSet<VirtualMachine> wait,
            ManagedElementSet<VirtualMachine> sleep,
            ManagedElementSet<VirtualMachine> stop, ManagedElementSet<Node> on,
            ManagedElementSet<Node> off, List<VJob> q) throws PlanException {

        long st = System.currentTimeMillis();
        queue = q;

        model = null;

        ManagedElementSet<VirtualMachine> vms;
        if (repair) {
            // Look for the VMs to consider
            vms = new SimpleManagedElementSet<VirtualMachine>();
            for (VJob v : queue) {
                for (PlacementConstraint c : v.getConstraints()) {
                    if (!c.isSatisfied(src)) {
                        vms.addAll(c.getMisPlaced(src));
                    }
                }
            }
            // Hardcore way for the packing. TODO: externalize
            // System.err.println("pack issue:" +
            // src.getRunnings(src.getUnacceptableNodes()));
            vms.addAll(src.getRunnings(Configurations
                    .futureOverloadedNodes(src)));
        } else {
            vms = src.getAllVirtualMachines();
        }

        model = new DefaultReconfigurationProblem(src, run, wait, sleep, stop,
                vms, on, off, getDurationEvaluator());

        packingConstraintClass.add(model);
        new SlicesPlanner().add(model);
        Map<Class<?>, Integer> occurences = new HashMap<Class<?>, Integer>();
        int nbConstraints = 0;

        // We first translate and inject absolute constraints as they directly
        // restrict the placement.
        // So the domain of the VMs will be already reduced for the relative
        // constraints
        List<PlacementConstraint> relatives = new ArrayList<PlacementConstraint>();
        for (VJob vjob : queue) {
            for (PlacementConstraint c : vjob.getConstraints()) {
                try {
                    if (c.getType() == PlacementConstraint.Type.absolute) {
                        c.inject(model);
                    } else {
                        relatives.add(c);
                    }

                    if (!occurences.containsKey(c.getClass())) {
                        occurences.put(c.getClass(), 0);
                    }
                    nbConstraints++;
                    occurences.put(c.getClass(),
                            1 + occurences.get(c.getClass()));
                } catch (Exception e) {
                    Plan.logger.error(e.getMessage(), e);
                }
            }
        }

        for (PlacementConstraint c : relatives) {
            c.inject(model);
        }
        /*
         * A pretty print of the problem
         */
        // The elements
        Plan.logger.debug(run.size() + wait.size() + sleep.size() + stop.size()
                + " VMs: " + run.size() + " will run; " + wait.size()
                + " will wait; " + sleep.size() + " will sleep; " + stop.size()
                + " will be stopped");
        Plan.logger.debug(on.size() + off.size() + " nodes: " + on.size()
                + " to run; " + off.size() + " to halt");
        Plan.logger.debug("Manage " + vms.size() + " VMs ("
                + (repair ? "repair" : "rebuild") + ")");
        if (getTimeLimit() > 0) {
            Plan.logger.debug("Timeout is " + getTimeLimit() + " seconds");
        } else {
            Plan.logger.debug("No timeout!");
        }

        // The constraints
        StringBuilder b = new StringBuilder();
        if (nbConstraints > 0) {
            b.append(nbConstraints).append(" constraints: ");
            for (Map.Entry<Class<?>, Integer> e : occurences.entrySet()) {
                b.append(e.getValue()).append(" ")
                .append(e.getKey().getSimpleName()).append("; ");
            }

        } else {
            b.append("No constraints");
        }
        Plan.logger.debug(b.toString());
        /**
         * globalCost is equals to the sum of each action costs.
         */
        IntDomainVar globalCost = getGlobalCost(getModel(), this);
        // System.err.println("global cost : " + globalCost);
        // SConstraint<?> cs = model.leq(model.getEnd(), globalCost);
        // model.post(cs);

        try {
            setTotalDurationBounds(globalCost, vms);
        } catch (DurationEvaluationException e) {
            throw new PlanException(e.getMessage(), e);
        }
        updateUB();

        // TODO: Set the LB for the horizon && the end of each action
        // cs = model.leq(model.getEnd(), explodedSum(model,
        // ActionModels.extractDurations(allActions), 200, true));
        // costConstraints.add(cs);
        // model.post(cs);

        if (getTimeLimit() > 0) {
            model.setTimeLimit(getTimeLimit() * 1000);
        }
        new BasicPlacementHeuristic2(globalCost, packingConstraintClass, vms)
        .add(this);
        new DummyPlacementHeuristic().add(model);
        model.setDoMaximize(false);
        model.setObjective(globalCost);
        model.setRestart(false);
        model.setFirstSolution(EntropyProperties.isFirstSolutionEnabled());
        model.generateSearchStrategy();
        ISolutionPool sp = SolutionPoolFactory.makeInfiniteSolutionPool(model
                .getSearchStrategy());
        model.getSearchStrategy().setSolutionPool(sp);

        long ed = System.currentTimeMillis();
        generationTime = ed - st;
        logger.debug(generationTime + "ms to build the solver "
                + model.getNbIntConstraints() + " cstr " + model.getNbIntVars()
                + "+" + model.getNbBooleanVars() + " variables "
                + model.getNbConstants() + " cte");

        model.launch();
        Boolean ret = model.isFeasible();
        if (ret == null) {
            throw new PlanException(
                    "Unable to check wether a solution exists or not");
        } else {
            Plan.logger.debug("#nodes= " + model.getNodeCount()
                    + ", #backtracks= " + model.getBackTrackCount()
                    + ", #duration= " + model.getTimeCount() + ", #nbsol= "
                    + model.getNbSolutions());

            if (Boolean.FALSE.equals(ret)) {
                throw new PlanException("No solution");
            } else {
                TimedReconfigurationPlan plan = model.extractSolution();
                Configuration res = plan.getDestination();
                if (!Configurations.futureOverloadedNodes(res).isEmpty()) {
                    throw new PlanException(
                            "Resulting configuration is not viable: Overloaded nodes="
                                    + Configurations.futureOverloadedNodes(res));
                }

                if (model.getEnd().getVal() != plan.getDuration()) {
                    throw new PlanException("Practical duration("
                            + plan.getDuration() + ") and theoretical ("
                            + model.getEnd().getVal() + ") missmatch:\n" + plan);
                }
                for (VJob vjob : queue) {
                    for (PlacementConstraint c : vjob.getConstraints()) {
                        if (!c.isSatisfied(res)) {
                            throw new PlanException(
                                    "Resulting configuration does not satisfy '"
                                            + c.toString() + "'");
                        }
                    }
                }
                return plan;
            }
        }
    }

    /**
     * Estimate the lower and the upper bound of model.getEnd()
     * 
     * @param totalDuration the totalDuration of all the action
     * @throws entropy.plan.durationEvaluator.DurationEvaluationException if an error occured during
     *         evaluation of the durations.
     */
    private void setTotalDurationBounds(IntDomainVar totalDuration,
            ManagedElementSet<VirtualMachine> vms)
                    throws DurationEvaluationException {
        int sup = ReconfigurationProblem.MAX_TIME;
        int min = 0;
        try {
            model.getEnd().setInf(min);
            model.getEnd().setSup(sup);
            totalDuration.setInf(min);
            totalDuration.setSup(sup);
        } catch (Exception e) {
            Plan.logger.warn(e.getMessage(), e);
        }
        Plan.logger.debug(totalDuration.pretty());
        Plan.logger.debug(model.getEnd().pretty());
    }

    /**
     * Update the upper bounds of all the variable to simplify the problem.
     */
    private void updateUB() {
        int ub = model.getEnd().getSup();
        List<ActionModel> allActionModels = new LinkedList<ActionModel>(
                model.getNodeMachineActions());
        allActionModels.addAll(model.getVirtualMachineActions());

        try {
            for (VirtualMachineActionModel a : model.getVirtualMachineActions()) {
                if (a.end().getSup() > ub) {
                    a.end().setSup(ub);
                }
                if (a.start().getSup() > ub) {
                    a.start().setSup(ub);
                }

                if (a.getGlobalCost().getSup() > ub) {
                    a.getGlobalCost().setSup(ub);
                }

                Slice task = a.getDemandingSlice();
                if (task != null) {
                    if (task.end().getSup() > ub) {
                        task.end().setSup(ub);
                    }
                    if (task.start().getSup() > ub) {
                        task.start().setSup(ub);
                    }
                    if (task.duration().getSup() > ub) {
                        task.duration().setSup(ub);
                    }
                }

                task = a.getConsumingSlice();
                if (task != null) {
                    if (task.end().getSup() > ub) {
                        task.end().setSup(ub);
                    }
                    if (task.start().getSup() > ub) {
                        task.start().setSup(ub);
                    }
                    if (task.duration().getSup() > ub) {
                        task.duration().setSup(ub);
                    }
                }
            }
        } catch (Exception e) {
            Plan.logger.warn(e.getMessage(), e);
        }
    }

    /**
     * Get all the vjobs managed by the module
     * 
     * @return a list of vjobs, may be empty
     */
    public List<VJob> getQueue() {
        return queue;
    }

    /**
     * Use the repair mode.
     * 
     * @param b {@code true} to use the repair mode
     */
    public void setRepairMode(boolean b) {
        repair = b;
    }

    /**
     * Make a sum of a large number of variables using decomposition
     * 
     * @param m the model
     * @param vars the variables to sum
     * @param step the size of the subsums.
     * @return the variable storing the result of the sum.
     */
    public IntExp explodedSum(ReconfigurationProblem m, IntDomainVar[] vars,
            int step, boolean post) {
        int s = vars.length > step ? step : vars.length;
        IntDomainVar[] subSum = new IntDomainVar[s];
        int nbSubs = (int) Math.ceil(vars.length / step);
        if (vars.length % step != 0) {
            nbSubs++;
        }
        IntDomainVar[] ress = new IntDomainVar[nbSubs];

        int curRes = 0;
        int shiftedX = 0;
        for (int i = 0; i < vars.length; i++) {
            subSum[shiftedX++] = vars[i];
            if (shiftedX == subSum.length) {
                IntDomainVar subRes = m.createBoundIntVar("subSum["
                        + (i - shiftedX + 1) + ".." + i + "]", 0,
                        ReconfigurationProblem.MAX_TIME);
                SConstraint c = m.eq(subRes, m.sum(subSum));
                if (post) {
                    m.post(c);
                } else {
                    costConstraints.add(c);
                }
                ress[curRes++] = subRes;
                if (i != vars.length - 1) {
                    int remainder = vars.length - (i + 1);
                    s = remainder > step ? step : remainder;
                    subSum = new IntDomainVar[s];
                }
                shiftedX = 0;
            }
        }
        return m.sum(ress);
    }

    public List<SConstraint> getCostConstraints() {
        return costConstraints;
    }

    GlobalCostEvaluator globalCostEvaluator = null;

    /**
     * @return the globalCostEvaluator
     */
    public GlobalCostEvaluator getGlobalCostEvaluator() {
        return globalCostEvaluator;
    }

    /**
     * @param globalCostEvaluator the globalCostEvaluator to set
     */
    public void setGlobalCostEvaluator(GlobalCostEvaluator globalCostEvaluator) {
        this.globalCostEvaluator = globalCostEvaluator;
    }

    public IntDomainVar getGlobalCost(ReconfigurationProblem model,
            ChocoCustomRP rp) {
        if (globalCostEvaluator == null) {
            return PackingCost.INSTANCE.getGlobalCost(model, rp);
        } else {
            return globalCostEvaluator.getGlobalCost(model, rp);
        }
    }
}
