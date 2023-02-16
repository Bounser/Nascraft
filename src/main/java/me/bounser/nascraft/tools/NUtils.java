package me.bounser.nascraft.tools;

import me.bounser.nascraft.Nascraft;
import me.leoko.advancedgui.manager.ResourceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class NUtils {

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

        BufferedImage image = null;
        try {
            InputStream input = Nascraft.getInstance().getResource("Images1_19/" + mat + ".png");
            assert input != null;
            image = ImageIO.read(input);
        } catch (IOException e) {
            Nascraft.getInstance().getLogger().info("Unable to read image: " + mat + ".png");
            e.printStackTrace();
        }
        return ResourceManager.getInstance().processImage(image, width, height, dithering);
    }

}
