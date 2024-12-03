package com.uber.nullaway;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.Lists;
import com.google.errorprone.DiagnosticTestHelper;
import com.google.errorprone.ErrorProneJavaCompiler;
import com.google.errorprone.FileManagers;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.file.JavacFileManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("UnusedVariable")
public class PatchInPlaceTest {

  @Rule public final TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void patchWithBugPatternCustomization() throws IOException {
    // Patching modifies files on disk, so we must create an actual file that matches the
    // `SimpleJavaFileObject` defined below.
    Path location = tempDir.getRoot().toPath().resolve("StringConstantWrapper.java");
    String source = "class StringConstantWrapper {\n" + "  String s = \"old-value\";\n" + "}\n";
    Files.writeString(location, source);

    CompilationResult result =
        doCompile(
            Collections.singleton(
                new SimpleJavaFileObject(location.toUri(), SimpleJavaFileObject.Kind.SOURCE) {
                  @Override
                  public String getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                  }
                }),
            Arrays.asList(
                "-XepPatchChecks:NullAway",
                "-XepPatchLocation:IN_PLACE",
                "-XepOpt:NullAway:AnnotatedPackages=com.uber"),
            Collections.singletonList(NullAway.class));
    System.err.println(result.output);
    assertThat(result.succeeded).isTrue();
    assertThat(Files.readString(location))
        .isEqualTo("class StringConstantWrapper {\n" + "  String s = \"old-value\";\n" + "}\n");
  }

  private CompilationResult doCompile(
      Iterable<? extends JavaFileObject> files,
      List<String> extraArgs,
      List<Class<? extends BugChecker>> customCheckers) {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, UTF_8), true);
    JavacFileManager fileManager = FileManagers.testFileManager();

    List<String> args = Lists.newArrayList("-d", tempDir.getRoot().getAbsolutePath(), "-proc:none");
    args.addAll(extraArgs);

    JavaCompiler errorProneJavaCompiler =
        customCheckers.isEmpty()
            ? new ErrorProneJavaCompiler()
            : new ErrorProneJavaCompiler(ScannerSupplier.fromBugCheckerClasses(customCheckers));
    JavaCompiler.CompilationTask task =
        errorProneJavaCompiler.getTask(
            printWriter, fileManager, diagnosticHelper.collector, args, null, files);

    return new CompilationResult(
        task.call(), new String(outputStream.toByteArray(), UTF_8), diagnosticHelper);
  }

  private static class CompilationResult {
    public final boolean succeeded;
    public final String output;
    public final DiagnosticTestHelper diagnosticHelper;

    public CompilationResult(
        boolean succeeded, String output, DiagnosticTestHelper diagnosticHelper) {
      this.succeeded = succeeded;
      this.output = output;
      this.diagnosticHelper = diagnosticHelper;
    }
  }
}
