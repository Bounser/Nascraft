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

    private final HashMap<Material, BufferedImage> images = new HashMap<>();

    private static Images instance;

    public static Images getInstance() { return instance == null ? instance = new Images() : instance; }


    public BufferedImage getImage(Item item, int width, int height, boolean dithering) {
        return ResourceManager.getInstance().processImage(item.getIcon(), width, height, dithering);
    }

    public BufferedImage getImage(Material material, int width, int height, boolean dithering) {

        if(images.containsKey(material)) { return ResourceManager.getInstance().processImage(images.get(material), width, height, dithering); }

        BufferedImage image = null;
        try {
            InputStream input = Nascraft.getInstance().getResource("1-20-1-materials/" + material.toString().toLowerCase() + ".png");
            assert input != null;
            image = ImageIO.read(input);
        } catch (IOException e) {
            Nascraft.getInstance().getLogger().info("Unable to read image: " + material.toString().toLowerCase() + ".png");
            e.printStackTrace();
        }
        images.put(material, image);

        return ResourceManager.getInstance().processImage(image, width, height, dithering);
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
