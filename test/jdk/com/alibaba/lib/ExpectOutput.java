public class ExpectOutput {
    private final String[] expectLines;
    private final String[] shouldNotContainLines;

    public ExpectOutput(String[] expectLines) {
        this.expectLines = expectLines;
        this.shouldNotContainLines = null;
    }

    public ExpectOutput(String[] expectLines, String[] shouldNotContainLines) {
        this.expectLines = expectLines;
        this.shouldNotContainLines = shouldNotContainLines;
    }

    public String[] getExpectLines() {
        return expectLines;
    }

    public String[] getShouldNotContainLines() {
        return shouldNotContainLines;
    }
}
