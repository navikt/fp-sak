package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import java.time.LocalDate;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
@FagsakYtelseTypeRef("SVP")
@BehandlingTypeRef("BT-004")
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFelles {

    private SvangerskapspengerUttakResultatRepository uttakRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingsresultatBeregningTjenesteSVP ugunstTjenesteSVP;

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider, // NOSONAR
                                                 HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                 OpphørUttakTjeneste opphørUttakTjeneste,
                                                 @FagsakYtelseTypeRef("SVP") SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                 MedlemTjeneste medlemTjeneste,
                                                 @FagsakYtelseTypeRef("SVP") BehandlingsresultatBeregningTjenesteSVP ugunstTjenesteSVP) {
        super(repositoryProvider,
                beregningsgrunnlagTjeneste,
                medlemTjeneste,
                opphørUttakTjeneste,
                skjæringstidspunktTjeneste);
        this.uttakRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.ugunstTjenesteSVP = ugunstTjenesteSVP;
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

    @Override
    protected boolean erEndringIBeregning(BehandlingReferanse ref) {
        return ugunstTjenesteSVP.erEndring(ref);
    }

}
