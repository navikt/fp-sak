package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.util.Comparator;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@Dependent
@ProsessTask("behandlingsprosess.esregulering.reguler")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class EngangsstønadReguleringTask extends FagsakProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(EngangsstønadReguleringTask.class);

    private final RevurderingTjeneste revurderingTjeneste;
    private final LegacyESBeregningRepository esBeregningRepository;
    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private final PersoninfoAdapter personinfoAdapter;
    private final BehandlingRepository behandlingRepository;
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
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
        this.esBeregningRepository = esBeregningRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        LOG.info("ESregulering fagsak med fagsakId = {}", fagsakId);

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) ||
            behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsakId)) {
            return;
        }

        familieHendelseTjeneste.finnAggregat(behandling.getId())
            .flatMap(g -> utledRevurderingsbehovMedÅrsak(behandling, g))
            .ifPresent(årsak -> {
                var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
                opprettRevurdering(behandling, årsak, enhet);
            });
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
        var fødselsdato = personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(behandling.getAktørId(), intervaller).stream()
            .map(FødtBarnInfo::fødselsdato).max(Comparator.naturalOrder()).orElse(null);
        if (fødselsdato != null && esBeregningRepository.skalReberegne(behandling.getId(), fødselsdato)) {
            return Optional.of(BehandlingÅrsakType.RE_SATS_REGULERING);
        }
        return Optional.empty();
    }

    private void opprettRevurdering(Behandling behandling, BehandlingÅrsakType årsak, OrganisasjonsEnhet enhetForRevurdering) {
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(behandling.getFagsak(), årsak, enhetForRevurdering);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
    }

}
