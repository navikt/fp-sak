package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.vedtak.util.InputValideringRegex;


public class SvpArbeidsforholdDto {

    private Long tilretteleggingId;
    private LocalDate tilretteleggingBehovFom;
    private List<SvpTilretteleggingDatoDto> tilretteleggingDatoer = new ArrayList<>();
    private String arbeidsgiverReferanse;
    private String arbeidsgiverNavn;
    private String arbeidsgiverIdent;
    private String arbeidsgiverIdentVisning;
    private String opplysningerOmRisiko;
    private String opplysningerOmTilrettelegging;
    private Boolean kopiertFraTidligereBehandling;
    private LocalDateTime mottattTidspunkt;
    private String internArbeidsforholdReferanse;
    private String eksternArbeidsforholdReferanse;
    private boolean skalBrukes = true;
    private List<VelferdspermisjonDto> velferdspermisjoner = new ArrayList<>();

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    public LocalDate getTilretteleggingBehovFom() {
        return tilretteleggingBehovFom;
    }

    public void setTilretteleggingBehovFom(LocalDate tilretteleggingBehovFom) {
        this.tilretteleggingBehovFom = tilretteleggingBehovFom;
    }

    public List<SvpTilretteleggingDatoDto> getTilretteleggingDatoer() {
        return tilretteleggingDatoer;
    }

    public void setTilretteleggingDatoer(List<SvpTilretteleggingDatoDto> tilretteleggingDatoer) {
        this.tilretteleggingDatoer = tilretteleggingDatoer;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public void setArbeidsgiverIdent(String arbeidsgiverIdent) {
        this.arbeidsgiverIdent = arbeidsgiverIdent;
    }

    public Long getTilretteleggingId() {
        return tilretteleggingId;
    }

    public void setTilretteleggingId(Long tilretteleggingId) {
        this.tilretteleggingId = tilretteleggingId;
    }

    public String getOpplysningerOmRisiko() {
        return opplysningerOmRisiko;
    }

    public void setOpplysningerOmRisiko(String opplysningerOmRisiko) {
        this.opplysningerOmRisiko = opplysningerOmRisiko;
    }

    public String getOpplysningerOmTilrettelegging() {
        return opplysningerOmTilrettelegging;
    }

    public void setOpplysningerOmTilrettelegging(String opplysningerOmTilrettelegging) {
        this.opplysningerOmTilrettelegging = opplysningerOmTilrettelegging;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Boolean getKopiertFraTidligereBehandling() {
        return kopiertFraTidligereBehandling;
    }

    public void setKopiertFraTidligereBehandling(Boolean kopiertFraTidligereBehandling) {
        this.kopiertFraTidligereBehandling = kopiertFraTidligereBehandling;
    }

    public LocalDateTime getMottattTidspunkt() {
        return mottattTidspunkt;
    }

    public void setMottattTidspunkt(LocalDateTime mottattTidspunkt) {
        this.mottattTidspunkt = mottattTidspunkt;
    }

    public void setInternArbeidsforholdReferanse(String internArbeidsforholdRef) {
        this.internArbeidsforholdReferanse = internArbeidsforholdRef;
    }

    public String getInternArbeidsforholdReferanse() {
        return internArbeidsforholdReferanse;
    }

    public boolean getSkalBrukes() {
        return skalBrukes;
    }

    public void setSkalBrukes(boolean skalBrukes) {
        this.skalBrukes = skalBrukes;
    }

    public String getEksternArbeidsforholdReferanse() {
        return eksternArbeidsforholdReferanse;
    }

    public void setEksternArbeidsforholdReferanse(String eksternArbeidsforholdReferanse) {
        this.eksternArbeidsforholdReferanse = eksternArbeidsforholdReferanse;
    }

    public String getArbeidsgiverIdentVisning() {
        return arbeidsgiverIdentVisning;
    }

    public void setArbeidsgiverIdentVisning(String arbeidsgiverIdentVisning) {
        this.arbeidsgiverIdentVisning = arbeidsgiverIdentVisning;
    }

    public List<VelferdspermisjonDto> getVelferdspermisjoner() {
        return velferdspermisjoner;
    }

    public void setVelferdspermisjoner(List<VelferdspermisjonDto> velferdspermisjoner) {
        this.velferdspermisjoner = velferdspermisjoner;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }
}
