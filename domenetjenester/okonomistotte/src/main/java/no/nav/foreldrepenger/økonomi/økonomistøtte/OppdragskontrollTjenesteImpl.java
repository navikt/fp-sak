package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class OppdragskontrollTjenesteImpl implements OppdragskontrollTjeneste {

    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandlingRepository behandlingRepository;
    private OppdragskontrollManagerFactoryProvider oppdragskontrollManagerFactoryProvider;

    OppdragskontrollTjenesteImpl() {
        // For CDI
    }

    @Inject
    public OppdragskontrollTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                        ØkonomioppdragRepository økonomioppdragRepository,
                                        OppdragskontrollManagerFactoryProvider oppdragskontrollManagerFactoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppdragskontrollManagerFactoryProvider = oppdragskontrollManagerFactoryProvider;
        this.økonomioppdragRepository = økonomioppdragRepository;
    }

    @Override
    public final Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();

        List<Oppdragskontroll> tidligereOppdragListe = økonomioppdragRepository.finnAlleOppdragForSak(saksnummer);

        boolean tidligereOppdragFinnes = finnesOppdragForTidligereBehandlingISammeFagsak(behandling, tidligereOppdragListe);

        Oppdragskontroll oppdragskontroll = FastsettOppdragskontroll.finnEllerOpprett(tidligereOppdragListe, behandlingId, prosessTaskId, saksnummer);

        OppdragskontrollManagerFactory factory = oppdragskontrollManagerFactoryProvider.getTjeneste(behandling.getFagsakYtelseType());
        Optional<OppdragskontrollManager> managerOpt = factory.getManager(behandling, tidligereOppdragFinnes);

        return managerOpt.map(manager -> manager.opprettØkonomiOppdrag(behandling, oppdragskontroll));
    }

    @Override
    public final void lagre(Oppdragskontroll oppdragskontroll) {
        økonomioppdragRepository.lagre(oppdragskontroll);
    }

    private boolean finnesOppdragForTidligereBehandlingISammeFagsak(Behandling behandling, List<Oppdragskontroll> alleOppdragForFagsak) {
        return alleOppdragForFagsak.stream()
            .map(ok -> behandlingRepository.hentBehandling(ok.getBehandlingId()))
            .anyMatch(beh -> behandling.getOpprettetTidspunkt().isAfter(beh.getOpprettetTidspunkt()));
    }
}
