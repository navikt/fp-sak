package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.GrunnbeløpTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class GrunnbeløpTjenesteImplFP extends GrunnbeløpTjeneste {

    private Integer grunnbeløpMilitærHarKravPåFP;

    protected GrunnbeløpTjenesteImplFP() {
        super(null);
        // for CDI proxy
    }

    /**
     * @param grunnbeløpMilitærHarKravPå - Antall grunnbeløp søker har krav på hvis det søkes om foreldrepenger og søker har militærstatus
     *            (positivt heltall)
     */
    @Inject
    public GrunnbeløpTjenesteImplFP(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                  @KonfigVerdi(value = "fp.militær.grunnbeløp.minstekrav", defaultVerdi = "3") Integer grunnbeløpMilitærHarKravPå) {
        super(beregningsgrunnlagRepository);
        this.grunnbeløpMilitærHarKravPåFP = grunnbeløpMilitærHarKravPå;
    }

    @Override
    public Integer finnAntallGrunnbeløpMilitærHarKravPå() {
        return grunnbeløpMilitærHarKravPåFP;
    }
}
