package com.github.rolandhe.file.persist.mysql;

/**
 * mysql字符串参数转义，在直接拼接sql时如果不转义，会导致sql注入。
 *
 * 本代码copy from mysql jdbc驱动包
 *
 */
public class MysqlStringEscape {
    private MysqlStringEscape() {
    }


    public static String escapeString(String x) {
        if (!isEscapeNeededForString(x)) {
            return x;
        }
        StringBuilder buf = new StringBuilder((int) (x.length() * 1.1));

        buf.append('\'');

        int stringLength = x.length();

        //
        // Note: buf.append(char) is _faster_ than appending in blocks, because the block append requires a System.arraycopy().... go figure...
        //

        for (int i = 0; i < stringLength; ++i) {
            char c = x.charAt(i);

            switch (c) {
                case 0: /* Must be escaped for 'mysql' */
                    buf.append('\\');
                    buf.append('0');
                    break;
                case '\n': /* Must be escaped for logs */
                    buf.append('\\');
                    buf.append('n');
                    break;
                case '\r':
                    buf.append('\\');
                    buf.append('r');
                    break;
                case '\\':
                    buf.append('\\');
                    buf.append('\\');
                    break;
                case '\'':
                    buf.append('\'');
                    buf.append('\'');
                    break;
                case '"': /* Better safe than sorry */
//                    if (this.session.getServerSession().useAnsiQuotedIdentifiers()) {
//                        buf.append('\\');
//                    }
                    buf.append('"');
                    break;
                case '\032': /* This gives problems on Win32 */
                    buf.append('\\');
                    buf.append('Z');
                    break;
                case '\u00a5':
                case '\u20a9':
                    // escape characters interpreted as backslash by mysql
//                    if (this.charsetEncoder != null) {
//                        CharBuffer cbuf = CharBuffer.allocate(1);
//                        ByteBuffer bbuf = ByteBuffer.allocate(1);
//                        cbuf.put(c);
//                        cbuf.position(0);
//                        this.charsetEncoder.encode(cbuf, bbuf, true);
//                        if (bbuf.get(0) == '\\') {
//                            buf.append('\\');
//                        }
//                    }
                    buf.append(c);
                    break;

                default:
                    buf.append(c);
            }
        }

        buf.append('\'');

        return buf.toString();
    }

    public static boolean isEscapeNeededForString(String x) {
        if(x == null || x.length() == 0){
            return false;
        }
        boolean needsHexEscape = false;

        int stringLength = x.length();

        for (int i = 0; i < stringLength; ++i) {
            char c = x.charAt(i);

            switch (c) {
                case 0: /* Must be escaped for 'mysql' */
                case '\n': /* Must be escaped for logs */
                case '\r':
                case '\\':
                case '\'':
                case '"': /* Better safe than sorry */
                case '\032': /* This gives problems on Win32 */
                    needsHexEscape = true;
                    break;
            }

            if (needsHexEscape) {
                break; // no need to scan more
            }
        }
        return needsHexEscape;
    }
}
