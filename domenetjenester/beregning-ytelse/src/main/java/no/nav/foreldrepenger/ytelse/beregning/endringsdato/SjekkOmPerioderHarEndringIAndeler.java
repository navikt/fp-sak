package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatAndelEndringModell;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatPeriodeEndringModell;

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
    public boolean sjekk(BeregningsresultatPeriodeEndringModell nyPeriode, BeregningsresultatPeriodeEndringModell gammelPeriode) {
        List<BeregningsresultatAndelEndringModell> nyeAndeler = nyPeriode.getAndeler();
        List<BeregningsresultatAndelEndringModell> gamleAndeler = gammelPeriode.getAndeler();
        if (nyeAndeler.size() != gamleAndeler.size()) {
            return true;
        }
        return !nyeAndeler.stream().allMatch(nyAndel -> finnKorresponderendeAndel(nyAndel, gamleAndeler));
    }

    private boolean finnKorresponderendeAndel(BeregningsresultatAndelEndringModell nyAndel, List<BeregningsresultatAndelEndringModell> gamleAndeler) {
        long antallAndelerSomKorresponderer = gamleAndeler.stream().filter(gammelAndel ->
            Objects.equals(nyAndel.erBrukerMottaker(), gammelAndel.erBrukerMottaker()) &&
                Objects.equals(nyAndel.getInntektskategori(), gammelAndel.getInntektskategori()) &&
                Objects.equals(nyAndel.getArbeidsgiver(), gammelAndel.getArbeidsgiver()) &&
                Objects.equals(nyAndel.getAktivitetStatus(), gammelAndel.getAktivitetStatus()) &&
                Objects.equals(nyAndel.getArbeidsforholdReferanse(), gammelAndel.getArbeidsforholdReferanse()) &&
                Objects.equals(nyAndel.getDagsats(), gammelAndel.getDagsats())).count();
        if (antallAndelerSomKorresponderer > 1) {
            throw FinnEndringsdatoFeil.FACTORY.fantFlereKorresponderendeAndelerFeil(nyAndel.toString()).toException();
        }
        return antallAndelerSomKorresponderer == 1;
    }

}
