package no.nav.foreldrepenger.mottak.vedtak.spokelse;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykepengeVedtak {

    @JsonProperty("vedtaksreferanse")
    private String vedtaksreferanse;
    @JsonProperty("utbetalinger")
    private List<SykepengeUtbetaling> utbetalinger;
    @JsonProperty("vedtattTidspunkt")
    private LocalDateTime vedtattTidspunkt;


    @JsonCreator
    public SykepengeVedtak(@JsonProperty("vedtaksreferanse") String vedtaksreferanse,
                           @JsonProperty("utbetalinger") List<SykepengeUtbetaling> utbetalinger,
                           @JsonProperty("vedtattTidspunkt") LocalDateTime vedtattTidspunkt) {
        this.vedtaksreferanse = vedtaksreferanse;
        this.utbetalinger = utbetalinger;
        this.vedtattTidspunkt = vedtattTidspunkt;
    }

    public String getVedtaksreferanse() {
        return vedtaksreferanse;
    }

    public List<SykepengeUtbetaling> getUtbetalinger() {
        return utbetalinger != null ? utbetalinger : Collections.emptyList();
    }

    public LocalDateTime getVedtattTidspunkt() {
        return vedtattTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SykepengeVedtak that = (SykepengeVedtak) o;
        return Objects.equals(vedtaksreferanse, that.vedtaksreferanse) &&
            Objects.equals(utbetalinger, that.utbetalinger) &&
            Objects.equals(vedtattTidspunkt, that.vedtattTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vedtaksreferanse, utbetalinger, vedtattTidspunkt);
    }

    @Override
    public String toString() {
        return "SykepengeVedtak{" +
            "vedtaksreferanse='" + vedtaksreferanse + '\'' +
            ", utbetalinger=" + utbetalinger +
            ", vedtattTidspunkt=" + vedtattTidspunkt +
            '}';
    }

}
