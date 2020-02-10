package no.nav.foreldrepenger.behandlingslager.uttak;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.TraverseEntityGraphFactory;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class UttakRepository  {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public UttakRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
    }

    protected UttakRepository() {
        // CDI proxy
    }

    public void lagreOpprinneligUttakResultatPerioder(Long behandlingId, UttakResultatPerioderEntitet opprinneligPerioder) {
        lagreUttaksresultat(behandlingId, builder -> builder.nullstill().medOpprinneligPerioder(opprinneligPerioder));
    }

    public void lagreOverstyrtUttakResultatPerioder(Long behandlingId, UttakResultatPerioderEntitet overstyrtPerioder) {
        lagreUttaksresultat(behandlingId, builder -> builder.medOverstyrtPerioder(overstyrtPerioder));
    }

    private void lagreUttaksresultat(Long behandlingId, Function<UttakResultatEntitet.Builder, UttakResultatEntitet.Builder> resultatTransformator) {
        final BehandlingLås lås = behandlingLåsRepository.taLås(behandlingId);

        Optional<UttakResultatEntitet> eksistrendeResultat = hentUttakResultatHvisEksisterer(behandlingId);

        UttakResultatEntitet.Builder builder = new UttakResultatEntitet.Builder(hentBeregningsresultat(behandlingId));
        if (eksistrendeResultat.isPresent()) {
            UttakResultatEntitet eksisterende = eksistrendeResultat.get();
            if (eksisterende.getOpprinneligPerioder() != null) {
                builder.medOpprinneligPerioder(eksisterende.getOpprinneligPerioder());
            }
            if (eksisterende.getOverstyrtPerioder() != null) {
                builder.medOverstyrtPerioder(eksisterende.getOverstyrtPerioder());
            }
            deaktiverResultat(eksisterende);
        }
        builder = resultatTransformator.apply(builder);

        UttakResultatEntitet nyttResultat = builder.build();

        persistResultat(nyttResultat);
        verifiserBehandlingLås(lås);
        entityManager.flush();
    }

    private Behandlingsresultat hentBeregningsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Må ha beregningsresultat ved lagring av uttak"));
    }

    private void persistResultat(UttakResultatEntitet resultat) {
        UttakResultatPerioderEntitet overstyrtPerioder = resultat.getOverstyrtPerioder();
        if (overstyrtPerioder != null) {
            persistPerioder(overstyrtPerioder);
        }
        UttakResultatPerioderEntitet opprinneligPerioder = resultat.getOpprinneligPerioder();
        if (opprinneligPerioder != null) {
            persistPerioder(opprinneligPerioder);
        }
        entityManager.persist(resultat);
    }

    private void persistPerioder(UttakResultatPerioderEntitet perioder) {
        entityManager.persist(perioder);
        for (UttakResultatPeriodeEntitet periode : perioder.getPerioder()) {
            persisterPeriode(periode);
        }
    }

    private void persisterPeriode(UttakResultatPeriodeEntitet periode) {
        if (periode.getPeriodeSøknad().isPresent()) {
            persistPeriodeSøknad(periode.getPeriodeSøknad().get());
        }
        entityManager.persist(periode);
        if (periode.getDokRegel() != null) {
            entityManager.persist(periode.getDokRegel());
        }
        for (UttakResultatPeriodeAktivitetEntitet periodeAktivitet : periode.getAktiviteter()) {
            persistAktivitet(periodeAktivitet);
        }
    }

    private void persistPeriodeSøknad(UttakResultatPeriodeSøknadEntitet periodeSøknad) {
        if (periodeSøknad != null) {
            entityManager.persist(periodeSøknad);
        }
    }

    private void persistAktivitet(UttakResultatPeriodeAktivitetEntitet periodeAktivitet) {
        persistUttakAktivitet(periodeAktivitet.getUttakAktivitet());
        entityManager.persist(periodeAktivitet);
    }

    private void persistUttakAktivitet(UttakAktivitetEntitet uttakAktivitet) {
        entityManager.persist(uttakAktivitet);
    }

    private void deaktiverResultat(UttakResultatEntitet resultat) {
        resultat.deaktiver();
        entityManager.persist(resultat);
        entityManager.flush();
    }

    public Optional<UttakResultatEntitet> hentUttakResultatHvisEksisterer(Long behandlingId) {
        TypedQuery<UttakResultatEntitet> query = entityManager.createQuery(
            "select uttakResultat from UttakResultatEntitet uttakResultat " +
                "join uttakResultat.behandlingsresultat resultat" +
                " where resultat.behandling.id=:behandlingId and uttakResultat.aktiv='J'", UttakResultatEntitet.class); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); // NOSONAR //$NON-NLS-1$
        return hentUniktResultat(query);
    }

    public UttakResultatEntitet hentUttakResultat(Long behandlingId) {
        Optional<UttakResultatEntitet> resultat = hentUttakResultatHvisEksisterer(behandlingId);
        return resultat.orElseThrow(() -> new NoResultException("Fant ikke uttak resultat på behandlingen " + behandlingId + ", selv om det var forventet."));
    }

    private Optional<Uttaksperiodegrense> getAktivtUttaksperiodegrense(Behandlingsresultat behandlingsresultat) {
        Objects.requireNonNull(behandlingsresultat, "behandlingsresultat"); // NOSONAR $NON-NLS-1$
        final TypedQuery<Uttaksperiodegrense> query = entityManager.createQuery("FROM Uttaksperiodegrense Upg " +
            "WHERE Upg.behandlingsresultat.id = :behandlingresultatId " +
            "AND Upg.aktiv = :aktivt", Uttaksperiodegrense.class);
        query.setParameter("behandlingresultatId", behandlingsresultat.getId());
        query.setParameter("aktivt", true);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<UttakResultatEntitet> hentUttakResultatPåId(Long id) {
        Objects.requireNonNull(id, "aggregatId"); // NOSONAR $NON-NLS-1$
        final TypedQuery<UttakResultatEntitet> query = entityManager.createQuery("FROM UttakResultatEntitet ur " +
            "WHERE ur.id = :id ", UttakResultatEntitet.class);
        query.setParameter("id", id);
        return HibernateVerktøy.hentUniktResultat(query);
    }


    public void lagreUttaksperiodegrense(Long behandlingId, Uttaksperiodegrense uttaksperiodegrense) {
        final BehandlingLås lås = behandlingLåsRepository.taLås(behandlingId);
        Behandlingsresultat behandlingsresultat = hentBeregningsresultat(behandlingId);
        final Optional<Uttaksperiodegrense> tidligereAggregat = getAktivtUttaksperiodegrense(behandlingsresultat);
        if (tidligereAggregat.isPresent()) {
            final Uttaksperiodegrense aggregat = tidligereAggregat.get();
            boolean erForskjellig = uttaksperiodegrenseAggregatDiffer().areDifferent(aggregat, uttaksperiodegrense);
            if (erForskjellig) {
                aggregat.setAktiv(false);
                entityManager.persist(aggregat);
                entityManager.flush();
            }
        }
        behandlingsresultat.leggTilUttaksperiodegrense(uttaksperiodegrense);
        entityManager.persist(uttaksperiodegrense);
        verifiserBehandlingLås(lås);
        entityManager.flush();
    }

    public void ryddUttaksperiodegrense(Long behandlingId) {
        BehandlingLås lås = behandlingLåsRepository.taLås(behandlingId);
        Behandlingsresultat behandlingsresultat = hentBeregningsresultat(behandlingId);
        Optional<Uttaksperiodegrense> aktivtAggregat = getAktivtUttaksperiodegrense(behandlingsresultat);
        if (aktivtAggregat.isPresent()) {
            Uttaksperiodegrense aggregat = aktivtAggregat.get();
            aggregat.setAktiv(false);
            entityManager.persist(aggregat);
            verifiserBehandlingLås(lås);
            entityManager.flush();
        }
    }

    private Optional<Long> finnAktivUttakId(Long behandlingId){
        return hentUttakResultatHvisEksisterer(behandlingId).map(UttakResultatEntitet::getId);
    }

    //Denne metoden bør legges i Tjeneste
    public EndringsresultatSnapshot finnAktivAggregatId(Long behandlingId){
        Optional<Long> funnetId = finnAktivUttakId(behandlingId);
        return funnetId
            .map(id-> EndringsresultatSnapshot.medSnapshot(UttakResultatEntitet.class,id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(UttakResultatEntitet.class));
    }


    private Optional<Long> finnAktivUttakPeriodeGrenseId(Long behandlingId){
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if(behandlingsresultat.isEmpty()){
            return Optional.empty();
        }
        return getAktivtUttaksperiodegrense(behandlingsresultat.get()).map(Uttaksperiodegrense::getId);
    }

    //Denne metoden bør legges i Tjeneste
    public EndringsresultatSnapshot finnAktivUttakPeriodeGrenseAggregatId(Long behandlingId){
        Optional<Long> funnetId = finnAktivUttakPeriodeGrenseId(behandlingId);
        return funnetId
            .map(id-> EndringsresultatSnapshot.medSnapshot(Uttaksperiodegrense.class,id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(Uttaksperiodegrense.class));
    }

    public Uttaksperiodegrense hentUttaksperiodegrense(Long behandlingId) {
        TypedQuery<Uttaksperiodegrense> query = entityManager
            .createQuery("select u from Uttaksperiodegrense u " +
                "where u.behandlingsresultat.behandling.id = :behandlingId " +
                "and u.aktiv = true", Uttaksperiodegrense.class)
            .setParameter("behandlingId", behandlingId); // NOSONAR
        return hentEksaktResultat(query);
    }

    public Optional<Uttaksperiodegrense> hentUttaksperiodegrenseHvisEksisterer(Long behandlingId) {
        TypedQuery<Uttaksperiodegrense> query = entityManager
            .createQuery("select u from Uttaksperiodegrense u " +
                "where u.behandlingsresultat.behandling.id = :behandlingId " +
                "and u.aktiv = true", Uttaksperiodegrense.class)
            .setParameter("behandlingId", behandlingId); // NOSONAR
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public List<OrgManuellÅrsakEntitet> finnOrgManuellÅrsak(String virksomhetsnummer) {
        TypedQuery<OrgManuellÅrsakEntitet> query = entityManager
            .createQuery("select u from OrgManuellÅrsakEntitet u " +
                "where u.virksomhetsnummer = :virksomhetsnummer", OrgManuellÅrsakEntitet.class)
            .setParameter("virksomhetsnummer", virksomhetsnummer);
        return query.getResultList();
    }

    public void deaktivterAktivtResultat(Long behandlingId) {
        Optional<UttakResultatEntitet> uttakResultat = hentUttakResultatHvisEksisterer(behandlingId);
        uttakResultat.ifPresent(this::deaktiverResultat);
    }

    private DiffEntity uttaksperiodegrenseAggregatDiffer() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build(false);
        return new DiffEntity(traverser);
    }

    private void verifiserBehandlingLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
    }
}
