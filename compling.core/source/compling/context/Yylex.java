/* The following code was generated by JFlex 1.4.3 on 3/24/15 2:32 PM */

package compling.context;

import java_cup.runtime.Symbol;
import compling.grammar.ecg.ecgreader.Location;


/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.4.3
 * on 3/24/15 2:32 PM from the specification file
 * <tt>MiniOntology.lex</tt>
 */
public class Yylex implements java_cup.runtime.Scanner {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int commentstyle2 = 4;
  public static final int YYINITIAL = 0;
  public static final int commentstyle1 = 2;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0,  0,  1,  1,  2, 2
  };

  /** 
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED = 
    "\10\0\1\1\1\43\1\5\1\0\1\42\1\4\22\0\1\43\1\0"+
    "\1\3\5\0\1\12\1\13\1\7\2\0\1\11\1\0\1\6\12\11"+
    "\1\32\6\0\1\26\1\16\1\21\1\30\1\17\1\31\1\37\1\10"+
    "\1\24\1\10\1\36\1\27\1\35\1\23\1\40\1\34\1\41\1\22"+
    "\1\14\1\20\1\15\1\25\2\10\1\33\1\10\1\0\1\2\2\0"+
    "\1\10\1\0\1\26\1\16\1\21\1\30\1\17\1\31\1\37\1\10"+
    "\1\24\1\10\1\36\1\27\1\35\1\23\1\40\1\34\1\41\1\22"+
    "\1\14\1\20\1\15\1\25\2\10\1\33\1\10\57\0\1\10\12\0"+
    "\1\10\4\0\1\10\5\0\27\10\1\0\37\10\1\0\u01ca\10\4\0"+
    "\14\10\16\0\5\10\7\0\1\10\1\0\1\10\201\0\5\10\1\0"+
    "\2\10\2\0\4\10\10\0\1\10\1\0\3\10\1\0\1\10\1\0"+
    "\24\10\1\0\123\10\1\0\213\10\10\0\236\10\11\0\46\10\2\0"+
    "\1\10\7\0\47\10\110\0\33\10\5\0\3\10\55\0\53\10\25\0"+
    "\12\11\4\0\2\10\1\0\143\10\1\0\1\10\17\0\2\10\7\0"+
    "\2\10\12\11\3\10\2\0\1\10\20\0\1\10\1\0\36\10\35\0"+
    "\131\10\13\0\1\10\16\0\12\11\41\10\11\0\2\10\4\0\1\10"+
    "\5\0\26\10\4\0\1\10\11\0\1\10\3\0\1\10\27\0\31\10"+
    "\253\0\66\10\3\0\1\10\22\0\1\10\7\0\12\10\4\0\12\11"+
    "\1\0\7\10\1\0\7\10\5\0\10\10\2\0\2\10\2\0\26\10"+
    "\1\0\7\10\1\0\1\10\3\0\4\10\3\0\1\10\20\0\1\10"+
    "\15\0\2\10\1\0\3\10\4\0\12\11\2\10\23\0\6\10\4\0"+
    "\2\10\2\0\26\10\1\0\7\10\1\0\2\10\1\0\2\10\1\0"+
    "\2\10\37\0\4\10\1\0\1\10\7\0\12\11\2\0\3\10\20\0"+
    "\11\10\1\0\3\10\1\0\26\10\1\0\7\10\1\0\2\10\1\0"+
    "\5\10\3\0\1\10\22\0\1\10\17\0\2\10\4\0\12\11\25\0"+
    "\10\10\2\0\2\10\2\0\26\10\1\0\7\10\1\0\2\10\1\0"+
    "\5\10\3\0\1\10\36\0\2\10\1\0\3\10\4\0\12\11\1\0"+
    "\1\10\21\0\1\10\1\0\6\10\3\0\3\10\1\0\4\10\3\0"+
    "\2\10\1\0\1\10\1\0\2\10\3\0\2\10\3\0\3\10\3\0"+
    "\14\10\26\0\1\10\25\0\12\11\25\0\10\10\1\0\3\10\1\0"+
    "\27\10\1\0\12\10\1\0\5\10\3\0\1\10\32\0\2\10\6\0"+
    "\2\10\4\0\12\11\25\0\10\10\1\0\3\10\1\0\27\10\1\0"+
    "\12\10\1\0\5\10\3\0\1\10\40\0\1\10\1\0\2\10\4\0"+
    "\12\11\1\0\2\10\22\0\10\10\1\0\3\10\1\0\51\10\2\0"+
    "\1\10\20\0\1\10\21\0\2\10\4\0\12\11\12\0\6\10\5\0"+
    "\22\10\3\0\30\10\1\0\11\10\1\0\1\10\2\0\7\10\72\0"+
    "\60\10\1\0\2\10\14\0\7\10\11\0\12\11\47\0\2\10\1\0"+
    "\1\10\2\0\2\10\1\0\1\10\2\0\1\10\6\0\4\10\1\0"+
    "\7\10\1\0\3\10\1\0\1\10\1\0\1\10\2\0\2\10\1\0"+
    "\4\10\1\0\2\10\11\0\1\10\2\0\5\10\1\0\1\10\11\0"+
    "\12\11\2\0\2\10\42\0\1\10\37\0\12\11\26\0\10\10\1\0"+
    "\44\10\33\0\5\10\163\0\53\10\24\0\1\10\12\11\6\0\6\10"+
    "\4\0\4\10\3\0\1\10\3\0\2\10\7\0\3\10\4\0\15\10"+
    "\14\0\1\10\1\0\12\11\6\0\46\10\12\0\53\10\1\0\1\10"+
    "\3\0\u0149\10\1\0\4\10\2\0\7\10\1\0\1\10\1\0\4\10"+
    "\2\0\51\10\1\0\4\10\2\0\41\10\1\0\4\10\2\0\7\10"+
    "\1\0\1\10\1\0\4\10\2\0\17\10\1\0\71\10\1\0\4\10"+
    "\2\0\103\10\45\0\20\10\20\0\125\10\14\0\u026c\10\2\0\21\10"+
    "\1\0\32\10\5\0\113\10\25\0\15\10\1\0\4\10\16\0\22\10"+
    "\16\0\22\10\16\0\15\10\1\0\3\10\17\0\64\10\43\0\1\10"+
    "\4\0\1\10\3\0\12\11\46\0\12\11\6\0\130\10\10\0\51\10"+
    "\1\0\1\10\5\0\106\10\12\0\35\10\51\0\12\11\36\10\2\0"+
    "\5\10\13\0\54\10\25\0\7\10\10\0\12\11\46\0\27\10\11\0"+
    "\65\10\53\0\12\11\6\0\12\11\15\0\1\10\135\0\57\10\21\0"+
    "\7\10\4\0\12\11\51\0\36\10\15\0\2\10\12\11\6\0\46\10"+
    "\32\0\44\10\34\0\12\11\3\0\3\10\12\11\44\10\153\0\4\10"+
    "\1\0\4\10\16\0\300\10\100\0\u0116\10\2\0\6\10\2\0\46\10"+
    "\2\0\6\10\2\0\10\10\1\0\1\10\1\0\1\10\1\0\1\10"+
    "\1\0\37\10\2\0\65\10\1\0\7\10\1\0\1\10\3\0\3\10"+
    "\1\0\7\10\3\0\4\10\2\0\6\10\4\0\15\10\5\0\3\10"+
    "\1\0\7\10\164\0\1\10\15\0\1\10\20\0\15\10\145\0\1\10"+
    "\4\0\1\10\2\0\12\10\1\0\1\10\3\0\5\10\6\0\1\10"+
    "\1\0\1\10\1\0\1\10\1\0\4\10\1\0\13\10\2\0\4\10"+
    "\5\0\5\10\4\0\1\10\64\0\2\10\u0a7b\0\57\10\1\0\57\10"+
    "\1\0\205\10\6\0\4\10\21\0\46\10\12\0\66\10\11\0\1\10"+
    "\20\0\27\10\11\0\7\10\1\0\7\10\1\0\7\10\1\0\7\10"+
    "\1\0\7\10\1\0\7\10\1\0\7\10\1\0\7\10\120\0\1\10"+
    "\u01d5\0\2\10\52\0\5\10\5\0\2\10\4\0\126\10\6\0\3\10"+
    "\1\0\132\10\1\0\4\10\5\0\51\10\3\0\136\10\21\0\33\10"+
    "\65\0\20\10\u0200\0\u19b6\10\112\0\u51cc\10\64\0\u048d\10\103\0\56\10"+
    "\2\0\u010d\10\3\0\20\10\12\11\2\10\24\0\57\10\20\0\31\10"+
    "\10\0\106\10\61\0\11\10\2\0\147\10\2\0\4\10\1\0\2\10"+
    "\16\0\12\10\120\0\10\10\1\0\3\10\1\0\4\10\1\0\27\10"+
    "\35\0\64\10\16\0\62\10\34\0\12\11\30\0\6\10\3\0\1\10"+
    "\4\0\12\11\34\10\12\0\27\10\31\0\35\10\7\0\57\10\34\0"+
    "\1\10\12\11\46\0\51\10\27\0\3\10\1\0\10\10\4\0\12\11"+
    "\6\0\27\10\3\0\1\10\5\0\60\10\1\0\1\10\3\0\2\10"+
    "\2\0\5\10\2\0\1\10\1\0\1\10\30\0\3\10\43\0\6\10"+
    "\2\0\6\10\2\0\6\10\11\0\7\10\1\0\7\10\221\0\43\10"+
    "\15\0\12\11\6\0\u2ba4\10\14\0\27\10\4\0\61\10\u2104\0\u012e\10"+
    "\2\0\76\10\2\0\152\10\46\0\7\10\14\0\5\10\5\0\1\10"+
    "\1\0\12\10\1\0\15\10\1\0\5\10\1\0\1\10\1\0\2\10"+
    "\1\0\2\10\1\0\154\10\41\0\u016b\10\22\0\100\10\2\0\66\10"+
    "\50\0\14\10\164\0\5\10\1\0\207\10\23\0\12\11\7\0\32\10"+
    "\6\0\32\10\13\0\131\10\3\0\6\10\2\0\6\10\2\0\6\10"+
    "\2\0\3\10\43\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\2\0\1\1\2\2\2\3\1\2\1\4\1\5\1\6"+
    "\11\4\1\1\1\7\2\10\1\1\2\11\2\0\1\12"+
    "\1\13\1\14\2\4\1\15\12\4\2\0\1\1\1\16"+
    "\1\1\1\0\1\12\1\0\1\17\2\4\1\20\1\21"+
    "\2\4\1\22\2\4\1\23\1\24\3\4\1\25\1\4"+
    "\1\26\10\4\1\27\4\4\1\30\1\31\5\4\1\32"+
    "\10\4\1\33\1\4\1\34\6\4\1\35";

  private static int [] zzUnpackAction() {
    int [] result = new int[110];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\44\0\110\0\154\0\220\0\264\0\154\0\330"+
    "\0\374\0\154\0\154\0\u0120\0\u0144\0\u0168\0\u018c\0\u01b0"+
    "\0\u01d4\0\u01f8\0\u021c\0\u0240\0\154\0\154\0\u0264\0\154"+
    "\0\u0288\0\u02ac\0\u02d0\0\220\0\u02f4\0\154\0\154\0\154"+
    "\0\u0318\0\u033c\0\374\0\u0360\0\u0384\0\u03a8\0\u03cc\0\u03f0"+
    "\0\u0414\0\u0438\0\u045c\0\u0480\0\u04a4\0\u02ac\0\u04c8\0\u04ec"+
    "\0\154\0\u0510\0\u0534\0\220\0\u0558\0\374\0\u057c\0\u05a0"+
    "\0\374\0\374\0\u05c4\0\u05e8\0\374\0\u060c\0\u0630\0\374"+
    "\0\374\0\u0654\0\u0678\0\u069c\0\374\0\u06c0\0\u06e4\0\u0708"+
    "\0\u072c\0\u0750\0\u0774\0\u0798\0\u07bc\0\u07e0\0\u0804\0\154"+
    "\0\u0828\0\u084c\0\u0870\0\u0894\0\154\0\374\0\u08b8\0\u08dc"+
    "\0\u0900\0\u0924\0\u0948\0\374\0\u096c\0\u0990\0\u09b4\0\u09d8"+
    "\0\u09fc\0\u0a20\0\u0a44\0\u0a68\0\374\0\u0a8c\0\374\0\u0ab0"+
    "\0\u0ad4\0\u0af8\0\u0b1c\0\u0b40\0\u0b64\0\374";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[110];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\3\4\1\5\1\6\1\7\1\10\1\4\1\11\1\4"+
    "\1\12\1\13\1\14\2\11\1\15\1\16\1\11\1\17"+
    "\1\20\1\21\3\11\1\22\1\23\1\4\1\11\1\24"+
    "\5\11\2\25\4\26\1\27\1\30\36\26\4\31\1\6"+
    "\1\7\1\32\1\33\34\31\44\0\2\34\1\35\1\36"+
    "\2\0\36\34\5\0\1\7\44\0\1\37\1\40\44\0"+
    "\2\11\2\0\16\11\1\0\7\11\12\0\2\11\2\0"+
    "\1\11\1\41\1\11\1\42\12\11\1\0\7\11\12\0"+
    "\2\11\2\0\16\11\1\0\6\11\1\43\12\0\2\11"+
    "\2\0\16\11\1\0\1\44\6\11\12\0\2\11\2\0"+
    "\3\11\1\45\12\11\1\0\7\11\12\0\2\11\2\0"+
    "\16\11\1\0\5\11\1\46\1\11\12\0\2\11\2\0"+
    "\7\11\1\47\6\11\1\0\2\11\1\50\4\11\12\0"+
    "\2\11\2\0\3\11\1\51\12\11\1\0\7\11\12\0"+
    "\2\11\2\0\1\11\1\52\6\11\1\53\5\11\1\0"+
    "\7\11\12\0\2\11\2\0\3\11\1\54\6\11\1\55"+
    "\3\11\1\0\7\11\7\0\1\30\36\0\4\31\2\0"+
    "\1\56\1\57\40\31\2\0\1\60\1\0\40\31\2\0"+
    "\1\61\1\62\34\31\1\34\1\63\1\35\1\64\1\0"+
    "\1\65\35\34\1\63\10\0\2\11\2\0\2\11\1\66"+
    "\13\11\1\0\7\11\12\0\2\11\2\0\4\11\1\67"+
    "\11\11\1\0\7\11\12\0\2\11\2\0\16\11\1\0"+
    "\1\11\1\70\5\11\12\0\2\11\2\0\13\11\1\71"+
    "\2\11\1\0\2\11\1\72\4\11\12\0\2\11\2\0"+
    "\7\11\1\73\6\11\1\0\7\11\12\0\2\11\2\0"+
    "\1\74\13\11\1\75\1\11\1\0\7\11\12\0\2\11"+
    "\2\0\16\11\1\0\1\11\1\76\5\11\12\0\2\11"+
    "\2\0\15\11\1\77\1\0\7\11\12\0\2\11\2\0"+
    "\7\11\1\100\6\11\1\0\7\11\12\0\2\11\2\0"+
    "\13\11\1\101\2\11\1\0\7\11\12\0\2\11\2\0"+
    "\6\11\1\102\7\11\1\0\7\11\12\0\2\11\2\0"+
    "\5\11\1\103\10\11\1\0\7\11\2\0\4\31\3\0"+
    "\1\62\40\31\2\0\1\60\1\57\40\31\2\0\1\56"+
    "\1\62\34\31\1\34\1\63\1\35\1\36\1\0\1\65"+
    "\35\34\1\63\1\0\1\65\1\34\2\0\1\65\35\0"+
    "\1\65\10\0\2\11\2\0\5\11\1\104\10\11\1\0"+
    "\7\11\12\0\2\11\2\0\3\11\1\105\12\11\1\0"+
    "\7\11\12\0\2\11\2\0\2\11\1\106\13\11\1\0"+
    "\7\11\12\0\2\11\2\0\4\11\1\107\11\11\1\0"+
    "\7\11\12\0\2\11\2\0\16\11\1\0\5\11\1\110"+
    "\1\11\12\0\2\11\2\0\1\111\15\11\1\0\7\11"+
    "\12\0\2\11\2\0\1\112\15\11\1\0\7\11\12\0"+
    "\2\11\2\0\16\11\1\0\3\11\1\113\3\11\12\0"+
    "\2\11\2\0\1\11\1\114\14\11\1\0\7\11\12\0"+
    "\2\11\2\0\13\11\1\115\2\11\1\0\7\11\12\0"+
    "\2\11\2\0\1\116\15\11\1\0\7\11\12\0\2\11"+
    "\2\0\6\11\1\117\7\11\1\0\7\11\12\0\2\11"+
    "\2\0\16\11\1\120\7\11\12\0\2\11\2\0\10\11"+
    "\1\121\5\11\1\0\7\11\12\0\2\11\2\0\12\11"+
    "\1\122\3\11\1\0\7\11\12\0\2\11\2\0\6\11"+
    "\1\123\7\11\1\0\7\11\12\0\2\11\2\0\16\11"+
    "\1\0\5\11\1\124\1\11\12\0\2\11\2\0\16\11"+
    "\1\125\7\11\12\0\2\11\2\0\4\11\1\126\11\11"+
    "\1\0\7\11\12\0\2\11\2\0\1\127\15\11\1\0"+
    "\7\11\12\0\2\11\2\0\16\11\1\0\4\11\1\130"+
    "\2\11\12\0\2\11\2\0\6\11\1\131\7\11\1\0"+
    "\7\11\12\0\2\11\2\0\5\11\1\132\10\11\1\0"+
    "\7\11\12\0\2\11\2\0\4\11\1\133\11\11\1\0"+
    "\7\11\12\0\2\11\2\0\3\11\1\134\12\11\1\0"+
    "\7\11\12\0\2\11\2\0\3\11\1\135\12\11\1\0"+
    "\7\11\12\0\2\11\2\0\16\11\1\0\3\11\1\136"+
    "\3\11\12\0\2\11\2\0\3\11\1\137\12\11\1\0"+
    "\7\11\12\0\2\11\2\0\7\11\1\140\6\11\1\0"+
    "\7\11\12\0\2\11\2\0\10\11\1\141\5\11\1\0"+
    "\7\11\12\0\2\11\2\0\7\11\1\142\6\11\1\0"+
    "\7\11\12\0\2\11\2\0\4\11\1\143\11\11\1\0"+
    "\7\11\12\0\2\11\2\0\7\11\1\144\6\11\1\0"+
    "\7\11\12\0\2\11\2\0\4\11\1\145\11\11\1\0"+
    "\7\11\12\0\2\11\2\0\10\11\1\146\5\11\1\0"+
    "\7\11\12\0\2\11\2\0\16\11\1\0\4\11\1\147"+
    "\2\11\12\0\2\11\2\0\7\11\1\150\6\11\1\0"+
    "\7\11\12\0\2\11\2\0\4\11\1\151\11\11\1\0"+
    "\7\11\12\0\2\11\2\0\3\11\1\152\12\11\1\0"+
    "\7\11\12\0\2\11\2\0\6\11\1\153\7\11\1\0"+
    "\7\11\12\0\2\11\2\0\11\11\1\154\4\11\1\0"+
    "\7\11\12\0\2\11\2\0\12\11\1\155\3\11\1\0"+
    "\7\11\12\0\2\11\2\0\13\11\1\156\2\11\1\0"+
    "\7\11\2\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[2952];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\2\0\1\1\1\11\2\1\1\11\2\1\2\11\11\1"+
    "\2\11\1\1\1\11\3\1\2\0\3\11\15\1\2\0"+
    "\1\1\1\11\1\1\1\0\1\1\1\0\32\1\1\11"+
    "\4\1\1\11\31\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[110];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** number of newlines encountered up to the start of the matched text */
  private int yyline;

  /** the number of characters up to the start of the matched text */
  private int yychar;

  /**
   * the number of characters from the last newline up to the start of the 
   * matched text
   */
  private int yycolumn;

  /** 
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;

  /* user code: */
  protected int lineNum;
  public int getLineNumber() 
    { return 1 + lineNum; /* zero-based */ }
  public Location getLocation() 
    { return new Location(yytext(), file, getLineNumber(), yychar); }
  public String getMatchedText() { return yytext(); }
  public  String file = "unknown file";


  /**
   * Creates a new scanner
   * There is also a java.io.InputStream version of this constructor.
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public Yylex(java.io.Reader in) {
    this.zzReader = in;
  }

  /**
   * Creates a new scanner.
   * There is also java.io.Reader version of this constructor.
   *
   * @param   in  the java.io.Inputstream to read input from.
   */
  public Yylex(java.io.InputStream in) {
    this(new java.io.InputStreamReader(in));
  }

  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x10000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 1706) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   * 
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {

    /* first: make room (if you can) */
    if (zzStartRead > 0) {
      System.arraycopy(zzBuffer, zzStartRead,
                       zzBuffer, 0,
                       zzEndRead-zzStartRead);

      /* translate stored positions */
      zzEndRead-= zzStartRead;
      zzCurrentPos-= zzStartRead;
      zzMarkedPos-= zzStartRead;
      zzStartRead = 0;
    }

    /* is the buffer big enough? */
    if (zzCurrentPos >= zzBuffer.length) {
      /* if not: blow it up */
      char newBuffer[] = new char[zzCurrentPos*2];
      System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
      zzBuffer = newBuffer;
    }

    /* finally: fill the buffer with new input */
    int numRead = zzReader.read(zzBuffer, zzEndRead,
                                            zzBuffer.length-zzEndRead);

    if (numRead > 0) {
      zzEndRead+= numRead;
      return false;
    }
    // unlikely but not impossible: read 0 characters, but not at end of stream    
    if (numRead == 0) {
      int c = zzReader.read();
      if (c == -1) {
        return true;
      } else {
        zzBuffer[zzEndRead++] = (char) c;
        return false;
      }     
    }

	// numRead < 0
    return true;
  }

    
  /**
   * Closes the input stream.
   */
  public final void yyclose() throws java.io.IOException {
    zzAtEOF = true;            /* indicate end of file */
    zzEndRead = zzStartRead;  /* invalidate buffer    */

    if (zzReader != null)
      zzReader.close();
  }


  /**
   * Resets the scanner to read from a new input stream.
   * Does not close the old reader.
   *
   * All internal variables are reset, the old input stream 
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <tt>ZZ_INITIAL</tt>.
   *
   * @param reader   the new input stream 
   */
  public final void yyreset(java.io.Reader reader) {
    zzReader = reader;
    zzAtBOL  = true;
    zzAtEOF  = false;
    zzEOFDone = false;
    zzEndRead = zzStartRead = 0;
    zzCurrentPos = zzMarkedPos = 0;
    yyline = yychar = yycolumn = 0;
    zzLexicalState = YYINITIAL;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final String yytext() {
    return new String( zzBuffer, zzStartRead, zzMarkedPos-zzStartRead );
  }


  /**
   * Returns the character at position <tt>pos</tt> from the 
   * matched text. 
   * 
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch. 
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer[zzStartRead+pos];
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of 
   * yypushback(int) and a match-all fallback rule) this method 
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  } 


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Contains user EOF-code, which will be executed exactly once,
   * when the end of file is reached
   */
  private void zzDoEOF() throws java.io.IOException {
    if (!zzEOFDone) {
      zzEOFDone = true;
      yyclose();
    }
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public java_cup.runtime.Symbol next_token() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    char [] zzBufferL = zzBuffer;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      yychar+= zzMarkedPosL-zzStartRead;

      boolean zzR = false;
      for (zzCurrentPosL = zzStartRead; zzCurrentPosL < zzMarkedPosL;
                                                             zzCurrentPosL++) {
        switch (zzBufferL[zzCurrentPosL]) {
        case '\u000B':
        case '\u000C':
        case '\u0085':
        case '\u2028':
        case '\u2029':
          yyline++;
          zzR = false;
          break;
        case '\r':
          yyline++;
          zzR = true;
          break;
        case '\n':
          if (zzR)
            zzR = false;
          else {
            yyline++;
          }
          break;
        default:
          zzR = false;
        }
      }

      if (zzR) {
        // peek one character ahead if it is \n (if we have counted one line too much)
        boolean zzPeek;
        if (zzMarkedPosL < zzEndReadL)
          zzPeek = zzBufferL[zzMarkedPosL] == '\n';
        else if (zzAtEOF)
          zzPeek = false;
        else {
          boolean eof = zzRefill();
          zzEndReadL = zzEndRead;
          zzMarkedPosL = zzMarkedPos;
          zzBufferL = zzBuffer;
          if (eof) 
            zzPeek = false;
          else 
            zzPeek = zzBufferL[zzMarkedPosL] == '\n';
        }
        if (zzPeek) yyline--;
      }
      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
  
      zzState = ZZ_LEXSTATE[zzLexicalState];


      zzForAction: {
        while (true) {
    
          if (zzCurrentPosL < zzEndReadL)
            zzInput = zzBufferL[zzCurrentPosL++];
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = zzBufferL[zzCurrentPosL++];
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          int zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
        case 4: 
          { return new Symbol(sym.IDENTIFIER, 1 + lineNum, yychar, yytext());
          }
        case 30: break;
        case 2: 
          { System.err.printf("ERROR in file %s: unknown character '%s' at line %d\n",
      file, yytext(), lineNum + 1); 
  Thread.dumpStack();
          }
        case 31: break;
        case 16: 
          { return new Symbol(sym.REL);
          }
        case 32: break;
        case 26: 
          { return new Symbol(sym.PACKAGE);
          }
        case 33: break;
        case 8: 
          { ++lineNum; yybegin(YYINITIAL);
          }
        case 34: break;
        case 19: 
          { return new Symbol(sym.FUN);
          }
        case 35: break;
        case 12: 
          { yybegin(commentstyle2);
          }
        case 36: break;
        case 14: 
          { yybegin(YYINITIAL);
          }
        case 37: break;
        case 29: 
          { return new Symbol(sym.CURRENTINTERVAL);
          }
        case 38: break;
        case 20: 
          { return new Symbol(sym.FIL);
          }
        case 39: break;
        case 9: 
          { System.out.println("ERROR In file " + file + ": Unclosed comment");
          }
        case 40: break;
        case 27: 
          { return new Symbol(sym.PERSISTENT);
          }
        case 41: break;
        case 3: 
          { ++lineNum;
          }
        case 42: break;
        case 21: 
          { return new Symbol(sym.TYPE);
          }
        case 43: break;
        case 18: 
          { return new Symbol(sym.IND);
          }
        case 44: break;
        case 22: 
          { return new Symbol(sym.INST);
          }
        case 45: break;
        case 15: 
          { return new Symbol(sym.SUB);
          }
        case 46: break;
        case 5: 
          { return new Symbol(sym.OPENPAREN);
          }
        case 47: break;
        case 28: 
          { return new Symbol(sym.NONBLOCKING);
          }
        case 48: break;
        case 17: 
          { return new Symbol(sym.REM);
          }
        case 49: break;
        case 11: 
          { yybegin(commentstyle1);
          }
        case 50: break;
        case 13: 
          { return new Symbol(sym.EQ);
          }
        case 51: break;
        case 7: 
          { /* nothing */
          }
        case 52: break;
        case 24: 
          { return new Symbol(sym.INSTS);
          }
        case 53: break;
        case 23: 
          { return new Symbol(sym.DEFS);
          }
        case 54: break;
        case 10: 
          { return new Symbol(sym.STR, new String(yytext()));
          }
        case 55: break;
        case 25: 
          { return new Symbol(sym.IMPORT);
          }
        case 56: break;
        case 6: 
          { return new Symbol(sym.CLOSEPAREN);
          }
        case 57: break;
        case 1: 
          { 
          }
        case 58: break;
        default: 
          if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
            zzAtEOF = true;
            zzDoEOF();
              { return new java_cup.runtime.Symbol(sym.EOF); }
          } 
          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
