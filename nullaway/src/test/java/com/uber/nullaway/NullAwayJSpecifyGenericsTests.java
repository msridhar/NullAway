package com.uber.nullaway;

import com.google.errorprone.CompilationTestHelper;
import java.util.Arrays;
import org.junit.Test;

public class NullAwayJSpecifyGenericsTests extends NullAwayTestsBase {

  @Test
  public void basicTypeParamInstantiation() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class NonNullTypeParam<E> {}",
            "  static class NullableTypeParam<E extends @Nullable Object> {}",
            "  // BUG: Diagnostic contains: Generic type parameter",
            "  static void testBadNonNull(NonNullTypeParam<@Nullable String> t1) {",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    NonNullTypeParam<@Nullable String> t2 = null;",
            "    NullableTypeParam<@Nullable String> t3 = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructorTypeParamInstantiation() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class NonNullTypeParam<E> {}",
            "  static class NullableTypeParam<E extends @Nullable Object> {}",
            "  static void testOkNonNull(NonNullTypeParam<String> t) {",
            "    NonNullTypeParam<String> t2 = new NonNullTypeParam<String>();",
            "  }",
            "  static void testBadNonNull(NonNullTypeParam<String> t) {",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    NonNullTypeParam<String> t2 = new NonNullTypeParam<@Nullable String>();",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    testBadNonNull(new NonNullTypeParam<@Nullable String>());",
            "  }",
            "  static void testOkNullable(NullableTypeParam<String> t1, NullableTypeParam<@Nullable String> t2) {",
            "    NullableTypeParam<String> t3 = new NullableTypeParam<String>();",
            "    NullableTypeParam<@Nullable String> t4 = new NullableTypeParam<@Nullable String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void multipleTypeParametersInstantiation() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class MixedTypeParam<E1, E2 extends @Nullable Object, E3 extends @Nullable Object, E4> {}",
            "  static class PartiallyInvalidSubclass",
            "      // BUG: Diagnostic contains: Generic type parameter",
            "      extends MixedTypeParam<@Nullable String, String, String, @Nullable String> {}",
            "  static class ValidSubclass1",
            "      extends MixedTypeParam<String, @Nullable String, @Nullable String, String> {}",
            "  static class PartiallyInvalidSubclass2",
            "      extends MixedTypeParam<",
            "          String,",
            "          String,",
            "          String,",
            "          // BUG: Diagnostic contains: Generic type parameter",
            "          @Nullable String> {}",
            "  static class ValidSubclass2 extends MixedTypeParam<String, String, String, String> {}",
            "}")
        .doTest();
  }

  @Test
  public void subClassTypeParamInstantiation() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class NonNullTypeParam<E> {}",
            "  static class NullableTypeParam<E extends @Nullable Object> {}",
            "  static class SuperClassForValidSubclass {",
            "    static class ValidSubclass extends NullableTypeParam<@Nullable String> {}",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    static class InvalidSubclass extends NonNullTypeParam<@Nullable String> {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void interfaceImplementationTypeParamInstantiation() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static interface NonNullTypeParamInterface<E> {}",
            "  static interface NullableTypeParamInterface<E extends @Nullable Object> {}",
            "  static class InvalidInterfaceImplementation",
            "      // BUG: Diagnostic contains: Generic type parameter",
            "      implements NonNullTypeParamInterface<@Nullable String> {}",
            "  static class ValidInterfaceImplementation implements NullableTypeParamInterface<String> {}",
            "}")
        .doTest();
  }

  @Test
  public void nestedTypeParams() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class NonNullTypeParam<E> {}",
            "  static class NullableTypeParam<E extends @Nullable Object> {}",
            "  // BUG: Diagnostic contains: Generic type parameter",
            "  static void testBadNonNull(NullableTypeParam<NonNullTypeParam<@Nullable String>> t) {",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    NullableTypeParam<NonNullTypeParam<NonNullTypeParam<@Nullable String>>> t2 = null;",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    t2 = new NullableTypeParam<NonNullTypeParam<NonNullTypeParam<@Nullable String>>>();",
            "    // this is fine",
            "    NullableTypeParam<NonNullTypeParam<NullableTypeParam<@Nullable String>>> t3 = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeParamInstantiation() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class NonNullTypeParam<E> {}",
            "  static class NullableTypeParam<E extends @Nullable Object> {}",
            "  // BUG: Diagnostic contains: Generic type parameter",
            "  static NonNullTypeParam<@Nullable String> testBadNonNull() {",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    return new NonNullTypeParam<@Nullable String>();",
            "  }",
            "  static NullableTypeParam<@Nullable String> testOKNull() {",
            "    return new NullableTypeParam<@Nullable String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOKNewClassInstantiationForOtherAnnotations() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import lombok.NonNull;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class NonNullTypeParam<E> {}",
            "  static class DifferentAnnotTypeParam1<E extends @NonNull Object> {}",
            "  static class DifferentAnnotTypeParam2<@NonNull E> {}",
            "  static void testOKOtherAnnotation(NonNullTypeParam<String> t) {",
            "    // should not show error for annotation other than @Nullable",
            "    testOKOtherAnnotation(new NonNullTypeParam<@NonNull String>());",
            "    DifferentAnnotTypeParam1<String> t1 = new DifferentAnnotTypeParam1<String>();",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    DifferentAnnotTypeParam2<String> t2 = new DifferentAnnotTypeParam2<@Nullable String>();",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    DifferentAnnotTypeParam1<String> t3 = new DifferentAnnotTypeParam1<@Nullable String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void downcastInstantiation() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class NonNullTypeParam<E> {}",
            "  static void instOf(Object o) {",
            "    // BUG: Diagnostic contains: Generic type parameter",
            "    Object p = (NonNullTypeParam<@Nullable String>) o;",
            "  }",
            "}")
        .doTest();
  }

  /** check that we don't report errors on invalid instantiations in unannotated code */
  @Test
  public void instantiationInUnannotatedCode() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.other;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class NonNullTypeParam<E> {}",
            "  static void instOf(Object o) {",
            "    Object p = (NonNullTypeParam<@Nullable String>) o;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void genericsChecksForAssignments() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class NullableTypeParam<E extends @Nullable Object> {}",
            "  static void testPositive(NullableTypeParam<@Nullable String> t1) {",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    NullableTypeParam<String> t2 = t1;",
            "  }",
            "  static void testNegative(NullableTypeParam<@Nullable String> t1) {",
            "    NullableTypeParam<@Nullable String> t2 = t1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedChecksForAssignmentsMultipleArguments() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class SampleClass<E extends @Nullable Object> {}",
            "  static class SampleClassMultipleArguments<E1 extends @Nullable Object, E2> {}",
            "  static void testPositive() {",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    SampleClassMultipleArguments<SampleClass<SampleClass<@Nullable String>>, String> t1 =",
            "        new SampleClassMultipleArguments<SampleClass<SampleClass<String>>, String>();",
            "  }",
            "  static void testNegative() {",
            "    SampleClassMultipleArguments<SampleClass<SampleClass<@Nullable String>>, String> t1 =",
            "        new SampleClassMultipleArguments<SampleClass<SampleClass<@Nullable String>>, String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void superTypeAssignmentChecksSingleInterface() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  interface Fn<P extends @Nullable Object, R extends @Nullable Object> {}",
            "  class FnImpl implements Fn<@Nullable String, @Nullable String> {}",
            "  void testPositive() {",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    Fn<@Nullable String, String> f = new FnImpl();",
            "  }",
            "  void testNegative() {",
            "    Fn<@Nullable String, @Nullable String> f = new FnImpl();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void superTypeAssignmentChecksMultipleInterface() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  interface Fn1<P1 extends @Nullable Object, P2 extends @Nullable Object> {}",
            "  interface Fn2<P extends @Nullable Object> {}",
            "  class FnImpl implements Fn1<@Nullable String, @Nullable String>, Fn2<String> {}",
            "  void testPositive() {",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    Fn2<@Nullable String> f = new FnImpl();",
            "  }",
            "  void testNegative() {",
            "    Fn2<String> f = new FnImpl();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void superTypeAssignmentChecksMultipleLevelInheritance() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  class SuperClassC<P1 extends @Nullable Object> {}",
            "  class SuperClassB<P extends @Nullable Object> extends SuperClassC<P> {}",
            "  class SubClassA<P extends @Nullable Object> extends SuperClassB<P> {}",
            "  class FnImpl1 extends SubClassA<String> {}",
            "  class FnImpl2 extends SubClassA<@Nullable String> {}",
            "  void testPositive() {",
            "    SuperClassC<@Nullable String> f;",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    f = new FnImpl1();",
            "  }",
            "  void testNegative() {",
            "    SuperClassC<@Nullable String> f;",
            "    // No error",
            "    f = new FnImpl2();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void subtypeWithParameters() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  class D<P extends @Nullable Object> {}",
            "  class B<P extends @Nullable Object> extends D<P> {}",
            "  void testPositive(B<@Nullable String> b) {",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    D<String> f1 = new B<@Nullable String>();",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    D<String> f2 = b;",
            "  }",
            "  void testNegative(B<String> b) {",
            "    D<String> f1 = new B<String>();",
            "    D<String> f2 = b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fancierSubtypeWithParameters() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  class Super<A extends @Nullable Object, B> {}",
            "  class Sub<C, D extends @Nullable Object> extends Super<D, C> {}",
            "  void testNegative() {",
            "    // valid assignment",
            "    Super<@Nullable String, String> s = new Sub<String, @Nullable String>();",
            "  }",
            "  void testPositive() {",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    Super<@Nullable String, String> s2 = new Sub<@Nullable String, String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedVariableDeclarationChecks() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  class D<P extends @Nullable Object> {}",
            "  class B<P extends @Nullable Object> extends D<P> {}",
            "  class C<P extends @Nullable Object> {}",
            "  class A<T extends C<P>, P extends @Nullable Object> {}",
            "  void testPositive() {",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    D<C<String>> f1 = new B<C<@Nullable String>>();",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    A<C<String>, String> f2 = new A<C<String>, @Nullable String>();",
            "    // BUG: Diagnostic contains: Cannot assign from type",
            "    D<C<String>> f3 = new B<@Nullable C<String>>();",
            "  }",
            "  void testNegative() {",
            "    D<C<@Nullable String>> f1 = new B<C<@Nullable String>>();",
            "    A<C<String>, String> f2 = new A<C<String>, String>();",
            "    D<@Nullable C<String>> f3 = new B<@Nullable C<String>>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testForMethodReferenceInAnAssignment() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  interface A<T1 extends @Nullable Object> {",
            "    String function(T1 o);",
            "  }",
            "  static String foo(Object o) {",
            "    return o.toString();",
            "  }",
            "  static void testPositive() {",
            "    // TODO: we should report an error here, since Test::foo cannot take",
            "    // a @Nullable parameter.  we don't catch this yet",
            "    A<@Nullable Object> p = Test::foo;",
            "  }",
            "  static void testNegative() {",
            "    A<Object> p = Test::foo;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testForLambdasInAnAssignment() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  interface A<T1 extends @Nullable Object> {",
            "    String function(T1 o);",
            "  }",
            "  static void testPositive() {",
            "    // TODO: we should report an error here, since the lambda cannot take",
            "    // a @Nullable parameter.  we don't catch this yet",
            "    A<@Nullable Object> p = o -> o.toString();",
            "  }",
            "  static void testNegative() {",
            "    A<Object> p = o -> o.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testForDiamondInAnAssignment() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  interface A<T1 extends @Nullable Object> {",
            "    String function(T1 o);",
            "  }",
            "  static class B<T1> implements A<T1> {",
            "    public String function(T1 o) {",
            "      return o.toString();",
            "    }",
            "  }",
            "  static void testPositive() {",
            "    // TODO: we should report an error here, since B's type parameter",
            "    // cannot be @Nullable; we do not catch this yet",
            "    A<@Nullable Object> p = new B<>();",
            "  }",
            "  static void testNegative() {",
            "    A<Object> p = new B<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void genericFunctionReturnTypeNewClassTree() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class A<T extends @Nullable Object> { }",
            "  static A<String> testPositive1() {",
            "   // BUG: Diagnostic contains: mismatched nullability of type parameters",
            "   return new A<@Nullable String>();",
            "  }",
            "  static A<@Nullable String> testPositive2() {",
            "   // BUG: Diagnostic contains: mismatched nullability of type parameters",
            "   return new A<String>();",
            "  }",
            "  static A<@Nullable String> testNegative() {",
            "   return new A<@Nullable String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void genericFunctionReturnTypeNormalTree() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class A<T extends @Nullable Object> { }",
            "  static A<String> testPositive(A<@Nullable String> a) {",
            "   // BUG: Diagnostic contains: mismatched nullability of type parameters",
            "   return a;",
            "  }",
            "  static A<@Nullable String> testNegative(A<@Nullable String> a) {",
            "   return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void genericFunctionReturnTypeMultipleReturnStatementsIfElseBlock() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class A<T extends @Nullable Object> { }",
            "  static A<String> testPositive(A<@Nullable String> a, int num) {",
            "   if (num % 2 == 0) {",
            "    // BUG: Diagnostic contains: mismatched nullability of type parameters",
            "     return a;",
            "    } else {",
            "     return new A<String>();",
            "    }",
            "  }",
            "  static A<String> testNegative(A<String> a, int num) {",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void genericsChecksForTernaryOperator() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "static class A<T extends @Nullable Object> { }",
            "  static A<String> testPositive(A<String> a, boolean t) {",
            "    // BUG: Diagnostic contains: Conditional expression must have type",
            "    A<@Nullable String> t1 = t ? new A<String>() : new A<@Nullable String>();",
            "    // BUG: Diagnostic contains: Conditional expression must have type",
            "    return t ? new A<@Nullable String>() : new A<@Nullable String>();",
            "  }",
            "  static void testPositiveTernaryMethodArgument(boolean t) {",
            "    // BUG: Diagnostic contains: Conditional expression must have type",
            "    A<String> a = testPositive(t ? new A<String>() : new A<@Nullable String>(), t);",
            "  }",
            "  static A<@Nullable String> testNegative(boolean t) {",
            "    A<@Nullable String> t1 = t ? new A<@Nullable String>() : new A<@Nullable String>();",
            "    return t ? new A<@Nullable String>() : new A<@Nullable String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ternaryOperatorComplexSubtyping() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class A<T extends @Nullable Object> {}",
            "  static class B<T extends @Nullable Object> extends A<T> {}",
            "  static class C<T extends @Nullable Object> extends A<T> {}",
            "  static void testPositive(boolean t) {",
            "    // BUG: Diagnostic contains: Conditional expression must have type",
            "    A<@Nullable String> t1 = t ? new B<@Nullable String>() : new C<String>();",
            "    // BUG: Diagnostic contains: Conditional expression must have type",
            "    A<@Nullable String> t2 = t ? new C<String>() : new B<@Nullable String>();",
            "    // BUG: Diagnostic contains:Conditional expression must have type",
            "    A<@Nullable String> t3 = t ? new B<String>() : new C<@Nullable String>();",
            "    // BUG: Diagnostic contains: Conditional expression must have type",
            "    A<String> t4 = t ? new B<@Nullable String>() : new C<@Nullable String>();",
            "  }",
            "  static void testNegative(boolean t) {",
            "    A<@Nullable String> t1 = t ? new B<@Nullable String>() : new C<@Nullable String>();",
            "    A<@Nullable String> t2 = t ? new C<@Nullable String>() : new B<@Nullable String>();",
            "    A<String> t3 = t ? new C<String>() : new B<String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedTernary() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class A<T extends @Nullable Object> {}",
            "  static class B<T extends @Nullable Object> extends A<T> {}",
            "  static class C<T extends @Nullable Object> extends A<T> {}",
            "  static void testPositive(boolean t) {",
            "    A<@Nullable String> t1 = t ? new C<@Nullable String>() :",
            "        // BUG: Diagnostic contains: Conditional expression must have type",
            "        (t ? new B<@Nullable String>() : new A<String>());",
            "  }",
            "  static void testNegative(boolean t) {",
            "    A<@Nullable String> t1 = t ? new C<@Nullable String>() :",
            "        (t ? new B<@Nullable String>() : new A<@Nullable String>());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ternaryMismatchedAssignmentContext() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "static class A<T extends @Nullable Object> { }",
            "  static void testPositive(boolean t) {",
            "    // we get two errors here, one for each sub-expression; perhaps ideally we would report",
            "    // just one error (that the ternary operator has type A<String> but the assignment LHS",
            "    // has type A<@Nullable String>), but implementing that check in general is",
            "    // a bit tricky",
            "    A<@Nullable String> t1 = t",
            "        // BUG: Diagnostic contains: Conditional expression must have type",
            "        ? new A<String>()",
            "        // BUG: Diagnostic contains: Conditional expression must have type",
            "        : new A<String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void parameterPassing() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "static class A<T extends @Nullable Object> { }",
            "  static A<String> sampleMethod(A<A<String>> a1, A<String> a2) {",
            "     return a2;",
            "  }",
            "  static void testPositive(A<A<@Nullable String>> a1, A<String> a2) {",
            "    // BUG: Diagnostic contains: Cannot pass parameter of type",
            "    A<String> a = sampleMethod(a1, a2);",
            "  }",
            "  static void testNegative(A<A<String>> a1, A<String> a2) {",
            "    A<String> a = sampleMethod(a1, a2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargsParameter() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  static class A<T extends @Nullable Object> { }",
            "  static A<@Nullable String> sampleMethodWithVarArgs(A<String>... args) {",
            "     return new A<@Nullable String>();",
            "  }",
            "  static void testPositive(A<@Nullable String> a1, A<String> a2) {",
            "     // BUG: Diagnostic contains: Cannot pass parameter of type",
            "     A<@Nullable String> b = sampleMethodWithVarArgs(a1);",
            "     // BUG: Diagnostic contains: Cannot pass parameter of type",
            "     A<@Nullable String> b2 = sampleMethodWithVarArgs(a2, a1);",
            "  }",
            "  static void testNegative(A<String> a1, A<String> a2) {",
            "     A<@Nullable String> b = sampleMethodWithVarArgs(a1);",
            "     A<@Nullable String> b2 = sampleMethodWithVarArgs(a2, a1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void overrideReturnTypes() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  interface Fn<P extends @Nullable Object, R extends @Nullable Object> {",
            "   R apply(P p);",
            "  }",
            " static class TestFunc1 implements Fn<String, @Nullable String> {",
            "  @Override",
            "  public @Nullable String apply(String s) {",
            "   return s;",
            "  }",
            " }",
            " static class TestFunc2 implements Fn<String, @Nullable String> {",
            "  @Override",
            "  public String apply(String s) {",
            "   return s;",
            "  }",
            " }",
            " static class TestFunc3 implements Fn<String, String> {",
            "  @Override",
            "  // BUG: Diagnostic contains: method returns @Nullable, but superclass",
            "  public @Nullable String apply(String s) {",
            "   return s;",
            "  }",
            " }",
            " static void useTestFunc(String s) {",
            "    Fn<String, @Nullable String> f1 = new TestFunc1();",
            "    String t1 = f1.apply(s);",
            "    // BUG: Diagnostic contains: dereferenced expression",
            "    t1.hashCode();",
            "    TestFunc2 f2 = new TestFunc2();",
            "    String t2 = f2.apply(s);",
            "    // There should not be an error here",
            "    t2.hashCode();",
            "    Fn<String, @Nullable String> f3 = new TestFunc2();",
            "    String t3 = f3.apply(s);",
            "    // BUG: Diagnostic contains: dereferenced expression",
            "    t3.hashCode();",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void overrideWithNullCheck() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  interface Fn<P extends @Nullable Object, R extends @Nullable Object> {",
            "   R apply(P p);",
            "  }",
            " static class TestFunc1 implements Fn<String, @Nullable String> {",
            "  @Override",
            "  public @Nullable String apply(String s) {",
            "   return s;",
            "  }",
            " }",
            " static void useTestFuncWithCast() {",
            "    Fn<String, @Nullable String> f1 = new TestFunc1();",
            "    if (f1.apply(\"hello\") != null) {",
            "      String t1 = f1.apply(\"hello\");",
            "      // no error here due to null check",
            "      t1.hashCode();",
            "    }",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void overrideParameterType() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  interface Fn<P extends @Nullable Object, R extends @Nullable Object> {",
            "   R apply(P p);",
            "  }",
            " static class TestFunc1 implements Fn<@Nullable String, String> {",
            "  @Override",
            "  // BUG: Diagnostic contains: parameter s is",
            "  public String apply(String s) {",
            "   return s;",
            "  }",
            " }",
            " static class TestFunc2 implements Fn<@Nullable String, String> {",
            "  @Override",
            "  public String apply(@Nullable String s) {",
            "   return \"hi\";",
            "  }",
            " }",
            " static class TestFunc3 implements Fn<String, String> {",
            "  @Override",
            "  public String apply(String s) {",
            "   return \"hi\";",
            "  }",
            " }",
            " static class TestFunc4 implements Fn<String, String> {",
            "  // this override is legal, we should get no error",
            "  @Override",
            "  public String apply(@Nullable String s) {",
            "   return \"hi\";",
            "  }",
            " }",
            " static void useTestFunc(String s) {",
            "    Fn<@Nullable String, String> f1 = new TestFunc2();",
            "    // should get no error here",
            "    f1.apply(null);",
            "    Fn<String, String> f2 = new TestFunc3();",
            "    // BUG: Diagnostic contains: passing @Nullable parameter",
            "    f2.apply(null);",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void nestedMethodMatch() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  class P<T extends @Nullable Object>{}",
            "  interface Fn< T extends P<R>, R extends @Nullable Object> {",
            "   R apply(String s);",
            "  }",
            " static class TestFunc1 implements Fn<P<@Nullable String>, @Nullable String> {",
            "  @Override",
            "  public String apply(String s) {",
            "   return s;",
            "  }",
            " }",
            " static class TestFunc2 implements Fn<P<@Nullable String>, @Nullable String> {",
            "  @Override",
            "  public @Nullable String apply(String s) {",
            "   return s;",
            "  }",
            " }",
            " static void useTestFunc(String s) {",
            "   Fn<P<@Nullable String>, @Nullable String> f1 = new TestFunc1();",
            "   String t1 = f1.apply(s);",
            "   // BUG: Diagnostic contains: dereferenced expression",
            "   t1.hashCode();",
            "   Fn<P<@Nullable String>, @Nullable String> f2 = new TestFunc2();",
            "   String t2 = f2.apply(s);",
            "   // BUG: Diagnostic contains: dereferenced expression",
            "   t2.hashCode();",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void methodMatchNullableAnnotatedMethod() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            " interface Fn<P extends @Nullable Object, R> {",
            "   @Nullable R apply(P p);",
            "  }",
            " static class TestFunc implements Fn<String, String> {",
            "  @Override",
            "  //This override is fine and is handled by the current code",
            "  public @Nullable String apply(String s) {",
            "   return s;",
            "  }",
            " }",
            " static void useTestFunc(String s) {",
            "  Fn<String, String> f = new TestFunc();",
            "  String t = f.apply(s);",
            "  // BUG: Diagnostic contains: dereferenced expression",
            "  t.hashCode();",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void nestedMethodReturnTypeMatch() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            " class P<T1 extends @Nullable Object, T2 extends @Nullable Object>{}",
            " interface Fn<T extends P<R, R>, R extends @Nullable Object> {",
            "  T apply();",
            " }",
            " class TestFunc implements Fn<P<@Nullable String, String>, @Nullable String> {",
            " @Override",
            "  // BUG: Diagnostic contains: Cannot return",
            " public P<@Nullable String, @Nullable String> apply() {",
            "   return new P<@Nullable String, @Nullable String>();",
            "  }",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void nestedMethodParamTypeMatch() {
    makeHelper()
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  class P<T1 extends @Nullable Object, T2 extends @Nullable Object>{}",
            " interface Fn<T extends P<R, R>, R extends @Nullable Object> {",
            "  String apply(T t, String s);",
            " }",
            " class TestFunc implements Fn<P<String, String>, String> {",
            " @Override",
            "  // BUG: Diagnostic contains: Cannot have method parameter",
            "  public String apply(P<@Nullable String, String> p, String s) {",
            "    return s;",
            "  }",
            " }",
            "}")
        .doTest();
  }

  private CompilationTestHelper makeHelper() {
    return makeTestHelperWithArgs(
        Arrays.asList(
            "-XepOpt:NullAway:AnnotatedPackages=com.uber", "-XepOpt:NullAway:JSpecifyMode=true"));
  }
}
