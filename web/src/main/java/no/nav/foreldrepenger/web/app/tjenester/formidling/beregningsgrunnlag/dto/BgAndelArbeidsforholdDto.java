package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BgAndelArbeidsforholdDto {

    @JsonProperty(value = "arbeidsgiverIdent")
    @Valid
    @NotNull
    private String arbeidsgiverIdent;

    @JsonProperty(value = "arbeidsforholdRef")
    @Valid
    private String arbeidsforholdRef;

    @JsonProperty(value = "naturalytelseBortfaltPrÅr")
    @Valid
    @Digits(integer = 8, fraction = 2)
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal naturalytelseBortfaltPrÅr;

    @JsonProperty(value = "naturalytelseTilkommetPrÅr")
    @Valid
    @Digits(integer = 8, fraction = 2)
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal naturalytelseTilkommetPrÅr;

    public BgAndelArbeidsforholdDto(@Valid @NotNull String arbeidsgiverIdent,
                                    @Valid String arbeidsforholdRef,
                                    @Valid @Digits(integer = 8, fraction = 2) @DecimalMin("0.00") @DecimalMax("10000000.00") BigDecimal naturalytelseBortfaltPrÅr,
                                    @Valid @Digits(integer = 8, fraction = 2) @DecimalMin("0.00") @DecimalMax("10000000.00") BigDecimal naturalytelseTilkommetPrÅr) {
        this.arbeidsgiverIdent = arbeidsgiverIdent;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.naturalytelseBortfaltPrÅr = naturalytelseBortfaltPrÅr;
        this.naturalytelseTilkommetPrÅr = naturalytelseTilkommetPrÅr;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public BigDecimal getNaturalytelseBortfaltPrÅr() {
        return naturalytelseBortfaltPrÅr;
    }

    public BigDecimal getNaturalytelseTilkommetPrÅr() {
        return naturalytelseTilkommetPrÅr;
    }
}
