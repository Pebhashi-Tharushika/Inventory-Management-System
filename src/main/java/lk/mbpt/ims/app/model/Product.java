package lk.mbpt.ims.app.model;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product implements Serializable {
    private Integer code;
    private String description;
    private BigDecimal qty;
    private BigDecimal sellingPrice;
    private Image preview;

    public String getDisplayProductCode(){
        return String.format("P-%08d", code);
    }

    public ImageView getProductPreview(){
        ImageView imageView = new ImageView(preview);
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        return imageView;
    }

    public byte[] getPictureBytes(){
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(preview, null);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "png", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDisplayQty(){ return new DecimalFormat("#,#00").format(this.getQty());}

    public String getDisplaySellingPrice(){ return new DecimalFormat("#,#00.00").format(this.getSellingPrice());}
}
