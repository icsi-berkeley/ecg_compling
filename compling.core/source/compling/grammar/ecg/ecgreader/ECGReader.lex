
package compling.grammar.ecg.ecgreader;

import java_cup.runtime.Symbol;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.ecgreader.Location;

%%

%{
	private int lineNum;
	public String file = "unknown file";
	public StringBuffer scannerErrors = new StringBuffer();
	public int lastOpenCommentLine = -1;
	
	public int getLineNumber() { return yyline; }
	public Location getLocation() { 
		return new Location(yytext(), file, getLineNumber(), yychar);
	}
	public String getScannerErrors() { return scannerErrors.toString(); }
%}

%class Yylex

%cup
%state commentstyle1, commentstyle2
%public
%unicode
%caseless
%char
%line

WHITE_SPACE_CHAR = [\ \t\b\012]
STRING_TEXT = ( \\\" | [^\n\r\"] | \\ {WHITE_SPACE_CHAR}+ \\ )*
COMMENT_TEXT = ( [^*\n\r] | \*+ [^/*\n\r] )*

IDENT = ([:letter:] | _) ([:letter:] | [:digit:] | _ | -)*
EXTIDENT = [\@] {IDENT}+
LINE_TERMINATOR = \r | \n | \r\n 
PROBABILITY = 0[\.][0-9]+ | [\.][0-9]+ | 1 | 1[\.]0 

%%

<YYINITIAL> "["     				  { return new Symbol(sym.OPENBRACKET); }
<YYINITIAL> "]"                 { return new Symbol(sym.CLOSEBRACKET); }
<YYINITIAL> "<--"               { return new Symbol(sym.ASSIGN, ECGConstants.ASSIGN); }
<YYINITIAL> "<->"               { return new Symbol(sym.IDENTIFY, ECGConstants.IDENTIFY); }
<YYINITIAL> "<-->"              { return new Symbol(sym.IDENTIFY, ECGConstants.IDENTIFY); }
<YYINITIAL> "constructional"    { return new Symbol(sym.CONSTRUCTIONAL); }
<YYINITIAL> "construction"      { return new Symbol(sym.CONSTRUCTION); }
<YYINITIAL> "optional"          { return new Symbol(sym.OPTIONAL); }
<YYINITIAL> "extraposed"        { return new Symbol(sym.EXTRAPOSED); }
<YYINITIAL> "constraints"       { return new Symbol(sym.CONSTRAINTS); }
<YYINITIAL> "subcase"           { return new Symbol(sym.SUBCASE); }
<YYINITIAL> "schema"            { return new Symbol(sym.SCHEMA); }
<YYINITIAL> "form"              { return new Symbol(sym.FORM); }
<YYINITIAL> "meaning"           { return new Symbol(sym.MEANING); }
<YYINITIAL> "feature"           { return new Symbol(sym.FEATURE); }
<YYINITIAL> "semantic"          { return new Symbol(sym.SEMANTIC); }
<YYINITIAL> "constituents"      { return new Symbol(sym.CONSTITUENTS);}
<YYINITIAL> "before"            { return new Symbol(sym.BEFORE, ECGConstants.BEFORE); }
<YYINITIAL> "ignore"            { return new Symbol(sym.IGNORE); }
<YYINITIAL> "meets"             { return new Symbol(sym.MEETS, ECGConstants.MEETS); }
<YYINITIAL> ":"                 { return new Symbol(sym.COLON); }
<YYINITIAL> ","                 { return new Symbol(sym.COMMA); }
<YYINITIAL> "evokes"            { return new Symbol(sym.EVOKES); }
<YYINITIAL> "as"                { return new Symbol(sym.AS); }
<YYINITIAL> "of"                { return new Symbol(sym.OF); }
<YYINITIAL> "abstract"          { return new Symbol(sym.ABSTRACT); }
<YYINITIAL> "general"           { return new Symbol(sym.ABSTRACT); }
<YYINITIAL> "roles"             { return new Symbol(sym.ROLES); }
<YYINITIAL> "map"               { return new Symbol(sym.MAP); }
<YYINITIAL> "situation"         { return new Symbol(sym.SITUATION); }
<YYINITIAL> \"{STRING_TEXT}\"   { return new Symbol(sym.STR, new String(yytext())); }
<YYINITIAL> ({IDENT}\.)+{IDENT} { return new Symbol(sym.SLOTCHAIN, new String(yytext())); }
<YYINITIAL> {IDENT}             { return new Symbol(sym.IDENTIFIER, 
                                                    1 + lineNum, // left
                                                    yychar, // right
                                                    yytext()); }

<YYINITIAL> {EXTIDENT}          { return new Symbol(sym.EXTERNALTYPE, new String(yytext())); }
<YYINITIAL> {PROBABILITY}       { return new Symbol(sym.PROB, new String(yytext())); }

<YYINITIAL> "//"                { yybegin(commentstyle1); }
<YYINITIAL> "/*"                { yybegin(commentstyle2); }

<commentstyle1> [^\r\n]         { /* nothing */ }
<commentstyle1> {LINE_TERMINATOR} { ++lineNum; yybegin(YYINITIAL); }

<commentstyle2> ~"*/"           { yybegin(YYINITIAL);  lastOpenCommentLine = lineNum; }
<commentstyle2> {COMMENT_TEXT}  { /* nothing */ }
<commentstyle2> {LINE_TERMINATOR} { ++lineNum; }
<commentstyle2> . | \n          { scannerErrors.append("ERROR In file "+file+": Unclosed comment starting on line "+lastOpenCommentLine+"\n" ); }

<YYINITIAL> [ \t]               { /* nothing */ }
<YYINITIAL> {LINE_TERMINATOR}   { ++lineNum; }
<YYINITIAL> . | \n              { scannerErrors.append("ERROR: In file "+file+": Unknown character " + yytext() + " at line " + yyline +"\n"); }
