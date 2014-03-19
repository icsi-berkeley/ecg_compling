package compling.simulator;

import java_cup.runtime.Symbol;

%%
%{
public int getLineNumber(){return yyline;}
%}
%class ScriptSplitterLexer
%cupsym ScriptSplitterSym
%cup
%line
%state commentstyle1, commentstyle2
%public
%unicode


WHITE_SPACE_CHAR=[\ \t\b\012]
STRING_TEXT=(\\\"|[^\n\"]|\\{WHITE_SPACE_CHAR}+\\)*
COMMENT_TEXT=([^/*\n]|[^*\n]"/"[^*\n]|[^/\n]"*"[^/\n]|"*"[^/\n]|"/"[^*\n])*
BRACKETED_TEXT=([^\\\{\\\}])*

%%
<YYINITIAL> [S|s][C|c][R|r][I|i][P|p][T|t] {return new Symbol(ScriptSplitterSym.SCRIPT); }
<YYINITIAL> [0-9A-Za-z][0-9a-zA-Z\_\-]* {return new Symbol(ScriptSplitterSym.IDENTIFIER, new String(yytext())); }
<YYINITIAL> "{"{BRACKETED_TEXT}"}" {return new Symbol(ScriptSplitterSym.CONTENT, new String(yytext())); }

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

