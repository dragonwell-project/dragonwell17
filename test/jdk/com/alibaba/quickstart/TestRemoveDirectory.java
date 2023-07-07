/*
 * @test Remove non-empty directory
 * @library /test/lib
 * @library /lib/testlibrary
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *                                sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI TestRemoveDirectory
 */
import sun.hotspot.WhiteBox;

import java.io.File;

import static jdk.testlibrary.Asserts.assertFalse;

public class TestRemoveDirectory {
    private static String userPath = System.getProperty("user.dir");
    private static String dirName = "alibaba.quickstart.sharedcache";
    private static String dirPath = null;

    public static void main(String[] args) throws Exception {
        dirPath = userPath + "/" + dirName;
        verifyRemoveDirectory(dirPath);
    }

    public static void verifyRemoveDirectory(String dirPath) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();
        File file = new File(dirPath);
        if (file.exists()) {
            System.out.println("there is a directory " + dirPath);
            // delete it
            wb.removeDirectory(dirPath);
        }
        assertFalse(file.exists());

        System.out.println("create directory " + dirPath);
        file.mkdir();

        String newFile = dirPath + "/" + "tmpfile";
        File file2 = new File(newFile);
        file2.createNewFile();

        System.out.println("create new file " + newFile);
        wb.removeDirectory(dirPath);
        assertFalse(file.exists());
    }
}
