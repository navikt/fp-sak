package no.nav.foreldrepenger.mottak.hendelser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødfødselHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.UtflyttingHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.freg.DødForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.freg.DødfødselForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.freg.FødselForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.freg.UtflyttingForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererProvider;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.ForretningshendelseSaksvelgerProvider;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.task.OppdaterBehandlendeEnhetTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class ForretningshendelseMottak {

    private static final boolean IS_PROD = Environment.current().isProd();
    // Setter kjøring av mottak litt etter at Oppdrag har åpnet for business kl 06:00.
    private static final LocalTime OPPDRAG_VÅKNER = LocalTime.of(6, 30);

    private static final Map<ForretningshendelseType, Function<HendelseDto , ? extends Forretningshendelse>> OVERSETTER = Map.of(
        ForretningshendelseType.DØD, d -> new DødForretningshendelse(mapToAktørIds(d), ((DødHendelseDto)d).getDødsdato(), getEndringstype(d)),
        ForretningshendelseType.DØDFØDSEL, d -> new DødfødselForretningshendelse(mapToAktørIds(d), ((DødfødselHendelseDto)d).getDødfødselsdato(), getEndringstype(d)),
        ForretningshendelseType.FØDSEL, f -> new FødselForretningshendelse(mapToAktørIds(f), ((FødselHendelseDto)f).getFødselsdato(), getEndringstype(f)),
        ForretningshendelseType.UTFLYTTING, f -> new UtflyttingForretningshendelse(mapToAktørIds(f), ((UtflyttingHendelseDto)f).getUtflyttingsdato(), getEndringstype(f))
    );

    private static final Logger LOG = LoggerFactory.getLogger(ForretningshendelseMottak.class);

    private ForretningshendelseHåndtererProvider håndtererProvider;
    private ForretningshendelseSaksvelgerProvider saksvelgerProvider;
    private FagsakRepository fagsakRepository;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    private ProsessTaskTjeneste taskTjeneste;
    private KøKontroller køKontroller;

    ForretningshendelseMottak() {
        //for CDI proxy
    }

    @Inject
    public ForretningshendelseMottak(ForretningshendelseHåndtererProvider håndtererProvider,
                                     ForretningshendelseSaksvelgerProvider saksvelgerProvider,
                                     BehandlingRepositoryProvider repositoryProvider,
                                     BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                                     FagsakEgenskapRepository fagsakEgenskapRepository,
                                     ProsessTaskTjeneste taskTjeneste,
                                     KøKontroller køKontroller) {
        this.håndtererProvider = håndtererProvider;
        this.saksvelgerProvider = saksvelgerProvider;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.taskTjeneste = taskTjeneste;
        this.køKontroller = køKontroller;
    }

    /**
    * 1. steg av håndtering av mottatt forretningshendelse. Identifiserer fagsaker som er kandidat for revurdering.
    */
    public void mottaForretningshendelse(ForretningshendelseType hendelseType, HendelseDto dto) {
        var forretningshendelse = OVERSETTER.get(hendelseType).apply(dto);
        var saksvelger = saksvelgerProvider.finnSaksvelger(hendelseType);

        var fagsaker = saksvelger.finnRelaterteFagsaker(forretningshendelse);
        var taskGruppe = new ProsessTaskGruppe();
        if (ForretningshendelseType.UTFLYTTING.equals(hendelseType) && Endringstype.OPPRETTET.equals(getEndringstype(dto))) {
            merkUtlandsSakerFlyttBehandlinger(fagsaker.getOrDefault(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING, List.of()), taskGruppe);
        }
        for (var entry : fagsaker.entrySet()) {
            entry.getValue().forEach(fagsak -> taskGruppe.addNesteSekvensiell(opprettProsesstaskForFagsak(fagsak, hendelseType, entry.getKey())));
        }
        if (!taskGruppe.getTasks().isEmpty()) {
            taskTjeneste.lagre(taskGruppe);
        }
    }

    /**
     * 2. steg av håndtering av mottatt forretningshendelse. Hendelsen på fagsaken brukes som TRIGGER ift. protokoll
     * for mottak av hendelser på fagsak/behandling
     */
    public void håndterHendelsePåFagsak(Long fagsakId, ForretningshendelseType hendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        Objects.requireNonNull(fagsakId);

        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var håndterer = håndtererProvider.finnHåndterer(hendelseType, fagsak.getYtelseType());

        // Hent siste ytelsebehandling
        var sisteYtelsebehandling = behandlingRevurderingTjeneste.hentAktivIkkeBerørtEllerSisteYtelsesbehandling(fagsakId).orElse(null);

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

        var sisteInnvilgedeYtelsesbehandling = behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId());

        // Case 4: Ytelsesbehandling finnes, men verken åpen eller innvilget. Antas å inntreffe sjelden
        if (sisteInnvilgedeYtelsesbehandling.isEmpty()) {
            LOG.info("FP-524248 Det finnes fagsak for ytelsesbehandling, men ingen åpen eller innvilget ytelsesesbehandling. Gjelder forretningshendelse '{}' på fagsakId {}.", hendelseType.getKode(), fagsakId);
            return;
        }
        var innvilgetBehandling = sisteInnvilgedeYtelsesbehandling.get();

        // Case 5: Avsluttet eller iverksatt ytelsesbehandling
        håndterer.håndterAvsluttetBehandling(innvilgetBehandling, hendelseType, behandlingÅrsakType);
    }

    private ProsessTaskData opprettProsesstaskForFagsak(Fagsak fagsak, ForretningshendelseType hendelseType, BehandlingÅrsakType behandlingÅrsakType) {
        var taskData = ProsessTaskData.forProsessTask(MottaHendelseFagsakTask.class);
        taskData.setProperty(MottaHendelseFagsakTask.PROPERTY_HENDELSE_TYPE, hendelseType.getKode());
        taskData.setProperty(MottaHendelseFagsakTask.PROPERTY_ÅRSAK_TYPE, behandlingÅrsakType.getKode());
        taskData.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        if (IS_PROD) {
            // Unngå samtidighetsproblemer dersom begge foreldres saker revurderes pga hendelse
            var tidstillegg = !RelasjonsRolleType.erMor(fagsak.getRelasjonsRolleType()) ? 45 : 0;
            if (LocalTime.now().isBefore(OPPDRAG_VÅKNER)) {
                // Porsjoner utover neste 29 min
                var tidspunkt = OPPDRAG_VÅKNER.plusSeconds(LocalDateTime.now().getNano() % 1739).plusSeconds(tidstillegg);
                taskData.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), tidspunkt));
            } else {
                taskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(tidstillegg));
            }
        }
        return taskData;
    }

    private static List<AktørId> mapToAktørIds(HendelseDto hendelseDto) {
        return hendelseDto.getAlleAktørId().stream().map(AktørId::new).toList();
    }

    private static Endringstype getEndringstype(HendelseDto d) {
        return Endringstype.valueOf(d.getEndringstype().name());
    }

    private void merkUtlandsSakerFlyttBehandlinger(List<Fagsak> saker, ProsessTaskGruppe taskGruppe) {
        saker.forEach(f -> fagsakEgenskapRepository.leggTilFagsakMarkering(f.getId(), FagsakMarkering.BOSATT_UTLAND));
        var åpneBehandlingerFlyttes = saker.stream()
            .map(f -> behandlingRepository.hentÅpneBehandlingerForFagsakId(f.getId()))
            .flatMap(Collection::stream)
            .filter(b -> BehandlendeEnhetTjeneste.getNasjonalEnhet().equals(b.getBehandlendeOrganisasjonsEnhet()))
            .toList();
        // Bytt enhet ved utland for åpne behandlinger - vil sørge for å oppdatere oppgaver
        åpneBehandlingerFlyttes.stream().map(this::opprettOppdaterEnhetTask).forEach(taskGruppe::addNesteSekvensiell);
        // Oppdater LOS-oppgaver (blir nødvendig ved sentralisering av spesialenheter)
        //fagsakTjeneste.hentBehandlingerMedÅpentAksjonspunkt(fagsak).stream().map(this::opprettLosProsessTask).forEach(taskGruppe::addNesteSekvensiell);
    }

    private ProsessTaskData opprettOppdaterEnhetTask(Behandling behandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OppdaterBehandlendeEnhetTask.class);
        prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        return prosessTaskData;
    }
}
