package no.nav.foreldrepenger.behandlingslager.fagsak;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandDokumentasjonStatus;

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

    public <E extends Enum<E>> Optional<E> finnEgenskapVerdi(long fagsakId, EgenskapNøkkel nøkkel, Class<E> enumCls) {
        return finnEgenskap(fagsakId, nøkkel).flatMap(fe -> fe.getEgenskapVerdiHvisFinnes(enumCls));
    }

    public Optional<UtlandDokumentasjonStatus> finnUtlandDokumentasjonStatus(long fagsakId) {
        return finnEgenskap(fagsakId, EgenskapNøkkel.UTLAND_DOKUMENTASJON).map(FagsakEgenskap::getEgenskapVerdi).map(UtlandDokumentasjonStatus::valueOf);
    }

    public Optional<FagsakMarkering> finnFagsakMarkering(long fagsakId) {
        return finnEgenskap(fagsakId, EgenskapNøkkel.FAGSAK_MARKERING)
            .map(FagsakEgenskap::getEgenskapVerdi)
            .map(FagsakMarkering::valueOf);
    }

    private Optional<FagsakEgenskap> finnEgenskapMedVerdi(long fagsakId, EgenskapNøkkel nøkkel) {
        var query = entityManager.createQuery(
                "from FagsakEgenskap where fagsakId = :fagsak AND egenskapNøkkel = :nøkkel AND egenskapVerdi is not null AND aktiv = true",
                FagsakEgenskap.class)
            .setParameter(FAGSAK_QP, fagsakId)
            .setParameter("nøkkel", nøkkel);
        return hentUniktResultat(query);
    }

    private Optional<FagsakEgenskap> finnEgenskap(long fagsakId, EgenskapNøkkel nøkkel) {
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


    public void fjernEgenskapBeholdHistorikk(long fagsakId, EgenskapNøkkel nøkkel) {
        finnEgenskapMedVerdi(fagsakId, nøkkel).ifPresent(egenskap -> {
            egenskap.setAktiv(false);
            entityManager.persist(egenskap);
            entityManager.flush();
        });
    }

    public void lagreEgenskapBeholdHistorikk(long fagsakId, EgenskapVerdi verdi) {
        Objects.requireNonNull(verdi);
        var eksisterende = finnEgenskap(fagsakId, verdi.getNøkkel());
        if (eksisterende.filter(e -> e.harSammeVerdi(verdi)).isPresent()) {
            return;
        }
        var lagres = eksisterende.map(FagsakEgenskap::new).orElseGet(() -> new FagsakEgenskap(fagsakId, verdi));
        lagres.setEgenskapVerdi(verdi);
        eksisterende.ifPresent(e -> {
            e.setAktiv(false);
            entityManager.persist(e);
        });
        entityManager.persist(lagres);
        entityManager.flush();
    }

    public void fjernEgenskapUtenHistorikk(long fagsakId, EgenskapNøkkel nøkkel) {
        finnEgenskapMedVerdi(fagsakId, nøkkel).ifPresent(egenskap -> {
            egenskap.fjernEgenskapVerdi();
            entityManager.persist(egenskap);
            entityManager.flush();
        });
    }

    // Primært ifm migrering
    public void lagreEgenskapUtenHistorikk(long fagsakId, EgenskapVerdi verdi) {
        Objects.requireNonNull(verdi);
        var eksisterende = finnEgenskap(fagsakId, verdi.getNøkkel());
        if (eksisterende.filter(e -> e.harSammeVerdi(verdi)).isPresent()) {
            return;
        }
        var lagres = eksisterende.orElseGet(() -> new FagsakEgenskap(fagsakId, verdi));
        lagres.setEgenskapVerdi(verdi);
        lagres.setAktiv(true);
        entityManager.persist(lagres);
        entityManager.flush();
    }

}
