/****************************************************************************

    PACModel - a PAC-based model checker
    Copyright (C) 2024

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

package pacpma.algebra.function;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import pacpma.algebra.TemplateFunction;
import pacpma.algebra.Variable;
import pacpma.algebra.function.parser.ExpressionParser;

/**
 * @author Andrea Turrini
 *
 */
public class ExpressionFunction implements TemplateFunction {
    private List<String> terms;
    
    /**
     * Parses the expressions in the list as function terms.
     * @param list
     *  the list of expressions for this expression function
     */
    public ExpressionFunction(String termsList) {
        ExpressionParser parser = new ExpressionParser(termsList); 
        terms = parser.parseTerms();
    }

    @Override
    public boolean isValid() {
        return terms != null;
    }
    
    @Override
    public List<String> getCoefficients() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, BigDecimal> evaluate(Map<Variable, BigDecimal> values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLatexExpression(Map<String, BigDecimal> coefficientValues) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMathExpression(Map<String, BigDecimal> coefficientValues) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMatlabExpression(Map<String, BigDecimal> coefficientValues) {
        // TODO Auto-generated method stub
        return null;
    }
}
