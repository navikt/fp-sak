package no.nav.foreldrepenger.mottak.kabal;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.kabal.KabalHendelse;
import no.nav.foreldrepenger.behandling.kabal.KabalUtfall;
import no.nav.foreldrepenger.behandling.kabal.MottaFraKabalTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaMessageHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

/*
 * Dokumentasjon https://github.com/navikt/kabal-api/tree/main/docs/integrasjon
 */
@Transactional
@ActivateRequestContext
@ApplicationScoped
public class KabalHendelseHåndterer implements KafkaMessageHandler.KafkaStringMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(KabalHendelseHåndterer.class);
    private static final String GROUP_ID = "fpsak"; // Hold konstant pga offset commit !!
    private static final String KABAL = "KABAL";
    private static final String VKY_TEKST = "Vedtaket er delvis omgjort i ankebehandling. Opprett en ny behandling.";

    private String topicName;
    private ProsessTaskTjeneste taskTjeneste;
    private HendelsemottakRepository mottakRepository;
    private BehandlingRepository behandlingRepository;
    private AnkeRepository ankeRepository;
    private KlageRepository klageRepository;

    KabalHendelseHåndterer() {
        // CDI
    }

    @Inject
    public KabalHendelseHåndterer(@KonfigVerdi(value = "kafka.kabal.topic") String topicName,
                                  ProsessTaskTjeneste taskTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  HendelsemottakRepository mottakRepository,
                                  AnkeRepository ankeRepository,
                                  KlageRepository klageRepository) {
        this.topicName = topicName;
        this.taskTjeneste = taskTjeneste;
        this.mottakRepository = mottakRepository;
        this.behandlingRepository = behandlingRepository;
        this.ankeRepository = ankeRepository;
        this.klageRepository = klageRepository;
    }

    @Override
    public void handleRecord(String key, String value) {
        KabalHendelse mottattHendelse;
        try {
            mottattHendelse = StandardJsonConfig.fromJson(value, KabalHendelse.class);
        } catch (Exception e) {
            LOG.error("KABAL har endret kontrakt uten forvarsel melding {}", value, e);
            return;
        }
        setCallIdForHendelse(mottattHendelse);
        LOG.info("KABAL mottatt hendelse key={} hendelse={}", key, mottattHendelse);

        if (!Objects.equals(Fagsystem.FPSAK.getOffisiellKode(), mottattHendelse.kilde())) return;
        if (!mottakRepository.hendelseErNy(KABAL+mottattHendelse.eventId().toString())) {
            LOG.warn("KABAL mottatt hendelse på nytt key={} hendelse={}", key, mottattHendelse);
            return;
        }
        handleMessageInternal(mottattHendelse);
    }

    private void handleMessageInternal(KabalHendelse mottattHendelse) {
        var behandling = behandlingRepository.hentBehandling(UUID.fromString(mottattHendelse.kildeReferanse()));
        if (behandling == null) {
            LOG.warn("KABAL mottatt hendelse med ukjent referanse hendelse={}", mottattHendelse);
            return;
        }
        if (BehandlingType.KLAGE.equals(behandling.getType()) && klageRepository.hentKlageResultatHvisEksisterer(behandling.getId()).isEmpty()) {
            LOG.warn("KABAL mottatt hendelse for klage uten klageresultat hendelse={}", mottattHendelse);
            return;
        } else if (BehandlingType.ANKE.equals(behandling.getType()) && ankeRepository.hentAnkeResultat(behandling.getId()).isEmpty()) {
            LOG.warn("KABAL mottatt hendelse for anke uten resultat hendelse={}", mottattHendelse);
            return;
        } else if (!BehandlingType.KLAGE.equals(behandling.getType()) && !BehandlingType.ANKE.equals(behandling.getType())) {
            LOG.warn("KABAL mottatt hendelse for behandling som ikke er klage eller anke hendelse={} type={}", mottattHendelse, behandling.getType());
            return;
        }
        mottakRepository.registrerMottattHendelse(KABAL+ mottattHendelse.eventId());
        var task = ProsessTaskData.forProsessTask(MottaFraKabalTask.class);
        task.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        task.setCallIdFraEksisterende();
        task.setProperty(MottaFraKabalTask.HENDELSETYPE_KEY, mottattHendelse.type().name());
        task.setProperty(MottaFraKabalTask.KABALREF_KEY, mottattHendelse.kabalReferanse());

        if (KabalHendelse.BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET.equals(mottattHendelse.type())) {
            task.setProperty(MottaFraKabalTask.UTFALL_KEY, mottattHendelse.detaljer().klagebehandlingAvsluttet().utfall().name());
            mottattHendelse.detaljer().klagebehandlingAvsluttet().journalpostReferanser().stream()
                .findFirst().ifPresent(journalpost -> task.setProperty(MottaFraKabalTask.JOURNALPOST_KEY, journalpost));
        } else if (KabalHendelse.BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET.equals(mottattHendelse.type())) {
            task.setProperty(MottaFraKabalTask.UTFALL_KEY, mottattHendelse.detaljer().ankeITrygderettenbehandlingOpprettet().utfall().name());
            task.setProperty(MottaFraKabalTask.OVERSENDTR_KEY, mottattHendelse.detaljer().ankeITrygderettenbehandlingOpprettet().sendtTilTrygderetten()
                .toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        } else if (KabalHendelse.BehandlingEventType.ANKEBEHANDLING_AVSLUTTET.equals(mottattHendelse.type())) {
            task.setProperty(MottaFraKabalTask.UTFALL_KEY, mottattHendelse.detaljer().ankebehandlingAvsluttet().utfall().name());
            mottattHendelse.detaljer().ankebehandlingAvsluttet().journalpostReferanser().stream()
                .findFirst().ifPresent(journalpost -> task.setProperty(MottaFraKabalTask.JOURNALPOST_KEY, journalpost));
        } else if (KabalHendelse.BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET.equals(mottattHendelse.type())) {
            task.setProperty(MottaFraKabalTask.UTFALL_KEY, mottattHendelse.detaljer().behandlingEtterTrygderettenOpphevetAvsluttet().utfall().name());
            mottattHendelse.detaljer().behandlingEtterTrygderettenOpphevetAvsluttet().journalpostReferanser().stream()
                .findFirst().ifPresent(journalpost -> task.setProperty(MottaFraKabalTask.JOURNALPOST_KEY, journalpost));
        } else if (KabalHendelse.BehandlingEventType.BEHANDLING_FEILREGISTRERT.equals(mottattHendelse.type())) {
            task.setProperty(MottaFraKabalTask.FEILOPPRETTET_TYPE_KEY, mottattHendelse.detaljer().behandlingFeilregistrert().type().name());
        }
        taskTjeneste.lagre(task);

        if (KabalHendelse.BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET.equals(mottattHendelse.type()) &&
            KabalUtfall.DELVIS_MEDHOLD.equals(mottattHendelse.detaljer().ankeITrygderettenbehandlingOpprettet().utfall())) {
            var sisteYtelseEnhet = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId())
                .map(Behandling::getBehandlendeEnhet).orElse(null);
            var opprettOppgave = ProsessTaskData.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class);
            opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, sisteYtelseEnhet);
            opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, VKY_TEKST);
            opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
            opprettOppgave.setCallIdFraEksisterende();
            opprettOppgave.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            taskTjeneste.lagre(opprettOppgave);
        }
    }

    private static void setCallIdForHendelse(KabalHendelse hendelse) {
        if (hendelse.eventId() == null) {
            MDCOperations.putCallId();
        } else {
            MDCOperations.putCallId(hendelse.eventId().toString());
        }
    }

    @Override
    public String topic() {
        return topicName;
    }

    @Override
    public String groupId() {
        return GROUP_ID;
    }

    @Override
    public Optional<OffsetResetStrategy> autoOffsetReset() {
        return Optional.of(OffsetResetStrategy.EARLIEST); // For sikkerhets skyld - dersom lavt ferievolum
    }
}
