package no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.hendelser.behandling.Behandlingstype;
import no.nav.vedtak.hendelser.behandling.Hendelse;
import no.nav.vedtak.hendelser.behandling.Kildesystem;
import no.nav.vedtak.hendelser.behandling.Ytelse;
import no.nav.vedtak.hendelser.behandling.v1.BehandlingHendelseV1;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Dependent
@ProsessTask(value = "oppgavebehandling.behandlingshendelse", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserBehandlingHendelseTask extends GenerellProsessTask {

    public static final String HENDELSE_TYPE = "hendelseType";

    private static final Logger LOG = LoggerFactory.getLogger(PubliserBehandlingHendelseTask.class);

    private static final Set<AksjonspunktDefinisjon> PAPIR = Set.of(
        AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD,
        AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER,
        AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER,
        AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER);

    private final BehandlingHendelseProducer kafkaProducer;
    private final BehandlingRepository behandlingRepository;

    @Inject
    public PubliserBehandlingHendelseTask(BehandlingHendelseProducer kafkaProducer, BehandlingRepository behandlingRepository) {
        this.kafkaProducer = kafkaProducer;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var hendelseForBehandling = HendelseForBehandling.valueOf(prosessTaskData.getPropertyValue(HENDELSE_TYPE));
        var hendelseType = utledHendelse(behandling, hendelseForBehandling);
        if (hendelseType == null) {
            LOG.info("Publiser ikke behandlingshendelse for behandling {} hendelse {}", behandling.getId(), hendelseForBehandling);
            return;
        }
        var hendelse = new BehandlingHendelseV1.Builder().medHendelse(hendelseType)
            .medHendelseUuid(UUID.randomUUID())
            .medBehandlingUuid(behandling.getUuid())
            .medKildesystem(Kildesystem.FPSAK)
            .medAktørId(behandling.getAktørId().getId())
            .medSaksnummer(behandling.getSaksnummer().getVerdi())
            .medBehandlingstype(mapBehandlingstype(behandling))
            .medYtelse(mapYtelse(behandling))
            .medTidspunkt(LocalDateTime.now())
            .build();
        kafkaProducer.sendJsonMedNøkkel(behandling.getSaksnummer().getVerdi(), DefaultJsonMapper.toJson(hendelse));
        LOG.info("Publiser behandlingshendelse på kafka for behandlingsId: {}", behandling.getId());
    }

    public static ProsessTaskData prosessTask(Saksnummer saksnummer, Long fagsakId, Long behandlingsId, HendelseForBehandling hendelse) {
        var prosessTaskData = ProsessTaskData.forProsessTask(PubliserBehandlingHendelseTask.class);
        prosessTaskData.setBehandling(saksnummer.getVerdi(), fagsakId, behandlingsId);
        prosessTaskData.setProperty(PubliserBehandlingHendelseTask.HENDELSE_TYPE, hendelse.name());
        prosessTaskData.setCallIdFraEksisterende();
        return prosessTaskData;
    }

    private static Hendelse utledHendelse(Behandling behandling, HendelseForBehandling oppgittHendelse) {
        if (HendelseForBehandling.ENHET.equals(oppgittHendelse)) {
            return Hendelse.ENHET;
        } else if (behandling.isBehandlingPåVent()) {
            return behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD) ? Hendelse.MANGLERSØKNAD : Hendelse.VENTETILSTAND;
        } else if (!behandling.getÅpneAksjonspunkter(PAPIR).isEmpty()) {
            return Hendelse.PAPIRSØKNAD;
        } else if (HendelseForBehandling.OPPRETTET.equals(oppgittHendelse)) {
            return Hendelse.OPPRETTET;
        } else if (BehandlingStatus.AVSLUTTET.equals(behandling.getStatus())) {
            return Hendelse.AVSLUTTET;
        } else if (!behandling.getAksjonspunkter().isEmpty()) {
            return Hendelse.AKSJONSPUNKT;
        } else {
            return null;
        }
    }

    private static Behandlingstype mapBehandlingstype(Behandling behandling) {
        return switch (behandling.getType()) {
            case ANKE -> Behandlingstype.ANKE;
            case FØRSTEGANGSSØKNAD -> Behandlingstype.FØRSTEGANGS;
            case INNSYN -> Behandlingstype.INNSYN;
            case KLAGE -> Behandlingstype.KLAGE;
            case REVURDERING -> Behandlingstype.REVURDERING;
            default -> null;
        };
    }

    private static Ytelse mapYtelse(Behandling behandling) {
        return switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD -> Ytelse.ENGANGSTØNAD;
            case FORELDREPENGER -> Ytelse.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelse.SVANGERSKAPSPENGER;
            default -> null;
        };
    }

}
