package no.nav.foreldrepenger.dokumentbestiller.formidling;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.BrevBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilt;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
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

    /**
     *
     * @param brevBestilling bestill brev dto
     * @param opprinneligDokumentMal settes til den opprinnelige dokumentMalTypen hvis fritekstmalen brukes.
     * @param aktør historikk aktør.
     */
    public void bestillVedtak(BrevBestilling brevBestilling, DokumentMalType opprinneligDokumentMal, HistorikkAktør aktør) {
        bestillOgLogg(brevBestilling, opprinneligDokumentMal, aktør);
    }

    public void bestillDokument(BrevBestilling bestillBrevDto, HistorikkAktør aktør) {
        bestillOgLogg(bestillBrevDto, null, aktør);
    }

    private void bestillOgLogg(BrevBestilling brevBestilling, DokumentMalType opprinneligDokumentMal, HistorikkAktør aktør) {
        var behandling = behandlingRepository.hentBehandling(brevBestilling.behandlingUuid());
        bestillDokumentOgLoggHistorikk(behandling, brevBestilling.dokumentMal(), brevBestilling.fritekst(), brevBestilling.revurderingÅrsak(), aktør,
            opprinneligDokumentMal);
    }

    private void bestillDokumentOgLoggHistorikk(Behandling behandling,
                                                DokumentMalType dokumentMal,
                                                String fritekst,
                                                RevurderingVarslingÅrsak årsak,
                                                HistorikkAktør aktør,
                                                DokumentMalType opprinneligDokumentMal) {
        var bestillingUuid = UUID.randomUUID();
        opprettBestillDokumentTask(behandling, dokumentMal, fritekst, årsak, bestillingUuid, opprinneligDokumentMal);
        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, dokumentMal, bestillingUuid, opprinneligDokumentMal);
        dokumentBestilt.opprettHistorikkinnslag(aktør, behandling, dokumentMal, opprinneligDokumentMal);
    }

    private void opprettBestillDokumentTask(Behandling behandling,
                                            DokumentMalType dokumentMalType,
                                            String fritekst,
                                            RevurderingVarslingÅrsak årsak,
                                            UUID bestillingUuid,
                                            DokumentMalType opprinneligDokumentMal) {
        var prosessTaskData = ProsessTaskData.forProsessTask(DokumentBestillerTask.class);
        prosessTaskData.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        prosessTaskData.setProperty(CommonTaskProperties.BEHANDLING_UUID, behandling.getUuid().toString());
        prosessTaskData.setProperty(DokumentBestillerTask.BESTILLING_UUID, String.valueOf(bestillingUuid));
        prosessTaskData.setProperty(DokumentBestillerTask.DOKUMENT_MAL_TYPE, dokumentMalType.getKode());
        Optional.ofNullable(opprinneligDokumentMal).ifPresent(a -> prosessTaskData.setProperty(DokumentBestillerTask.OPPRINNELIG_DOKUMENT_MAL, a.getKode()));
        Optional.ofNullable(årsak).ifPresent(a -> prosessTaskData.setProperty(DokumentBestillerTask.REVURDERING_VARSLING_ÅRSAK, a.getKode()));
        prosessTaskData.setPayload(fritekst);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }
}
