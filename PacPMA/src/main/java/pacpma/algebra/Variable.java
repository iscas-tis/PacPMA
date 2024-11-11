/****************************************************************************

w    PacPMA - the PAC-based Parametric Model Analyzer
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing the variables to be used in the template function
 * 
 * @author Andrea Turrini
 *
 */
public class Variable {
    private static final Map<String, Variable> variables = new HashMap<>();
    private static final List<Variable> orderedVariables = new ArrayList<>();

    private final String variableName;

    private Variable(String variable) {
        this.variableName = variable;
    }

    public static void setVariables(List<Parameter> parameters) {
        for (Parameter p : parameters) {
            String variableName = p.getName();
            Variable variable = new Variable(variableName);
            variables.put(variableName, variable);
            orderedVariables.add(variable);
        }
    }
    
    public static Variable asVariable(Parameter parameter) {
        assert variables.containsKey(parameter.getName());
        return variables.get(parameter.getName());
    }

    public static List<Variable> getVariables() {
        return orderedVariables;
    }

    public String getName() {
        return variableName;
    }

    @Override
    public String toString() {
        return variableName;
    }

    @Override
    public int hashCode() {
        return variableName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Variable) {
            return variableName.equals(((Variable) other).variableName);
        }
        return false;
    }
}
