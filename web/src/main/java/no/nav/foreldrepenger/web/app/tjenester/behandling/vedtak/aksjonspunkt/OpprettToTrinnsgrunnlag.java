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
        var beregningsgrunnlagOpt = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId())
                .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        var ytelseFordelingIdOpt = ytelsesFordelingRepository.hentIdPåAktivYtelsesFordeling(behandling.getId());
        var uttakResultatOpt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        var iayGrunnlagOpt = iayTjeneste.finnGrunnlag(behandling.getId());

        var totrinnsresultatgrunnlag = new Totrinnresultatgrunnlag(behandling,
            ytelseFordelingIdOpt.orElse(null),
            uttakResultatOpt.map(UttakResultatEntitet::getId).orElse(null),
            beregningsgrunnlagOpt.map(BeregningsgrunnlagEntitet::getId).orElse(null),
            iayGrunnlagOpt.map(InntektArbeidYtelseGrunnlag::getEksternReferanse).orElse(null));

        totrinnTjeneste.lagreNyttTotrinnresultat(behandling, totrinnsresultatgrunnlag);
    }

    public void settNyttTotrinnsgrunnlag(Behandling behandling, Long uttakId) {
        var grunnlag = totrinnTjeneste.hentTotrinngrunnlagHvisEksisterer(behandling).orElseThrow();

        var totrinnsresultatgrunnlag = new Totrinnresultatgrunnlag(behandling,
            grunnlag.getYtelseFordelingGrunnlagEntitetId().orElse(null),
            uttakId,
            grunnlag.getBeregningsgrunnlagId().orElse(null),
            grunnlag.getGrunnlagUuid().orElse(null));

        totrinnTjeneste.lagreNyttTotrinnresultat(behandling, totrinnsresultatgrunnlag);
    }

}
