package jp.apple.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import jp.apple.ARTPECore;
import jp.apple.network.PacketFinishEditing;
import jp.apple.network.PacketTileUpdate;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class GuiTrainPlacer extends GuiContainer {
    private final ContainerTrainPlacer container;

    private GuiTextField nameField;
    private String currentName = "車両";

    private static final int MAX_COLS = 5;
    private static final int MAX_SLOTS = 20;
    private static final int SLOT_W = 50;
    private static final int SLOT_H = 20;
    private static final int MARGIN = 4;
    private static final int GUI_W = 15 + MAX_COLS * (SLOT_W + MARGIN) - MARGIN + 15;
    private static final int GUI_H = 25 + 4 * (SLOT_H + MARGIN) - MARGIN + 10 + 20 + 10;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("artpe", "textures/gui/train_placer.png");

    public GuiTrainPlacer(ContainerTrainPlacer inventorySlotsIn) {
        super(inventorySlotsIn);
        this.container = inventorySlotsIn;
        this.xSize = GUI_W;
        this.ySize = GUI_H;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int fieldWidth = 160;
        int bottomY = this.guiTop + this.ySize - 30;


        this.nameField = new GuiTextField(
                this.fontRendererObj,
                this.guiLeft + 15, bottomY,
                fieldWidth, 20);
        this.nameField.setMaxStringLength(32);
        this.nameField.setText(currentName);
        this.nameField.setFocused(false);
        this.nameField.setCanLoseFocus(true);

        this.refreshButtons();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }


    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (this.nameField != null && this.nameField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.nameField.setFocused(false);
            } else {
                this.nameField.textboxKeyTyped(typedChar, keyCode);
                this.currentName = this.nameField.getText();
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.nameField != null) {
            this.nameField.mouseClicked(mouseX, mouseY, mouseButton);
        }


        if (mouseButton == 1) {
            for (Object obj : this.buttonList) {
                GuiButton button = (GuiButton) obj;
                if (button.mousePressed(this.mc, mouseX, mouseY) && button.id < 100) {
                    this.container.tile.toggleDirection(button.id);
                    ARTPECore.network.sendToServer(
                            new PacketTileUpdate(this.container.tile, 1, button.id));

                    button.func_146113_a(this.mc.getSoundHandler());
                    this.refreshButtons();
                    return;
                }
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.nameField != null) this.nameField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 100) {

            if (this.container.tile.trainModels.size() < MAX_SLOTS) {
                this.container.tile.addEmptySlot();
                ARTPECore.network.sendToServer(
                        new PacketTileUpdate(this.container.tile, 0, 0));
                this.refreshButtons();
            }
        } else if (button.id == 200) {

            String exportName = (this.nameField != null) ? this.nameField.getText().trim() : "車両";
            if (exportName.isEmpty()) exportName = "車両";
            ARTPECore.network.sendToServer(new PacketFinishEditing(
                    this.container.tile.trainModels,
                    this.container.tile.trainDirs,
                    exportName));
            this.mc.displayGuiScreen(null);
        } else if (button.id < 100) {

            this.container.tile.editingIndex = button.id;
            ARTPECore.network.sendToServer(
                    new PacketTileUpdate(this.container.tile, 2, button.id));
            this.mc.displayGuiScreen(
                    new GuiSelectModelFilter(this.mc.theWorld, this.container.tile));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawDefaultBackground();
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xCC000000);
        this.fontRendererObj.drawString("編成エディタ", guiLeft + 10, guiTop + 8, 0xFFFFFF);

        int count = this.container.tile.trainModels.size();
        this.fontRendererObj.drawString(count + " / " + MAX_SLOTS + " 両",
                guiLeft + xSize - 60, guiTop + 8, 0xAAAAAA);

        if (this.nameField != null) this.nameField.drawTextBox();
    }

    private void refreshButtons() {
        this.buttonList.clear();
        int listSize = this.container.tile.trainModels.size();

        for (int i = 0; i < listSize; i++) {
            int col = i % MAX_COLS;
            int row = i / MAX_COLS;
            int x = this.guiLeft + 15 + col * (SLOT_W + MARGIN);
            int y = this.guiTop + 25 + row * (SLOT_H + MARGIN);

            String modelName = this.container.tile.trainModels.get(i);
            if (modelName == null || modelName.isEmpty()) modelName = "未選択";
            String dirLabel = (this.container.tile.trainDirs.get(i) == 0) ? "▶" : "◀";
            this.buttonList.add(new GuiButton(i, x, y, SLOT_W, SLOT_H,
                    (i + 1) + ":" + dirLabel + truncate(modelName, 6)));
        }


        if (listSize < MAX_SLOTS) {
            int col = listSize % MAX_COLS;
            int row = listSize / MAX_COLS;
            int px = this.guiLeft + 15 + col * (SLOT_W + MARGIN);
            int py = this.guiTop + 25 + row * (SLOT_H + MARGIN);
            this.buttonList.add(new GuiButton(100, px, py, SLOT_W, SLOT_H, "+"));
        }


        if (this.nameField != null) {
            int btnX = this.nameField.xPosition + this.nameField.width + 5;
            int btnW = this.guiLeft + this.xSize - 15 - btnX;
            this.buttonList.add(new GuiButton(200, btnX, this.nameField.yPosition, btnW, 20, "出力"));
        }
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "…";
    }
}