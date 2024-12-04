package no.nav.foreldrepenger.behandlingslager.task;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Brukes når det skal opprettes nye behandlinger eller fagsaken skal endres
 * Task som utfører noe på en fagsak, før prosessen kjøres videre.
 * Sikrer at fagsaklås task på riktig plass..
 */
public abstract class FagsakProsessTask implements ProsessTaskHandler {

    private FagsakLåsRepository fagsakLåsRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    protected FagsakProsessTask(FagsakLåsRepository fagsakLåsRepository, BehandlingLåsRepository behandlingLåsRepository) {
        this.fagsakLåsRepository = fagsakLåsRepository;
        this.behandlingLåsRepository = behandlingLåsRepository;
    }

    protected FagsakProsessTask() {
        // for CDI proxy
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        var behandlingId = getBehandlingId(prosessTaskData);

        identifiserBehandling(prosessTaskData)
            .stream()
            .sorted(Comparator.naturalOrder())
            .forEach(behandling -> behandlingLåsRepository.taLås(behandling));

        fagsakLåsRepository.taLås(fagsakId);
        prosesser(prosessTaskData, fagsakId, behandlingId);
    }

    protected abstract void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId);

    /**
     * Må alltid ta behandlingen før vi tar lås på fagsaken.
     * Ellers risikerer vi deadlock.
     * <p>
     * Identifiserer behandlingen som skal manipuleres
     *
     * @param prosessTaskData prosesstaskdata
     * @return behandlingId
     */
    protected List<Long> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return Optional.ofNullable(getBehandlingId(prosessTaskData)).map(List::of).orElseGet(List::of);
    }

    private Long getBehandlingId(ProsessTaskData data) {
        return data.getBehandlingId() != null ? Long.valueOf(data.getBehandlingId()) : null;
    }
}
