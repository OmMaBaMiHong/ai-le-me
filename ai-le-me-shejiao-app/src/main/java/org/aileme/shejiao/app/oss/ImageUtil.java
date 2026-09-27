package org.aileme.shejiao.app.oss;

import lombok.SneakyThrows;
import org.aileme.shejiao.app.oss.factory.OSSFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.*;
import java.net.URL;

/**
 * 图片模糊处理工具类
 */
public class ImageUtil {

    @SneakyThrows
    public static String mohu(String url){
        int radius = 44;
        int size = radius * 2 + 1;
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];

        for (int i = 0; i < data.length; i++) {
            data[i] = weight;
        }

        Kernel kernel = new Kernel(size, size, data);
        ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage image= ImageIO.read(new URL(url));
        image=convolveOp.filter(image,null);

        InputStream inputStream=bufferedImageToInputStream(image);
        return OSSFactory.build().uploadSuffix(inputStream, "post");
    }

    /**
     * 将BufferedImage转换为InputStream
     * @param image
     * @return
     */
    public static InputStream bufferedImageToInputStream(BufferedImage image) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        InputStream input = new ByteArrayInputStream(os.toByteArray());
        return input;
    }
}
