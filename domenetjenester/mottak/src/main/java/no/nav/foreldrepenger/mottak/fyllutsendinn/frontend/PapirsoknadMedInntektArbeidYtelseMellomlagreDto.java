package no.nav.foreldrepenger.mottak.fyllutsendinn.frontend;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mellomlagring-variant av {@link no.nav.foreldrepenger.web.app.tjenester.registrering.dto.MedInntektArbeidYtelseRegistrering}.
 * Feltnavnene speiler frontend-skjemaets form field names.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class PapirsoknadMedInntektArbeidYtelseMellomlagreDto extends PapirsoknadMellomlagreDto {

    @Size(max = 50)
    private List<@Valid ArbeidsforholdFormValues> arbeidsforhold;

    @Valid
    private AndreYtelserFormValues andreYtelser;

    @Valid
    private EgenVirksomhetFormValues egenVirksomhet;

    @Valid
    private FrilansFormValues frilans;

    protected PapirsoknadMedInntektArbeidYtelseMellomlagreDto() {
        // For Jackson
    }

    public List<ArbeidsforholdFormValues> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(List<ArbeidsforholdFormValues> arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public AndreYtelserFormValues getAndreYtelser() {
        return andreYtelser;
    }

    public void setAndreYtelser(AndreYtelserFormValues andreYtelser) {
        this.andreYtelser = andreYtelser;
    }

    public EgenVirksomhetFormValues getEgenVirksomhet() {
        return egenVirksomhet;
    }

    public void setEgenVirksomhet(EgenVirksomhetFormValues egenVirksomhet) {
        this.egenVirksomhet = egenVirksomhet;
    }

    public FrilansFormValues getFrilans() {
        return frilans;
    }

    public void setFrilans(FrilansFormValues frilans) {
        this.frilans = frilans;
    }

    // --- Selvstendige form-value records ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArbeidsforholdFormValues(String arbeidsgiver, LocalDate periodeFom, LocalDate periodeTom, String land) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AndreYtelserFormValues(List<String> andreYtelserTyper,
                                         Map<String, List<YtelserPeriode>> andreYtelserPerioder) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record YtelserPeriode(LocalDate periodeFom, LocalDate periodeTom) { }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EgenVirksomhetFormValues(Boolean harArbeidetIEgenVirksomhet, List<VirksomhetFormValues> virksomheter) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record VirksomhetFormValues(String navn, String organisasjonsnummer, LocalDate fom, LocalDate tom) { }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FrilansFormValues(Boolean harSøkerPeriodeMedFrilans, Collection<FrilansPeriode> perioder,
                                    Boolean erNyoppstartetFrilanser, Boolean harInntektFraFosterhjem,
                                    Boolean harHattOppdragForFamilie, Collection<OppdragPeriode> oppdragPerioder) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record FrilansPeriode(LocalDate periodeFom, LocalDate periodeTom) { }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record OppdragPeriode(String oppdragsgiver, LocalDate fomDato, LocalDate tomDato) { }
    }
}
