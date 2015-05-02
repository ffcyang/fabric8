/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Generates the Java source for mapping kinds to classes from the generated Kubernetes schema
 */
public class GenerateKindToClassMap {
    public static void main(String[] args) throws Exception {
        ClassPath classPath = ClassPath.from(GenerateKindToClassMap.class.getClassLoader());
        SortedMap<String, String> sortedMap = new TreeMap<>();
        String[] topLevelPackages = {"io.fabric8.kubernetes.api.model", "io.fabric8.openshift.api.model"};
        for (String topLevelPackage : topLevelPackages) {
            ImmutableSet<ClassPath.ClassInfo> classInfos = classPath.getTopLevelClassesRecursive(topLevelPackage);
            for (ClassPath.ClassInfo classInfo : classInfos) {
                String simpleName = classInfo.getSimpleName();
                if (simpleName.endsWith("Builder") || simpleName.endsWith("Fluent")) {
                    continue;
                }
                sortedMap.put(simpleName, classInfo.getName());
            }
        }
        String basedir = System.getProperty("basedir", ".");
        File file = new File(basedir, "src/main/java/io/fabric8/kubernetes/api/support/KindToClassMapping.java");
        file.getParentFile().mkdirs();
        System.out.println("Generating " + file);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            Set<Map.Entry<String, String>> entries = sortedMap.entrySet();
            writer.println("/**\n" +
                    " * Licensed to the Apache Software Foundation (ASF) under one or more\n" +
                    " * contributor license agreements.  See the NOTICE file distributed with\n" +
                    " * this work for additional information regarding copyright ownership.\n" +
                    " * The ASF licenses this file to You under the Apache License, Version 2.0\n" +
                    " * (the \"License\"); you may not use this file except in compliance with\n" +
                    " * the License.  You may obtain a copy of the License at\n" +
                    " * <p/>\n" +
                    " * http://www.apache.org/licenses/LICENSE-2.0\n" +
                    " * <p/>\n" +
                    " * Unless required by applicable law or agreed to in writing, software\n" +
                    " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    " * See the License for the specific language governing permissions and\n" +
                    " * limitations under the License.\n" +
                    " */\n" +
                    "package io.fabric8.kubernetes.api.support;\n" +
                    "\n");
            for (Map.Entry<String, String> entry : entries) {
                writer.println("import " + entry.getValue() + ";");
            }

            writer.println("\n" +
                    "import java.util.HashMap;\n" +
                    "import java.util.Map;\n" +
                    "\n" +
                    "/**\n" +
                    " * Maps the Kubernetes kinds to the Jackson DTO classes\n" +
                    " */\n" +
                    "public class KindToClassMapping {\n" +
                    "    private static Map<String,Class<?>> map = new HashMap<>();\n" +
                    "\n" +
                    "    static {");


            for (Map.Entry<String, String> entry : entries) {
                String kind = entry.getKey();
                String className = entry.getValue();
                writer.println("        map.put(\"" + kind + "\", " + kind + ".class);");
            }

            writer.println("    }\n" +
                    "\n" +
                    "    public static Map<String,Class<?>> getKindToClassMap() {\n" +
                    "        return map;\n" +
                    "    }\n" +
                    "}\n");
        }
    }
}
