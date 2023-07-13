public enum QuickStartFeature {
    EAGER_APPCDS("", new String[]{"-XX:+IgnoreAppCDSDirCheck", "-Xlog:class+eagerappcds=trace"}),
    AOT("-eagerappcds,-appcds,+aot", new String[]{"-Dcom.alibaba.quickstart.aot.compilethreads=1"}),
    FULL("+eagerappcds,+aot,+jarindex", new String[]{"-Dcom.alibaba.quickstart.aot.tieredDisabled=true", "-XX:+IgnoreAppCDSDirCheck", "-Xlog:class+eagerappcds=trace"});
    String quickstartSubOption;
    String[] appendixOption;

    QuickStartFeature(String quickstartSubOption, String[] appendixOption) {
        this.quickstartSubOption = quickstartSubOption;
        this.appendixOption = appendixOption;
    }

    public String getQuickstartSubOption() {
        return quickstartSubOption;
    }

    public String[] getAppendixOption() {
        return appendixOption;
    }
}
