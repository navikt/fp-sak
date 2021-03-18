package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;

@ApplicationScoped
public class BeregningsresultatRepository {
    private static final long G_MULTIPLIKATOR = 6L;
    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    protected BeregningsresultatRepository() {
        // for CDI proxy
    }

    @Inject
    public BeregningsresultatRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
    }

    public Optional<BeregningsresultatEntitet> hentBeregningsresultat(Long behandlingId) {
        return hentBeregningsresultatAggregat(behandlingId).map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultatFP);
    }

    public Optional<BeregningsresultatEntitet> hentUtbetBeregningsresultat(Long behandlingId) {
        var aggregat = hentBeregningsresultatAggregat(behandlingId);
        return aggregat.map(BehandlingBeregningsresultatEntitet::getUtbetBeregningsresultatFP)
            .or(() -> aggregat.map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultatFP));
    }

    public Optional<BehandlingBeregningsresultatEntitet> hentBeregningsresultatAggregat(Long behandlingId) {
        TypedQuery<BehandlingBeregningsresultatEntitet> query = entityManager.createQuery(
            "from BeregningsresultatFPAggregatEntitet aggregat " +
                "where aggregat.behandlingId=:behandlingId and aggregat.aktiv = 'J'", BehandlingBeregningsresultatEntitet.class); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        return hentUniktResultat(query);
    }

    public void lagre(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        BehandlingBeregningsresultatBuilder builder = opprettResultatBuilderFor(behandling.getId());
        builder.medBgBeregningsresultatFP(beregningsresultat);
        lagreOgFlush(behandling, builder);
    }

    public void lagreUtbetBeregningsresultat(Behandling behandling, BeregningsresultatEntitet utbetBeregningsresultatFP) {
        BehandlingBeregningsresultatBuilder builder = opprettResultatBuilderFor(behandling.getId());
        builder.medUtbetBeregningsresultatFP(utbetBeregningsresultatFP);
        lagreOgFlush(behandling, builder);
    }

    /**
     * Lagrer beregningsresultataggregatet med en verdi for hindreTilbaketrekk
     *
     * @param behandling en {@link Behandling}
     * @param skalHindreTilbaketrekk skal tilkjent ytelse omfordeles mellom bruker og arbeidsgiver?
     * @return Tidligere verdi
     */
    public Optional<Boolean> lagreMedTilbaketrekk(Behandling behandling, boolean skalHindreTilbaketrekk) {
        Long behandlingId = behandling.getId();
        Optional<BehandlingBeregningsresultatEntitet> aggregatOpt = hentBeregningsresultatAggregat(behandlingId);
        if (!aggregatOpt.isPresent()) {
            throw new IllegalStateException("Finner ikke beregningsresultataggregat for behandlingen" + behandlingId);
        }

        BehandlingBeregningsresultatBuilder builder = opprettResultatBuilderFor(aggregatOpt);
        builder.medSkalHindreTilbaketrekk(skalHindreTilbaketrekk);
        lagreOgFlush(behandling, builder);

        return aggregatOpt.flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk);
    }

    private void lagreOgFlush(Behandling behandling, BehandlingBeregningsresultatBuilder builder) {
        Optional<BehandlingBeregningsresultatEntitet> tidligereAggregat = hentBeregningsresultatAggregat(behandling.getId());
        if (tidligereAggregat.isPresent()) {
            tidligereAggregat.get().setAktiv(false);
            entityManager.persist(tidligereAggregat.get());
        }
        BehandlingBeregningsresultatEntitet aggregatEntitet = builder.build(behandling.getId());
        entityManager.persist(aggregatEntitet.getBgBeregningsresultatFP());
        aggregatEntitet.getBgBeregningsresultatFP().getBeregningsresultatPerioder().forEach(this::lagre);
        if (aggregatEntitet.getUtbetBeregningsresultatFP() != null) {
            entityManager.persist(aggregatEntitet.getUtbetBeregningsresultatFP());
            aggregatEntitet.getUtbetBeregningsresultatFP().getBeregningsresultatPerioder().forEach(this::lagre);
        }
        entityManager.persist(aggregatEntitet);
        entityManager.flush();
    }

    private BehandlingBeregningsresultatBuilder opprettResultatBuilderFor(Long behandlingId) {
        Optional<BehandlingBeregningsresultatEntitet> aggregat = hentBeregningsresultatAggregat(behandlingId);
        return opprettResultatBuilderFor(aggregat);
    }

    private BehandlingBeregningsresultatBuilder opprettResultatBuilderFor(Optional<BehandlingBeregningsresultatEntitet> aggregat) {
        return BehandlingBeregningsresultatBuilder.oppdatere(aggregat);
    }

    private void lagre(BeregningsresultatPeriode beregningsresultatPeriode) {
        entityManager.persist(beregningsresultatPeriode);
        beregningsresultatPeriode.getBeregningsresultatAndelList().forEach(this::lagre);
    }

    private void lagre(BeregningsresultatAndel beregningsresultatAndel) {
        entityManager.persist(beregningsresultatAndel);
    }

    public void deaktiverBeregningsresultat(Long behandlingId, BehandlingLås skriveLås) {
        Optional<BehandlingBeregningsresultatEntitet> aggregatOpt = hentBeregningsresultatAggregat(behandlingId);
        aggregatOpt.ifPresent(aggregat -> setAktivOgLagre(aggregat, false));
        verifiserBehandlingLås(skriveLås);
        entityManager.flush();
    }

    private void setAktivOgLagre(BehandlingBeregningsresultatEntitet aggregat, boolean aktiv) {
        aggregat.setAktiv(aktiv);
        entityManager.persist(aggregat);
        entityManager.flush();
    }

    private void verifiserBehandlingLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
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

    public BeregningSats finnGjeldendeSats(BeregningSatsType satsType) {
        TypedQuery<BeregningSats> query = entityManager.createQuery("from BeregningSats where satsType=:satsType", BeregningSats.class); //$NON-NLS-1$

        query.setParameter("satsType", satsType); //$NON-NLS-1$
        return query.getResultList().stream()
            .max(Comparator.comparing(s -> s.getPeriode().getFomDato())).orElseThrow(() -> new IllegalStateException("Fant ikke nyeste sats"));
    }

    public void lagreSats(BeregningSats sats) {
        entityManager.persist(sats);
        entityManager.flush();
    }

    public long avkortingMultiplikatorG(@SuppressWarnings("unused") LocalDate dato) {
        return G_MULTIPLIKATOR;
    }
}
