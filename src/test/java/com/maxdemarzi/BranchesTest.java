package com.maxdemarzi;

import org.junit.*;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;

import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertTrue;

public class BranchesTest {

    @Rule
    public final Neo4jRule neo4j = new Neo4jRule()
            .withProcedure(Branches.class);

    @Test
    @Ignore
    public void testBranches() throws Exception {

        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY);
        String results = response.get("results").get(0).get("data").get(0).get("row").get(0).asText();
        assertTrue(results.startsWith("Tree created in"));
    }

    private static final Map QUERY =
            singletonMap("statements", asList(singletonMap("statement",
                    "CALL com.maxdemarzi.branches('test', '/Users/maxdemarzi/Documents/Projects/branches/training.csv', '/Users/maxdemarzi/Documents/Projects/branches/answers.csv', 0.02)")));

}
