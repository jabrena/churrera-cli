package info.jab.churrera.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class containing information about a sequence within a parallel workflow.
 */
public class SequenceInfo {
    private final String model;
    private final String repository;
    private final List<PromptInfo> prompts;

    public SequenceInfo(String model, String repository, List<PromptInfo> prompts) {
        this.model = model;
        this.repository = repository;
        this.prompts = new ArrayList<>(prompts);
    }

    public String getModel() {
        return model;
    }

    public String getRepository() {
        return repository;
    }

    public List<PromptInfo> getPrompts() {
        return new ArrayList<>(prompts);
    }
}

