package com.uber.nullaway;

import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import java.util.HashSet;
import java.util.List;

/** Methods for performing checks related to generic types and nullability. */
public class GenericsChecks {

  private GenericsChecks() {
    // just utility methods
  }

  private static void invalidInstantiationError(Tree tree, VisitorState state, NullAway analysis) {
    ErrorBuilder errorBuilder = analysis.getErrorBuilder();
    ErrorMessage errorMessage =
        new ErrorMessage(
            ErrorMessage.MessageTypes.TYPE_PARAMETER_CANNOT_BE_NULLABLE,
            "Generic type parameter cannot be @Nullable");
    state.reportMatch(
        errorBuilder.createErrorDescription(
            errorMessage, analysis.buildDescription(tree), state, null));
  }

  private static HashSet<String> getNullableAnnotatedArgumentsListForNewClassTree(
      ParameterizedTypeTree tree, int nestingLevel, int nestedParamIndex) {
    HashSet<String> nullableAnnotatedArguments = new HashSet<String>();
    List<? extends Tree> typeArguments = tree.getTypeArguments();
    for (int i = 0; i < typeArguments.size(); i++) {
      if (typeArguments.get(i).getClass().equals(JCTree.JCAnnotatedType.class)) {
        JCTree.JCAnnotatedType annotatedType = (JCTree.JCAnnotatedType) typeArguments.get(i);
        for (JCTree.JCAnnotation annotation : annotatedType.getAnnotations()) {
          Attribute.Compound attribute = annotation.attribute;
          if (attribute.toString().equals("@org.jspecify.nullness.Nullable")) {
            String currentString = nestingLevel + "." + nestedParamIndex + "." + String.valueOf(i);
            nullableAnnotatedArguments.add(currentString);
            break;
          }
        }
      }
    }

    for (int i = 0; i < typeArguments.size(); i++) {
      if (typeArguments.get(i).getClass().equals(JCTree.JCTypeApply.class)) {
        ParameterizedTypeTree parameterizedTypeTreeForTypeArgument =
            (ParameterizedTypeTree) typeArguments.get(i);
        Type argumentType = ASTHelpers.getType(parameterizedTypeTreeForTypeArgument);
        if (argumentType != null
            && argumentType.getTypeArguments() != null
            && argumentType.getTypeArguments().length() > 0) { // Nested generics
          nullableAnnotatedArguments.addAll(
              getNullableAnnotatedArgumentsListForNewClassTree(
                  parameterizedTypeTreeForTypeArgument, nestingLevel + 1, i));
        }
      }
    }

    return nullableAnnotatedArguments;
  }

  private static HashSet<String> getNullableAnnotatedArgumentsListForNormalTree(
      Type type, Config config, int nestingLevel, int nestedArgumentIndex) {
    HashSet<String> nullableTypeArguments = new HashSet<String>();
    if (type == null) {
      return nullableTypeArguments;
    }
    com.sun.tools.javac.util.List<Type> typeArguments = type.getTypeArguments();
    for (int index = 0; index < typeArguments.size(); index++) {
      com.sun.tools.javac.util.List<Attribute.TypeCompound> annotationMirrors =
          typeArguments.get(index).getAnnotationMirrors();
      boolean hasNullableAnnotation =
          Nullness.hasNullableAnnotation(annotationMirrors.stream(), config);
      if (hasNullableAnnotation) {
        String currentAnnotation =
            nestingLevel + "." + nestedArgumentIndex + "." + String.valueOf(index);
        nullableTypeArguments.add(currentAnnotation);
      }
    }
    for (int i = 0; i < typeArguments.size(); i++) {
      // check for nesting
      if (typeArguments.get(i).getTypeArguments().length() > 0) {
        // add values to the result set
        nullableTypeArguments.addAll(
            getNullableAnnotatedArgumentsListForNormalTree(
                typeArguments.get(i), config, nestingLevel + 1, i));
      }
    }
    return nullableTypeArguments;
  }

  private static HashSet<String> getNullableAnnotatedArgumentsList(JCTree tree, Config config) {
    HashSet<String> nullableAnnotatedArguments = new HashSet<String>();
    if (tree.getClass().getName().equals("com.sun.tools.javac.tree.JCTree$JCNewClass")) {
      ParameterizedTypeTree parameterizedTypeTree =
          (ParameterizedTypeTree) ((JCTree.JCNewClass) tree).getIdentifier();
      nullableAnnotatedArguments =
          getNullableAnnotatedArgumentsListForNewClassTree(parameterizedTypeTree, 0, 0);
    } else {

      if (tree != null) {
        Type type = ASTHelpers.getType(tree);
        if (type != null) {
          nullableAnnotatedArguments =
              getNullableAnnotatedArgumentsListForNormalTree(type, config, 0, 0);
        }
      }
    }
    return nullableAnnotatedArguments;
  }

  // Nested generics are not handled yet
  public static void checkAssignments(
      AssignmentTree tree, VisitorState state, NullAway analysis, Config config) {
    JCTree lhsTree = ((JCTree.JCAssign) tree).lhs;
    JCTree rhsTree = ((JCTree.JCAssign) tree).rhs;

    HashSet<String> lhsNullableAnnotatedArguments =
        getNullableAnnotatedArgumentsList(lhsTree, config);
    HashSet<String> rhsNullableAnnotatedArguments =
        getNullableAnnotatedArgumentsList(rhsTree, config);

    if (lhsNullableAnnotatedArguments.size() != rhsNullableAnnotatedArguments.size()) {
      invalidInstantiationError(tree, state, analysis);
    } else {
      for (String annotatedParam : lhsNullableAnnotatedArguments) {
        if (!rhsNullableAnnotatedArguments.contains(annotatedParam)) {
          invalidInstantiationError(tree, state, analysis);
          break;
        }
      }
    }
  }
}
