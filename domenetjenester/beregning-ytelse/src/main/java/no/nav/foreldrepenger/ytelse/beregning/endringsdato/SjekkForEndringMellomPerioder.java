package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatPeriodeEndringModell;

@ApplicationScoped
public class SjekkForEndringMellomPerioder {

    private SjekkForIngenAndelerOgAndelerUtenDagsats sjekkForIngenAndelerOgAndelerUtenDagsats;
    private SjekkOmPerioderHarEndringIAndeler sjekkOmPerioderHarEndringIAndeler;

    SjekkForEndringMellomPerioder(){
        // for CDI proxy
    }

    @Inject
    public SjekkForEndringMellomPerioder(SjekkForIngenAndelerOgAndelerUtenDagsats sjekkForIngenAndelerOgAndelerUtenDagsats,
                                         SjekkOmPerioderHarEndringIAndeler sjekkOmPerioderHarEndringIAndeler){
        this.sjekkForIngenAndelerOgAndelerUtenDagsats = sjekkForIngenAndelerOgAndelerUtenDagsats;
        this.sjekkOmPerioderHarEndringIAndeler = sjekkOmPerioderHarEndringIAndeler;
    }

    /**
     * Sjekker en endring ved å sammenligne to perioder.
     * Kaster en feil hvis begge periodene er null.
     * Sjekker først om periodene har ingen andeler eller andeler uten dagsats. Se {@link SjekkForIngenAndelerOgAndelerUtenDagsats}.
     * - Return false hvis dette er tilfellet
     * Sjekker deretter om enten  ny eller gammel periode er null
     * - Return true hvis dette er tilfellet
     * Sjekker deretter om det har skjedd en endring i andelene. Se {@link SjekkOmPerioderHarEndringIAndeler}
     * - Return true hvis dette er tilfellet, false hvis ikke.
     * @param nyPeriode - Periode fra revurdering
     * @param gammelPeriode - Periode fra førstegangsbehandling
     * @return True hvis det har skjedd en endring.
     *         False hvis ingen endring har skjedd.
     */
    public boolean sjekk(BeregningsresultatPeriodeEndringModell nyPeriode, BeregningsresultatPeriodeEndringModell gammelPeriode) {
        if (nyPeriode == null && gammelPeriode == null) {
            throw new IllegalStateException("Utviklerfeil: Både ny og gammel periode kan ikke være null");
        }
        if (sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode)) {
            return false;
        }
        if (nyPeriode == null || gammelPeriode == null) {
            return true;
        }
        return false;
//        return sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
    }

}
