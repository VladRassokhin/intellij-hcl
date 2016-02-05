// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.intellij.plugins.hil.HILElementTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HILParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == IL_BINARY_ADD_EXPRESSION) {
      r = ILExpression(b, 0, 2);
    }
    else if (t == IL_BINARY_MUL_EXPRESSION) {
      r = ILExpression(b, 0, 3);
    }
    else if (t == IL_EXPRESSION) {
      r = ILExpression(b, 0, -1);
    }
    else if (t == IL_EXPRESSION_HOLDER) {
      r = ILExpressionHolder(b, 0);
    }
    else if (t == IL_LITERAL_EXPRESSION) {
      r = ILLiteralExpression(b, 0);
    }
    else if (t == IL_METHOD_CALL_EXPRESSION) {
      r = ILExpression(b, 0, 4);
    }
    else if (t == IL_PARAMETER_LIST) {
      r = ILParameterList(b, 0);
    }
    else if (t == IL_PARENTHESIZED_EXPRESSION) {
      r = ILParenthesizedExpression(b, 0);
    }
    else if (t == IL_SELECT_EXPRESSION) {
      r = ILExpression(b, 0, 1);
    }
    else if (t == IL_VARIABLE) {
      r = ILVariable(b, 0);
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
    create_token_set_(IL_BINARY_ADD_EXPRESSION, IL_BINARY_MUL_EXPRESSION, IL_EXPRESSION, IL_EXPRESSION_HOLDER,
      IL_LITERAL_EXPRESSION, IL_METHOD_CALL_EXPRESSION, IL_PARENTHESIZED_EXPRESSION, IL_SELECT_EXPRESSION,
      IL_VARIABLE),
  };

  /* ********************************************************** */
  // '+' | '-'
  static boolean AddOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AddOp")) return false;
    if (!nextTokenIs(b, "", OP_PLUS, OP_MINUS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OP_PLUS);
    if (!r) r = consumeToken(b, OP_MINUS);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '.'
  static boolean DotOp(PsiBuilder b, int l) {
    return consumeToken(b, POINT);
  }

  /* ********************************************************** */
  // '(' (ILExpression? (',' ILExpression )* )?')'
  public static boolean ILParameterList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParameterList")) return false;
    if (!nextTokenIs(b, L_PAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, L_PAREN);
    p = r; // pin = 1
    r = r && report_error_(b, ILParameterList_1(b, l + 1));
    r = p && consumeToken(b, R_PAREN) && r;
    exit_section_(b, l, m, IL_PARAMETER_LIST, r, p, null);
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
    Marker m = enter_section_(b, l, _NONE_, null);
    r = ILParameterList_1_0_0(b, l + 1);
    p = r; // pin = 1
    r = r && ILParameterList_1_0_1(b, l + 1);
    exit_section_(b, l, m, null, r, p, null);
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
    int c = current_position_(b);
    while (true) {
      if (!ILParameterList_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ILParameterList_1_0_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' ILExpression
  private static boolean ILParameterList_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILParameterList_1_0_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && ILExpression(b, l + 1, -1);
    exit_section_(b, l, m, null, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '*' | '/' | '%'
  static boolean MulOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MulOp")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OP_MUL);
    if (!r) r = consumeToken(b, OP_DIV);
    if (!r) r = consumeToken(b, OP_MOD);
    exit_section_(b, m, null, r);
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
  // ILExpression
  static boolean root(PsiBuilder b, int l) {
    return ILExpression(b, l + 1, -1);
  }

  /* ********************************************************** */
  // Expression root: ILExpression
  // Operator priority table:
  // 0: PREFIX(ILParenthesizedExpression)
  // 1: PREFIX(ILExpressionHolder)
  // 2: BINARY(ILSelectExpression)
  // 3: BINARY(ILBinaryAddExpression)
  // 4: BINARY(ILBinaryMulExpression)
  // 5: POSTFIX(ILMethodCallExpression)
  // 6: ATOM(ILLiteralExpression)
  // 7: ATOM(ILVariable)
  public static boolean ILExpression(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "ILExpression")) return false;
    addVariant(b, "<expression>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expression>");
    r = ILParenthesizedExpression(b, l + 1);
    if (!r) r = ILExpressionHolder(b, l + 1);
    if (!r) r = ILLiteralExpression(b, l + 1);
    if (!r) r = ILVariable(b, l + 1);
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
      if (g < 2 && DotOp(b, l + 1)) {
        r = ILExpression(b, l, 6);
        exit_section_(b, l, m, IL_SELECT_EXPRESSION, r, true, null);
      }
      else if (g < 3 && AddOp(b, l + 1)) {
        r = ILExpression(b, l, 3);
        exit_section_(b, l, m, IL_BINARY_ADD_EXPRESSION, r, true, null);
      }
      else if (g < 4 && MulOp(b, l + 1)) {
        r = ILExpression(b, l, 4);
        exit_section_(b, l, m, IL_BINARY_MUL_EXPRESSION, r, true, null);
      }
      else if (g < 5 && ILParameterList(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, IL_METHOD_CALL_EXPRESSION, r, true, null);
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
    if (!nextTokenIsFast(b, L_PAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeTokenSmart(b, L_PAREN);
    p = r;
    r = p && ILExpression(b, l, 0);
    r = p && report_error_(b, consumeToken(b, R_PAREN)) && r;
    exit_section_(b, l, m, IL_PARENTHESIZED_EXPRESSION, r, p, null);
    return r || p;
  }

  public static boolean ILExpressionHolder(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILExpressionHolder")) return false;
    if (!nextTokenIsFast(b, INTERPOLATION_START)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeTokenSmart(b, INTERPOLATION_START);
    p = r;
    r = p && ILExpression(b, l, 1);
    r = p && report_error_(b, consumeToken(b, INTERPOLATION_END)) && r;
    exit_section_(b, l, m, IL_EXPRESSION_HOLDER, r, p, null);
    return r || p;
  }

  // literal /*| identifier*/ | number | 'true' | 'false' | 'null'
  public static boolean ILLiteralExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILLiteralExpression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<Literal>");
    r = literal(b, l + 1);
    if (!r) r = number(b, l + 1);
    if (!r) r = consumeTokenSmart(b, TRUE);
    if (!r) r = consumeTokenSmart(b, FALSE);
    if (!r) r = consumeTokenSmart(b, NULL);
    exit_section_(b, l, m, IL_LITERAL_EXPRESSION, r, false, null);
    return r;
  }

  // identifier | '*'
  public static boolean ILVariable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ILVariable")) return false;
    if (!nextTokenIsFast(b, OP_MUL, ID)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<Identifier>");
    r = identifier(b, l + 1);
    if (!r) r = consumeTokenSmart(b, OP_MUL);
    exit_section_(b, l, m, IL_VARIABLE, r, false, null);
    return r;
  }

}
