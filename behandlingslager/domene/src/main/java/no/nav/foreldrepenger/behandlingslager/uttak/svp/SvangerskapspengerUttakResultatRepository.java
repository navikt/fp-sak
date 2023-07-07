package no.nav.foreldrepenger.behandlingslager.uttak.svp;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;

@ApplicationScoped
public class SvangerskapspengerUttakResultatRepository {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    public SvangerskapspengerUttakResultatRepository() {
        //For rammeverkets skyld
    }

    @Inject
    public SvangerskapspengerUttakResultatRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
    }

    public void lagre(Long behandlingId, SvangerskapspengerUttakResultatEntitet svangerskapspengerUttakResultatEntitet) {
        var lås = behandlingLåsRepository.taLås(behandlingId);
        var eksisterendeUttak = hentHvisEksisterer(behandlingId);
        eksisterendeUttak.ifPresent(this::deaktiverResultat);
        entityManager.persist(svangerskapspengerUttakResultatEntitet);
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
        entityManager.flush();
    }

    public Optional<SvangerskapspengerUttakResultatEntitet> hentHvisEksisterer(Long behandlingId) {
        var query = entityManager.createQuery(
                "select uttakResultat from SvangerskapspengerUttakResultatEntitet uttakResultat " +
                "join uttakResultat.behandlingsresultat resultat " +
                "where resultat.behandling.id = :behandlingId and uttakResultat.aktiv='J'",
            SvangerskapspengerUttakResultatEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        return hentUniktResultat(query);
    }

    private void deaktiverResultat(SvangerskapspengerUttakResultatEntitet resultat) {
        resultat.deaktiver();
        entityManager.persist(resultat);
        entityManager.flush();
    }

}
