package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.time.LocalDate;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIUttakFraEndringsdato;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak;
import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFellesImpl;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.EndringsdatoRevurderingUtleder;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
@FagsakYtelseTypeRef("FP")
@BehandlingTypeRef("BT-004")
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFellesImpl {

    private UttakRepository uttakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider,  // NOSONAR
                                                 HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                   @FagsakYtelseTypeRef("FP") EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder,
                                                   RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                                   StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                                   OpphørUttakTjeneste opphørUttakTjeneste,
                                                   UttakInputTjeneste uttakInputTjeneste,
                                                   @FagsakYtelseTypeRef("FP") HarEtablertYtelse harEtablertYtelse,
                                                   @FagsakYtelseTypeRef("FP") ErEndringIUttakFraEndringsdato erEndringIUttakFraEndringsdato,
                                                   @FagsakYtelseTypeRef("FP") ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
                                                   @FagsakYtelseTypeRef("FP") SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                   MedlemTjeneste medlemTjeneste) {
        super(repositoryProvider,
            beregningsgrunnlagTjeneste,
            medlemTjeneste,
            opphørUttakTjeneste,
            harEtablertYtelse,
            erEndringIUttakFraEndringsdato,
            erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
            skjæringstidspunktTjeneste
        );
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.endringsdatoRevurderingUtleder = endringsdatoRevurderingUtleder;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
    }

    @Override
    protected UttakResultatHolder getUttakResultat(Long behandlingId) {
        return new UttakResultatHolderImpl(uttakRepository.hentUttakResultatHvisEksisterer(behandlingId));
    }

    @Override
    protected boolean erSluttPåStønadsdager(Behandling behandling) {
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        return stønadskontoSaldoTjeneste.erSluttPåStønadsdager(uttakInput);
    }

    @Override
    protected UttakResultatHolder getAnnenPartUttak(Saksnummer saksnummer) {
        return new UttakResultatHolderImpl(relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattUttaksplan(saksnummer)
            .filter(this::erTilknyttetLøpendeFagsak));
    }

    @Override
    protected LocalDate finnEndringsdato(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        var aggregatOpt = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
        return aggregatOpt.flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getGjeldendeEndringsdato)
            .orElseGet(() -> {
                return endringsdatoRevurderingUtleder.utledEndringsdato(uttakInputTjeneste.lagInput(behandlingId));
            });
    }

    private boolean erTilknyttetLøpendeFagsak(UttakResultatEntitet uttakResultatEntitet) {
        return uttakResultatEntitet.getBehandlingsresultat().getBehandling().getFagsak().getStatus().equals(FagsakStatus.LØPENDE);
    }
}
