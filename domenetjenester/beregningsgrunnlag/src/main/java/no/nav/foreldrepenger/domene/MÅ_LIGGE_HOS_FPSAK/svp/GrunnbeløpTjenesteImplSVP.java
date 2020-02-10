package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.GrunnbeløpTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
class GrunnbeløpTjenesteImplSVP extends GrunnbeløpTjeneste {

    private Integer grunnbeløpMilitærHarKravPåSVP;

    protected GrunnbeløpTjenesteImplSVP() {
        super(null);
        // for CDI proxy
    }

    /**
     * @param grunnbeløpMilitærHarKravPå - Antall grunnbeløp søker har krav på hvis det søkes om foreldrepenger og søker har militærstatus
     *            (positivt heltall)
     */
    @Inject
    public GrunnbeløpTjenesteImplSVP(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                    @KonfigVerdi(value = "fp.militær.grunnbeløp.minstekrav", defaultVerdi = "3") Integer grunnbeløpMilitærHarKravPå) {
        super(beregningsgrunnlagRepository);
        this.grunnbeløpMilitærHarKravPåSVP = grunnbeløpMilitærHarKravPå;
    }

    @Override
    public Integer finnAntallGrunnbeløpMilitærHarKravPå() {
        return grunnbeløpMilitærHarKravPåSVP;
    }
}
