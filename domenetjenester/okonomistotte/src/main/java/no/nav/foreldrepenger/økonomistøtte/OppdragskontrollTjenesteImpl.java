package no.nav.foreldrepenger.økonomistøtte;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@Dependent
@Named("oppdragTjeneste")
public class OppdragskontrollTjenesteImpl implements OppdragskontrollTjeneste {

    private static final Logger log = LoggerFactory.getLogger(OppdragskontrollTjenesteImpl.class);

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
        Optional<OppdragskontrollManager> manager = factory.getManager(behandling, tidligereOppdragFinnes);
        if (manager.isPresent()) {
            Oppdragskontroll oppdrag = manager.get().opprettØkonomiOppdrag(behandling, oppdragskontroll);

            if (behandling.getFagsak().getSaksnummer().equals(new Saksnummer("147260073"))){
                log.warn("Antall oppdrag uten linjer: {} - de skal fjernes.", oppdragskontroll.getOppdrag110Liste().stream().filter(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().isEmpty()).count());
                oppdragskontroll.getOppdrag110Liste().removeIf(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().isEmpty());
            }

            OppdragskontrollPostConditionCheck.valider(oppdrag);
            return Optional.of(oppdrag);
        } else {
            return Optional.empty();
        }
    }

    private boolean finnesOppdragForTidligereBehandlingISammeFagsak(Behandling behandling, List<Oppdragskontroll> alleOppdragForFagsak) {
        return alleOppdragForFagsak.stream()
            .map(ok -> behandlingRepository.hentBehandling(ok.getBehandlingId()))
            .anyMatch(beh -> behandling.getOpprettetTidspunkt().isAfter(beh.getOpprettetTidspunkt()));
    }

    @Override
    public Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId, boolean brukFellesEndringstidspunkt) {
        return opprettOppdrag(behandlingId, prosessTaskId);
    }

    @Override
    public void lagre(Oppdragskontroll oppdragskontroll) {
        økonomioppdragRepository.lagre(oppdragskontroll);
    }
}
