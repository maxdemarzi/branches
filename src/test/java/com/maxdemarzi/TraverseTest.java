package com.maxdemarzi;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;

import java.util.ArrayList;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertEquals;

public class TraverseTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Rule
    public final Neo4jRule neo4j = new Neo4jRule()
            .withFixture(MODEL_STATEMENT)
            .withProcedure(DecisionTreeTraverser.class);

    @Test
    public void testExpressionTraversal() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY1);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("no", path1.get(path1.size() - 1).get("id"));
    }

    private static final Map QUERY1 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('bar entrance', {gender:'male', age:'20'}) yield path return path")));

    @Test
    public void testExpressionTraversalTwo() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY2);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("yes", path1.get(path1.size() - 1).get("id"));
    }

    private static final Map QUERY2 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('bar entrance', {gender:'female', age:'19'}) yield path return path")));

    @Test
    public void testExpressionTraversalThree() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY3);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("yes", path1.get(path1.size() - 1).get("id"));
    }

    private static final Map QUERY3 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('bar entrance', {gender:'male', age:'23'}) yield path return path")));

    @Test
    public void testExpressionTraversalFour() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY4);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("age", path1.get(path1.size() - 1).get("name"));
    }

    private static final Map QUERY4 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('bar entrance', {}) yield path return path")));

    @Test
    public void testExpressionTraversalFive() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY5);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("gender", path1.get(path1.size() - 1).get("name"));
    }

    private static final Map QUERY5 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('bar entrance', {age:'20'}) yield path return path")));


    @Test
    public void testExpressionTraversalSix() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY6);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("age", path1.get(path1.size() - 1).get("name"));
    }

    private static final Map QUERY6 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('bar entrance', {gender:'female'}) yield path return path")));


    @Test
    public void testScriptTraversal() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), SQUERY1);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("incorrect", path1.get(path1.size() - 1).get("id"));
    }

    private static final Map SQUERY1 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('funeral', {answer_1:'yeah', answer_2:'yeah', answer_3:'yeah'}) yield path return path")));

    @Test
    public void testScriptTraversalTwo() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), SQUERY2);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("unknown", path1.get(path1.size() - 1).get("id"));
    }

    private static final Map SQUERY2 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('funeral', {answer_1:'what', answer_2:'', answer_3:''}) yield path return path")));

    @Test
    public void testScriptTraversalThree() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), SQUERY3);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("correct", path1.get(path1.size() - 1).get("id"));
    }

    private static final Map SQUERY3 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('funeral', {answer_1:'what', answer_2:'yeah', answer_3:'okay'}) yield path return path")));

    @Test
    public void testScriptTraversalFour() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), SQUERY4);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("answer_1", path1.get(path1.size() - 1).get("name"));
    }

    private static final Map SQUERY4 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('funeral', {}) yield path return path")));

    @Test
    public void testScriptTraversalFive() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), SQUERY5);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("answer_2", path1.get(path1.size() - 1).get("name"));
    }
    private static final Map SQUERY5 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('funeral', {answer_1:'what'}) yield path return path")));


    private static final String MODEL_STATEMENT =
            "CREATE (tree:Tree { id: 'bar entrance' })" +
                    "CREATE (over21_rule:Rule { parameter_names: 'age', parameter_types:'int', expression:'age >= 21' })" +
                    "CREATE (gender_rule:Rule { parameter_names: 'age,gender', parameter_types:'int,String', expression:'(age >= 18) && gender.equals(\"female\")' })" +
                    "CREATE (answer_yes:Answer { id: 'yes'})" +
                    "CREATE (answer_no:Answer { id: 'no'})" +
                    "CREATE (tree)-[:HAS]->(over21_rule)" +
                    "CREATE (over21_rule)-[:IS_TRUE]->(answer_yes)" +
                    "CREATE (over21_rule)-[:IS_FALSE]->(gender_rule)" +
                    "CREATE (gender_rule)-[:IS_TRUE]->(answer_yes)" +
                    "CREATE (gender_rule)-[:IS_FALSE]->(answer_no)" +
                    "CREATE (age:Parameter {name:'age', type:'int', prompt:'How old are you?', expression:'(age > 0) &&  (age < 150)'})" +
                    "CREATE (gender:Parameter {name:'gender', type:'String', prompt:'What is your gender?', expression: '\"male\".equals(gender) || \"female\".equals(gender)'} )" +
                    "CREATE (over21_rule)-[:REQUIRES]->(age)" +
                    "CREATE (gender_rule)-[:REQUIRES]->(age)" +
                    "CREATE (gender_rule)-[:REQUIRES]->(gender)" +
                    "CREATE (tree2:Tree { id: 'funeral' })" +
                    "CREATE (good_man_rule:Rule { name: 'Was Lil Jon a good man?', parameter_names: 'answer_1', parameter_types:'String', script:'switch (answer_1) { case \"yeah\": return \"OPTION_1\"; case \"what\": return \"OPTION_2\"; case \"okay\": return \"OPTION_3\"; default: return \"UNKNOWN\"; }' })" +
                    "CREATE (good_man_two_rule:Rule { name: 'I said, was he a good man?', parameter_names: 'answer_2', parameter_types:'String', script:'switch (answer_2) { case \"yeah\": return \"OPTION_1\"; case \"what\": return \"OPTION_2\"; case \"okay\": return \"OPTION_3\"; default: return \"UNKNOWN\"; }' })" +
                    "CREATE (rest_in_peace_rule:Rule { name: 'May he rest in peace', parameter_names: 'answer_3', parameter_types:'String', script:'switch (answer_3) { case \"yeah\": return \"OPTION_1\"; case \"what\": return \"OPTION_2\"; case \"okay\": return \"OPTION_3\"; default: return \"UNKNOWN\"; } ' })" +
                    "CREATE (answer_correct:Answer { id: 'correct'})" +
                    "CREATE (answer_incorrect:Answer { id: 'incorrect'})" +
                    "CREATE (answer_unknown:Answer { id: 'unknown'})" +
                    "CREATE (tree2)-[:HAS]->(good_man_rule)" +
                    "CREATE (good_man_rule)-[:OPTION_1]->(answer_incorrect)" +
                    "CREATE (good_man_rule)-[:OPTION_2]->(good_man_two_rule)" +
                    "CREATE (good_man_rule)-[:OPTION_3]->(answer_incorrect)" +
                    "CREATE (good_man_rule)-[:UNKNOWN]->(answer_unknown)" +

                    "CREATE (good_man_two_rule)-[:OPTION_1]->(rest_in_peace_rule)" +
                    "CREATE (good_man_two_rule)-[:OPTION_2]->(answer_incorrect)" +
                    "CREATE (good_man_two_rule)-[:OPTION_3]->(answer_incorrect)" +
                    "CREATE (good_man_two_rule)-[:UNKNOWN]->(answer_unknown)" +

                    "CREATE (rest_in_peace_rule)-[:OPTION_1]->(answer_incorrect)" +
                    "CREATE (rest_in_peace_rule)-[:OPTION_2]->(answer_incorrect)" +
                    "CREATE (rest_in_peace_rule)-[:OPTION_3]->(answer_correct)" +
                    "CREATE (rest_in_peace_rule)-[:UNKNOWN]->(answer_unknown)" +

                    "CREATE (parameter1:Parameter {name:'answer_1', type:'String', prompt:'What is the first answer?', expression:'\"yeah\".equals(answer_1) || \"what\".equals(answer_1) || \"okay\".equals(answer_1) || \"\".equals(answer_1)'})" +
                    "CREATE (parameter2:Parameter {name:'answer_2', type:'String', prompt:'What is the second answer?', expression:'\"yeah\".equals(answer_2) || \"what\".equals(answer_2) || \"okay\".equals(answer_2) || \"\".equals(answer_2)'})" +
                    "CREATE (parameter3:Parameter {name:'answer_3', type:'String', prompt:'What is the third answer?', expression:'\"yeah\".equals(answer_3) || \"what\".equals(answer_3) || \"okay\".equals(answer_3) || \"\".equals(answer_3)'})" +
                    "CREATE (good_man_rule)-[:REQUIRES]->(parameter1)" +
                    "CREATE (good_man_two_rule)-[:REQUIRES]->(parameter2)" +
                    "CREATE (rest_in_peace_rule)-[:REQUIRES]->(parameter3)";
}
