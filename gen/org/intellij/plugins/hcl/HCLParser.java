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
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HCLParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == EXPRESSION) {
      r = Expression(b, 0, -1);
    }
    else if (t == FOR_CONDITION) {
      r = ForCondition(b, 0);
    }
    else if (t == FOR_INTRO) {
      r = ForIntro(b, 0);
    }
    else if (t == PARAMETER_LIST) {
      r = ParameterList(b, 0);
    }
    else if (t == SPLAT_SELECT_EXPRESSION) {
      r = SplatSelectExpression(b, 0);
    }
    else if (t == TEMPLATE) {
      r = Template(b, 0);
    }
    else if (t == TEMPLATE_DIRECTIVE) {
      r = TemplateDirective(b, 0);
    }
    else if (t == TEMPLATE_EXPRESSION) {
      r = TemplateExpression(b, 0);
    }
    else if (t == TEMPLATE_FOR) {
      r = TemplateFor(b, 0);
    }
    else if (t == TEMPLATE_IF) {
      r = TemplateIf(b, 0);
    }
    else if (t == TEMPLATE_INTERPOLATION) {
      r = TemplateInterpolation(b, 0);
    }
    else if (t == ARRAY) {
      r = array(b, 0);
    }
    else if (t == BLOCK) {
      r = block(b, 0);
    }
    else if (t == BLOCK_OBJECT) {
      r = block_object(b, 0);
    }
    else if (t == BOOLEAN_LITERAL) {
      r = boolean_literal(b, 0);
    }
    else if (t == HEREDOC_CONTENT) {
      r = heredoc_content(b, 0);
    }
    else if (t == HEREDOC_LITERAL) {
      r = heredoc_literal(b, 0);
    }
    else if (t == HEREDOC_MARKER) {
      r = heredoc_marker(b, 0);
    }
    else if (t == IDENTIFIER) {
      r = identifier(b, 0);
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
    create_token_set_(ARRAY, BINARY_ADDITION_EXPRESSION, BINARY_AND_EXPRESSION, BINARY_EQUALITY_EXPRESSION,
      BINARY_MULTIPLY_EXPRESSION, BINARY_OR_EXPRESSION, BINARY_RELATIONAL_EXPRESSION, BLOCK_OBJECT,
      BOOLEAN_LITERAL, COLLECTION_VALUE, CONDITIONAL_EXPRESSION, EXPRESSION,
      FOR_ARRAY_EXPRESSION, FOR_OBJECT_EXPRESSION, HEREDOC_LITERAL, IDENTIFIER,
      INDEX_SELECT_EXPRESSION, LITERAL, METHOD_CALL_EXPRESSION, NULL_LITERAL,
      NUMBER_LITERAL, OBJECT, PARENTHESIZED_EXPRESSION, SELECT_EXPRESSION,
      SPLAT_SELECT_EXPRESSION, STRING_LITERAL, TEMPLATE_EXPRESSION, UNARY_EXPRESSION,
      VALUE, VARIABLE),
  };

  /* ********************************************************** */
  // "if" Expression
  public static boolean ForCondition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForCondition")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FOR_CONDITION, "<for condition>");
    r = consumeToken(b, "if");
    p = r; // pin = 1
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // "for" identifier ("," identifier)? ("in" Expression) ":"
  public static boolean ForIntro(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForIntro")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FOR_INTRO, "<for intro>");
    r = consumeToken(b, "for");
    p = r; // pin = 1
    r = r && report_error_(b, identifier(b, l + 1));
    r = p && report_error_(b, ForIntro_2(b, l + 1)) && r;
    r = p && report_error_(b, ForIntro_3(b, l + 1)) && r;
    r = p && consumeToken(b, OP_COLON) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ("," identifier)?
  private static boolean ForIntro_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForIntro_2")) return false;
    ForIntro_2_0(b, l + 1);
    return true;
  }

  // "," identifier
  private static boolean ForIntro_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForIntro_2_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && identifier(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // "in" Expression
  private static boolean ForIntro_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForIntro_3")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, "in");
    p = r; // pin = 1
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // (identifier | Expression) property
  static boolean ObjectElement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ObjectElement")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ObjectElement_0(b, l + 1);
    r = r && property(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // identifier | Expression
  private static boolean ObjectElement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ObjectElement_0")) return false;
    boolean r;
    r = identifier(b, l + 1);
    if (!r) r = Expression(b, l + 1, -1);
    return r;
  }

  /* ********************************************************** */
  // '(' (Expression? (',' Expression)* (',' | '...')?)? ')'
  public static boolean ParameterList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList")) return false;
    if (!nextTokenIs(b, L_PAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER_LIST, null);
    r = consumeToken(b, L_PAREN);
    p = r; // pin = 1
    r = r && report_error_(b, ParameterList_1(b, l + 1));
    r = p && consumeToken(b, R_PAREN) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (Expression? (',' Expression)* (',' | '...')?)?
  private static boolean ParameterList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1")) return false;
    ParameterList_1_0(b, l + 1);
    return true;
  }

  // Expression? (',' Expression)* (',' | '...')?
  private static boolean ParameterList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = ParameterList_1_0_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, ParameterList_1_0_1(b, l + 1));
    r = p && ParameterList_1_0_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // Expression?
  private static boolean ParameterList_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1_0_0")) return false;
    Expression(b, l + 1, -1);
    return true;
  }

  // (',' Expression)*
  private static boolean ParameterList_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ParameterList_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ParameterList_1_0_1", c)) break;
    }
    return true;
  }

  // ',' Expression
  private static boolean ParameterList_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1_0_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (',' | '...')?
  private static boolean ParameterList_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1_0_2")) return false;
    ParameterList_1_0_2_0(b, l + 1);
    return true;
  }

  // ',' | '...'
  private static boolean ParameterList_1_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, "...");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Expression ('[' '*' ']' (IndexSelectExpression | SelectExpression)?)
  public static boolean SplatSelectExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SplatSelectExpression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SPLAT_SELECT_EXPRESSION, "<splat select expression>");
    r = Expression(b, l + 1, -1);
    r = r && SplatSelectExpression_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '[' '*' ']' (IndexSelectExpression | SelectExpression)?
  private static boolean SplatSelectExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SplatSelectExpression_1")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeTokens(b, 1, L_BRACKET, OP_MUL, R_BRACKET);
    p = r; // pin = '\['
    r = r && SplatSelectExpression_1_3(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (IndexSelectExpression | SelectExpression)?
  private static boolean SplatSelectExpression_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SplatSelectExpression_1_3")) return false;
    SplatSelectExpression_1_3_0(b, l + 1);
    return true;
  }

  // IndexSelectExpression | SelectExpression
  private static boolean SplatSelectExpression_1_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SplatSelectExpression_1_3_0")) return false;
    boolean r;
    r = Expression(b, l + 1, 9);
    if (!r) r = Expression(b, l + 1, 8);
    return r;
  }

  /* ********************************************************** */
  // (
  //     TemplateExpression |
  //     TemplateInterpolation |
  //     TemplateDirective
  // )*
  public static boolean Template(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Template")) return false;
    Marker m = enter_section_(b, l, _NONE_, TEMPLATE, "<template>");
    while (true) {
      int c = current_position_(b);
      if (!Template_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Template", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  // TemplateExpression |
  //     TemplateInterpolation |
  //     TemplateDirective
  private static boolean Template_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Template_0")) return false;
    boolean r;
    r = TemplateExpression(b, l + 1);
    if (!r) r = TemplateInterpolation(b, l + 1);
    if (!r) r = TemplateDirective(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // TemplateIf | TemplateFor
  public static boolean TemplateDirective(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateDirective")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TEMPLATE_DIRECTIVE, "<template directive>");
    r = TemplateIf(b, l + 1);
    if (!r) r = TemplateFor(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // string_literal | heredoc_literal
  public static boolean TemplateExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateExpression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, TEMPLATE_EXPRESSION, "<template expression>");
    r = string_literal(b, l + 1);
    if (!r) r = heredoc_literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ("%{" | "%{~") "for" identifier ("," identifier) "in" Expression ("}" | "~}")
  //     Template
  //     ("%{" | "%{~") "endfor" ("}" | "~}")
  public static boolean TemplateFor(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateFor")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TEMPLATE_FOR, "<template for>");
    r = TemplateFor_0(b, l + 1);
    r = r && consumeToken(b, "for");
    r = r && identifier(b, l + 1);
    r = r && TemplateFor_3(b, l + 1);
    r = r && consumeToken(b, "in");
    r = r && Expression(b, l + 1, -1);
    r = r && TemplateFor_6(b, l + 1);
    r = r && Template(b, l + 1);
    r = r && TemplateFor_8(b, l + 1);
    r = r && consumeToken(b, "endfor");
    r = r && TemplateFor_10(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // "%{" | "%{~"
  private static boolean TemplateFor_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateFor_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "%{");
    if (!r) r = consumeToken(b, "%{~");
    exit_section_(b, m, null, r);
    return r;
  }

  // "," identifier
  private static boolean TemplateFor_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateFor_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && identifier(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // "}" | "~}"
  private static boolean TemplateFor_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateFor_6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_CURLY);
    if (!r) r = consumeToken(b, "~}");
    exit_section_(b, m, null, r);
    return r;
  }

  // "%{" | "%{~"
  private static boolean TemplateFor_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateFor_8")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "%{");
    if (!r) r = consumeToken(b, "%{~");
    exit_section_(b, m, null, r);
    return r;
  }

  // "}" | "~}"
  private static boolean TemplateFor_10(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateFor_10")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_CURLY);
    if (!r) r = consumeToken(b, "~}");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ("%{" | "%{~") "if" Expression ("}" | "~}")
  //     Template
  //     (
  //         ("%{" | "%{~") "else" ("}" | "~}")
  //         Template
  //     )?
  //     ("%{" | "%{~") "endif" ("}" | "~}")
  public static boolean TemplateIf(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateIf")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TEMPLATE_IF, "<template if>");
    r = TemplateIf_0(b, l + 1);
    r = r && consumeToken(b, "if");
    r = r && Expression(b, l + 1, -1);
    r = r && TemplateIf_3(b, l + 1);
    r = r && Template(b, l + 1);
    r = r && TemplateIf_5(b, l + 1);
    r = r && TemplateIf_6(b, l + 1);
    r = r && consumeToken(b, "endif");
    r = r && TemplateIf_8(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // "%{" | "%{~"
  private static boolean TemplateIf_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateIf_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "%{");
    if (!r) r = consumeToken(b, "%{~");
    exit_section_(b, m, null, r);
    return r;
  }

  // "}" | "~}"
  private static boolean TemplateIf_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateIf_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_CURLY);
    if (!r) r = consumeToken(b, "~}");
    exit_section_(b, m, null, r);
    return r;
  }

  // (
  //         ("%{" | "%{~") "else" ("}" | "~}")
  //         Template
  //     )?
  private static boolean TemplateIf_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateIf_5")) return false;
    TemplateIf_5_0(b, l + 1);
    return true;
  }

  // ("%{" | "%{~") "else" ("}" | "~}")
  //         Template
  private static boolean TemplateIf_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateIf_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TemplateIf_5_0_0(b, l + 1);
    r = r && consumeToken(b, "else");
    r = r && TemplateIf_5_0_2(b, l + 1);
    r = r && Template(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // "%{" | "%{~"
  private static boolean TemplateIf_5_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateIf_5_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "%{");
    if (!r) r = consumeToken(b, "%{~");
    exit_section_(b, m, null, r);
    return r;
  }

  // "}" | "~}"
  private static boolean TemplateIf_5_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateIf_5_0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_CURLY);
    if (!r) r = consumeToken(b, "~}");
    exit_section_(b, m, null, r);
    return r;
  }

  // "%{" | "%{~"
  private static boolean TemplateIf_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateIf_6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "%{");
    if (!r) r = consumeToken(b, "%{~");
    exit_section_(b, m, null, r);
    return r;
  }

  // "}" | "~}"
  private static boolean TemplateIf_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateIf_8")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_CURLY);
    if (!r) r = consumeToken(b, "~}");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ("${" | "${~") Expression ("}" | "~}")
  public static boolean TemplateInterpolation(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateInterpolation")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TEMPLATE_INTERPOLATION, "<template interpolation>");
    r = TemplateInterpolation_0(b, l + 1);
    r = r && Expression(b, l + 1, -1);
    r = r && TemplateInterpolation_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // "${" | "${~"
  private static boolean TemplateInterpolation_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateInterpolation_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "${");
    if (!r) r = consumeToken(b, "${~");
    exit_section_(b, m, null, r);
    return r;
  }

  // "}" | "~}"
  private static boolean TemplateInterpolation_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateInterpolation_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_CURLY);
    if (!r) r = consumeToken(b, "~}");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // OP_PLUS | OP_MINUS
  static boolean addOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "addOp")) return false;
    if (!nextTokenIs(b, "<operator>", OP_MINUS, OP_PLUS)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = consumeToken(b, OP_PLUS);
    if (!r) r = consumeToken(b, OP_MINUS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // OP_AND_AND
  static boolean andOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "andOp")) return false;
    if (!nextTokenIs(b, "<operator>", OP_AND_AND)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = consumeToken(b, OP_AND_AND);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '[' array_element* ']'
  public static boolean array(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array")) return false;
    if (!nextTokenIs(b, L_BRACKET)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, ARRAY, null);
    r = consumeToken(b, L_BRACKET);
    p = r; // pin = 1
    r = r && report_error_(b, array_1(b, l + 1));
    r = p && consumeToken(b, R_BRACKET) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // array_element*
  private static boolean array_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!array_element(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "array_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // (Expression) (','|&']')
  static boolean array_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_element")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = array_element_0(b, l + 1);
    p = r; // pin = 1
    r = r && array_element_1(b, l + 1);
    exit_section_(b, l, m, r, p, not_bracket_or_next_value_parser_);
    return r || p;
  }

  // (Expression)
  private static boolean array_element_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_element_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Expression(b, l + 1, -1);
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
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, R_BRACKET);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // property_name* block_object
  public static boolean block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, BLOCK, "<block>");
    r = block_0(b, l + 1);
    r = r && block_object(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // property_name*
  private static boolean block_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!property_name(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "block_0", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '{' object_element* '}'
  public static boolean block_object(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_object")) return false;
    if (!nextTokenIs(b, L_CURLY)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, BLOCK_OBJECT, null);
    r = consumeToken(b, L_CURLY);
    p = r; // pin = 1
    r = r && report_error_(b, block_object_1(b, l + 1));
    r = p && consumeToken(b, R_CURLY) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // object_element*
  private static boolean block_object_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_object_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!object_element(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "block_object_1", c)) break;
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
    if (!nextTokenIs(b, "<boolean>", FALSE, TRUE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BOOLEAN_LITERAL, "<boolean>");
    r = consumeToken(b, TRUE);
    if (!r) r = consumeToken(b, FALSE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // OP_EQUAL
  //                     | OP_NOT_EQUAL
  static boolean equalityOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "equalityOp")) return false;
    if (!nextTokenIs(b, "<operator>", OP_EQUAL, OP_NOT_EQUAL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = consumeToken(b, OP_EQUAL);
    if (!r) r = consumeToken(b, OP_NOT_EQUAL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // HD_START heredoc_marker heredoc_content heredoc_marker
  static boolean heredoc(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredoc")) return false;
    if (!nextTokenIs(b, HD_START)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, HD_START);
    r = r && heredoc_marker(b, l + 1);
    r = r && heredoc_content(b, l + 1);
    r = r && heredoc_marker(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // heredoc_line*
  public static boolean heredoc_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredoc_content")) return false;
    Marker m = enter_section_(b, l, _NONE_, HEREDOC_CONTENT, "<heredoc content>");
    while (true) {
      int c = current_position_(b);
      if (!heredoc_line(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "heredoc_content", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // HD_LINE HD_EOL
  static boolean heredoc_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredoc_line")) return false;
    if (!nextTokenIs(b, "<heredoc content>", HD_LINE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<heredoc content>");
    r = consumeTokens(b, 0, HD_LINE, HD_EOL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // heredoc
  public static boolean heredoc_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredoc_literal")) return false;
    if (!nextTokenIs(b, "<heredoc>", HD_START)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, HEREDOC_LITERAL, "<heredoc>");
    r = heredoc(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // HD_MARKER
  public static boolean heredoc_marker(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredoc_marker")) return false;
    if (!nextTokenIs(b, "<heredoc anchor>", HD_MARKER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, HEREDOC_MARKER, "<heredoc anchor>");
    r = consumeToken(b, HD_MARKER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ID
  public static boolean identifier(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "identifier")) return false;
    if (!nextTokenIs(b, "<identifier>", ID)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IDENTIFIER, "<identifier>");
    r = consumeToken(b, ID);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // OP_MUL | OP_DIV | OP_MOD
  static boolean mulOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "mulOp")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = consumeToken(b, OP_MUL);
    if (!r) r = consumeToken(b, OP_DIV);
    if (!r) r = consumeToken(b, OP_MOD);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !('}'|value)
  static boolean not_brace_or_next_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_brace_or_next_value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !not_brace_or_next_value_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
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
  // !('}'|ObjectElement)
  static boolean not_brace_or_next_value_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_brace_or_next_value_2")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !not_brace_or_next_value_2_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '}'|ObjectElement
  private static boolean not_brace_or_next_value_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_brace_or_next_value_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_CURLY);
    if (!r) r = ObjectElement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(']'|Expression)
  static boolean not_bracket_or_next_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_bracket_or_next_value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !not_bracket_or_next_value_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ']'|Expression
  private static boolean not_bracket_or_next_value_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_bracket_or_next_value_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_BRACKET);
    if (!r) r = Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // NULL
  public static boolean null_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "null_literal")) return false;
    if (!nextTokenIs(b, "<null>", NULL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NULL_LITERAL, "<null>");
    r = consumeToken(b, NULL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // NUMBER
  public static boolean number_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "number_literal")) return false;
    if (!nextTokenIs(b, "<number>", NUMBER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NUMBER_LITERAL, "<number>");
    r = consumeToken(b, NUMBER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '{' object_element2* '}'
  public static boolean object(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object")) return false;
    if (!nextTokenIs(b, L_CURLY)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, OBJECT, null);
    r = consumeToken(b, L_CURLY);
    p = r; // pin = 1
    r = r && report_error_(b, object_1(b, l + 1));
    r = p && consumeToken(b, R_CURLY) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // object_element2*
  private static boolean object_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!object_element2(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "object_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // (block_outer | property_outer) (','|&'}')?
  static boolean object_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = object_element_0(b, l + 1);
    p = r; // pin = 1
    r = r && object_element_1(b, l + 1);
    exit_section_(b, l, m, r, p, not_brace_or_next_value_parser_);
    return r || p;
  }

  // block_outer | property_outer
  private static boolean object_element_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element_0")) return false;
    boolean r;
    r = block_outer(b, l + 1);
    if (!r) r = property_outer(b, l + 1);
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
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, R_CURLY);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (ObjectElement) (','|&'}')?
  static boolean object_element2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element2")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = object_element2_0(b, l + 1);
    p = r; // pin = 1
    r = r && object_element2_1(b, l + 1);
    exit_section_(b, l, m, r, p, not_brace_or_next_value_2_parser_);
    return r || p;
  }

  // (ObjectElement)
  private static boolean object_element2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ObjectElement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (','|&'}')?
  private static boolean object_element2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element2_1")) return false;
    object_element2_1_0(b, l + 1);
    return true;
  }

  // ','|&'}'
  private static boolean object_element2_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element2_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    if (!r) r = object_element2_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &'}'
  private static boolean object_element2_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_element2_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, R_CURLY);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // OP_OR_OR
  static boolean orOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "orOp")) return false;
    if (!nextTokenIs(b, "<operator>", OP_OR_OR)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = consumeToken(b, OP_OR_OR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '=' Expression
  public static boolean property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property")) return false;
    if (!nextTokenIs(b, EQUALS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, PROPERTY, null);
    r = consumeToken(b, EQUALS);
    p = r; // pin = 1
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // identifier | string_literal
  static boolean property_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property_name")) return false;
    boolean r;
    r = identifier(b, l + 1);
    if (!r) r = string_literal(b, l + 1);
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
  // OP_LESS
  //                        | OP_GREATER
  //                        | OP_LESS_OR_EQUAL
  //                        | OP_GREATER_OR_EQUAL
  static boolean relationalOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "relationalOp")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = consumeToken(b, OP_LESS);
    if (!r) r = consumeToken(b, OP_GREATER);
    if (!r) r = consumeToken(b, OP_LESS_OR_EQUAL);
    if (!r) r = consumeToken(b, OP_GREATER_OR_EQUAL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // root_element*
  static boolean root(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root")) return false;
    while (true) {
      int c = current_position_(b);
      if (!root_element(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "root", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // block_outer | property_outer
  static boolean root_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "root_element")) return false;
    boolean r;
    r = block_outer(b, l + 1);
    if (!r) r = property_outer(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING
  public static boolean string_literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_literal")) return false;
    if (!nextTokenIs(b, "<string literal>", DOUBLE_QUOTED_STRING, SINGLE_QUOTED_STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRING_LITERAL, "<string literal>");
    r = consumeToken(b, DOUBLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, SINGLE_QUOTED_STRING);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // OP_MINUS | OP_NOT
  static boolean unaryOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unaryOp")) return false;
    if (!nextTokenIs(b, "<operator>", OP_MINUS, OP_NOT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = consumeToken(b, OP_MINUS);
    if (!r) r = consumeToken(b, OP_NOT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // literal | identifier | array | object
  public static boolean value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, VALUE, "<value>");
    r = literal(b, l + 1);
    if (!r) r = identifier(b, l + 1);
    if (!r) r = array(b, l + 1);
    if (!r) r = object(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Expression root: Expression
  // Operator priority table:
  // 0: PREFIX(ParenthesizedExpression)
  // 1: POSTFIX(ConditionalExpression)
  // 2: BINARY(BinaryOrExpression)
  // 3: BINARY(BinaryAndExpression)
  // 4: BINARY(BinaryEqualityExpression)
  // 5: BINARY(BinaryRelationalExpression)
  // 6: BINARY(BinaryAdditionExpression)
  // 7: BINARY(BinaryMultiplyExpression)
  // 8: POSTFIX(MethodCallExpression)
  // 9: BINARY(SelectExpression)
  // 10: POSTFIX(IndexSelectExpression)
  // 11: PREFIX(ForArrayExpression)
  // 12: ATOM(ForObjectExpression)
  // 13: ATOM(CollectionValue)
  // 14: ATOM(Variable)
  // 15: PREFIX(UnaryExpression)
  // 16: ATOM(literal)
  public static boolean Expression(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "Expression")) return false;
    addVariant(b, "<expression>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expression>");
    r = ParenthesizedExpression(b, l + 1);
    if (!r) r = ForArrayExpression(b, l + 1);
    if (!r) r = ForObjectExpression(b, l + 1);
    if (!r) r = CollectionValue(b, l + 1);
    if (!r) r = Variable(b, l + 1);
    if (!r) r = UnaryExpression(b, l + 1);
    if (!r) r = literal(b, l + 1);
    p = r;
    r = r && Expression_0(b, l + 1, g);
    exit_section_(b, l, m, null, r, p, null);
    return r || p;
  }

  public static boolean Expression_0(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "Expression_0")) return false;
    boolean r = true;
    while (true) {
      Marker m = enter_section_(b, l, _LEFT_, null);
      if (g < 1 && ConditionalExpression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, CONDITIONAL_EXPRESSION, r, true, null);
      }
      else if (g < 2 && orOp(b, l + 1)) {
        r = Expression(b, l, 2);
        exit_section_(b, l, m, BINARY_OR_EXPRESSION, r, true, null);
      }
      else if (g < 3 && andOp(b, l + 1)) {
        r = Expression(b, l, 3);
        exit_section_(b, l, m, BINARY_AND_EXPRESSION, r, true, null);
      }
      else if (g < 4 && equalityOp(b, l + 1)) {
        r = Expression(b, l, 4);
        exit_section_(b, l, m, BINARY_EQUALITY_EXPRESSION, r, true, null);
      }
      else if (g < 5 && relationalOp(b, l + 1)) {
        r = Expression(b, l, 5);
        exit_section_(b, l, m, BINARY_RELATIONAL_EXPRESSION, r, true, null);
      }
      else if (g < 6 && addOp(b, l + 1)) {
        r = Expression(b, l, 6);
        exit_section_(b, l, m, BINARY_ADDITION_EXPRESSION, r, true, null);
      }
      else if (g < 7 && mulOp(b, l + 1)) {
        r = Expression(b, l, 7);
        exit_section_(b, l, m, BINARY_MULTIPLY_EXPRESSION, r, true, null);
      }
      else if (g < 8 && ParameterList(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, METHOD_CALL_EXPRESSION, r, true, null);
      }
      else if (g < 9 && consumeTokenSmart(b, OP_DOT)) {
        r = Expression(b, l, 9);
        exit_section_(b, l, m, SELECT_EXPRESSION, r, true, null);
      }
      else if (g < 10 && IndexSelectExpression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, INDEX_SELECT_EXPRESSION, r, true, null);
      }
      else {
        exit_section_(b, l, m, null, false, false, null);
        break;
      }
    }
    return r;
  }

  public static boolean ParenthesizedExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParenthesizedExpression")) return false;
    if (!nextTokenIsSmart(b, L_PAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = ParenthesizedExpression_0(b, l + 1);
    p = r;
    r = p && Expression(b, l, 0);
    r = p && report_error_(b, ParenthesizedExpression_1(b, l + 1)) && r;
    exit_section_(b, l, m, PARENTHESIZED_EXPRESSION, r, p, null);
    return r || p;
  }

  // '(' <<push 0>>
  private static boolean ParenthesizedExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParenthesizedExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, L_PAREN);
    r = r && push(b, l + 1, 0);
    exit_section_(b, m, null, r);
    return r;
  }

  // ')' <<pop>>
  private static boolean ParenthesizedExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParenthesizedExpression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_PAREN);
    r = r && pop(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '?' <<push 1>> Expression (':' Expression) <<pop>>
  private static boolean ConditionalExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ConditionalExpression_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeTokenSmart(b, OP_QUEST);
    p = r; // pin = '\?'|'\:'
    r = r && report_error_(b, push(b, l + 1, 1));
    r = p && report_error_(b, Expression(b, l + 1, -1)) && r;
    r = p && report_error_(b, ConditionalExpression_0_3(b, l + 1)) && r;
    r = p && pop(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ':' Expression
  private static boolean ConditionalExpression_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ConditionalExpression_0_3")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeTokenSmart(b, OP_COLON);
    p = r; // pin = '\?'|'\:'
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // '[' Expression ']'
  private static boolean IndexSelectExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IndexSelectExpression_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeTokenSmart(b, L_BRACKET);
    p = r; // pin = '\['
    r = r && report_error_(b, Expression(b, l + 1, -1));
    r = p && consumeToken(b, R_BRACKET) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  public static boolean ForArrayExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForArrayExpression")) return false;
    if (!nextTokenIsSmart(b, L_BRACKET)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = ForArrayExpression_0(b, l + 1);
    p = r;
    r = p && Expression(b, l, 11);
    r = p && report_error_(b, ForArrayExpression_1(b, l + 1)) && r;
    exit_section_(b, l, m, FOR_ARRAY_EXPRESSION, r, p, null);
    return r || p;
  }

  // "[" ForIntro
  private static boolean ForArrayExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForArrayExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, L_BRACKET);
    r = r && ForIntro(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ForCondition? "]"
  private static boolean ForArrayExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForArrayExpression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ForArrayExpression_1_0(b, l + 1);
    r = r && consumeToken(b, R_BRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // ForCondition?
  private static boolean ForArrayExpression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForArrayExpression_1_0")) return false;
    ForCondition(b, l + 1);
    return true;
  }

  // "{" ForIntro Expression "=>" Expression "..."? ForCondition? "}"
  public static boolean ForObjectExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForObjectExpression")) return false;
    if (!nextTokenIsSmart(b, L_CURLY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, L_CURLY);
    r = r && ForIntro(b, l + 1);
    r = r && Expression(b, l + 1, -1);
    r = r && consumeToken(b, "=>");
    r = r && Expression(b, l + 1, -1);
    r = r && ForObjectExpression_5(b, l + 1);
    r = r && ForObjectExpression_6(b, l + 1);
    r = r && consumeToken(b, R_CURLY);
    exit_section_(b, m, FOR_OBJECT_EXPRESSION, r);
    return r;
  }

  // "..."?
  private static boolean ForObjectExpression_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForObjectExpression_5")) return false;
    consumeTokenSmart(b, "...");
    return true;
  }

  // ForCondition?
  private static boolean ForObjectExpression_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForObjectExpression_6")) return false;
    ForCondition(b, l + 1);
    return true;
  }

  // array | object
  public static boolean CollectionValue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CollectionValue")) return false;
    if (!nextTokenIsSmart(b, L_BRACKET, L_CURLY)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, COLLECTION_VALUE, "<collection value>");
    r = array(b, l + 1);
    if (!r) r = object(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // identifier | '*'
  public static boolean Variable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Variable")) return false;
    if (!nextTokenIsSmart(b, OP_MUL, ID)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, VARIABLE, "<Identifier>");
    r = identifier(b, l + 1);
    if (!r) r = consumeTokenSmart(b, OP_MUL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  public static boolean UnaryExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryExpression")) return false;
    if (!nextTokenIsSmart(b, OP_MINUS, OP_NOT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = unaryOp(b, l + 1);
    p = r;
    r = p && Expression(b, l, 15);
    exit_section_(b, l, m, UNARY_EXPRESSION, r, p, null);
    return r || p;
  }

  // string_literal | number_literal | boolean_literal | null_literal | heredoc_literal
  public static boolean literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "literal")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, LITERAL, "<literal>");
    r = string_literal(b, l + 1);
    if (!r) r = number_literal(b, l + 1);
    if (!r) r = boolean_literal(b, l + 1);
    if (!r) r = null_literal(b, l + 1);
    if (!r) r = heredoc_literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  final static Parser not_brace_or_next_value_2_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return not_brace_or_next_value_2(b, l + 1);
    }
  };
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
