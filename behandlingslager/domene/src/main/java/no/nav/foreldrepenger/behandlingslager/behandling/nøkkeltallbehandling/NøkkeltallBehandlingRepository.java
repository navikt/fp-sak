package no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class NøkkeltallBehandlingRepository {

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

    private static final String QUERY_BESLUTTERRETUR = """
        with cte as (
            select ttv.id as ttv_id,
            trunc(aarsak.opprettet_tid) as dato,
            aarsak.aarsak_type as aarsak_type,
            count(distinct(ttv.id)) over(partition by trunc(ttv.opprettet_tid)) as antall_totrinnsretur
            from vurder_aarsak_ttvurdering aarsak
            join totrinnsvurdering ttv on ttv.id = aarsak.totrinnsvurdering_id
            join behandling b on b.id = ttv.behandling_id
            where aarsak.opprettet_tid >= :datotidGrense
                and b.behandlende_enhet = :behandlendeEnhet
        )
        select cte.dato, cte.antall_totrinnsretur, cte.aarsak_type, count(1) as antall
        from cte
        group by cte.dato, cte.antall_totrinnsretur, cte.aarsak_type
        """;

    private EntityManager entityManager;

    protected NøkkeltallBehandlingRepository() {
        // for CDI proxy
    }

    @Inject
    public NøkkeltallBehandlingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public List<NøkkeltallBehandlingVentestatus> hentNøkkeltallBehandlingVentestatus() {
        var query = entityManager.createNativeQuery(QUERY_NØKKELTALL)
            .setParameter("åpenAksjonspunktStatus", AksjonspunktStatus.OPPRETTET.getKode())
            .setParameter("avsluttetBehandlingStatus", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("fpYtelseType", FagsakYtelseType.YtelseType.FP.name());
        @SuppressWarnings("unchecked")
        Stream<Object[]> result = query.getResultStream();
        return result.map(NøkkeltallBehandlingRepository::mapTilBehandlingVentestatus).toList();
    }

    public List<BeslutterRetur> hentNøkkeltallBeslutterRetur(String enhetsnummer) {
        var datoFom = LocalDate.now().minusWeeks(4).atStartOfDay().with(DayOfWeek.MONDAY);
        var nativeQuery = entityManager.createNativeQuery(QUERY_BESLUTTERRETUR)
            .setParameter("behandlendeEnhet", enhetsnummer)
            .setParameter("datotidGrense", datoFom);
        @SuppressWarnings("unchecked")
        Stream<Object[]> resultat = nativeQuery.getResultStream();
        return resultat.map(NøkkeltallBehandlingRepository::mapTilBeslutterReturData).toList();
    }

    private static NøkkeltallBehandlingVentestatus mapTilBehandlingVentestatus(Object[] record) {
        var behandlendeEnhet = (String) record[0];
        var behandlingType = BehandlingType.fraKode((String) record[1]);
        var status = BehandlingVenteStatus.valueOf((String) record[2]);
        var førsteUttaksMåned = localDate(record[3]);
        var antall = (BigDecimal) record[4];
        return new NøkkeltallBehandlingVentestatus(behandlendeEnhet, behandlingType,
            status, førsteUttaksMåned, antall.intValue());
    }

    private static BeslutterRetur mapTilBeslutterReturData(Object[] record) {
        var dato = ((Timestamp) record[0]).toLocalDateTime().toLocalDate();
        var totalAntall = ((BigDecimal) record[1]).longValue();
        var årsakKode = (String) record[2];
        var antall = ((BigDecimal) record[3]).longValue();
        return new BeslutterRetur(dato, totalAntall, VurderÅrsak.fraKode(årsakKode), antall);
    }

    private static LocalDate localDate(Object sqlTimestamp) {
        if (sqlTimestamp == null) {
            return null;
        }
        return ((Timestamp) sqlTimestamp).toLocalDateTime().toLocalDate();
    }

}
