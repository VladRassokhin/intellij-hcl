// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.terraform.il;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.intellij.plugins.hcl.terraform.il.psi.impl.*;

public interface TILElementTypes {

  IElementType IL_BINARY_ADD_EXPRESSION = new TILElementType("IL_BINARY_ADD_EXPRESSION");
  IElementType IL_BINARY_MUL_EXPRESSION = new TILElementType("IL_BINARY_MUL_EXPRESSION");
  IElementType IL_EXPRESSION = new TILElementType("IL_EXPRESSION");
  IElementType IL_LITERAL_EXPRESSION = new TILElementType("IL_LITERAL_EXPRESSION");
  IElementType IL_METHOD_CALL_EXPRESSION = new TILElementType("IL_METHOD_CALL_EXPRESSION");
  IElementType IL_PARAMETER_LIST = new TILElementType("IL_PARAMETER_LIST");
  IElementType IL_PARENTHESIZED_EXPRESSION = new TILElementType("IL_PARENTHESIZED_EXPRESSION");
  IElementType IL_VARIABLE = new TILElementType("IL_VARIABLE");

  IElementType COMMA = new TILTokenType(",");
  IElementType DOUBLE_QUOTED_STRING = new TILTokenType("DOUBLE_QUOTED_STRING");
  IElementType EQUALS = new TILTokenType("=");
  IElementType FALSE = new TILTokenType("false");
  IElementType ID = new TILTokenType("ID");
  IElementType INTERPOLATION_START = new TILTokenType("${");
  IElementType L_CURLY = new TILTokenType("{");
  IElementType L_PAREN = new TILTokenType("(");
  IElementType NULL = new TILTokenType("null");
  IElementType NUMBER = new TILTokenType("NUMBER");
  IElementType OP_DIV = new TILTokenType("/");
  IElementType OP_MINUS = new TILTokenType("-");
  IElementType OP_MOD = new TILTokenType("%");
  IElementType OP_MUL = new TILTokenType("*");
  IElementType OP_PLUS = new TILTokenType("+");
  IElementType R_CURLY = new TILTokenType("}");
  IElementType R_PAREN = new TILTokenType(")");
  IElementType TRUE = new TILTokenType("true");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == IL_BINARY_ADD_EXPRESSION) {
        return new ILBinaryAddExpressionImpl(node);
      }
      else if (type == IL_BINARY_MUL_EXPRESSION) {
        return new ILBinaryMulExpressionImpl(node);
      }
      else if (type == IL_EXPRESSION) {
        return new ILExpressionImpl(node);
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
      else if (type == IL_VARIABLE) {
        return new ILVariableImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
