package me.bounser.nascraft.tools;

import me.bounser.nascraft.Nascraft;
import me.leoko.advancedgui.manager.ResourceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

public class NUtils {

    public static HashMap<String, BufferedImage> images = new HashMap<>();

    public static float round(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(Config.getInstance().getDecimalPrecission(), RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float roundToOne(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static BufferedImage getImage(String mat, int width, int height, boolean dithering) {

        if(images.containsKey(mat)) { return ResourceManager.getInstance().processImage(images.get(mat), width, height, dithering); }

        BufferedImage image = null;
        try {
            InputStream input = Nascraft.getInstance().getResource("Images1_19/" + mat + ".png");
            assert input != null;
            image = ImageIO.read(input);
        } catch (IOException e) {
            Nascraft.getInstance().getLogger().info("Unable to read image: " + mat + ".png");
            e.printStackTrace();
        }
        images.put(mat, image);

        return ResourceManager.getInstance().processImage(image, width, height, dithering);
    }

    public static boolean areEqual(BufferedImage img1, BufferedImage img2) {

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }
        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

}
