package no.nav.foreldrepenger.behandlingslager.fagsak;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@ApplicationScoped
public class FagsakEgenskapRepository {

    private static final String FAGSAK_QP = "fagsak";

    private EntityManager entityManager;

    protected FagsakEgenskapRepository() {
        // for CDI proxy
    }

    @Inject
    public FagsakEgenskapRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Optional<FagsakEgenskap> finnEgenskapMedVerdi(long fagsakId, EgenskapNøkkel nøkkel) {
        var query = entityManager.createQuery(
            "from FagsakEgenskap where fagsakId = :fagsak AND egenskapNøkkel = :nøkkel AND egenskapVerdi is not null AND aktiv = true",
            FagsakEgenskap.class)
            .setParameter(FAGSAK_QP, fagsakId)
            .setParameter("nøkkel", nøkkel);
        return hentUniktResultat(query);
    }

    public <E extends Enum<E>> Optional<E> finnEgenskapVerdi(Class<E> enumCls, long fagsakId, EgenskapNøkkel nøkkel) {
        return finnEgenskap(fagsakId, nøkkel).map(fe -> fe.getEgenskapVerdi(enumCls));
    }

    public Optional<FagsakEgenskap> finnEgenskap(long fagsakId, EgenskapNøkkel nøkkel) {
        var query = entityManager.createQuery(
                "from FagsakEgenskap where fagsakId = :fagsak AND egenskapNøkkel = :nøkkel AND aktiv = true",
                FagsakEgenskap.class)
            .setParameter(FAGSAK_QP, fagsakId)
            .setParameter("nøkkel", nøkkel);
        return hentUniktResultat(query);
    }

    public List<FagsakEgenskap> finnEgenskaper(long fagsakId) {
        var query = entityManager.createQuery(
            "from FagsakEgenskap where fagsakId = :fagsak AND egenskapVerdi is not null AND aktiv = true",
            FagsakEgenskap.class)
            .setParameter(FAGSAK_QP, fagsakId);
        return query.getResultList();
    }


    public void fjernEgenskap(long fagsakId, EgenskapNøkkel nøkkel) {
        finnEgenskapMedVerdi(fagsakId, nøkkel).ifPresent(egenskap -> {
            egenskap.fjernEgenskapVerdi();
            entityManager.persist(egenskap);
            entityManager.flush();
        });
    }

    public void lagreEgenskap(long fagsakId, EgenskapVerdi verdi) {
        Objects.requireNonNull(verdi);
        lagreEgenskap(fagsakId, verdi.getNøkkel(), verdi.name());
    }

    public void lagreEgenskap(long fagsakId, EgenskapNøkkel nøkkel, String verdi) {
        Objects.requireNonNull(verdi);
        var eksisterende = finnEgenskap(fagsakId, nøkkel);
        if (eksisterende.filter(e -> Objects.equals(e.getEgenskapVerdi(), verdi)).isPresent()) {
            return;
        }
        var lagres = eksisterende.map(FagsakEgenskap::new).orElseGet(() -> new FagsakEgenskap(fagsakId, nøkkel, verdi));
        lagres.setEgenskapVerdi(verdi);
        eksisterende.ifPresent(e -> {
            e.setAktiv(false);
            entityManager.persist(e);
        });
        entityManager.persist(lagres);
        entityManager.flush();
    }

    // Primært ifm migrering
    public void lagreEllerOppdaterEgenskap(long fagsakId, EgenskapVerdi verdi) {
        Objects.requireNonNull(verdi);
        lagreEllerOppdaterEgenskap(fagsakId, verdi.getNøkkel(), verdi.name());
    }

    public void lagreEllerOppdaterEgenskap(long fagsakId, EgenskapNøkkel nøkkel, String verdi) {
        Objects.requireNonNull(verdi);
        var eksisterende = finnEgenskap(fagsakId, nøkkel);
        if (eksisterende.filter(e -> Objects.equals(e.getEgenskapVerdi(), verdi)).isPresent()) {
            return;
        }
        var lagres = eksisterende.orElseGet(() -> new FagsakEgenskap(fagsakId, nøkkel, verdi));
        lagres.setEgenskapVerdi(verdi);
        entityManager.persist(lagres);
        entityManager.flush();
    }

}
