package org.intellij.plugins.hcl;
import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import java.util.EnumSet;
import static org.intellij.plugins.hcl.HCLElementTypes.*;
import static com.intellij.psi.TokenType.BAD_CHARACTER;

@SuppressWarnings({"ALL"})
%%

%public
%class _HCLLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL="\r\n"|"\r"|"\n"
LINE_WS=[\ \t\f]
WHITE_SPACE=({LINE_WS}|{EOL})+

LINE_COMMENT=("/""/"|"#")[^\r\n]*
BLOCK_COMMENT="/*"([^"*"]|"*"[^/])*("*/")?

NUMBER=-?(0[xX])?[0-9]+(\.[0-9]+)?([eE][-+]?[0-9]+)?
ID=[a-zA-Z\.\-_][0-9a-zA-Z\.\-_]*

HIL_START=(\$\{)
HIL_STOP=(\})

HEREDOC_START="<<"

STRING_ELEMENT=([^\"\'\r\n\$\{\}]|\\[^\r\n])+

%state D_STRING, S_STRING, HIL_EXPRESSION, IN_NUMBER
%state S_HEREDOC_MARKER, S_HEREDOC_LINE
%{
  // This parameters can be getted from capabilities
    private boolean withNumbersWithBytesPostfix;
    private boolean withInterpolationLanguage;

    public _HCLLexer(EnumSet<HCLCapability> capabilities) {
      this((java.io.Reader)null);
      this.withNumbersWithBytesPostfix = capabilities.contains(HCLCapability.NUMBERS_WITH_BYTES_POSTFIX);
      this.withInterpolationLanguage = capabilities.contains(HCLCapability.INTERPOLATION_LANGUAGE);
    }
    enum StringType {
      None, SingleQ, DoubleQ
    }
  // State data
    StringType stringType = StringType.None;
    int stringStart = -1;
    int hil = 0;
    int myHereDocMarkerLength = 0;
    int myHereDocMarkerWeakHash = 0;
    boolean myHereDocIndented = false;

    private void hil_inc() {
      hil++;
    }
    private int hil_dec() {
      assert hil > 0;
      hil--;
      return hil;
    }
    private void push_eol() {
      yypushback(getEOLLength());
    }
    private int getEOLLength() {
      if (yylength() == 0) return 0;
      char last = yycharat(yylength() - 1);
      if (last != '\r' && last != '\n') return 0;
      if ((yylength() > 1) && yycharat(yylength() - 2) == '\r') return 2;
      return 1;
    }
    private IElementType eods() {
      yybegin(YYINITIAL); stringType = StringType.None; zzStartRead = stringStart; return DOUBLE_QUOTED_STRING;
    }
    private IElementType eoss() {
      yybegin(YYINITIAL); stringType = StringType.None; zzStartRead = stringStart; return SINGLE_QUOTED_STRING;
    }
    private IElementType eoil() {
      hil=0; return stringType == StringType.SingleQ ? eoss(): eods();
    }
    private void setHereDocMarker(CharSequence marker) {
      myHereDocIndented = true; // Temprorarly set to true see #30
      int length = marker.length();
      String value = marker.toString();
      assert(length > 0);
      if (marker.charAt(0) == '-') {
        assert(length > 1);
        // Indented heredoc
        myHereDocIndented = true;
        length--;
        value = value.substring(1);
      }
      myHereDocMarkerLength = length & 0xFF;
      int hash = value.hashCode();
      myHereDocMarkerWeakHash = hash & 0xFFFF;
    }
    private void resetHereDocMarker() {
      myHereDocMarkerLength = 0;
      myHereDocMarkerWeakHash = 0;
    }
    private boolean isHereDocMarkerDefined() {
      return myHereDocMarkerLength != 0 && myHereDocMarkerWeakHash != 0;
    }
    private boolean isHereDocMarker(CharSequence input) {
      if (myHereDocIndented) input = LexerUtil.trimLeading(input);
      if ((input.length() & 0xFF) != myHereDocMarkerLength) return false;
      int hash = input.toString().hashCode();
      return myHereDocMarkerWeakHash == (hash & 0xFFFF);
    }

%}

%%

<D_STRING> {
  {HIL_START} { if (withInterpolationLanguage) {hil_inc(); yybegin(HIL_EXPRESSION);} }
  \"          { return eods(); }
  {STRING_ELEMENT} {}
  \$ {}
  \{ {}
  \} {}
  \' {}
  {EOL} { push_eol(); return eods(); }
  <<EOF>> { return eods(); }
  [^] { return BAD_CHARACTER; }
}

<S_STRING> {
  {HIL_START} { if (withInterpolationLanguage) {hil_inc(); yybegin(HIL_EXPRESSION);} }
  \'          { return eoss(); }
  {STRING_ELEMENT} {}
  \$ {}
  \{ {}
  \} {}
  \" {}
  {EOL} { push_eol(); return eoss(); }
  <<EOF>> { return eoss(); }
  [^] { return BAD_CHARACTER; }
}


<HIL_EXPRESSION> {
  {HIL_START} {hil_inc();}
  {HIL_STOP} {if (hil_dec() <= 0) yybegin(stringType == StringType.SingleQ ? S_STRING: D_STRING); }
  {STRING_ELEMENT} {}
  \' {}
  \" {}
  \$ {}
  \{ {}
  {EOL} { push_eol(); return eoil(); }
  <<EOF>> { return eoil(); }
  [^] { return BAD_CHARACTER; }
}

<S_HEREDOC_MARKER> {
  ([^\r\n]|\\[^\r\n])+ {EOL}? {
    yypushback(getEOLLength());
    setHereDocMarker(yytext());
    return HD_MARKER;
  }
  {EOL} {
    if (!isHereDocMarkerDefined()) {
      yybegin(YYINITIAL);
      return BAD_CHARACTER;
    }
    yybegin(S_HEREDOC_LINE);
//    zzStartRead+=getEOLLength();
    return com.intellij.psi.TokenType.WHITE_SPACE;
  }
  <<EOF>> { yybegin(YYINITIAL); return BAD_CHARACTER; }
}

<S_HEREDOC_LINE> {
  ([^\r\n]|\\[^\r\n])+ {EOL}? {
    int eol = getEOLLength();
    int len = yylength();
    int len_eff = len - eol;
    assert len_eff >= 0;
    if((len_eff & 0xFF) >= myHereDocMarkerLength
       && isHereDocMarker(yytext().subSequence(0, len_eff))) {
      // End of HereDoc
      yypushback(eol);
      yybegin(YYINITIAL);
      resetHereDocMarker();
      return HD_MARKER;
    } else {
      return HD_LINE;
    }
  }
  {EOL} { return HD_LINE; }
  <<EOF>> { yybegin(YYINITIAL); return BAD_CHARACTER; }
}

<YYINITIAL>   \"  { stringType = StringType.DoubleQ; stringStart = zzStartRead; yybegin(D_STRING); }
<YYINITIAL>   \'  { stringType = StringType.SingleQ; stringStart = zzStartRead; yybegin(S_STRING); }
<YYINITIAL>   {HEREDOC_START}  { yybegin(S_HEREDOC_MARKER); return HD_START; }

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
  {NUMBER}                    { if (!withNumbersWithBytesPostfix) return NUMBER;
                                yybegin(IN_NUMBER); yypushback(yylength());}
  {ID}                        { return ID; }

  [^] { return BAD_CHARACTER; }
}

<IN_NUMBER> {
  {NUMBER} ([kKmMgG][bB]?) { yybegin(YYINITIAL); return NUMBER; }
  {NUMBER} { yybegin(YYINITIAL); return NUMBER; }
}