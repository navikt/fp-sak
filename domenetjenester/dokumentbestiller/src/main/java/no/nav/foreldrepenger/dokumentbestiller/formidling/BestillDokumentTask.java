package no.nav.foreldrepenger.dokumentbestiller.formidling;

import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillDokumentDtoMapper.mapDokumentMal;
import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillDokumentDtoMapper.mapRevurderignÅrsak;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.kontrakter.formidling.kodeverk.Saksnummer;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("formidling.bestillDokument")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class BestillDokumentTask implements ProsessTaskHandler {

    public static final String DOKUMENT_MAL = "dokumentMal";
    public static final String JOURNALFOER_SOM_DOKUMENT = "journalfoerSom";
    public static final String REVURDERING_ÅRSAK = "revurderingAarsak";
    public static final String BESTILLING_UUID = "bestillingUuid";
    private Dokument brev;

    BestillDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public BestillDokumentTask(Dokument brev) {
        this.brev = brev;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        brev.bestill(mapTilDto(prosessTaskData));
    }

    private DokumentBestillingDto mapTilDto(ProsessTaskData prosessTaskData) {
        return new DokumentBestillingDto(prosessTaskData.getBehandlingUuid(),
            new Saksnummer(prosessTaskData.getSaksnummer()),
            UUID.fromString(prosessTaskData.getPropertyValue(BESTILLING_UUID)),
            mapDokumentMal(DokumentMalType.valueOf(prosessTaskData.getPropertyValue(DOKUMENT_MAL))),
            mapRevurderignÅrsak(Optional.ofNullable(prosessTaskData.getPropertyValue(REVURDERING_ÅRSAK)).map(RevurderingVarslingÅrsak::valueOf).orElse(null)),
            prosessTaskData.getPayloadAsString(),
            mapDokumentMal(Optional.ofNullable(prosessTaskData.getPropertyValue(JOURNALFOER_SOM_DOKUMENT)).map(DokumentMalType::valueOf).orElse(null))
        );
    }
}
