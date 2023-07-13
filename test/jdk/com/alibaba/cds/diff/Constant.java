public class Constant {

    public static final String INVALID_CLASS_KEYWORD_IN_LOG = " found in invalid classes list";
    public static final String EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG = "Successful loading of class ";

    public static JavaSource LOGGER = new JavaSource("Logger", "interface Logger {\n" +
            "        void log(String msg);\n" +
            "    }");
    public static JavaSource MY_LOG4J_LOGGER = new JavaSource("MyLog4jLogger", "public class MyLog4jLogger implements Logger {\n" +
            "        public void log(String msg) {\n" +
            "            System.out.println(\"[log4j]\" + msg);\n" +
            "        }\n" +
            "    }");
    public static JavaSource MY_LOGBACK_LOGGER = new JavaSource("MyLogbackLogger", "public class MyLogbackLogger implements Logger {\n" +
            "        public void log(String msg) {\n" +
            "            System.out.println(\"[logback]\" + msg);\n" +
            "        }\n" +
            "    }");
    public static JavaSource LOGGER_FACTORY = new JavaSource("LoggerFactory", "public class LoggerFactory {\n" +
            "        public static Logger getLogger() throws Exception {\n" +
            "            try {\n" +
            "                Class c = LoggerFactory.class.getClassLoader().loadClass(\"MyLog4jLogger\");\n" +
            "                return (Logger) c.getConstructor().newInstance();\n" +
            "            } catch (ClassNotFoundException e) {\n" +
            "                Class c1 = LoggerFactory.class.getClassLoader().loadClass(\"MyLogbackLogger\");\n" +
            "                return (Logger) c1.getConstructor().newInstance();\n" +
            "            }\n" +
            "        }\n" +
            "    }");
    public static JavaSource TEST_LOGGER_MAIN = new JavaSource("Main", "public class Main {\n" +
            "        public static void main(String[] args) throws Exception {\n" +
            "            LoggerFactory.getLogger().log(\"hello world\");\n" +
            "        }\n" +
            "    }");


    public static JavaSource DISCOUNT = new JavaSource("com.x.Discount", "package com.x;\n" +
            "public interface Discount {\n" +
            "       float discount();\n" +
            "}");
    public static JavaSource DISCOUNT_MAX = new JavaSource("com.x.DiscountMax", "package com.x;\n" +
            "public class DiscountMax implements Discount {\n" +
            "       public float discount() { return 0.5f; }\n" +
            "}");
    public static JavaSource DISCOUNT_MIN = new JavaSource("com.x.DiscountMin", "package com.x;\n" +
            "public class DiscountMin implements Discount {\n" +
            "       public float discount() { return 0.8f; }\n" +
            "}");
    public static JavaSource DISCOUNT_MAIN = new JavaSource("com.x.Main", "package com.x;\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        Discount d = null;\n" +
            "        try {\n" +
            "            d = (Discount)(Class.forName(\"com.x.DiscountMax\").getConstructor().newInstance());\n" +
            "        } catch (Exception e) {\n" +
            "            d = (Discount)(Class.forName(\"com.x.DiscountMin\").getConstructor().newInstance());\n" +
            "        }\n" +
            "        int price = 100;\n" +
            "        System.out.println(\"You need pay \" + (price*d.discount()));\n" +
            "    }\n" +
            "}");

    // CDS Dynamic diff related
    public static final String DYNAMIC_LOOKUP_SHARE_FAILED           = "Failed DynamicCDSCheck (lookup_shared)";
    public static final String DYNAMIC_SUPER_VERIFICATION_FAILED     = "Super verification failed: [";
    public static final String DYNAMIC_INTERFACE_VERIFICATION_FAILED = "Interface verification failed: [";

}
