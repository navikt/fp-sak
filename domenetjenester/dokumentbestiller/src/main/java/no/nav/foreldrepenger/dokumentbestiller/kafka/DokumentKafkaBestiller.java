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
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class DokumentKafkaBestiller {
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BrevHistorikkinnslag brevHistorikkinnslag;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private static Set<DokumentMalType> BESTILLE_JSON_FOR_NYE_BREV = Set.of(DokumentMalType.SVANGERSKAPSPENGER_OPPHØR, DokumentMalType.SVANGERSKAPSPENGER_AVSLAG);

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

    public void bestillBrevFraKafka(BestillBrevDto bestillBrevDto, HistorikkAktør aktør, DokumentMalType bestilleJsonForDokumentMal) {
        RevurderingVarslingÅrsak årsak = null;
        if (bestillBrevDto.getÅrsakskode() != null && !bestillBrevDto.getÅrsakskode().isEmpty()) {
            årsak = RevurderingVarslingÅrsak.fraKode(bestillBrevDto.getÅrsakskode());
        }
        var behandling = bestillBrevDto.getBehandlingUuid() == null ? behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingId())
            : behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingUuid());
        bestillBrev(behandling, DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode()), bestillBrevDto.getFritekst(), årsak, aktør, bestilleJsonForDokumentMal);
    }

    public void bestillBrev(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør, DokumentMalType bestilleJsonForDokumentMal) {
        opprettKafkaTask(behandling, dokumentMalType, fritekst, årsak, aktør);

        if (!skalKunBestillesForÅLageJson(dokumentMalType)) {
            dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, dokumentMalType);
            brevHistorikkinnslag.opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, dokumentMalType);
        }

        if (bestilleJsonForDokumentMal != null) {
            opprettKafkaTask(behandling, bestilleJsonForDokumentMal, null, null, aktør);
        }
    }

    private boolean skalKunBestillesForÅLageJson(DokumentMalType dokumentMalType) {
        return BESTILLE_JSON_FOR_NYE_BREV.contains(dokumentMalType) && Environment.current().isProd();
    }

    private void opprettKafkaTask(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør) {
        var prosessTaskData = ProsessTaskData.forProsessTask(DokumentBestillerKafkaTask.class);
        prosessTaskData.setPayload(StandardJsonConfig.toJson(fritekst));
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.BEHANDLING_ID, behandling.getId().toString());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.DOKUMENT_MAL_TYPE, dokumentMalType.getKode());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.REVURDERING_VARSLING_ÅRSAK, årsak != null ? årsak.getKode() : null);
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.HISTORIKK_AKTØR, aktør.getKode());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.BESTILLING_UUID, UUID.randomUUID().toString());
        prosessTaskData.setProperty(DokumentBestillerKafkaTask.BEHANDLENDE_ENHET_NAVN, behandling.getBehandlendeOrganisasjonsEnhet().enhetNavn());
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }
}
