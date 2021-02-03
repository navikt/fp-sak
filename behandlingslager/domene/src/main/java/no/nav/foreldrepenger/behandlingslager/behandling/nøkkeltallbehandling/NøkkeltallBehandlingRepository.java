package no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
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

    public List<NøkkeltallBehandlingVentestatus> hentNøkkeltallBehandlingVentestatus() {
        Query query = entityManager.createNativeQuery(
            "with cte as ( " +
                "select " +
                "b.behandlende_enhet, " +
                "b.behandling_type,  " +
                "case when exists (select 1 from aksjonspunkt a " +
                "where a.aksjonspunkt_status in :åpenAksjonspunktStatus " +
                "AND SUBSTR(A.AKSJONSPUNKT_DEF,1,1) NOT IN ('5','6') " +
                "and a.behandling_id = b.id) then 'PÅ_VENT' else 'IKKE_PÅ_VENT' end as på_vent, " +
                "( " +
                "    select trunc(min(yfp.fom), 'MM') " +
                "    from gr_ytelses_fordeling gyf " +
                "    join yf_fordeling_periode yfp on yfp.fordeling_id = gyf.so_fordeling_id " +
                "    where gyf.behandling_id = b.id " +
                "    and gyf.aktiv = 'J' " +
                "    group by gyf.so_fordeling_id " +
                ") as tidligste_fom " +
                "from behandling b " +
                "join fagsak fs on fs.id = b.fagsak_id " +
                "where B.BEHANDLING_STATUS != :avsluttetBehandlingStatus and YTELSE_TYPE = :fpYtelseType) " +
                "select behandlende_enhet, behandling_type, på_vent, tidligste_fom, count(1) " +
                "from cte " +
                "group by behandlende_enhet, behandling_type, på_vent, tidligste_fom")
            .setParameter("åpenAksjonspunktStatus", åpneAksjonspunktStatus())
            .setParameter("avsluttetBehandlingStatus", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("fpYtelseType", FagsakYtelseType.YtelseType.FP.name());
        @SuppressWarnings("unchecked")
        var result = (List<Object[]>) query.getResultList();
        return result.stream().map(NøkkeltallBehandlingRepository::map).collect(Collectors.toList());
    }

    private static List<String> åpneAksjonspunktStatus() {
        return AksjonspunktStatus.getÅpneAksjonspunktStatuser().stream()
            .map(AksjonspunktStatus::getKode)
            .collect(Collectors.toList());
    }

    private static NøkkeltallBehandlingVentestatus map(Object record) {
        Object[] queryResultat = (Object[]) record;
        var behandlendeEnhet = (String) queryResultat[0];
        var behandlingType = BehandlingType.fraKode(queryResultat[1]);
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
