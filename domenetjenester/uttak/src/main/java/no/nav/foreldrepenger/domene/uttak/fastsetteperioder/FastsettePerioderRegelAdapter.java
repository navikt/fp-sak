package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.FastsettePerioderRegelOrkestrering;
import no.nav.foreldrepenger.regler.uttak.konfig.FeatureToggles;

@ApplicationScoped
public class FastsettePerioderRegelAdapter {

    private static final FastsettePerioderRegelOrkestrering regel = new FastsettePerioderRegelOrkestrering();
    private FastsettePerioderRegelGrunnlagBygger regelGrunnlagBygger;
    private FastsettePerioderRegelResultatKonverterer regelResultatKonverterer;

    FastsettePerioderRegelAdapter() {
        // For CDI
    }

    @Inject
    public FastsettePerioderRegelAdapter(FastsettePerioderRegelGrunnlagBygger regelGrunnlagBygger,
                                         FastsettePerioderRegelResultatKonverterer regelResultatKonverterer) {
        this.regelGrunnlagBygger = regelGrunnlagBygger;
        this.regelResultatKonverterer = regelResultatKonverterer;
    }

    UttakResultatPerioderEntitet fastsettePerioder(UttakInput input) {
        var grunnlag = regelGrunnlagBygger.byggGrunnlag(input);
        var resultat = regel.fastsettePerioder(grunnlag, new FeatureTogglesImpl());
        return regelResultatKonverterer.konverter(input, resultat);
    }

    private static class FeatureTogglesImpl implements FeatureToggles {
    }
}









