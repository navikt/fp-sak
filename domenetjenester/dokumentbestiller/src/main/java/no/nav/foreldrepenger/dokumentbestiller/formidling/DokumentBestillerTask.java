package no.nav.foreldrepenger.dokumentbestiller.formidling;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kontrakter.formidling.kodeverk.YtelseType;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingV2Dto;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("dokumentbestiller.bestilldokument")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class DokumentBestillerTask implements ProsessTaskHandler {

    public static final String DOKUMENT_MAL_TYPE = "dokumentMalType";
    public static final String REVURDERING_VARSLING_ÅRSAK = "revurderingVarslingAarsak";
    public static final String HISTORIKK_AKTØR = "historikkAktoer";
    public static final String BESTILLING_UUID = "bestillingUuid";

    private BehandlingRepository behandlingRepository;
    private Brev brev;

    DokumentBestillerTask() {
        // for CDI proxy
    }

    @Inject
    public DokumentBestillerTask(BehandlingRepository behandlingRepository, Brev brev) {
        this.behandlingRepository = behandlingRepository;
        this.brev = brev;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        brev.bestill(mapDokumentbestilling(prosessTaskData));
    }

    private DokumentbestillingV2Dto mapDokumentbestilling(ProsessTaskData prosessTaskData) {
        var behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingUuid());
        return new DokumentbestillingV2Dto(
            behandling.getUuid(),
            UUID.fromString(prosessTaskData.getPropertyValue(BESTILLING_UUID)),
            mapYtelse(behandling.getFagsakYtelseType()),
            prosessTaskData.getPropertyValue(DOKUMENT_MAL_TYPE),
            prosessTaskData.getPayloadAsString(),
            behandling.getBehandlendeOrganisasjonsEnhet().enhetNavn(),
            prosessTaskData.getPropertyValue(REVURDERING_VARSLING_ÅRSAK)
        );
    }

    private static YtelseType mapYtelse(FagsakYtelseType fpsakYtelseKode) {
        return switch (fpsakYtelseKode) {
            case ENGANGSTØNAD -> YtelseType.ES;
            case FORELDREPENGER -> YtelseType.FP;
            case SVANGERSKAPSPENGER -> YtelseType.SVP;
            default -> throw new TekniskException("FP-533280", "Klarte ikke utlede ytelsetype: %s. Kan ikke bestille dokument");
        };
    }
}
