package com.uber.nullaway;

import com.google.errorprone.CompilationTestHelper;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WarnOnUnneededSuppressionsTest extends NullAwayTestsBase {

  private CompilationTestHelper compilationTestHelper;

  @Before
  public void setupTestHelper() {
    compilationTestHelper =
        makeTestHelperWithArgs(
                Arrays.asList(
                    "-d",
                    temporaryFolder.getRoot().getAbsolutePath(),
                    "-XepWarnOnUnneededSuppressions",
                    "-XepOpt:NullAway:OnlyNullMarked=true"))
            .matchAllDiagnostics();
  }

  @Test
  public void classSuppression() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "@SuppressWarnings(\"NullAway\")",
            "// BUG: Diagnostic contains: Unnecessary @SuppressWarnings(\"NullAway\")",
            "class Test {}")
        .doTest();
  }

  @Test
  public void fieldInitNegativeClassAnnot() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "@SuppressWarnings(\"NullAway\")",
            "class Test {",
            "  Object f;",
            "}")
        .doTest();
  }

  @Test
  public void fieldInitNegativeFieldAnnot() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  @SuppressWarnings(\"NullAway\")",
            "  Object f;",
            "}")
        .doTest();
  }
}
