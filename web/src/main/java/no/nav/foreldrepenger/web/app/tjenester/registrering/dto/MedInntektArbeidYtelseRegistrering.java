package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.AndreYtelserDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;

import java.util.List;

public abstract class MedInntektArbeidYtelseRegistrering extends ManuellRegistreringDto {

    @Valid
    @Size(max = 50)
    private List<ArbeidsforholdDto> arbeidsforhold;

    @Valid
    @Size(max = 100)
    private List<AndreYtelserDto> andreYtelser;

    @Valid
    private EgenVirksomhetDto egenVirksomhet;

    @Valid
    private FrilansDto frilans;

    protected MedInntektArbeidYtelseRegistrering() {
        // TODO Auto-generated constructor stub
    }

    public List<ArbeidsforholdDto> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(List<ArbeidsforholdDto> arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public List<AndreYtelserDto> getAndreYtelser() {
        return andreYtelser;
    }

    public void setAndreYtelser(List<AndreYtelserDto> andreYtelser) {
        this.andreYtelser = andreYtelser;
    }

    public EgenVirksomhetDto getEgenVirksomhet() {
        return egenVirksomhet;
    }

    public void setEgenVirksomhet(EgenVirksomhetDto egenVirksomhet) {
        this.egenVirksomhet = egenVirksomhet;
    }

    public FrilansDto getFrilans() {
        return frilans;
    }

    public void setFrilans(FrilansDto frilans) {
        this.frilans = frilans;
    }
}
