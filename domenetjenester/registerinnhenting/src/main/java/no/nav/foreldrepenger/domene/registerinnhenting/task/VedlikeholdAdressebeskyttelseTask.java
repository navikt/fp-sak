package no.nav.foreldrepenger.domene.registerinnhenting.task;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("vedlikehold.adressebeskyttelse")
public class VedlikeholdAdressebeskyttelseTask implements ProsessTaskHandler {


    private FagsakRepository fagsakRepository;

    VedlikeholdAdressebeskyttelseTask() {
        // for CDI proxy
    }

    @Inject
    public VedlikeholdAdressebeskyttelseTask(BehandlingRepositoryProvider repositoryProvider) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        @SuppressWarnings("unused")
        var aktørId = new AktørId(prosessTaskData.getAktørId());
        /*
         * Placeholder for håndtering av adressebeskyttelsehendelser og øvrige tilfelle av oppdaget adressebeskyttelse
         * - maskere adresser for bruker
         * - flytte åpne behandlinger til Vikafossen
         * - Avklar PO og LagretVedtak med produkteiere
         * - lage endepunkt for fpabonnent
         * - opprette tasks når det oppdages SPSF/SPFO
         */

    }

    @SuppressWarnings("unused")
    private List<Fagsak> hentBrukersRelevanteSaker(AktørId aktørId, LocalDate opprettetEtter, LocalDate familieHendelse) {
        return fagsakRepository.hentForBruker(aktørId);
    }


}
