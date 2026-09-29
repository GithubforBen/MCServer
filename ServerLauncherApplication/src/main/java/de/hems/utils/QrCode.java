package de.hems.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * QR codes for the google authenticator link - drawn into the terminal by the installer, and as an image
 * on the admin website.
 */
public final class QrCode {

    private static final String BLACK_ON_WHITE = "\u001B[30;107m";
    private static final String RESET = "\u001B[0m";
    /** Modules of white around the code, which a scanner needs to find its edges. */
    private static final int MARGIN = 2;

    private QrCode() {
    }

    /**
     * Draws the code with text characters, so a phone can scan it straight off the terminal.
     * <p>
     * One character is one module wide and two high - the half blocks {@code ▀ ▄ █} - which keeps the code
     * roughly square in a normal terminal font. The colours are set explicitly, black on white, because a
     * scanner needs dark modules on a light ground and a dark terminal theme would otherwise invert it.
     *
     * @param text   what the code should contain
     * @param indent put in front of every line
     * @return the code as lines of text, or {@code null} if it could not be made
     */
    public static String terminal(String text, String indent) {
        BitMatrix matrix = encode(text);
        if (matrix == null) return null;
        StringBuilder out = new StringBuilder();
        for (int y = 0; y < matrix.getHeight(); y += 2) {
            out.append(indent).append(BLACK_ON_WHITE);
            for (int x = 0; x < matrix.getWidth(); x++) {
                boolean top = matrix.get(x, y);
                boolean bottom = y + 1 < matrix.getHeight() && matrix.get(x, y + 1);
                out.append(top ? (bottom ? '█' : '▀') : (bottom ? '▄' : ' '));
            }
            out.append(RESET).append('\n');
        }
        return out.toString();
    }

    /**
     * @param text what the code should contain
     * @return the code as an svg {@code data:} url for an {@code img}, or {@code null} if it could not be made
     */
    public static String svgDataUrl(String text) {
        BitMatrix matrix = encode(text);
        if (matrix == null) return null;
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y)) path.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
            }
        }
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + matrix.getWidth() + " "
                + matrix.getHeight() + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" "
                + "fill=\"#fff\"/><path fill=\"#000\" d=\"" + path + "\"/></svg>";
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private static BitMatrix encode(String text) {
        try {
            return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L,
                    EncodeHintType.MARGIN, MARGIN));
        } catch (WriterException e) {
            return null;
        }
    }
}
