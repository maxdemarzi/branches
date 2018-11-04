package com.maxdemarzi;

import clojure.java.api.Clojure;
import clojure.lang.*;
import com.maxdemarzi.results.StringResult;
import com.maxdemarzi.schema.Labels;
import com.maxdemarzi.schema.RelationshipTypes;
import com.opencsv.CSVIterator;
import com.opencsv.CSVReader;
import org.jblas.DoubleMatrix;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DecisionTreeCreator {

    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;

    @Procedure(name = "com.maxdemarzi.decision_tree.create", mode = Mode.WRITE)
    @Description("CALL com.maxdemarzi.decision_tree.create(tree, data, answers, threshold) - create tree")
    public Stream<StringResult> create(@Name("tree") String tree, @Name("data") String data,
                                       @Name("answers") String answers, @Name("threshold") Double threshold ) {
        long start = System.nanoTime();


        CSVIterator trainingAnswers = getCsvIterator(answers);
        CSVIterator trainingData = getCsvIterator(data);
        Set<Double> answerSet = new HashSet<>();
        HashMap<Double, Node> answerMap = new HashMap<>();
        ArrayList<Double> answerList = new ArrayList<>();
        while (trainingAnswers.hasNext()) {
            String[] nextLine = trainingAnswers.next();
            for (String item : nextLine) {
                answerList.add(Double.parseDouble(item));
            }
        }
        answerSet.addAll(answerList);
        for (Double value : answerSet) {
            Node answerNode = db.createNode(Labels.Answer);
            answerNode.setProperty("id", value);
            answerMap.put(value, answerNode);
        }

        String[] headers = trainingData.next();

        double[][] array = new double[answerList.size()][1 + headers.length];

        for (int r = 0; r < answerList.size(); r++) {
            array[r][0] = answerList.get(r);
            String[] columns = trainingData.next();
            for (int c = 0; c < columns.length; c++) {
                array[r][1 + c] = Double.parseDouble(columns[c]);
            }
        }

        DoubleMatrix fullData = new DoubleMatrix(array);

        fullData = fullData.transpose();
        int observationCount = fullData.columns;
        int featuresCount = fullData.rows;
        int[] rowIndices = IntStream.range(0, featuresCount).toArray();

        double[][] training = fullData.toArray2();

        DoubleMatrix X = fullData.getRows(rowIndices);

        /* Import clojure core. */
        final IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("DecisionStream"));

        /* Invoke Clojure trainDStream function. */
        final IFn trainFunc = Clojure.var("DecisionStream", "trainDStream");

        HashMap dStreamM = new HashMap<>((PersistentArrayMap) trainFunc.invoke(X, rowIndices, threshold));

        Node treeNode = db.createNode(Labels.Tree);
        treeNode.setProperty("id", tree);
        HashMap<String, Node> nodes = new HashMap<>();
        deepLinkMap(db, answerMap, nodes, headers, treeNode, RelationshipTypes.HAS, dStreamM, true);

        return Stream.of(new StringResult("Tree created in " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) + " seconds"));
    }

    static void deepLinkMap(GraphDatabaseService db, HashMap<Double, Node> answerMap, HashMap<String, Node> nodes, String[] headers, Node parent, RelationshipType relType, HashMap nestedMap, boolean leftSide) {
        RelationshipType leftType = RelationshipTypes.IS_FALSE;
        RelationshipType rightType = RelationshipTypes.IS_TRUE;

        // We are at a Leaf
        if (nestedMap.size() == 2) {
            Keyword labelCounts = (Keyword)nestedMap.keySet().toArray()[0];
            Collection<PersistentVector> values = (Collection) nestedMap.get(labelCounts);
            for (PersistentVector vector : values) {
                Node answer = answerMap.get(vector.get(0));
                Double weight = (Double)vector.get(1);
                Relationship rel = parent.createRelationshipTo(answer, relType);
                rel.setProperty("weight", weight);
            }
        } else {
            Node rule;
            String key = getKey(headers, nestedMap);
            String[] keyParts = key.split("-");
            String feature = keyParts[0];
            String threshold = keyParts[1];

            String parentFeature = (String)parent.getProperty("parameter_names", "");
//            boolean even = false;
//            if (relType.name().startsWith("OPTION")) {
//                even = (Integer.valueOf(relType.name().split("_")[1]) % 2) == 0;
//            }

            // Only valid for IS_FALSE same feature children nodes
            if(feature.equals(parentFeature) &&  leftSide) {
                rule = parent;
                ArrayList<Pair<String, String>> options = new ArrayList<>();
                String[] values;
                if (rule.hasProperty("values")){
                    values = (String[])rule.getProperty("values");
                } else {
                    String previousThreshold = ((String)rule.getProperty("expression")).split(" > ")[1];
                    values = new String[]{previousThreshold};
                }
                ArrayList<String> thresholds = new ArrayList<>(Arrays.asList(values));
                thresholds.add(threshold);

                options.add(Pair.of(feature + " > " + thresholds.get(0), "\"IS_TRUE\""));

                for (int i = 1; i < thresholds.size(); i++) {
                    options.add(Pair.of(feature + " <= " + thresholds.get(i - 1) + " && " + feature + " > " + thresholds.get(i), "\"OPTION_" + options.size() + "\""));
                    if (thresholds.size() - i == 1) {
                        options.add(Pair.of(feature + " <= " + thresholds.get(i - 1) + " && " + feature + " <= " + thresholds.get(i), "\"OPTION_" + options.size() + "\""));
                    }
                }

                rightType = RelationshipType.withName("OPTION_" + (options.size() - 2));
                leftType = RelationshipType.withName("OPTION_" + (options.size() - 1));
                rule.setProperty("values", thresholds.toArray(new String[]{}));
                rule.removeProperty("expression");

                //todo move this to the threshold loop
                // Delete redundant options
//                for (int i = 2; i < options.size() - 2; i+=2) {
//                    options.remove(i);
//                }

                StringBuilder script = new StringBuilder();
                for (Pair<String, String> pair : options) {
                    script.append(" if (").append(pair.first()).append(") { return ").append(pair.other()).append(";} ");
                }
                rule.setProperty("script", script.toString());
                nodes.put(key, rule);
            } else {
                if (nodes.containsKey(key)) {
                    rule = nodes.get(key);
                } else {
                    rule = db.createNode(Labels.Rule);
                    rule.setProperty("expression", feature + " > " + threshold);
                    rule.setProperty("parameter_names", feature);
                    rule.setProperty("parameter_types", "double");
                    nodes.put(key, rule);
                }
                parent.createRelationshipTo(rule, relType);
            }

            Symbol left = (Symbol) nestedMap.keySet().toArray()[2];
            HashMap leftMap = new HashMap<>((PersistentArrayMap) ((Atom) nestedMap.get(left)).deref());
            String leftKey = getKey(headers, leftMap);

            if (nodes.keySet().contains(leftKey)) {
                Node leftNode = nodes.get(leftKey);
                rule.createRelationshipTo(leftNode, leftType);
            } else {
                deepLinkMap(db, answerMap, nodes, headers, rule, leftType, leftMap, true);
            }

            Symbol right = (Symbol) nestedMap.keySet().toArray()[3];
            HashMap rightMap = new HashMap<>((PersistentArrayMap) ((Atom) nestedMap.get(right)).deref());
            String rightKey = getKey(headers, rightMap);

            if (nodes.keySet().contains(rightKey)) {
                Node rightNode = nodes.get(rightKey);
                rule.createRelationshipTo(rightNode, rightType);
            } else {
                deepLinkMap(db, answerMap, nodes, headers, rule, rightType, rightMap, false);
            }
        }
    }

    private static String getKey(String[] headers, HashMap map) {
        Double threshold = -1.0;
        int featureId = -1;
        Double leftThreshold = -1.0;
        int leftFeatureId = -1;
        Double rightThreshold = -1.0;
        int rightFeatureId = -1;
        String feature = "leaf";
        String leftFeature = "leaf";
        String rightFeature = "leaf";

        if (map.size() > 2) {
            Symbol thresholdSymbol = (Symbol) map.keySet().toArray()[0];
            Symbol featureIdSymbol = (Symbol) map.keySet().toArray()[1];
            threshold = (Double) map.get(thresholdSymbol);
            featureId = Math.toIntExact((Long) map.get(featureIdSymbol));
            feature = headers[featureId];

            Symbol left = (Symbol) map.keySet().toArray()[2];
            HashMap leftMap = new HashMap<>((PersistentArrayMap) ((Atom) map.get(left)).deref());
            if (leftMap.size() > 2) {
                Symbol leftThresholdSymbol = (Symbol) leftMap.keySet().toArray()[0];
                Symbol leftFeatureIdSymbol = (Symbol) leftMap.keySet().toArray()[1];
                leftThreshold = (Double) leftMap.get(leftThresholdSymbol);
                leftFeatureId = Math.toIntExact((Long) leftMap.get(leftFeatureIdSymbol));
                leftFeature = headers[leftFeatureId];
            }
            Symbol right = (Symbol) map.keySet().toArray()[3];
            HashMap rightMap = new HashMap<>((PersistentArrayMap) ((Atom) map.get(right)).deref());
            if (rightMap.size() > 2) {
                Symbol rightThresholdSymbol = (Symbol) rightMap.keySet().toArray()[0];
                Symbol rightFeatureIdSymbol = (Symbol) rightMap.keySet().toArray()[1];
                rightThreshold = (Double) rightMap.get(rightThresholdSymbol);
                rightFeatureId = Math.toIntExact((Long) rightMap.get(rightFeatureIdSymbol));
                rightFeature = headers[rightFeatureId];
            }
        }
        return feature + "-" + threshold + "-" + leftFeature + "-" + leftThreshold + "-" + rightFeature + "-" + rightThreshold;
    }

    private CSVIterator getCsvIterator(String file) {
        CSVIterator records = null;
        try {
            records = new CSVIterator(new CSVReader(new FileReader(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log.error("DecisionTreeCreator - File not found: " + file);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("DecisionTreeCreator - IO Exception: " + file);
        }
        return records;
    }

}
