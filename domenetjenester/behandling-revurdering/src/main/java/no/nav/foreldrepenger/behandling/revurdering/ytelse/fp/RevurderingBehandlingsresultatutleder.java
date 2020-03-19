package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.time.LocalDate;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIUttakFraEndringsdato;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak;
import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFellesImpl;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
@FagsakYtelseTypeRef("FP")
@BehandlingTypeRef("BT-004")
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFellesImpl {

    private UttakRepository uttakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private HarEtablertYtelse harEtablertYtelse;

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider,  // NOSONAR
                                                 HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                 OpphørUttakTjeneste opphørUttakTjeneste,
                                                 @FagsakYtelseTypeRef("FP") HarEtablertYtelse harEtablertYtelse,
                                                 @FagsakYtelseTypeRef("FP") ErEndringIUttakFraEndringsdato erEndringIUttakFraEndringsdato,
                                                 @FagsakYtelseTypeRef("FP") ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
                                                 @FagsakYtelseTypeRef("FP") SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                 MedlemTjeneste medlemTjeneste) {
        super(repositoryProvider,
            beregningsgrunnlagTjeneste,
            medlemTjeneste,
            opphørUttakTjeneste,
            erEndringIUttakFraEndringsdato,
            erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
            skjæringstidspunktTjeneste
        );
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.harEtablertYtelse = harEtablertYtelse;
    }

    @Override
    protected UttakResultatHolder getUttakResultat(Long behandlingId) {
        return new UttakResultatHolderImpl(uttakRepository.hentUttakResultatHvisEksisterer(behandlingId));
    }

    @Override
    protected HarEtablertYtelse harEtablertYtelse() {
        return harEtablertYtelse;
    }

    @Override
    protected LocalDate finnEndringsdato(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        var aggregatOpt = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
        return aggregatOpt.flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getGjeldendeEndringsdato)
            .orElseThrow();
    }
}
