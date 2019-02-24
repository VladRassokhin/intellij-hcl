// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.intellij.plugins.hil.psi.impl.*;

public interface HILElementTypes {

  IElementType IL_BINARY_ADDITION_EXPRESSION = new HILElementType("IL_BINARY_ADDITION_EXPRESSION");
  IElementType IL_BINARY_AND_EXPRESSION = new HILElementType("IL_BINARY_AND_EXPRESSION");
  IElementType IL_BINARY_EQUALITY_EXPRESSION = new HILElementType("IL_BINARY_EQUALITY_EXPRESSION");
  IElementType IL_BINARY_MULTIPLY_EXPRESSION = new HILElementType("IL_BINARY_MULTIPLY_EXPRESSION");
  IElementType IL_BINARY_OR_EXPRESSION = new HILElementType("IL_BINARY_OR_EXPRESSION");
  IElementType IL_BINARY_RELATIONAL_EXPRESSION = new HILElementType("IL_BINARY_RELATIONAL_EXPRESSION");
  IElementType IL_CONDITIONAL_EXPRESSION = new HILElementType("IL_CONDITIONAL_EXPRESSION");
  IElementType IL_EXPRESSION = new HILElementType("IL_EXPRESSION");
  IElementType IL_EXPRESSION_HOLDER = new HILElementType("IL_EXPRESSION_HOLDER");
  IElementType IL_INDEX_SELECT_EXPRESSION = new HILElementType("IL_INDEX_SELECT_EXPRESSION");
  IElementType IL_LITERAL_EXPRESSION = new HILElementType("IL_LITERAL_EXPRESSION");
  IElementType IL_METHOD_CALL_EXPRESSION = new HILElementType("IL_METHOD_CALL_EXPRESSION");
  IElementType IL_PARAMETER_LIST = new HILElementType("IL_PARAMETER_LIST");
  IElementType IL_PARENTHESIZED_EXPRESSION = new HILElementType("IL_PARENTHESIZED_EXPRESSION");
  IElementType IL_SELECT_EXPRESSION = new HILElementType("IL_SELECT_EXPRESSION");
  IElementType IL_UNARY_EXPRESSION = new HILElementType("IL_UNARY_EXPRESSION");
  IElementType IL_VARIABLE = new HILElementType("IL_VARIABLE");

  IElementType COMMA = new HILTokenType(",");
  IElementType DOUBLE_QUOTED_STRING = new HILTokenType("DOUBLE_QUOTED_STRING");
  IElementType FALSE = new HILTokenType("false");
  IElementType ID = new HILTokenType("ID");
  IElementType INTERPOLATION_END = new HILTokenType("}");
  IElementType INTERPOLATION_START = new HILTokenType("${");
  IElementType L_BRACKET = new HILTokenType("[");
  IElementType L_PAREN = new HILTokenType("(");
  IElementType NULL = new HILTokenType("null");
  IElementType NUMBER = new HILTokenType("NUMBER");
  IElementType OP_AND_AND = new HILTokenType("&&");
  IElementType OP_COLON = new HILTokenType(":");
  IElementType OP_DIV = new HILTokenType("/");
  IElementType OP_DOT = new HILTokenType(".");
  IElementType OP_EQUAL = new HILTokenType("==");
  IElementType OP_GREATER = new HILTokenType(">");
  IElementType OP_GREATER_OR_EQUAL = new HILTokenType(">=");
  IElementType OP_LESS = new HILTokenType("<");
  IElementType OP_LESS_OR_EQUAL = new HILTokenType("<=");
  IElementType OP_MINUS = new HILTokenType("-");
  IElementType OP_MOD = new HILTokenType("%");
  IElementType OP_MUL = new HILTokenType("*");
  IElementType OP_NOT = new HILTokenType("!");
  IElementType OP_NOT_EQUAL = new HILTokenType("!=");
  IElementType OP_OR_OR = new HILTokenType("||");
  IElementType OP_PLUS = new HILTokenType("+");
  IElementType OP_QUEST = new HILTokenType("?");
  IElementType R_BRACKET = new HILTokenType("]");
  IElementType R_PAREN = new HILTokenType(")");
  IElementType TRUE = new HILTokenType("true");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == IL_BINARY_ADDITION_EXPRESSION) {
        return new ILBinaryAdditionExpressionImpl(node);
      }
      else if (type == IL_BINARY_AND_EXPRESSION) {
        return new ILBinaryAndExpressionImpl(node);
      }
      else if (type == IL_BINARY_EQUALITY_EXPRESSION) {
        return new ILBinaryEqualityExpressionImpl(node);
      }
      else if (type == IL_BINARY_MULTIPLY_EXPRESSION) {
        return new ILBinaryMultiplyExpressionImpl(node);
      }
      else if (type == IL_BINARY_OR_EXPRESSION) {
        return new ILBinaryOrExpressionImpl(node);
      }
      else if (type == IL_BINARY_RELATIONAL_EXPRESSION) {
        return new ILBinaryRelationalExpressionImpl(node);
      }
      else if (type == IL_CONDITIONAL_EXPRESSION) {
        return new ILConditionalExpressionImpl(node);
      }
      else if (type == IL_EXPRESSION_HOLDER) {
        return new ILExpressionHolderImpl(node);
      }
      else if (type == IL_INDEX_SELECT_EXPRESSION) {
        return new ILIndexSelectExpressionImpl(node);
      }
      else if (type == IL_LITERAL_EXPRESSION) {
        return new ILLiteralExpressionImpl(node);
      }
      else if (type == IL_METHOD_CALL_EXPRESSION) {
        return new ILMethodCallExpressionImpl(node);
      }
      else if (type == IL_PARAMETER_LIST) {
        return new ILParameterListImpl(node);
      }
      else if (type == IL_PARENTHESIZED_EXPRESSION) {
        return new ILParenthesizedExpressionImpl(node);
      }
      else if (type == IL_SELECT_EXPRESSION) {
        return new ILSelectExpressionImpl(node);
      }
      else if (type == IL_UNARY_EXPRESSION) {
        return new ILUnaryExpressionImpl(node);
      }
      else if (type == IL_VARIABLE) {
        return new ILVariableImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
