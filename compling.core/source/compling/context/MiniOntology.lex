package compling.context;

import java_cup.runtime.Symbol;
import compling.grammar.ecg.ecgreader.Location;

%%

%{
  protected int lineNum;
  public int getLineNumber() 
    { return 1 + lineNum; /* zero-based */ }
  public Location getLocation() 
    { return new Location(yytext(), file, getLineNumber(), yychar); }
  public String getMatchedText() { return yytext(); }
  public  String file = "unknown file";
%}

%cup
%line
%state commentstyle1, commentstyle2
%public
%unicode
%caseless // keywords are case-insensitive
%char // keep track of the characters read

WHITE_SPACE_CHAR = [\ \t\b\012]
STRING_TEXT = (\\\"|[^\n\r\"]|\\{WHITE_SPACE_CHAR}+\\)*
COMMENT_TEXT = ([^/*\n\r]|[^*\n\r]"/"[^*\n\r]|[^/\n\r]"*"[^/\n\r]|"*"[^/\n\r]|"/"[^*\n\r])*
LINE_TERMINATOR = \r | \n | \r\n 
IDENT = ([:letter:] | _) ([:letter:] | [:digit:] | _ | -)*

%%

<YYINITIAL> \( { return new Symbol(sym.OPENPAREN); }
<YYINITIAL> \) { return new Symbol(sym.CLOSEPAREN); }
<YYINITIAL> "sub" { return new Symbol(sym.SUB); }
<YYINITIAL> "setcurrentinterval" { return new Symbol(sym.CURRENTINTERVAL); }
<YYINITIAL> "defs:" { return new Symbol(sym.DEFS); }
<YYINITIAL> "type" { return new Symbol(sym.TYPE); }
<YYINITIAL> "inst" { return new Symbol(sym.INST); }
<YYINITIAL> "insts:" { return new Symbol(sym.INSTS); }
<YYINITIAL> "rel" { return new Symbol(sym.REL); }
<YYINITIAL> "rem" { return new Symbol(sym.REM); }
<YYINITIAL> "fun" { return new Symbol(sym.FUN); }
<YYINITIAL> "ind" { return new Symbol(sym.IND); }
<YYINITIAL> "fil" { return new Symbol(sym.FIL); }
<YYINITIAL> "eq" { return new Symbol(sym.EQ); }
//<YYINITIAL> "transient" { return new Symbol(sym.TRANSIENT); }
<YYINITIAL> "persistent"  { return new Symbol(sym.PERSISTENT); }
<YYINITIAL> "nonblocking" { return new Symbol(sym.NONBLOCKING); }
<YYINITIAL> \"{STRING_TEXT}\" { return new Symbol(sym.STR, new String(yytext())); }
<YYINITIAL> "//" { yybegin(commentstyle1); }
<YYINITIAL> { IDENT } { return new Symbol(sym.IDENTIFIER, 1 + lineNum, yychar, yytext()); }

<commentstyle1> [^\n\r] { /* nothing */ }

<commentstyle1> { LINE_TERMINATOR } { ++lineNum; yybegin(YYINITIAL); }

<YYINITIAL> "/*" { yybegin(commentstyle2);}

<commentstyle2> "*/" { yybegin(YYINITIAL); }

<commentstyle2> { COMMENT_TEXT } {  }
<commentstyle2> { LINE_TERMINATOR } { ++lineNum; }
<commentstyle2> . { System.out.println("ERROR In file " + file + ": Unclosed comment");}

<YYINITIAL> [ \t\f] { }
<YYINITIAL> {LINE_TERMINATOR} { ++lineNum; }

<YYINITIAL> . | \n { 
  System.err.printf("ERROR in file %s: unknown character '%s' at line %d\n",
      file, yytext(), lineNum + 1); 
  Thread.dumpStack();                                        
}

