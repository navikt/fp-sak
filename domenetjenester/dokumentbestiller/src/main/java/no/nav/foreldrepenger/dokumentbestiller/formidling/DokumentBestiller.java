package no.nav.foreldrepenger.dokumentbestiller.formidling;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.vedtak.felles.prosesstask.api.CommonTaskProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class DokumentBestiller {
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private MellomlagringRepository mellomlagringRepository;

    public DokumentBestiller() {
        // CDI
    }

    @Inject
    public DokumentBestiller(BehandlingRepository behandlingRepository,
                             ProsessTaskTjeneste taskTjeneste,
                             DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                             MellomlagringRepository mellomlagringRepository) {
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.mellomlagringRepository = mellomlagringRepository;
    }

    public void bestillDokument(DokumentBestilling bestillBrevDto) {
        var behandling = behandlingRepository.hentBehandling(bestillBrevDto.behandlingUuid());
        opprettBestillBrevTask(behandling, bestillBrevDto);
        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling, bestillBrevDto);
        var malÅSlette = bestillBrevDto.journalførSom() != null ? bestillBrevDto.journalførSom() : bestillBrevDto.dokumentMal();
        var mellomlagringType = MellomlagringType.fraDokumentMalType(malÅSlette);
        if (mellomlagringType != null) {
            mellomlagringRepository.fjernMellomlagring(behandling.getId(), mellomlagringType);
        }
    }

    private void opprettBestillBrevTask(Behandling behandling, DokumentBestilling bestilling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(BestillDokumentTask.class);

        // Obligatorisk
        prosessTaskData.setProperty(CommonTaskProperties.BEHANDLING_UUID, String.valueOf(behandling.getUuid()));
        prosessTaskData.setProperty(BestillDokumentTask.BESTILLING_UUID, String.valueOf(bestilling.bestillingUuid()));
        prosessTaskData.setProperty(BestillDokumentTask.DOKUMENT_MAL, bestilling.dokumentMal().name());

        // Optionals
        Optional.ofNullable(bestilling.journalførSom()).ifPresent(a -> prosessTaskData.setProperty(BestillDokumentTask.JOURNALFOER_SOM_DOKUMENT, a.name()));
        Optional.ofNullable(bestilling.revurderingÅrsak()).ifPresent(a -> prosessTaskData.setProperty(BestillDokumentTask.REVURDERING_ÅRSAK, a.name()));
        prosessTaskData.setPayload(bestilling.fritekst());

        // Brukes kun i logging
        prosessTaskData.setSaksnummer(behandling.getSaksnummer().getVerdi());
        taskTjeneste.lagre(prosessTaskData);
    }
}
