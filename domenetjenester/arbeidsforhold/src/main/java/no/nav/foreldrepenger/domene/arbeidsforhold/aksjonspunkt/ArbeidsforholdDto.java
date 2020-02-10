package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.domene.arbeidsforhold.dto.PermisjonDto;
import no.nav.vedtak.util.InputValideringRegex;

public class ArbeidsforholdDto {

    // AktørId (13-tall) for person-arbeidsgiver, og orgnr (9-tall) for virksomhet
    @Pattern(regexp = "\\d{9}|\\d{13}")
    private String arbeidsgiverIdentifikator;

    /** Kun til bruk i gui for å unikt identifiser en rad. */
    @Size(max = 100)
    private String id;

    @Size(max = 100)
    private String arbeidsforholdId;

    @Size(max = 100)
    private String erstatterArbeidsforholdId;

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    @Min(0)
    @Max(150)
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsprosent;

    private String navn;
    private Boolean erNyttArbeidsforhold;
    private Boolean lagtTilAvSaksbehandler;
    private Boolean basertPaInntektsmelding;
    private Boolean brukArbeidsforholdet;
    private Boolean fortsettBehandlingUtenInntektsmelding;
    private Boolean inntektMedTilBeregningsgrunnlag;
    private Boolean brukPermisjon;
    private LocalDate fomDato;
    private LocalDate tomDato;
    private LocalDate overstyrtTom;
    private LocalDate mottattDatoInntektsmelding;
    private List<PermisjonDto> permisjoner;

    public ArbeidsforholdDto() {//NOSONAR
        // trengs for deserialisering av JSON
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public Boolean getBrukArbeidsforholdet() {
        return brukArbeidsforholdet;
    }

    public void setBrukArbeidsforholdet(Boolean brukArbeidsforholdet) {
        this.brukArbeidsforholdet = brukArbeidsforholdet;
    }

    public Boolean getFortsettBehandlingUtenInntektsmelding() {
        return fortsettBehandlingUtenInntektsmelding;
    }

    public void setFortsettBehandlingUtenInntektsmelding(Boolean fortsettBehandlingUtenInntektsmelding) {
        this.fortsettBehandlingUtenInntektsmelding = fortsettBehandlingUtenInntektsmelding;
    }

    public String getErstatterArbeidsforholdId() {
        return erstatterArbeidsforholdId;
    }

    public void setErstatterArbeidsforholdId(String erstatterArbeidsforholdId) {
        this.erstatterArbeidsforholdId = erstatterArbeidsforholdId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getErNyttArbeidsforhold() {
        return erNyttArbeidsforhold;
    }

    public void setErNyttArbeidsforhold(Boolean erNyttArbeidsforhold) {
        this.erNyttArbeidsforhold = erNyttArbeidsforhold;
    }

    public LocalDate getOverstyrtTom() {
        return overstyrtTom;
    }

    public void setOverstyrtTom(LocalDate overstyrtTom) {
        this.overstyrtTom = overstyrtTom;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public void setLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public void setFomDato(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }

    public Boolean getInntektMedTilBeregningsgrunnlag() {
        return inntektMedTilBeregningsgrunnlag;
    }

    public void setInntektMedTilBeregningsgrunnlag(Boolean inntektMedTilBeregningsgrunnlaget) {
        this.inntektMedTilBeregningsgrunnlag = inntektMedTilBeregningsgrunnlaget;
    }

    public Boolean getBrukPermisjon() {
        return brukPermisjon;
    }

    public void setBrukPermisjon(Boolean brukPermisjon) {
        this.brukPermisjon = brukPermisjon;
    }

    public LocalDate getMottattDatoInntektsmelding() {
        return mottattDatoInntektsmelding;
    }

    public void setMottattDatoInntektsmelding(LocalDate mottattDatoInntektsmelding) {
        this.mottattDatoInntektsmelding = mottattDatoInntektsmelding;
    }

    public List<PermisjonDto> getPermisjoner() {
        return permisjoner;
    }

    public void setPermisjoner(List<PermisjonDto> permisjoner) {
        this.permisjoner = permisjoner;
    }

    public Boolean getBasertPaInntektsmelding() {
        return basertPaInntektsmelding;
    }

    public void setBasertPaInntektsmelding(Boolean basertPaInntektsmelding) {
        this.basertPaInntektsmelding = basertPaInntektsmelding;
    }
}
