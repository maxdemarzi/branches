package com.maxdemarzi;

import clojure.java.api.Clojure;
import clojure.lang.*;
import com.opencsv.CSVIterator;
import com.opencsv.CSVReader;
import org.jblas.DoubleMatrix;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Branches {

    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;

    @Procedure(name = "com.maxdemarzi.branches", mode = Mode.WRITE)
    @Description("CALL com.maxdemarzi.branches(tree, data, answers, threshold) - traverse paths")
    public Stream<StringResult> branches(@Name("tree") String tree, @Name("data") String data,
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
        treeNode.setProperty("name", tree);
        HashMap<String, Node> nodes = new HashMap<>();
        deepLinkMap(db, answerMap, nodes, headers, treeNode, RelationshipTypes.HAS, dStreamM);

        return Stream.of(new StringResult("Tree created in " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) + " seconds"));
    }

    static void deepLinkMap(GraphDatabaseService db, HashMap<Double, Node> answerMap, HashMap<String, Node> nodes, String[] headers, Node parent, RelationshipType relType, HashMap nestedMap) {
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
            Symbol thresholdSymbol = (Symbol) nestedMap.keySet().toArray()[0];
            Symbol featureIdSymbol = (Symbol) nestedMap.keySet().toArray()[1];
            Double threshold = (Double) nestedMap.get(thresholdSymbol);
            int featureId = Math.toIntExact((Long) nestedMap.get(featureIdSymbol));

            String key = headers[featureId] + "-" + threshold;

            Node rule = db.createNode(Labels.Rule);
            rule.setProperty("expression", headers[featureId] + " > " + threshold);
            rule.setProperty("parameter_names", headers[featureId]);
            rule.setProperty("parameter_types", "double");

            nodes.put(key, rule);

            parent.createRelationshipTo(rule, relType);

            if (nestedMap.size() > 2) {
                Symbol left = (Symbol) nestedMap.keySet().toArray()[2];
                HashMap leftMap = new HashMap<>((PersistentArrayMap) ((Atom) nestedMap.get(left)).deref());
                if (leftMap.size() > 2) {
                    Symbol leftThresholdSymbol = (Symbol) leftMap.keySet().toArray()[0];
                    Symbol leftFeatureIdSymbol = (Symbol) leftMap.keySet().toArray()[1];
                    Double leftThreshold = (Double) leftMap.get(leftThresholdSymbol);
                    int leftFeatureId = Math.toIntExact((Long) leftMap.get(leftFeatureIdSymbol));

                    String leftKey = headers[leftFeatureId] + "-" + leftThreshold;
                    if (nodes.keySet().contains(leftKey)) {
                        Node leftNode = nodes.get(leftKey);
                        rule.createRelationshipTo(leftNode, RelationshipTypes.IS_FALSE);
                    } else {
                        deepLinkMap(db, answerMap, nodes, headers, rule, RelationshipTypes.IS_FALSE, leftMap);
                    }
                } else {
                    deepLinkMap(db, answerMap, nodes, headers, rule, RelationshipTypes.IS_FALSE, leftMap);
                }
            }

            if (nestedMap.size() > 3) {
                Symbol right = (Symbol) nestedMap.keySet().toArray()[3];
                HashMap rightMap = new HashMap<>((PersistentArrayMap) ((Atom) nestedMap.get(right)).deref());
                if (rightMap.size() > 2) {
                    Symbol rightThresholdSymbol = (Symbol) rightMap.keySet().toArray()[0];
                    Symbol rightFeatureIdSymbol = (Symbol) rightMap.keySet().toArray()[1];
                    Double rightThreshold = (Double) rightMap.get(rightThresholdSymbol);
                    int rightFeatureId = Math.toIntExact((Long) rightMap.get(rightFeatureIdSymbol));

                    String rightKey = headers[rightFeatureId] + "-" + rightThreshold;
                    if (nodes.keySet().contains(rightKey)) {
                        Node rightNode = nodes.get(rightKey);
                        rule.createRelationshipTo(rightNode, RelationshipTypes.IS_TRUE);
                    } else {
                        deepLinkMap(db, answerMap, nodes, headers, rule, RelationshipTypes.IS_TRUE, rightMap);
                    }
                } else {
                    deepLinkMap(db, answerMap, nodes, headers, rule, RelationshipTypes.IS_TRUE, rightMap);
                }
            }
        }
    }

    private CSVIterator getCsvIterator(String file) {
        CSVIterator records = null;
        try {
            records = new CSVIterator(new CSVReader(new FileReader(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log.error("Branches - File not found: " + file);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Branches - IO Exception: " + file);
        }
        return records;
    }

}
