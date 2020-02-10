package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;

public class ArbeidsforholdDto {

    //NOSONAR
    private String id;
    private String navn;
    // For mottak fra GUI (orgnr for virksomhet, og aktørId for person-arbeidsgiver)
    private String arbeidsgiverIdentifikator;
    // For visning i GUI (orgnr for virksomhet, og fødselsdato formatert dd.MM.yyyy for person-arbeidsgiver)
    private String arbeidsgiverIdentifiktorGUI;
    private String arbeidsforholdId;
    private String eksternArbeidsforholdId;
    private String begrunnelse;
    private String erstatterArbeidsforholdId;

    private ArbeidsforholdHandlingType handlingType;
    private ArbeidsforholdKildeDto kilde;
    private BigDecimal stillingsprosent;

    private LocalDate skjaeringstidspunkt;
    private LocalDate mottattDatoInntektsmelding;
    private LocalDate fomDato;
    private LocalDate tomDato;

    private Boolean harErstattetEttEllerFlere;
    private Boolean ikkeRegistrertIAaRegister;
    private Boolean tilVurdering;
    private Boolean vurderOmSkalErstattes;
    private Boolean brukArbeidsforholdet;
    private Boolean fortsettBehandlingUtenInntektsmelding;
    private Boolean erNyttArbeidsforhold;
    private Boolean erEndret;
    private Boolean erSlettet;
    private boolean brukMedJustertPeriode;
    private boolean lagtTilAvSaksbehandler;
    private boolean basertPaInntektsmelding;
    private Boolean brukPermisjon;
    private Boolean inntektMedTilBeregningsgrunnlag;
    private List<PermisjonDto> permisjoner;
    private LocalDate overstyrtTom;

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
        this.fomDato = fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    public ArbeidsforholdKildeDto getKilde() {
        return kilde;
    }

    public void setKilde(ArbeidsforholdKilde kilde) {
        this.kilde = new ArbeidsforholdKildeDto(kilde.getNavn());
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

    public boolean getBasertPaInntektsmelding() {
        return basertPaInntektsmelding;
    }

    public void setBasertPaInntektsmelding(boolean basertPaInntektsmelding) {
        this.basertPaInntektsmelding = basertPaInntektsmelding;
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

    public String getErstatterArbeidsforholdId() {
        return erstatterArbeidsforholdId;
    }

    public void setErstatterArbeidsforholdId(String erstatterArbeidsforholdId) {
        this.erstatterArbeidsforholdId = erstatterArbeidsforholdId;
    }

    public Boolean getHarErstattetEttEllerFlere() {
        return harErstattetEttEllerFlere;
    }

    public void setHarErstattetEttEllerFlere(Boolean harErstattetEttEllerFlere) {
        this.harErstattetEttEllerFlere = harErstattetEttEllerFlere;
    }

    public Boolean getIkkeRegistrertIAaRegister() {
        return ikkeRegistrertIAaRegister;
    }

    public void setIkkeRegistrertIAaRegister(Boolean ikkeRegistrertIAaRegister) {
        this.ikkeRegistrertIAaRegister = ikkeRegistrertIAaRegister;
    }

    public Boolean getTilVurdering() {
        return tilVurdering;
    }

    public void setTilVurdering(Boolean tilVurdering) {
        this.tilVurdering = tilVurdering;
    }

    public Boolean getVurderOmSkalErstattes() {
        return vurderOmSkalErstattes;
    }

    public void setVurderOmSkalErstattes(boolean vurderOmSkalErstattes) {
        this.vurderOmSkalErstattes = vurderOmSkalErstattes;
    }

    public String getArbeidsgiverIdentifiktorGUI() {
        return arbeidsgiverIdentifiktorGUI;
    }

    public void setArbeidsgiverIdentifiktorGUI(String arbeidsgiverIdentififaktorGUI) {
        this.arbeidsgiverIdentifiktorGUI = arbeidsgiverIdentififaktorGUI;
    }

    public ArbeidsforholdHandlingType getHandlingType() {
        return handlingType;
    }

    public void setHandlingType(ArbeidsforholdHandlingType handlingType) {
        this.handlingType = handlingType;
    }

    public boolean getBrukMedJustertPeriode() {
        return brukMedJustertPeriode;
    }

    public void setBrukMedJustertPeriode(boolean brukMedJustertPeriode) {
        this.brukMedJustertPeriode = brukMedJustertPeriode;
    }

    public void setLagtTilAvSaksbehandler(boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
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

    public LocalDate getOverstyrtTom() {
        return overstyrtTom;
    }

    public void setOverstyrtTom(LocalDate overstyrtTom) {
        this.overstyrtTom = overstyrtTom;
    }

}
