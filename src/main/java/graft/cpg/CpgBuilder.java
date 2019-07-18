package graft.cpg;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: javadoc
 */
public class CpgBuilder {

    private static Logger log = LoggerFactory.getLogger(CpgBuilder.class);

    private SourceRoot srcRoot;

    public CpgBuilder(String srcRoot) {
        this.srcRoot = new SourceRoot(Paths.get(srcRoot));
        log.debug("New CpgBuilder (srcRoot=" + srcRoot + ")");
    }

    /**
     * TODO: javadoc
     */
    public void buildCpg() {
        log.info("Building CPG");

        List<ParseResult<CompilationUnit>> results;
        try {
            results = srcRoot.tryToParse();
        } catch (IOException e) {
            log.error("IOException in <CpgBuilder>.buildCpg", e);
            return;
        }

        for (ParseResult<CompilationUnit> result : results) {
            if (result.isSuccessful()) {
                CompilationUnit cu = result.getResult().get();
                cu.findRootNode().walk(new AstWalker(null));
            } else {
                log.error("Problems with parse");
                List<Problem> problems = result.getProblems();
                for (Problem problem : problems) {
                    log.error(problem.getVerboseMessage());
                }
            }
        }
    }

}