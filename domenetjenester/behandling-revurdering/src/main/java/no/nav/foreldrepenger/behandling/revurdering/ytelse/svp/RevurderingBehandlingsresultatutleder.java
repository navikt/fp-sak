package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import java.time.LocalDate;

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
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFelles {

    private SvangerskapspengerUttakResultatRepository uttakRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider,
            SvangerskapspengerUttakResultatRepository uttakResultatRepository,
            HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
            OpphørUttakTjeneste opphørUttakTjeneste,
            @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            MedlemTjeneste medlemTjeneste) {
        super(repositoryProvider,
                beregningsgrunnlagTjeneste,
                medlemTjeneste,
                opphørUttakTjeneste,
                skjæringstidspunktTjeneste);
        this.uttakRepository = uttakResultatRepository;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    @Override
    protected UttakResultatHolder getUttakResultat(Long behandlingId) {
        var behandlingVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        return new UttakResultatHolderSVP(uttakRepository.hentHvisEksisterer(behandlingId), behandlingVedtak.orElse(null));
    }

    @Override
    protected boolean harEtablertYtelse(Behandling revurdering, boolean finnesInnvilgetIkkeOpphørtVedtak,
            UttakResultatHolder uttakresultatOriginal) {
        if (LocalDate.now().isAfter(uttakresultatOriginal.getSisteDagAvSistePeriode())) {
            return false;
        }
        return finnesInnvilgetIkkeOpphørtVedtak;
    }
}
