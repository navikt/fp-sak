package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class BehandlingRevurderingRepository {

    private static final String AVSLUTTET_KEY = "avsluttet";

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private SøknadRepository søknadRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    BehandlingRevurderingRepository() {
    }

    @Inject
    public BehandlingRevurderingRepository( EntityManager entityManager,
                                               BehandlingRepository behandlingRepository,
                                               FagsakRelasjonRepository fagsakRelasjonRepository,
                                               SøknadRepository søknadRepository,
                                               BehandlingLåsRepository behandlingLåsRepository) {

        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository);
        this.fagsakRelasjonRepository = Objects.requireNonNull(fagsakRelasjonRepository);
        this.søknadRepository = Objects.requireNonNull(søknadRepository);
        this.behandlingLåsRepository = Objects.requireNonNull(behandlingLåsRepository);
    }

    EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Hent første henlagte endringssøknad etter siste innvilgede behandlinger for en fagsak
     */
    public List<Behandling> finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, "fagsakId");

        var sisteInnvilgede = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);

        if (sisteInnvilgede.isPresent()) {
            var behandlingsIder = finnHenlagteBehandlingerEtter(fagsakId, sisteInnvilgede.get());
            for (var behandlingId : behandlingsIder) {
                behandlingLåsRepository.taLås(behandlingId);
            }
            return behandlingsIder.stream()
                .map(behandlingId -> behandlingRepository.hentBehandling(behandlingId))
                .toList();
        }
        return Collections.emptyList();
    }

    public Optional<Behandling> hentAktivIkkeBerørtEllerSisteYtelsesbehandling(Long fagsakId) {
        // Det kan ligge avsluttet berørt opprettet senere enn åpen behandling
        return finnÅpenogKøetYtelsebehandling(fagsakId).stream()
            .filter(SpesialBehandling::erIkkeSpesialBehandling)
            .findFirst()
            .or(() -> hentSisteYtelsesbehandling(fagsakId));
    }

    private Optional<Behandling> hentSisteYtelsesbehandling(Long fagsakId) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
    }

    private List<Long> finnHenlagteBehandlingerEtter(Long fagsakId, Behandling sisteInnvilgede) {
        var query = getEntityManager().createQuery("""
            SELECT b.id FROM Behandling b WHERE b.fagsak.id=:fagsakId
             AND b.behandlingType=:type
             AND b.opprettetTidspunkt >= to_timestamp(:etterTidspunkt)
             AND EXISTS (SELECT r FROM Behandlingsresultat r
                WHERE r.behandling=b
                AND r.behandlingResultatType IN :henlagtKoder)
             ORDER BY b.opprettetTidspunkt ASC
            """, Long.class);
        query.setParameter("fagsakId", fagsakId);
        query.setParameter("type", BehandlingType.REVURDERING);
        query.setParameter("henlagtKoder", BehandlingResultatType.getAlleHenleggelseskoder());
        query.setParameter("etterTidspunkt", sisteInnvilgede.getOpprettetDato());
        return query.getResultList();
    }

    public Optional<Behandling> finnÅpenYtelsesbehandling(Long fagsakId) {
        var åpenBehandling = finnÅpenogKøetYtelsebehandling(fagsakId).stream()
            .filter(beh -> !beh.erKøet())
            .toList();
        check(åpenBehandling.size() <= 1, "Kan maks ha én åpen ytelsesbehandling");
        return optionalFirst(åpenBehandling);
    }

    public Optional<Behandling> finnKøetYtelsesbehandling(Long fagsakId) {
        var køetBehandling = finnÅpenogKøetYtelsebehandling(fagsakId).stream()
            .filter(Behandling::erKøet)
            .toList();
        check(køetBehandling.size() <= 1, "Kan maks ha én køet ytelsesbehandling");
        return optionalFirst(køetBehandling);
    }

    private List<Behandling> finnÅpenogKøetYtelsebehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, "fagsakId");

        var query = getEntityManager().createQuery("""
            SELECT b.id from Behandling b
            where fagsak.id=:fagsakId
             and status not in (:avsluttet)
             and behandlingType in (:behandlingType)
             order by opprettetTidspunkt desc
           """, Long.class);
        query.setParameter("fagsakId", fagsakId);
        query.setParameter(AVSLUTTET_KEY, BehandlingStatus.getFerdigbehandletStatuser());
        query.setParameter("behandlingType", BehandlingType.getYtelseBehandlingTyper());

        var behandlingIder = query.getResultList();
        for (var behandlingId : behandlingIder) {
            behandlingLåsRepository.taLås(behandlingId);
        }
        var behandlinger = behandlingIder.stream().map(behandlingId -> behandlingRepository.hentBehandling(behandlingId)).toList();
        check(behandlinger.size() <= 2, "Kan maks ha én åpen og én køet ytelsesbehandling");
        check(behandlinger.stream().filter(Behandling::erKøet).count() <= 1, "Kan maks ha én køet ytelsesbehandling");
        check(behandlinger.stream().filter(it -> !it.erKøet()).count() <= 1, "Kan maks ha én åpen ytelsesbehandling");

        return behandlinger;
    }

    public Optional<Behandling> finnKøetBehandlingMedforelder(Fagsak fagsak) {
        return finnFagsakPåMedforelder(fagsak).flatMap(fs -> finnKøetYtelsesbehandling(fs.getId()));
    }

    public Optional<Fagsak> finnFagsakPåMedforelder(Fagsak fagsak) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak).flatMap(fr -> fr.getRelatertFagsak(fagsak));
    }

    public Optional<Behandling> finnSisteVedtatteIkkeHenlagteBehandlingForMedforelder(Fagsak fagsak) {
        return finnFagsakPåMedforelder(fagsak).flatMap(fs -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fs.getId()));
    }

    public Optional<Behandling> finnSisteInnvilgetBehandlingForMedforelder(Fagsak fagsak) {
        return finnFagsakPåMedforelder(fagsak).flatMap(fs -> behandlingRepository.finnSisteInnvilgetBehandling(fs.getId()));
    }

    public Optional<LocalDate> finnSøknadsdatoFraHenlagtBehandling(Behandling behandling) {
        var henlagteBehandlinger = finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(behandling.getFagsak().getId());
        var søknad = finnFørsteSøknadBlantBehandlinger(henlagteBehandlinger);
        return søknad.map(SøknadEntitet::getSøknadsdato);
    }

    private Optional<SøknadEntitet> finnFørsteSøknadBlantBehandlinger(List<Behandling> behandlinger) {
        return behandlinger.stream()
            .map(behandling -> søknadRepository.hentSøknadHvisEksisterer(behandling.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private static void check(boolean check, String message) {
        if (!check) {
            throw new IllegalArgumentException(message);
        }
    }

    private static Optional<Behandling> optionalFirst(List<Behandling> behandlinger) {
        return behandlinger.isEmpty() ? Optional.empty() : Optional.of(behandlinger.get(0));
    }
}
