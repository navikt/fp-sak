package no.nav.foreldrepenger.domene.vedtak.observer;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class VedtakFattetEventObserver {

    private ProsessTaskRepository taskRepository;

    public VedtakFattetEventObserver() {
    }

    @Inject
    public VedtakFattetEventObserver(ProsessTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void observerStegOvergang(@Observes BehandlingVedtakEvent event) {
        if (IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())
            && erBehandlingAvRettType(event.getBehandling(), event.getVedtak())) {
            opprettTaskForPubliseringAvVedtak(event.getBehandlingId());
        }
    }

    private boolean erBehandlingAvRettType(Behandling behandling, BehandlingVedtak vedtak) {
        if (behandling != null && behandling.erYtelseBehandling()) {
            final var resultatType = vedtak != null ? vedtak.getVedtakResultatType() : VedtakResultatType.AVSLAG;
            return Set.of(VedtakResultatType.INNVILGET, VedtakResultatType.DELVIS_INNVILGET, VedtakResultatType.OPPHØR).contains(resultatType);
        }
        return false;
    }

    private void opprettTaskForPubliseringAvVedtak(Long behandlingId) {
        final var taskData = new ProsessTaskData(PubliserVedtattYtelseHendelseTask.TASKTYPE);
        taskData.setProperty(PubliserVedtattYtelseHendelseTask.KEY, behandlingId.toString());
        taskData.setCallIdFraEksisterende();
        taskRepository.lagre(taskData);
    }

}
