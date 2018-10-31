package com.maxdemarzi.schema;

import com.maxdemarzi.results.StringResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.io.IOException;
import java.util.stream.Stream;

public class Schema {

    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;

    @Procedure(name = "com.maxdemarzi.schema.generate", mode = Mode.SCHEMA)
    @Description("CALL com.maxdemarzi.schema.generate() - generate schema")

    public Stream<StringResult> generate() throws IOException {
        org.neo4j.graphdb.schema.Schema schema = db.schema();
        if (!schema.getIndexes(Labels.Tree).iterator().hasNext()) {
            schema.constraintFor(Labels.Tree)
                    .assertPropertyIsUnique("id")
                    .create();
        }
        if (!schema.getIndexes(Labels.Parameter).iterator().hasNext()) {
            schema.constraintFor(Labels.Parameter)
                    .assertPropertyIsUnique("name")
                    .create();
        }
        return Stream.of(new StringResult("Schema Generated"));
    }
}
