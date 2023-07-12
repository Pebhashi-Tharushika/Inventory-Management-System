package lk.mbpt.ims.app.model;

import lk.mbpt.ims.app.util.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PurchaseOrder implements Serializable {
    private Integer orderId;
    private Integer itemCode;
    private Integer supplierId;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private LocalDate orderDate;
    private String uom;
    private Status status;


    public String getDisplayOrderId() {
        return String.format("PO-%08d", orderId);
    }

    public String getDisplaySupplierId() {
        return String.format("S-%08d", supplierId);
    }

    public String getDisplayItemCode() {
        return String.format("I-%08d", itemCode);
    }

    public BigDecimal getTotal() {
        return qty.multiply(unitPrice).setScale(2);
    }

    public String getDisplayQty() {
        return new DecimalFormat("#,#00.00").format(this.getQty());
    }

    public String getDisplayTotal(){ return new DecimalFormat("#,#00.00").format(this.getTotal());}

    public String getDisplayUnitPrice(){ return new DecimalFormat("#,#00.00").format(this.getUnitPrice());}

}

