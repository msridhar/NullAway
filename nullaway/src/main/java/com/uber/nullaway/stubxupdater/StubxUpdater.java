package com.uber.nullaway.stubxupdater;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.FileOfClasses;
import com.uber.nullaway.handlers.StubxCacheUtil;
import com.uber.nullaway.libmodel.MethodAnnotationsRecord;
import com.uber.nullaway.libmodel.StubxWriter;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import org.jspecify.annotations.Nullable;

public class StubxUpdater {

  private static final String DEFAULT_EXCLUSIONS = "org\\/objectweb\\/asm\\/.*";

  /**
   * First arg is path to android.jar, second arg is path to stubx file, third arg is path to output
   */
  public static void main(String[] args) throws ClassHierarchyException, IOException {
    if (args.length != 3) {
      System.err.println(
          "Usage: StubxUpdater <path to android.jar> <path to stubx file> <path to output>");
      System.exit(1);
    }
    String androidJarPath = args[0];
    String stubxPath = args[1];
    String outputPath = args[2];
    IClassHierarchy cha = getCHAForAndroidJar(androidJarPath);
    System.err.println(cha.getNumberOfClasses() + " classes");
    //    for (IClass klass : cha) {
    //      if (klass.getClassLoader().getName().equals(ClassLoaderReference.Application.getName()))
    // {
    //        System.err.println(klass);
    //      }
    //    }
    StubxCacheUtil cacheUtil = new StubxCacheUtil("WALA");
    cacheUtil.parseStubStream(new FileInputStream(stubxPath), stubxPath);
    Map<String, Map<String, Map<Integer, Set<String>>>> argAnnotCache =
        cacheUtil.getArgAnnotCache();
    System.err.println(argAnnotCache.size());
    Map<String, MethodAnnotationsRecord> methodRecords = new LinkedHashMap<>();
    for (String className : argAnnotCache.keySet()) {
      IClass klass =
          cha.lookupClass(
              TypeReference.findOrCreate(
                  ClassLoaderReference.Application,
                  StringStuff.deployment2CanonicalTypeString(className)));
      if (klass == null) {
        continue;
      }
      Map<String, Map<Integer, Set<String>>> recordsForClass = argAnnotCache.get(className);
      for (String methodSig : recordsForClass.keySet()) {
        Map<Integer, Set<String>> annots = recordsForClass.get(methodSig);
        String newMethodSig = convertMethodSig(methodSig, klass);
        Set<String> methodAnnots = new HashSet<>();
        Map<Integer, ImmutableSet<String>> resultArgAnnots = new LinkedHashMap<>();
        for (Map.Entry<Integer, Set<String>> entry : annots.entrySet()) {
          Integer argNum = entry.getKey();
          Set<String> argAnnots = entry.getValue();
          if (argNum == -1) { // return
            methodAnnots.addAll(argAnnots);
          } else {
            resultArgAnnots.put(argNum, ImmutableSet.copyOf(argAnnots));
          }
        }
        MethodAnnotationsRecord record =
            MethodAnnotationsRecord.create(
                ImmutableSet.copyOf(methodAnnots), ImmutableMap.copyOf(resultArgAnnots));
        methodRecords.put(newMethodSig, record);
      }
    }
    ImmutableMap<String, String> importedAnnotations =
        ImmutableMap.<String, String>builder()
            .put("Nonnull", "javax.annotation.Nonnull")
            .put("Nullable", "javax.annotation.Nullable")
            .build();
    try (DataOutputStream dataOutputStream =
        new DataOutputStream(
            new FileOutputStream(Paths.get(outputPath, "output.astubx").toFile()))) {
      StubxWriter.write(
          dataOutputStream,
          importedAnnotations,
          ImmutableMap.of(),
          ImmutableMap.of(),
          methodRecords,
          ImmutableSet.of(),
          ImmutableMap.of());
    }
    //        StubxParser parser = new StubxParser(androidJarPath);
    //        parser.parseStubx(stubxPath, outputPath);
  }

  private static String convertMethodSig(String input, IClass klass) {
    // Split into enclosing class and method part
    String[] parts = input.split(":");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid format");
    }

    // Extract method signature part
    String methodSignature = parts[1].trim();

    // Extract return type and method name with arguments
    String returnTypeAndMethod = methodSignature.substring(0, methodSignature.indexOf('(')).trim();
    String argumentsPart =
        methodSignature
            .substring(methodSignature.indexOf('(') + 1, methodSignature.indexOf(')'))
            .trim();

    // Extract return type and method name
    String[] returnTypeAndMethodParts = returnTypeAndMethod.split(" ");
    if (returnTypeAndMethodParts.length != 2) {
      throw new IllegalArgumentException("Invalid return type or method format");
    }

    //    String returnType = returnTypeAndMethodParts[0].trim();
    String methodName = returnTypeAndMethodParts[1].trim();

    // Split the arguments by commas
    String[] argumentTypes = argumentsPart.isEmpty() ? new String[0] : argumentsPart.split(",");
    List<String> arguments = new ArrayList<>();
    for (String argument : argumentTypes) {
      arguments.add(argument.trim());
    }
    IMethod resolvedMethod = resolveMethod(klass, methodName, arguments);
    return convertIMethodToMethodSig(resolvedMethod);
  }

  private static String convertIMethodToMethodSig(IMethod unused) {
    return null;
  }

  private static @Nullable IMethod resolveMethod(
      IClass klass, String methodName, List<String> argumentTypes) {
    if (methodName.contains("$")) {
      return null;
    }
    for (IMethod method : klass.getDeclaredMethods()) {
      if (method.getName().toString().equals(methodName)) {
        boolean isStatic = method.isStatic();
        int expectedNumArgs =
            isStatic ? method.getNumberOfParameters() : method.getNumberOfParameters() - 1;
        if (expectedNumArgs == argumentTypes.size()) {
          boolean match = true;
          for (int i = 0; i < argumentTypes.size(); i++) {
            int argInd = isStatic ? i : i + 1;
            TypeReference parameterType = method.getParameterType(argInd);
            String curArgTypeStr = argumentTypes.get(i);
            if (curArgTypeStr.equals("Array") && parameterType.isArrayType()) {
              continue;
            }
            String parameterTypeName =
                StringStuff.jvmToReadableType(parameterType.getName().toString());
            if (!parameterType.isPrimitiveType()) {
              parameterTypeName =
                  parameterTypeName.substring(parameterTypeName.lastIndexOf('.') + 1);
            }
            if (!parameterTypeName.equals(curArgTypeStr)) {
              match = false;
              break;
            }
          }
          if (match) {
            return method;
          }
        }
      }
    }
    System.err.println(
        "Method not found: "
            + methodName
            + "("
            + String.join(", ", argumentTypes)
            + ") in class "
            + klass.getName());
    return null;
  }

  private static IClassHierarchy getCHAForAndroidJar(String androidJarPath)
      throws IOException, ClassHierarchyException {
    AnalysisScope scope = AnalysisScopeReader.instance.makeBasePrimordialScope(null);
    scope.setExclusions(
        new FileOfClasses(
            new ByteArrayInputStream(DEFAULT_EXCLUSIONS.getBytes(StandardCharsets.UTF_8))));
    // JarInputStream jarIS = new JarInputStream(new FileInputStream(androidJarPath));
    scope.addToScope(ClassLoaderReference.Application, new JarFile(androidJarPath));
    //    AnalysisOptions options = new AnalysisOptions(scope, null);
    //    AnalysisCache cache = new AnalysisCacheImpl();
    IClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
    Warnings.clear();
    return cha;
  }
}
