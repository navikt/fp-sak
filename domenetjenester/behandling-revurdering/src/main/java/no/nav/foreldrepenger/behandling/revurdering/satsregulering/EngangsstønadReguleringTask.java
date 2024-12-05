package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@Dependent
@ProsessTask(value = "behandlingsprosess.esregulering.reguler", prioritet = 3)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class EngangsstønadReguleringTask extends FagsakProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(EngangsstønadReguleringTask.class);

    private final RevurderingTjeneste revurderingTjeneste;
    private final LegacyESBeregningRepository esBeregningRepository;
    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private final PersoninfoAdapter personinfoAdapter;
    private final BehandlingRepository behandlingRepository;
    private final FagsakRepository fagsakRepository;
    private final FamilieHendelseTjeneste familieHendelseTjeneste;
    private final BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @Inject
    public EngangsstønadReguleringTask(BehandlingRepositoryProvider repositoryProvider,
                                       FamilieHendelseTjeneste familieHendelseTjeneste,
                                       PersoninfoAdapter personinfoAdapter,
                                       BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                       LegacyESBeregningRepository esBeregningRepository,
                                       BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                       @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) RevurderingTjeneste revurderingTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
        this.esBeregningRepository = esBeregningRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId) {
        LOG.info("ESregulering fagsak med fagsakId = {}", fagsakId);

        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        if (!FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType()) ||
            behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsakId)) {
            return;
        }

        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).orElseThrow();

        familieHendelseTjeneste.finnAggregat(behandling.getId())
            .flatMap(g -> utledRevurderingsbehovMedÅrsak(behandling, g))
            .ifPresent(årsak -> opprettRevurdering(fagsak, årsak));
    }

    public Optional<BehandlingÅrsakType> utledRevurderingsbehovMedÅrsak(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag) {
        var bekreftetBarn = grunnlag.getGjeldendeBekreftetVersjon()
            .filter(fh -> FamilieHendelseType.FØDSEL.equals(fh.getType()) || fh.getGjelderAdopsjon())
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
            .filter(fhdato -> esBeregningRepository.skalReberegne(behandling.getId(), fhdato))
            .isPresent();
        if (bekreftetBarn) {
            return Optional.of(BehandlingÅrsakType.RE_SATS_REGULERING);
        }
        var intervaller = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandling));
        var fødselsdato = personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(behandling.getFagsakYtelseType(), behandling.getAktørId(), intervaller).stream()
            .map(FødtBarnInfo::fødselsdato).max(Comparator.naturalOrder()).orElse(null);
        if (fødselsdato != null && esBeregningRepository.skalReberegne(behandling.getId(), fødselsdato)) {
            return Optional.of(BehandlingÅrsakType.RE_SATS_REGULERING);
        }
        return Optional.empty();
    }

    private void opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType årsak) {
        var enhetForRevurdering = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, årsak, enhetForRevurdering);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
    }

}
