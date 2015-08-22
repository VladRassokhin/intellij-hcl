// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.intellij.plugins.hcl.psi.impl.*;

public interface HCLElementTypes {

  IElementType ARRAY = new HCLElementType("ARRAY");
  IElementType BLOCK = new HCLElementType("BLOCK");
  IElementType BOOLEAN_LITERAL = new HCLElementType("BOOLEAN_LITERAL");
  IElementType HEREDOC_LINE = new HCLElementType("HEREDOC_LINE");
  IElementType HEREDOC_LITERAL = new HCLElementType("HEREDOC_LITERAL");
  IElementType HEREDOC_MARKER = new HCLElementType("HEREDOC_MARKER");
  IElementType IDENTIFIER = new HCLElementType("IDENTIFIER");
  IElementType LITERAL = new HCLElementType("LITERAL");
  IElementType NULL_LITERAL = new HCLElementType("NULL_LITERAL");
  IElementType NUMBER_LITERAL = new HCLElementType("NUMBER_LITERAL");
  IElementType OBJECT = new HCLElementType("OBJECT");
  IElementType PROPERTY = new HCLElementType("PROPERTY");
  IElementType STRING_LITERAL = new HCLElementType("STRING_LITERAL");
  IElementType VALUE = new HCLElementType("VALUE");

  IElementType BLOCK_COMMENT = new HCLTokenType("block_comment");
  IElementType COMMA = new HCLTokenType(",");
  IElementType DOUBLE_QUOTED_STRING = new HCLTokenType("DOUBLE_QUOTED_STRING");
  IElementType EQUALS = new HCLTokenType("=");
  IElementType FALSE = new HCLTokenType("false");
  IElementType HD_LINE = new HCLTokenType("HD_LINE");
  IElementType HD_MARKER = new HCLTokenType("HD_MARKER");
  IElementType HD_START = new HCLTokenType("HD_START");
  IElementType ID = new HCLTokenType("ID");
  IElementType LINE_COMMENT = new HCLTokenType("line_comment");
  IElementType L_BRACKET = new HCLTokenType("[");
  IElementType L_CURLY = new HCLTokenType("{");
  IElementType NULL = new HCLTokenType("null");
  IElementType NUMBER = new HCLTokenType("NUMBER");
  IElementType R_BRACKET = new HCLTokenType("]");
  IElementType R_CURLY = new HCLTokenType("}");
  IElementType SINGLE_QUOTED_STRING = new HCLTokenType("SINGLE_QUOTED_STRING");
  IElementType TRUE = new HCLTokenType("true");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == ARRAY) {
        return new HCLArrayImpl(node);
      }
      else if (type == BLOCK) {
        return new HCLBlockImpl(node);
      }
      else if (type == BOOLEAN_LITERAL) {
        return new HCLBooleanLiteralImpl(node);
      }
      else if (type == HEREDOC_LINE) {
        return new HCLHeredocLineImpl(node);
      }
      else if (type == HEREDOC_LITERAL) {
        return new HCLHeredocLiteralImpl(node);
      }
      else if (type == HEREDOC_MARKER) {
        return new HCLHeredocMarkerImpl(node);
      }
      else if (type == IDENTIFIER) {
        return new HCLIdentifierImpl(node);
      }
      else if (type == LITERAL) {
        return new HCLLiteralImpl(node);
      }
      else if (type == NULL_LITERAL) {
        return new HCLNullLiteralImpl(node);
      }
      else if (type == NUMBER_LITERAL) {
        return new HCLNumberLiteralImpl(node);
      }
      else if (type == OBJECT) {
        return new HCLObjectImpl(node);
      }
      else if (type == PROPERTY) {
        return new HCLPropertyImpl(node);
      }
      else if (type == STRING_LITERAL) {
        return new HCLStringLiteralImpl(node);
      }
      else if (type == VALUE) {
        return new HCLValueImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
