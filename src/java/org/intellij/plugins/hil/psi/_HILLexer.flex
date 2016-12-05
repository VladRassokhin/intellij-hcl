package org.intellij.plugins.hil.psi;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static org.intellij.plugins.hil.HILElementTypes.*;

%%

%{
  public _HILLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _HILLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s

DOUBLE_QUOTED_STRING=\"([^\\\"\r\n]|\\[^\r\n])*\"?
NUMBER=(0x)?(0|[1-9])[0-9]*(\.[0-9]+)?([eE][-+]?[0-9]+)?
ID=[a-zA-Z_][0-9a-zA-Z\-_*]*

%%
<YYINITIAL> {
  {WHITE_SPACE}               { return com.intellij.psi.TokenType.WHITE_SPACE; }

  "${"                        { return INTERPOLATION_START; }
  "}"                         { return INTERPOLATION_END; }
  "("                         { return L_PAREN; }
  ")"                         { return R_PAREN; }
  "["                         { return L_BRACKET; }
  "]"                         { return R_BRACKET; }
  ","                         { return COMMA; }
  "."                         { return OP_DOT; }
  "+"                         { return OP_PLUS; }
  "-"                         { return OP_MINUS; }
  "*"                         { return OP_MUL; }
  "/"                         { return OP_DIV; }
  "%"                         { return OP_MOD; }
  "!"                         { return OP_NOT; }
  "=="                        { return OP_EQUAL; }
  "!="                        { return OP_NOT_EQUAL; }
  "<"                         { return OP_LESS; }
  ">"                         { return OP_GREATER; }
  "<="                        { return OP_LESS_OR_EQUAL; }
  ">="                        { return OP_GREATER_OR_EQUAL; }
  "&&"                        { return OP_AND_AND; }
  "||"                        { return OP_OR_OR; }
  ":"                         { return OP_COLON; }
  "?"                         { return OP_QUEST; }
  "true"                      { return TRUE; }
  "false"                     { return FALSE; }
  "null"                      { return NULL; }

  {DOUBLE_QUOTED_STRING}      { return DOUBLE_QUOTED_STRING; }
  {NUMBER}                    { return NUMBER; }
  {ID}                        { return ID; }

}

[^] { return com.intellij.psi.TokenType.BAD_CHARACTER; }
