package gui.qr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Implementación ligera del generador de QR basada en la librería pública de Project Nayuki
 * (Unlicense/MIT). Incluida en el proyecto para evitar dependencias externas durante la
 * ejecución en entornos sin conexión.
 */
public final class QrCode {

    public enum Ecc {
        LOW(1),
        MEDIUM(0),
        QUARTILE(3),
        HIGH(2);

        final int ordinal;

        Ecc(int ordinal) {
            this.ordinal = ordinal;
        }
    }

    public final int version;
    public final int size;
    public final Ecc errorCorrectionLevel;
    private final byte[][] modules;
    private final boolean[][] isFunction;

    public static QrCode encodeText(String text, Ecc ecl) {
        Objects.requireNonNull(text);
        return encodeSegments(QrSegment.makeSegments(text), ecl);
    }

    public static QrCode encodeSegments(List<QrSegment> segs, Ecc ecl) {
        Objects.requireNonNull(segs);
        Objects.requireNonNull(ecl);
        int version = 1;
        for (; version <= 40; version++) {
            int capacity = getNumDataCodewords(version, ecl) * 8;
            int dataUsed = getTotalBits(segs, version);
            if (dataUsed != -1 && dataUsed <= capacity) {
                break;
            }
        }
        if (version > 40) {
            throw new IllegalArgumentException("Datos demasiado largos para codificar en QR");
        }
        int dataCapacityBits = getNumDataCodewords(version, ecl) * 8;
        BitBuffer bb = new BitBuffer();
        for (QrSegment seg : segs) {
            bb.appendBits(seg.mode.modeBits, 4);
            bb.appendBits(seg.numChars, seg.mode.numCharCountBits(version));
            for (int bit : seg.data) {
                bb.appendBits(bit, 1);
            }
        }
        int terminator = Math.min(4, dataCapacityBits - bb.getSize());
        bb.appendBits(0, terminator);
        int pad = (8 - (bb.getSize() % 8)) % 8;
        bb.appendBits(0, pad);
        int[] paddingBytes = {0xec, 0x11};
        int i = 0;
        while (bb.getSize() < dataCapacityBits) {
            bb.appendBits(paddingBytes[i % 2], 8);
            i++;
        }
        int numDataCodewords = dataCapacityBits / 8;
        byte[] dataCodewords = new byte[numDataCodewords];
        for (i = 0; i < numDataCodewords; i++) {
            int val = 0;
            for (int j = 0; j < 8; j++) {
                val = (val << 1) | bb.getBits()[i * 8 + j];
            }
            dataCodewords[i] = (byte) val;
        }
        QrCode mejor = null;
        int mejorPenalizacion = Integer.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            QrCode candidato = new QrCode(version, ecl, dataCodewords, mask);
            int penalizacion = candidato.getPenaltyScore();
            if (penalizacion < mejorPenalizacion) {
                mejorPenalizacion = penalizacion;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private static int getTotalBits(List<QrSegment> segs, int version) {
        long result = 0;
        for (QrSegment seg : segs) {
            int ccbits = seg.mode.numCharCountBits(version);
            if (seg.numChars >= (1 << ccbits)) {
                return -1;
            }
            result += 4L + ccbits + seg.data.length;
        }
        if (result > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) result;
    }

    private static final int[] ECC_CODEWORDS_PER_BLOCK = {
            7,10,15,20,26,18,20,24,30,18,20,24,26,30,22,24,28,30,28,28,28,28,30,30,26,28,30,30,30,30,30,30,30,30,30,30,30,30,30,30,
            10,16,26,18,24,16,18,22,22,26,30,22,22,24,24,28,28,26,26,26,26,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28,
            13,22,18,26,18,24,18,22,20,24,28,26,24,20,30,24,28,28,26,30,28,30,24,30,30,30,28,30,30,30,30,30,30,30,30,30,30,30,30,30,
            17,28,22,16,22,28,26,26,24,28,24,28,22,24,24,30,28,28,26,28,30,24,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30
    };

    private static final int[] NUM_ERROR_CORRECTION_BLOCKS = {
            1,1,1,1,1,2,2,2,2,4,4,4,4,4,6,6,6,6,7,8,8,9,9,10,12,12,12,13,14,15,16,17,18,19,19,20,21,22,24,25,
            1,1,1,2,2,4,4,4,5,5,5,8,9,9,10,10,11,13,14,16,17,17,18,20,21,23,25,26,28,29,31,33,35,37,38,40,43,45,47,49,
            1,1,2,4,4,4,5,6,8,8,8,10,12,16,12,17,16,18,21,20,23,23,25,27,29,34,34,35,38,40,43,45,48,51,53,56,59,62,65,68,
            1,1,2,4,6,8,8,10,12,16,16,18,21,23,25,27,29,31,33,35,37,38,40,43,45,47,49,51,53,55,57,59,61,63,65,67,69,71,73,75
    };

    private static int getNumDataCodewords(int version, Ecc ecl) {
        int index = (ecl.ordinal * 40 + (version - 1));
        int totalCodewords = getNumRawDataModules(version) / 8;
        int ec = ECC_CODEWORDS_PER_BLOCK[index];
        int blocks = NUM_ERROR_CORRECTION_BLOCKS[index];
        return totalCodewords - ec * blocks;
    }

    private static int getNumRawDataModules(int version) {
        int result = (16 * version + 128) * version + 64;
        if (version >= 2) {
            int numAlign = version / 7 + 2;
            result -= (25 * numAlign - 10) * numAlign - 55;
            if (version >= 7) {
                result -= 36;
            }
        }
        return result;
    }

    private QrCode(int version, Ecc ecl, byte[] dataCodewords, int mask) {
        this.version = version;
        this.errorCorrectionLevel = ecl;
        this.size = version * 4 + 17;
        this.modules = new byte[size][size];
        this.isFunction = new boolean[size][size];
        drawFunctionPatterns();
        byte[] allCodewords = applyErrorCorrection(dataCodewords);
        drawCodewords(allCodewords);
        this.applyMask(mask);
        this.drawFormatBits(mask);
    }

    private int getPenaltyScore() {
        int result = 0;
        for (int y = 0; y < size; y++) {
            int runColor = modules[y][0];
            int runLen = 1;
            for (int x = 1; x < size; x++) {
                int color = modules[y][x];
                if (color == runColor) {
                    runLen++;
                } else {
                    if (runLen >= 5) {
                        result += 3 + (runLen - 5);
                    }
                    runColor = color;
                    runLen = 1;
                }
            }
            if (runLen >= 5) {
                result += 3 + (runLen - 5);
            }
        }

        for (int x = 0; x < size; x++) {
            int runColor = modules[0][x];
            int runLen = 1;
            for (int y = 1; y < size; y++) {
                int color = modules[y][x];
                if (color == runColor) {
                    runLen++;
                } else {
                    if (runLen >= 5) {
                        result += 3 + (runLen - 5);
                    }
                    runColor = color;
                    runLen = 1;
                }
            }
            if (runLen >= 5) {
                result += 3 + (runLen - 5);
            }
        }

        for (int y = 0; y < size - 1; y++) {
            for (int x = 0; x < size - 1; x++) {
                int color = modules[y][x];
                if (color == modules[y][x + 1] && color == modules[y + 1][x] && color == modules[y + 1][x + 1]) {
                    result += 3;
                }
            }
        }

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size - 6; x++) {
                if (modules[y][x] == 1 && modules[y][x + 1] == 0 && modules[y][x + 2] == 1 && modules[y][x + 3] == 1
                        && modules[y][x + 4] == 1 && modules[y][x + 5] == 0 && modules[y][x + 6] == 1) {
                    boolean before = x >= 4 && modules[y][x - 1] == 0 && modules[y][x - 2] == 0 && modules[y][x - 3] == 0 && modules[y][x - 4] == 0;
                    boolean after = x + 11 < size && modules[y][x + 7] == 0 && modules[y][x + 8] == 0 && modules[y][x + 9] == 0 && modules[y][x + 10] == 0;
                    if (before || after) {
                        result += 40;
                    }
                }
            }
        }
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size - 6; y++) {
                if (modules[y][x] == 1 && modules[y + 1][x] == 0 && modules[y + 2][x] == 1 && modules[y + 3][x] == 1
                        && modules[y + 4][x] == 1 && modules[y + 5][x] == 0 && modules[y + 6][x] == 1) {
                    boolean before = y >= 4 && modules[y - 1][x] == 0 && modules[y - 2][x] == 0 && modules[y - 3][x] == 0 && modules[y - 4][x] == 0;
                    boolean after = y + 11 < size && modules[y + 7][x] == 0 && modules[y + 8][x] == 0 && modules[y + 9][x] == 0 && modules[y + 10][x] == 0;
                    if (before || after) {
                        result += 40;
                    }
                }
            }
        }

        int dark = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (modules[y][x] == 1) dark++;
            }
        }
        int total = size * size;
        int k = Math.abs(dark * 20 - total * 10) / total;
        result += k * 10;
        return result;
    }

    public boolean getModule(int x, int y) {
        return modules[y][x] == 1;
    }

    private byte[] applyErrorCorrection(byte[] data) {
        int index = errorCorrectionLevel.ordinal * 40 + (version - 1);
        int blocks = NUM_ERROR_CORRECTION_BLOCKS[index];
        int eccLen = ECC_CODEWORDS_PER_BLOCK[index];
        int totalCodewords = getNumRawDataModules(version) / 8;
        int dataLen = totalCodewords - eccLen * blocks;
        int shortBlockLen = dataLen / blocks;
        int numLongBlocks = dataLen % blocks;
        byte[][] dcBlocks = new byte[blocks][];
        int[][] ecBlocks = new int[blocks][];
        int pos = 0;
        for (int i = 0; i < blocks; i++) {
            int curLen = shortBlockLen + (i < numLongBlocks ? 1 : 0);
            byte[] block = Arrays.copyOfRange(data, pos, pos + curLen);
            pos += curLen;
            dcBlocks[i] = block;
            ecBlocks[i] = reedSolomonComputeRemainder(block, eccLen);
        }
        BitBuffer bb = new BitBuffer();
        for (int i = 0; i < shortBlockLen + 1; i++) {
            for (int j = 0; j < blocks; j++) {
                if (i != shortBlockLen || j < numLongBlocks) {
                    bb.appendBits(dcBlocks[j][i] & 0xff, 8);
                }
            }
        }
        for (int i = 0; i < eccLen; i++) {
            for (int j = 0; j < blocks; j++) {
                bb.appendBits(ecBlocks[j][i] & 0xff, 8);
            }
        }
        int totalBits = totalCodewords * 8;
        bb.appendBits(0, totalBits - bb.getSize());
        byte[] result = new byte[totalCodewords];
        for (int i = 0; i < totalCodewords; i++) {
            int val = 0;
            for (int j = 0; j < 8; j++) {
                val = (val << 1) | bb.getBits()[i * 8 + j];
            }
            result[i] = (byte) val;
        }
        return result;
    }

    private void drawFunctionPatterns() {
        for (int i = 0; i < size; i++) {
            setFunctionModule(6, i, i % 2 == 0);
            setFunctionModule(i, 6, i % 2 == 0);
        }
        drawFinderPattern(3, 3);
        drawFinderPattern(size - 4, 3);
        drawFinderPattern(3, size - 4);
        drawSeparators();
        drawAlignmentPatterns();
        drawTimingPatterns();
        drawVersion();
    }

    private void drawFinderPattern(int x, int y) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                int xx = x + dx, yy = y + dy;
                if (0 <= xx && xx < size && 0 <= yy && yy < size) {
                    setFunctionModule(xx, yy, dist != 2 && dist != 4);
                }
            }
        }
    }

    private void drawSeparators() {
        for (int i = 0; i < 8; i++) {
            setFunctionModule(7, i, false);
            setFunctionModule(i, 7, false);
            setFunctionModule(size - 8, i, false);
            setFunctionModule(size - 1 - i, 7, false);
            setFunctionModule(7, size - 1 - i, false);
            setFunctionModule(i, size - 8, false);
        }
    }

    private void drawAlignmentPatterns() {
        if (version == 1) return;
        int numAlign = version / 7 + 2;
        int step = (version == 32) ? 26 : (version * 4 + numAlign * 2 + 1) / (2 * numAlign - 2) * 2;
        int[] coords = new int[numAlign];
        coords[0] = 6;
        coords[numAlign - 1] = size - 7;
        for (int i = 1; i < numAlign - 1; i++) {
            coords[i] = coords[i - 1] + step;
        }
        for (int i = 0; i < coords.length; i++) {
            for (int j = 0; j < coords.length; j++) {
                int x = coords[i];
                int y = coords[j];
                if (isFunction[x][y]) continue;
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        int dist = Math.max(Math.abs(dx), Math.abs(dy));
                        setFunctionModule(x + dx, y + dy, dist != 1);
                    }
                }
            }
        }
    }

    private void drawTimingPatterns() {
        for (int i = 8; i < size - 8; i++) {
            boolean val = i % 2 == 0;
            setFunctionModule(6, i, val);
            setFunctionModule(i, 6, val);
        }
    }

    private void drawVersion() {
        if (version < 7) return;
        int rem = version;
        int bits = 0;
        for (int i = 0; i < 12; i++) {
            bits = (bits << 1) | (rem & 1);
            rem >>= 1;
        }
        bits = (bits << 6) | getBchCode(bits, 0x1f25);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                boolean bit = ((bits >> (i * 3 + j)) & 1) != 0;
                setFunctionModule(i, size - 11 + j, bit);
                setFunctionModule(size - 11 + j, i, bit);
            }
        }
    }

    private void drawFormatBits(int mask) {
        int data = errorCorrectionLevel.ordinal;
        data = (data << 3) | mask;
        int rem = data;
        for (int i = 0; i < 10; i++) {
            rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        }
        data = (data << 10) | rem;
        data ^= 0x5412;
        for (int i = 0; i <= 5; i++) setFunctionModule(8, i, ((data >>> i) & 1) != 0);
        setFunctionModule(8, 7, ((data >>> 6) & 1) != 0);
        setFunctionModule(8, 8, ((data >>> 7) & 1) != 0);
        setFunctionModule(7, 8, ((data >>> 8) & 1) != 0);
        for (int i = 9; i < 15; i++) setFunctionModule(14 - i, 8, ((data >>> i) & 1) != 0);
        for (int i = 0; i < 8; i++) setFunctionModule(size - 1 - i, 8, ((data >>> i) & 1) != 0);
        for (int i = 8; i < 15; i++) setFunctionModule(8, size - 15 + i, ((data >>> i) & 1) != 0);
        setFunctionModule(8, size - 8, true);
    }

    private void drawCodewords(byte[] data) {
        int i = 0;
        int direction = -1;
        int x = size - 1;
        int y = size - 1;
        while (x > 0) {
            if (x == 6) x--;
            for (int k = 0; k < size; k++) {
                int yy = y + direction * k;
                for (int j = 0; j < 2; j++) {
                    int xx = x - j;
                    if (!isFunction[xx][yy] && i < data.length * 8) {
                        boolean bit = ((data[i / 8] >>> (7 - i % 8)) & 1) == 1;
                        modules[yy][xx] = (byte) (bit ? 1 : 0);
                        i++;
                    }
                }
            }
            x -= 2;
            direction = -direction;
            y += direction * (size - 1);
        }
    }

    private void applyMask(int mask) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (isFunction[x][y]) continue;
                boolean invert;
                switch (mask) {
                    case 0 -> invert = (x + y) % 2 == 0;
                    case 1 -> invert = y % 2 == 0;
                    case 2 -> invert = x % 3 == 0;
                    case 3 -> invert = (x + y) % 3 == 0;
                    case 4 -> invert = ((x / 3) + (y / 2)) % 2 == 0;
                    case 5 -> invert = (x * y) % 2 + (x * y) % 3 == 0;
                    case 6 -> invert = ((x * y) % 3 + x + y) % 2 == 0;
                    case 7 -> invert = ((x + y) % 2 + (x * y) % 3) % 2 == 0;
                    default -> throw new IllegalArgumentException("Máscara inválida");
                }
                if (invert) {
                    modules[y][x] ^= 1;
                }
            }
        }
    }

    private void setFunctionModule(int x, int y, boolean isDark) {
        modules[y][x] = (byte) (isDark ? 1 : 0);
        isFunction[x][y] = true;
    }

    private static int[] reedSolomonComputeRemainder(byte[] data, int degree) {
        int[] result = new int[degree];
        for (byte b : data) {
            int factor = (b ^ result[0]) & 0xFF;
            System.arraycopy(result, 1, result, 0, degree - 1);
            result[degree - 1] = 0;
            if (factor != 0) {
                int[] alpha = RS_EXP;
                int[] log = RS_LOG;
                for (int i = 0; i < degree; i++) {
                    result[i] ^= alpha[(log[factor] + RS_COEFFS[degree][i]) % 255];
                }
            }
        }
        return result;
    }

    private static int getBchCode(int val, int poly) {
        int msb = Integer.highestOneBit(poly);
        val <<= Integer.numberOfLeadingZeros(val) - Integer.numberOfLeadingZeros(msb);
        poly <<= Integer.numberOfLeadingZeros(val) - Integer.numberOfLeadingZeros(msb);
        while (Integer.numberOfLeadingZeros(val) <= Integer.numberOfLeadingZeros(msb)) {
            val ^= poly;
            poly >>= Integer.numberOfTrailingZeros(poly);
        }
        return val;
    }

    private static final class BitBuffer {
        private final List<Integer> bits = new ArrayList<>();

        void appendBits(int val, int length) {
            if (length < 0 || length > 31) throw new IllegalArgumentException();
            for (int i = length - 1; i >= 0; i--) {
                bits.add((val >>> i) & 1);
            }
        }

        int[] getBits() {
            return bits.stream().mapToInt(Integer::intValue).toArray();
        }

        int getSize() { return bits.size(); }
    }

    // Reed-Solomon tables (log/exp)
    private static final int[] RS_EXP = new int[512];
    private static final int[] RS_LOG = new int[256];
    private static final int[][] RS_COEFFS = new int[71][];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            RS_EXP[i] = x;
            RS_LOG[x] = i;
            x = (x << 1) ^ ((x >>> 7) * 0x11d);
        }
        for (int i = 255; i < RS_EXP.length; i++) {
            RS_EXP[i] = RS_EXP[i - 255];
        }
        for (int degree = 1; degree < RS_COEFFS.length; degree++) {
            int[] poly = new int[degree];
            poly[degree - 1] = 1;
            int root = 0;
            for (int i = degree - 2; i >= 0; i--) {
                root = (root + 1) % 255;
                int coefficient = RS_EXP[root];
                for (int j = degree - 1; j > 0; j--) {
                    poly[j] = poly[j] ^ multiply(poly[j - 1], coefficient);
                }
                poly[0] = multiply(poly[0], coefficient);
            }
            RS_COEFFS[degree] = poly;
        }
    }

    private static int multiply(int x, int y) {
        if (x == 0 || y == 0) return 0;
        return RS_EXP[RS_LOG[x] + RS_LOG[y]];
    }
}