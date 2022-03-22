package no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class NøkkeltallBehandlingRepository {

    private EntityManager entityManager;

    @Inject
    public NøkkeltallBehandlingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    protected NøkkeltallBehandlingRepository() {
        // for CDI proxy
    }

    private static final String QUERY_NØKKELTALL = """
        select
           behandling.enhet, behandling.behandling_type,
           coalesce(ventestatus.på_vent, 'IKKE_PÅ_VENT') as på_vent
           ,behandling.tidligste_fom, count(1)
        from
           (
               select b.id, B.BEHANDLENDE_ENHET as enhet, b.behandling_type,
               (
                   select trunc(min(yfp.fom), 'MM')
                   from gr_ytelses_fordeling gyf
                   join yf_fordeling_periode yfp on yfp.fordeling_id = gyf.so_fordeling_id
                   where gyf.behandling_id = b.id
                   and gyf.aktiv = 'J'
                   group by gyf.so_fordeling_id
               ) as tidligste_fom
               from behandling b
               join fagsak fs on fs.id = b.fagsak_id
               where b.behandling_status != :avsluttetBehandlingStatus
               and fs.YTELSE_TYPE = :fpYtelseType
               and b.opprettet_tid > to_timestamp('31.05.2020 23:59:59','dd.mm.yyyy hh24:mi:ss')
           ) behandling
           left join (
               select a.behandling_id, 'PÅ_VENT' as på_vent
               from aksjonspunkt a
               where a.aksjonspunkt_status = :åpenAksjonspunktStatus
               and substr(a.aksjonspunkt_def, 1, 1) not in ('5', '6')
           ) ventestatus on ventestatus.behandling_id = behandling.id
        group by behandling.enhet, behandling.behandling_type, coalesce(ventestatus.på_vent, 'IKKE_PÅ_VENT'),
           behandling.tidligste_fom
        """;

    public List<NøkkeltallBehandlingVentestatus> hentNøkkeltallBehandlingVentestatus() {
        var query = entityManager.createNativeQuery(QUERY_NØKKELTALL)
            .setParameter("åpenAksjonspunktStatus", AksjonspunktStatus.OPPRETTET.getKode())
            .setParameter("avsluttetBehandlingStatus", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("fpYtelseType", FagsakYtelseType.YtelseType.FP.name());
        @SuppressWarnings("unchecked")
        var result = (List<Object[]>) query.getResultList();
        return result.stream().map(NøkkeltallBehandlingRepository::map).collect(Collectors.toList());
    }

    private static NøkkeltallBehandlingVentestatus map(Object record) {
        var queryResultat = (Object[]) record;
        var behandlendeEnhet = (String) queryResultat[0];
        var behandlingType = BehandlingType.fraKode((String) queryResultat[1]);
        var status = BehandlingVenteStatus.valueOf((String) queryResultat[2]);
        var førsteUttaksMåned = localDate(queryResultat[3]);
        var antall = (BigDecimal) queryResultat[4];
        return new NøkkeltallBehandlingVentestatus(behandlendeEnhet, behandlingType,
            status, førsteUttaksMåned, antall.intValue());
    }

    private static LocalDate localDate(Object sqlTimestamp) {
        if (sqlTimestamp == null) {
            return null;
        }
        return ((Timestamp) sqlTimestamp).toLocalDateTime().toLocalDate();
    }

}
