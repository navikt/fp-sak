package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;

public class OpptjeningsperiodeGrunnlag implements VilkårGrunnlag {

    // Input til regel
    private RegelSøkerRolle søkerRolle;
    private FagsakÅrsak fagsakÅrsak;
    private LocalDate førsteUttaksDato;
    private LocalDate hendelsesDato;
    private LocalDate terminDato;
    private LocalDate morsMaksdato;

    private Period periodeLengde;

    private Period tidligsteUttakFørFødselPeriode;
    // Settes i løpet av regelevaluering

    private LocalDate skjæringsdatoOpptjening;
    private LocalDate opptjeningsperiodeFom;
    private LocalDate opptjeningsperiodeTom;
    public OpptjeningsperiodeGrunnlag() {
    }

    public OpptjeningsperiodeGrunnlag(FagsakÅrsak fagsakÅrsak, RegelSøkerRolle soekerRolle, LocalDate førsteUttaksDato,
                                      LocalDate hendelsesDato, LocalDate terminDato) {
        this.fagsakÅrsak = fagsakÅrsak;
        this.søkerRolle = soekerRolle;
        this.førsteUttaksDato = førsteUttaksDato;
        this.hendelsesDato = hendelsesDato;
        this.terminDato = terminDato;
    }

    public RegelSøkerRolle getSøkerRolle() {
        return søkerRolle;
    }

    public FagsakÅrsak getFagsakÅrsak() {
        return fagsakÅrsak;
    }

    public LocalDate getFørsteUttaksDato() {
        return førsteUttaksDato;
    }

    public LocalDate getHendelsesDato() {
        return hendelsesDato;
    }

    public LocalDate getTerminDato() {
        return terminDato;
    }

    public Period getPeriodeLengde() { return periodeLengde; }

    public void setSøkerRolle(RegelSøkerRolle søkerRolle) {
        this.søkerRolle = søkerRolle;
    }

    public void setFagsakÅrsak(FagsakÅrsak fagsakÅrsak) {
        this.fagsakÅrsak = fagsakÅrsak;
    }

    public void setFørsteUttaksDato(LocalDate førsteUttaksDato) {
        this.førsteUttaksDato = førsteUttaksDato;
    }

    public void setHendelsesDato(LocalDate hendelsesDato) {
        this.hendelsesDato = hendelsesDato;
    }

    public void setTerminDato(LocalDate terminDato) {
        this.terminDato = terminDato;
    }

    public void setPeriodeLengde(Period periodeLengde) {
        this.periodeLengde = periodeLengde;
    }

    public LocalDate getSkjæringsdatoOpptjening() {
        return skjæringsdatoOpptjening;
    }

    public LocalDate getOpptjeningsperiodeFom() {
        return opptjeningsperiodeFom;
    }

    public LocalDate getOpptjeningsperiodeTom() {
        return opptjeningsperiodeTom;
    }

    public void setSkjæringsdatoOpptjening(LocalDate skjæringsdatoOpptjening) {
        this.skjæringsdatoOpptjening = skjæringsdatoOpptjening;
    }

    public void setOpptjeningsperiodeFom(LocalDate opptjeningsperiodeFom) {
        this.opptjeningsperiodeFom = opptjeningsperiodeFom;
    }

    public void setOpptjeningsperiodeTom(LocalDate opptjeningsperiodeTom) {
        this.opptjeningsperiodeTom = opptjeningsperiodeTom;
    }

    public Period getTidligsteUttakFørFødselPeriode() {
        return tidligsteUttakFørFødselPeriode;
    }

    public void setTidligsteUttakFørFødselPeriode(Period tidligsteUttakFørFødselPeriode) {
        this.tidligsteUttakFørFødselPeriode = tidligsteUttakFørFødselPeriode;
    }

    public Optional<LocalDate> getMorsMaksdato() {
        return Optional.ofNullable(morsMaksdato);
    }

    public void setMorsMaksdato(LocalDate morsMaksdato) {
        this.morsMaksdato = morsMaksdato;
    }
}


