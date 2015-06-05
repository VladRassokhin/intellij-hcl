// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.intellij.plugins.hcl.HCLElementTypes.*;
import static org.intellij.plugins.hcl.psi.HCLParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HCLParser implements PsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == ARRAY) {
      r = array(b, 0);
    }
    else if (t == BLOCK) {
      r = block(b, 0);
    }
    else if (t == BOOLEAN_LITERAL) {
      r = boolean_literal(b, 0);
    }
    else if (t == IDENTIFIER) {
      r = identifier(b, 0);
    }
    else if (t == LITERAL) {
      r = literal(b, 0);
    }
    else if (t == NULL_LITERAL) {
      r = null_literal(b, 0);
    }
    else if (t == NUMBER_LITERAL) {
      r = number_literal(b, 0);
    }
    else if (t == OBJECT) {
      r = object(b, 0);
    }
    else if (t == PROPERTY) {
      r = property(b, 0);
    }
    else if (t == STRING_LITERAL) {
      r = string_literal(b, 0);
    }
    else if (t == VALUE) {
      r = value(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return root(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(ARRAY, OBJECT),
    create_token_set_(BOOLEAN_LITERAL, LITERAL, NULL_LITERAL, NUMBER_LITERAL,
      STRING_LITERAL),
    create_token_set_(ARRAY, BOOLEAN_LITERAL, IDENTIFIER, LITERAL,
      NULL_LITERAL, NUMBER_LITERAL, OBJECT, STRING_LITERAL,
      VALUE),
  };

  /* ********************************************************** */
  // '[' array_element* ']'
  public static boolean array(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array")) return false;
    if (!nextTokenIs(b, L_BRACKET)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, L_BRACKET);
    p = r; // pin = 1
    r = r && report_error_(b, array_1(b, l + 1));
    r = p && consumeToken(b, R_BRACKET) && r;
    exit_section_(b, l, m, ARRAY, r, p, null);
    return r || p;
  }

  // array_element*
  private static boolean array_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!array_element(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "array_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // (literal | array) (','|&']')
  static boolean array_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_element")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = array_element_0(b, l + 1);
    p = r; // pin = 1
    r = r && array_element_1(b, l + 1);
    exit_section_(b, l, m, null, r, p, not_bracket_or_next_value_parser_);
    return r || p;
  }

  // literal | array
  private static boolean array_element_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_element_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = literal(b, l + 1);
    if (!r) r = array(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','|&']'
  private static boolean array_element_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_element_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    if (!r) r = array_element_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &']'
  private static boolean array_element_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_element_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_, null);
    r = consumeToken(b, R_BRACKET);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // property_name* object
  public static boolean block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, "<block>");
    r = block_0(b, l + 1);
    r = r && object(b, l + 1);
    exit_section_(b, l, m, BLOCK, r, false, null);
    return r;
  }

  // property_name*
  private static boolean block_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_0")) return false;
    int c = current_position_(b);
    while (true) {
      if (!property_name(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "block_0", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // property_name block
  static boolean block_outer(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_outer")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = property_name(b, l + 1);
    r = r && block(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // TRUE | FALSE
  public static boolean boolean_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boolean_literal")) return false;
    if (!nextTokenIs(b, "<boolean literal>", FALSE, TRUE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<boolean literal>");
    r = consumeToken(b, TRUE);
    if (!r) r = consumeToken(b, FALSE);
    exit_section_(b, l, m, BOOLEAN_LITERAL, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ID
  public static boolean identifier(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "identifier")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ID);
    exit_section_(b, m, IDENTIFIER, r);
    return r;
  }

  /* ********************************************************** */
  // string_literal | number_literal | boolean_literal | null_literal
  public static boolean literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "literal")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, "<literal>");
    r = string_literal(b, l + 1);
    if (!r) r = number_literal(b, l + 1);
    if (!r) r = boolean_literal(b, l + 1);
    if (!r) r = null_literal(b, l + 1);
    exit_section_(b, l, m, LITERAL, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !('}'|value)
  static boolean not_brace_or_next_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_brace_or_next_value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !not_brace_or_next_value_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // '}'|value
  private static boolean not_brace_or_next_value_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_brace_or_next_value_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_CURLY);
    if (!r) r = value(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(']'|value)
  static boolean not_bracket_or_next_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_bracket_or_next_value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !not_bracket_or_next_value_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ']'|value
  private static boolean not_bracket_or_next_value_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_bracket_or_next_value_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_BRACKET);
    if (!r) r = value(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // NULL
  public static boolean null_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "null_literal")) return false;
    if (!nextTokenIs(b, NULL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NULL);
    exit_section_(b, m, NULL_LITERAL, r);
    return r;
  }

  /* ********************************************************** */
  // NUMBER
  public static boolean number_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "number_literal")) return false;
    if (!nextTokenIs(b, NUMBER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NUMBER);
    exit_section_(b, m, NUMBER_LITERAL, r);
    return r;
  }

  /* ********************************************************** */
  // '{' object_element* '}'
  public static boolean object(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object")) return false;
    if (!nextTokenIs(b, L_CURLY)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, L_CURLY);
    p = r; // pin = 1
    r = r && report_error_(b, object_1(b, l + 1));
    r = p && consumeToken(b, R_CURLY) && r;
    exit_section_(b, l, m, OBJECT, r, p, null);
    return r || p;
  }

  // object_element*
  private static boolean object_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!object_element(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "object_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // (block_outer | property_outer) (','|&'}')?
  static boolean object_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = object_element_0(b, l + 1);
    p = r; // pin = 1
    r = r && object_element_1(b, l + 1);
    exit_section_(b, l, m, null, r, p, not_brace_or_next_value_parser_);
    return r || p;
  }

  // block_outer | property_outer
  private static boolean object_element_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = block_outer(b, l + 1);
    if (!r) r = property_outer(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (','|&'}')?
  private static boolean object_element_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element_1")) return false;
    object_element_1_0(b, l + 1);
    return true;
  }

  // ','|&'}'
  private static boolean object_element_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    if (!r) r = object_element_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &'}'
  private static boolean object_element_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_, null);
    r = consumeToken(b, R_CURLY);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '=' value
  public static boolean property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property")) return false;
    if (!nextTokenIs(b, EQUALS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, EQUALS);
    p = r; // pin = 1
    r = r && value(b, l + 1);
    exit_section_(b, l, m, PROPERTY, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // identifier | string_literal
  static boolean property_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property_name")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = identifier(b, l + 1);
    if (!r) r = string_literal(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // property_name property
  static boolean property_outer(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property_outer")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = property_name(b, l + 1);
    r = r && property(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // root_element*
  static boolean root(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root")) return false;
    int c = current_position_(b);
    while (true) {
      if (!root_element(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "root", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // object | block_outer | property_outer
  static boolean root_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root_element")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = object(b, l + 1);
    if (!r) r = block_outer(b, l + 1);
    if (!r) r = property_outer(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING
  public static boolean string_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_literal")) return false;
    if (!nextTokenIs(b, "<string literal>", DOUBLE_QUOTED_STRING, SINGLE_QUOTED_STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<string literal>");
    r = consumeToken(b, DOUBLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, SINGLE_QUOTED_STRING);
    exit_section_(b, l, m, STRING_LITERAL, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // literal | identifier | array | object
  public static boolean value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, "<value>");
    r = literal(b, l + 1);
    if (!r) r = identifier(b, l + 1);
    if (!r) r = array(b, l + 1);
    if (!r) r = object(b, l + 1);
    exit_section_(b, l, m, VALUE, r, false, null);
    return r;
  }

  final static Parser not_brace_or_next_value_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return not_brace_or_next_value(b, l + 1);
    }
  };
  final static Parser not_bracket_or_next_value_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return not_bracket_or_next_value(b, l + 1);
    }
  };
}
