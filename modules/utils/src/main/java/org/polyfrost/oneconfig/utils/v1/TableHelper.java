/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.utils.v1;

public final class TableHelper {
    public static String makeTableFromRows(String message, String[]... rows) {
        int[] widths = new int[rows[0].length];
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        int maxSize = 1;
        for (int width : widths) {
            maxSize += width + 3;
        }
        int cap = message.length() + maxSize * (rows.length + 4);
        StringBuilder sb = new StringBuilder(cap);
        sb.append(message).append('\n');

        for (int i = 0; i < maxSize; i++) {
            sb.append('-');
        }
        sb.append('\n');
        boolean first = true;
        for (String[] row : rows) {
            sb.append("| ");
            for (int i = 0; i < row.length; i++) {
                sb.append(row[i]);
                for (int j = 0; j < widths[i] - row[i].length(); j++) {
                    sb.append(' ');
                }
                sb.append(" | ");
            }
            sb.append('\n');
            if (first) {
                for (int i = 0; i < maxSize; i++) {
                    sb.append('-');
                }
                sb.append('\n');
                first = false;
            }
        }
        for (int i = 0; i < maxSize; i++) {
            sb.append('-');
        }

        return sb.toString();
    }

    public static String makeTableFromColumns(String message, String[]... columns) {
        String[][] rows = new String[columns[0].length][columns.length];
        for (int i = 0; i < columns.length; i++) {
            for (int j = 0; j < columns[i].length; j++) {
                rows[j][i] = columns[i][j];
            }
        }
        return makeTableFromRows(message, rows);
    }
}
