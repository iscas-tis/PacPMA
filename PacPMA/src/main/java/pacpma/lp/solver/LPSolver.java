/****************************************************************************

    PacPMA - the PAC-based Parametric Model Analyzer
    Copyright (C) 2023

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 *****************************************************************************/

package pacpma.lp.solver;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import pacpma.lp.ConstraintComparison;
import pacpma.lp.LPVariable;
import pacpma.lp.OptimizationDirection;
import pacpma.options.OptionsPacPMA;

/**
 * Interface representing a solver for an LP problem. A class implementing this
 * interface can impose specific a specific order on the method invocation,
 * namely first methods can only be called as
 * <ul>
 * <li>{@link #setVariables(List)}</li>
 * <li>{@link #setObjectiveFunction(OptimizationDirection, Map)}</li>
 * <li>{@link #addConstraint(Map, ConstraintComparison, double)} several
 * times</li>
 * <li>{@link #solve()}
 * </ul>
 * Calling methods in different order can cause assertion failures.
 * 
 * @author Andrea Turrini
 *
 */
/**
 * @author Andrea Turrini
 *
 */
public interface LPSolver {

    /**
     * Initializes the variables that can occur in the terms in this LP problem. All
     * variables are set as unbounded, except for {@code lambda}, the first variable
     * in {@code listOfVariables} that has {@code 0} as lower bound and
     * {@link OptionsPacPMA#getLambda()} as upper bound. Classes implementing
     * {@link LPSolver} must manage {@code lambda} by themselves.
     * 
     * @param listOfVariables
     *            the variables to be used in this LP problem
     */
    public void setVariables(List<LPVariable> listOfVariables);

    /**
     * Sets the objective function for this LP problem.
     * 
     * @param direction
     *            the optimizaton direction
     * @param terms
     *            the terms of the objective function, as variables and relative
     *            coefficients
     */
    public void setObjectiveFunction(OptimizationDirection direction, Map<LPVariable, BigDecimal> terms);

    /**
     * Adds a constraint to this LP problem.
     * 
     * @param terms
     *            the terms of the constraints, as variables and relative
     *            coefficients
     * @param comparison
     *            the comparison direction
     * @param bound
     *            the boundary value
     */
    public void addConstraint(Map<LPVariable, BigDecimal> terms, ConstraintComparison comparison, BigDecimal bound);

    /**
     * Solves the LP problem as defined by the previous calls of
     * {@link #setObjectiveFunction(OptimizationDirection, Map)} and
     * {@link #addConstraint(Map, ConstraintComparison, double)}.
     * 
     * @return the value for each variable, or {@code null} if the problem is not
     *         satisfiable
     */
    public Map<LPVariable, BigDecimal> solve();
    
    
    /**
     * Provides the value of the computed {@code lambda} variable.
     * 
     * @return the value of the {@code lambda} variable as computed by
     *         {@link #solve()}, or {@code null} if the problem was not satisfiable
     */
    public BigDecimal getLambdaValue();
}
