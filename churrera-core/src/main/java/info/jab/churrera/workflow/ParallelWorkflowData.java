package info.jab.churrera.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class containing information about a parallel workflow.
 */
public class ParallelWorkflowData {
    private final PromptInfo parallelPrompt;
    private final String bindResultType;
    private final List<SequenceInfo> sequences;

    public ParallelWorkflowData(PromptInfo parallelPrompt, String bindResultType, List<SequenceInfo> sequences) {
        this.parallelPrompt = parallelPrompt;
        this.bindResultType = bindResultType;
        this.sequences = new ArrayList<>(sequences);
    }

    public PromptInfo getParallelPrompt() {
        return parallelPrompt;
    }

    public String getBindResultType() {
        return bindResultType;
    }

    public List<SequenceInfo> getSequences() {
        return new ArrayList<>(sequences);
    }

    public boolean hasBindResultType() {
        return bindResultType != null && !bindResultType.trim().isEmpty();
    }
}

