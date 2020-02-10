package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;

@ApplicationScoped
public class OpprettToTrinnsgrunnlag {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private UttakRepository uttakRepository;
    private TotrinnTjeneste totrinnTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    OpprettToTrinnsgrunnlag() {
        // CDI
    }

    @Inject
    public OpprettToTrinnsgrunnlag(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                           YtelsesFordelingRepository ytelsesFordelingRepository,
                           UttakRepository uttakRepository,
                           TotrinnTjeneste totrinnTjeneste,
                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.uttakRepository = uttakRepository;
        this.iayTjeneste = iayTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    public void settNyttTotrinnsgrunnlag(Behandling behandling) {
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId())
                .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        Optional<Long> ytelseFordelingIdOpt = ytelsesFordelingRepository.hentIdPåAktivYtelsesFordeling(behandling.getId());
        Optional<UttakResultatEntitet> uttakResultatOpt = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = iayTjeneste.finnGrunnlag(behandling.getId());

        Totrinnresultatgrunnlag totrinnsresultatgrunnlag = new Totrinnresultatgrunnlag(behandling,
            ytelseFordelingIdOpt.orElse(null),
            uttakResultatOpt.map(UttakResultatEntitet::getId).orElse(null),
            beregningsgrunnlagOpt.map(BeregningsgrunnlagEntitet::getId).orElse(null),
            iayGrunnlagOpt.map(InntektArbeidYtelseGrunnlag::getEksternReferanse).orElse(null));

        totrinnTjeneste.lagreNyttTotrinnresultat(behandling, totrinnsresultatgrunnlag);
    }

}
