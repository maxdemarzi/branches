# Decision Stream into Neo4j
POC Decision Stream creator and traverser with rules

This project requires Neo4j 3.4.x or higher

Instructions
------------ 

This project uses maven, to build a jar-file with the procedure in this
project, simply package the project with maven:

    mvn clean package

This will produce a jar-file, `target/branches-1.0-SNAPSHOT.jar`,
that can be copied to the `plugin` directory of your Neo4j instance.

    cp target/branches-1.0-SNAPSHOT.jar neo4j-enterprise-3.4.9/plugins/.
    
Download and Copy two additional files to your Neo4j plugins directory:

    http://central.maven.org/maven2/org/codehaus/janino/commons-compiler/3.0.8/commons-compiler-3.0.8.jar
    http://central.maven.org/maven2/org/codehaus/janino/janino/3.0.8/janino-3.0.8.jar
      

Restart your Neo4j Server.

Create the Schema by running this stored procedure:

    CALL com.maxdemarzi.schema.generate

Then create a Tree:

    CALL com.maxdemarzi.decision_tree.create('credit', '/Users/maxdemarzi/Documents/Projects/branches/training.csv', '/Users/maxdemarzi/Documents/Projects/branches/answers.csv', 0.02)
        
Try it:
    
    CALL com.maxdemarzi.decision_tree.traverse('credit', {RevolvingUtilizationOfUnsecuredLines:'0.7661', Age:'45', NumberOfTime30to59DaysPastDueNotWorse:'2', DebtRatio:'0.803', MonthlyIncome:'9120',NumberOfOpenCreditLinesAndLoans:'13', NumberOfTimes90DaysLate:'0',NumberRealEstateLoansOrLines:'6', NumberOfTime60to89DaysPastDueNotWorse:'0', NumberOfDependents:'2'});
    CALL com.maxdemarzi.decision_tree.traverse('credit', {RevolvingUtilizationOfUnsecuredLines:'0.9572', Age:'40', NumberOfTime30to59DaysPastDueNotWorse:'20', DebtRatio:'0.1219', MonthlyIncome:'2600',NumberOfOpenCreditLinesAndLoans:'4', NumberOfTimes90DaysLate:'0',NumberRealEstateLoansOrLines:'0', NumberOfTime60to89DaysPastDueNotWorse:'0', NumberOfDependents:'1'});
    
