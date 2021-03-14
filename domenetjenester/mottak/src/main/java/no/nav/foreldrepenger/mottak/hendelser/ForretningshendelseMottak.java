package no.nav.foreldrepenger.mottak.hendelser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødForretningshendelse;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødfødselForretningshendelse;
import no.nav.foreldrepenger.familiehendelse.fødsel.FødselForretningshendelse;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødfødselHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererProvider;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.ForretningshendelseSaksvelgerProvider;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class ForretningshendelseMottak {

    // Setter kjøring av mottak litt etter at Oppdrag har åpnet for business kl 06:00.
    private static final LocalTime OPPDRAG_VÅKNER = LocalTime.of(6, 30);

    private static final Map<ForretningshendelseType, Function<HendelseDto , ? extends Forretningshendelse>> OVERSETTER = Map.of(
        ForretningshendelseType.DØD, d -> new DødForretningshendelse(mapToAktørIds(d), ((DødHendelseDto)d).getDødsdato(), getEndringstype(d)),
        ForretningshendelseType.DØDFØDSEL, d -> new DødfødselForretningshendelse(mapToAktørIds(d), ((DødfødselHendelseDto)d).getDødfødselsdato(), getEndringstype(d)),
        ForretningshendelseType.FØDSEL, f -> new FødselForretningshendelse(mapToAktørIds(f), ((FødselHendelseDto)f).getFødselsdato(), getEndringstype(f))
    );

    private Logger LOG = LoggerFactory.getLogger(getClass());

    private ForretningshendelseHåndtererProvider håndtererProvider;
    private ForretningshendelseSaksvelgerProvider saksvelgerProvider;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingRevurderingRepository revurderingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private KøKontroller køKontroller;
    private boolean isProd;

    ForretningshendelseMottak() {
        //for CDI proxy
    }

    @Inject
    public ForretningshendelseMottak(ForretningshendelseHåndtererProvider håndtererProvider,
                                     ForretningshendelseSaksvelgerProvider saksvelgerProvider,
                                     BehandlingRepositoryProvider repositoryProvider,
                                     ProsessTaskRepository prosessTaskRepository,
                                     KøKontroller køKontroller) {
        this.håndtererProvider = håndtererProvider;
        this.saksvelgerProvider = saksvelgerProvider;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.køKontroller = køKontroller;
        this.isProd = Environment.current().isProd();
    }

    /**
    * 1. steg av håndtering av mottatt forretningshendelse. Identifiserer fagsaker som er kandidat for revurdering.
    */
    public void mottaForretningshendelse(ForretningshendelseType hendelseType, HendelseDto dto) {
        Forretningshendelse forretningshendelse = OVERSETTER.get(hendelseType).apply(dto);
        ForretningshendelseSaksvelger<Forretningshendelse> saksvelger = saksvelgerProvider.finnSaksvelger(hendelseType);

        Map<BehandlingÅrsakType, List<Fagsak>> fagsaker = saksvelger.finnRelaterteFagsaker(forretningshendelse);
        for (Map.Entry<BehandlingÅrsakType, List<Fagsak>> entry : fagsaker.entrySet()) {
            entry.getValue().forEach(fagsak -> opprettProsesstaskForFagsak(fagsak, hendelseType.getKode(), entry.getKey()));
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

        // Case 2: Berørt (køet) behandling eller behandling på medforelder
        if (køKontroller.skalEvtNyBehandlingKøes(fagsak)) {
            håndterer.håndterKøetBehandling(fagsak, behandlingÅrsakType);
            return;
        }

        // Case 3: Åpen ytelsesbehandling
        if (!sisteYtelsebehandling.erStatusFerdigbehandlet()) {
            håndterer.håndterÅpenBehandling(sisteYtelsebehandling, behandlingÅrsakType);
            return;
        }

        Optional<Behandling> sisteInnvilgedeYtelsesbehandling = behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId());

        // Case 4: Ytelsesbehandling finnes, men verken åpen eller innvilget. Antas å inntreffe sjelden
        if (sisteInnvilgedeYtelsesbehandling.isEmpty()) {
            LOG.info("FP-524248 Det finnes fagsak for ytelsesbehandling, men ingen åpen eller innvilget ytelsesesbehandling. Gjelder forretningshendelse '{}' på fagsakId {}.", hendelseType.getKode(), fagsakId);
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
        if (isProd && LocalTime.now().isBefore(OPPDRAG_VÅKNER)) {
            // Porsjoner utover neste 7 min
            taskData.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), OPPDRAG_VÅKNER.plusSeconds(LocalDateTime.now().getNano() % 1739)));
        }
        prosessTaskRepository.lagre(taskData);
    }

    private static List<AktørId> mapToAktørIds(HendelseDto hendelseDto) {
        return hendelseDto.getAlleAktørId().stream().map(AktørId::new).collect(Collectors.toList());
    }

    private static Endringstype getEndringstype(HendelseDto d) {
        return Endringstype.valueOf(d.getEndringstype().name());
    }
}
