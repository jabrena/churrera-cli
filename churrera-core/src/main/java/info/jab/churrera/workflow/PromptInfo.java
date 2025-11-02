package info.jab.churrera.workflow;

/**
 * Data class containing information about a single prompt.
 */
public class PromptInfo {
    private final String srcFile;
    private final String type;
    private final String bindResultExp;

    public PromptInfo(String srcFile, String type) {
        this(srcFile, type, null);
    }

    public PromptInfo(String srcFile, String type, String bindResultExp) {
        this.srcFile = srcFile;
        this.type = type;
        this.bindResultExp = bindResultExp;
    }

    public String getSrcFile() {
        return srcFile;
    }

    public String getType() {
        return type;
    }

    public String getBindResultExp() {
        return bindResultExp;
    }

    public boolean hasBindResultExp() {
        return bindResultExp != null && !bindResultExp.trim().isEmpty();
    }

    public boolean isPml() {
        return "pml".equalsIgnoreCase(type);
    }
}

