package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;

@ApplicationScoped
public class OpprettToTrinnsgrunnlag {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FpUttakRepository fpUttakRepository;
    private TotrinnTjeneste totrinnTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    OpprettToTrinnsgrunnlag() {
        // CDI
    }

    @Inject
    public OpprettToTrinnsgrunnlag(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                           YtelsesFordelingRepository ytelsesFordelingRepository,
                           FpUttakRepository fpUttakRepository,
                           TotrinnTjeneste totrinnTjeneste,
                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.fpUttakRepository = fpUttakRepository;
        this.iayTjeneste = iayTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    public void settNyttTotrinnsgrunnlag(Behandling behandling) {
        var behandlingId = behandling.getId();
        var beregningsgrunnlagOpt = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId)
                .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        var ytelseFordelingIdOpt = ytelsesFordelingRepository.hentIdPåAktivYtelsesFordeling(behandlingId);
        var uttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        var iayGrunnlagOpt = iayTjeneste.finnGrunnlag(behandlingId);

        var totrinnsresultatgrunnlag = new Totrinnresultatgrunnlag(behandling,
            ytelseFordelingIdOpt.orElse(null),
            uttakResultatOpt.map(UttakResultatEntitet::getId).orElse(null),
            beregningsgrunnlagOpt.map(BeregningsgrunnlagEntitet::getId).orElse(null),
            iayGrunnlagOpt.map(InntektArbeidYtelseGrunnlag::getEksternReferanse).orElse(null));

        totrinnTjeneste.lagreNyttTotrinnresultat(totrinnsresultatgrunnlag);
    }

}
