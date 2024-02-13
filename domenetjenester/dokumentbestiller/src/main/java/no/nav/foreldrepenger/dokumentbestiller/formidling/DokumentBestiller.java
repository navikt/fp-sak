package no.nav.foreldrepenger.dokumentbestiller.formidling;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.BrevBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.CommonTaskProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class DokumentBestiller {
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private DokumentBestilt dokumentBestilt;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    public DokumentBestiller() {
        // CDI
    }

    @Inject
    public DokumentBestiller(BehandlingRepository behandlingRepository,
                             ProsessTaskTjeneste taskTjeneste,
                             DokumentBestilt dokumentBestilt,
                             DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.dokumentBestilt = dokumentBestilt;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    public void bestillDokument(BrevBestilling bestillBrevDto, HistorikkAktør aktør) {
        bestillOgLogg(bestillBrevDto, aktør);
    }

    private void bestillOgLogg(BrevBestilling brevBestilling, HistorikkAktør aktør) {
        var behandling = behandlingRepository.hentBehandling(brevBestilling.behandlingUuid());
        bestillDokumentOgLoggHistorikk(behandling, aktør, brevBestilling);
    }

    private void bestillDokumentOgLoggHistorikk(Behandling behandling, HistorikkAktør aktør, BrevBestilling bestilling) {
        opprettBestillBrevTask(behandling, bestilling);
        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, bestilling);
        dokumentBestilt.opprettHistorikkinnslag(aktør, behandling, bestilling);
    }

    private void opprettBestillBrevTask(Behandling behandling, BrevBestilling bestilling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(BestillBrevTask.class);

        // Obligatorisk
        prosessTaskData.setProperty(CommonTaskProperties.BEHANDLING_UUID, String.valueOf(behandling.getUuid()));
        prosessTaskData.setProperty(BestillBrevTask.BESTILLING_UUID, String.valueOf(bestilling.bestillingUuid()));
        prosessTaskData.setProperty(BestillBrevTask.DOKUMENT_MAL, bestilling.dokumentMal().name());

        // Optionals
        Optional.ofNullable(bestilling.journalførSom()).ifPresent(a -> prosessTaskData.setProperty(BestillBrevTask.JOURNALFOER_SOM_DOKUMENT, a.name()));
        Optional.ofNullable(bestilling.revurderingÅrsak()).ifPresent(a -> prosessTaskData.setProperty(BestillBrevTask.REVURDERING_ÅRSAK, a.name()));
        prosessTaskData.setPayload(bestilling.fritekst());

        // Brukes kun i logging
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        taskTjeneste.lagre(prosessTaskData);
    }
}
