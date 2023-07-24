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

/**
 * A constant, as given from command line. The value for this constant is
 * expected to be a {@link String String}, to be simply forwarded to the solver.
 * 
 * @author Andrea Turrini
 *
 */
public class Constant {
    private final String name;
    private final String value;

    public Constant(String name, String value) {
        assert name != null;
        assert name.length() > 0;
        assert value != null;
        assert value.length() > 0;

        this.name = name;
        this.value = value;
    }

    /**
     * @return the name of this parameter
     */
    public String getName() {
        return name;
    }

    /**
     * @return the value for this parameter
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
