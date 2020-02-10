package no.nav.foreldrepenger.ytelse.beregning;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

@ApplicationScoped
public class SjekkForIngenAndelerOgAndelerUtenDagsats {

    public SjekkForIngenAndelerOgAndelerUtenDagsats() {
        // for CDI proxy
    }

    /**
     * Sjekker for ingen andeler eller andeler uten dagsats.
     * Hvis ny periode er null, sjekk gammel periode for andeler uten dagsats
     * - Return true hvis ingen andeler med dagsats, false ellers.
     * Hvis gammel periode er null, sjekk ny periode for andeler uten dagsats
     * - Return true hvis ingen andeler med dagsats, false ellers.
     * Hvis gammel og ny periode ikke er null, sjekk for andeler uten dagsats
     * - Return hvis begge periodene ikke har noen dagsas, false ellers.
     * @param nyPeriode Periode fra revurdering
     * @param gammelPeriode Periode fra førstegangsbehandling
     * @return True hvis det har skjedd en endring
     *         False hvis det ikke har skjedd en endring
     */
    public boolean sjekk(BeregningsresultatPeriode nyPeriode, BeregningsresultatPeriode gammelPeriode) {
        if (nyPeriode == null) {
            return erIngenAndelerEllerAndelerUtenDagsats(gammelPeriode);
        }
        if (gammelPeriode == null) {
            return erIngenAndelerEllerAndelerUtenDagsats(nyPeriode);
        }
        return erIngenAndelerEllerAndelerUtenDagsats(nyPeriode) && erIngenAndelerEllerAndelerUtenDagsats(gammelPeriode);
    }

    private boolean erIngenAndelerEllerAndelerUtenDagsats(BeregningsresultatPeriode periode){
        return periode.getBeregningsresultatAndelList().stream()
            .allMatch(andel -> andel.getDagsats() == 0);
    }

}
