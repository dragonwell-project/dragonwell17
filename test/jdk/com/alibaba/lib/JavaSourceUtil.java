import java.util.ArrayList;
import java.util.List;

public class JavaSourceUtil {
    public static JavaSource[] updateMethod(JavaSource[] baseSource, String targetClass, String targetMethodId, String targetMethodBody, String[] imports) {
        JavaSource[] newSources = new JavaSource[baseSource.length];
        boolean found = false;
        for (int i = 0; i < baseSource.length; i++) {
            JavaSource javaSource = baseSource[i];
            if (javaSource.getClassName().equals(targetClass)) {
                for (JavaSource.MethodDesc m : javaSource.getMethods()) {
                    if (m.getId().equals(targetMethodId)) {
                        found = true;
                        newSources[i] = new JavaSource(javaSource.getClassName(), javaSource.getDeclare(),
                                mergeImports(javaSource.getImports(), imports),
                                javaSource.getFields(),
                                mergeMethod(m, targetMethodBody, javaSource.getMethods()));
                        break;
                    }
                }
                if (!found) {
                    throw new RuntimeException("Not found class: " + targetClass + " with method id : " + targetMethodId + " for update!");
                }
            } else {
                newSources[i] = javaSource;
            }
        }
        return newSources;
    }

    public static JavaSource[] updateClass(JavaSource[] baseSource, String targetClass, String newClassSource) {
        JavaSource[] newSources = new JavaSource[baseSource.length];
        boolean found = false;
        for (int i = 0; i < baseSource.length; i++) {
            JavaSource javaSource = baseSource[i];
            if (javaSource.getClassName().equals(targetClass)) {
                found = true;
                newSources[i] = new JavaSource(targetClass, newClassSource);
            } else {
                newSources[i] = javaSource;
            }
        }
        if (!found) {
            throw new RuntimeException("Not found class: " + targetClass + " for update!");
        }
        return newSources;
    }

    private static List<JavaSource.MethodDesc> mergeMethod(JavaSource.MethodDesc toReplace, String targetMethodBody, List<JavaSource.MethodDesc> methods) {
        List<JavaSource.MethodDesc> newMethods = new ArrayList<>();
        for (JavaSource.MethodDesc m : methods) {
            if (m == toReplace) {
                newMethods.add(new JavaSource.MethodDesc(toReplace.getId(), targetMethodBody));
            } else {
                newMethods.add(m);
            }
        }
        return newMethods;
    }

    private static List<String> mergeImports(List<String> imports, String[] addImports) {
        if (null == addImports || addImports.length == 0) {
            return imports;
        } else {
            List<String> newImports = new ArrayList<>(imports);
            for (String newImport : addImports) {
                newImports.add(newImport);
            }
            return newImports;
        }
    }
}
