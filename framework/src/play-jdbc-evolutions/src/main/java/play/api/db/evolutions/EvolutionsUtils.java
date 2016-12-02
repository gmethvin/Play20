package play.api.db.evolutions;

import java.util.List;

/**
 * Internal utilities for dealing with evolutions.
 */
class EvolutionsUtils {

  /**
   * Split an SQL script into separate statements delimited by the provided
   * separator string. Each individual statement will be added to the provided
   * {@code List}.
   * <p>Within the script, the provided {@code commentPrefix} will be honored:
   * any text beginning with the comment prefix and extending to the end of the
   * line will be omitted from the output. Similarly, the provided
   * {@code blockCommentStartDelimiter} and {@code blockCommentEndDelimiter}
   * delimiters will be honored: any text enclosed in a block comment will be
   * omitted from the output. In addition, multiple adjacent whitespace characters
   * will be collapsed into a single space.
   * @param script the SQL script; never {@code null} or empty
   * @param separator text separating each statement &mdash; typically a ';' or
   * newline character; never {@code null}
   * @param commentPrefix the prefix that identifies SQL line comments &mdash;
   * typically "--"; never {@code null} or empty
   * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter;
   * never {@code null} or empty
   * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter;
   * never {@code null} or empty
   * @param statements the list that will contain the individual statements
   *
   * <em>
   * This code was adapted from the ScriptUtils class in the Spring Framework, which is released under the Apache
   * license, version 2, the same license used by Play. You can view the
   * <a href="https://git.io/v1nS4">original code on GitHub</a>.
   * </em>
   */
  static void splitSqlScript(String script, String separator, String commentPrefix,
      String blockCommentStartDelimiter, String blockCommentEndDelimiter, List<String> statements) {

    StringBuilder sb = new StringBuilder();
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    boolean inEscape = false;
    char[] content = script.toCharArray();
    for (int i = 0; i < script.length(); i++) {
      char c = content[i];  
      if (inEscape) {
        inEscape = false;
        sb.append(c);
        continue;
      }
      // MySQL style escapes
      if (c == '\\') {
        inEscape = true;
        sb.append(c);
        continue;
      }
      if (!inDoubleQuote && (c == '\'')) {
        inSingleQuote = !inSingleQuote;
      }
      else if (!inSingleQuote && (c == '"')) {
        inDoubleQuote = !inDoubleQuote;
      }
      if (!inSingleQuote && !inDoubleQuote) {
        if (script.startsWith(separator, i)) {
          // we've reached the end of the current statement
          if (sb.length() > 0) {
            statements.add(sb.toString());
            sb = new StringBuilder();
          }
          i += separator.length() - 1;
          continue;
        }
        else if (script.startsWith(commentPrefix, i)) {
          // skip over any content from the start of the comment to the EOL
          int indexOfNextNewline = script.indexOf("\n", i);
          if (indexOfNextNewline > i) {
            i = indexOfNextNewline;
            continue;
          }
          else {
            // if there's no EOL, we must be at the end
            // of the script, so stop here.
            break;
          }
        }
        else if (script.startsWith(blockCommentStartDelimiter, i)) {
          // skip over any block comments
          int indexOfCommentEnd = script.indexOf(blockCommentEndDelimiter, i);
          if (indexOfCommentEnd > i) {
            i = indexOfCommentEnd + blockCommentEndDelimiter.length() - 1;
            continue;
          }
          else {
            throw new RuntimeException(String.format("Missing block comment end delimiter [%s].", blockCommentEndDelimiter);
          }
        }
        else if (c == ' ' || c == '\n' || c == '\t') {
          // avoid multiple adjacent whitespace characters
          if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
            c = ' ';
          }
          else {
            continue;
          }
        }
      }
      sb.append(c);
    }
    if (sb.length() > 0) {
      statements.add(sb.toString());
    }
  }
}
