package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.batch;

import java.util.Optional;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.EtterkontrollRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Henter ut behandlinger som har fått innvilget engangsstønad på bakgrunn av
 * terminbekreftelsen, for å etterkontrollere om rett antall barn har blitt
 * født.
 *
 * Vedtak er innvilget og fattet med bakgrunn i bekreftet terminbekreftelse Det
 * har gått minst 60 dager siden termin Det er ikke registrert fødselsdato på
 * barnet/barna Det ikke allerede er opprettet revurderingsbehandling med en av
 * disse årsakene: Manglende fødsel i PDL Manglende fødsel i PDL mellom uke 26
 * og 29 Avvik i antall barn
 *
 * Ved avvik så opprettes det, hvis det ikke allerede finnes,
 * revurderingsbehandling på saken
 */

@ApplicationScoped
public class AutomatiskEtterkontrollBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAME = "BVL002";
    private final ProsessTaskTjeneste taskTjeneste;
    private final EtterkontrollRepository etterkontrollRepository;
    private final BehandlingRepository behandlingRepository;

    @Inject
    public AutomatiskEtterkontrollBatchTjeneste(ProsessTaskTjeneste taskTjeneste,
                                                EtterkontrollRepository etterkontrollRepository,
                                                BehandlingRepository behandlingRepository) {
        this.taskTjeneste = taskTjeneste;
        this.etterkontrollRepository = etterkontrollRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public String launch(Properties properties) {
        // Etterkontrolltidspunkt er allerede satt 60D fram i EK-repo
        var kontrollKandidater = etterkontrollRepository.finnKandidaterForAutomatiskEtterkontroll().stream()
            .map(behandlingRepository::finnSisteAvsluttedeIkkeHenlagteBehandling)
            .flatMap(Optional::stream)
            .toList();

        var callId = Optional.ofNullable(MDCOperations.getCallId()).orElseGet(MDCOperations::generateCallId);

        for (var kandidat : kontrollKandidater) {
            opprettEtterkontrollTask(kandidat, callId);
        }
        return BATCHNAME;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

    private void opprettEtterkontrollTask(Behandling kandidat, String callId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(kandidat.getSaksnummer().getVerdi(), kandidat.getFagsakId(), kandidat.getId());
        prosessTaskData.setCallId(callId + "_" + kandidat.getFagsakId());
        taskTjeneste.lagre(prosessTaskData);
    }

}
