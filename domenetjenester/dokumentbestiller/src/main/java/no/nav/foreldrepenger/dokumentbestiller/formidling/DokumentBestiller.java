package no.nav.foreldrepenger.dokumentbestiller.formidling;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.BrevHistorikkinnslag;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.kafka.DokumentBestillerKafkaTask;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class DokumentBestiller {
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BrevHistorikkinnslag brevHistorikkinnslag;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    public DokumentBestiller() {
        //CDI
    }

    @Inject
    public DokumentBestiller(BehandlingRepository behandlingRepository,
                             ProsessTaskTjeneste taskTjeneste,
                             BrevHistorikkinnslag brevHistorikkinnslag,
                             DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.brevHistorikkinnslag = brevHistorikkinnslag;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    public void bestillBrev(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {

        var behandling = bestillBrevDto.getBehandlingUuid() == null ? behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingId())
            : behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingUuid());
        bestillBrev(behandling,
                    DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode()),
                    bestillBrevDto.getFritekst(),
                    bestillBrevDto.getÅrsakskode(),
                    aktør);
    }

    public void bestillBrev(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, String årsak, HistorikkAktør aktør) {
        var bestillingUuid = UUID.randomUUID();
        opprettBrevTask(behandling, dokumentMalType, fritekst, årsak, aktør, bestillingUuid);

        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, dokumentMalType, bestillingUuid);
        brevHistorikkinnslag.opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, dokumentMalType);
    }

    private void opprettBrevTask(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, String årsak, HistorikkAktør aktør, UUID bestillingUuid) {
        var prosessTaskData = ProsessTaskData.forProsessTask(DokumentBestillerTask.class);
        prosessTaskData.setPayload(StandardJsonConfig.toJson(fritekst));
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.BEHANDLING_ID, behandling.getId().toString());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.DOKUMENT_MAL_TYPE, dokumentMalType.getKode());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.REVURDERING_VARSLING_ÅRSAK, årsak);
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.HISTORIKK_AKTØR, aktør.getKode());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.BESTILLING_UUID, String.valueOf(bestillingUuid));
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.BEHANDLENDE_ENHET_NAVN, behandling.getBehandlendeOrganisasjonsEnhet().enhetNavn());
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }
}
