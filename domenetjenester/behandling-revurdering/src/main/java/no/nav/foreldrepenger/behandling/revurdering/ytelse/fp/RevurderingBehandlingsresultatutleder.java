package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIUttak;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak;
import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFellesImpl;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
@FagsakYtelseTypeRef("FP")
@BehandlingTypeRef("BT-004")
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFellesImpl {

    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private HarEtablertYtelse harEtablertYtelse;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider,  // NOSONAR
                                                 HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                 OpphørUttakTjeneste opphørUttakTjeneste,
                                                 @FagsakYtelseTypeRef("FP") HarEtablertYtelse harEtablertYtelse,
                                                 @FagsakYtelseTypeRef("FP") ErEndringIUttak erEndringIUttak,
                                                 @FagsakYtelseTypeRef("FP") ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
                                                 @FagsakYtelseTypeRef("FP") SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                 MedlemTjeneste medlemTjeneste,
                                                 ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        super(repositoryProvider,
            beregningsgrunnlagTjeneste,
            medlemTjeneste,
            opphørUttakTjeneste,
            erEndringIUttak,
            erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
            skjæringstidspunktTjeneste
        );
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.harEtablertYtelse = harEtablertYtelse;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    @Override
    protected UttakResultatHolder getUttakResultat(Long behandlingId) {
        var vedtak = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandlingId);
        return new UttakResultatHolderImpl(foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandlingId), vedtak.orElse(null));
    }

    @Override
    protected HarEtablertYtelse harEtablertYtelse() {
        return harEtablertYtelse;
    }
}
