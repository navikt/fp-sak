package no.nav.foreldrepenger.mottak.hendelser;

import static no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseMottakFeil.FEILFACTORY;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererProvider;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.ForretningshendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.oversetter.ForretningshendelseOversetterProvider;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.ForretningshendelseSaksvelgerProvider;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class ForretningshendelseMottak {

    // Setter kjøring av mottak litt etter at Oppdrag har åpnet for business kl 06:00.
    private static final LocalTime OPPDRAG_VÅKNER = LocalTime.of(6, 30);
    private Logger logger = LoggerFactory.getLogger(getClass());

    private ForretningshendelseOversetterProvider oversetterProvider;
    private ForretningshendelseHåndtererProvider håndtererProvider;
    private ForretningshendelseSaksvelgerProvider saksvelgerProvider;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingRevurderingRepository revurderingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private KøKontroller køKontroller;

    ForretningshendelseMottak() {
        //for CDI proxy
    }

    @Inject
    public ForretningshendelseMottak(ForretningshendelseOversetterProvider oversetterProvider,
                                     ForretningshendelseHåndtererProvider håndtererProvider,
                                     ForretningshendelseSaksvelgerProvider saksvelgerProvider,
                                     BehandlingRepositoryProvider repositoryProvider,
                                     ProsessTaskRepository prosessTaskRepository,
                                     KøKontroller køKontroller) {
        this.oversetterProvider = oversetterProvider;
        this.håndtererProvider = håndtererProvider;
        this.saksvelgerProvider = saksvelgerProvider;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.køKontroller = køKontroller;
    }

    /**
    * 1. steg av håndtering av mottatt forretningshendelse. Identifiserer fagsaker som er kandidat for revurdering.
    */
    public void mottaForretningshendelse(ForretningshendelseDto forretningshendelseDto) {
        String hendelseKode = forretningshendelseDto.getForretningshendelseType();
        ForretningshendelseType forretningshendelseType = ForretningshendelseType.fraKode(hendelseKode);
        if (forretningshendelseType == null) {
            FEILFACTORY.ukjentForretningshendelse(hendelseKode).log(logger);
            return;
        }
        Forretningshendelse forretningshendelse = oversetterProvider.finnOversetter(forretningshendelseType).oversett(forretningshendelseDto);
        ForretningshendelseSaksvelger<Forretningshendelse> saksvelger = saksvelgerProvider.finnSaksvelger(forretningshendelseType);

        Map<BehandlingÅrsakType, List<Fagsak>> fagsaker = saksvelger.finnRelaterteFagsaker(forretningshendelse);
        for (Map.Entry<BehandlingÅrsakType, List<Fagsak>> entry : fagsaker.entrySet()) {
            entry.getValue().forEach(fagsak -> opprettProsesstaskForFagsak(fagsak, hendelseKode, entry.getKey()));
        }
    }

    /**
     * 2. steg av håndtering av mottatt forretningshendelse. Hendelsen på fagsaken brukes som TRIGGER ift. protokoll
     * for mottak av hendelser på fagsak/behandling
     */
    public void håndterHendelsePåFagsak(Long fagsakId, String hendelseTypeKode, String årsakTypeKode) {
        Objects.requireNonNull(hendelseTypeKode);
        Objects.requireNonNull(fagsakId);
        Objects.requireNonNull(årsakTypeKode);

        ForretningshendelseType hendelseType = ForretningshendelseType.fraKode(hendelseTypeKode);
        BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.fraKode(årsakTypeKode);

        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        ForretningshendelseHåndterer håndterer = håndtererProvider.finnHåndterer(hendelseType, fagsak.getYtelseType());

        // Hent siste ytelsebehandling
        Behandling sisteYtelsebehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId())
            .orElse(null);

        // Case 1: Ingen ytelsesbehandling er opprettet på fagsak - hendelse skal ikke opprette noen behandling
        if (sisteYtelsebehandling == null) {
            return;
        }

        // Case 2: Berørt (køet) behandling
        if (behandlingSkalLeggesPåKøPåGrunnAvÅpenBehandlingPåMedforelder(sisteYtelsebehandling)) {
            håndterer.håndterKøetBehandling(fagsak, behandlingÅrsakType);
            return;
        }

        // Case 3: Åpen ytelsesbehandling
        if (!sisteYtelsebehandling.erStatusFerdigbehandlet()) {
            if (erBehandlingBerørt(sisteYtelsebehandling) || harAlleredeKøetBehandling(sisteYtelsebehandling)) {
                håndterer.håndterKøetBehandling(fagsak, behandlingÅrsakType);
                return;
            }
            håndterer.håndterÅpenBehandling(sisteYtelsebehandling, behandlingÅrsakType);
            return;
        }

        Optional<Behandling> sisteInnvilgedeYtelsesbehandling = behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId());

        // Case 4: Ytelsesbehandling finnes, men verken åpen eller innvilget. Antas å inntreffe sjelden
        if (!sisteInnvilgedeYtelsesbehandling.isPresent()) {
            FEILFACTORY.finnesYtelsebehandlingSomVerkenErÅpenEllerInnvilget(hendelseType.getKode(), fagsakId).log(logger);
            return;
        }
        Behandling innvilgetBehandling = sisteInnvilgedeYtelsesbehandling.get();

        // Case 5: Avsluttet eller iverksatt ytelsesbehandling
        håndterer.håndterAvsluttetBehandling(innvilgetBehandling, hendelseType, behandlingÅrsakType);
    }

    private void opprettProsesstaskForFagsak(Fagsak fagsak, String hendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        ProsessTaskData taskData = new ProsessTaskData(MottaHendelseFagsakTask.TASKTYPE);
        taskData.setProperty(MottaHendelseFagsakTask.PROPERTY_HENDELSE_TYPE, hendelseType);
        taskData.setProperty(MottaHendelseFagsakTask.PROPERTY_ÅRSAK_TYPE, behandlingÅrsakType.getKode());
        taskData.setFagsakId(fagsak.getId());
        taskData.setCallIdFraEksisterende();
        if (LocalTime.now().isBefore(OPPDRAG_VÅKNER)) {
            // Porsjoner utover neste 7 min
            taskData.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), OPPDRAG_VÅKNER.plusSeconds(LocalDateTime.now().getNano() % 1739)));
        }
        prosessTaskRepository.lagre(taskData);
    }

    private boolean behandlingSkalLeggesPåKøPåGrunnAvÅpenBehandlingPåMedforelder(Behandling sisteYtelsebehandling) {
        Optional<Behandling> åpenBehandlingPåMedforelder = finnÅpenBehandlingPåMedforelder(sisteYtelsebehandling.getFagsak());
        return åpenBehandlingPåMedforelder.isPresent()
            && !køKontroller.skalSnikeIKø(sisteYtelsebehandling.getFagsak(), åpenBehandlingPåMedforelder.get());
    }

    private Optional<Behandling> finnÅpenBehandlingPåMedforelder(Fagsak fagsak) {
        return revurderingRepository.finnÅpenBehandlingMedforelder(fagsak);
    }

    private boolean erBehandlingBerørt(Behandling behandling) {
        return behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING);
    }

    private boolean harAlleredeKøetBehandling(Behandling behandling) {
        Optional<Behandling> køetBehandling = revurderingRepository.finnKøetYtelsesbehandling(behandling.getFagsakId());
        return køetBehandling.isPresent();
    }
}
