package no.nav.foreldrepenger.datavarehus.metrikker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;

import org.hibernate.jpa.HibernateHints;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingLong;

@ApplicationScoped
public class VedtakStatistikkRepository {
    private EntityManager entityManager;

    VedtakStatistikkRepository() {
        // for CDI proxy
    }

    @Inject
    public VedtakStatistikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<VedtakStatistikk> hent() {
        var query = entityManager.createQuery("""
            select bv.vedtakResultatType, count(1) as antall
            from BehandlingVedtak bv
            group by bv.vedtakResultatType
            """, VedtakResultatQR.class)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        var behandlingStatistikkEntitet = query.getResultList();
        return map(behandlingStatistikkEntitet);
    }

    record VedtakResultatQR(VedtakResultatType vedtakResultatType, Long antall) { }
    public record VedtakStatistikk(VedtakResultatType vedtakResultatType, Long antall) { }


    static List<VedtakStatistikk> map(List<VedtakResultatQR> b) {
        return b.stream()
            .map(VedtakStatistikkRepository::tilBehandlingStatistikk)
            .collect(groupingBy(bs -> Optional.ofNullable(bs.vedtakResultatType()).orElse(VedtakResultatType.UDEFINERT),
                summarizingLong(bs -> Optional.ofNullable(bs.antall()).orElse(0L))))
            .entrySet()
            .stream()
            .map(vedtakResultatType -> new VedtakStatistikk(vedtakResultatType.getKey(), vedtakResultatType.getValue().getSum()))
            .toList();
    }

    private static VedtakStatistikk tilBehandlingStatistikk(VedtakResultatQR entitet) {
        return new VedtakStatistikk(entitet.vedtakResultatType, entitet.antall());
    }

}
