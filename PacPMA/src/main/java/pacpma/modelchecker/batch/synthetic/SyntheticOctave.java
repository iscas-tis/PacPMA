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

package pacpma.modelchecker.batch.synthetic;

import static pacpma.util.Util.appendConstant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pacpma.algebra.Constant;
import pacpma.externaltool.ToolRunner;
import pacpma.log.LogEngine;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.batch.BatchModelChecker;
import pacpma.options.OptionsPacPMA;

/**
 * Wrapper for Octave acting as a synthetic solver.
 * 
 * @author Andrea Turrini
 *
 */
public class SyntheticOctave implements BatchModelChecker {
    private final static LogEngine logEngine = OptionsPacPMA.getLogEngineInstance();
    private final static String RESULT = "RESULT";
    
    private final static String FUN_PARS = "FUN_PARS";
    private final static String FUN_NAME = "FUN_NAME";
    private final static String FUNCTION = "fra=@(p,q)(p.^3 + q .* p.^2 - p.^2 - 2 * q .* p + p + q)./(p+ q);"
            + "fda=@(p,q)(-2*(q.^2).*p + 2*q.^2 + p)./(p + q);"
            + FUN_NAME + "=@(" + FUN_PARS + ")(fra(p,q)-fda(p,q));";
    
    private Map<Integer, List<Constant>> parameterValues = null;

    public SyntheticOctave() {}
    
    @Override
    public BatchModelChecker setParameterValues(Map<Integer, List<Constant>> parameterValues) {
        this.parameterValues = parameterValues;
        return this;
    }

    @Override
    public Map<Integer, ModelCheckerResult> check() throws IllegalStateException {
        logEngine.log(LogEngine.LEVEL_INFO, "SyntheticOctave: starting check procedure");

        Map<Integer, ModelCheckerResult> results = new HashMap<>();
        
        for (Integer identifier : parameterValues.keySet()) {
            List<Constant> singleParameters = parameterValues.get(identifier);
            StringBuilder listOfParameters = new StringBuilder();
            for (Constant c : singleParameters) {
                if (!listOfParameters.isEmpty()) {
                    listOfParameters.append(',');
                }
                listOfParameters.append(c.getName());
            }
            
            File functionEvaluationTempFile = null;
            try {
                functionEvaluationTempFile = File.createTempFile("functionEvaluation", ".octave");
                functionEvaluationTempFile.deleteOnExit();
            } catch (Exception ioe) {
                return null;
            }

            List<String> command = new LinkedList<>();
            String program = OptionsPacPMA.getModelCheckerPath();
            if (program == null) {
                command.add("octave");
            } else {
                command.add(program);
            }
            command.add("--no-gui");
            command.add("--no-history");
            command.add("--no-window-system");
            command.add("--silent");
            command.add("--no-line-editing");
            command.add(functionEvaluationTempFile.getAbsolutePath());

            StringBuilder sb = new StringBuilder();
            singleParameters.forEach(p -> appendConstant(sb,p));
            
            logEngine.log(LogEngine.LEVEL_DEBUG, "SyntheticOctave: sample " + sb.toString());
            
            StringBuilder functionEvaluation = new StringBuilder();
            singleParameters.forEach(p -> 
                {
                    functionEvaluation
                        .append(p.getName())
                        .append('=')
                        .append(p.getValue())
                        .append(';');
                }
            );
            
            functionEvaluation
                .append(FUNCTION.replace(FUN_PARS, listOfParameters))
                .append("fprintf('")
                .append(RESULT)
                .append(":%.20f\\n', ")
                .append(FUN_NAME)
                .append('(')
                .append(listOfParameters)
                .append("));");
            
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(functionEvaluationTempFile))) {
                bw.write(functionEvaluation.toString());
            } catch (IOException ioe) {
                return null;
            }

            logEngine.log(LogEngine.LEVEL_INFO, "SyntheticOctave: calling actual solver");
            ToolRunner toolRunner = new ToolRunner(command); 
            List<String> output = toolRunner.run();
            
            functionEvaluationTempFile.delete();
            
            logEngine.log(LogEngine.LEVEL_INFO, "SyntheticOctave: calling actual solver done");
            logEngine.log(LogEngine.LEVEL_INFO, "SyntheticOctave: exit value: " + toolRunner.getExitValue());
            if (output == null) {
                throw new RuntimeException("no output returned; exit value: " + toolRunner.getExitValue());
            }
            logEngine.log(LogEngine.LEVEL_INFO, "SyntheticOctave: extracting result");
            boolean hasFailed = true;
            for (String message : output) {
                logEngine.log(LogEngine.LEVEL_DEBUG, "SyntheticOctave: raw result: " + message);
                if (message.startsWith(RESULT)) {
                    results.put(identifier, new ModelCheckerResult(new BigDecimal(message.split(":")[1])));
                    hasFailed = false;
                }
            }
            if (hasFailed) {
                throw new RuntimeException("Failed execution; raw output:\n" + output);            
            }
            logEngine.log(LogEngine.LEVEL_INFO, "SyntheticOctave: extracting result done");
        }
        logEngine.log(LogEngine.LEVEL_INFO, "SyntheticOctave: check procedure done");
        return results;
    }
}
