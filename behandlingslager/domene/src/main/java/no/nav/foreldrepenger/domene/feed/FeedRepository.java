package no.nav.foreldrepenger.domene.feed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.DiscriminatorValue;
import javax.persistence.EntityManager;

import org.hibernate.jpa.QueryHints;

@ApplicationScoped
public class FeedRepository {

    private EntityManager entityManager;

    FeedRepository() {
        // CDI
    }

    @Inject
    public FeedRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }


    public<V extends UtgåendeHendelse> Long lagre(V utgåendeHendelse) {
        Objects.requireNonNull(utgåendeHendelse);
        if (utgåendeHendelse.getSekvensnummer() == 0) {
            utgåendeHendelse.setSekvensnummer(hentNesteSekvensnummer(utgåendeHendelse.getClass()));
        }
        entityManager.persist(utgåendeHendelse);
        entityManager.flush();
        return utgåendeHendelse.getId();

    }

    public <V extends UtgåendeHendelse> boolean harHendelseMedKildeId(String kildeId) {
        Objects.requireNonNull(kildeId);

        var query = entityManager
            .createNativeQuery("SELECT count(1) FROM UTGAAENDE_HENDELSE where kilde_id = :kildeId")
            .setParameter("kildeId", kildeId)
            .setHint(QueryHints.HINT_READONLY, "true");

        var antall = (BigDecimal) query.getSingleResult();
        return antall.compareTo(BigDecimal.ZERO) > 0;
    }


    public Optional<UtgåendeHendelse> hentUtgåendeHendelse(Long hendelseId) {
        return Optional.ofNullable(entityManager.find(UtgåendeHendelse.class, hendelseId));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <V extends UtgåendeHendelse> List<V> hentUtgåendeHendelser(Class<V> cls, HendelseCriteria hendelseCriteria) {
        var discVal = cls.getDeclaredAnnotation(DiscriminatorValue.class);
        Objects.requireNonNull(discVal, "Mangler @DiscriminatorValue i klasse:" + cls); //$NON-NLS-1$
        var outputFeedKode  = discVal.value();

        var results = createScrollableResult(outputFeedKode, hendelseCriteria);
        List<V> hendelser = new ArrayList<>();
        for (var object : results) {
            final var resultObjects = (Object[]) object;

            if (resultObjects.length > 0) {
                var hendelse = hentUtgåendeHendelse(((BigDecimal) resultObjects[0]).longValue()); // NOSONAR
                hendelse.ifPresent(h -> hendelser .add((V) h));
            }
        }

        return hendelser;
    }


    private String createNativeSql(String type, String aktørId) {
        var sb = new StringBuilder();
        sb.append("SELECT * FROM (");
        sb.append(" SELECT uh.id, ROW_NUMBER() OVER (ORDER BY uh.SEKVENSNUMMER ASC) AS R");
        sb.append(" FROM UTGAAENDE_HENDELSE uh");
        sb.append(" WHERE uh.sekvensnummer > :sistLestSekvensnummer");
        sb.append(" AND output_feed_kode = :outputFeedKode");

        if(type != null) {
            sb.append(" AND uh.type = :type");
        }

        if(aktørId != null) {
            sb.append(" AND uh.aktoer_id = :aktørId");
        }

        sb.append(" ORDER BY uh.SEKVENSNUMMER ASC");

        sb.append(") WHERE R BETWEEN 1 AND :maxAntall");

        return sb.toString();
    }

    @SuppressWarnings("rawtypes")
    private List createScrollableResult(String outputFeedKode, HendelseCriteria hendelseCriteria) {
        var q = entityManager.createNativeQuery(createNativeSql(hendelseCriteria.getType(), hendelseCriteria.getAktørId()))
                .setParameter("outputFeedKode", outputFeedKode)
                .setParameter("maxAntall", hendelseCriteria.getMaxAntall())
                .setParameter("sistLestSekvensnummer", hendelseCriteria.getSisteLestSekvensId());

        if (hendelseCriteria.getType() != null) {
            q.setParameter("type", hendelseCriteria.getType());
        }
        if (hendelseCriteria.getAktørId() != null) {
            q.setParameter("aktørId", hendelseCriteria.getAktørId());
        }


        return q.getResultList();
    }

    public <V extends UtgåendeHendelse> long hentNesteSekvensnummer(Class<V> cls) {
        var sekVal = cls.getDeclaredAnnotation(SekvensnummerNavn.class);
        Objects.requireNonNull(sekVal, "Mangler @SekvensnummerGeneratorNavn i klasse:" + cls); //$NON-NLS-1$
        var sql  = "select " + sekVal.value() + ".nextval as num from dual";

        var query = entityManager.createNativeQuery(sql); //NOSONAR Her har vi full kontroll på sql
        var singleResult = (BigDecimal) query.getSingleResult();
        return singleResult.longValue();
    }
}
