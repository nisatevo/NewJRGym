package gym.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;

public final class ImageStore {
    private static final Path DIR=Paths.get("data","photos");
    private ImageStore(){}
    public static Path directory(){try{Files.createDirectories(DIR);return DIR;}catch(IOException e){throw new RuntimeException("Could not create photo directory.",e);}}
    public static String copy(Path source,String prefix,int id) throws IOException{
        if(source==null||!Files.isRegularFile(source))throw new IOException("Selected image was not found.");
        if(Files.size(source)>5*1024*1024)throw new IOException("Image must be 5 MB or smaller.");
        BufferedImage img=ImageIO.read(source.toFile()); if(img==null)throw new IOException("Please select a valid JPG, PNG or GIF image.");
        String name=source.getFileName().toString().toLowerCase(); String ext=name.endsWith(".png")?".png":name.endsWith(".gif")?".gif":".jpg";
        Path target=directory().resolve(prefix+"_"+id+ext); Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING); return target.toString();
    }
}
