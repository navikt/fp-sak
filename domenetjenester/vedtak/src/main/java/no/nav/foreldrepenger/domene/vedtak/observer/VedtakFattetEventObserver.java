package no.nav.foreldrepenger.domene.vedtak.observer;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class VedtakFattetEventObserver {

    private static final Set<VedtakResultatType> SKAL_SENDE_HENDELSE = Set.of(VedtakResultatType.INNVILGET, VedtakResultatType.OPPHØR);
    private ProsessTaskTjeneste taskRepository;
    private BehandlingVedtakRepository vedtakRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;

    public VedtakFattetEventObserver() {
    }

    @Inject
    public VedtakFattetEventObserver(ProsessTaskTjeneste taskRepository,
                                     BehandlingVedtakRepository vedtakRepository,
                                     TilbakekrevingRepository tilbakekrevingRepository) {
        this.taskRepository = taskRepository;
        this.vedtakRepository = vedtakRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
    }

    public void observerStegOvergang(@Observes BehandlingVedtakEvent event) {
        if (event.iverksattYtelsesVedtak()
            && erBehandlingAvRettType(event.behandling(), event.vedtak())) {
            opprettTaskForPubliseringAvVedtak(event.getBehandlingId());
        }
    }

    private boolean erBehandlingAvRettType(Behandling behandling, BehandlingVedtak vedtak) {
        var resultatType = vedtak != null ? vedtak.getVedtakResultatType() : VedtakResultatType.UDEFINERT;
        return SKAL_SENDE_HENDELSE.contains(resultatType) ||
            tilbakekrevingRepository.hent(behandling.getId()).isPresent() ||
            revurderingAvslåttMedForrigeInnvilget(behandling, resultatType);
    }

    private boolean revurderingAvslåttMedForrigeInnvilget(Behandling behandling, VedtakResultatType vedtakResultatType) {
        return VedtakResultatType.AVSLAG.equals(vedtakResultatType) && behandling.erRevurdering() &&
            behandling.getOriginalBehandlingId()
            .flatMap(b -> vedtakRepository.hentForBehandlingHvisEksisterer(b))
            .filter(v -> SKAL_SENDE_HENDELSE.contains(v.getVedtakResultatType()))
            .isPresent();
    }

    private void opprettTaskForPubliseringAvVedtak(Long behandlingId) {
        var taskData = ProsessTaskData.forProsessTask(PubliserVedtattYtelseHendelseTask.class);
        taskData.setProperty(PubliserVedtattYtelseHendelseTask.KEY, behandlingId.toString());
        taskData.setCallIdFraEksisterende();
        taskRepository.lagre(taskData);
    }

}
