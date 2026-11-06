package com.benschnorr.survival.featrues.signing;

/**
 * Checks the letter-case rule of {@code /signItem}. Plain main; exits non-zero on failure.
 * <pre>
 * javac -d /tmp/sigcheck \
 *   Survival/src/main/java/com/benschnorr/survival/featrues/signing/SignItemLabel.java \
 *   Survival/src/test/java/com/benschnorr/survival/featrues/signing/SignItemLabelCheck.java
 * java -cp /tmp/sigcheck com.benschnorr.survival.featrues.signing.SignItemLabelCheck
 * </pre>
 */
public final class SignItemLabelCheck {
    static int failed = 0;

    static void expect(String in, String expected) {
        String actual = SignItemLabel.rewrite(in);
        if (expected == null ? actual != null : !expected.equals(actual)) {
            System.out.println("FAIL " + in + ": expected " + expected + " got " + actual);
            failed++;
        }
    }

    public static void main(String[] args) {
        expect("/signItem", "/signitem");
        expect("/SIGNITEM", "/signitem");
        expect("/signitem", null);
        expect("/signItem extra", "/signitem extra");
        expect("/signItem Extra Arg", "/signitem Extra Arg");
        expect("/signitemx", null);
        expect("/signItemX", null);
        expect("/other", null);
        expect("/", null);
        if (failed > 0) System.exit(1);
        System.out.println("ok");
    }
}
