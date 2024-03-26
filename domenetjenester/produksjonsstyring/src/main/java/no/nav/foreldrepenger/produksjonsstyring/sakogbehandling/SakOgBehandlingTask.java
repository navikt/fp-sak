package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import java.time.LocalDateTime;
import java.time.Month;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
@ProsessTask("behandlingskontroll.oppdatersakogbehandling")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SakOgBehandlingTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(SakOgBehandlingTask.class);

    private static final LocalDateTime VINDU_START = LocalDateTime.of(2023, Month.SEPTEMBER, 27, 10, 0);
    private static final LocalDateTime VINDU_SLUTT = LocalDateTime.of(2023, Month.SEPTEMBER, 30, 12, 0);

    private PersonoversiktHendelseProducer hendelseProducer;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersoninfoAdapter personinfoAdapter;


    SakOgBehandlingTask() {
        //for CDI proxy
    }

    @Inject
    public SakOgBehandlingTask(PersonoversiktHendelseProducer hendelseProducer,
                               PersoninfoAdapter personinfoAdapter,
                               BehandlingRepositoryProvider repositoryProvider) {
        super();
        this.hendelseProducer = hendelseProducer;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandlingReadOnly(behandlingId);
        if (behandling.getOpprettetTidspunkt().isAfter(VINDU_START) && behandling.getOpprettetTidspunkt().isBefore(VINDU_SLUTT)) {
            sendMelding(behandling, "behandlingOpprettet", behandling.getOpprettetTidspunkt(), false);
        }
        if (!BehandlingStatus.AVSLUTTET.equals(behandling.getStatus())) return;
        if (behandling.getEndretTidspunkt().isAfter(VINDU_START) && behandling.getEndretTidspunkt().isBefore(VINDU_SLUTT)) {
            sendMelding(behandling, "behandlingAvsluttet", behandling.getEndretTidspunkt(), true);
        }
    }

    private void sendMelding(Behandling behandling, String hendelse, LocalDateTime tidspunkt, boolean avsluttet) {
        var behandlingTema = BehandlingTema.fraFagsak(behandling.getFagsak(), familieHendelseRepository
            .hentAggregatHvisEksisterer(behandling.getId()).map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null));

        var callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();

        LOG.info("SOBKAFKA sender behandlingsstatus {} for id {}", hendelse, behandling.getId());

        var ident = personinfoAdapter.hentFnr(behandling.getAktørId()).orElse(null);
        var personSoB = PersonoversiktBehandlingStatusDto.lagPersonoversiktBehandlingStatusDto(hendelse, callId, behandling, tidspunkt, behandlingTema, ident, avsluttet);
        hendelseProducer.sendJsonMedNøkkel(createUniqueKey(String.valueOf(behandling.getId()), behandling.getStatus().getKode()), StandardJsonConfig.toJson(personSoB));
    }

    private String createUniqueKey(String behandlingsId, String event) {
        return String.format("%s_%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandlingsId, event);
    }

}
