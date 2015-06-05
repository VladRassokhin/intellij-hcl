package org.intellij.plugins.hcl.psi;
import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static org.intellij.plugins.hcl.HCLElementTypes.*;

%%

%{
  public _HCLLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _HCLLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL="\r"|"\n"|"\r\n"
LINE_WS=[\ \t\f]
WHITE_SPACE=({LINE_WS}|{EOL})+

LINE_COMMENT=("//".*)|(#.*)
BLOCK_COMMENT="/"\*([^*]|\*[^/])*\*?(\*"/")?
DOUBLE_QUOTED_STRING=\"([^\\\"\r\n]|\\[^\r\n])*\"?
SINGLE_QUOTED_STRING='([^\\'\r\n]|\\[^\r\n])*'?
NUMBER=-?(0x)?(0|[1-9])[0-9]*(\.[0-9]+)?([eE][-+]?[0-9]+)?
ID=[a-zA-Z\.\-_][0-9a-zA-Z\.\-_]*

%%
<YYINITIAL> {
  {WHITE_SPACE}               { return com.intellij.psi.TokenType.WHITE_SPACE; }

  "["                         { return L_BRACKET; }
  "]"                         { return R_BRACKET; }
  "{"                         { return L_CURLY; }
  "}"                         { return R_CURLY; }
  ","                         { return COMMA; }
  "="                         { return EQUALS; }
  "true"                      { return TRUE; }
  "false"                     { return FALSE; }
  "null"                      { return NULL; }

  {LINE_COMMENT}              { return LINE_COMMENT; }
  {BLOCK_COMMENT}             { return BLOCK_COMMENT; }
  {DOUBLE_QUOTED_STRING}      { return DOUBLE_QUOTED_STRING; }
  {SINGLE_QUOTED_STRING}      { return SINGLE_QUOTED_STRING; }
  {NUMBER}                    { return NUMBER; }
  {ID}                        { return ID; }

  [^] { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}
