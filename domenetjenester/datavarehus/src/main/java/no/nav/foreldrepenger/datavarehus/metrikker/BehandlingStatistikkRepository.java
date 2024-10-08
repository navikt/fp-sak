package no.nav.foreldrepenger.datavarehus.metrikker;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class BehandlingStatistikkRepository {

    private EntityManager entityManager;

    BehandlingStatistikkRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingStatistikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<BehandlingPerYtelseTypeOgÅrsakStatistikk> hentAntallBehandlinger() {
        var query = entityManager.createQuery("""
            select f.ytelseType, b.behandlingType, ba.behandlingÅrsakType, count(1) as antall from Fagsak f
            join Behandling b on b.fagsak.id = f.id
            left outer join BehandlingÅrsak ba on ba.behandling.id = b.id
            group by f.ytelseType, b.behandlingType, ba.behandlingÅrsakType
            """, BehandlingPerYtelseTypeOgÅrsakStatistikk.class)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }


    public record BehandlingPerYtelseTypeOgÅrsakStatistikk(FagsakYtelseType ytelseType,
                                                           BehandlingType behandlingType,
                                                           BehandlingÅrsakType behandlingArsakType,
                                                           Long antall) {
    }
}
