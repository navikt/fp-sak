package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.AdopsjonGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.AnnenPartGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.ArbeidGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.BehandlingGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.DatoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.KontoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.MedlemskapGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.OpptjeningGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.RettOgOmsorgGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.RevurderingGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.SøknadGrunnlagBygger;
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
                                                KontoerGrunnlagBygger kontoerGrunnlagBygger) { // NOSONAR
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
    }

    public RegelGrunnlag byggGrunnlag(UttakInput input) {
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = input.getYtelsespesifiktGrunnlag();
        return new RegelGrunnlag.Builder()
            .medArbeid(arbeidGrunnlagBygger.byggGrunnlag(input))
            .medRettOgOmsorg(rettOgOmsorgGrunnlagBygger.byggGrunnlag(input))
            .medBehandling(behandlingGrunnlagBygger.byggGrunnlag(input))
            .medMedlemskap(medlemskapGrunnlagBygger.byggGrunnlag(input))
            .medSøknad(søknadGrunnlagBygger.byggGrunnlag(input))
            .medRevurdering(revurderingGrunnlagBygger.byggGrunnlag(input).orElse(null))
            .medAnnenPart(annenPartGrunnlagBygger.byggGrunnlag(foreldrepengerGrunnlag).orElse(null))
            .medDatoer(datoerGrunnlagBygger.byggGrunnlag(input))
            .medInngangsvilkår(inngangsvilkårGrunnlagBygger.byggGrunnlag(input))
            .medOpptjening(opptjeningGrunnlagBygger.byggGrunnlag(input))
            .medAdopsjon(adopsjonGrunnlagBygger.byggGrunnlag(foreldrepengerGrunnlag).orElse(null))
            .medKontoer(kontoerGrunnlagBygger.byggGrunnlag(input.getBehandlingReferanse()))
            .build();
    }
}
