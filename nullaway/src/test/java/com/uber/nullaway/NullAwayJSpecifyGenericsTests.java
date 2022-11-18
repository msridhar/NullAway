package com.uber.nullaway;

import com.google.errorprone.CompilationTestHelper;
import java.util.Arrays;
import org.junit.Test;

public class NullAwayJSpecifyGenericsTests extends NullAwayTestsBase {

  @Test
  public void genericsChecksForAssignments() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.nullness.Nullable;",
            "class Test {",
            " static class NullableTypeParam<E extends @Nullable Object> {}",
            " static class NullableTypeParamMultipleArguments<E1 extends @Nullable Object, E2> {}",
            " static class NullableTypeParamMultipleArgumentsNested<E1 extends @Nullable Object, E2, E3 extends @Nullable Object> {}",
            " static NullableTypeParam<@Nullable String> testOKOtherAnnotation(NullableTypeParam<String> t) {",
            "       NullableTypeParam<String> t3;",
            "       // BUG: Diagnostic contains: Generic type parameter",
            "        t3 = new NullableTypeParam<@Nullable String>();",
            "        NullableTypeParam<@Nullable String> t4;",
            "       // BUG: Diagnostic contains: Generic type parameter",
            "        t4 = t;",
            "       NullableTypeParamMultipleArguments<String, String> t5 = new NullableTypeParamMultipleArguments<String, String>();",
            "       NullableTypeParamMultipleArguments<@Nullable String, String> t6 = new NullableTypeParamMultipleArguments<@Nullable String, String>();",
            "       // BUG: Diagnostic contains: Generic type parameter",
            "       t5 = t6;",
            "       NullableTypeParam<NullableTypeParam<NullableTypeParam<@Nullable String>>> t7 = new NullableTypeParam<NullableTypeParam<NullableTypeParam<@Nullable String>>>();",
            "       NullableTypeParam<NullableTypeParam<NullableTypeParam<String>>> t8 = new NullableTypeParam<NullableTypeParam<NullableTypeParam<String>>>();",
            "       // BUG: Diagnostic contains: Generic type parameter",
            "       t7 = t8;",
            "       NullableTypeParam<NullableTypeParam<NullableTypeParam<@Nullable String>>> t9 = new  NullableTypeParam<NullableTypeParam<NullableTypeParam<@Nullable String>>> ();",
            "       //No error",
            "       t7 = t9;",
            "       NullableTypeParamMultipleArguments<NullableTypeParam<NullableTypeParam<@Nullable String>>, String> t10 = new  NullableTypeParamMultipleArguments<NullableTypeParam<NullableTypeParam<@Nullable String>>, String> ();",
            "       NullableTypeParamMultipleArguments<NullableTypeParam<NullableTypeParam<String>>, String> t11 = new  NullableTypeParamMultipleArguments<NullableTypeParam<NullableTypeParam<String>>, String> ();",
            "       // BUG: Diagnostic contains: Generic type parameter",
            "       t10 = t11;",
            "       NullableTypeParamMultipleArgumentsNested<NullableTypeParam<NullableTypeParam<@Nullable String>>, String, @Nullable String> t12 = new  NullableTypeParamMultipleArgumentsNested<NullableTypeParam<NullableTypeParam<@Nullable String>>, String, @Nullable String> ();",
            "       NullableTypeParamMultipleArgumentsNested<NullableTypeParam<NullableTypeParam<String>>, String, @Nullable String> t13 = new  NullableTypeParamMultipleArgumentsNested<NullableTypeParam<NullableTypeParam<String>>, String, @Nullable String>  ();",
            "       // BUG: Diagnostic contains: Generic type parameter",
            "       t12 = t13;",
            "       return t;",
            "    }",
            " static void callOtherMethod() {",
            "      NullableTypeParam<String> t1 = new NullableTypeParam<String>();",
            "      NullableTypeParam<String> t2 = null; ",
            "       // BUG: Diagnostic contains: Generic type parameter",
            "      t2 = testOKOtherAnnotation(t1);",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void testObjectAssignments() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.nullness.Nullable;",
            "class NullableTypeParam<E extends @Nullable Object> {}",
            "class TestClass1 { ",
            " public NullableTypeParam<String> t1;",
            " TestClass1() {",
            "   // BUG: Diagnostic contains: Generic type parameter",
            "   this.t1 = new NullableTypeParam<@Nullable String>();",
            " }",
            "}",
            "class TestClass2 {",
            "  TestClass2() {",
            "    TestClass1 object = new TestClass1();",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    object.t1 = new NullableTypeParam<@Nullable String>();",
            "    Object t2 = new NullableTypeParam<@Nullable String>();",
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
