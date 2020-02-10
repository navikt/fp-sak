package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class KodeverkSynkroniseringRepository {

    private static final String KODEVERK_EIER = "Kodeverkforvaltning";
    private EntityManager entityManager;

    KodeverkSynkroniseringRepository() {
        // for CDI proxy
    }

    @Inject
    public KodeverkSynkroniseringRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public List<Kodeverk> hentKodeverkForSynkronisering() {
        TypedQuery<Kodeverk> query = entityManager.createQuery(
            "from Kodeverk k where (k.synkNyeKoderFraKodeverEier = 'J' or k.synkEksisterendeKoderFraKodeverkEier = 'J') and k.kodeverkEier=:kodeverkEier",
            Kodeverk.class)
            .setParameter("kodeverkEier", KODEVERK_EIER);
        query.setHint(QueryHints.HINT_READONLY, "true");
        return query.getResultList();
    }

    public List<DefaultKodeverdi> hentKodeliste(String kodeverkNavn) {

        // bruker ikke her entiteter, da kan vi hente opp hva som helst av kodeverk
        @SuppressWarnings("unchecked")
        TypedQuery<Tuple> query = (TypedQuery<Tuple>) entityManager.createNativeQuery(
            "select k.kode, "
                + "k.kodeverk, "
                + "k.offisiell_kode, "
                + "(select k18n.navn from kodeliste_navn_i18n k18n where k18n.kl_kode=k.kode and k18n.kl_kodeverk=k.kodeverk and k18n.sprak=:sprak) as navn, "
                + "k.gyldig_fom, "
                + "k.gyldig_tom "
                + "from Kodeliste k where k.kodeverk=:kodeverkNavn",
            Tuple.class)
            .setParameter("kodeverkNavn", kodeverkNavn)
            .setParameter("sprak", "NB");

        return query
            .getResultStream().map((Tuple t) -> {
                return new DefaultKodeverdi(
                    t.get("kodeverk", String.class),
                    t.get("kode", String.class),
                    t.get("offisiell_kode", String.class),
                    t.get("navn", String.class),
                    t.get("gyldig_fom", java.sql.Timestamp.class).toLocalDateTime().toLocalDate(),
                    t.get("gyldig_tom", java.sql.Timestamp.class).toLocalDateTime().toLocalDate());
            })
            .collect(Collectors.toList());
    }

    public Map<String, String> hentKodeverkEierNavnMap() {
        TypedQuery<Kodeverk> query = entityManager.createQuery(
            "from Kodeverk k where k.kodeverkEierNavn is not null and k.kodeverkEier=:kodeverkEier",
            Kodeverk.class)
            .setParameter("kodeverkEier", KODEVERK_EIER);
        query.setHint(QueryHints.HINT_READONLY, "true");
        return query.getResultList().stream()
            .collect(Collectors.toMap(Kodeverk::getKodeverkEierNavn, Kodeverk::getKode));
    }

    public void opprettNyKode(String kodeverk, String kode, String offisiellKode, String navn, LocalDate fom, LocalDate tom) {
        Query query = entityManager.createNativeQuery(
            "INSERT INTO KODELISTE (kodeverk, kode, offisiell_kode, gyldig_fom, gyldig_tom) " +
                " VALUES (?, ?, ?, ?, ?)");
        query.setParameter(1, kodeverk);
        query.setParameter(2, kode);
        query.setParameter(3, offisiellKode);
        query.setParameter(4, Date.valueOf(fom));
        query.setParameter(5, Date.valueOf(tom));
        query.executeUpdate();
        Query query2 = entityManager.createNativeQuery(
            "INSERT INTO KODELISTE_NAVN_I18N (id, kl_kodeverk, kl_kode, sprak, navn) " +
                " VALUES (seq_kodeliste_navn_i18n.nextval, ?, ?, ?, ?)");
        query2.setParameter(1, kodeverk);
        query2.setParameter(2, kode);
        query2.setParameter(3, "NB");
        query2.setParameter(4, navn);
        query2.executeUpdate();
    }

    public void oppdaterEksisterendeKodeverk(String kodeverk, String versjon, String uri) {
        Query query = entityManager.createNativeQuery(
            "UPDATE KODEVERK SET  kodeverk_eier_ver=?, kodeverk_eier_ref=? " +
                " WHERE kode=? ");
        query.setParameter(1, versjon);
        query.setParameter(2, uri);
        query.setParameter(3, kodeverk);
        query.executeUpdate();
    }

    public void oppdaterEksisterendeKode(String kodeverk, String kode, String offisiellKode, String navn, LocalDate fom, LocalDate tom) {
        Query query = entityManager.createNativeQuery(
            "UPDATE KODELISTE SET  offisiell_kode=?, gyldig_fom=?, gyldig_tom=? " +
                " WHERE kodeverk=? AND kode=?");
        query.setParameter(1, offisiellKode);
        query.setParameter(2, Date.valueOf(fom));
        query.setParameter(3, Date.valueOf(tom));
        query.setParameter(4, kodeverk);
        query.setParameter(5, kode);
        query.executeUpdate();
        Query query2 = entityManager.createNativeQuery(
            "UPDATE KODELISTE_NAVN_I18N SET  navn=? " +
                " WHERE kl_kodeverk=? AND kl_kode=? AND sprak=?");
        query2.setParameter(1, navn);
        query2.setParameter(2, kodeverk);
        query2.setParameter(3, kode);
        query2.setParameter(4, "NB");
        query2.executeUpdate();
    }

    public void lagre() {
        entityManager.flush();
    }
}
