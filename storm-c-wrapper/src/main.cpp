#include <iostream>
#include <string>

#include "storm-config.h"

#include <boost/algorithm/string.hpp>

#include <l3pp.h>

#include "storm/utility/initialize.h"
#include "storm/utility/constants.h"
#include "storm/adapters/RationalFunctionAdapter.h"
#include <carl/numbers/numbers.h>
#include <carl/core/VariablePool.h>


#include "storm/settings/SettingsManager.h"
#include "storm/settings/modules/GeneralSettings.h"

#include "storm-pars/utility/ModelInstantiator.h"
#include "storm/api/storm.h"
#include "storm-parsers/api/storm-parsers.h"
#include "storm/models/sparse/Model.h"
#include "storm/models/sparse/Ctmc.h"
#include "storm/models/sparse/Dtmc.h"
#include "storm/models/sparse/Mdp.h"
#include "storm/modelchecker/results/ExplicitQuantitativeCheckResult.h"
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

void checkCtmc(std::shared_ptr<storm::models::sparse::Ctmc<storm::RationalFunction>> ctmc, std::shared_ptr<storm::logic::Formula const> formula) {
    storm::utility::ModelInstantiator<storm::models::sparse::Ctmc<storm::RationalFunction>, storm::models::sparse::Ctmc<double>> modelInstantiator(*ctmc);

    std::set<storm::RationalFunctionVariable> variables = storm::models::sparse::getAllParameters(*ctmc);

    std::string line;
    while (std::getline(std::cin, line)) {
        if ("EOF" == line) {
            break;
        }
        
        std::vector<std::string> argument;
        boost::split(argument, line, boost::is_any_of(":"));
        std::string identifier = argument[0];

        storm::models::sparse::Ctmc<double> const& concrete_ctmc(modelInstantiator.instantiate(getParameterValues(argument[1], variables)));

        storm::modelchecker::SparseCtmcCslModelChecker<storm::models::sparse::Ctmc<double>> checker(concrete_ctmc);
        
        std::unique_ptr<storm::modelchecker::CheckResult> result = checker.check(*formula);
        
        storm::modelchecker::ExplicitQuantitativeCheckResult<double>& quantitativeResult = result->asExplicitQuantitativeCheckResult<double>();
        
        std::cout << "StormCWrapper_RESULT:" << identifier << ":" << quantitativeResult[*concrete_ctmc.getInitialStates().begin()] << "\n";
    }
}

void checkDtmc(std::shared_ptr<storm::models::sparse::Dtmc<storm::RationalFunction>> dtmc, std::shared_ptr<storm::logic::Formula const> formula) {
    storm::utility::ModelInstantiator<storm::models::sparse::Dtmc<storm::RationalFunction>, storm::models::sparse::Dtmc<double>> modelInstantiator(*dtmc);

    std::set<storm::RationalFunctionVariable> variables = storm::models::sparse::getAllParameters(*dtmc);

    std::string line;
    while (std::getline(std::cin, line)) {
        if ("EOF" == line) {
            break;
        }
        
        std::vector<std::string> argument;
        boost::split(argument, line, boost::is_any_of(":"));
        std::string identifier = argument[0];

        storm::models::sparse::Dtmc<double> const& concrete_dtmc(modelInstantiator.instantiate(getParameterValues(argument[1], variables)));

        storm::modelchecker::SparseDtmcPrctlModelChecker<storm::models::sparse::Dtmc<double>> checker(concrete_dtmc);
        
        std::unique_ptr<storm::modelchecker::CheckResult> result = checker.check(*formula);
        
        storm::modelchecker::ExplicitQuantitativeCheckResult<double>& quantitativeResult = result->asExplicitQuantitativeCheckResult<double>();
        
        std::cout << "StormCWrapper_RESULT:" << identifier << ":" << quantitativeResult[*concrete_dtmc.getInitialStates().begin()] << "\n";
    }
}

void checkMdp(std::shared_ptr<storm::models::sparse::Mdp<storm::RationalFunction>> mdp, std::shared_ptr<storm::logic::Formula const> formula) {
    storm::utility::ModelInstantiator<storm::models::sparse::Mdp<storm::RationalFunction>, storm::models::sparse::Mdp<double>> modelInstantiator(*mdp);

    std::set<storm::RationalFunctionVariable> variables = storm::models::sparse::getAllParameters(*mdp);

    std::string line;
    while (std::getline(std::cin, line)) {
        if ("EOF" == line) {
            break;
        }
        
        std::vector<std::string> argument;
        boost::split(argument, line, boost::is_any_of(":"));
        std::string identifier = argument[0];

        storm::models::sparse::Mdp<double> const& concrete_mdp(modelInstantiator.instantiate(getParameterValues(argument[1], variables)));

        storm::modelchecker::SparseMdpPrctlModelChecker<storm::models::sparse::Mdp<double>> checker(concrete_mdp);
        
        std::unique_ptr<storm::modelchecker::CheckResult> result = checker.check(*formula);
        
        storm::modelchecker::ExplicitQuantitativeCheckResult<double>& quantitativeResult = result->asExplicitQuantitativeCheckResult<double>();
        
        std::cout << "StormCWrapper_RESULT:" << identifier << ":" << quantitativeResult[*concrete_mdp.getInitialStates().begin()] << "\n";
    }
}

int main (int argc, char *argv[]) {

    // Init loggers
    storm::utility::setUp();
    storm::utility::setLogLevel(l3pp::LogLevel::OFF);

    // Set some settings objects.
    storm::settings::initializeAll("storm-c-wrapper", "storm-c-wrapper");

    std::vector<std::string> arguments;
    arguments.reserve(argc);
    arguments.assign(argv, argv + argc);

    std::string modelType = arguments[1];
    std::string modelFile = arguments[2];
    std::string propertyFormula = arguments[3];
    std::string constants = arguments[4];

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
            checkMdp(common_model->as<storm::models::sparse::Mdp<storm::RationalFunction>>(), formula);
        } else {
            checkDtmc(common_model->as<storm::models::sparse::Dtmc<storm::RationalFunction>>(), formula);
        }
    } else {
        // continuous time model
        if (common_model->isNondeterministicModel()) {
            return -3;
        } else {
            checkCtmc(common_model->as<storm::models::sparse::Ctmc<storm::RationalFunction>>(), formula);
        }
    }
    return 0;
}