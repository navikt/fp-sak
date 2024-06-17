package no.nav.foreldrepenger.behandlingslager.aktør;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class NavBrukerRepository {

    private EntityManager entityManager;

    NavBrukerRepository() {
        // for CDI proxy
    }

    @Inject
    public NavBrukerRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Optional<NavBruker> hent(AktørId aktorId) {
        var query = entityManager.createQuery("from Bruker where aktørId=:aktorId", NavBruker.class);
        query.setParameter("aktorId", aktorId);
        return hentUniktResultat(query);
    }

    public void lagre(NavBruker bruker) {
        entityManager.persist(bruker);
        entityManager.flush();
    }

    public Optional<NavBruker> oppdaterSpråk(AktørId aktørId, Språkkode språk) {
        // For å unngå å måtte låse en stabel med saker og inkrmentere versjonsnummer.
        if (språk != null && !Språkkode.UDEFINERT.equals(språk)) {
            var query = entityManager.createNativeQuery("UPDATE BRUKER SET sprak_kode = :sprak WHERE AKTOER_ID=:aktoer");
            query.setParameter("aktoer", aktørId.getId());
            query.setParameter("sprak", språk.getKode());
            query.executeUpdate();
            entityManager.flush();
            hent(aktørId).ifPresent(b -> entityManager.refresh(b));
        }
        return hent(aktørId);
    }

}
