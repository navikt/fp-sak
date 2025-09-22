package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = NONE, getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, creatorVisibility = NONE)
public class FeriepengegrunnlagAndelDto {

    @JsonProperty(value = "aktivitetStatus")
    @NotNull
    @Valid
    private AktivitetStatus aktivitetStatus;

    @JsonProperty(value = "arbeidsgiverId")
    @NotNull
    @Valid
    private String arbeidsgiverId;

    @JsonProperty(value = "arbeidsforholdId")
    @Valid
    private String arbeidsforholdId;

    @JsonProperty(value = "opptjeningsår")
    @NotNull
    @Valid
    private Integer opptjeningsår;

    @JsonProperty(value = "årsbeløp")
    @NotNull
    @Valid
    private BigDecimal årsbeløp;

    @JsonProperty(value = "erBrukerMottaker")
    @NotNull
    @Valid
    private Boolean erBrukerMottaker;


    private FeriepengegrunnlagAndelDto() {
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public Integer getOpptjeningsår() {
        return opptjeningsår;
    }

    public BigDecimal getÅrsbeløp() {
        return årsbeløp;
    }

    public Boolean getErBrukerMottaker() {
        return erBrukerMottaker;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private FeriepengegrunnlagAndelDto kladd;

        public Builder() {
            kladd = new FeriepengegrunnlagAndelDto();
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            kladd.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medArbeidsgiverId(String arbeidsgiverId) {
            kladd.arbeidsgiverId = arbeidsgiverId;
            return this;
        }

        public Builder medArbeidsforholdId(String arbeidsforholdId) {
            kladd.arbeidsforholdId = arbeidsforholdId;
            return this;
        }


        public Builder medOpptjeningsår(Integer opptjeningsår) {
            kladd.opptjeningsår = opptjeningsår;
            return this;
        }


        public Builder medÅrsbeløp(BigDecimal årsbeløp) {
            kladd.årsbeløp = årsbeløp;
            return this;
        }


        public Builder medErBrukerMottaker(Boolean erBrukerMottaker) {
            kladd.erBrukerMottaker = erBrukerMottaker;
            return this;
        }

        public FeriepengegrunnlagAndelDto build() {
            validerTilstand();
            return kladd;
        }

        private void validerTilstand() {
            Objects.requireNonNull(kladd.aktivitetStatus, "aktivitetStatus");
            Objects.requireNonNull(kladd.erBrukerMottaker, "erBrukerMottaker");
            Objects.requireNonNull(kladd.årsbeløp, "årsbeløp");
            Objects.requireNonNull(kladd.opptjeningsår, "opptjeningsår");
        }

    }

}
