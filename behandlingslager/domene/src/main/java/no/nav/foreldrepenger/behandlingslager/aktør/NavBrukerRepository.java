package no.nav.foreldrepenger.behandlingslager.aktør;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class NavBrukerRepository {

    private EntityManager entityManager;

    NavBrukerRepository() {
        // for CDI proxy
    }

    @Inject
    public NavBrukerRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<NavBruker> hent(AktørId aktorId) {
        TypedQuery<NavBruker> query = entityManager.createQuery("from Bruker where aktørId=:aktorId", NavBruker.class);
        query.setParameter("aktorId", aktorId);
        return hentUniktResultat(query);
    }

}
