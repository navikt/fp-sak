package no.nav.foreldrepenger.behandling.anke;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.anke.impl.AnkeHistorikkTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class AnkeTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private AnkeRepository ankeRepository;
    private AnkeHistorikkTjeneste ankeHistorikkTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    AnkeTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AnkeTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             BehandlingRepository behandlingRepository,
                             AnkeRepository ankeRepository,
                             ProsessTaskRepository prosessTaskRepository,
                             AnkeHistorikkTjeneste ankeHistorikkTjeneste,
                             BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.ankeRepository = ankeRepository;
        this.ankeHistorikkTjeneste = ankeHistorikkTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    public Optional<Behandling> opprettAnkeBehandling(Fagsak fagsak) {
        Optional<Behandling> behandlingAnkeGjelder = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());

        if (!behandlingAnkeGjelder.isPresent()) {
            return Optional.empty();
        }
        BehandlingType behandlingTypeAnke = BehandlingType.ANKE;
        // TODO(FLUORITT): Midlertidig hardkodet inn for Klageinstans da den ikke kommer med i response fra NORG. Fjern dette når det er på plass.
        OrganisasjonsEnhet enhetAnke = behandlendeEnhetTjeneste.getKlageInstans();

        Behandling ankeBehandling = behandlingskontrollTjeneste.opprettNyBehandling(fagsak, behandlingTypeAnke,
            (beh) -> {
                beh.setBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingTypeAnke.getBehandlingstidFristUker()));
                beh.setBehandlendeEnhet(enhetAnke);
            });

        ankeRepository.leggTilAnkeResultat(ankeBehandling);
        opprettTaskForÅStarteBehandling(ankeBehandling);
        ankeHistorikkTjeneste.opprettHistorikkinnslag(ankeBehandling);

        return Optional.of(ankeBehandling);
    }

    private void opprettTaskForÅStarteBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

}
