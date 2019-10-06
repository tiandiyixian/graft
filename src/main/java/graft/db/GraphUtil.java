package graft.db;

import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graft.GraftException;

import static graft.Const.*;

/**
 * TODO: javadocs
 */
public class GraphUtil {

    private static Logger log = LoggerFactory.getLogger(GraphUtil.class);
    private static Graph graph = null;

    /**
     * Initialise the graph database with the given options.
     *
     * @param options the graph database options
     */
    public static void initGraph(Configuration options) {
        log.debug("Initializing graph...");
        if (!options.containsKey("implementation")) {
            throw new GraftException("No graph implementation specified");
        }

        String impl = options.getString("implementation");
        switch (impl) {
            case TINKERGRAPH:
                initTinkerGraph(options);
                break;
            case NEO4J:
                initNeo4j(options);
                break;

            default:
                throw new GraftException("Unknown graph implementation '" + impl + "'");
        }
    }

    /**
     * Get a reference to the graph database.
     *
     * @return reference to the graph database
     * @throws GraftException if the graph has not been initialized
     */
    public static Graph graph() {
        if (graph != null) {
            return graph;
        } else {
            throw new GraftException("Graph not initialized");
        }
    }

    /**
     * Close the graph database, if it is still open.
     */
    public static void closeGraph() {
        log.debug("Closing graph...");
        if (graph != null) {
            try {
                graph.close();
            } catch (Exception e) {
                throw new GraftException("Could not close graph", e);
            }
        } else {
            log.warn("Graph already closed");
        }
    }

    // TODO: graph configs
    // TODO: config2 vs config mismatches

    private static void initTinkerGraph(Configuration options) {
        log.debug("Graph implementation: {}", TINKERGRAPH);
        org.apache.commons.configuration.Configuration tinkerConfig = new org.apache.commons.configuration.BaseConfiguration();
        graph = TinkerGraph.open();
    }

    private static void initNeo4j(Configuration options) {
        log.debug("Graph implementation: {}", NEO4J);
        org.apache.commons.configuration.Configuration neo4jConfig = new org.apache.commons.configuration.BaseConfiguration();
        // TODO: sensible default
        neo4jConfig.setProperty("gremlin.neo4j.directory", options.getString("directory", "/tmp/neo4j"));
        // XXX
        neo4jConfig.setProperty("gremlin.neo4j.conf.dbms.auto_index.nodes.enabled", "true");
        neo4jConfig.setProperty("gremlin.neo4j.conf.dbms.auto_index.relationships.enabled", "true");
        graph = Neo4jGraph.open(neo4jConfig);
    }
    
}