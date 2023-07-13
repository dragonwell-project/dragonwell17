public abstract class PairProjectProvider {

    // whether we are a CDS static diff or dynamic diff
    public boolean isStaticDiff;

    public PairProjectProvider(String arg) {
        this.isStaticDiff = determineStaticOrDynamic(arg);
    }

    public static boolean determineStaticOrDynamic(String arg) {
        if (arg.equals("static")) {
            return true;
        } else if (arg.equals("dynamic")) {
            return false;
        } else {
            throw new Error("Should not reach here");
        }
    }

    public abstract Project versionA();

    public abstract Project versionB();
}
