package yeah.im.liam.toolpalette.color;

import java.util.Locale;

/** Colour conversions: sRGB, linear light, OKLab and HSV. Colours are packed 0xRRGGBB. */
public final class ColorMath {
    private ColorMath() {
    }

    public static int red(int rgb) {
        return rgb >> 16 & 0xFF;
    }

    public static int green(int rgb) {
        return rgb >> 8 & 0xFF;
    }

    public static int blue(int rgb) {
        return rgb & 0xFF;
    }

    public static int rgb(int r, int g, int b) {
        return clamp8(r) << 16 | clamp8(g) << 8 | clamp8(b);
    }

    private static int clamp8(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static float toLinear(int channel) {
        float c = channel / 255f;
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055) / 1.055, 2.4);
    }

    public static int toSrgb(float linear) {
        float l = Math.max(0f, Math.min(1f, linear));
        float c = l <= 0.0031308f ? l * 12.92f : (float) (1.055 * Math.pow(l, 1 / 2.4) - 0.055);
        return Math.round(c * 255f);
    }

    public static int fromLinear(float r, float g, float b) {
        return rgb(toSrgb(r), toSrgb(g), toSrgb(b));
    }

    /** OKLab {L, a, b} of an sRGB colour. */
    public static float[] oklab(int rgb) {
        return oklabLinear(toLinear(red(rgb)), toLinear(green(rgb)), toLinear(blue(rgb)));
    }

    public static float[] oklabLinear(float r, float g, float b) {
        float l = (float) Math.cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b);
        float m = (float) Math.cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b);
        float s = (float) Math.cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b);
        return new float[] {
            0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
            1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
            0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
        };
    }

    /** The sRGB colour of an OKLab colour, clipped to the sRGB range. */
    public static int fromOklab(float[] lab) {
        float l = lab[0] + 0.3963377774f * lab[1] + 0.2158037573f * lab[2];
        float m = lab[0] - 0.1055613458f * lab[1] - 0.0638541728f * lab[2];
        float s = lab[0] - 0.0894841775f * lab[1] - 1.2914855480f * lab[2];
        l = l * l * l;
        m = m * m * m;
        s = s * s * s;
        return fromLinear(
            4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
            -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
            -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
        );
    }

    public static float[] lerp(float[] from, float[] to, float t) {
        return new float[] {
            from[0] + (to[0] - from[0]) * t,
            from[1] + (to[1] - from[1]) * t,
            from[2] + (to[2] - from[2]) * t
        };
    }

    public static float distanceSquared(float[] x, float[] y) {
        float dl = x[0] - y[0];
        float da = x[1] - y[1];
        float db = x[2] - y[2];
        return dl * dl + da * da + db * db;
    }

    /** HSV {hue 0-1, saturation 0-1, value 0-1}. */
    public static float[] hsv(int rgb) {
        float r = red(rgb) / 255f;
        float g = green(rgb) / 255f;
        float b = blue(rgb) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h = 0f;
        if (delta > 0f) {
            if (max == r) {
                h = ((g - b) / delta) % 6f;
            } else if (max == g) {
                h = (b - r) / delta + 2f;
            } else {
                h = (r - g) / delta + 4f;
            }
            h /= 6f;
            if (h < 0f) {
                h += 1f;
            }
        }
        return new float[] {h, max == 0f ? 0f : delta / max, max};
    }

    public static int fromHsv(float h, float s, float v) {
        float hue = (h - (float) Math.floor(h)) * 6f;
        int sector = (int) hue % 6;
        float f = hue - (int) hue;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return rgb(Math.round(r * 255f), Math.round(g * 255f), Math.round(b * 255f));
    }

    public static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /** Reads RRGGBB or RGB, with or without a leading #; -1 if it isn't a colour. */
    public static int parseHex(String text) {
        String s = text.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        }
        if (s.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
