package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.domene.arbeidsforhold.dto.PermisjonDto;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.util.FPDateUtil;

//NOSONAR
public class ArbeidsforholdWrapper {

    private String navn;
    private String arbeidsgiverIdentifikator;
    private String personArbeidsgiverIdentifikator;
    private String arbeidsforholdId;
    private String eksternArbeidsforholdId;
    private String begrunnelse;
    private String erstatterArbeidsforhold;
    private LocalDate fomDato = FPDateUtil.iDag();
    private LocalDate tomDato;
    private ArbeidsforholdKilde kilde;
    private LocalDate mottattDatoInntektsmelding;
    private BigDecimal stillingsprosent;
    private Boolean brukArbeidsforholdet;
    private Boolean fortsettBehandlingUtenInntektsmelding;
    private Boolean erNyttArbeidsforhold;
    private Boolean erEndret;
    private Boolean erSlettet;
    private Boolean harErsattetEttEllerFlere;
    private Boolean ikkeRegistrertIAaRegister;
    private boolean harAksjonspunkt = false;
    private boolean vurderOmSkalErstattes = false;
    private boolean lagtTilAvSaksbehandler;
    private boolean basertPåInntektsmelding;
    private boolean brukMedJustertPeriode;
    private Boolean inntektMedTilBeregningsgrunnlag;
    private Boolean brukPermisjon;
    private LocalDate skjaeringstidspunkt;
    private LocalDate overstyrtTom;
    private ArbeidsforholdHandlingType handlingType;
    private List<PermisjonDto> permisjoner;

    public boolean isHarAksjonspunkt() {
        return harAksjonspunkt;
    }

    public void setHarAksjonspunkt(boolean harAksjonspunkt) {
        this.harAksjonspunkt = harAksjonspunkt;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public void setFomDato(LocalDate fomDato) {
        if (fomDato != null) {
            this.fomDato = fomDato;
        }
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    public ArbeidsforholdKilde getKilde() {
        return kilde;
    }

    public void setKilde(ArbeidsforholdKilde kilde) {
        this.kilde = kilde;
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

    public LocalDate getMottattDatoInntektsmelding() {
        return mottattDatoInntektsmelding;
    }

    public void setMottattDatoInntektsmelding(LocalDate mottattDatoInntektsmelding) {
        this.mottattDatoInntektsmelding = mottattDatoInntektsmelding;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public void setEksternArbeidsforholdId(String eksternArbeidsforholdId) {
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
    }

    public Boolean getErNyttArbeidsforhold() {
        return erNyttArbeidsforhold;
    }

    public void setErNyttArbeidsforhold(Boolean erNyttArbeidsforhold) {
        this.erNyttArbeidsforhold = erNyttArbeidsforhold;
    }

    public Boolean getErEndret() {
        return erEndret;
    }

    public void setErEndret(Boolean erEndret) {
        this.erEndret = erEndret;
    }

    public Boolean getErSlettet() {
        return erSlettet;
    }

    public void setErSlettet(Boolean erSlettet) {
        this.erSlettet = erSlettet;
    }

    public String getErstatterArbeidsforhold() {
        return erstatterArbeidsforhold;
    }

    public void setErstatterArbeidsforhold(String erstatterArbeidsforhold) {
        this.erstatterArbeidsforhold = erstatterArbeidsforhold;
    }

    public Boolean getHarErsattetEttEllerFlere() {
        return harErsattetEttEllerFlere;
    }

    public void setHarErsattetEttEllerFlere(Boolean harErsattetEttEllerFlere) {
        this.harErsattetEttEllerFlere = harErsattetEttEllerFlere;
    }

    public Boolean getIkkeRegistrertIAaRegister() {
        return ikkeRegistrertIAaRegister;
    }

    public void setIkkeRegistrertIAaRegister(Boolean ikkeRegistrertIAaRegister) {
        this.ikkeRegistrertIAaRegister = ikkeRegistrertIAaRegister;
    }

    public boolean getVurderOmSkalErstattes() {
        return vurderOmSkalErstattes;
    }

    public void setVurderOmSkalErstattes(boolean vurderOmSkalErstattes) {
        this.vurderOmSkalErstattes = vurderOmSkalErstattes;
    }

    public String getPersonArbeidsgiverIdentifikator() {
        return personArbeidsgiverIdentifikator;
    }

    public void setPersonArbeidsgiverIdentifikator(String personArbeidsgiverIdentifikator) {
        this.personArbeidsgiverIdentifikator = personArbeidsgiverIdentifikator;
    }

    public ArbeidsforholdHandlingType getHandlingType() {
        return handlingType;
    }

    public void setHandlingType(ArbeidsforholdHandlingType handlingType) {
        this.handlingType = handlingType;
    }

    public Boolean getInntektMedTilBeregningsgrunnlag() {
        return inntektMedTilBeregningsgrunnlag;
    }

    public void setInntektMedTilBeregningsgrunnlag(Boolean inntektMedTilBeregningsgrunnlag) {
        this.inntektMedTilBeregningsgrunnlag = inntektMedTilBeregningsgrunnlag;
    }

    public LocalDate getSkjaeringstidspunkt() {
        return skjaeringstidspunkt;
    }

    public void setSkjaeringstidspunkt(LocalDate skjaeringstidspunkt) {
        this.skjaeringstidspunkt = skjaeringstidspunkt;
    }

    public boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public void setLagtTilAvSaksbehandler(boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public Boolean getBrukPermisjon() {
        return brukPermisjon;
    }

    public void setBrukPermisjon(Boolean brukPermisjon) {
        this.brukPermisjon = brukPermisjon;
    }

    public void setPermisjoner(List<PermisjonDto> permisjoner) {
        this.permisjoner = permisjoner;
    }

    public List<PermisjonDto> getPermisjoner() {
        return permisjoner;
    }

    public boolean getBrukMedJustertPeriode() {
        return brukMedJustertPeriode;
    }

    public void setBrukMedJustertPeriode(boolean brukMedJustertPeriode) {
        this.brukMedJustertPeriode = brukMedJustertPeriode;
    }

    public LocalDate getOverstyrtTom() {
        return overstyrtTom;
    }

    public void setOverstyrtTom(LocalDate overstyrtTom) {
        this.overstyrtTom = overstyrtTom;
    }

    public boolean getBasertPåInntektsmelding() {
        return basertPåInntektsmelding;
    }

    public void setBasertPåInntektsmelding(boolean basertPåInntektsmelding) {
        this.basertPåInntektsmelding = basertPåInntektsmelding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidsforholdWrapper that = (ArbeidsforholdWrapper) o;
        return Objects.equals(arbeidsgiverIdentifikator, that.arbeidsgiverIdentifikator) &&
            InternArbeidsforholdRef.ref(arbeidsforholdId).gjelderFor(InternArbeidsforholdRef.ref(that.arbeidsforholdId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiverIdentifikator, arbeidsforholdId);
    }

}
