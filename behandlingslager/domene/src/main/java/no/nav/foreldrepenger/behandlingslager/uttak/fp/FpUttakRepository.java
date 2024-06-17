package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class FpUttakRepository {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public FpUttakRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
    }

    protected FpUttakRepository() {
        // CDI proxy
    }


    public void lagreOpprinneligUttakResultatPerioder(Long behandlingId,
                                                      Stønadskontoberegning stønadskontoberegning,
                                                      UttakResultatPerioderEntitet opprinneligPerioder) {
        // Nullstilling er forventet - fjerner evt overstyring
        lagreUttaksresultat(behandlingId,
            builder -> builder.nullstill().medOpprinneligPerioder(opprinneligPerioder).medStønadskontoberegning(stønadskontoberegning));
    }

    // Kun testformål
    public void lagreOpprinneligUttakResultatPerioder(Long behandlingId, UttakResultatPerioderEntitet opprinneligPerioder) {
        lagreOpprinneligUttakResultatPerioder(behandlingId, null, opprinneligPerioder);
    }

    public void lagreOverstyrtUttakResultatPerioder(Long behandlingId, UttakResultatPerioderEntitet overstyrtPerioder) {
        lagreUttaksresultat(behandlingId, builder -> builder.medOverstyrtPerioder(overstyrtPerioder));
    }

    private void lagreUttaksresultat(Long behandlingId, UnaryOperator<UttakResultatEntitet.Builder> resultatTransformator) {
        var lås = behandlingLåsRepository.taLås(behandlingId);

        var eksistrendeResultat = hentUttakResultatHvisEksisterer(behandlingId);

        var builder = new UttakResultatEntitet.Builder(hentBehandlingsresultat(behandlingId));
        if (eksistrendeResultat.isPresent()) {
            var eksisterende = eksistrendeResultat.get();
            if (eksisterende.getStønadskontoberegning() != null) {
                builder.medStønadskontoberegning(eksisterende.getStønadskontoberegning());
            }
            if (eksisterende.getOpprinneligPerioder() != null) {
                builder.medOpprinneligPerioder(eksisterende.getOpprinneligPerioder());
            }
            if (eksisterende.getOverstyrtPerioder() != null) {
                builder.medOverstyrtPerioder(eksisterende.getOverstyrtPerioder());
            }
            deaktiverResultat(eksisterende);
        }
        builder = resultatTransformator.apply(builder);

        var nyttResultat = builder.build();

        persistResultat(nyttResultat);
        verifiserBehandlingLås(lås);
        entityManager.flush();
    }

    private Behandlingsresultat hentBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Må ha behandlingsresultat ved lagring av uttak. Behandling " + behandlingId));
    }

    private void persistResultat(UttakResultatEntitet resultat) {
        var kontoutregning = resultat.getStønadskontoberegning();
        if (kontoutregning != null) {
            persistKontoutregning(kontoutregning);
        }
        var overstyrtPerioder = resultat.getOverstyrtPerioder();
        if (overstyrtPerioder != null) {
            persistPerioder(overstyrtPerioder);
        }
        var opprinneligPerioder = resultat.getOpprinneligPerioder();
        if (opprinneligPerioder != null) {
            persistPerioder(opprinneligPerioder);
        }
        entityManager.persist(resultat);
    }

    private void persistKontoutregning(Stønadskontoberegning stønadskontoberegning) {
        entityManager.persist(stønadskontoberegning);
        for (var stønadskonto : stønadskontoberegning.getStønadskontoer()) {
            entityManager.persist(stønadskonto);
        }
    }

    private void persistPerioder(UttakResultatPerioderEntitet perioder) {
        entityManager.persist(perioder);
        for (var periode : perioder.getPerioder()) {
            persisterPeriode(periode);
        }
    }

    private void persisterPeriode(UttakResultatPeriodeEntitet periode) {
        periode.getPeriodeSøknad().ifPresent(this::persistPeriodeSøknad);
        entityManager.persist(periode);
        if (periode.getDokRegel() != null) {
            entityManager.persist(periode.getDokRegel());
        }
        for (var periodeAktivitet : periode.getAktiviteter()) {
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
        var query = entityManager.createQuery(
            "select uttakResultat from UttakResultatEntitet uttakResultat " + "join uttakResultat.behandlingsresultat resultat"
                + " where resultat.behandling.id=:behandlingId and uttakResultat.aktiv = true", UttakResultatEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        return hentUniktResultat(query);
    }

    public UttakResultatEntitet hentUttakResultat(Long behandlingId) {
        var resultat = hentUttakResultatHvisEksisterer(behandlingId);
        return resultat.orElseThrow(
            () -> new NoResultException("Fant ikke uttak resultat på behandlingen " + behandlingId + ", selv om det var forventet."));
    }

    public Optional<UttakResultatEntitet> hentUttakResultatPåId(Long id) {
        Objects.requireNonNull(id, "aggregatId");
        var query = entityManager.createQuery("FROM UttakResultatEntitet ur " + "WHERE ur.id = :id ", UttakResultatEntitet.class);
        query.setParameter("id", id);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void deaktivterAktivtResultat(Long behandlingId) {
        var uttakResultat = hentUttakResultatHvisEksisterer(behandlingId);
        uttakResultat.ifPresent(this::deaktiverResultat);
    }

    private void verifiserBehandlingLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
    }
}
