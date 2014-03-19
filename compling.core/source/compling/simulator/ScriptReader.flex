package compling.simulator;

import java_cup.runtime.Symbol;

%%
%{
public int getLineNumber(){return yyline;}

public int getColumn(){return yycolumn;}
%}
%class ScriptReaderLexer
%cupsym ScriptReaderSym
%cup
%line
%state commentstyle1, commentstyle2
%public
%unicode
%column

WHITE_SPACE_CHAR=[\ \t\b\012]
STRING_TEXT=(\\\"|[^\n\"]|\\{WHITE_SPACE_CHAR}+\\)*
COMMENT_TEXT=([^/*\n]|[^*\n]"/"[^*\n]|[^/\n]"*"[^/\n]|"*"[^/\n]|"/"[^*\n])*
%%
<YYINITIAL> \{ {return new Symbol(ScriptReaderSym.OPENBRACKET); }
<YYINITIAL> \} {return new Symbol(ScriptReaderSym.CLOSEBRACKET); }
<YYINITIAL> [E|e][X|x][E|e][C|c] {return new Symbol(ScriptReaderSym.EXEC); }
<YYINITIAL> [Q|q][U|u][E|e][R|r][Y|y] {return new Symbol(ScriptReaderSym.QUERY); }
<YYINITIAL> [T|t][E|e][S|s][T|t][E|e][X|x][I|i][S|s][T|t] {return new Symbol(ScriptReaderSym.TESTEXIST); }
<YYINITIAL> [T|t][E|e][S|s][T|t] {return new Symbol(ScriptReaderSym.TEST); }
<YYINITIAL> [R|r][U|u][N|n][S|s][C|c][R|r][I|i][P|p][T|t] {return new Symbol(ScriptReaderSym.RUN); }
<YYINITIAL> "!" {return new Symbol(ScriptReaderSym.NEGATION); }
<YYINITIAL> [D|d][E|e][F|f][A|a][U|u][L|l][T|t] {return new Symbol(ScriptReaderSym.DEFAULT); }
<YYINITIAL> ";" {return new Symbol(ScriptReaderSym.SEMICOLON); }
<YYINITIAL> \( {return new Symbol(ScriptReaderSym.OPENPAREN); }
<YYINITIAL> \) {return new Symbol(ScriptReaderSym.CLOSEPAREN); }
<YYINITIAL> [I|i][N|n][D|d] {return new Symbol(ScriptReaderSym.IND); }
<YYINITIAL> [F|f][I|i][L|l] {return new Symbol(ScriptReaderSym.FIL); }
<YYINITIAL> [S|s][E|e][T|t][C|c][U|u][R|r][R|r][E|e][N|n][T|t][I|i][N|n][T|t][E|e][R|r][V|v][A|a][L|l] {return new Symbol(ScriptReaderSym.CURRENTINTERVAL); }
<YYINITIAL> [I|i][N|n][S|s][T|t] {return new Symbol(ScriptReaderSym.INST); }
<YYINITIAL> [R|r][E|e][M|m] {return new Symbol(ScriptReaderSym.REM); }
<YYINITIAL> \"{STRING_TEXT}\" { return new Symbol(ScriptReaderSym.STR, new String(yytext())); }
<YYINITIAL> \_[0-9A-Za-z][0-9a-zA-Z\_\-]* {return new Symbol(ScriptReaderSym.VARIABLE, new String(yytext().substring(1))); }
<YYINITIAL> \?[0-9A-Za-z][0-9a-zA-Z\_\-]* {return new Symbol(ScriptReaderSym.QUERYVAR, new String(yytext())); }
<YYINITIAL> [0-9A-Za-z][0-9a-zA-Z\_\-]* {return new Symbol(ScriptReaderSym.IDENTIFIER, new String(yytext())); }
<YYINITIAL> "//" { yybegin(commentstyle1); }
<commentstyle1> [^\n] { }
<commentstyle1> \n { yybegin(YYINITIAL); }

<YYINITIAL> "/*" { yybegin(commentstyle2);}
<commentstyle2> "*/" { yybegin(YYINITIAL); }
<commentstyle2> {COMMENT_TEXT} {  }
<commentstyle2> \n {  }
<commentstyle2> . {System.out.println("ERROR: Unclosed comment at line " + (yyline+1));}

<YYINITIAL> [ \t\f\r\n] { }
<YYINITIAL> . {System.out.println("Unknown character " + yytext() + " at line " + (yyline+1));}
