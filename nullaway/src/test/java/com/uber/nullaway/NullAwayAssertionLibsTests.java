package com.uber.nullaway;

import java.util.Arrays;
import org.junit.Test;

public class NullAwayAssertionLibsTests extends NullAwayTestsBase {

  @Test
  public void supportTruthAssertThatIsNotNull_Object() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static com.google.common.truth.Truth.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object o) {",
            "    assertThat(o).isNotNull();",
            "    o.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supportTruthAssertThatIsNotNull_String() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static com.google.common.truth.Truth.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable String s) {",
            "    assertThat(s).isNotNull();",
            "    s.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supportTruthAssertThatIsNotNull_MapKey() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static com.google.common.truth.Truth.assertThat;",
            "",
            "import java.util.Map;",
            "",
            "class Test {",
            "  private void foo(Map<String, Object> m) {",
            "    assertThat(m.get(\"foo\")).isNotNull();",
            "    m.get(\"foo\").toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doNotSupportTruthAssertThatWhenDisabled() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=false"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static com.google.common.truth.Truth.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object a) {",
            "    assertThat(a).isNotNull();",
            "    // BUG: Diagnostic contains: dereferenced expression",
            "    a.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supportHamcrestAssertThatMatchersIsNotNull() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.hamcrest.MatcherAssert.assertThat;",
            "import static org.hamcrest.Matchers.*;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object a, @Nullable Object b) {",
            "    assertThat(a, is(notNullValue()));",
            "    a.toString();",
            "    assertThat(b, is(not(nullValue())));",
            "    b.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doNotSupportHamcrestAssertThatWhenDisabled() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=false"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.hamcrest.MatcherAssert.assertThat;",
            "import static org.hamcrest.Matchers.*;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object a) {",
            "    assertThat(a, is(notNullValue()));",
            "    // BUG: Diagnostic contains: dereferenced expression",
            "    a.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supportHamcrestAssertThatCoreMatchersIsNotNull() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.hamcrest.CoreMatchers.*;",
            "import static org.hamcrest.MatcherAssert.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object a, @Nullable Object b) {",
            "    assertThat(a, is(notNullValue()));",
            "    a.toString();",
            "    assertThat(b, is(not(nullValue())));",
            "    b.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supportHamcrestAssertThatCoreIsNotNull() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.hamcrest.CoreMatchers.*;",
            "import static org.hamcrest.MatcherAssert.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "import org.hamcrest.core.IsNull;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object a, @Nullable Object b) {",
            "    assertThat(a, is(IsNull.notNullValue()));",
            "    a.toString();",
            "    assertThat(b, is(not(IsNull.nullValue())));",
            "    b.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supportJunitAssertThatIsNotNull_Object() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.hamcrest.Matchers.*;",
            "import static org.junit.Assert.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object a, @Nullable Object b) {",
            "    assertThat(a, is(notNullValue()));",
            "    a.toString();",
            "    assertThat(b, is(not(nullValue())));",
            "    b.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doNotSupportJunitAssertThatWhenDisabled() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=false"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.hamcrest.Matchers.*;",
            "import static org.junit.Assert.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object a) {",
            "    assertThat(a, is(notNullValue()));",
            "    // BUG: Diagnostic contains: dereferenced expression",
            "    a.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supportAssertJAssertThatIsNotNull_Object() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object o) {",
            "    assertThat(o).isNotNull();",
            "    o.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supportAssertJAssertThatIsNotNull_String() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable String s) {",
            "    assertThat(s).isNotNull();",
            "    s.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supportAssertJAssertThatIsNotNull_MapKey() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=true"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "",
            "import java.util.Map;",
            "",
            "class Test {",
            "  private void foo(Map<String, Object> m) {",
            "    assertThat(m.get(\"foo\")).isNotNull();",
            "    m.get(\"foo\").toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doNotSupportAssertJAssertThatWhenDisabled() {
    makeTestHelperWithArgs(
            Arrays.asList(
                "-d",
                temporaryFolder.getRoot().getAbsolutePath(),
                "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                "-XepOpt:NullAway:HandleTestAssertionLibraries=false"))
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "",
            "import static org.assertj.core.api.Assertions.assertThat;",
            "",
            "import javax.annotation.Nullable;",
            "",
            "class Test {",
            "  private void foo(@Nullable Object a) {",
            "    assertThat(a).isNotNull();",
            "    // BUG: Diagnostic contains: dereferenced expression",
            "    a.toString();",
            "  }",
            "}")
        .doTest();
  }
}
