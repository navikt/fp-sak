package no.nav.foreldrepenger.historikk.kafka;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ProsessTask("historikk.migrer.journalpostId.fra.formidling")
public class LagreJournalpostTask implements ProsessTaskHandler {

    private BehandlingDokumentRepository behandlingDokumentRepository;
    private BehandlingRepository behandlingRepository;

    LagreJournalpostTask() {
        // cdi
    }

    @Inject
    public LagreJournalpostTask(BehandlingDokumentRepository behandlingDokumentRepository, BehandlingRepository behandlingRepository) {
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var historikk = StandardJsonConfig.fromJson(prosessTaskData.getPayloadAsString(), DokumentProdusertDto.class);
        oppdaterDokumentBestillingMedJournalpostId(historikk.behandlingUuid(), historikk.dokumentMal(), historikk.journalpostId());
    }

    private void oppdaterDokumentBestillingMedJournalpostId(UUID behandlingUuid, String dokumentMal, String journalpostId) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());

        Supplier<Stream<BehandlingDokumentBestiltEntitet>> streamSupplier = () -> behandlingDokument.map(
                BehandlingDokumentEntitet::getBestilteDokumenter)
            .orElse(List.of())
            .stream()
            .filter(bdb -> dokumentMal.equals(bdb.getDokumentMalType()))
            .filter(bdb -> bdb.getJournalpostId() == null);

        if (streamSupplier.get().count() == 1) {
            streamSupplier.get().findFirst().ifPresent(dokBestillt -> {
                dokBestillt.setJournalpostId(new JournalpostId(journalpostId));
                behandlingDokumentRepository.lagreOgFlush(dokBestillt);
            });
        }
    }
}
