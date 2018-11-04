package com.maxdemarzi;

import org.junit.*;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;

import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertTrue;

public class CreateTest {

    @Rule
    public final Neo4jRule neo4j = new Neo4jRule()
            .withProcedure(DecisionTreeCreator.class)
            .withProcedure(DecisionTreeTraverser.class);

    @Test
    @Ignore
    public void testCreation() throws Exception {

        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY);
        String results = response.get("results").get(0).get("data").get(0).get("row").get(0).asText();
        assertTrue(results.startsWith("Tree created in"));
        response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY2);
        int count = response.get("results").get(0).get("data").size();
    }

    private static final Map QUERY =
            singletonMap("statements", asList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.create('credit', '/Users/maxdemarzi/Documents/Projects/branches/training.csv', '/Users/maxdemarzi/Documents/Projects/branches/answers.csv', 0.02)")));

    private static final Map QUERY2 =
            singletonMap("statements", asList(singletonMap("statement",
                    "CALL com.maxdemarzi.decision_tree.traverse('credit', {RevolvingUtilizationOfUnsecuredLines:'0.7661', Age:'45', NumberOfTime30to59DaysPastDueNotWorse:'2', DebtRatio:'0.803', MonthlyIncome:'9120',NumberOfOpenCreditLinesAndLoans:'13', NumberOfTimes90DaysLate:'0',NumberRealEstateLoansOrLines:'6', NumberOfTime60to89DaysPastDueNotWorse:'0', NumberOfDependents:'2'})")));



}
