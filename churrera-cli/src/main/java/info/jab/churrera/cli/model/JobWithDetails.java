package info.jab.churrera.cli.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class containing a job with all its related prompts.
 */
public class JobWithDetails {
    private final Job job;
    private final List<Prompt> prompts;

    public JobWithDetails(Job job, List<Prompt> prompts) {
        this.job = job;
        this.prompts = new ArrayList<>(prompts);
    }

    public Job getJob() {
        return job;
    }

    public List<Prompt> getPrompts() {
        return new ArrayList<>(prompts);
    }
}

