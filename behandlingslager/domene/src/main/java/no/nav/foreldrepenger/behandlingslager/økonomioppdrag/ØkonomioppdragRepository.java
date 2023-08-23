package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;

@ApplicationScoped
public class ØkonomioppdragRepository {

    private static final String SAKSNR = "saksnr";

    private EntityManager entityManager;

    ØkonomioppdragRepository() {
        // for CDI proxy
    }

    @Inject
    public ØkonomioppdragRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Oppdragskontroll hentOppdragskontroll(long oppdragskontrollId) {
        var query = entityManager.createQuery(
            "from Oppdragskontroll where id=:oppdragskontrollId", Oppdragskontroll.class);
        query.setParameter("oppdragskontrollId", oppdragskontrollId);
        return hentEksaktResultat(query);
    }

    public List<Oppdrag110> hentOppdrag110ForPeriodeOgFagområde(LocalDate fomDato, LocalDate tomDato, KodeFagområde fagområde) {
        Objects.requireNonNull(fomDato, "fomDato");
        Objects.requireNonNull(tomDato, "tomDato");
        Objects.requireNonNull(fagområde, "fagområde");
        if (fomDato.isAfter(tomDato)) {
            throw new IllegalArgumentException("Datointervall for økonomioppdrag må inneholde minst 1 dag");
        }

        return entityManager
            .createQuery("""
                select o110 from Oppdrag110 as o110
                where o110.opprettetTidspunkt >= :fomTidspunkt
                    and o110.opprettetTidspunkt < :tilTidspunkt
                    and o110.kodeFagomrade = :fagomrade
                order by o110.opprettetTidspunkt, o110.nøkkelAvstemming
                    """,
                Oppdrag110.class)
            .setParameter("fomTidspunkt", fomDato.atStartOfDay())
            .setParameter("tilTidspunkt", tomDato.plusDays(1).atStartOfDay())
            .setParameter("fagomrade", fagområde)
            .getResultList();
    }

    public Oppdrag110 hentOppdragUtenKvittering(long fagsystemId, long behandlingId) {
        var typedQuery = entityManager
            .createQuery("""
                    from Oppdrag110 as o110 left join OppdragKvittering kvitto on o110.id = kvitto.oppdrag110.id
                    where o110.oppdragskontroll.behandlingId = :behandlingId
                    and o110.fagsystemId = :fagsystemId
                    and kvitto is null
                    """, Oppdrag110.class)
            .setParameter("fagsystemId", fagsystemId)
            .setParameter("behandlingId", behandlingId)
            ;
        return hentEksaktResultat(typedQuery);
    }

    public Optional<Oppdragskontroll> finnOppdragForBehandling(long behandlingId) {
        var resultList = entityManager.createQuery(
            "from Oppdragskontroll where behandlingId = :behandlingId", Oppdragskontroll.class)
            .setParameter("behandlingId", behandlingId)
            .getResultList();

        if (resultList.size() > 1) {
            throw new IllegalStateException("Utviklerfeil: Finnes mer enn en Oppdragskontroll for behandling " + behandlingId);
        }
        return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.get(0));
    }


    public List<Oppdragskontroll> finnAlleOppdragForSak(Saksnummer saksnr) {
        return entityManager.createQuery(
            "from Oppdragskontroll where saksnummer = :saksnr", Oppdragskontroll.class)
            .setParameter(SAKSNR, saksnr)
            .getResultList();
    }


    public long lagre(Oppdragskontroll oppdragskontroll) {
        entityManager.persist(oppdragskontroll);
        oppdragskontroll.getOppdrag110Liste().forEach(this::lagre);
        entityManager.flush();
        return oppdragskontroll.getId();
    }

    private void lagre(Oppdrag110 oppdrag110) {
        entityManager.persist(oppdrag110);
        oppdrag110.getOmpostering116().ifPresent(this::lagre);
        oppdrag110.getOppdragslinje150Liste().forEach(this::lagre);
    }

    private void lagre(Ompostering116 ompostering116) {
        if (ompostering116 != null) {
            entityManager.persist(ompostering116);
        }
    }

    private void lagre(Oppdragslinje150 oppdragslinje150) {
        entityManager.persist(oppdragslinje150);
        lagre(oppdragslinje150.getRefusjonsinfo156());
    }

    private void lagre(Refusjonsinfo156 refusjonsinfo156) {
        if (null != refusjonsinfo156) {
            entityManager.persist(refusjonsinfo156);
        }
    }

    public void lagre(OppdragKvittering oppdragKvittering) {
        entityManager.persist(oppdragKvittering);
        entityManager.flush();
    }
}
