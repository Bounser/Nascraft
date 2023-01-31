package me.bounser.nascraft.tools;

import me.bounser.nascraft.Nascraft;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageManager {

    public static ImageManager instance;

    public static ImageManager getInstance() {
        return instance == null ? instance = new ImageManager() : instance;
    }

    public BufferedImage getImage(String mat, int width, int height) {

        BufferedImage image = null;
        try {
            InputStream input = Nascraft.getInstance().getResource("Images1_19/" + mat + ".png");
            assert input != null;
            image = ImageIO.read(input);
        } catch (IOException e) {
            Nascraft.getInstance().getLogger().info("Unable to read image: " + mat + ".png");
            e.printStackTrace();
        }

        AffineTransform at = new AffineTransform();
        at.scale(width / (double) image.getWidth(), height / (double) image.getHeight());

        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage scaledImage = scaleOp.filter(image, new BufferedImage(width, height, image.getType()));
        return scaledImage;
    }

}
