#include <iostream>
#include <string>

#include <boost/algorithm/string.hpp>

#include <l3pp.h>

#include "storm-config.h"
#include "storm/adapters/RationalFunctionAdapter.h"
#include "storm/api/storm.h"
#include "storm/environment/Environment.h"
#include "storm/environment/solver/MinMaxSolverEnvironment.h"
#include "storm/environment/solver/NativeSolverEnvironment.h"
#include "storm/environment/solver/TopologicalSolverEnvironment.h"
#include "storm/modelchecker/results/ExplicitQuantitativeCheckResult.h"
#include "storm/models/sparse/Ctmc.h"
#include "storm/models/sparse/Dtmc.h"
#include "storm/models/sparse/Mdp.h"
#include "storm/models/sparse/Model.h"
#include "storm/settings/modules/GeneralSettings.h"
#include "storm/utility/initialize.h"
#include "storm/utility/constants.h"
#include "storm-pars/utility/ModelInstantiator.h"
#include "storm-parsers/api/storm-parsers.h"
#include "storm/storage/jani/Property.h"


std::map<std::string, std::string> splitParameters(std::string input_string) {
    std::map<std::string, std::string> mapping;

    std::vector<std::string> split_strings;
    boost::split(split_strings, input_string, boost::is_any_of(","));

    for (const std::string & single_string : split_strings) {
        std::vector<std::string> pair_strings;
        boost::split(pair_strings, single_string, boost::is_any_of("="));
        mapping.insert(std::pair<std::string, std::string>(pair_strings[0], pair_strings[1]));
    }

    return mapping;
} 

std::map<storm::RationalFunctionVariable, storm::RationalFunctionCoefficient> getParameterValues(std::string parameters, std::set<storm::RationalFunctionVariable> variables) {
    std::map<storm::RationalFunctionVariable, storm::RationalFunctionCoefficient> valuation;

    std::map<std::string, std::string> single_parameters = splitParameters(parameters);
    for(const carl::Variable variable : variables) {
        valuation.insert(std::make_pair(variable, storm::utility::convertNumber<storm::RationalFunctionCoefficient>(single_parameters.at(variable.name()))));
    }
    return valuation;
}

void applyMethod(storm::Environment &env, std::string method) {
    if (method == "ABOVI") {
        env.solver().native().setMethod(storm::solver::NativeLinearEquationSolverMethod::AdaptiveBayesianOptimizationValueIteration);
        env.solver().minMax().setMethod(storm::solver::MinMaxMethod::AdaptiveBayesianOptimizationValueIteration);
        env.solver().topological().setUnderlyingMinMaxMethod(storm::solver::MinMaxMethod::AdaptiveBayesianOptimizationValueIteration);
    } else {
        env.solver().native().setMethod(storm::solver::NativeLinearEquationSolverMethod::Jacobi);
        env.solver().minMax().setMethod(storm::solver::MinMaxMethod::Topological);
        env.solver().topological().setUnderlyingMinMaxMethod(storm::solver::MinMaxMethod::ValueIteration);
    }
}

void applyOptions(storm::Environment &env, std::string values) {

    std::vector<std::string> optionValues;
    boost::split(optionValues, values, boost::is_any_of(","));
    for (const std::string & opValue : optionValues) {
        std::vector<std::string> pair;
        boost::split(pair, opValue, boost::is_any_of("="));
        if (pair[0] == "MAX-ITER") {
            std::cout << "MAX-ITER old value (native): " << env.solver().native().getMaximalNumberOfIterations() << "\nMAX-ITER old value (min-max):" << env.solver().minMax().getMaximalNumberOfIterations() << "\n";
            env.solver().native().setMaximalNumberOfIterations(std::stoull(pair[1]));
            env.solver().minMax().setMaximalNumberOfIterations(std::stoull(pair[1]));
        } else if (pair[0] == "ABOVI-EFFECTIVE-TOLERANCE") {
            env.solver().native().setABOVIEffectiveTolerance(storm::utility::convertNumber<storm::RationalNumber>(pair[1]));
            env.solver().minMax().setABOVIEffectiveTolerance(storm::utility::convertNumber<storm::RationalNumber>(pair[1]));
        } else if (pair[0] == "ABOVI-PRINT-ESTIMATED-ERROR") {
            env.solver().native().setABOVIPrintEstimatedError("true" == pair[1]);
            env.solver().minMax().setABOVIPrintEstimatedError("true" == pair[1]);
        }
    }
}

void checkCtmc(std::shared_ptr<storm::models::sparse::Ctmc<storm::RationalFunction>> ctmc, std::shared_ptr<storm::logic::Formula const> formula, storm::Environment &env) {
    storm::utility::ModelInstantiator<storm::models::sparse::Ctmc<storm::RationalFunction>, storm::models::sparse::Ctmc<double>> modelInstantiator(*ctmc);

    std::set<storm::RationalFunctionVariable> variables = storm::models::sparse::getAllParameters(*ctmc);

    std::string line;
    while (std::getline(std::cin, line)) {
        if ("EOF" == line) {
            break;
        }
        
        std::vector<std::string> instanceValues;
        boost::split(instanceValues, line, boost::is_any_of(":"));

        if (instanceValues[0] == "OPTIONS") {
            applyOptions(env, instanceValues[1]);
        } else if (instanceValues[0] == "METHOD") {
            applyMethod(env, instanceValues[1]);
        } else {
            std::map<carl::Variable, storm::RationalFunctionCoefficient> parameterValues = getParameterValues(instanceValues[1], variables);

            storm::models::sparse::Ctmc<double> const& concrete_model(modelInstantiator.instantiate(parameterValues));

            storm::modelchecker::SparseCtmcCslModelChecker<storm::models::sparse::Ctmc<double>> checker(concrete_model);
                
            std::unique_ptr<storm::modelchecker::CheckResult> checkerResult = checker.check(env, *formula);
                
            storm::modelchecker::ExplicitQuantitativeCheckResult<double>& quantitativeResult = checkerResult->asExplicitQuantitativeCheckResult<double>();
                
            std::cout << "StormCWrapper_RESULT:" << instanceValues[0] << ":" << quantitativeResult[*concrete_model.getInitialStates().begin()] << "\n";
        }
    }
}

void checkDtmc(std::shared_ptr<storm::models::sparse::Dtmc<storm::RationalFunction>> dtmc, std::shared_ptr<storm::logic::Formula const> formula, storm::Environment &env) {
    storm::utility::ModelInstantiator<storm::models::sparse::Dtmc<storm::RationalFunction>, storm::models::sparse::Dtmc<double>> modelInstantiator(*dtmc);

    std::set<storm::RationalFunctionVariable> variables = storm::models::sparse::getAllParameters(*dtmc);

    std::string line;
    while (std::getline(std::cin, line)) {
        if ("EOF" == line) {
            break;
        }
        
        std::vector<std::string> instanceValues;
        boost::split(instanceValues, line, boost::is_any_of(":"));

        if (instanceValues[0] == "OPTIONS") {
            applyOptions(env, instanceValues[1]);
        } else if (instanceValues[0] == "METHOD") {
            applyMethod(env, instanceValues[1]);
        } else {
            std::map<carl::Variable, storm::RationalFunctionCoefficient> parameterValues = getParameterValues(instanceValues[1], variables);

            storm::models::sparse::Dtmc<double> const& concrete_model(modelInstantiator.instantiate(parameterValues));

            storm::modelchecker::SparseDtmcPrctlModelChecker<storm::models::sparse::Dtmc<double>> checker(concrete_model);
                
            std::unique_ptr<storm::modelchecker::CheckResult> checkerResult = checker.check(env, *formula);
                
            storm::modelchecker::ExplicitQuantitativeCheckResult<double>& quantitativeResult = checkerResult->asExplicitQuantitativeCheckResult<double>();
                
            std::cout << "StormCWrapper_RESULT:" << instanceValues[0] << ":" << quantitativeResult[*concrete_model.getInitialStates().begin()] << "\n";
        }
    }
}

void checkMdp(std::shared_ptr<storm::models::sparse::Mdp<storm::RationalFunction>> mdp, std::shared_ptr<storm::logic::Formula const> formula, storm::Environment &env) {
    storm::utility::ModelInstantiator<storm::models::sparse::Mdp<storm::RationalFunction>, storm::models::sparse::Mdp<double>> modelInstantiator(*mdp);

    std::set<storm::RationalFunctionVariable> variables = storm::models::sparse::getAllParameters(*mdp);

    std::string line;
    while (std::getline(std::cin, line)) {
        if ("EOF" == line) {
            break;
        }
        
        std::vector<std::string> instanceValues;
        boost::split(instanceValues, line, boost::is_any_of(":"));

        if (instanceValues[0] == "OPTIONS") {
            applyOptions(env, instanceValues[1]);
        } else if (instanceValues[0] == "METHOD") {
            applyMethod(env, instanceValues[1]);
        } else {
            std::map<carl::Variable, storm::RationalFunctionCoefficient> parameterValues = getParameterValues(instanceValues[1], variables);

            storm::models::sparse::Mdp<double> const& concrete_model(modelInstantiator.instantiate(parameterValues));

            storm::modelchecker::SparseMdpPrctlModelChecker<storm::models::sparse::Mdp<double>> checker(concrete_model);
                
            std::unique_ptr<storm::modelchecker::CheckResult> checkerResult = checker.check(env, *formula);
                
            storm::modelchecker::ExplicitQuantitativeCheckResult<double>& quantitativeResult = checkerResult->asExplicitQuantitativeCheckResult<double>();
                
            std::cout << "StormCWrapper_RESULT:" << instanceValues[0] << ":" << quantitativeResult[*concrete_model.getInitialStates().begin()] << "\n";
        }
    }
}

int main (int argc, char *argv[]) {

    // Init loggers
    storm::utility::setUp();
    storm::utility::setLogLevel(l3pp::LogLevel::OFF);

    // Set some settings objects.
    storm::settings::initializeAll("Storm", "storm");

    std::vector<std::string> arguments;
    arguments.reserve(argc);
    arguments.assign(argv, argv + argc);

    std::string modelType = arguments[1];
    std::string modelFile = arguments[2];
    std::string propertyFormula = arguments[3];
    std::string constants = arguments[4];

    storm::Environment env;
    env.solver().setLinearEquationSolverType(storm::solver::EquationSolverType::Topological);
    env.solver().topological().setUnderlyingEquationSolverType(storm::solver::EquationSolverType::Native);
    if (argc > 5) {
        applyMethod(env, arguments[5]);
    } else {
        applyMethod(env, "ORIGINAL");
    }

    storm::utility::setOutputDigitsFromGeneralPrecision(storm::settings::getModule<storm::settings::modules::GeneralSettings>().getPrecision());

    std::shared_ptr<storm::models::sparse::Model<storm::RationalFunction>> common_model;
    std::shared_ptr<storm::logic::Formula const> formula;

    if (modelType == "prism") {
        storm::prism::Program program = storm::api::parseProgram(modelFile, true);

        if (constants.length() > 0) {
            program = storm::utility::prism::preprocess(program, constants);
        }

        formula = storm::api::extractFormulasFromProperties(storm::api::parsePropertiesForPrismProgram(propertyFormula, program)).front();

        storm::generator::NextStateGeneratorOptions options(*formula);
        
        common_model = storm::builder::ExplicitModelBuilder<storm::RationalFunction>(program, options).build();
    } else if (modelType == "jani") {
        std::pair<storm::jani::Model, std::vector<storm::jani::Property>> pair_input = storm::api::parseJaniModel(modelFile);

        if (constants.length() > 0) {
            // adapted from stormpy/core/input.cpp#preprocess_symbolic_input
            storm::storage::SymbolicModelDescription input = pair_input.first;
            std::map<storm::expressions::Variable, storm::expressions::Expression> constantDefinitions;
            constantDefinitions = input.parseConstantDefinitions(constants);
            pair_input.first = input.preprocess(constantDefinitions).asJaniModel();
        }

        formula = storm::api::extractFormulasFromProperties(storm::api::parsePropertiesForJaniModel(propertyFormula, pair_input.first)).front();

        storm::generator::NextStateGeneratorOptions options(*formula);
        
        common_model = storm::builder::ExplicitModelBuilder<storm::RationalFunction>(pair_input.first, options).build();
    } else {
        return -1;
    }

    if (common_model->isPartiallyObservable()) {
        return -2;
    }
    if (common_model->isDiscreteTimeModel()) {
        // discrete time models
        if (common_model->isNondeterministicModel()) {
            checkMdp(common_model->as<storm::models::sparse::Mdp<storm::RationalFunction>>(), formula, env);
        } else {
            checkDtmc(common_model->as<storm::models::sparse::Dtmc<storm::RationalFunction>>(), formula, env);
        }
    } else {
        // continuous time model
        if (common_model->isNondeterministicModel()) {
            return -3;
        } else {
            checkCtmc(common_model->as<storm::models::sparse::Ctmc<storm::RationalFunction>>(), formula, env);
        }
    }
    return 0;
}
