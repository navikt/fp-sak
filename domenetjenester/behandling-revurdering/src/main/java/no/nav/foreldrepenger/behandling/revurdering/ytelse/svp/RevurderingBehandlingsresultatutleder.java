package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFelles {

    private final SvangerskapspengerUttakResultatRepository uttakRepository;
    private final BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider,
                                                 BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                                                 SvangerskapspengerUttakResultatRepository uttakResultatRepository,
                                                 BeregningTjeneste beregningTjeneste,
                                                 OpphørUttakTjeneste opphørUttakTjeneste,
                                                 @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                 MedlemTjeneste medlemTjeneste,
                                                 DekningsgradTjeneste dekningsgradTjeneste) {
        super(repositoryProvider, grunnlagRepositoryProvider, beregningTjeneste, medlemTjeneste, opphørUttakTjeneste, skjæringstidspunktTjeneste,
            dekningsgradTjeneste);
        this.uttakRepository = uttakResultatRepository;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    @Override
    protected UttakResultatHolder getUttakResultat(Long behandlingId) {
        var behandlingVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        return new UttakResultatHolderSVP(uttakRepository.hentHvisEksisterer(behandlingId), behandlingVedtak.orElse(null));
    }

}
