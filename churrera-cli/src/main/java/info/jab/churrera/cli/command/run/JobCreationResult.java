package info.jab.churrera.cli.command.run;

import java.util.List;

/**
 * Result of job creation operation.
 */
public class JobCreationResult {
    private final boolean success;
    private final String jobId;
    private final List<String> errors;

    private JobCreationResult(boolean success, String jobId, List<String> errors) {
        this.success = success;
        this.jobId = jobId;
        this.errors = errors;
    }

    /**
     * Creates a successful result.
     *
     * @param jobId the created job ID
     * @return successful result
     */
    public static JobCreationResult success(String jobId) {
        return new JobCreationResult(true, jobId, List.of());
    }

    /**
     * Creates a failed result.
     *
     * @param errors list of error messages
     * @return failed result
     */
    public static JobCreationResult failure(List<String> errors) {
        return new JobCreationResult(false, null, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getJobId() {
        return jobId;
    }

    public List<String> getErrors() {
        return errors;
    }
}

