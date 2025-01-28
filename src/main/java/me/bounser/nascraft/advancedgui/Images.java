package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.unit.Item;
import me.leoko.advancedgui.manager.ResourceManager;
import org.bukkit.Material;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class Images {

    private static final HashMap<Material, BufferedImage> images = new HashMap<>();

    private static Images instance;

    public static Images getInstance() { return instance == null ? instance = new Images() : instance; }


    public static BufferedImage getProcessedImage(Item item, int width, int height, boolean dithering) {

        BufferedImage icon = item.getIcon();

        if (icon == null) {
            Nascraft.getInstance().getLogger().warning("Default icon of item " + item.getIdentifier() + " not found.");
            return ResourceManager.getInstance().processImage(getImage(Material.STRUCTURE_VOID), width, height, dithering);
        }

        return ResourceManager.getInstance().processImage(item.getIcon(), width, height, dithering);
    }

    public static BufferedImage getImage(Material material) {

        if(images.containsKey(material)) { return images.get(material); }

        BufferedImage image = null;
        try {
            InputStream input = Nascraft.getInstance().getResource("1-21-4-materials/minecraft_" + material.toString().toLowerCase() + ".png");
            assert input != null;
            image = ImageIO.read(input);
        } catch (IOException e) {
            Nascraft.getInstance().getLogger().info("Unable to read image: " + material.toString().toLowerCase() + ".png");
            e.printStackTrace();
        }
        images.put(material, image);

        return image;
    }

    public static boolean areEqual(BufferedImage img1, BufferedImage img2) {

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }

        for (int y = 0; y < img1.getHeight(); y++)
            for (int x = 0; x < img1.getWidth(); x++)
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) return false;

        return true;
    }

}
