package no.nav.foreldrepenger.behandlingslager.behandling.innsyn;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class InnsynRepository {

    private EntityManager entityManager;

    InnsynRepository() {
        // for CDI proxy
    }

    @Inject
    public InnsynRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public void lagreInnsyn(InnsynEntitet innsyn, Collection<? extends InnsynDokumentEntitet> innsynDokumenter) {
        entityManager.persist(innsyn);
        innsyn.getInnsynDokumenterOld().clear();

        innsynDokumenter.forEach(dok -> {
            var entitet = new InnsynDokumentEntitet(dok.isFikkInnsyn(), dok.getJournalpostId(), dok.getDokumentId());
            entitet.setInnsyn(innsyn);
            innsyn.getInnsynDokumenterOld().add(entitet);
        });
        entityManager.persist(innsyn);
        entityManager.flush();
    }

    public Optional<InnsynEntitet> hentForBehandling(long behandlingId) {
        var query = entityManager.createQuery("from Innsyn where behandlingId = :behandlingId", InnsynEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public List<InnsynDokumentEntitet> hentDokumenterForInnsyn(long innsynId) {
        var query = entityManager.createQuery("from InnsynDokument where innsyn.id=:innsynId", InnsynDokumentEntitet.class);
        query.setParameter("innsynId", innsynId);
        return query.getResultList();
    }
}
