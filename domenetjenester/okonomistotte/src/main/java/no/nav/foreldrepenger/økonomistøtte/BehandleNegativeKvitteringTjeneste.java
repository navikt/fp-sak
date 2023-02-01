package no.nav.foreldrepenger.økonomistøtte;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
public class BehandleNegativeKvitteringTjeneste {

    private ProsessTaskTjeneste repository;

    BehandleNegativeKvitteringTjeneste() {
        // CDI
    }

    @Inject
    BehandleNegativeKvitteringTjeneste(ProsessTaskTjeneste repository) {
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
                prosessTaskData.venterPåHendelse(null);
                prosessTaskData.setStatus(ProsessTaskStatus.FEILET);

                var feil = new NegativeKvitteringFeil("Det finnes negativ kvittering for minst en av oppdragsmottakerne.");

                prosessTaskData.setSisteFeil(DefaultJsonMapper.toPrettyJson(feil));

                repository.lagre(prosessTaskData);
            }
        } catch (NoResultException e) {
            throw new IllegalStateException(String.format("Prosess task med prossess task id = %d finnes ikke", prosessTaskId), e);
        }
    }

    public record NegativeKvitteringFeil(String feilmelding) {
    }
}
