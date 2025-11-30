package no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.TimestampConverter;

@ApplicationScoped
public class NøkkeltallBehandlingRepository {

    private EntityManager entityManager;

    @Inject
    public NøkkeltallBehandlingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    protected NøkkeltallBehandlingRepository() {
        // for CDI proxy
    }

    private static final String QUERY_FØRSTE_UTTAK_PR_MÅNED = """
        select enhet, btype, på_vent, dato, sum(antall) from
        (
            select
               behandling.enhet as enhet, behandling.behandling_type as btype,
               coalesce(ventestatus.på_vent, 'IKKE_PÅ_VENT') as på_vent,
               case when behandling.tidligste_fom < sysdate - 180 then trunc(sysdate - 180, 'MM')
                    when behandling.tidligste_fom > sysdate + 300 then trunc(sysdate + 300, 'MM')
                    else behandling.tidligste_fom end as dato,
                count(1) as antall
            from
               (
                   select b.id, B.BEHANDLENDE_ENHET as enhet, b.behandling_type,
                   (
                       select trunc(min(yfp.fom), 'MM')
                       from gr_ytelses_fordeling gyf
                       join yf_fordeling_periode yfp on yfp.fordeling_id = gyf.so_fordeling_id
                       left outer join behandling_arsak ba on ba.behandling_id = gyf.behandling_id
                       where gyf.behandling_id = b.id
                       and gyf.aktiv = 'J'
                       and (b.behandling_type = :forstegang or ba.behandling_arsak_type = :endringsoknad)
                       and yfp.kl_aarsak_type not in (:utsettelseOpphold)
                       group by gyf.so_fordeling_id
                   ) as tidligste_fom
                   from behandling b
                   join fagsak fs on fs.id = b.fagsak_id
                   where b.behandling_status != :avsluttetBehandlingStatus
                   and fs.YTELSE_TYPE = :fpYtelseType
                   and b.id not in (select behandling_id from aksjonspunkt where aksjonspunkt_def = :ventSoknad and aksjonspunkt_status = :åpenAksjonspunktStatus)
               ) behandling
               left join (
                   select a.behandling_id, 'PÅ_VENT' as på_vent
                   from aksjonspunkt a
                   where a.aksjonspunkt_status = :åpenAksjonspunktStatus
                   and a.aksjonspunkt_def >= :lavesteVentKode
               ) ventestatus on ventestatus.behandling_id = behandling.id
            group by behandling.enhet, behandling.behandling_type, coalesce(ventestatus.på_vent, 'IKKE_PÅ_VENT'), behandling.tidligste_fom
        )
        group by enhet, btype, på_vent, dato
        """;

    public List<NøkkeltallBehandlingFørsteUttak> hentNøkkeltallSøknadFørsteUttakPrMånedForeldrepenger() {
        var query = entityManager.createNativeQuery(QUERY_FØRSTE_UTTAK_PR_MÅNED)
            .setParameter("åpenAksjonspunktStatus", AksjonspunktStatus.OPPRETTET.getKode())
            .setParameter("avsluttetBehandlingStatus", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("forstegang", BehandlingType.FØRSTEGANGSSØKNAD.getKode())
            .setParameter("endringsoknad", BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER.getKode())
            .setParameter("utsettelseOpphold", List.of(OppholdÅrsak.DISKRIMINATOR, UtsettelseÅrsak.DISKRIMINATOR))
            .setParameter("ventSoknad", AksjonspunktDefinisjon.VENT_PÅ_SØKNAD.getKode())
            .setParameter("lavesteVentKode", AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT.getKode())
            .setParameter("fpYtelseType", FagsakYtelseType.YtelseType.FP.name());
        @SuppressWarnings("unchecked")
        var result = (List<Object[]>) query.getResultList();
        return result.stream().map(NøkkeltallBehandlingRepository::mapFørsteUttak).toList();
    }

    private static NøkkeltallBehandlingFørsteUttak mapFørsteUttak(Object record) {
        var queryResultat = (Object[]) record;
        var behandlendeEnhet = (String) queryResultat[0];
        var behandlingType = BehandlingType.fraKode((String) queryResultat[1]);
        var status = BehandlingVenteStatus.valueOf((String) queryResultat[2]);
        var førsteUttaksMåned = localDate(queryResultat[3]);
        var antall = (BigDecimal) queryResultat[4];
        return new NøkkeltallBehandlingFørsteUttak(behandlendeEnhet, behandlingType,
            status, førsteUttaksMåned, antall.intValue());
    }

    private static final String QUERY_FRIST_UTLØPER_DAG = """
        select enhet, yt, frist, to_char(frist, 'IYYY-IW'), count(1) as ant from (
            select enhet, yt,
                   case when fristi <= sysdate then trunc(sysdate + 1)
                        when fristi > sysdate + 185 then trunc(sysdate+185)
                        else fristi end as frist
            from (
                select b.behandlende_enhet as enhet, f.ytelse_type as yt, trunc(nvl(ap.frist_tid, nvl(ap.endret_tid, ap.opprettet_tid) + 28))  as fristi
                from aksjonspunkt ap join behandling b on ap.behandling_id = b.id join fagsak f on b.fagsak_id = f.id
                where b.behandling_status != :avsluttetBehandlingStatus and ap.aksjonspunkt_status=:åpenAksjonspunktStatus
                    and b.behandling_type = :førstegang and ap.aksjonspunkt_def >= :lavesteVentKode and ap.aksjonspunkt_def not in (:ventIgnorer)
            )
        )
        group by enhet, yt, frist, to_char(frist, 'IYYY-IW')
        """;

    public List<NøkkeltallBehandlingVentefristUtløper> hentNøkkeltallVentefristUtløper() {
        var query = entityManager.createNativeQuery(QUERY_FRIST_UTLØPER_DAG)
            .setParameter("åpenAksjonspunktStatus", AksjonspunktStatus.OPPRETTET.getKode())
            .setParameter("avsluttetBehandlingStatus", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("førstegang", BehandlingType.FØRSTEGANGSSØKNAD.getKode())
            .setParameter("lavesteVentKode", AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT.getKode())
            .setParameter("ventIgnorer", List.of(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD.getKode(), AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING.getKode()))
            ;
        @SuppressWarnings("unchecked")
        var result = (List<Object[]>) query.getResultList();
        return result.stream().map(NøkkeltallBehandlingRepository::mapFrist).toList();
    }

    private static NøkkeltallBehandlingVentefristUtløper mapFrist(Object record) {
        var queryResultat = (Object[]) record;
        var behandlendeEnhet = (String) queryResultat[0];
        var ytelseType = FagsakYtelseType.fraKode((String) queryResultat[1]);
        var fristUtløper = localDate(queryResultat[2]);
        var fristUke = (String) queryResultat[3];
        var antall = (BigDecimal) queryResultat[4];
        return new NøkkeltallBehandlingVentefristUtløper(behandlendeEnhet, ytelseType, fristUtløper, fristUke, antall.longValue());
    }

    private static final String QUERY_FRIST_UTLØPER_UKE = """
        select enhet, yt, frist, to_char(frist, 'IYYY-IW'), count(1) as ant from (
            select enhet, yt,
                   case when fristi <= sysdate then trunc(sysdate + 1, 'IW')
                        when fristi > sysdate + 245 then trunc(sysdate+245, 'IW')
                        else trunc(fristi, 'IW') end as frist
            from (
                select b.behandlende_enhet as enhet, f.ytelse_type as yt, trunc(nvl(ap.frist_tid, nvl(ap.endret_tid, ap.opprettet_tid) + 28))  as fristi
                from aksjonspunkt ap join behandling b on ap.behandling_id = b.id join fagsak f on b.fagsak_id = f.id
                where b.behandling_status != :avsluttetBehandlingStatus and ap.aksjonspunkt_status=:åpenAksjonspunktStatus
                    and b.behandling_type = :førstegang and ap.aksjonspunkt_def >= :lavesteVentKode and ap.aksjonspunkt_def not in (:ventIgnorer)
            )
        )
        group by enhet, yt, frist, to_char(frist, 'IYYY-IW')
        """;

    public List<NøkkeltallBehandlingVentefristUtløper> hentNøkkeltallVentefristUtløperUke() {
        var query = entityManager.createNativeQuery(QUERY_FRIST_UTLØPER_UKE)
            .setParameter("åpenAksjonspunktStatus", AksjonspunktStatus.OPPRETTET.getKode())
            .setParameter("avsluttetBehandlingStatus", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("førstegang", BehandlingType.FØRSTEGANGSSØKNAD.getKode())
            .setParameter("lavesteVentKode", AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT.getKode())
            .setParameter("ventIgnorer", List.of(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD.getKode(), AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING.getKode()))
            ;
        @SuppressWarnings("unchecked")
        var result = (List<Object[]>) query.getResultList();
        return result.stream().map(NøkkeltallBehandlingRepository::mapFrist).toList();
    }

    private static LocalDate localDate(Object sqlTimestamp) {
        return TimestampConverter.toLocalDate(sqlTimestamp);
    }

}
