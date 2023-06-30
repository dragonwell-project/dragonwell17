import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Classes4CDS {
    BufferedReader in;
    PrintStream out;

    static Classes4CDS instance = null;
    public static Classes4CDS getInstance() {
        if (instance == null) {
            instance = new Classes4CDS();
        }
        return instance;
    }

    private Classes4CDS() {}   // prevent created from outside.

    public void setInputStream(BufferedReader input) {
        in = input;
    }

    public void setOutputStream(PrintStream prt) {
        out = prt;
    }

    boolean status = true; // succeeded
    public boolean succeeded() { return status; }

    /*
       exclude: classname contains *$$Lambda$
                source contains "__JVM_DefineClass__"
       for classes loaded by bootloader, source is jrt:/java.*
       struct CDSKlass:  className(String)
                id(String)
                superId(String)
                interFaceIds(String[])
                source(String)

       ArrayList<CDSKlass) all: contains all the parsed CDS classes
       HashSet<id, intId>:
                id is string and unique, intId will be increased by 1;
                before output, replace id by intId;
       cond:    if source is jrt:/...  source set to null
                HashSet<id, className>
       convert id based on 1 (java/lang/Object)

     */

    int p1 = 0, p2 = 0;  // index for line1 and line2 position.

    class CDSData {
        public String className;
        public String id;
        public String superId;
        public List<String> interfaceIds;
        public String source;
        public String definingHash;
        public String initiatingHash;
        public String fingerprint;
        public CDSData(String name, String id, String superId, List<String> iids, String sourcePath, String definingHash, String fingerprint) {
            className = name;
            this.id = id;
            this.superId = superId;
            interfaceIds = iids;
            source = sourcePath;
            this.definingHash = definingHash;
            this.initiatingHash = null;
            this.fingerprint = fingerprint;
        }

        public CDSData(String name, String sourcePath, String initiatingHash) {
            className = name;
            this.id = null;
            this.superId = null;
            interfaceIds = null;
            source = sourcePath;
            this.definingHash = null;
            this.initiatingHash = initiatingHash;
            this.fingerprint = null;
        }

    }

    private String getKlassName(String line) {
        int index = 0;
        while (line.charAt(index) != ' ') index++;
        String name = line.substring(0, index);
        return name;
    }

    private String getKlassPath(String line) {
        int index = line.indexOf("source:");
        if (index == -1) {
            return null;
        }
        index += "source:".length();
        while(line.charAt(index) == ' ') index++;
        int start = index;
        while(line.charAt(index) != ' ') index++;
        String path = line.substring(start, index);
        return path;
    }

    private String getId(String line) throws Exception {
        int index = line.indexOf("klass: ");
        if (index == -1) {
            throw new Exception("no Id in line: " + line);
        }
        index += "klass: ".length();
        while(line.charAt(index) == ' ') index++;
        int start = index;
        while(index < line.length() && line.charAt(index) != ' ') index++;
        String id = line.substring(start, index);
        return id.trim();
    }

    private String getSuperId(String line) {
        int index = line.indexOf("super: ");
        index += "super: ".length();
        while(line.charAt(index) == ' ') index++;
        int start = index;
        while(index < line.length() && line.charAt(index) != ' ') index++;
        String superId = line.substring(start, index);
        return superId;
    }

    private List<String> getInterfaces(String line) {
        List<String> itfs = new ArrayList<String>();
        int index = line.indexOf("interfaces: ");
        if (index == -1) {
            return itfs;
        }
        index += "interfaces: ".length();
        while(line.charAt(index) == ' ') index++;

        String intfs;
        // find first alphabet
        int i;
        for (i = index; i < line.length(); i++) {
            if (Character.isAlphabetic(line.charAt(i)) || line.charAt(i) == '_') {
                break;
            }
        }
        if (i == line.length()) {   // no alphabet anymore
            intfs = line.substring(index).trim();
        } else {
            intfs = line.substring(index, i).trim();
        }

        String[] iids = intfs.split(" ");
        for(String s: iids) {
            itfs.add(s);
        }
        return itfs;
    }

    private String getDefiningLoaderHash(String line) {
        int index = line.indexOf("defining_loader_hash: ");
        if (index == -1)  return null;
        index += "defining_loader_hash: ".length();
        while(line.charAt(index) == ' ') index++;
        int start = index;
        while(index < line.length() && line.charAt(index) != ' ') index++;
        String hash = line.substring(start, index);
        return hash;
    }

    private String getInitiatingLoaderHash(String line) {
        int index = line.indexOf("initiating_loader_hash: ");
        if (index == -1)  return null;
        index += "initiating_loader_hash: ".length();
        while(line.charAt(index) == ' ') index++;
        int start = index;
        while(index < line.length() && line.charAt(index) != ' ') index++;
        String hash = line.substring(start, index);
        return hash;
    }

    private String getFingerprint(String line) {
        int index = line.indexOf("fingerprint: ");
        if (index == -1)  return null;
        index += "fingerprint: ".length();
        while(line.charAt(index) == ' ') index++;
        int start = index;
        while(index < line.length() && line.charAt(index) != ' ') index++;
        String name = line.substring(start, index);
        return name;
    }

    void printCDSData(CDSData data) {
        out.print(data.className); out.print(" ");
        out.print("id: " + data.id); out.print(" ");

        if (data.source != null && !data.source.contains("jrt:")) {
            if (data.superId != null) {
                out.print("super: " + data.superId);
                out.print(" ");
            }
            if (data.interfaceIds != null && data.interfaceIds.size() != 0) {
                out.print("interfaces: ");
                for (String s : data.interfaceIds) {
                    out.print(s);
                    out.print(" ");
                }
            }
            out.print("source: " + data.source); out.print(" ");
            System.out.println(data.source);
        }
        if (data.initiatingHash != null) {
            out.print("initiating_loader_hash: " + data.initiatingHash);
            out.print(" ");
        }
        if (data.definingHash != null) {
            out.print("defining_loader_hash: " + data.definingHash);
            out.print(" ");
        }
        if (data.fingerprint != null) {
            out.print("fingerprint: " + data.fingerprint);
            out.print(" ");
        }
        out.println();
    }

    private void decodeSource(CDSData data) throws Exception {
        if (data.source == null) {
            return;
        }
        String source = data.source;  // convenience
        if (source.contains("file:")) {
            // regular jar case
            // file:<dir/<main jar>
            int index = source.indexOf("file:");
            index += "file:".length();
            while(source.charAt(index) == ' ') {
                index++;
            }
            data.source = source.substring(index);
        }
    }

    private boolean invalidCheck(CDSData data) {
        if (data.source == null) {
            return false;
        }
        // appCDS
        if (data.definingHash == null) {
            if (eagerCDSSet.contains(data.className)) {
                return true;
            } else {
                appCDSSet.add(data.className);
            }
        } else {
            //eagerAppCDS
            if (data.initiatingHash == null) {
                return true;
            } else {
                if (appCDSSet.contains(data.className)) {
                    return true;
                } else {
                    eagerCDSSet.add(data.className);
                }
            }
        }
        return false;
    }

    HashMap<String, String> idIds = new HashMap<String, String>();
    HashMap<String, CDSData> nameidCDSData = new HashMap<String, CDSData>();
    HashMap<String, CDSData> notFoundCDSData = new HashMap<String, CDSData>();
    Set<String> eagerCDSSet = new HashSet<>();
    Set<String> appCDSSet = new HashSet<>();

    List<CDSData> all = new ArrayList<CDSData>();
    List<CDSData> allnotfound = new ArrayList<CDSData>();

    void run() {
        try {
            String line = in.readLine();
            while (!line.isEmpty()) {
                // skip the comment in classlist
                if(line.charAt(0) != '#') {
                    if (line.contains("defining_loader_hash:") && line.contains("initiating_loader_hash:")) {
                        String name = getKlassName(line);
                        String id = getId(line);
                        String definingHash = getDefiningLoaderHash(line);
                        String initiatingHash = getInitiatingLoaderHash(line);
                        CDSData oldData = nameidCDSData.get(name + id + definingHash);
                        if (oldData.initiatingHash == null) {
                            oldData.initiatingHash = initiatingHash;
                        }
                    } else if (line.contains("source: not.found.class")) {
                        String name = getKlassName(line);
                        String source = getKlassPath(line);
                        String initiatingHash = getInitiatingLoaderHash(line);
                        CDSData newData = new CDSData(name, source, initiatingHash);
                        allnotfound.add(newData);
                    } else {
                        String name = getKlassName(line);
                        String id = getId(line);
                        String source = getKlassPath(line);
                        String superId = getSuperId(line);
                        List<String> iids = getInterfaces(line);
                        String definingHash = getDefiningLoaderHash(line);
                        String fingerprint = getFingerprint(line);
                        CDSData newData = new CDSData(name, id, superId, iids, source, definingHash, fingerprint);
                        all.add(newData);
                        nameidCDSData.put(name + id + definingHash, newData);
                    }
                }
                line = in.readLine();
                if (line == null) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            status = false;
            return;
        }

        // Now replace Id with incremental numbers
        // First should be java/lang/Object
        System.out.println("Total class load: " + all.size());
        int klassID = 1;
        CDSData data = all.get(0);
        if (!data.className.equals("java/lang/Object")) {
            System.out.println("First should be java/lang/Object!");
            status = false;
            return;
        }
        data.superId = null;
        idIds.put(data.id, "1");
        data.id = String.valueOf(klassID);
        try {
            decodeSource(data);
        } catch (Exception e) {
            System.out.println("Error happened, Exception is " + e);
            e.printStackTrace();
            status = false;
            return;
        }
        printCDSData(data);
        for (int i = 1; i < all.size(); i++) {
            data = all.get(i);
            if (invalidCheck(data)) {
                continue;
            }
            String newId = String.valueOf(++klassID);
            idIds.put(data.id, newId);
            data.id = newId;
            String sp = idIds.get(data.superId);
            data.superId = sp;
            if (data.interfaceIds.size() != 0) {
                for (int j = 0; j < data.interfaceIds.size(); j++) {
                    String intf = data.interfaceIds.get(j);
                    String iid = idIds.get(intf);
                    data.interfaceIds.remove(j);
                    data.interfaceIds.add(j, iid);
                }
            }
            try {
                decodeSource(data);
            } catch (Exception e) {
                System.out.println("Error happened, Exception is " + e);
                e.printStackTrace();
                status = false;
                return;
            }
            printCDSData(data);
        }
        for (int i = 0; i < allnotfound.size(); i++) {
            data = allnotfound.get(i);
            String name = data.className;
            if (appCDSSet.contains(name)) {
                continue;
            }
            String newId = String.valueOf(++klassID);
            data.id = newId;
            printCDSData(data);
        }
    }

    public static void main(String... args) throws Exception {
        if (args.length != 2) {
            printHelp();
        }

        File f  = new File(args[0]);
        if (!f.exists()) {
            System.out.println("Non exists input file: " + args[0]);
            return;
        }
        BufferedReader in = new BufferedReader(new FileReader(f));
        // clean file content
        PrintWriter pw = new PrintWriter(args[1]);
        pw.close();
        PrintStream out   = new PrintStream(args[1]);
        Classes4CDS cds = Classes4CDS.getInstance();
        cds.setInputStream(in);
        cds.setOutputStream(out);
        cds.run();
        if (cds.status) {
            System.out.println("Succeeded!");
        } else {
            System.out.println("Failed!");
            System.exit(-1);
        }
    }
    public static void printHelp() {
        System.out.println("Usage: ");
        System.out.println(" <input file> <output file>");
        System.exit(0);
    }
}
