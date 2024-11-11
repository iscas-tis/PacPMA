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

package pacpma.algebra.polynomial;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pacpma.algebra.TemplateFunction;
import pacpma.algebra.Variable;

/**
 * A class representing a polynomial.
 * 
 * @author Andrea Turrini
 *
 */
public class Polynomial implements TemplateFunction {
    private final int maximumDegree;
    private final Map<Integer, Collection<Monomial>> monomials;

    /**
     * Generates a complete polynomial over the variables defined in
     * {@link Variable}, where the monomials in the polynomial have degree at most
     * {@code maximumDegree}.
     * 
     * @param maximumDegree
     *            the maximum degree of this polynomial
     */
    public Polynomial(int maximumDegree) {
        this.maximumDegree = maximumDegree;
        this.monomials = new HashMap<>();
        
        Monomial.initializeMonomial(Variable.getVariables());

        Collection<Monomial> cm = new HashSet<Monomial>();
        cm.add(Monomial.getZeroDegreeMonomial());
        monomials.put(0, cm);

        for (int currentDegree = 1; currentDegree <= maximumDegree; currentDegree++) {
            Collection<Monomial> currentDegreeMonomials = new HashSet<>();
            Collection<Monomial> previousDegreeMonomials = monomials.get(currentDegree - 1);
            for (Variable v : Variable.getVariables()) {
                for (Monomial m : previousDegreeMonomials) {
                    currentDegreeMonomials.add(Monomial.increaseDegree(m, v));
                }
            }
            monomials.put(currentDegree, currentDegreeMonomials);
        }
    }
    
    @Override
    public List<String> getCoefficients() {
        List<String> coefficients = new LinkedList<>();
        monomials.values().forEach(
                mons -> mons.forEach(
                        monomial -> coefficients.add(monomial.getNamedCoefficient())
                )
        );
        return coefficients;
    }

    @Override
    public Map<String, BigDecimal> evaluate(Map<Variable, BigDecimal> values) {
        Map<String, BigDecimal> coefficients = new HashMap<>();
        monomials.values().forEach(
                mons -> mons.forEach(
                        monomial -> coefficients.put(monomial.getNamedCoefficient(), monomial.evaluate(values))
                )
        );
        return coefficients;
    }
    
    @Override
    public String getLatexExpression(Map<String, BigDecimal> coefficientValues) {
        StringBuilder sb = new StringBuilder();
        sb.append(coefficientValues.get(Monomial.getZeroDegreeMonomial().getNamedCoefficient()));
        for (int degree = 1; degree <= maximumDegree; degree++) {
            for (Monomial monomial : monomials.get(degree)) {
                BigDecimal value = coefficientValues.get(monomial.getNamedCoefficient());
                assert value != null;
                if (value.compareTo(BigDecimal.ZERO) >= 0) {
                    sb.append(" + ");
                } else {
                    sb.append(" - ");
                }
                sb.append(value.abs().toPlainString()).append(" \\cdot ").append(monomial.getLatexExpression());
            }
        }
        return sb.toString();
    }
    
    @Override
    public String getMathExpression(Map<String, BigDecimal> coefficientValues) {
        StringBuilder sb = new StringBuilder();
        sb.append(coefficientValues.get(Monomial.getZeroDegreeMonomial().getNamedCoefficient()));
        for (int degree = 1; degree <= maximumDegree; degree++) {
            for (Monomial monomial : monomials.get(degree)) {
                BigDecimal value = coefficientValues.get(monomial.getNamedCoefficient());
                assert value != null;
                if (value.compareTo(BigDecimal.ZERO) >= 0) {
                    sb.append(" + ");
                } else {
                    sb.append(" - ");
                }
                sb.append(value.abs().toPlainString()).append(" * ").append(monomial.getMathExpression());
            }
        }
        return sb.toString();
    }
    
    @Override
    public String getMatlabExpression(Map<String, BigDecimal> coefficientValues) {
        StringBuilder sb = new StringBuilder();
        sb.append(coefficientValues.get(Monomial.getZeroDegreeMonomial().getNamedCoefficient()));
        for (int degree = 1; degree <= maximumDegree; degree++) {
            for (Monomial monomial : monomials.get(degree)) {
                BigDecimal value = coefficientValues.get(monomial.getNamedCoefficient());
                assert value != null;
                if (value.compareTo(BigDecimal.ZERO) >= 0) {
                    sb.append(" + ");
                } else {
                    sb.append(" - ");
                }
                sb.append(value.abs().toPlainString()).append(" * ").append(monomial.getMatlabExpression());
            }
        }
        return sb.toString();
    }
}
