package com.maxdemarzi;

import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.ScriptEvaluator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import java.util.Map;

class DecisionTree {

    static boolean isValid(Node node, String fact) throws Exception {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setExpressionType(boolean.class);

        String[] parameterNames = Magic.explode((String) node.getProperty("name", ""));
        Class<?>[] parameterTypes = Magic.stringToTypes((String) node.getProperty("type", ""));

        // Fill the arguments array with their corresponding values
        Object[] arguments = {Magic.createObject(parameterTypes[0], fact)};

        // Set our parameters with their matching types
        ee.setParameters(parameterNames, parameterTypes);

        // And now we "cook" (scan, parse, compile and load) the expression.
        ee.cook((String) node.getProperty("expression"));

        return (boolean) ee.evaluate(arguments);
    }

    static RelationshipType trueOrFalse(Node node, Map<String, String> facts) throws Exception {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setExpressionType(boolean.class);

        String[] parameterNames = Magic.explode((String) node.getProperty("parameter_names", node.getProperty("name", "")));
        Class<?>[] parameterTypes = Magic.stringToTypes((String) node.getProperty("parameter_types", node.getProperty("type", "")));

        // Fill the arguments array with their corresponding values
        Object[] arguments = new Object[parameterNames.length];
        for (int j = 0; j < parameterNames.length; ++j) {
            arguments[j] = Magic.createObject(parameterTypes[j], facts.get(parameterNames[j]));
        }

        // Set our parameters with their matching types
        ee.setParameters(parameterNames, parameterTypes);

        // And now we "cook" (scan, parse, compile and load) the expression.
        ee.cook((String) node.getProperty("expression"));

        return RelationshipType.withName("IS_" + ee.evaluate(arguments).toString().toUpperCase());
    }

    static RelationshipType choosePath(Node node, Map<String, String> facts) throws Exception {
        ScriptEvaluator se = new ScriptEvaluator();
        se.setReturnType(String.class);

        // Get the properties of the node stored in the node
        String[] parameterNames = Magic.explode((String) node.getProperty("parameter_names", node.getProperty("name", "")));
        Class<?>[] parameterTypes = Magic.stringToTypes((String) node.getProperty("parameter_types", node.getProperty("type", "")));

        // Fill the arguments array with their corresponding values
        Object[] arguments = new Object[parameterNames.length];
        for (int j = 0; j < parameterNames.length; ++j) {
            arguments[j] = Magic.createObject(parameterTypes[j], facts.get(parameterNames[j]));
        }

        // Set our parameters with their matching types
        se.setParameters(parameterNames, parameterTypes);

        // And now we "cook" (scan, parse, compile and load) the script.
        se.cook((String) node.getProperty("script"));

        return RelationshipType.withName((String) se.evaluate(arguments));
    }
}
