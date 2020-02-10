package no.nav.foreldrepenger.dokumentbestiller.kafka;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.BrevHistorikkinnslag;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.util.StringUtils;

@ApplicationScoped
public class DokumentKafkaBestiller {
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private BrevHistorikkinnslag brevHistorikkinnslag;

    public DokumentKafkaBestiller() {
        //CDI
    }

    @Inject
    public DokumentKafkaBestiller(BehandlingRepository behandlingRepository,
                                  ProsessTaskRepository prosessTaskRepository,
                                  BrevHistorikkinnslag brevHistorikkinnslag) {
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.brevHistorikkinnslag = brevHistorikkinnslag;
    }

    public void bestillBrevFraKafka(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {
        RevurderingVarslingÅrsak årsak = null;
        if (!StringUtils.nullOrEmpty(bestillBrevDto.getÅrsakskode())) {
            årsak = RevurderingVarslingÅrsak.fraKode(bestillBrevDto.getÅrsakskode());
        }
        Behandling behandling = behandlingRepository.hentBehandling(bestillBrevDto.getBehandlingId());
        bestillBrev(behandling, bestillBrevDto.getBrevmalkode(), bestillBrevDto.getFritekst(), årsak, aktør);
    }

    public void bestillBrev(Behandling behandling, String dokumentMalKode, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør) {
        bestillBrev(behandling, DokumentMalType.fraKode(dokumentMalKode), fritekst, årsak, aktør);
    }

    public void bestillBrev(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør) {
        opprettKafkaTask(behandling, dokumentMalType, fritekst, årsak, aktør);
        brevHistorikkinnslag.opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, dokumentMalType);
    }

    private void opprettKafkaTask(Behandling behandling, DokumentMalType dokumentMalType, String fritekst, RevurderingVarslingÅrsak årsak, HistorikkAktør aktør) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(DokumentbestillerKafkaTaskProperties.TASKTYPE);
        prosessTaskData.setPayload(JsonMapper.toJson(fritekst));
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID, behandling.getId().toString());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE, dokumentMalType.getKode());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.REVURDERING_VARSLING_ÅRSAK, årsak != null ? årsak.getKode() : null);
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.HISTORIKK_AKTØR, aktør.getKode());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BESTILLING_UUID, UUID.randomUUID().toString());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BEHANDLENDE_ENHET_NAVN, behandling.getBehandlendeOrganisasjonsEnhet().getEnhetNavn());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

}
