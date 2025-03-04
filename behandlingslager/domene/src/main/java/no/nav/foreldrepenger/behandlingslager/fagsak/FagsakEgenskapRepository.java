package no.nav.foreldrepenger.behandlingslager.fagsak;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

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
        return finnEgenskaper(fagsakId, nøkkel).stream().findFirst().flatMap(fe -> fe.getEgenskapVerdiHvisFinnes(enumCls));
    }

    public Optional<UtlandDokumentasjonStatus> finnUtlandDokumentasjonStatus(long fagsakId) {
        return finnEgenskaper(fagsakId, EgenskapNøkkel.UTLAND_DOKUMENTASJON).stream().findFirst()
            .map(FagsakEgenskap::getEgenskapVerdi)
            .map(UtlandDokumentasjonStatus::valueOf);
    }

    public void lagreUtlandDokumentasjonStatus(long fagsakId, UtlandDokumentasjonStatus verdi) {
        if (finnEgenskapMedGittVerdi(fagsakId, verdi).isEmpty()) {
            var lagres = finnEgenskaper(fagsakId, verdi.getNøkkel()).stream().findFirst().orElseGet(() -> new FagsakEgenskap(fagsakId, verdi));
            lagres.setEgenskapVerdi(verdi);
            lagres.setAktiv(true);
            entityManager.persist(lagres);
            entityManager.flush();
        }
    }

    public Collection<FagsakMarkering> finnFagsakMarkeringer(long fagsakId) {
        return finnEgenskaper(fagsakId, EgenskapNøkkel.FAGSAK_MARKERING).stream()
            .map(FagsakEgenskap::getEgenskapVerdi)
            .filter(Objects::nonNull)
            .map(FagsakMarkering::valueOf)
            .collect(Collectors.toSet());
    }

    public boolean harFagsakMarkering(long fagsakId, FagsakMarkering markering) {
        return finnEgenskapMedGittVerdi(fagsakId, EgenskapNøkkel.FAGSAK_MARKERING, markering.name()).isPresent();
    }

    private Optional<FagsakEgenskap> finnEgenskapMedGittVerdi(long fagsakId, EgenskapVerdi verdi) {
        return finnEgenskapMedGittVerdi(fagsakId, verdi.getNøkkel(), verdi.getVerdi());
    }

    private Optional<FagsakEgenskap> finnEgenskapMedGittVerdi(long fagsakId, EgenskapNøkkel nøkkel, String egenskap) {
        var query = entityManager.createQuery(
                "from FagsakEgenskap where fagsakId = :fagsak AND egenskapNøkkel = :nøkkel AND egenskapVerdi = :egenskap AND aktiv = true",
                FagsakEgenskap.class)
            .setParameter(FAGSAK_QP, fagsakId)
            .setParameter("nøkkel", nøkkel)
            .setParameter("egenskap", egenskap);
        return hentUniktResultat(query);
    }

    private List<FagsakEgenskap> finnEgenskaper(long fagsakId, EgenskapNøkkel nøkkel) {
        var query = entityManager.createQuery(
                "from FagsakEgenskap where fagsakId = :fagsak AND egenskapNøkkel = :nøkkel AND aktiv = true",
                FagsakEgenskap.class)
            .setParameter(FAGSAK_QP, fagsakId)
            .setParameter("nøkkel", nøkkel);
        return query.getResultList();
    }

    public void lagreAlleFagsakMarkeringer(long fagsakId, Collection<FagsakMarkering> markeringer) {
        var eksisterende = finnFagsakMarkeringer(fagsakId);
        markeringer.stream().filter(fm -> !eksisterende.contains(fm)).forEach(fm -> lagreFagsakMarkering(fagsakId, fm));
        eksisterende.stream().filter(fm -> !markeringer.contains(fm)).forEach(fm -> fjernFagsakMarkering(fagsakId, fm));
    }

    public void leggTilFagsakMarkering(long fagsakId, FagsakMarkering markering) {
        lagreFagsakMarkering(fagsakId, markering);
    }

    private void lagreFagsakMarkering(long fagsakId, FagsakMarkering verdi) {
        Objects.requireNonNull(verdi);
        var eksisterende = finnEgenskapMedGittVerdi(fagsakId, verdi);
        if (eksisterende.isEmpty()) {
            lagreFagsakEgenskap(fagsakId, verdi);
        }
    }

    public void fjernFagsakMarkering(long fagsakId, FagsakMarkering verdi) {
        Objects.requireNonNull(verdi);
        var eksisterende = finnEgenskapMedGittVerdi(fagsakId, verdi);
        eksisterende.ifPresent(this::fjernFagsakEgenskap);
    }

    private void lagreFagsakEgenskap(long fagsakId, FagsakMarkering verdi) {
        var lagres = new FagsakEgenskap(fagsakId, verdi);
        entityManager.persist(lagres);
        entityManager.flush();
    }

    private void fjernFagsakEgenskap(FagsakEgenskap fagsakEgenskap) {
        fagsakEgenskap.setAktiv(false);
        entityManager.persist(fagsakEgenskap);
        entityManager.flush();
    }

}
