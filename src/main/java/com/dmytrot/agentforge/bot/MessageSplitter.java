package com.dmytrot.agentforge.bot;

import java.util.ArrayList;
import java.util.List;

/** Cuts long answers into pieces that fit into Telegram messages. */
final class MessageSplitter {

  /** Telegram rejects messages longer than this many characters. */
  static final int TELEGRAM_LIMIT = 4096;

  private MessageSplitter() {}

  /**
   * Splits a text, preferring to break at a line end.
   *
   * @param text text to split
   * @param limit maximum length of one piece
   * @return pieces in order; a single piece if the text already fits
   */
  static List<String> split(String text, int limit) {
    List<String> parts = new ArrayList<>();
    String rest = text;
    while (rest.length() > limit) {
      int cut = rest.lastIndexOf('\n', limit);
      if (cut <= 0) {
        cut = limit;
      }
      parts.add(rest.substring(0, cut));
      rest = rest.substring(cut).stripLeading();
    }
    parts.add(rest);
    return parts;
  }
}
