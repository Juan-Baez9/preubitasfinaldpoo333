package gui.qr;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Segmento de datos para la generación de QR. Código tomado y simplificado del proyecto
 * público "QR Code generator" de Project Nayuki (licencia MIT/Unlicense).
 */
public final class QrSegment {

    public enum Mode {
        NUMERIC(0x1, 10, 12, 14),
        ALPHANUMERIC(0x2, 9, 11, 13),
        BYTE(0x4, 8, 16, 16),
        KANJI(0x8, 8, 10, 12),
        ECI(0x7, 0, 0, 0);

        final int modeBits;
        private final int[] numBitsCharCount;

        Mode(int modeBits, int cc0, int cc1, int cc2) {
            this.modeBits = modeBits;
            numBitsCharCount = new int[]{cc0, cc1, cc2};
        }

        int numCharCountBits(int version) {
            if      (1 <= version && version <= 9)  return numBitsCharCount[0];
            else if (10 <= version && version <= 26) return numBitsCharCount[1];
            else if (27 <= version && version <= 40) return numBitsCharCount[2];
            else throw new IllegalArgumentException("Versión QR fuera de rango");
        }
    }

    public final Mode mode;
    public final int numChars;
    public final int[] data;

    public QrSegment(Mode mode, int numChars, int[] data) {
        this.mode = Objects.requireNonNull(mode);
        this.numChars = numChars;
        this.data = Objects.requireNonNull(data);
    }

    public static QrSegment makeBytes(byte[] data) {
        Objects.requireNonNull(data);
        int[] bits = new int[data.length * 8];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + 7 - j] = (data[i] >>> j) & 1;
            }
        }
        return new QrSegment(Mode.BYTE, data.length, bits);
    }

    public static QrSegment makeAlphanumeric(String text) {
        Objects.requireNonNull(text);
        final String ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";
        int length = text.length();
        List<Integer> bitBuffer = new ArrayList<>();
        int i = 0;
        while (i < length) {
            int c1 = ALPHANUMERIC_CHARSET.indexOf(text.charAt(i));
            if (c1 == -1) {
                throw new IllegalArgumentException("Caracter no alfanumérico en el QR");
            }
            if (i + 1 < length) {
                int c2 = ALPHANUMERIC_CHARSET.indexOf(text.charAt(i + 1));
                bitBuffer.addAll(appendBits(c1 * 45 + c2, 11));
                i += 2;
            } else {
                bitBuffer.addAll(appendBits(c1, 6));
                i += 1;
            }
        }
        int[] bits = bitBuffer.stream().mapToInt(Integer::intValue).toArray();
        return new QrSegment(Mode.ALPHANUMERIC, text.length(), bits);
    }

    public static List<QrSegment> makeSegments(String text) {
        Objects.requireNonNull(text);
        List<QrSegment> result = new ArrayList<>();
        result.add(makeBytes(text.getBytes(StandardCharsets.UTF_8)));
        return result;
    }

    private static List<Integer> appendBits(int val, int length) {
        List<Integer> out = new ArrayList<>(length);
        for (int i = length - 1; i >= 0; i--) {
            out.add((val >>> i) & 1);
        }
        return out;
    }
}