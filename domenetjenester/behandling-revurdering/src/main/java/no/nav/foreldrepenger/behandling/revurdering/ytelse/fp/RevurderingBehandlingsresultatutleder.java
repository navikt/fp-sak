package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFelles {

    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private HarEtablertYtelseFP harEtablertYtelse;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider, // NOSONAR
            HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
            OpphørUttakTjeneste opphørUttakTjeneste,
            HarEtablertYtelseFP harEtablertYtelse,
            @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            MedlemTjeneste medlemTjeneste,
            ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        super(repositoryProvider,
                beregningsgrunnlagTjeneste,
                medlemTjeneste,
                opphørUttakTjeneste,
                skjæringstidspunktTjeneste);
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.harEtablertYtelse = harEtablertYtelse;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    @Override
    protected UttakResultatHolder getUttakResultat(Long behandlingId) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        return new UttakResultatHolderFP(foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandlingId), vedtak.orElse(null));
    }

    @Override
    protected boolean harEtablertYtelse(Behandling revurdering, boolean finnesInnvilgetIkkeOpphørtVedtak,
            UttakResultatHolder uttakresultatOriginal) {
        return harEtablertYtelse.vurder(revurdering, finnesInnvilgetIkkeOpphørtVedtak, uttakresultatOriginal);
    }
}
