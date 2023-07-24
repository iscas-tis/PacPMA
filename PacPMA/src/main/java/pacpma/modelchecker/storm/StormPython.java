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

package pacpma.modelchecker.storm;

import static pacpma.util.Util.appendConstant;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pacpma.algebra.Constant;
import pacpma.externaltool.ToolRunner;
import pacpma.log.Logger;
import pacpma.modelchecker.ModelChecker;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.options.OptionsPacPMA;

/**
 * Wrapper for the Storm model checker, interacting via Python.
 * 
 * @author Andrea Turrini
 *
 */
public class StormPython implements ModelChecker {
    private final static String RESULT_IDENTIFIER = "StormPython_RESULT";
    
    private final static String FIELD_SEPARATOR = ":";
    
    private String filePath = null;
    private String modelType = null;
    private String propertyFormula = null;
    private List<Constant> constants = null;
    private Map<Integer, List<Constant>> parameterValues = null;

    public StormPython() {}
    
    @Override
    public ModelChecker getInstance() {
        return new StormPython();
    }
    
    @Override
    public ModelChecker setModelFile(String filePath) {
        this.filePath = filePath;
        return this;
    }

    @Override
    public ModelChecker setModelType(String modelType) {
        this.modelType = modelType;
        return this;
    }

    @Override
    public ModelChecker setPropertyFormula(String propertyFormula) {
        this.propertyFormula = propertyFormula;
        return this;
    }

    @Override
    public ModelChecker setConstants(List<Constant> constants) {
        this.constants = constants;
        return this;
    }

    @Override
    public ModelChecker setParameterValues(Map<Integer, List<Constant>> parameterValues) {
        this.parameterValues = parameterValues;
        return this;
    }

    @Override
    public ModelChecker setOptions(List<String> options) {
        // we don't expect options, for now
        return this;
    }

    @Override
    public Map<Integer, ModelCheckerResult> check() throws IllegalStateException {
        Logger.log(Logger.LEVEL_INFO, "StormPython: starting check procedure");
        if (filePath == null) {
            throw new IllegalStateException("Model file not specified");
        }
        if (!(new File(filePath).canRead())) {
            throw new IllegalStateException("The model file " + filePath + " cannot be read");
        }
        if (modelType == null) {
            throw new IllegalStateException("Model type not specified");
        }
        if (propertyFormula == null) {
            throw new IllegalStateException("Property formula not specified");
        }

        Map<Integer, ModelCheckerResult> results = new HashMap<>();

        List<String> command = new LinkedList<>();
        List<String> messages = new LinkedList<>();
        
        String program = OptionsPacPMA.getModelCheckerPath();
        if (program == null) {
            command.add("storm-python.py");
        } else {
            command.add(program);
        }
        command.add(modelType);
        command.add(filePath);
        command.add(propertyFormula);
        
        StringBuilder sbc = new StringBuilder();
        constants.forEach(c -> appendConstant(sbc, c));
        command.add(sbc.toString());
        
        for (Integer identifier : parameterValues.keySet()) {
            StringBuilder sbp = new StringBuilder();
            sbp.append(identifier).append(FIELD_SEPARATOR);
            parameterValues.get(identifier).forEach(c -> appendConstant(sbp, c));
            Logger.log(Logger.LEVEL_DEBUG, "StormPython: sample " + sbp.toString());
            messages.add(sbp.toString());
        }
        
        Logger.log(Logger.LEVEL_INFO, "StormPython: calling actual solver");
        List<String> output = new ToolRunner(command, messages).run();
        Logger.log(Logger.LEVEL_INFO, "StormPython: calling actual solver done");
        if (output == null) {
            throw new RuntimeException("Storm failed to run");
        }
        Logger.log(Logger.LEVEL_INFO, "StormPython: extracting results");
        for (String message: output) {
            if (message.startsWith(RESULT_IDENTIFIER)) {
                String[] messageSplit = message.split(FIELD_SEPARATOR);
                String result = messageSplit[2];
                ModelCheckerResult modelCheckerResult;
                if (result.equals("inf")) {
                    modelCheckerResult = new ModelCheckerResult();
                } else {
                    modelCheckerResult = new ModelCheckerResult(new BigDecimal(result));
                }
                results.put(Integer.valueOf(messageSplit[1]), modelCheckerResult);
                Logger.log(Logger.LEVEL_DEBUG, "StormPython: result " + messageSplit[1] + ":" + result);
            }
        }
        Logger.log(Logger.LEVEL_INFO, "StormPython: extracting results done");
        if (results.size() != parameterValues.size()) {
            throw new RuntimeException("Storm failed to run");            
        }
        Logger.log(Logger.LEVEL_INFO, "StormPython: check procedure done");
        return results;
    }
}
