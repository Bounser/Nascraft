package me.bounser.nascraft.advancedgui.components;

import me.bounser.nascraft.advancedgui.InteractionsManager;
import me.bounser.nascraft.chart.price.AdvancedGUIChart;
import me.bounser.nascraft.chart.price.ChartType;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.plot.GraphData;
import me.leoko.advancedgui.manager.ResourceManager;
import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.TextComponent;
import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.entity.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GraphComponent extends RectangularComponent {

    private GraphData graphData;

    public int width, height, yc, xc;

    private TextComponent textslide;

    private TextComponent perslide;
    private ImageComponent up;
    private ImageComponent down;

    public GraphComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height) {
        super(id, clickAction, hidden, interaction, x, y, width, height);

        this.width = width-1;
        this.height = height;
        this.xc = x;
        this.yc = y;

        if (interaction.getComponentTree() == null) return;

        this.textslide = interaction.getComponentTree().locate("textslide1", TextComponent.class);
        this.perslide = interaction.getComponentTree().locate("perslide1", TextComponent.class);
        this.up = interaction.getComponentTree().locate("upgreen", ImageComponent.class);
        this.down = interaction.getComponentTree().locate("downred", ImageComponent.class);
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

        Item item = InteractionsManager.getInstance().getItemFromPlayer(player);

        BufferedImage graphImage = AdvancedGUIChart.getImage(item, ChartType.DAY, player.getUniqueId());

        BufferedImage result = mergeImages(graphImage, ResourceManager.getInstance().processImage(extractGraphImage(graphImage), 390, 140, true));

        graphic.drawImage(result, 0, 53, null);
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {

        Item item = InteractionsManager.getInstance().getItemFromPlayer(player);

        if (item == null) return "0";

        return String.valueOf(item.getPrice().getValue());
    }

    @Override
    public Component clone(Interaction interaction) {
        return new GraphComponent(id, clickAction, hidden, interaction, x, y, width, height);
    }

    public GraphData getGraphData() { return graphData; }

    public static BufferedImage extractGraphImage(BufferedImage sourceImage) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();

        BufferedImage maskedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = sourceImage.getRGB(x, y);
                Color color = new Color(rgb, true);

                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();

                if (isGraphShade(red, green, blue)) {
                    maskedImage.setRGB(x, y, rgb);
                } else {
                    maskedImage.setRGB(x, y, 0);
                }
            }
        }
        return maskedImage;
    }

    private static boolean isGraphShade(int red, int green, int blue) {
        return red > 30 && green < 40 && blue < 40 || green > 30 && red < 80 && blue < 80;
    }

    public static BufferedImage mergeImages(BufferedImage img1, BufferedImage img2) {
        int width = img1.getWidth();
        int height = img1.getHeight();

        BufferedImage mergedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = mergedImage.createGraphics();

        g2d.drawImage(img1, 0, 0, null);
        g2d.drawImage(img2, 0, 0, null);

        g2d.dispose();

        return mergedImage;
    }

}
