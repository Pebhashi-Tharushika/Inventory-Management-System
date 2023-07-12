package lk.mbpt.ims.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Item implements Serializable {
    private Integer code;
    private String description;
    private BigDecimal qty;
    private String uom;
    private BigDecimal alertQty;

    public String getDisplayItemCode(){
        return String.format("I-%08d", code);
    }

    public String getDisplayQty(){ return new DecimalFormat("#,#00.00").format(this.getQty());}

    public String getDisplayAlertQty(){ return new DecimalFormat("#,#00.00").format(this.getAlertQty());}
}
