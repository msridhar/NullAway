package com.uber.nullaway.stubxupdater;

import com.ibm.wala.classLoader.IClass;
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
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;

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
    //    String outputPath = args[2];
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
    System.err.println(cacheUtil.getArgAnnotCache().size());
    for (String className : cacheUtil.getArgAnnotCache().keySet()) {
      IClass klass =
          cha.lookupClass(
              TypeReference.findOrCreate(
                  ClassLoaderReference.Application,
                  StringStuff.deployment2CanonicalTypeString(className)));
      if (klass != null) {
        System.err.println(klass);
      } else {
        System.err.println("Could not find class: " + className);
      }
    }
    //        StubxParser parser = new StubxParser(androidJarPath);
    //        parser.parseStubx(stubxPath, outputPath);
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
