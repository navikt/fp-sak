package no.nav.foreldrepenger.behandling.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto;
import no.nav.vedtak.felles.integrasjon.kafka.EventHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class BehandlingskontrollEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingskontrollEventObserver.class);

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingRepository behandlingRepository;
    private ObjectMapper objectMapper = new ObjectMapper();

    public BehandlingskontrollEventObserver() {
    }

    @Inject
    public BehandlingskontrollEventObserver(ProsessTaskRepository prosessTaskRepository, BehandlingRepository behandlingRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public void observerStoppetEvent(@Observes BehandlingskontrollEvent.StoppetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.BEHANDLINGSKONTROLL_EVENT);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            LOG.warn("Publisering av StoppetEvent feilet", ex);
        }
    }

    // Lytter på AksjonspunkterFunnetEvent, filtrer ut når behandling er satt
    // manuelt på vent og legger melding på kafka
    public void observerAksjonspunkterFunnetEvent(@Observes AksjonspunktStatusEvent event) {
        if (event.getAksjonspunkter().stream().anyMatch(e -> e.erOpprettet() && AUTO_MANUELT_SATT_PÅ_VENT.equals(e.getAksjonspunktDefinisjon()))) {
            try {
                ProsessTaskData prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_OPPRETTET);
                prosessTaskRepository.lagre(prosessTaskData);
            } catch (Exception ex) {
                LOG.warn("Publisering av AksjonspunkterFunnetEvent feilet", ex);
            }
        }
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_AVBRUTT);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            LOG.warn("Publisering av BehandlingAvsluttetEvent feilet", ex);
        }
    }

    public void observerAksjonspunktHarEndretBehandlendeEnhetEvent(@Observes BehandlingEnhetEvent event) {
        try {
            ProsessTaskData prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_HAR_ENDRET_BEHANDLENDE_ENHET);
            prosessTaskRepository.lagre(prosessTaskData);
        } catch (Exception ex) {
            LOG.warn("Publisering av AksjonspunktHarEndretBehandlendeEnhetEvent feilet", ex);
        }
    }

    private ProsessTaskData opprettProsessTask(Long behandlingId, EventHendelse eventHendelse) throws IOException {
        ProsessTaskData taskData = new ProsessTaskData(PubliserEventTask.TASKTYPE);
        taskData.setCallIdFraEksisterende();
        taskData.setPrioritet(90);

        Optional<Behandling> behandling = behandlingRepository.finnUnikBehandlingForBehandlingId(behandlingId);

        BehandlingProsessEventDto behandlingProsessEventDto = getProduksjonstyringEventDto(eventHendelse, behandling.get());

        String json = getJson(behandlingProsessEventDto);
        taskData.setProperty(PubliserEventTask.PROPERTY_EVENT, json);
        taskData.setProperty(PubliserEventTask.PROPERTY_KEY, behandlingId.toString());
        return taskData;
    }

    private String getJson(BehandlingProsessEventDto produksjonstyringEventDto) throws IOException {
        Writer jsonWriter = new StringWriter();
        objectMapper.writeValue(jsonWriter, produksjonstyringEventDto);
        jsonWriter.flush();
        return jsonWriter.toString();
    }

    private BehandlingProsessEventDto getProduksjonstyringEventDto(EventHendelse eventHendelse, Behandling behandling) {
        Map<String, String> aksjonspunktKoderMedStatusListe = new HashMap<>();

        behandling.getAksjonspunkter().forEach(aksjonspunkt -> aksjonspunktKoderMedStatusListe.put(aksjonspunkt.getAksjonspunktDefinisjon().getKode(),
                aksjonspunkt.getStatus().getKode()));

        return BehandlingProsessEventDto.builder()
                .medFagsystem("FPSAK")
                .medBehandlingId(behandling.getId())
                .medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi())
                .medAktørId(behandling.getAktørId().getId())
                .medEventHendelse(eventHendelse)
                .medBehandlinStatus(behandling.getStatus().getKode())
                .medBehandlingSteg(behandling.getAktivtBehandlingSteg() == null ? null : behandling.getAktivtBehandlingSteg().getKode())
                .medBehandlendeEnhet(behandling.getBehandlendeEnhet())
                .medYtelseTypeKode(behandling.getFagsakYtelseType().getKode())
                .medBehandlingTypeKode(behandling.getType().getKode())
                .medOpprettetBehandling(behandling.getOpprettetDato())
                .medAksjonspunktKoderMedStatusListe(aksjonspunktKoderMedStatusListe)
                .medEksternId(behandling.getUuid())
                .build();
    }
}
