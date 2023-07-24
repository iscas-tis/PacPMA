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

package pacpma.sample;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import pacpma.algebra.Parameter;

/**
 * Interface representing a generator of samples.
 * 
 * @author Andrea Turrini
 *
 */
public interface Sampler {

    /**
     * Generates the samples, by assigning a valid value to each of the parameters.
     * 
     * @param parameters
     *            the parameters for which generate samples
     * @return the list of generated samples
     */
    List<Map<Parameter, BigDecimal>> getSamples(List<Parameter> parameters);

}