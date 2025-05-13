package no.nav.foreldrepenger.behandlingskontroll.spi;

import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;

/**
 * Provider for å enklere å kunne hente ut ulike repository uten for mange
 * injection points.
 */
@ApplicationScoped
public class BehandlingskontrollServiceProvider {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;
    private FagsakRepository fagsakRepository;
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakLåsRepository fagsakLåsRepository;
    private TekniskRepository tekniskRepository;
    private BehandlingskontrollEventPubliserer eventPubliserer;

    @Inject
    public BehandlingskontrollServiceProvider(EntityManager entityManager, BehandlingskontrollEventPubliserer eventPubliserer) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;

        // behandling repositories
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        this.fagsakRepository = new FagsakRepository(entityManager);
        this.aksjonspunktKontrollRepository = new AksjonspunktKontrollRepository();
        this.fagsakLåsRepository = new FagsakLåsRepository(entityManager);
        this.tekniskRepository = new TekniskRepository(entityManager);
        this.eventPubliserer = Objects.requireNonNullElse(eventPubliserer, BehandlingskontrollEventPubliserer.NULL_EVENT_PUB);
    }

    public BehandlingskontrollServiceProvider(FagsakRepository fagsakRepository,
            BehandlingRepository behandlingRepository,
            FagsakLåsRepository fagsakLåsRepository,
            BehandlingLåsRepository behandlingLåsRepository, AksjonspunktKontrollRepository aksjonspunktKontrollRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.fagsakLåsRepository = fagsakLåsRepository;
        this.behandlingLåsRepository = behandlingLåsRepository;
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
        this.eventPubliserer = BehandlingskontrollEventPubliserer.NULL_EVENT_PUB;
    }

    BehandlingskontrollServiceProvider() {
        // for CDI proxy
    }

    public AksjonspunktKontrollRepository getAksjonspunktKontrollRepository() {
        return aksjonspunktKontrollRepository;
    }

    public BehandlingskontrollEventPubliserer getEventPubliserer() {
        return eventPubliserer;
    }

    public BehandlingRepository getBehandlingRepository() {
        return behandlingRepository;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return fagsakRepository;
    }

    public TekniskRepository getTekniskRepository() {
        return tekniskRepository;
    }

    public Behandling hentBehandling(Long behandlingId) {
        return getBehandlingRepository().hentBehandling(behandlingId);
    }

    public Behandling hentBehandling(UUID behandlingUuid) {
        return getBehandlingRepository().hentBehandling(behandlingUuid);
    }

    public void lagreOgClear(Behandling behandling, BehandlingLås skriveLås) {
        getBehandlingRepository().lagreOgClear(behandling, skriveLås);
    }

    public BehandlingLås taLås(Long behandlingId) {
        return behandlingLåsRepository.taLås(behandlingId);
    }

    public BehandlingLås taLås(UUID behandlingUuid) {
        return behandlingLåsRepository.taLås(behandlingUuid);
    }

    public FagsakLås taFagsakLås(Long fagsakId) {
        return fagsakLåsRepository.taLås(fagsakId);
    }

    public void oppdaterLåsVersjon(FagsakLås lås) {
        fagsakLåsRepository.oppdaterLåsVersjon(lås);
    }
}
