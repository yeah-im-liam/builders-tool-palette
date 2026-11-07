package yeah.im.liam.toolpalette.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import yeah.im.liam.toolpalette.PaletteState;
import yeah.im.liam.toolpalette.ToolPalette;
import yeah.im.liam.toolpalette.color.ColorMath;
import yeah.im.liam.toolpalette.color.Gradient;
import yeah.im.liam.toolpalette.color.PaletteIndex;

/**
 * The colour picker and gradient helper, as two tabs of one screen with a searchable block list and the hotbar. Blocks are
 * picked up as copies on the cursor; only in creative mode can they be put on the hotbar.
 */
public final class PaletteScreen extends Screen {
    private static final int PAD = 7;
    private static final int SLOT = 18;
    private static final int MAIN_W = 232;
    private static final int MAIN_H = 136;
    private static final int TAB_H = 20;
    private static final int HEIGHT = PAD + TAB_H + 4 + MAIN_H + 4 + SLOT + PAD;

    private static final int SV = 80;
    private static final int HUE_X = 84;
    private static final int HUE_W = 10;
    private static final int RIGHT_X = 100;
    private static final int SWATCH_X = 122;
    private static final int SWATCH_W = 50;
    private static final int HEX_X = 176;
    private static final int SLIDER_X = 108;
    private static final int SLIDER_W = 94;
    private static final int SLIDER_H = 7;
    private static final int SLIDER_Y = 22;
    private static final int SLIDER_STEP = 11;
    private static final int FIELD_X = SLIDER_X + SLIDER_W + 4;
    private static final int[] SLIDER_MAX = {255, 255, 255, 360, 100, 100};
    private static final String[] SLIDER_NAMES = {"R", "G", "B", "H", "S", "V"};
    private static final int RESULTS_Y = 96;
    private static final int RESULT_COLS = 12;
    private static final int RESULT_COUNT = 24;

    private static final int GRADIENT_X = (MAIN_W - PaletteState.GRADIENT_SLOTS * SLOT) / 2;
    private static final int INPUT_Y = 12;
    private static final int UP_Y = 38;
    private static final int OUTPUT_Y = 46;
    private static final int DOWN_Y = 66;
    private static final int TARGET_Y = 75;
    private static final int BUTTONS_Y = 88;

    private enum Drag { NONE, SV, HUE, SLIDER, SCROLL }

    private enum Kind { HOTBAR, LIST, RESULT, EYEDROPPER, INPUT, OUTPUT }

    private record Hit(Kind kind, int index, ItemStack stack, int x, int y) {
    }

    private final @Nullable Screen parent;
    private final PaletteState state = PaletteState.get();

    private int left;
    private int top;
    private int panelWidth;
    private int mainX;
    private int mainY;
    private int sideX;
    private int columns;
    private int rows;
    private int hotbarX;
    private int hotbarY;

    private Button colorTab;
    private Button gradientTab;
    private final Button[] filterButtons = new Button[4];
    private Button copyButton;
    private Button reverseButton;
    private Button clearButton;
    private EditBox search;
    private EditBox hex;
    private final EditBox[] sliderFields = new EditBox[SLIDER_NAMES.length];
    private String searchText = "";
    private boolean settingFields;

    private ItemStack carried = ItemStack.EMPTY;
    private Drag drag = Drag.NONE;
    private int dragSlider;
    private int scroll;
    private double lastMouseX;
    private double lastMouseY;
    private boolean wasCreative;

    private boolean dirty = true;
    private List<PaletteIndex.Entry> similar = List.of();
    private Gradient.Slot[] gradient = new Gradient.Slot[0];
    private @Nullable String listedFor;
    private List<PaletteIndex.Entry> listed = List.of();

    public PaletteScreen(@Nullable Screen parent, boolean gradientTab) {
        super(Component.translatable("screen.builders_tool_palette.title"));
        this.parent = parent;
        state.gradientTab = gradientTab;
    }

    private static MutableComponent text(String key) {
        return Component.translatable("builders_tool_palette." + key);
    }

    @Override
    protected void init() {
        int fixed = PAD + MAIN_W + PAD + PAD + 8 + PAD;
        columns = Math.max(2, Math.min(7, (width - 8 - fixed) / SLOT));
        panelWidth = fixed - PAD + columns * SLOT;
        left = (width - panelWidth) / 2;
        top = Math.max(0, (height - HEIGHT) / 2);
        mainX = left + PAD;
        mainY = top + PAD + TAB_H + 4;
        sideX = mainX + MAIN_W + PAD;
        rows = (top + HEIGHT - PAD - mainY) / SLOT;
        hotbarX = mainX + GRADIENT_X;
        hotbarY = top + HEIGHT - PAD - SLOT;

        colorTab = addRenderableWidget(Button.builder(text("tab.color_picker"), b -> showTab(false))
            .bounds(mainX, top + PAD, 87, TAB_H).build());
        gradientTab = addRenderableWidget(Button.builder(text("tab.gradient_helper"), b -> showTab(true))
            .bounds(mainX + 89, top + PAD, 87, TAB_H).build());
        String[] filters = {"full_cube", "solid", "opaque", "same_texture"};
        for (int i = 0; i < 4; i++) {
            int index = i;
            filterButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> toggleFilter(index))
                .bounds(mainX + 178 + i * 14, top + PAD, 12, TAB_H)
                .tooltip(Tooltip.create(Component.empty().append(text("filter." + filters[i])).append("\n")
                    .append(text("filter." + filters[i] + ".description").withStyle(ChatFormatting.GRAY))))
                .build());
        }

        search = new EditBox(font, sideX, top + PAD + 2, columns * SLOT + 8 - 20, 16, text("search"));
        search.setHint(text("search.hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(searchText);
        search.setResponder(value -> {
            searchText = value;
            scroll = 0;
        });
        addRenderableWidget(search);
        addRenderableWidget(new PackColorsButton(sideX + columns * SLOT + 8 - 18, top + PAD + 2, () -> state.packColors, this::togglePackColors));

        hex = new EditBox(font, mainX + HEX_X, mainY + 1, MAIN_W - HEX_X, 16, text("hex"));
        hex.setMaxLength(7);
        hex.setResponder(this::hexTyped);
        addRenderableWidget(hex);
        for (int i = 0; i < SLIDER_NAMES.length; i++) {
            int slider = i;
            EditBox field = new EditBox(font, mainX + FIELD_X + 2, mainY + SLIDER_Y + i * SLIDER_STEP, MAIN_W - FIELD_X - 3, 9,
                Component.literal(SLIDER_NAMES[i]));
            field.setBordered(false);
            field.setMaxLength(3);
            field.setResponder(value -> sliderTyped(slider, value));
            sliderFields[i] = addRenderableWidget(field);
        }

        int buttonsX = mainX + GRADIENT_X;
        copyButton = addRenderableWidget(Button.builder(text("copy_to_hotbar"), b -> copyToHotbar())
            .bounds(buttonsX, mainY + BUTTONS_Y, 80, 20).build());
        reverseButton = addRenderableWidget(Button.builder(text("reverse"), b -> reverseGradient())
            .bounds(buttonsX + 82, mainY + BUTTONS_Y, 40, 20).build());
        clearButton = addRenderableWidget(Button.builder(text("clear"), b -> clearGradient())
            .bounds(buttonsX + 124, mainY + BUTTONS_Y, 38, 20).build());

        updateWidgets();
        updateFields();
    }

    private void updateWidgets() {
        boolean onGradient = state.gradientTab;
        colorTab.active = onGradient;
        gradientTab.active = !onGradient;
        boolean[] on = {state.fullCube, state.solid, state.opaque, state.sameTexture};
        String[] letters = {"F", "S", "O", "T"};
        for (int i = 0; i < 4; i++) {
            filterButtons[i].setMessage(Component.literal(letters[i]).withStyle(on[i] ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        hex.visible = !onGradient;
        for (EditBox field : sliderFields) {
            field.visible = !onGradient;
        }
        copyButton.visible = onGradient;
        reverseButton.visible = onGradient;
        clearButton.visible = onGradient;
        wasCreative = creative();
        copyButton.active = wasCreative;
        copyButton.setTooltip(Tooltip.create(text(wasCreative ? "copy_to_hotbar.description" : "creative_only")));
    }

    private void showTab(boolean onGradient) {
        state.gradientTab = onGradient;
        drag = Drag.NONE;
        setFocused(null);
        updateWidgets();
    }

    private void toggleFilter(int index) {
        switch (index) {
            case 0 -> state.fullCube = !state.fullCube;
            case 1 -> state.solid = !state.solid;
            case 2 -> state.opaque = !state.opaque;
            default -> state.sameTexture = !state.sameTexture;
        }
        Arrays.fill(state.choice, 0);
        dirty = true;
        updateWidgets();
    }

    private void togglePackColors() {
        int eyedropperColor = PaletteIndex.colorOf(state.eyedropper);
        state.packColors = !state.packColors;
        // A colour taken with the eyedropper follows the switch; a colour picked by hand stays.
        if (eyedropperColor >= 0 && eyedropperColor == state.color()) {
            int rgb = PaletteIndex.colorOf(state.eyedropper);
            if (rgb >= 0) {
                state.setColor(rgb);
                updateFields();
            }
        }
        Arrays.fill(state.choice, 0);
        listedFor = null;
        dirty = true;
    }

    private boolean creative() {
        return minecraft.player != null && minecraft.gameMode != null && minecraft.player.isCreative();
    }

    // Colour

    private void colorChanged() {
        dirty = true;
        updateFields();
    }

    /** Shows the current colour in the hex and number fields, except the one being typed in. */
    private void updateFields() {
        settingFields = true;
        if (hex != null && !hex.isFocused()) {
            hex.setValue(ColorMath.hex(state.color()));
        }
        for (int i = 0; i < sliderFields.length; i++) {
            if (sliderFields[i] != null && !sliderFields[i].isFocused()) {
                sliderFields[i].setValue(Integer.toString(sliderNumber(i)));
            }
        }
        settingFields = false;
    }

    private void hexTyped(String value) {
        if (settingFields) {
            return;
        }
        int rgb = ColorMath.parseHex(value);
        if (rgb >= 0 && rgb != state.color()) {
            state.setColor(rgb);
            dirty = true;
        }
    }

    private float sliderValue(int slider) {
        int rgb = state.color();
        return switch (slider) {
            case 0 -> ColorMath.red(rgb) / 255f;
            case 1 -> ColorMath.green(rgb) / 255f;
            case 2 -> ColorMath.blue(rgb) / 255f;
            case 3 -> state.hue;
            case 4 -> state.saturation;
            default -> state.value;
        };
    }

    private void setSlider(int slider, float value) {
        float v = Math.max(0f, Math.min(1f, value));
        int rgb = state.color();
        switch (slider) {
            case 0 -> state.setColor(ColorMath.rgb(Math.round(v * 255), ColorMath.green(rgb), ColorMath.blue(rgb)));
            case 1 -> state.setColor(ColorMath.rgb(ColorMath.red(rgb), Math.round(v * 255), ColorMath.blue(rgb)));
            case 2 -> state.setColor(ColorMath.rgb(ColorMath.red(rgb), ColorMath.green(rgb), Math.round(v * 255)));
            case 3 -> state.hue = Math.min(v, 0.9999f);
            case 4 -> state.saturation = v;
            default -> state.value = v;
        }
        colorChanged();
    }

    /** The colour the slider's track shows at {@code t}. */
    private int sliderColor(int slider, float t) {
        int rgb = state.color();
        int c = Math.round(t * 255);
        return switch (slider) {
            case 0 -> ColorMath.rgb(c, ColorMath.green(rgb), ColorMath.blue(rgb));
            case 1 -> ColorMath.rgb(ColorMath.red(rgb), c, ColorMath.blue(rgb));
            case 2 -> ColorMath.rgb(ColorMath.red(rgb), ColorMath.green(rgb), c);
            case 3 -> ColorMath.fromHsv(t, 1f, 1f);
            case 4 -> ColorMath.fromHsv(state.hue, t, state.value);
            default -> ColorMath.fromHsv(state.hue, state.saturation, t);
        };
    }

    /** The slider's value as typed: 0-255 for R, G and B, degrees for H, percent for S and V. */
    private int sliderNumber(int slider) {
        return Math.round(sliderValue(slider) * SLIDER_MAX[slider]);
    }

    private void sliderTyped(int slider, String value) {
        if (settingFields) {
            return;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (!digits.equals(value)) {
            settingFields = true;
            sliderFields[slider].setValue(digits);
            settingFields = false;
        }
        if (digits.isEmpty()) {
            return;
        }
        int number = Math.min(Integer.parseInt(digits), SLIDER_MAX[slider]);
        if (number != sliderNumber(slider)) {
            setSlider(slider, number / (float) SLIDER_MAX[slider]);
        }
    }

    private int sliderStepCount(int slider) {
        return slider < 3 ? 255 : slider == 3 ? 360 : 100;
    }

    // Gradient

    private void gradientChanged() {
        Arrays.fill(state.choice, 0);
        dirty = true;
    }

    private void reverseGradient() {
        ItemStack[] slots = state.gradient;
        for (int i = 0, j = slots.length - 1; i < j; i++, j--) {
            ItemStack swap = slots[i];
            slots[i] = slots[j];
            slots[j] = swap;
        }
        gradientChanged();
    }

    private void clearGradient() {
        Arrays.fill(state.gradient, ItemStack.EMPTY);
        gradientChanged();
    }

    private void copyToHotbar() {
        refresh();
        for (int i = 0; i < gradient.length && i < Inventory.getSelectionSize(); i++) {
            if (!gradient[i].stack().isEmpty()) {
                putInHotbar(i, gradient[i].stack());
            }
        }
    }

    // Hotbar (creative only)

    private void putInHotbar(int slot, ItemStack stack) {
        if (!creative() || stack.isEmpty()) {
            return;
        }
        ItemStack full = stack.copyWithCount(stack.getMaxStackSize());
        minecraft.player.getInventory().setItem(slot, full);
        minecraft.gameMode.handleCreativeModeItemAdd(full, 36 + slot);
    }

    /** Shift-click: the first empty hotbar slot, or the selected one when none is empty. */
    private void quickPut(ItemStack stack) {
        Inventory inventory = minecraft.player.getInventory();
        int slot = inventory.getSelectedSlot();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                slot = i;
                break;
            }
        }
        putInHotbar(slot, stack);
    }

    private ItemStack hotbarItem(int slot) {
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getInventory().getItem(slot);
    }

    // Results

    private void refresh() {
        if (dirty) {
            similar = PaletteIndex.nearest(ColorMath.oklab(state.color()), state.filters(), RESULT_COUNT);
            gradient = Gradient.compute(state.gradient, state.choice, state.filters());
            dirty = false;
        }
        if (!searchText.equals(listedFor)) {
            String query = searchText.trim().toLowerCase(Locale.ROOT);
            List<PaletteIndex.Entry> matches = new ArrayList<>();
            for (PaletteIndex.Entry entry : PaletteIndex.entries()) {
                if (query.isEmpty() || entry.search().contains(query)) {
                    matches.add(entry);
                }
            }
            listed = matches;
            listedFor = searchText;
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int maxScroll() {
        int totalRows = (listed.size() + columns - 1) / columns;
        return Math.max(0, totalRows - rows);
    }

    // Hit testing

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private @Nullable Hit hit(double mx, double my) {
        for (int k = 0; k < Inventory.getSelectionSize(); k++) {
            int x = hotbarX + k * SLOT;
            if (in(mx, my, x, hotbarY, SLOT, SLOT)) {
                return new Hit(Kind.HOTBAR, k, hotbarItem(k), x, hotbarY);
            }
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x = sideX + col * SLOT;
                int y = mainY + row * SLOT;
                if (in(mx, my, x, y, SLOT, SLOT)) {
                    int index = (scroll + row) * columns + col;
                    return new Hit(Kind.LIST, index, index < listed.size() ? listed.get(index).stack() : ItemStack.EMPTY, x, y);
                }
            }
        }
        if (!state.gradientTab) {
            if (in(mx, my, mainX + RIGHT_X, mainY, SLOT, SLOT)) {
                return new Hit(Kind.EYEDROPPER, 0, state.eyedropper, mainX + RIGHT_X, mainY);
            }
            for (int i = 0; i < RESULT_COUNT; i++) {
                int x = mainX + i % RESULT_COLS * SLOT;
                int y = mainY + RESULTS_Y + i / RESULT_COLS * SLOT;
                if (in(mx, my, x, y, SLOT, SLOT)) {
                    return new Hit(Kind.RESULT, i, i < similar.size() ? similar.get(i).stack() : ItemStack.EMPTY, x, y);
                }
            }
        } else {
            for (int k = 0; k < PaletteState.GRADIENT_SLOTS; k++) {
                int x = mainX + GRADIENT_X + k * SLOT;
                if (in(mx, my, x, mainY + INPUT_Y, SLOT, SLOT)) {
                    return new Hit(Kind.INPUT, k, state.gradient[k], x, mainY + INPUT_Y);
                }
                if (in(mx, my, x, mainY + OUTPUT_Y, SLOT, SLOT)) {
                    return new Hit(Kind.OUTPUT, k, k < gradient.length ? gradient[k].stack() : ItemStack.EMPTY, x, mainY + OUTPUT_Y);
                }
            }
        }
        return null;
    }

    /** Whether gradient slot k is filled in by the helper, so it has matches to step through. */
    private boolean steppable(int k) {
        return k < gradient.length && !gradient[k].input() && gradient[k].target() >= 0;
    }

    private void step(int k, int by) {
        if (!steppable(k)) {
            return;
        }
        int next = state.choice[k] + by;
        if (next < 0 || by > 0 && gradient[k].last()) {
            return;
        }
        state.choice[k] = next;
        dirty = true;
    }

    // Input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        setFocused(null);
        updateFields();
        refresh();
        double mx = event.x();
        double my = event.y();
        Hit hit = hit(mx, my);
        if (event.button() == MouseButtons.RIGHT) {
            if (!carried.isEmpty()) {
                carried = ItemStack.EMPTY;
            } else if (hit != null && hit.kind() == Kind.INPUT) {
                state.gradient[hit.index()] = ItemStack.EMPTY;
                gradientChanged();
            } else if (hit != null && hit.kind() == Kind.EYEDROPPER) {
                state.eyedropper = ItemStack.EMPTY;
            }
            return true;
        }
        if (event.button() != MouseButtons.LEFT) {
            return false;
        }
        if (!state.gradientTab) {
            if (in(mx, my, mainX, mainY, SV, SV)) {
                startDrag(Drag.SV, 0, mx, my);
                return true;
            }
            if (in(mx, my, mainX + HUE_X, mainY, HUE_W, SV)) {
                startDrag(Drag.HUE, 0, mx, my);
                return true;
            }
            for (int i = 0; i < SLIDER_NAMES.length; i++) {
                if (in(mx, my, mainX + SLIDER_X - 2, mainY + SLIDER_Y + i * SLIDER_STEP - 1, SLIDER_W + 4, SLIDER_H + 2)) {
                    startDrag(Drag.SLIDER, i, mx, my);
                    return true;
                }
            }
        } else {
            for (int k = 0; k < PaletteState.GRADIENT_SLOTS; k++) {
                int x = mainX + GRADIENT_X + k * SLOT;
                if (in(mx, my, x, mainY + UP_Y - 2, SLOT, 8)) {
                    step(k, -1);
                    return true;
                }
                if (in(mx, my, x, mainY + DOWN_Y - 1, SLOT, 8)) {
                    step(k, 1);
                    return true;
                }
            }
        }
        if (in(mx, my, scrollbarX(), mainY, 6, rows * SLOT)) {
            startDrag(Drag.SCROLL, 0, mx, my);
            return true;
        }
        if (hit == null) {
            boolean had = !carried.isEmpty();
            carried = ItemStack.EMPTY;
            return had;
        }
        clickSlot(hit, event.hasShiftDown());
        return true;
    }

    private void clickSlot(Hit hit, boolean shift) {
        switch (hit.kind()) {
            case HOTBAR -> {
                if (!carried.isEmpty()) {
                    // In survival nothing moves, so the block stays on the cursor.
                    if (creative()) {
                        putInHotbar(hit.index(), carried);
                        carried = ItemStack.EMPTY;
                    }
                } else if (!hit.stack().isEmpty()) {
                    carried = hit.stack().copyWithCount(1);
                }
            }
            case EYEDROPPER -> {
                if (!carried.isEmpty()) {
                    state.eyedropper = carried;
                    carried = ItemStack.EMPTY;
                    int rgb = PaletteIndex.colorOf(state.eyedropper);
                    if (rgb >= 0) {
                        state.setColor(rgb);
                        colorChanged();
                    }
                } else if (!state.eyedropper.isEmpty()) {
                    carried = state.eyedropper;
                    state.eyedropper = ItemStack.EMPTY;
                }
            }
            case INPUT -> {
                if (!carried.isEmpty()) {
                    state.gradient[hit.index()] = carried;
                    carried = ItemStack.EMPTY;
                    gradientChanged();
                } else if (!hit.stack().isEmpty()) {
                    carried = hit.stack();
                    state.gradient[hit.index()] = ItemStack.EMPTY;
                    gradientChanged();
                }
            }
            default -> {
                if (hit.stack().isEmpty()) {
                    carried = ItemStack.EMPTY;
                } else if (shift && creative()) {
                    quickPut(hit.stack());
                } else {
                    carried = hit.stack().copyWithCount(1);
                }
            }
        }
    }

    private void startDrag(Drag kind, int slider, double mx, double my) {
        drag = kind;
        dragSlider = slider;
        dragTo(mx, my);
    }

    private void dragTo(double mx, double my) {
        switch (drag) {
            case SV -> {
                state.saturation = clamp01((float) (mx - mainX) / (SV - 1));
                state.value = 1f - clamp01((float) (my - mainY) / (SV - 1));
                colorChanged();
            }
            case HUE -> {
                state.hue = Math.min(clamp01((float) (my - mainY) / (SV - 1)), 0.9999f);
                colorChanged();
            }
            case SLIDER -> setSlider(dragSlider, (float) (mx - mainX - SLIDER_X) / (SLIDER_W - 1));
            case SCROLL -> {
                int max = maxScroll();
                float t = clamp01((float) (my - mainY - 4) / Math.max(1, rows * SLOT - 8));
                scroll = Math.round(t * max);
            }
            default -> {
            }
        }
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (drag != Drag.NONE) {
            dragTo(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (drag != Drag.NONE) {
            drag = Drag.NONE;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return super.mouseScrolled(mx, my, scrollX, scrollY);
        }
        int direction = scrollY > 0 ? 1 : -1;
        if (in(mx, my, sideX, mainY, columns * SLOT + 8, rows * SLOT)) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - direction));
            return true;
        }
        if (state.gradientTab) {
            for (int k = 0; k < PaletteState.GRADIENT_SLOTS; k++) {
                if (in(mx, my, mainX + GRADIENT_X + k * SLOT, mainY + UP_Y - 2, SLOT, DOWN_Y + 7 - UP_Y + 2)) {
                    step(k, -direction);
                    return true;
                }
            }
        } else {
            for (int i = 0; i < SLIDER_NAMES.length; i++) {
                if (in(mx, my, mainX + RIGHT_X, mainY + SLIDER_Y + i * SLIDER_STEP - 1, MAIN_W - RIGHT_X, SLIDER_H + 2)) {
                    setSlider(i, sliderValue(i) + direction / (float) sliderStepCount(i));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (getFocused() instanceof EditBox box && box.isFocused()) {
            if (event.isEscape() || event.isConfirmation()) {
                setFocused(null);
                updateFields();
                return true;
            }
            return super.keyPressed(event);
        }
        if (creative()) {
            Hit hit = hit(lastMouseX, lastMouseY);
            ItemStack stack = !carried.isEmpty() ? carried : hit != null && hit.kind() != Kind.HOTBAR ? hit.stack() : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                    if (minecraft.options.keyHotbarSlots[i].matches(event)) {
                        putInHotbar(i, stack);
                        return true;
                    }
                }
            }
        }
        if (minecraft.options.keyInventory.matches(event) || ToolPalette.isOpenKey(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        lastMouseX = mx;
        lastMouseY = my;
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (parent != null) {
            Screens.set(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public void removed() {
        state.save();
        super.removed();
    }

    // Drawing

    private int scrollbarX() {
        return sideX + columns * SLOT + 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        refresh();
        if (creative() != wasCreative) {
            updateWidgets();
        }
        Draw.panel(g, left, top, left + panelWidth, top + HEIGHT);
        if (state.gradientTab) {
            drawGradientTab(g);
        } else {
            drawColorTab(g);
        }
        drawSide(g);
        drawHotbar(g);

        super.extractRenderState(g, mouseX, mouseY, partialTick);

        Hit hit = hit(mouseX, mouseY);
        drawItems(g, hit);
        if (!carried.isEmpty()) {
            g.nextStratum();
            g.item(carried, mouseX - 8, mouseY - 8);
            if (hit != null && hit.kind() == Kind.HOTBAR && !creative()) {
                g.setTooltipForNextFrame(font, text("survival_note"), mouseX, mouseY);
            }
        } else {
            tooltips(g, hit, mouseX, mouseY);
        }
    }

    private void drawColorTab(GuiGraphicsExtractor g) {
        int x = mainX;
        int y = mainY;
        for (int i = 0; i < SV; i++) {
            int topColor = 0xFF000000 | ColorMath.fromHsv(state.hue, i / (float) (SV - 1), 1f);
            g.fillGradient(x + i, y, x + i + 1, y + SV, topColor, 0xFF000000);
        }
        g.outline(x - 1, y - 1, SV + 2, SV + 2, 0xFF373737);
        int markX = x + Math.round(state.saturation * (SV - 1));
        int markY = y + Math.round((1f - state.value) * (SV - 1));
        g.outline(markX - 2, markY - 2, 5, 5, state.value > 0.5f ? 0xFF000000 : 0xFFFFFFFF);

        int hx = x + HUE_X;
        for (int i = 0; i < SV; i++) {
            g.fill(hx, y + i, hx + HUE_W, y + i + 1, 0xFF000000 | ColorMath.fromHsv(i / (float) (SV - 1), 1f, 1f));
        }
        g.outline(hx - 1, y - 1, HUE_W + 2, SV + 2, 0xFF373737);
        int hueY = y + Math.round(state.hue * (SV - 1));
        g.fill(hx - 2, hueY - 1, hx + HUE_W + 2, hueY + 2, 0xFF000000);
        g.fill(hx - 1, hueY, hx + HUE_W + 1, hueY + 1, 0xFFFFFFFF);

        Draw.slot(g, x + RIGHT_X, y);
        Draw.inset(g, x + SWATCH_X, y, x + SWATCH_X + SWATCH_W, y + SLOT, 0xFF000000 | state.color());

        for (int i = 0; i < SLIDER_NAMES.length; i++) {
            int sy = y + SLIDER_Y + i * SLIDER_STEP;
            g.text(font, SLIDER_NAMES[i], x + RIGHT_X, sy, Draw.TEXT, false);
            int sx = x + SLIDER_X;
            for (int p = 0; p < SLIDER_W; p += 2) {
                g.fill(sx + p, sy, sx + Math.min(p + 2, SLIDER_W), sy + SLIDER_H, 0xFF000000 | sliderColor(i, p / (float) (SLIDER_W - 1)));
            }
            g.outline(sx - 1, sy - 1, SLIDER_W + 2, SLIDER_H + 2, 0xFF373737);
            int thumb = sx + Math.round(sliderValue(i) * (SLIDER_W - 1));
            g.fill(thumb - 1, sy - 2, thumb + 2, sy + SLIDER_H + 2, 0xFF000000);
            g.fill(thumb, sy - 1, thumb + 1, sy + SLIDER_H + 1, 0xFFFFFFFF);
            // Background of the number field, which draws only its text.
            int fx = x + FIELD_X;
            g.fill(fx, sy - 2, x + MAIN_W, sy + 9, sliderFields[i].isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
            g.fill(fx + 1, sy - 1, x + MAIN_W - 1, sy + 8, 0xFF000000);
        }

        g.text(font, text("similar"), x, y + RESULTS_Y - 10, Draw.TEXT, false);
        for (int i = 0; i < RESULT_COUNT; i++) {
            Draw.slot(g, x + i % RESULT_COLS * SLOT, y + RESULTS_Y + i / RESULT_COLS * SLOT);
        }
    }

    private void drawGradientTab(GuiGraphicsExtractor g) {
        int x0 = mainX + GRADIENT_X;
        int y = mainY;
        g.text(font, text("gradient.hint"), x0, y + 1, Draw.TEXT, false);
        boolean any = false;
        for (int k = 0; k < PaletteState.GRADIENT_SLOTS; k++) {
            int x = x0 + k * SLOT;
            Draw.slot(g, x, y + INPUT_Y);
            Gradient.Slot slot = k < gradient.length ? gradient[k] : null;
            boolean filled = slot != null && !slot.stack().isEmpty();
            if (slot != null && slot.input()) {
                Draw.inset(g, x, y + OUTPUT_Y, x + SLOT, y + OUTPUT_Y + SLOT, 0xFF6F6F6F);
            } else {
                Draw.slot(g, x, y + OUTPUT_Y);
            }
            if (steppable(k)) {
                any = true;
                Draw.arrow(g, x + 7, y + UP_Y, true, state.choice[k] > 0 ? 0xFF202020 : 0xFF9A9A9A);
                Draw.arrow(g, x + 7, y + DOWN_Y, false, !slot.last() ? 0xFF202020 : 0xFF9A9A9A);
            }
            if (slot != null && slot.target() >= 0) {
                g.fill(x + 1, y + TARGET_Y, x + SLOT - 1, y + TARGET_Y + 6, 0xFF000000 | slot.target());
            } else if (filled) {
                g.fill(x + 1, y + TARGET_Y, x + SLOT - 1, y + TARGET_Y + 6, 0xFF8B8B8B);
            }
        }
        if (any) {
            g.text(font, text("gradient.cycle"), x0, y + BUTTONS_Y + 26, Draw.TEXT_LIGHT, false);
        }
    }

    private void drawSide(GuiGraphicsExtractor g) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                Draw.slot(g, sideX + col * SLOT, mainY + row * SLOT);
            }
        }
        int sx = scrollbarX();
        int trackHeight = rows * SLOT;
        Draw.inset(g, sx, mainY, sx + 6, mainY + trackHeight, 0xFF8B8B8B);
        int max = maxScroll();
        int thumbHeight = max == 0 ? trackHeight - 2 : Math.max(8, (trackHeight - 2) * rows / (rows + max));
        int thumbY = mainY + 1 + (max == 0 ? 0 : Math.round((trackHeight - 2 - thumbHeight) * scroll / (float) max));
        Draw.button(g, sx + 1, thumbY, sx + 5, thumbY + thumbHeight, drag == Drag.SCROLL);
    }

    private void drawHotbar(GuiGraphicsExtractor g) {
        g.text(font, text("hotbar"), mainX, hotbarY + 5, Draw.TEXT, false);
        for (int k = 0; k < Inventory.getSelectionSize(); k++) {
            Draw.slot(g, hotbarX + k * SLOT, hotbarY);
        }
    }

    private void drawItems(GuiGraphicsExtractor g, @Nullable Hit hover) {
        if (hover != null) {
            Draw.hoverHighlight(g, hover.x(), hover.y());
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = (scroll + row) * columns + col;
                if (index < listed.size()) {
                    g.item(listed.get(index).stack(), sideX + col * SLOT + 1, mainY + row * SLOT + 1);
                }
            }
        }
        for (int k = 0; k < Inventory.getSelectionSize(); k++) {
            ItemStack stack = hotbarItem(k);
            int x = hotbarX + k * SLOT + 1;
            g.item(stack, x, hotbarY + 1);
            g.itemDecorations(font, stack, x, hotbarY + 1);
        }
        if (state.gradientTab) {
            for (int k = 0; k < PaletteState.GRADIENT_SLOTS; k++) {
                int x = mainX + GRADIENT_X + k * SLOT + 1;
                g.item(state.gradient[k], x, mainY + INPUT_Y + 1);
                if (k < gradient.length) {
                    g.item(gradient[k].stack(), x, mainY + OUTPUT_Y + 1);
                }
            }
        } else {
            g.item(state.eyedropper, mainX + RIGHT_X + 1, mainY + 1);
            for (int i = 0; i < similar.size(); i++) {
                g.item(similar.get(i).stack(), mainX + i % RESULT_COLS * SLOT + 1, mainY + RESULTS_Y + i / RESULT_COLS * SLOT + 1);
            }
        }
    }

    private void tooltips(GuiGraphicsExtractor g, @Nullable Hit hit, int mouseX, int mouseY) {
        if (hit == null) {
            return;
        }
        if (hit.stack().isEmpty()) {
            if (hit.kind() == Kind.EYEDROPPER) {
                g.setComponentTooltipForNextFrame(font, List.of(text("eyedropper"), text("eyedropper.description").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            } else if (hit.kind() == Kind.INPUT) {
                g.setTooltipForNextFrame(font, text("gradient.input"), mouseX, mouseY);
            }
            return;
        }
        List<Component> lines = new ArrayList<>(getTooltipFromItem(minecraft, hit.stack()));
        int rgb = PaletteIndex.colorOf(hit.stack());
        if (rgb >= 0) {
            lines.add(Component.literal(ColorMath.hex(rgb)).withStyle(style -> style.withColor(rgb)));
        }
        if (hit.kind() == Kind.OUTPUT && steppable(hit.index())) {
            lines.add(Component.translatable("builders_tool_palette.gradient.match", state.choice[hit.index()] + 1).withStyle(ChatFormatting.GRAY));
        }
        if (!creative() && hit.kind() != Kind.HOTBAR && hit.kind() != Kind.EYEDROPPER && hit.kind() != Kind.INPUT) {
            lines.add(text("survival_note").withStyle(ChatFormatting.DARK_GRAY));
        }
        g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }
}
