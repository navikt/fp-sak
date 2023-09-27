package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.task;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.foreldrepenger.konfig.Cluster;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.PersonoversiktBehandlingStatusDto;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.PersonoversiktHendelseProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
@ProsessTask("behandlingskontroll.oppdatersakogbehandling")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SakOgBehandlingTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(SakOgBehandlingTask.class);
    private static final Cluster CLUSTER = Environment.current().getCluster();

    private PersonoversiktHendelseProducer aivenProducer;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersoninfoAdapter personinfoAdapter;


    SakOgBehandlingTask() {
        //for CDI proxy
    }

    @Inject
    public SakOgBehandlingTask(PersonoversiktHendelseProducer aivenProducer,
                               PersoninfoAdapter personinfoAdapter,
                               BehandlingRepositoryProvider repositoryProvider) {
        super();
        this.aivenProducer = aivenProducer;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandlingReadOnly(behandlingId);
        if (Set.of(BehandlingStatus.FATTER_VEDTAK, BehandlingStatus.IVERKSETTER_VEDTAK).contains(behandling.getStatus()))
            return;
        final LocalDateTime tidspunkt;
        if (BehandlingStatus.AVSLUTTET.equals(behandling.getStatus())) {
            tidspunkt = behandling.getAvsluttetDato();
        } else {
            tidspunkt = BehandlingStatus.OPPRETTET.equals(behandling.getStatus()) ? behandling.getOpprettetDato() : LocalDateTime.now();
        }
        try {
            var behandlingTema = BehandlingTema.fraFagsak(behandling.getFagsak(), familieHendelseRepository
                .hentAggregatHvisEksisterer(behandlingId).map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null));
            var erAvsluttet = behandling.erAvsluttet();

            var callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();

            LOG.info("SOBKAFKA sender behandlingsstatus {} for id {}", behandling.getStatus().getKode(), behandling.getId());

            var ident = personinfoAdapter.hentFnr(behandling.getAktørId()).orElse(null);
            var personSoB = erAvsluttet ? new PersonoversiktBehandlingStatusDto.PersonoversiktBehandlingAvsluttetDto(callId, behandling, tidspunkt, behandlingTema, ident) :
                new PersonoversiktBehandlingStatusDto.PersonoversiktBehandlingOpprettetDto(callId, behandling, tidspunkt, behandlingTema, ident);
            aivenProducer.sendJsonMedNøkkel(createUniqueKey(String.valueOf(behandling.getId()), behandling.getStatus().getKode()), StandardJsonConfig.toJson(personSoB));
        } catch (Exception e) {
            LOG.info("SOBKAFKA noe gikk feil for behandling {}", behandlingId, e);
            if (CLUSTER.isProd())
                throw e;
        }
    }

    private String createUniqueKey(String behandlingsId, String event) {
        return String.format("%s_%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandlingsId, event);
    }

}
