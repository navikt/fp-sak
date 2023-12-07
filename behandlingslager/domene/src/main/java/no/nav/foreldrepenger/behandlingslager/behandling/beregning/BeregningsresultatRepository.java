package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;

@ApplicationScoped
public class BeregningsresultatRepository {
    private static final long G_MULTIPLIKATOR = 6L;
    private static final long MIL_MULTIPLIKATOR = 3L; // Egentlig 2G for SVP - men denne brukes bare til å plukke kandidater. Så OK med 3G.
    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    protected BeregningsresultatRepository() {
        // for CDI proxy
    }

    @Inject
    public BeregningsresultatRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
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
        var query = entityManager.createQuery(
            "from BeregningsresultatFPAggregatEntitet aggregat " +
                "where aggregat.behandlingId=:behandlingId and aggregat.aktiv = true", BehandlingBeregningsresultatEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        return hentUniktResultat(query);
    }

    public void lagre(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        var builder = opprettResultatBuilderFor(behandling.getId());
        builder.medBgBeregningsresultatFP(beregningsresultat);
        lagreOgFlush(behandling, builder);
    }

    public void lagreUtbetBeregningsresultat(Behandling behandling, BeregningsresultatEntitet utbetBeregningsresultatFP) {
        var builder = opprettResultatBuilderFor(behandling.getId());
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
        var behandlingId = behandling.getId();
        var aggregatOpt = hentBeregningsresultatAggregat(behandlingId);
        if (aggregatOpt.isEmpty()) {
            throw new IllegalStateException("Finner ikke beregningsresultataggregat for behandlingen" + behandlingId);
        }

        var builder = opprettResultatBuilderFor(aggregatOpt);
        builder.medSkalHindreTilbaketrekk(skalHindreTilbaketrekk);
        lagreOgFlush(behandling, builder);

        return aggregatOpt.flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk);
    }

    private void lagreOgFlush(Behandling behandling, BehandlingBeregningsresultatBuilder builder) {
        var tidligereAggregat = hentBeregningsresultatAggregat(behandling.getId());
        if (tidligereAggregat.isPresent()) {
            tidligereAggregat.get().setAktiv(false);
            entityManager.persist(tidligereAggregat.get());
        }
        var aggregatEntitet = builder.build(behandling.getId());
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
        var aggregat = hentBeregningsresultatAggregat(behandlingId);
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
        var aggregatOpt = hentBeregningsresultatAggregat(behandlingId);
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
        var query = entityManager.createQuery("from BeregningSats where satsType=:satsType" +
                " and periode.fomDato<=:dato" +
                " and periode.tomDato>=:dato", BeregningSats.class);

        query.setParameter("satsType", satsType);
        query.setParameter("dato", dato);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        query.getResultList();
        return hentEksaktResultat(query);
    }

    public BeregningSats finnGjeldendeSats(BeregningSatsType satsType) {
        var query = entityManager.createQuery("from BeregningSats where satsType=:satsType", BeregningSats.class);

        query.setParameter("satsType", satsType);
        return query.getResultList().stream()
            .max(Comparator.comparing(s -> s.getPeriode().getFomDato())).orElseThrow(() -> new IllegalStateException("Fant ikke nyeste sats"));
    }

    public void lagreSats(BeregningSats sats) {
        entityManager.persist(sats);
        entityManager.flush();
    }

    public static long avkortingMultiplikatorG() {
        return G_MULTIPLIKATOR;
    }

    public static long militærMultiplikatorG() {
        return MIL_MULTIPLIKATOR;
    }
}
