package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class LegacyESBeregningRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    LegacyESBeregningRepository() {
        // for CDI proxy
    }

    public LegacyESBeregningRepository(EntityManager entityManager) {
        // for test
        this(entityManager, new BehandlingRepository(entityManager), new  BehandlingsresultatRepository(entityManager));
    }

    @Inject
    public LegacyESBeregningRepository( EntityManager entityManager, BehandlingRepository behandlingRepository, BehandlingsresultatRepository behandlingsresultatRepository) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.entityManager = entityManager;
    }

    private EntityManager getEntityManager() {
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
        TypedQuery<BeregningSats> query = entityManager.createQuery("from BeregningSats where satsType=:satsType" + //$NON-NLS-1$
                " and periode.fomDato<=:dato" + //$NON-NLS-1$
                " and periode.tomDato>=:dato", BeregningSats.class); //$NON-NLS-1$

        query.setParameter("satsType", satsType); //$NON-NLS-1$
        query.setParameter("dato", dato); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true");//$NON-NLS-1$
        query.getResultList();
        return hentEksaktResultat(query);
    }

    public Optional<LegacyESBeregning> getSisteBeregning(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .map(Behandlingsresultat::getBeregningResultat)
            .flatMap(LegacyESBeregningsresultat::getSisteBeregning);
    }

    public boolean skalReberegne(Long behandlingId, LocalDate fødselsdato) {
        var vedtakSats = getSisteBeregning(behandlingId).map(LegacyESBeregning::getSatsVerdi).orElse(0L);
        var satsVedFødsel = finnEksaktSats(BeregningSatsType.ENGANG, fødselsdato).getVerdi();
        return vedtakSats != satsVedFødsel;
    }

    public void lagreBeregning(Long behandlingId, LegacyESBeregning nyBeregning) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        LegacyESBeregning sisteBeregning = getSisteBeregning(behandlingId).orElse(null);

        LegacyESBeregningsresultat beregningResultat = (sisteBeregning == null ? LegacyESBeregningsresultat.builder()
                : LegacyESBeregningsresultat.builderFraEksisterende(sisteBeregning.getBeregningResultat()))
                        .medBeregning(nyBeregning)
                        .buildFor(behandling, behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null));

        BehandlingLås skriveLås = taSkriveLås(behandlingId);
        lagre(beregningResultat, skriveLås);
    }

    protected BehandlingLås taSkriveLås(Long behandlingId) {
        return behandlingRepository.taSkriveLås(behandlingId);
    }

    // sjekk lås og oppgrader til skriv
    protected void verifiserBehandlingLås(BehandlingLås lås) {
        BehandlingLåsRepository låsHåndterer = new BehandlingLåsRepository(getEntityManager());
        låsHåndterer.oppdaterLåsVersjon(lås);
    }
}
