package com.uber.nullaway;

import com.google.errorprone.CompilationTestHelper;
import java.util.Arrays;
import org.junit.Test;

public class NullAwayJSpecifyGenericMethodsTests extends NullAwayTestsBase {

  @Test
  public void basic() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class C<T extends @Nullable Object> {}",
            "  interface A<T1 extends @Nullable Object> {",
            "    String function(T1 o);",
            "  }",
            "  static class Util {",
            "    static <U extends @Nullable Object> void match(C<U> c, A<U> a) {}",
            "  }",
            "  static void testPositive1(C<@Nullable String> c, A<String> a) {",
            "    // BUG: diagnostic contains: something",
            "    Util.match(c, a);",
            "  }",
            "  static void testNegative1(C<@Nullable String> c, A<@Nullable String> a) {",
            "    Util.match(c, a);",
            "  }",
            "}")
        .doTest();
  }

  private CompilationTestHelper makeHelper() {
    return makeTestHelperWithArgs(
        Arrays.asList(
            "-XepOpt:NullAway:AnnotatedPackages=com.uber", "-XepOpt:NullAway:JSpecifyMode=true"));
  }
}
