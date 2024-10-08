package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class LegacyESBeregningRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private SatsRepository satsRepository;

    LegacyESBeregningRepository() {
        // for CDI proxy
    }

    public LegacyESBeregningRepository(EntityManager entityManager) {
        // for test
        this(entityManager, new BehandlingRepository(entityManager), new SatsRepository(entityManager));
    }

    @Inject
    public LegacyESBeregningRepository( EntityManager entityManager, BehandlingRepository behandlingRepository, SatsRepository satsRepository) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
        this.satsRepository = satsRepository;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Lagrer beregnignsresultat på behandling. Sørger for at samtidige oppdateringer på samme Behandling, eller andre Behandlinger
     * på samme Fagsak ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     */
    public void lagre(LegacyESBeregningsresultat beregningResultat, BehandlingLås lås) {
        getEntityManager().persist(beregningResultat);
        beregningResultat.getBeregninger().forEach(beregning -> getEntityManager().persist(beregning));
        verifiserBehandlingLås(lås);
        getEntityManager().flush();
    }

    public Optional<LegacyESBeregning> getSisteBeregning(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return getSisteBeregning(behandling);
    }

    private Optional<LegacyESBeregning> getSisteBeregning(Behandling behandling) {
        return Optional.ofNullable(behandling.getBehandlingsresultat()).map(Behandlingsresultat::getBeregningResultat).flatMap(LegacyESBeregningsresultat::getSisteBeregning);
    }

    public boolean skalReberegne(Long behandlingId, LocalDate fødselsdato) {
        var vedtakSats = getSisteBeregning(behandlingId).map(LegacyESBeregning::getSatsVerdi).orElse(0L);
        var satsVedFødsel = satsRepository.finnEksaktSats(BeregningSatsType.ENGANG, fødselsdato).getVerdi();
        return vedtakSats != satsVedFødsel;
    }

    public void lagreBeregning(Long behandlingId, LegacyESBeregning nyBeregning) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var sisteBeregning = getSisteBeregning(behandlingId).orElse(null);

        var beregningResultat = (sisteBeregning == null ? LegacyESBeregningsresultat.builder()
                : LegacyESBeregningsresultat.builderFraEksisterende(sisteBeregning.getBeregningResultat()))
                        .medBeregning(nyBeregning)
                        .buildFor(behandling, behandling.getBehandlingsresultat());

        var skriveLås = taSkriveLås(behandlingId);
        lagre(beregningResultat, skriveLås);
    }

    protected BehandlingLås taSkriveLås(Long behandlingId) {
        return behandlingRepository.taSkriveLås(behandlingId);
    }

    // sjekk lås og oppgrader til skriv
    protected void verifiserBehandlingLås(BehandlingLås lås) {
        var låsHåndterer = new BehandlingLåsRepository(getEntityManager());
        låsHåndterer.oppdaterLåsVersjon(lås);
    }
}
