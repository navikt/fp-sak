package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.NaringsvirksomhetTypeDto;
import no.nav.vedtak.util.InputValideringRegex;

public class VirksomhetDto {

    @Size(max = 100)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @NotNull
    private String navn;

    private Boolean virksomhetRegistrertINorge;

    @Pattern(regexp = "[\\d]{9}")
    private String organisasjonsnummer;

    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.NAVN)
    private String landJobberFra;

    @Valid
    @NotNull
    private NaringsvirksomhetTypeDto typeVirksomhet;

    private Boolean varigEndretEllerStartetSisteFireAr;
    private Boolean harVarigEndring;
    private Boolean erNyoppstartet;
    private LocalDate varigEndringGjeldendeFom;

    @Digits(integer = 9, fraction = 0)
    private Long inntekt;

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String beskrivelseAvEndring;

    private Boolean harRegnskapsforer;

    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String navnRegnskapsforer;

    @Size(min = 1, max = 30)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String tlfRegnskapsforer;

    private Boolean familieEllerVennerTilknyttetNaringen;
    private Boolean erNyIArbeidslivet;
    private LocalDate oppstartsdato;
    private LocalDate fom;
    private LocalDate tom;

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public Boolean getVirksomhetRegistrertINorge() {
        return virksomhetRegistrertINorge;
    }

    public void setVirksomhetRegistrertINorge(Boolean virksomhetRegistrertINorge) {
        this.virksomhetRegistrertINorge = virksomhetRegistrertINorge;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public void setOrganisasjonsnummer(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }

    public String getLandJobberFra() {
        return landJobberFra;
    }

    public void setLandJobberFra(String landJobberFra) {
        this.landJobberFra = landJobberFra;
    }

    public NaringsvirksomhetTypeDto getTypeVirksomhet() {
        return typeVirksomhet;
    }

    public void setTypeVirksomhet(NaringsvirksomhetTypeDto typeVirksomhet) {
        this.typeVirksomhet = typeVirksomhet;
    }


    public Long getInntekt() {
        return inntekt;
    }

    public void setInntekt(Long inntekt) {
        this.inntekt = inntekt;
    }

    public String getBeskrivelseAvEndring() {
        return beskrivelseAvEndring;
    }

    public void setBeskrivelseAvEndring(String beskrivelseAvEndring) {
        this.beskrivelseAvEndring = beskrivelseAvEndring;
    }

    public Boolean getHarRegnskapsforer() {
        return harRegnskapsforer;
    }

    public void setHarRegnskapsforer(Boolean harRegnskapsforer) {
        this.harRegnskapsforer = harRegnskapsforer;
    }

    public String getNavnRegnskapsforer() {
        return navnRegnskapsforer;
    }

    public void setNavnRegnskapsforer(String navnRegnskapsforer) {
        this.navnRegnskapsforer = navnRegnskapsforer;
    }

    public String getTlfRegnskapsforer() {
        return tlfRegnskapsforer;
    }

    public void setTlfRegnskapsforer(String tlfRegnskapsforer) {
        this.tlfRegnskapsforer = tlfRegnskapsforer;
    }

    public Boolean getFamilieEllerVennerTilknyttetNaringen() {
        return familieEllerVennerTilknyttetNaringen;
    }

    public void setFamilieEllerVennerTilknyttetNaringen(Boolean familieEllerVennerTilknyttetNaringen) {
        this.familieEllerVennerTilknyttetNaringen = familieEllerVennerTilknyttetNaringen;
    }

    public Boolean getVarigEndretEllerStartetSisteFireAr() {
        return varigEndretEllerStartetSisteFireAr;
    }

    public void setVarigEndretEllerStartetSisteFireAr(Boolean varigEndretEllerStartetSisteFireAr) {
        this.varigEndretEllerStartetSisteFireAr = varigEndretEllerStartetSisteFireAr;
    }

    public Boolean getHarVarigEndring() {
        return harVarigEndring;
    }

    public void setHarVarigEndring(Boolean harVarigEndring) {
        this.harVarigEndring = harVarigEndring;
    }

    public Boolean getErNyoppstartet() {
        return erNyoppstartet;
    }

    public void setErNyoppstartet(Boolean erNyoppstartet) {
        this.erNyoppstartet = erNyoppstartet;
    }

    public LocalDate getVarigEndringGjeldendeFom() {
        return varigEndringGjeldendeFom;
    }

    public void setVarigEndringGjeldendeFom(LocalDate varigEndringGjeldendeFom) {
        this.varigEndringGjeldendeFom = varigEndringGjeldendeFom;
    }

    public Boolean getErNyIArbeidslivet() {
        return erNyIArbeidslivet;
    }

    public void setErNyIArbeidslivet(Boolean erNyIArbeidslivet) {
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public LocalDate getOppstartsdato() {
        return oppstartsdato;
    }

    public void setOppstartsdato(LocalDate oppstartsdato) {
        this.oppstartsdato = oppstartsdato;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
