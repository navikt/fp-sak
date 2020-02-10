package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import java.time.LocalDate;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Kjoenn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.SoekerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public class FødselsvilkårGrunnlag implements VilkårGrunnlag {

    /**
     * Søkers kjønn
     */
    private Kjoenn soekersKjonn;
    /**
     * Bekreftete fødselsdato
     */
    private LocalDate bekreftetFoedselsdato;
    /**
     * Antall barn ...
     */
    private int antallBarn;
    /**
     * Bekreftete termindato
     */
    private LocalDate bekreftetTermindato;
    /**
     * Mor eller far
     */
    private SoekerRolle soekerRolle;
    /**
     * Søknadsdato
     */
    private LocalDate dagensdato;

    private boolean erMorForSykVedFødsel;

    /**
     * Søknad gjelder termin
     */
    private boolean erSøktOmTermin;

    private boolean erTerminBekreftelseUtstedtEtterXUker;

    public FødselsvilkårGrunnlag() {
    }

    public FødselsvilkårGrunnlag(Kjoenn soekersKjonn, SoekerRolle soekerRolle, LocalDate dagensdato,
                                 boolean erMorForSykVedFødsel, boolean erSøktOmTermin,
                                 boolean erTerminBekreftelseUtstedtEtterXUker) {
        this.soekersKjonn = soekersKjonn;
        this.soekerRolle = soekerRolle;
        this.dagensdato = dagensdato;
        this.erMorForSykVedFødsel = erMorForSykVedFødsel;
        this.erSøktOmTermin = erSøktOmTermin;
        this.erTerminBekreftelseUtstedtEtterXUker = erTerminBekreftelseUtstedtEtterXUker;
    }

    public Kjoenn getSoekersKjonn() {
        return soekersKjonn;
    }

    public LocalDate getBekreftetFoedselsdato() {
        return bekreftetFoedselsdato;
    }

    public LocalDate getBekreftetTermindato() {
        return bekreftetTermindato;
    }

    public SoekerRolle getSoekerRolle() {
        return soekerRolle;
    }

    public LocalDate getDagensdato() {
        return dagensdato;
    }

    public void setBekreftetFoedselsdato(LocalDate bekreftetFoedselsdato) {
        this.bekreftetFoedselsdato = bekreftetFoedselsdato;
    }

    public void setBekreftetTermindato(LocalDate bekreftetTermindato) {
        this.bekreftetTermindato = bekreftetTermindato;
    }

    public int getAntallBarn() {
        return antallBarn;
    }

    public void setAntallBarn(int antallBarn) {
        this.antallBarn = antallBarn;
    }

    public boolean isErMorForSykVedFødsel() {
        return erMorForSykVedFødsel;
    }

    public boolean isErSøktOmTermin() {
        return erSøktOmTermin;
    }

    public boolean isErTerminBekreftelseUtstedtEtterXUker() {
        return erTerminBekreftelseUtstedtEtterXUker;
    }
}
