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

package pacpma.algebra;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class representing a monomial, with variables taken from
 * 
 * @author Andrea Turrini
 *
 */
class Monomial {
    
    private static final char[] superscriptDigits = new char[] {'⁰','¹','²','³','⁴','⁵','⁶','⁷','⁸', '⁹'};

    /** We keep only one monomial with degree zero */
    private static Monomial zeroDegreeMonomial = null;

    /** Used to fix variable order for generating */
    private static Variable[] variablesMonomial = null;

    /** Mapping each variable in the monomial to its exponent */
    private final Map<Variable, Integer> exponents;

    private final String latexExpression;

    private final String mathExpression;

    private final String matlabExpression;

    private final String namedCoefficient;

    /**
     * Generates the monomial with degree zero
     */
    private Monomial() {
        exponents = new HashMap<>();

        for (Variable v : variablesMonomial) {
            exponents.put(v, 0);
        }
        latexExpression = __getLatexExpression();
        mathExpression = __getMathExpression();
        matlabExpression = __getMatlabExpression();
        namedCoefficient = __getNamedCoefficient();
    }

    private Monomial(Monomial m, Variable v) {
        exponents = new HashMap<>();
        for (Entry<Variable, Integer> entry : m.exponents.entrySet()) {
            Variable var = entry.getKey();
            Integer exp = entry.getValue() + (var.equals(v) ? 1 : 0);
            exponents.put(var, exp);
        }
        latexExpression = __getLatexExpression();
        mathExpression = __getMathExpression();
        matlabExpression = __getMatlabExpression();
        namedCoefficient = __getNamedCoefficient();
    }

    /**
     * Initialize the monomials that can be generated, by providing the variables
     * that can be used in the monomial. Successive invocations of this method are
     * ignored, even if different variables are provided.
     * 
     * @param variables
     *            the variables that can be used in the monomial
     * @return the monomial with all variables with exponent set to zero
     */
    static void initializeMonomial(List<Variable> variables) {
        if (zeroDegreeMonomial == null) {
            variablesMonomial = new Variable[variables.size()];
            int i = 0;
            for (Variable v : variables) {
                variablesMonomial[i++] = v;
            }
            zeroDegreeMonomial = new Monomial();
        }
    }

    /**
     * Get the monomial with degree zero based on the current variables, which need
     * to be set by a previous invocation of
     * {@link Monomial#initializeMonomial(Collection)}.
     * 
     * @return the zero degree monomial
     * @throws IllegalStateException
     *             if {@link Monomial#initializeMonomial(Collection)} has not been
     *             called
     */
    static Monomial getZeroDegreeMonomial() throws IllegalStateException {
        if (zeroDegreeMonomial == null) {
            throw new IllegalStateException();
        }
        return zeroDegreeMonomial;
    }

    /**
     * Increase the exponent of the variable v in the monomial m by 1.
     * 
     * @param m
     *            the monomial
     * @param v
     *            the variable whose exponent is to be increased
     * @return a new monomial with the exponent of v increased by 1
     */
    static Monomial increaseDegree(Monomial m, Variable v) {
        return new Monomial(m, v);
    }

    /**
     * Evaluates the monomial with respect to the given value of the variables.
     * 
     * @param values
     *            the values of the variables
     * @return the value of this monomial
     */
    BigDecimal evaluate(Map<Variable, BigDecimal> values) {
        assert values.keySet().containsAll(exponents.keySet());

        BigDecimal result = new BigDecimal(1.0);
        for (Entry<Variable, Integer> entry : exponents.entrySet()) {
            BigDecimal base = values.get(entry.getKey());
            int exponent = entry.getValue();
            switch (exponent) {
            case 0:
                break;
            case 1:
                result = result.multiply(base);
                break;
            default:
                result = result.multiply(base.pow(exponent));
                break;
            }
        }

        return result;
    }

    /**
     * Provides a formatted string representing the coefficient for this monomial.
     * The implementation ensures that the variables appear in the same order for
     * all monomials generated in the current execution.
     * 
     * The name has the format {@code coeff(_var_exp)*}.
     * 
     * @return the coefficient name for this monomial
     */
    public String getNamedCoefficient() {
        return namedCoefficient;
    }

    private String __getNamedCoefficient() {
        StringBuilder sb = new StringBuilder("coeff");
        for (Entry<Variable, Integer> entry : exponents.entrySet()) {
            sb.append("_")
                .append(entry.getKey().toString())
                .append("_")
                .append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Provides a LaTeX expression representing this monomial. The
     * implementation ensures that the variables appear in the same order for all
     * monomials generated in the current execution.
     * 
     * The LaTeX expression has the format {@code 1} for the monomial with
     * degree zero, and the format
     * {@code var^{exp}}({@code * var^{exp}})* for
     * the monomials with higher degree; only variables with exponent
     * {@code exp > 0} are included.
     * 
     * @return the LaTeX expression for this monomial
     */
    public String getLatexExpression() {
        return latexExpression;
    }

    private String __getLatexExpression() {
        if (this.equals(zeroDegreeMonomial)) {
            return "1";
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Entry<Variable, Integer> entry : exponents.entrySet()) {
            int exp = entry.getValue();
            if (exp > 0) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(" \\cdot ");
                }
                sb.append(entry.getKey().getName())
                    .append("^{")
                    .append(exp)
                    .append("}");
            }
        }
        return sb.toString();
    }

    /**
     * Provides a mathematical expression representing this monomial. The
     * implementation ensures that the variables appear in the same order for all
     * monomials generated in the current execution.
     * 
     * The mathematical expression has the format {@code 1} for the monomial with
     * degree zero, and the format
     * {@code var}<sup>{@code exp}</sup>({@code * var}<sup>{@code exp}</sup>)* for
     * the monomials with higher degree; only variables with exponent
     * {@code exp > 0} are included.
     * 
     * @return the mathematical expression for this monomial
     */
    public String getMathExpression() {
        return mathExpression;
    }

    private String __getMathExpression() {
        if (this.equals(zeroDegreeMonomial)) {
            return "1";
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Entry<Variable, Integer> entry : exponents.entrySet()) {
            int exp = entry.getValue();
            if (exp > 0) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(" * ");
                }
                sb.append(entry.getKey().getName());
                for (char d : Integer.toString(exp).toCharArray()) {
                    sb.append(superscriptDigits[d - '0']);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Provides a MATLAB expression representing this monomial. The
     * implementation ensures that the variables appear in the same order for all
     * monomials generated in the current execution.
     * 
     * The mathematical expression has the format {@code 1} for the monomial with
     * degree zero, and the format
     * {@code var .^ exp}({@code .* var .^ exp})* for
     * the monomials with higher degree; only variables with exponent
     * {@code exp > 0} are included.
     * 
     * @return the MATLAB expression for this monomial
     */
    public String getMatlabExpression() {
        return matlabExpression;
    }

    private String __getMatlabExpression() {
        if (this.equals(zeroDegreeMonomial)) {
            return "1";
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Entry<Variable, Integer> entry : exponents.entrySet()) {
            int exp = entry.getValue();
            if (exp > 0) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(" .* ");
                }
                sb.append('(')
                    .append(entry.getKey().getName())
                    .append(" .^ ")
                    .append(exp)
                    .append(')');
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return mathExpression;
    }

    @Override
    public int hashCode() {
        return exponents.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Monomial) {
            return exponents.equals(((Monomial) other).exponents);
        }
        return false;
    }
}
