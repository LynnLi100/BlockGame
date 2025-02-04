package game;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;


// main class
public class Texture {

    static class TextureRegion {
        String name;
        int x, y, width, height;
        boolean rotated, trimmed;
        int[] sourceSize, spriteSource;

        public TextureRegion(String name, int x, int y, int width, int height,
                boolean rotated, boolean trimmed,
                int[] sourceSize, int[] spriteSource) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.rotated = rotated;
            this.trimmed = trimmed;
            this.sourceSize = sourceSize;
            this.spriteSource = spriteSource;
        }
    }

    static class TextureAtlas {
        BufferedImage image;
        Map<String, TextureRegion> regions = new HashMap<>();

        public TextureAtlas(String imagePath) throws Exception {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                System.err.println("Error: File not found -> " + imageFile.getAbsolutePath());
                return;
            }

            this.image = ImageIO.read(imageFile);
        }

        public void addRegion(TextureRegion region) {
            regions.put(region.name, region);
        }

        public BufferedImage getSubImage(String name) {
            TextureRegion region = regions.get(name);
            if (region == null)
                return null;

            // 裁剪基础区域
            BufferedImage sub = image.getSubimage(
                    region.x, region.y, region.width, region.height);

            // 旋转处理
            if (region.rotated) {
                sub = rotate90(sub);
            }

            // 修剪处理（示例逻辑）
            if (region.trimmed && region.sourceSize != null) {
                BufferedImage canvas = new BufferedImage(
                        region.sourceSize[0],
                        region.sourceSize[1],
                        BufferedImage.TYPE_INT_ARGB);
                canvas.getGraphics().drawImage(
                        sub,
                        region.spriteSource[0],
                        region.spriteSource[1],
                        null);
                sub = canvas;
            }
            return sub;
        }

        private BufferedImage rotate90(BufferedImage img) {
            int w = img.getWidth(), h = img.getHeight();
            BufferedImage rotated = new BufferedImage(h, w, img.getType());
            Graphics2D g2d = rotated.createGraphics();

            AffineTransform transform = new AffineTransform();
            transform.translate(h / 2.0, h / 2.0);
            transform.rotate(Math.toRadians(90));
            transform.translate(-w / 2.0, -h / 2.0);

            g2d.setTransform(transform);
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();

            return rotated;
        }


    }

    // JSON 解析器
    static class JSONParser {
        public TextureAtlas parse(String atlasFile, String imagePath) throws Exception {
            TextureAtlas atlas = new TextureAtlas(imagePath);
            JsonObject data = JsonParser.parseReader(new FileReader(atlasFile)).getAsJsonObject();
            JsonObject frames = data.getAsJsonObject("frames");

            for (Map.Entry<String, com.google.gson.JsonElement> entry : frames.entrySet()) {
                String name = entry.getKey();
                JsonObject info = entry.getValue().getAsJsonObject();
                JsonObject frame = info.getAsJsonObject("frame");
                int x = frame.get("x").getAsInt();
                int y = frame.get("y").getAsInt();
                int w = frame.get("w").getAsInt();
                int h = frame.get("h").getAsInt();
                boolean rotated = info.get("rotated").getAsBoolean();
                boolean trimmed = info.get("trimmed").getAsBoolean();
                JsonObject sourceSize = info.getAsJsonObject("sourceSize");
                JsonObject spriteSource = info.has("spriteSourceSize") && !info.get("spriteSourceSize").isJsonNull()
                        ? info.getAsJsonObject("spriteSourceSize")
                        : null;

                int[] spriteSourceValues = spriteSource != null
                        ? new int[]{
                        spriteSource.get("x").getAsInt(),
                        spriteSource.get("y").getAsInt(),
                        spriteSource.get("w").getAsInt(),
                        spriteSource.get("h").getAsInt()
                }
                        : new int[]{0, 0, w, h};  // Default values


                TextureRegion region = new TextureRegion(
                        name, x, y, w, h, rotated, trimmed,
                        new int[] { sourceSize.get("w").getAsInt(), sourceSize.get("h").getAsInt() },
                        new int[] {
                                spriteSource.get("x").getAsInt(),
                                spriteSource.get("y").getAsInt(),
                                spriteSource.get("w").getAsInt(),
                                spriteSource.get("h").getAsInt()
                        });
                atlas.addRegion(region);
            }
            return atlas;
        }
    }

    // 主方法
    public static void main(String[] args) {
        System.out.println("Current working directory: " + System.getProperty("user.dir"));

        try {
            JSONParser parser = new JSONParser();
            TextureAtlas atlas = parser.parse("atlas.json", "atlas.png");

            // 提取并保存子图（例如提取 "hero"）
            BufferedImage subImage = atlas.getSubImage("hero");
            Path outputPath = Path.of("hero.png");
            ImageIO.write(subImage, "PNG", outputPath.toFile());
            System.out.println("子图已保存至: " + outputPath.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
