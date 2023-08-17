package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import contract.sob.dto.Aktoer;
import contract.sob.dto.Applikasjoner;
import contract.sob.dto.Avslutningsstatuser;
import contract.sob.dto.BehandlingAvsluttet;
import contract.sob.dto.BehandlingOpprettet;
import contract.sob.dto.Behandlingstemaer;
import contract.sob.dto.Behandlingstyper;
import contract.sob.dto.Sakstemaer;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
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
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.BehandlingStatusDto;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.PersonoversiktHendelseProducer;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.SakOgBehandlingHendelseProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
@ProsessTask("behandlingskontroll.oppdatersakogbehandling")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SakOgBehandlingTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(SakOgBehandlingTask.class);
    private static final Cluster CLUSTER = Environment.current().getCluster();

    private SakOgBehandlingHendelseProducer producer;
    private PersonoversiktHendelseProducer aivenProducer;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersoninfoAdapter personinfoAdapter;


    SakOgBehandlingTask() {
        //for CDI proxy
    }

    @Inject
    public SakOgBehandlingTask(SakOgBehandlingHendelseProducer producer,
                               PersonoversiktHendelseProducer aivenProducer,
                               PersoninfoAdapter personinfoAdapter,
                               BehandlingRepositoryProvider repositoryProvider) {
        super();
        this.producer = producer;
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
        var dto = BehandlingStatusDto.getBuilder()
            .medAktørId(behandling.getAktørId())
            .medBehandlingId(behandlingId)
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingStatus(behandling.getStatus())
            .medBehandlingType(behandling.getType())
            .medBehandlingTema(BehandlingTema.fraFagsak(behandling.getFagsak(), familieHendelseRepository
                .hentAggregatHvisEksisterer(behandlingId).map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null)))
            .medEnhet(behandling.getBehandlendeOrganisasjonsEnhet())
            .medHendelsesTidspunkt(tidspunkt)
            .build();
        try {
            behandlingStatusEndret(dto);
        } catch (Exception e) {
            LOG.info("SOBKAFKA noe gikk feil for behandling {}", behandlingId, e);
            if (Cluster.PROD_FSS.equals(CLUSTER))
                throw e;
        }
    }

    // Skulle i teorien vært egen klasse for å understreke ACL i BehandlingStatusDto - men egenskapen er der.
    private void behandlingStatusEndret(BehandlingStatusDto dto) {
        var erAvsluttet = dto.erBehandlingAvsluttet();

        var callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();

        var builder = erAvsluttet ? BehandlingAvsluttet.builder().avslutningsstatus(Avslutningsstatuser.builder().value("ok").build()) : BehandlingOpprettet.builder();

        builder.sakstema(Sakstemaer.builder().value(Tema.FOR.getOffisiellKode()).build())
            .behandlingstema(Behandlingstemaer.builder().value(dto.getBehandlingTema().getOffisiellKode()).build())
            .behandlingstype(Behandlingstyper.builder().value(dto.getBehandlingType().getOffisiellKode()).build())
            .behandlingsID(createUniqueBehandlingsId(String.valueOf(dto.getBehandlingId())))
            .aktoerREF(List.of(new Aktoer(dto.getAktørId().getId())))
            .ansvarligEnhetREF(dto.getEnhet().enhetId())
            .hendelsesId(callId)
            .hendelsesprodusentREF(Applikasjoner.builder().value(Fagsystem.FPSAK.getOffisiellKode()).build())
            .hendelsesTidspunkt(dto.getHendelsesTidspunkt());

        // OBS setter ikke feltet primaerBehandlingREF - etter diskusjon med SOB og Kvernstuen
        // OBS applikasjonSakREF applikasjonBehandlingREF settes ikke - fordi de ikke var satt i MQ-tiden. Feedback fra SOB

        LOG.info("SOBKAFKA sender behandlingsstatus {}", dto);

        if (Cluster.DEV_FSS.equals(CLUSTER)) {
            try {
                // Ny topic vil gjerne ha FNR siden Infotrygd bruker det
                personinfoAdapter.hentFnr(dto.getAktørId()).ifPresent(ident -> builder.aktoerREF(List.of(new Aktoer(ident.getIdent()))));
                aivenProducer.sendJsonMedNøkkel(createUniqueKey(String.valueOf(dto.getBehandlingId()), dto.getBehandlingStatusKode()), generatePayload(builder.build()));
            } catch (Exception e) {
                LOG.info("SOBKAFKA AIVEN ga feil for {}", dto, e);
            }

        }
        producer.sendJsonMedNøkkel(createUniqueKey(String.valueOf(dto.getBehandlingId()), dto.getBehandlingStatusKode()), generatePayload(builder.build()));

    }

    private String createUniqueBehandlingsId(String behandlingsId) {
        return String.format("%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandlingsId);
    }

    private String createUniqueKey(String behandlingsId, String event) {
        return String.format("%s_%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandlingsId, event);
    }

    private String generatePayload(contract.sob.dto.BehandlingStatus hendelse) {
        return StandardJsonConfig.toJson(hendelse);
    }

}
