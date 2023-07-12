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
public class SaleOrder implements Serializable {
    private Integer orderId;
    private Integer productCode;
    private Integer customerId;
    private Integer qty;
    private BigDecimal unitPrice;
    private LocalDate orderDate;
    private Status status;

    public String getDisplayOrderId(){
        return String.format("SO-%08d", orderId);
    }

    public String getDisplayCustomerId() {return String.format("C-%08d", customerId);}

    public String getDisplayProductCode() {return String.format("P-%08d", productCode);}

    public BigDecimal getTotal() {
        return new BigDecimal(qty).multiply(unitPrice).setScale(2);
    }

    public String getDisplayQty(){ return new DecimalFormat("#,#00").format(this.getQty());}

    public String getDisplayTotal(){ return new DecimalFormat("#,#00.00").format(this.getTotal());}

    public String getDisplayUnitPrice(){ return new DecimalFormat("#,#00.00").format(this.getUnitPrice());}
}

