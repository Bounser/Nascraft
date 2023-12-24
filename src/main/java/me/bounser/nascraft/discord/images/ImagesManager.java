package me.bounser.nascraft.discord.images;

import me.bounser.nascraft.Nascraft;
import org.bukkit.Material;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImagesManager {

    private static ImagesManager instance;

    public static ImagesManager getInstance() { return instance == null ? instance = new ImagesManager() : instance; }


    public BufferedImage getImage(Material material) {

        BufferedImage image = null;
        try {
            InputStream input = Nascraft.getInstance().getResource("1-20-1-materials/" + material.toString().toLowerCase() + ".png");
            assert input != null;
            image = ImageIO.read(input);
        } catch (IOException e) {
            Nascraft.getInstance().getLogger().info("Unable to read image: " + material.toString().toLowerCase() + ".png");
            e.printStackTrace();
        }

        return image;
    }


}
