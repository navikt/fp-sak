package no.nav.foreldrepenger.ytelse.beregning;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

@ApplicationScoped
public class SjekkOmPerioderHarEndringIAndeler {

    public SjekkOmPerioderHarEndringIAndeler() {
        // for CDI proxy
    }

    /**
     * Sjekker om det har skjedd en endring i andelene mellom to perioder.
     * Hvis periodene har forskjellig antall perioder har det alltid skjedd en endring, og dermed returner man true.
     * Hvis periodene har samme antall andeler, sjekk hver enkel andel for en korresponderende andel med samme verdi.
     * Return true hvis alle andelene har en korresponderende verdi, false ellers.
     * Hvis en andel har mer enn en korresponderende andel har det skjedd en feil og en exception blir kastet.
     * @param nyPeriode Periode for revurdering
     * @param gammelPeriode Periode for førstegangsbehandling
     * @return True hvis det har skjedd en endring
     *         False hvis det ikke har skjedd en endring
     */
    public boolean sjekk(BeregningsresultatPeriode nyPeriode, BeregningsresultatPeriode gammelPeriode) {
        List<BeregningsresultatAndel> nyeAndeler = nyPeriode.getBeregningsresultatAndelList();
        List<BeregningsresultatAndel> gamleAndeler = gammelPeriode.getBeregningsresultatAndelList();
        if (nyeAndeler.size() != gamleAndeler.size()) {
            return true;
        }
        return !nyeAndeler.stream().allMatch(nyAndel -> finnKorresponderendeAndel(nyAndel, gamleAndeler));
    }

    private boolean finnKorresponderendeAndel(BeregningsresultatAndel nyAndel, List<BeregningsresultatAndel> gamleAndeler) {
        var nyAndelNøkkel = nyAndel.getAktivitetOgArbeidsforholdNøkkel();
        long antallAndelerSomKorresponderer = gamleAndeler.stream().filter(gammelAndel ->
            Objects.equals(nyAndel.erBrukerMottaker(), gammelAndel.erBrukerMottaker()) &&
                Objects.equals(nyAndelNøkkel, gammelAndel.getAktivitetOgArbeidsforholdNøkkel()) &&
                Objects.equals(nyAndel.getDagsats(), gammelAndel.getDagsats()) &&
                erLike(nyAndel.getBeregningsresultatFeriepengerPrÅrListe(), gammelAndel.getBeregningsresultatFeriepengerPrÅrListe())).count();
        if (antallAndelerSomKorresponderer > 1) {
            throw FinnEndringsdatoFeil.FACTORY.fantFlereKorresponderendeAndelerFeil(nyAndel.getId()).toException();
        }
        return antallAndelerSomKorresponderer == 1;
    }

    private boolean erLike(List<BeregningsresultatFeriepengerPrÅr> liste1, List<BeregningsresultatFeriepengerPrÅr> liste2) {
        if (liste1.size() != liste2.size()) {
            return false;
        }
        return liste2.containsAll(liste1);
    }

}
