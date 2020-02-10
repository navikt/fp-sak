package no.nav.foreldrepenger.ytelse.beregning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

@ApplicationScoped
public class SjekkForEndringMellomAndelerOgFOM {

    private SjekkOmPerioderInneholderSammeAndeler sjekkOmPerioderInneholderSammeAndeler;

    SjekkForEndringMellomAndelerOgFOM(){
        // for CDI proxy
    }

    @Inject
    public SjekkForEndringMellomAndelerOgFOM(SjekkOmPerioderInneholderSammeAndeler sjekkOmPerioderInneholderSammeAndeler){
        this.sjekkOmPerioderInneholderSammeAndeler = sjekkOmPerioderInneholderSammeAndeler;
    }

    /**
     * Sjekker først om det har skjedd en endring i FOM datoen ved å sammenligne periodene.
     * Sjekker deretter for endringer i andelene ved å sammenligne andelene til periodene. Se {@link SjekkOmPerioderInneholderSammeAndeler}
     * @param nyPeriode - En periode (revurdering)
     * @param gammelPeriode - En periode (førstegangsbehandling)
     * @return True hvis det har skjedd en endring.
     *         False hvis det ikke har skjedd en endring.
     */
    public boolean sjekk(BeregningsresultatPeriode nyPeriode, BeregningsresultatPeriode gammelPeriode) {
        return !harSammePeriodeFOM(nyPeriode, gammelPeriode) || !sjekkOmPerioderInneholderSammeAndeler.sjekk(nyPeriode, gammelPeriode);
    }

    private boolean harSammePeriodeFOM(BeregningsresultatPeriode nyPeriode, BeregningsresultatPeriode gammelPeriode) {
        return nyPeriode.getBeregningsresultatPeriodeFom().isEqual(gammelPeriode.getBeregningsresultatPeriodeFom());
    }

}
