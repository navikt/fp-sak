package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class LegacyESBeregningRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;

    LegacyESBeregningRepository() {
        // for CDI proxy
    }

    public LegacyESBeregningRepository(EntityManager entityManager) {
        // for test
        this(entityManager, new BehandlingRepository(entityManager));
    }

    @Inject
    public LegacyESBeregningRepository(EntityManager entityManager, BehandlingRepository behandlingRepository) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
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

    public BeregningSats finnEksaktSats(BeregningSatsType satsType, LocalDate dato) {
        var query = entityManager.createQuery(
            "from BeregningSats where satsType=:satsType" + " and periode.fomDato<=:dato" + " and periode.tomDato>=:dato", BeregningSats.class);

        query.setParameter("satsType", satsType);
        query.setParameter("dato", dato);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        query.getResultList();
        return hentEksaktResultat(query);
    }

    public Optional<LegacyESBeregning> getSisteBeregning(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return getSisteBeregning(behandling);
    }

    private Optional<LegacyESBeregning> getSisteBeregning(Behandling behandling) {
        return Optional.ofNullable(behandling.getBehandlingsresultat())
            .map(Behandlingsresultat::getBeregningResultat)
            .flatMap(LegacyESBeregningsresultat::getSisteBeregning);
    }

    public boolean skalReberegne(Long behandlingId, LocalDate fødselsdato) {
        var vedtakSats = getSisteBeregning(behandlingId).map(LegacyESBeregning::getSatsVerdi).orElse(0L);
        var satsVedFødsel = finnEksaktSats(BeregningSatsType.ENGANG, fødselsdato).getVerdi();
        return vedtakSats != satsVedFødsel;
    }

    public void lagreBeregning(Long behandlingId, LegacyESBeregning nyBeregning) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var sisteBeregning = getSisteBeregning(behandlingId).orElse(null);

        var beregningResultat = (sisteBeregning == null ? LegacyESBeregningsresultat.builder() : LegacyESBeregningsresultat.builderFraEksisterende(
            sisteBeregning.getBeregningResultat())).medBeregning(nyBeregning).buildFor(behandling, behandling.getBehandlingsresultat());

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
