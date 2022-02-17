package no.nav.foreldrepenger.dokumentbestiller.kafka;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.BrevHistorikkinnslag;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class DokumentKafkaBestiller {
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BrevHistorikkinnslag brevHistorikkinnslag;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    public DokumentKafkaBestiller() {
        //CDI
    }

    @Inject
    public DokumentKafkaBestiller(BehandlingRepository behandlingRepository,
                                  ProsessTaskTjeneste taskTjeneste,
                                  BrevHistorikkinnslag brevHistorikkinnslag,
                                  DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.brevHistorikkinnslag = brevHistorikkinnslag;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    public void bestillBrevFraKafka(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {
        RevurderingVarslingÅrsak årsak = null;
        if (bestillBrevDto.getÅrsakskode() != null && !bestillBrevDto.getÅrsakskode().isEmpty()) {
            årsak = RevurderingVarslingÅrsak.fraKode(bestillBrevDto.getÅrsakskode());
        }
        var behandling = bestillBrevDto.getBehandlingUuid() == null ? behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingId())
            : behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingUuid());
        bestillBrev(behandling, DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode()), bestillBrevDto.getFritekst(), årsak, aktør);
    }

    public void bestillBrev(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør) {
        var bestillingUuid = UUID.randomUUID();
        opprettKafkaTask(behandling, dokumentMalType, fritekst, årsak, aktør, bestillingUuid);

        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, dokumentMalType, bestillingUuid);
        brevHistorikkinnslag.opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, dokumentMalType);
    }

    private void opprettKafkaTask(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør, UUID bestillingUuid) {
        var prosessTaskData = ProsessTaskData.forProsessTask(DokumentBestillerKafkaTask.class);
        prosessTaskData.setPayload(StandardJsonConfig.toJson(fritekst));
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.BEHANDLING_ID, behandling.getId().toString());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.DOKUMENT_MAL_TYPE, dokumentMalType.getKode());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.REVURDERING_VARSLING_ÅRSAK, årsak != null ? årsak.getKode() : null);
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.HISTORIKK_AKTØR, aktør.getKode());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.BESTILLING_UUID, String.valueOf(bestillingUuid));
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.BEHANDLENDE_ENHET_NAVN, behandling.getBehandlendeOrganisasjonsEnhet().enhetNavn());
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }
}
