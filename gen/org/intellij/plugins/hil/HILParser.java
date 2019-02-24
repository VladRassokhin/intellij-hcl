// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.intellij.plugins.hil.HILElementTypes.*;
import static org.intellij.plugins.hil.psi.HILParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings("ALL")
public class HILParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == IL_EXPRESSION) {
      r = ILExpression(b, 0, -1);
    }
    else if (t == IL_PARAMETER_LIST) {
      r = ILParameterList(b, 0);
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
    create_token_set_(IL_BINARY_ADDITION_EXPRESSION, IL_BINARY_AND_EXPRESSION, IL_BINARY_EQUALITY_EXPRESSION, IL_BINARY_MULTIPLY_EXPRESSION,
      IL_BINARY_OR_EXPRESSION, IL_BINARY_RELATIONAL_EXPRESSION, IL_CONDITIONAL_EXPRESSION, IL_EXPRESSION,
      IL_EXPRESSION_HOLDER, IL_INDEX_SELECT_EXPRESSION, IL_LITERAL_EXPRESSION, IL_METHOD_CALL_EXPRESSION,
      IL_PARENTHESIZED_EXPRESSION, IL_SELECT_EXPRESSION, IL_UNARY_EXPRESSION, IL_VARIABLE),
  };

  /* ********************************************************** */
  // OP_PLUS | OP_MINUS
  static boolean AddOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AddOp")) return false;
    if (!nextTokenIs(b, "<operator>", OP_MINUS, OP_PLUS)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = consumeToken(b, OP_PLUS);
    if (!r) r = consumeToken(b, OP_MINUS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // orOp
  //   | andOp
  //   | equalityOp
  //   | relationalOp
  static boolean ConditionOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ConditionOp")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = orOp(b, l + 1);
    if (!r) r = andOp(b, l + 1);
    if (!r) r = equalityOp(b, l + 1);
    if (!r) r = relationalOp(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '(' (ILExpression? (',' ILExpression )* )?')'
  public static boolean ILParameterList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParameterList")) return false;
    if (!nextTokenIs(b, L_PAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IL_PARAMETER_LIST, null);
    r = consumeToken(b, L_PAREN);
    p = r; // pin = 1
    r = r && report_error_(b, ILParameterList_1(b, l + 1));
    r = p && consumeToken(b, R_PAREN) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (ILExpression? (',' ILExpression )* )?
  private static boolean ILParameterList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParameterList_1")) return false;
    ILParameterList_1_0(b, l + 1);
    return true;
  }

  // ILExpression? (',' ILExpression )*
  private static boolean ILParameterList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParameterList_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = ILParameterList_1_0_0(b, l + 1);
    p = r; // pin = 1
    r = r && ILParameterList_1_0_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ILExpression?
  private static boolean ILParameterList_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParameterList_1_0_0")) return false;
    ILExpression(b, l + 1, -1);
    return true;
  }

  // (',' ILExpression )*
  private static boolean ILParameterList_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParameterList_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ILParameterList_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ILParameterList_1_0_1", c)) break;
    }
    return true;
  }

  // ',' ILExpression
  private static boolean ILParameterList_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParameterList_1_0_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && ILExpression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // OP_MUL | OP_DIV | OP_MOD
  static boolean MulOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MulOp")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = consumeToken(b, OP_MUL);
    if (!r) r = consumeToken(b, OP_DIV);
    if (!r) r = consumeToken(b, OP_MOD);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // AddOp | OP_NOT
  static boolean UnaryOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryOp")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<operator>");
    r = AddOp(b, l + 1);
    if (!r) r = consumeToken(b, OP_NOT);
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
  // ID
  static boolean identifier(PsiBuilder b, int l) {
    return consumeToken(b, ID);
  }

  /* ********************************************************** */
  // DOUBLE_QUOTED_STRING
  static boolean literal(PsiBuilder b, int l) {
    return consumeToken(b, DOUBLE_QUOTED_STRING);
  }

  /* ********************************************************** */
  // NUMBER
  static boolean number(PsiBuilder b, int l) {
    return consumeToken(b, NUMBER);
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
  // ILExpressionHolder
  static boolean root(PsiBuilder b, int l) {
    return ILExpressionHolder(b, l + 1);
  }

  /* ********************************************************** */
  // Expression root: ILExpression
  // Operator priority table:
  // 0: PREFIX(ILParenthesizedExpression)
  // 1: PREFIX(ILExpressionHolder)
  // 2: POSTFIX(ILConditionalExpression)
  // 3: BINARY(ILBinaryOrExpression)
  // 4: BINARY(ILBinaryAndExpression)
  // 5: BINARY(ILBinaryEqualityExpression)
  // 6: BINARY(ILBinaryRelationalExpression)
  // 7: BINARY(ILBinaryAdditionExpression)
  // 8: BINARY(ILBinaryMultiplyExpression)
  // 9: POSTFIX(ILMethodCallExpression)
  // 10: PREFIX(ILUnaryExpression)
  // 11: BINARY(ILSelectExpression)
  // 12: POSTFIX(ILIndexSelectExpression)
  // 13: ATOM(ILVariable)
  // 14: ATOM(ILLiteralExpression)
  public static boolean ILExpression(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "ILExpression")) return false;
    addVariant(b, "<expression>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expression>");
    r = ILParenthesizedExpression(b, l + 1);
    if (!r) r = ILExpressionHolder(b, l + 1);
    if (!r) r = ILUnaryExpression(b, l + 1);
    if (!r) r = ILVariable(b, l + 1);
    if (!r) r = ILLiteralExpression(b, l + 1);
    p = r;
    r = r && ILExpression_0(b, l + 1, g);
    exit_section_(b, l, m, null, r, p, null);
    return r || p;
  }

  public static boolean ILExpression_0(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "ILExpression_0")) return false;
    boolean r = true;
    while (true) {
      Marker m = enter_section_(b, l, _LEFT_, null);
      if (g < 2 && ILConditionalExpression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, IL_CONDITIONAL_EXPRESSION, r, true, null);
      }
      else if (g < 3 && orOp(b, l + 1)) {
        r = ILExpression(b, l, 3);
        exit_section_(b, l, m, IL_BINARY_OR_EXPRESSION, r, true, null);
      }
      else if (g < 4 && andOp(b, l + 1)) {
        r = ILExpression(b, l, 4);
        exit_section_(b, l, m, IL_BINARY_AND_EXPRESSION, r, true, null);
      }
      else if (g < 5 && equalityOp(b, l + 1)) {
        r = ILExpression(b, l, 5);
        exit_section_(b, l, m, IL_BINARY_EQUALITY_EXPRESSION, r, true, null);
      }
      else if (g < 6 && relationalOp(b, l + 1)) {
        r = ILExpression(b, l, 6);
        exit_section_(b, l, m, IL_BINARY_RELATIONAL_EXPRESSION, r, true, null);
      }
      else if (g < 7 && AddOp(b, l + 1)) {
        r = ILExpression(b, l, 7);
        exit_section_(b, l, m, IL_BINARY_ADDITION_EXPRESSION, r, true, null);
      }
      else if (g < 8 && MulOp(b, l + 1)) {
        r = ILExpression(b, l, 8);
        exit_section_(b, l, m, IL_BINARY_MULTIPLY_EXPRESSION, r, true, null);
      }
      else if (g < 9 && ILParameterList(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, IL_METHOD_CALL_EXPRESSION, r, true, null);
      }
      else if (g < 11 && consumeTokenSmart(b, OP_DOT)) {
        r = ILExpression(b, l, 12);
        exit_section_(b, l, m, IL_SELECT_EXPRESSION, r, true, null);
      }
      else if (g < 12 && ILIndexSelectExpression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, IL_INDEX_SELECT_EXPRESSION, r, true, null);
      }
      else {
        exit_section_(b, l, m, null, false, false, null);
        break;
      }
    }
    return r;
  }

  public static boolean ILParenthesizedExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParenthesizedExpression")) return false;
    if (!nextTokenIsSmart(b, L_PAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = ILParenthesizedExpression_0(b, l + 1);
    p = r;
    r = p && ILExpression(b, l, 0);
    r = p && report_error_(b, ILParenthesizedExpression_1(b, l + 1)) && r;
    exit_section_(b, l, m, IL_PARENTHESIZED_EXPRESSION, r, p, null);
    return r || p;
  }

  // '(' <<push 0>>
  private static boolean ILParenthesizedExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParenthesizedExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, L_PAREN);
    r = r && push(b, l + 1, 0);
    exit_section_(b, m, null, r);
    return r;
  }

  // ')' <<pop>>
  private static boolean ILParenthesizedExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParenthesizedExpression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, R_PAREN);
    r = r && pop(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  public static boolean ILExpressionHolder(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILExpressionHolder")) return false;
    if (!nextTokenIsSmart(b, INTERPOLATION_START)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeTokenSmart(b, INTERPOLATION_START);
    p = r;
    r = p && ILExpression(b, l, 1);
    r = p && report_error_(b, consumeToken(b, INTERPOLATION_END)) && r;
    exit_section_(b, l, m, IL_EXPRESSION_HOLDER, r, p, null);
    return r || p;
  }

  // '?' <<push 1>> ILExpression (':' ILExpression) <<pop>>
  private static boolean ILConditionalExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILConditionalExpression_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeTokenSmart(b, OP_QUEST);
    p = r; // pin = '\?'|'\:'
    r = r && report_error_(b, push(b, l + 1, 1));
    r = p && report_error_(b, ILExpression(b, l + 1, -1)) && r;
    r = p && report_error_(b, ILConditionalExpression_0_3(b, l + 1)) && r;
    r = p && pop(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ':' ILExpression
  private static boolean ILConditionalExpression_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILConditionalExpression_0_3")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeTokenSmart(b, OP_COLON);
    p = r; // pin = '\?'|'\:'
    r = r && ILExpression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  public static boolean ILUnaryExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILUnaryExpression")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = UnaryOp(b, l + 1);
    p = r;
    r = p && ILExpression(b, l, 10);
    exit_section_(b, l, m, IL_UNARY_EXPRESSION, r, p, null);
    return r || p;
  }

  // '[' ILExpression ']'
  private static boolean ILIndexSelectExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILIndexSelectExpression_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeTokenSmart(b, L_BRACKET);
    p = r; // pin = '\['
    r = r && report_error_(b, ILExpression(b, l + 1, -1));
    r = p && consumeToken(b, R_BRACKET) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // identifier | '*'
  public static boolean ILVariable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILVariable")) return false;
    if (!nextTokenIsSmart(b, ID, OP_MUL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IL_VARIABLE, "<Identifier>");
    r = identifier(b, l + 1);
    if (!r) r = consumeTokenSmart(b, OP_MUL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // literal /*| identifier*/ | number | 'true' | 'false' | 'null'
  public static boolean ILLiteralExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILLiteralExpression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IL_LITERAL_EXPRESSION, "<Literal>");
    r = literal(b, l + 1);
    if (!r) r = number(b, l + 1);
    if (!r) r = consumeTokenSmart(b, TRUE);
    if (!r) r = consumeTokenSmart(b, FALSE);
    if (!r) r = consumeTokenSmart(b, NULL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
