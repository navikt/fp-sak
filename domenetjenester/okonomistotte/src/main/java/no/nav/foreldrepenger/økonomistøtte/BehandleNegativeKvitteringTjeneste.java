package no.nav.foreldrepenger.økonomistøtte;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

@ApplicationScoped
public class BehandleNegativeKvitteringTjeneste {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
    }

    private ProsessTaskRepository repository;

    BehandleNegativeKvitteringTjeneste() {
        // CDI
    }

    @Inject
    BehandleNegativeKvitteringTjeneste(ProsessTaskRepository repository) {
        this.repository = repository;
    }

    /**
     * Endre hendelse til prosess så det kan kjøres igjen
     * @param prosessTaskId id til prosess task
     */
    public void nullstilleØkonomioppdragTask(Long prosessTaskId) {
        try {
            var prosessTaskData = repository.finn(prosessTaskId);
            if (prosessTaskData == null) {
                throw new IllegalStateException(String.format("Prosess task med prossess task id = %d finnes ikke", prosessTaskId));
            }

            if (Objects.equals(prosessTaskData.getStatus(), ProsessTaskStatus.VENTER_SVAR)) {
                prosessTaskData.setProperty(ProsessTaskData.HENDELSE_PROPERTY, null);
                prosessTaskData.setStatus(ProsessTaskStatus.FEILET);

                var feil = new NegativeKvitteringFeil("Det finnes negativ kvittering for minst en av oppdragsmottakerne.");
                try {
                    prosessTaskData.setSisteFeil(OBJECT_MAPPER.writeValueAsString(feil));
                } catch (JsonProcessingException jpe) {
                    throw new IllegalArgumentException(String.format("Kunne ikke serialiseres til json: %s", feil), jpe);
                }

                repository.lagre(prosessTaskData);
            }
        } catch (NoResultException e) {
            throw new IllegalStateException(String.format("Prosess task med prossess task id = %d finnes ikke", prosessTaskId), e);
        }
    }

    public static record NegativeKvitteringFeil(String feilmelding) {
    }
}
