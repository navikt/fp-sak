package no.nav.foreldrepenger.domene.vedtak.observer;

import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class VedtakFattetEventObserver {

    private ProsessTaskRepository taskRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository vedtakRepository;

    public VedtakFattetEventObserver() {
    }

    @Inject
    public VedtakFattetEventObserver(ProsessTaskRepository taskRepository, BehandlingRepository behandlingRepository, BehandlingVedtakRepository vedtakRepository) {
        this.taskRepository = taskRepository;
        this.behandlingRepository = behandlingRepository;
        this.vedtakRepository = vedtakRepository;
    }

    public void observerStegOvergang(@Observes BehandlingVedtakEvent event) {
        if (erBehandlingAvRettType(event.getBehandlingId())) {
            opprettTaskForPubliseringAvVedtak(event.getBehandlingId());
        }
    }

    private boolean erBehandlingAvRettType(Long behandlingId) {
        final Optional<Behandling> optBehandling = behandlingRepository.finnUnikBehandlingForBehandlingId(behandlingId);
        if (optBehandling.isPresent()) {
            final Behandling behandling = optBehandling.get();
            if (behandling.erYtelseBehandling()) {
                final VedtakResultatType resultatType = vedtakRepository.hentBehandlingvedtakForBehandlingId(behandlingId)
                    .map(BehandlingVedtak::getVedtakResultatType)
                    .orElse(VedtakResultatType.AVSLAG);

                return Set.of(VedtakResultatType.INNVILGET, VedtakResultatType.DELVIS_INNVILGET, VedtakResultatType.OPPHØR).contains(resultatType);
            }
        }
        return false;
    }

    private void opprettTaskForPubliseringAvVedtak(Long behandlingId) {
        final ProsessTaskData taskData = new ProsessTaskData(PubliserVedtattYtelseHendelseTask.TASKTYPE);
        taskData.setProperty(PubliserVedtattYtelseHendelseTask.KEY, behandlingId.toString());
        taskRepository.lagre(taskData);
    }

}
