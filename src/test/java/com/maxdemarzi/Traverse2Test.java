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

public class Traverse2Test {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Rule
    public final Neo4jRule neo4j = new Neo4jRule()
            .withFixture(MODEL_STATEMENT)
            .withProcedure(DecisionTreeTraverser.class);

    @Test
    public void testScriptTraversal() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY1);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("option2", path1.get(path1.size() - 1).get("id"));
    }

    private static final Map QUERY1 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('credit', {Late60to89Days:'3'}) yield path return path")));

    private static final String MODEL_STATEMENT =
            "CREATE (tree:Tree { id: 'credit' })" +
                    "CREATE (one:Rule { parameter_names: 'Late60to89Days', parameter_types:'double', script:'if (Late60to89Days > 11.0) { return \"IS_TRUE\";}  if (Late60to89Days <= 11.0 && Late60to89Days > 3.0) { return \"OPTION_1\";}  if (Late60to89Days <= 3.0 && Late60to89Days > 0.0) { return \"OPTION_2\";}  if (Late60to89Days <= 3.0 && Late60to89Days <= 0.0) { return \"OPTION_3\";} return \"NONE\";' })" +
                    "CREATE (is_true:Answer { id: 'true'})" +
                    "CREATE (option1:Answer { id: 'option1'})" +
                    "CREATE (option2:Answer { id: 'option2'})" +
                    "CREATE (option3:Answer { id: 'option3'})" +
                    "CREATE (answer_no:Answer { id: 'no'})" +
                    "CREATE (tree)-[:HAS]->(one)" +
                    "CREATE (one)-[:IS_TRUE]->(is_true)" +
                    "CREATE (one)-[:OPTION_1]->(option1)" +
                    "CREATE (one)-[:OPTION_2]->(option2)" +
                    "CREATE (one)-[:OPTION_3]->(option3)" +
                    "CREATE (late:Parameter {name:'Late60to89Days', type:'double', prompt:'How many times were payments late 60-89 days?', expression:'(Late60to89Days > 0) &&  (Late60to89Days < 100)'})";


}
