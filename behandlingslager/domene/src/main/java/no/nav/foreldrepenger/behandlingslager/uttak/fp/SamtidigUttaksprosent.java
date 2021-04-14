package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@Embeddable
@JsonSerialize(using = SamtidigUttaksprosent.MySamtidigUttaksprosentSerializer.class)
public class SamtidigUttaksprosent implements Comparable<SamtidigUttaksprosent> {

    public static final SamtidigUttaksprosent ZERO = new SamtidigUttaksprosent(0);
    public static final SamtidigUttaksprosent TEN = new SamtidigUttaksprosent(10);

    @Column(name = "samtidig_uttaksprosent")
    @Min(0)
    @Max(100)
    @Digits(integer = 3, fraction = 2)
    private BigDecimal verdi;

    @JsonCreator
    public SamtidigUttaksprosent(BigDecimal verdi) {
        this.verdi = scale(verdi);
    }

    public SamtidigUttaksprosent(double verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    public SamtidigUttaksprosent(int verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    SamtidigUttaksprosent() {

    }

    public BigDecimal decimalValue() {
        return scale(verdi);
    }

    private BigDecimal scale(BigDecimal verdi) {
        return verdi.setScale(2, RoundingMode.DOWN);
    }

    @Override
    public String toString() {
        return verdi.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (SamtidigUttaksprosent) o;
        return Objects.equals(decimalValue(), that.decimalValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(decimalValue());
    }

    @Override
    public int compareTo(SamtidigUttaksprosent samtidigUttaksprosent) {
        return decimalValue().compareTo(samtidigUttaksprosent.decimalValue());
    }

    static class SamtidigUttaksprosentSerializer<V extends SamtidigUttaksprosent> extends StdSerializer<V> {

        public SamtidigUttaksprosentSerializer(Class<V> targetCls) {
            super(targetCls);
        }

        @Override
        public void serialize(V value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeNumber(value.decimalValue());
        }
    }

    static class MySamtidigUttaksprosentSerializer extends SamtidigUttaksprosent.SamtidigUttaksprosentSerializer<SamtidigUttaksprosent> {
        public MySamtidigUttaksprosentSerializer() {
            super(SamtidigUttaksprosent.class);
        }
    }
}
