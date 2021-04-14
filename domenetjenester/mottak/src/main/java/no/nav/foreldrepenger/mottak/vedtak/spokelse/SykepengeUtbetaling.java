package no.nav.foreldrepenger.mottak.vedtak.spokelse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykepengeUtbetaling {

    @JsonProperty("fom")
    private LocalDate fom;
    @JsonProperty("tom")
    private LocalDate tom;
    @JsonProperty("grad")
    private BigDecimal grad;


    public SykepengeUtbetaling(@JsonProperty("fom") LocalDate fom,
                               @JsonProperty("tom") LocalDate tom,
                               @JsonProperty("grad") BigDecimal grad) {
        this.fom = fom;
        this.tom = tom;
        if (grad != null) {
            this.grad = grad.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public BigDecimal getGrad() {
        return grad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (SykepengeUtbetaling) o;
        return Objects.equals(fom, that.fom) &&
                Objects.equals(tom, that.tom) &&
                Objects.equals(grad, that.grad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fom, tom, grad);
    }

    @Override
    public String toString() {
        return "SykepengeUtbetaling{" +
                "fom=" + fom +
                ", tom=" + tom +
                ", grad=" + grad +
                '}';
    }
}
