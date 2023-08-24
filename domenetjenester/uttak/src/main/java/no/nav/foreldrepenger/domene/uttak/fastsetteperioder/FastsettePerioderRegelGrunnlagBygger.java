package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.*;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RegelGrunnlag;

@ApplicationScoped
public class FastsettePerioderRegelGrunnlagBygger {

    private AnnenPartGrunnlagBygger annenPartGrunnlagBygger;
    private ArbeidGrunnlagBygger arbeidGrunnlagBygger;
    private BehandlingGrunnlagBygger behandlingGrunnlagBygger;
    private DatoerGrunnlagBygger datoerGrunnlagBygger;
    private MedlemskapGrunnlagBygger medlemskapGrunnlagBygger;
    private RettOgOmsorgGrunnlagBygger rettOgOmsorgGrunnlagBygger;
    private RevurderingGrunnlagBygger revurderingGrunnlagBygger;
    private SøknadGrunnlagBygger søknadGrunnlagBygger;
    private InngangsvilkårGrunnlagBygger inngangsvilkårGrunnlagBygger;
    private OpptjeningGrunnlagBygger opptjeningGrunnlagBygger;
    private AdopsjonGrunnlagBygger adopsjonGrunnlagBygger;
    private KontoerGrunnlagBygger kontoerGrunnlagBygger;
    private YtelserGrunnlagBygger ytelserGrunnlagBygger;

    public FastsettePerioderRegelGrunnlagBygger() {
        // CDI
    }

    @Inject
    public FastsettePerioderRegelGrunnlagBygger(AnnenPartGrunnlagBygger annenPartGrunnlagBygger,
                                                ArbeidGrunnlagBygger arbeidGrunnlagBygger,
                                                BehandlingGrunnlagBygger behandlingGrunnlagBygger,
                                                DatoerGrunnlagBygger datoerGrunnlagBygger,
                                                MedlemskapGrunnlagBygger medlemskapGrunnlagBygger,
                                                RettOgOmsorgGrunnlagBygger rettOgOmsorgGrunnlagBygger,
                                                RevurderingGrunnlagBygger revurderingGrunnlagBygger,
                                                SøknadGrunnlagBygger søknadGrunnlagBygger,
                                                InngangsvilkårGrunnlagBygger inngangsvilkårGrunnlagBygger,
                                                OpptjeningGrunnlagBygger opptjeningGrunnlagBygger,
                                                AdopsjonGrunnlagBygger adopsjonGrunnlagBygger,
                                                KontoerGrunnlagBygger kontoerGrunnlagBygger,
                                                YtelserGrunnlagBygger ytelserGrunnlagBygger) {
        this.annenPartGrunnlagBygger = annenPartGrunnlagBygger;
        this.arbeidGrunnlagBygger = arbeidGrunnlagBygger;
        this.behandlingGrunnlagBygger = behandlingGrunnlagBygger;
        this.datoerGrunnlagBygger = datoerGrunnlagBygger;
        this.medlemskapGrunnlagBygger = medlemskapGrunnlagBygger;
        this.rettOgOmsorgGrunnlagBygger = rettOgOmsorgGrunnlagBygger;
        this.revurderingGrunnlagBygger = revurderingGrunnlagBygger;
        this.søknadGrunnlagBygger = søknadGrunnlagBygger;
        this.inngangsvilkårGrunnlagBygger = inngangsvilkårGrunnlagBygger;
        this.opptjeningGrunnlagBygger = opptjeningGrunnlagBygger;
        this.adopsjonGrunnlagBygger = adopsjonGrunnlagBygger;
        this.kontoerGrunnlagBygger = kontoerGrunnlagBygger;
        this.ytelserGrunnlagBygger = ytelserGrunnlagBygger;
    }

    public RegelGrunnlag byggGrunnlag(UttakInput input) {
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = input.getYtelsespesifiktGrunnlag();
        return new RegelGrunnlag.Builder()
            .arbeid(arbeidGrunnlagBygger.byggGrunnlag(input))
            .rettOgOmsorg(rettOgOmsorgGrunnlagBygger.byggGrunnlag(input))
            .behandling(behandlingGrunnlagBygger.byggGrunnlag(input))
            .medlemskap(medlemskapGrunnlagBygger.byggGrunnlag(input))
            .søknad(søknadGrunnlagBygger.byggGrunnlag(input))
            .revurdering(revurderingGrunnlagBygger.byggGrunnlag(input).orElse(null))
            .annenPart(annenPartGrunnlagBygger.byggGrunnlag(foreldrepengerGrunnlag).orElse(null))
            .datoer(datoerGrunnlagBygger.byggGrunnlag(input))
            .inngangsvilkår(inngangsvilkårGrunnlagBygger.byggGrunnlag(input))
            .opptjening(opptjeningGrunnlagBygger.byggGrunnlag(input))
            .adopsjon(adopsjonGrunnlagBygger.byggGrunnlag(foreldrepengerGrunnlag).orElse(null))
            .kontoer(kontoerGrunnlagBygger.byggGrunnlag(input))
            .ytelser(ytelserGrunnlagBygger.byggGrunnlag(input))
            .build();
    }
}
