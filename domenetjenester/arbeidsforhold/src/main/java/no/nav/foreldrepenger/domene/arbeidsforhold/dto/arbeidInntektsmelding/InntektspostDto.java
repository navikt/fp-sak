package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = NONE, getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, creatorVisibility = NONE)
public class InntektspostDto {
    @JsonProperty(value = "beløp")
    @NotNull
    @Valid
    private BigDecimal beløp;

    @JsonProperty(value = "fom")
    @NotNull
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom")
    @NotNull
    @Valid
    private LocalDate tom;

    @JsonProperty(value = "type")
    @NotNull
    @Valid
    private InntektspostType type;

    public InntektspostDto(BigDecimal beløp, LocalDate fom, LocalDate tom, InntektspostType type) {
        this.beløp = beløp;
        this.fom = fom;
        this.tom = tom;
        this.type = type;
    }

    public BigDecimal getBeløp() {
        return beløp;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public InntektspostType getType() {
        return type;
    }
}
