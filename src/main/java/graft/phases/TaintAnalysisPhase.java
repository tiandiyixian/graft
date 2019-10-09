package graft.phases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This phase handles the running of taint analyses.
 *
 * @author Wim Keirsgieter
 */
public class TaintAnalysisPhase implements GraftPhase {

    private static Logger log = LoggerFactory.getLogger(TaintAnalysisPhase.class);

    public TaintAnalysisPhase() { }

    @Override
    public void run() {
        log.info("Running TaintAnalysisPhase...");
    }

}
