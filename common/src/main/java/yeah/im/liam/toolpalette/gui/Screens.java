package yeah.im.liam.toolpalette.gui;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/** Opens screens on every version this jar covers: 26.2 moved the open screen from Minecraft to Gui. */
public final class Screens {
    private static final MethodHandle SET;
    private static final MethodHandle GET;

    static {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType setType = MethodType.methodType(void.class, Screen.class);
        MethodHandle set;
        MethodHandle get;
        try {
            set = lookup.findVirtual(Gui.class, "setScreen", setType);
            get = lookup.findVirtual(Gui.class, "screen", MethodType.methodType(Screen.class));
        } catch (ReflectiveOperationException modern) {
            try {
                set = lookup.findVirtual(Minecraft.class, "setScreen", setType);
                get = lookup.findGetter(Minecraft.class, "screen", Screen.class);
            } catch (ReflectiveOperationException old) {
                old.addSuppressed(modern);
                throw new ExceptionInInitializerError(old);
            }
        }
        SET = set;
        GET = get;
    }

    private Screens() {
    }

    public static void set(@Nullable Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            SET.invoke(owner(minecraft), screen);
        } catch (Throwable e) {
            throw new IllegalStateException("Could not open " + screen, e);
        }
    }

    public static @Nullable Screen current() {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            return (Screen) GET.invoke(owner(minecraft));
        } catch (Throwable e) {
            throw new IllegalStateException("Could not read the open screen", e);
        }
    }

    private static Object owner(Minecraft minecraft) {
        return SET.type().parameterType(0) == Gui.class ? minecraft.gui : minecraft;
    }
}
