package no.nav.foreldrepenger.dokumentbestiller.formidling;

import java.util.Optional;
import java.util.UUID;

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
        var bestillingUuid = UUID.randomUUID();
        opprettBestillDokumentTask(behandling, bestillingUuid, bestilling);
        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, bestillingUuid, bestilling);
        dokumentBestilt.opprettHistorikkinnslag(aktør, behandling, bestilling);
    }

    private void opprettBestillDokumentTask(Behandling behandling, UUID bestillingUuid,
                                            BrevBestilling bestilling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(DokumentBestillerTask.class);

        // Obligatorisk
        prosessTaskData.setProperty(CommonTaskProperties.BEHANDLING_UUID, behandling.getUuid().toString());
        prosessTaskData.setProperty(DokumentBestillerTask.BESTILLING_UUID, String.valueOf(bestillingUuid));
        prosessTaskData.setProperty(DokumentBestillerTask.DOKUMENT_MAL_TYPE, bestilling.dokumentMal().getKode());

        // Optionals
        Optional.ofNullable(bestilling.journalførSom()).ifPresent(a -> prosessTaskData.setProperty(DokumentBestillerTask.OPPRINNELIG_DOKUMENT_MAL, a.getKode()));
        Optional.ofNullable(bestilling.revurderingÅrsak()).ifPresent(a -> prosessTaskData.setProperty(DokumentBestillerTask.REVURDERING_VARSLING_ÅRSAK, a.getKode()));
        prosessTaskData.setPayload(bestilling.fritekst());

        // Brukes kun i logging
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        taskTjeneste.lagre(prosessTaskData);
    }
}
