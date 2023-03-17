package com.uber.nullaway;

import static com.uber.nullaway.NullabilityUtil.castToNonNull;

import com.google.common.base.Preconditions;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeMetadata;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** Methods for performing checks related to generic types and nullability. */
public final class GenericsChecks {

  private static final String NULLABLE_NAME = "org.jspecify.annotations.Nullable";

  private static final Supplier<Type> NULLABLE_TYPE_SUPPLIER =
      Suppliers.typeFromString(NULLABLE_NAME);
  private VisitorState state;
  private Config config;
  private NullAway analysis;

  public GenericsChecks(VisitorState state, Config config, NullAway analysis) {
    this.state = state;
    this.config = config;
    this.analysis = analysis;
  }

  /**
   * Checks that for an instantiated generic type, {@code @Nullable} types are only used for type
   * variables that have a {@code @Nullable} upper bound.
   *
   * @param tree the tree representing the instantiated type
   * @param state visitor state
   * @param analysis the analysis object
   * @param config the analysis config
   */
  public static void checkInstantiationForParameterizedTypedTree(
      ParameterizedTypeTree tree, VisitorState state, NullAway analysis, Config config) {
    if (!config.isJSpecifyMode()) {
      return;
    }
    List<? extends Tree> typeArguments = tree.getTypeArguments();
    if (typeArguments.size() == 0) {
      return;
    }
    Map<Integer, Tree> nullableTypeArguments = new HashMap<>();
    for (int i = 0; i < typeArguments.size(); i++) {
      Tree curTypeArg = typeArguments.get(i);
      if (curTypeArg instanceof AnnotatedTypeTree) {
        AnnotatedTypeTree annotatedType = (AnnotatedTypeTree) curTypeArg;
        for (AnnotationTree annotation : annotatedType.getAnnotations()) {
          Type annotationType = ASTHelpers.getType(annotation);
          if (annotationType != null
              && Nullness.isNullableAnnotation(annotationType.toString(), config)) {
            nullableTypeArguments.put(i, curTypeArg);
            break;
          }
        }
      }
    }
    // base type that is being instantiated
    Type baseType = ASTHelpers.getType(tree);
    if (baseType == null) {
      return;
    }
    com.sun.tools.javac.util.List<Type> baseTypeArgs = baseType.tsym.type.getTypeArguments();
    for (int i = 0; i < baseTypeArgs.size(); i++) {
      if (nullableTypeArguments.containsKey(i)) {

        Type typeVariable = baseTypeArgs.get(i);
        Type upperBound = typeVariable.getUpperBound();
        com.sun.tools.javac.util.List<Attribute.TypeCompound> annotationMirrors =
            upperBound.getAnnotationMirrors();
        boolean hasNullableAnnotation =
            Nullness.hasNullableAnnotation(annotationMirrors.stream(), config);
        // if base type argument does not have @Nullable annotation then the instantiation is
        // invalid
        if (!hasNullableAnnotation) {
          reportInvalidInstantiationError(
              nullableTypeArguments.get(i), baseType, typeVariable, state, analysis);
        }
      }
    }
  }

  private static void reportInvalidInstantiationError(
      Tree tree, Type baseType, Type baseTypeVariable, VisitorState state, NullAway analysis) {
    ErrorBuilder errorBuilder = analysis.getErrorBuilder();
    ErrorMessage errorMessage =
        new ErrorMessage(
            ErrorMessage.MessageTypes.TYPE_PARAMETER_CANNOT_BE_NULLABLE,
            String.format(
                "Generic type parameter cannot be @Nullable, as type variable %s of type %s does not have a @Nullable upper bound",
                baseTypeVariable.tsym.toString(), baseType.tsym.toString()));
    state.reportMatch(
        errorBuilder.createErrorDescription(
            errorMessage, analysis.buildDescription(tree), state, null));
  }

  private static void reportInvalidAssignmentInstantiationError(
      Tree tree, Type lhsType, Type rhsType, VisitorState state, NullAway analysis) {
    ErrorBuilder errorBuilder = analysis.getErrorBuilder();
    ErrorMessage errorMessage =
        new ErrorMessage(
            ErrorMessage.MessageTypes.ASSIGN_GENERIC_NULLABLE,
            String.format(
                "Cannot assign from type "
                    + rhsType
                    + " to type "
                    + lhsType
                    + " due to mismatched nullability of type parameters"));
    state.reportMatch(
        errorBuilder.createErrorDescription(
            errorMessage, analysis.buildDescription(tree), state, null));
  }

  private static void reportInvalidReturnTypeError(
      Tree tree, Type methodType, Type returnType, VisitorState state, NullAway analysis) {
    ErrorBuilder errorBuilder = analysis.getErrorBuilder();
    ErrorMessage errorMessage =
        new ErrorMessage(
            ErrorMessage.MessageTypes.RETURN_NULLABLE_GENERIC,
            String.format(
                "Cannot return expression of type "
                    + returnType
                    + " from method with return type "
                    + methodType
                    + " due to mismatched nullability of type parameters"));
    state.reportMatch(
        errorBuilder.createErrorDescription(
            errorMessage, analysis.buildDescription(tree), state, null));
  }

  private static void reportMismatchedTypeForTernaryOperator(
      Tree tree, Type expressionType, Type subPartType, VisitorState state, NullAway analysis) {
    ErrorBuilder errorBuilder = analysis.getErrorBuilder();
    ErrorMessage errorMessage =
        new ErrorMessage(
            ErrorMessage.MessageTypes.ASSIGN_GENERIC_NULLABLE,
            String.format(
                "Conditional expression must have type "
                    + expressionType
                    + " but the sub-expression has type "
                    + subPartType
                    + ", which has mismatched nullability of type parameters"));
    state.reportMatch(
        errorBuilder.createErrorDescription(
            errorMessage, analysis.buildDescription(tree), state, null));
  }

  private void reportInvalidParametersNullabilityError(
      Type formalParameterType,
      Type actualParameterType,
      Tree paramExpression,
      VisitorState state,
      NullAway analysis) {
    ErrorBuilder errorBuilder = analysis.getErrorBuilder();
    ErrorMessage errorMessage =
        new ErrorMessage(
            ErrorMessage.MessageTypes.PASS_NULLABLE_GENERIC,
            "Cannot pass parameter of type "
                + actualParameterType
                + ", as formal parameter has type "
                + formalParameterType
                + ", which has mismatched type parameter nullability");
    state.reportMatch(
        errorBuilder.createErrorDescription(
            errorMessage, analysis.buildDescription(paramExpression), state, null));
  }

  private void reportInvalidOverridingMethodReturnTypeError(
      Tree methodTree, Type typeParameterType, Type methodReturnType) {
    ErrorBuilder errorBuilder = analysis.getErrorBuilder();
    ErrorMessage errorMessage =
        new ErrorMessage(
            ErrorMessage.MessageTypes.PASS_NULLABLE_GENERIC,
            "Cannot return type "
                + methodReturnType
                + ", as corresponding type parameter has type "
                + typeParameterType
                + ", which has mismatched type parameter nullability");
    state.reportMatch(
        errorBuilder.createErrorDescription(
            errorMessage, analysis.buildDescription(methodTree), state, null));
  }

  private void reportInvalidOverridingMethodParamTypeError(
      Tree methodTree, Type typeParameterType, Type methodParamType) {
    ErrorBuilder errorBuilder = analysis.getErrorBuilder();
    ErrorMessage errorMessage =
        new ErrorMessage(
            ErrorMessage.MessageTypes.PASS_NULLABLE_GENERIC,
            "Cannot have method parameter type "
                + methodParamType
                + ", as corresponding type parameter has type "
                + typeParameterType
                + ", which has mismatched type parameter nullability");
    state.reportMatch(
        errorBuilder.createErrorDescription(
            errorMessage, analysis.buildDescription(methodTree), state, null));
  }

  /**
   * This method returns the type of the given tree, including any type use annotations.
   *
   * <p>This method is required because in some cases, the type returned by {@link
   * com.google.errorprone.util.ASTHelpers#getType(Tree)} fails to preserve type use annotations,
   * particularly when dealing with {@link com.sun.source.tree.NewClassTree} (e.g., {@code new
   * Foo<@Nullable A>}).
   *
   * @param tree A tree for which we need the type with preserved annotations.
   * @return Type of the tree with preserved annotations.
   */
  @Nullable
  private Type getTreeType(Tree tree) {
    if (tree instanceof NewClassTree
        && ((NewClassTree) tree).getIdentifier() instanceof ParameterizedTypeTree) {
      ParameterizedTypeTree paramTypedTree =
          (ParameterizedTypeTree) ((NewClassTree) tree).getIdentifier();
      if (paramTypedTree.getTypeArguments().isEmpty()) {
        // diamond operator, which we do not yet support; for now, return null
        // TODO: support diamond operators
        return null;
      }
      return typeWithPreservedAnnotations(paramTypedTree);
    } else {
      return ASTHelpers.getType(tree);
    }
  }

  /**
   * For a tree representing an assignment, ensures that from the perspective of type parameter
   * nullability, the type of the right-hand side is assignable to (a subtype of) the type of the
   * left-hand side. This check ensures that for every parameterized type nested in each of the
   * types, the type parameters have identical nullability.
   *
   * @param tree the tree to check, which must be either an {@link AssignmentTree} or a {@link
   *     VariableTree}
   */
  public void checkTypeParameterNullnessForAssignability(Tree tree) {
    if (!config.isJSpecifyMode()) {
      return;
    }
    Tree lhsTree;
    Tree rhsTree;
    if (tree instanceof VariableTree) {
      VariableTree varTree = (VariableTree) tree;
      lhsTree = varTree.getType();
      rhsTree = varTree.getInitializer();
    } else {
      AssignmentTree assignmentTree = (AssignmentTree) tree;
      lhsTree = assignmentTree.getVariable();
      rhsTree = assignmentTree.getExpression();
    }
    // rhsTree can be null for a VariableTree.  Also, we don't need to do a check
    // if rhsTree is the null literal
    if (rhsTree == null || rhsTree.getKind().equals(Tree.Kind.NULL_LITERAL)) {
      return;
    }
    Type lhsType = getTreeType(lhsTree);
    Type rhsType = getTreeType(rhsTree);

    if (lhsType instanceof Type.ClassType && rhsType instanceof Type.ClassType) {
      boolean isAssignmentValid =
          compareNullabilityAnnotations((Type.ClassType) lhsType, (Type.ClassType) rhsType);
      if (!isAssignmentValid) {
        reportInvalidAssignmentInstantiationError(tree, lhsType, rhsType, state, analysis);
      }
    }
  }

  public void checkTypeParameterNullnessForFunctionReturnType(
      ExpressionTree retExpr, Symbol.MethodSymbol methodSymbol) {
    if (!config.isJSpecifyMode()) {
      return;
    }

    Type formalReturnType = methodSymbol.getReturnType();
    // check nullability of parameters only for generics
    if (formalReturnType.getTypeArguments().isEmpty()) {
      return;
    }
    Type returnExpressionType = getTreeType(retExpr);
    if (formalReturnType instanceof Type.ClassType
        && returnExpressionType instanceof Type.ClassType) {
      boolean isReturnTypeValid =
          compareNullabilityAnnotations(
              (Type.ClassType) formalReturnType, (Type.ClassType) returnExpressionType);
      if (!isReturnTypeValid) {
        reportInvalidReturnTypeError(
            retExpr, formalReturnType, returnExpressionType, state, analysis);
      }
    }
  }

  /**
   * Compare two types from an assignment for identical type parameter nullability, recursively
   * checking nested generic types. See <a
   * href="https://jspecify.dev/docs/spec/#nullness-delegating-subtyping">the JSpecify
   * specification</a> and <a
   * href="https://docs.oracle.com/javase/specs/jls/se14/html/jls-4.html#jls-4.10.2">the JLS
   * subtyping rules for class and interface types</a>.
   *
   * @param lhsType type for the lhs of the assignment
   * @param rhsType type for the rhs of the assignment
   */
  private boolean compareNullabilityAnnotations(Type.ClassType lhsType, Type.ClassType rhsType) {
    Types types = state.getTypes();
    // The base type of rhsType may be a subtype of lhsType's base type.  In such cases, we must
    // compare lhsType against the supertype of rhsType with a matching base type.
    rhsType = (Type.ClassType) types.asSuper(rhsType, lhsType.tsym);
    // This is impossible, considering the fact that standard Java subtyping succeeds before running
    // NullAway
    if (rhsType == null) {
      throw new RuntimeException("Did not find supertype of " + rhsType + " matching " + lhsType);
    }
    List<Type> lhsTypeArguments = lhsType.getTypeArguments();
    List<Type> rhsTypeArguments = rhsType.getTypeArguments();
    // This is impossible, considering the fact that standard Java subtyping succeeds before running
    // NullAway
    if (lhsTypeArguments.size() != rhsTypeArguments.size()) {
      throw new RuntimeException(
          "Number of types arguments in " + rhsType + " does not match " + lhsType);
    }
    for (int i = 0; i < lhsTypeArguments.size(); i++) {
      Type lhsTypeArgument = lhsTypeArguments.get(i);
      Type rhsTypeArgument = rhsTypeArguments.get(i);
      boolean isLHSNullableAnnotated = false;
      List<Attribute.TypeCompound> lhsAnnotations = lhsTypeArgument.getAnnotationMirrors();
      // To ensure that we are checking only jspecify nullable annotations
      for (Attribute.TypeCompound annotation : lhsAnnotations) {
        if (annotation.getAnnotationType().toString().equals(NULLABLE_NAME)) {
          isLHSNullableAnnotated = true;
          break;
        }
      }
      boolean isRHSNullableAnnotated = false;
      List<Attribute.TypeCompound> rhsAnnotations = rhsTypeArgument.getAnnotationMirrors();
      // To ensure that we are checking only jspecify nullable annotations
      for (Attribute.TypeCompound annotation : rhsAnnotations) {
        if (annotation.getAnnotationType().toString().equals(NULLABLE_NAME)) {
          isRHSNullableAnnotated = true;
          break;
        }
      }
      if (isLHSNullableAnnotated != isRHSNullableAnnotated) {
        return false;
      }
      // nested generics
      if (lhsTypeArgument.getTypeArguments().length() > 0) {
        if (!compareNullabilityAnnotations(
            (Type.ClassType) lhsTypeArgument, (Type.ClassType) rhsTypeArgument)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * For the Parameterized typed trees, ASTHelpers.getType(tree) does not return a Type with
   * preserved annotations. This method takes a Parameterized typed tree as an input and returns the
   * Type of the tree with the annotations.
   *
   * @param tree A parameterized typed tree for which we need class type with preserved annotations.
   * @return A Type with preserved annotations.
   */
  private Type.ClassType typeWithPreservedAnnotations(ParameterizedTypeTree tree) {
    Type.ClassType type = (Type.ClassType) ASTHelpers.getType(tree);
    Preconditions.checkNotNull(type);
    Type nullableType = NULLABLE_TYPE_SUPPLIER.get(state);
    List<? extends Tree> typeArguments = tree.getTypeArguments();
    List<Type> newTypeArgs = new ArrayList<>();
    for (int i = 0; i < typeArguments.size(); i++) {
      boolean hasNullableAnnotation = false;
      AnnotatedTypeTree annotatedType = null;
      Tree curTypeArg = typeArguments.get(i);
      // If the type argument has an annotation, it will either be an AnnotatedTypeTree, or a
      // ParameterizedTypeTree in the case of a nested generic type
      if (curTypeArg instanceof AnnotatedTypeTree) {
        annotatedType = (AnnotatedTypeTree) curTypeArg;
      } else if (curTypeArg instanceof ParameterizedTypeTree
          && ((ParameterizedTypeTree) curTypeArg).getType() instanceof AnnotatedTypeTree) {
        annotatedType = (AnnotatedTypeTree) ((ParameterizedTypeTree) curTypeArg).getType();
      }
      List<? extends AnnotationTree> annotations =
          annotatedType != null ? annotatedType.getAnnotations() : Collections.emptyList();
      for (AnnotationTree annotation : annotations) {
        if (ASTHelpers.isSameType(
            nullableType, ASTHelpers.getType(annotation.getAnnotationType()), state)) {
          hasNullableAnnotation = true;
          break;
        }
      }
      // construct a TypeMetadata object containing a nullability annotation if needed
      com.sun.tools.javac.util.List<Attribute.TypeCompound> nullableAnnotationCompound =
          hasNullableAnnotation
              ? com.sun.tools.javac.util.List.from(
                  Collections.singletonList(
                      new Attribute.TypeCompound(
                          nullableType, com.sun.tools.javac.util.List.nil(), null)))
              : com.sun.tools.javac.util.List.nil();
      TypeMetadata typeMetadata =
          new TypeMetadata(new TypeMetadata.Annotations(nullableAnnotationCompound));
      Type currentTypeArgType = castToNonNull(ASTHelpers.getType(curTypeArg));
      if (currentTypeArgType.getTypeArguments().size() > 0) {
        // nested generic type; recursively preserve its nullability type argument annotations
        currentTypeArgType = typeWithPreservedAnnotations((ParameterizedTypeTree) curTypeArg);
      }
      Type.ClassType newTypeArgType =
          (Type.ClassType) currentTypeArgType.cloneWithMetadata(typeMetadata);
      newTypeArgs.add(newTypeArgType);
    }
    Type.ClassType finalType =
        new Type.ClassType(
            type.getEnclosingType(), com.sun.tools.javac.util.List.from(newTypeArgs), type.tsym);
    return finalType;
  }

  /**
   * The Nullness for the overridden method return type that is derived from the type parameter for
   * the owner is by default considered as NonNull. This method computes and returns the Nullness of
   * the method return type based on the Nullness of the corresponding instantiated type parameter
   * for the owner
   *
   * @param overriddenMethod A symbol of the overridden method
   * @param overridingMethodOwnerType An owner of the overriding method
   * @param state Visitor state
   * @param config The analysis config
   * @return Returns the Nullness of the return type of the overridden method
   */
  public static Nullness getOverriddenMethodReturnTypeNullness(
      Symbol.MethodSymbol overriddenMethod,
      Type overridingMethodOwnerType,
      VisitorState state,
      Config config) {

    Type overriddenMethodType =
        state.getTypes().memberType(overridingMethodOwnerType, overriddenMethod);
    if (!(overriddenMethodType instanceof Type.MethodType)) {
      return Nullness.NONNULL;
    }
    return getTypeNullness(overriddenMethodType.getReturnType(), config);
  }

  /**
   * For a conditional expression <em>c</em>, check whether the type parameter nullability for each
   * sub-expression of <em>c</em> matches the type parameter nullability of <em>c</em> itself.
   *
   * <p>Note that the type parameter nullability for <em>c</em> is computed by javac and reflects
   * what is required of the surrounding context (an assignment, parameter pass, etc.). It is
   * possible that both sub-expressions of <em>c</em> will have identical type parameter
   * nullability, but will still not match the type parameter nullability of <em>c</em> itself, due
   * to requirements from the surrounding context. In such a case, our error messages may be
   * somewhat confusing; we may want to improve this in the future.
   *
   * @param tree A conditional expression tree to check
   */
  public void checkTypeParameterNullnessForConditionalExpression(ConditionalExpressionTree tree) {
    if (!config.isJSpecifyMode()) {
      return;
    }

    Tree truePartTree = tree.getTrueExpression();
    Tree falsePartTree = tree.getFalseExpression();

    Type condExprType = getTreeType(tree);
    Type truePartType = getTreeType(truePartTree);
    Type falsePartType = getTreeType(falsePartTree);
    // The condExpr type should be the least-upper bound of the true and false part types.  To check
    // the nullability annotations, we check that the true and false parts are assignable to the
    // type of the whole expression
    if (condExprType instanceof Type.ClassType) {
      if (truePartType instanceof Type.ClassType) {
        if (!compareNullabilityAnnotations(
            (Type.ClassType) condExprType, (Type.ClassType) truePartType)) {
          reportMismatchedTypeForTernaryOperator(
              truePartTree, condExprType, truePartType, state, analysis);
        }
      }
      if (falsePartType instanceof Type.ClassType) {
        if (!compareNullabilityAnnotations(
            (Type.ClassType) condExprType, (Type.ClassType) falsePartType)) {
          reportMismatchedTypeForTernaryOperator(
              falsePartTree, condExprType, falsePartType, state, analysis);
        }
      }
    }
  }

  /**
   * Checks that for each parameter p at a call, the type parameter nullability for p's type matches
   * that of the corresponding formal parameter. If a mismatch is found, report an error.
   *
   * @param formalParams the formal parameters
   * @param actualParams the actual parameters
   * @param isVarArgs true if the call is to a varargs method
   */
  public void compareGenericTypeParameterNullabilityForCall(
      List<Symbol.VarSymbol> formalParams,
      List<? extends ExpressionTree> actualParams,
      boolean isVarArgs) {
    if (!config.isJSpecifyMode()) {
      return;
    }
    for (int i = 0; i < formalParams.size(); i++) {
      Type formalParameter = formalParams.get(i).type;
      if (!formalParameter.getTypeArguments().isEmpty()) {
        Type actualParameter = getTreeType(actualParams.get(i));
        if (formalParameter instanceof Type.ClassType
            && actualParameter instanceof Type.ClassType) {
          if (!compareNullabilityAnnotations(
              (Type.ClassType) formalParameter, (Type.ClassType) actualParameter)) {
            reportInvalidParametersNullabilityError(
                formalParameter, actualParameter, actualParams.get(i), state, analysis);
          }
        }
      }
    }
    if (isVarArgs && !formalParams.isEmpty()) {
      Type.ArrayType varargsArrayType =
          (Type.ArrayType) formalParams.get(formalParams.size() - 1).type;
      Type varargsElementType = varargsArrayType.elemtype;
      if (varargsElementType.getTypeArguments().size() > 0) {
        for (int i = formalParams.size() - 1; i < actualParams.size(); i++) {
          Type actualParameter = getTreeType(actualParams.get(i));
          if (varargsElementType instanceof Type.ClassType
              && actualParameter instanceof Type.ClassType) {
            if (!compareNullabilityAnnotations(
                (Type.ClassType) varargsElementType, (Type.ClassType) actualParameter)) {
              reportInvalidParametersNullabilityError(
                  varargsElementType, actualParameter, actualParams.get(i), state, analysis);
            }
          }
        }
      }
    }
  }

  /**
   * This method gets a method type with return type and method parameters having annotations of the
   * corresponding type parameters of the owner and then use it to compare the nullability
   * annotations of the return type and the method params with the corresponding type params of the
   * owner.
   *
   * @param tree A method tree to check
   * @param overridingMethod A symbol of the overriding method
   * @param overriddenMethod A symbol of the overridden method
   */
  public void checkTypeParameterNullnessForMethodOverriding(
      MethodTree tree, Symbol.MethodSymbol overridingMethod, Symbol.MethodSymbol overriddenMethod) {
    if (!config.isJSpecifyMode()) {
      return;
    }
    // A method type with the return type and method parameters having the annotations of the type
    // parameters of the
    // owner if they are derived from the type parameters
    Type methodWithTypeParams =
        state.getTypes().memberType(overridingMethod.owner.type, overriddenMethod);

    checkTypeParameterNullnessForOverridingMethodReturnType(tree, methodWithTypeParams);
    checkTypeParameterNullnessForOverridingMethodParameterType(tree, methodWithTypeParams);
  }

  /**
   * The Nullness for the overriding method arguments that are derived from the type parameters for
   * the owner are by default considered as NonNull. This method computes and returns the Nullness
   * of the method arguments based on the Nullness of the corresponding instantiated type parameters
   * for the owner
   *
   * @param paramIndex An index of the method parameter to get the Nullness
   * @param methodSymbol A symbol of the overridden method
   * @param tree A method tree to check
   * @return Returns Nullness of the parameterIndex th parameter of the overriding method
   */
  public Nullness getOverridingMethodParamNullness(
      int paramIndex, Symbol.MethodSymbol methodSymbol, Tree tree) {
    JCTree.JCMethodInvocation methodInvocationTree = (JCTree.JCMethodInvocation) tree;
    // if methodInvocationTree.meth is of type JCTree.JCIdent return Nullness.NONNULL
    // if not in JSpecify mode then also the Nullness is set to Nullness.NONNULL, so it won't affect
    // the logic outside our scope
    if (!(methodInvocationTree.meth instanceof JCTree.JCFieldAccess)) {
      return Nullness.NONNULL;
    }
    Type methodReceiverType = ((JCTree.JCFieldAccess) methodInvocationTree.meth).selected.type;
    Type methodType = state.getTypes().memberType(methodReceiverType, methodSymbol);
    Type formalParamType = methodType.getParameterTypes().get(paramIndex);
    return getTypeNullness(formalParamType, config);
  }

  /**
   * The Nullness for the overridden method arguments that are derived from the type parameters for
   * the owner are by default considered as NonNull. This method computes and returns the Nullness
   * of the method arguments based on the Nullness of the corresponding instantiated type parameters
   * for the owner
   *
   * @param parameterIndex An index of the method parameter to get the Nullness
   * @param overriddenMethod A symbol of the overridden method
   * @param overridingMethodParam A paramIndex th parameter of the overriding method
   * @return Returns Nullness of the parameterIndex th parameter of the overridden method
   */
  public Nullness getOverriddenMethodArgNullness(
      int parameterIndex,
      Symbol.MethodSymbol overriddenMethod,
      Symbol.VarSymbol overridingMethodParam) {
    Type methodType =
        state.getTypes().memberType(overridingMethodParam.owner.owner.type, overriddenMethod);
    Type paramType = methodType.getParameterTypes().get(parameterIndex);
    return getTypeNullness(paramType, config);
  }

  /**
   * This method compares the type parameter annotations for the overriding method parameters with
   * corresponding type parameters for the owner and reports an error if they don't match
   *
   * @param tree A method tree to check
   * @param methodWithTypeParams A method type with the annotations of corresponding type parameters
   *     of the owner of the overriding method
   */
  private void checkTypeParameterNullnessForOverridingMethodParameterType(
      MethodTree tree, Type methodWithTypeParams) {
    List<? extends VariableTree> methodParameters = tree.getParameters();
    List<Type> typeParameterTypes = methodWithTypeParams.getParameterTypes();
    for (int i = 0; i < methodParameters.size(); i++) {
      Type methodParameterType = ASTHelpers.getType(methodParameters.get(i));
      Type typeParameterType = typeParameterTypes.get(i);
      if (typeParameterType instanceof Type.ClassType
          && methodParameterType instanceof Type.ClassType) {
        // for generic types check if the nullability annotations of the type params match
        boolean doTypeParamNullabilityAnnotationsMatch =
            compareNullabilityAnnotations(
                (Type.ClassType) typeParameterType, (Type.ClassType) methodParameterType);

        if (!doTypeParamNullabilityAnnotationsMatch) {
          reportInvalidOverridingMethodParamTypeError(
              methodParameters.get(i), typeParameterType, methodParameterType);
        }
      }
    }
  }

  /**
   * This method compares the type parameter annotations for the overriding method return type with
   * corresponding type parameters for the owner and reports an error if they don't match
   *
   * @param tree A method tree to check
   * @param methodWithTypeParams A method type with the annotations of corresponding type parameters
   *     of the owner of the overriding method
   */
  private void checkTypeParameterNullnessForOverridingMethodReturnType(
      MethodTree tree, Type methodWithTypeParams) {
    Type typeParamType = methodWithTypeParams.getReturnType();
    Type methodReturnType = ASTHelpers.getType(tree.getReturnType());
    if (!(typeParamType instanceof Type.ClassType && methodReturnType instanceof Type.ClassType)) {
      return;
    }
    boolean doNullabilityAnnotationsMatch =
        compareNullabilityAnnotations(
            (Type.ClassType) typeParamType, (Type.ClassType) methodReturnType);

    if (!doNullabilityAnnotationsMatch) {
      reportInvalidOverridingMethodReturnTypeError(tree, typeParamType, methodReturnType);
    }
  }

  /**
   * @param type A type for which we need the Nullness.
   * @param config The analysis config
   * @return Returns the Nullness of the type based on the Nullability annotation.
   */
  private static Nullness getTypeNullness(Type type, Config config) {
    boolean hasNullableAnnotation =
        Nullness.hasNullableAnnotation(type.getAnnotationMirrors().stream(), config);
    if (hasNullableAnnotation) {
      return Nullness.NULLABLE;
    }
    return Nullness.NONNULL;
  }
}
