package no.nav.foreldrepenger.dokumentbestiller.infobrev;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import org.hibernate.jpa.HibernateHints;

import java.util.List;

/**
 * Spesialmetoder for å hente opp saker og personer som er kandidat for å sende
 * informasjonsbrev om feil praksis utsettelse og gradering
 */

@ApplicationScoped
public class FeilPraksisUtsettelseRepository {

    private EntityManager entityManager;

    FeilPraksisUtsettelseRepository() {
        // for CDI proxy
    }

    @Inject
    public FeilPraksisUtsettelseRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /*
     *  MOR: Avslag utsettelse perioder etter 1/10-21
     */
    private static final String QUERY_MERKING_MOR = """
        select distinct f.id from fagsak f
        join behandling b on fagsak_id = f.id
        join behandling_resultat br  on b.id = br.behandling_id
        join behandling_vedtak bv on bv.behandling_resultat_id = br.id
        join (select beh.fagsak_id fsmax, max(bvsq.opprettet_tid) maxbr from behandling beh
            join behandling_resultat brsq on brsq.behandling_id=beh.id
            join behandling_vedtak bvsq on bvsq.behandling_resultat_id = brsq.id
            where beh.behandling_type in ('BT-002','BT-004') and beh.behandling_status in ('AVSLU', 'IVED')
            group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.opprettet_tid = maxbr)
        join uttak_resultat ur on (ur.behandling_resultat_id = br.id and ur.aktiv='J')
        join uttak_resultat_periode peri on uttak_resultat_perioder_id = nvl(ur.overstyrt_perioder_id, ur.opprinnelig_perioder_id)
        join uttak_resultat_periode_akt peria on peria.uttak_resultat_periode_id = peri.id
        where 1=1
            and b.behandling_type in ('BT-002','BT-004') and b.behandling_status in ('AVSLU', 'IVED')
            and peri.tom >= '01.10.2021'
            and uttak_utsettelse_type <> '-'
            and (utbetalingsprosent is null or utbetalingsprosent = 0)
            and trekkdager_desimaler > 0
            and trekkonto <> 'FORELDREPENGER_FØR_FØDSEL'
            and periode_resultat_aarsak not in ( '4030', '4031', '4071', '4072', '4077', '4103', '4104', '4110', '4111', '4112', '4115', '4116', '4117' )
            and bruker_rolle = 'MORA'
            and ytelse_type='FP'
            and til_infotrygd = 'N'
            and (bv.vedtak_resultat_type = 'INNVILGET' or (bv.vedtak_resultat_type = 'OPPHØR'
            and exists (select * from  br_resultat_behandling ty
                      join br_periode brp on brp.BEREGNINGSRESULTAT_FP_ID = nvl(UTBET_BEREGNINGSRESULTAT_FP_ID,BG_BEREGNINGSRESULTAT_FP_ID)
                      join br_andel ba on ba.br_periode_id = brp.id
                      where ty.behandling_id=b.id and ty.aktiv='J' and dagsats > 0)))
            and f.id not in (select fagsak_id from fagsak_egenskap where egenskap_value = 'PRAKSIS_UTSETTELSE' and aktiv='J')
        """;

    public List<Long> alleSakerMorAvslagUtsettelse() {
        var query = entityManager.createNativeQuery(QUERY_MERKING_MOR)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        @SuppressWarnings("unchecked")
        var resultat = query.getResultList();
        return resultat;
    }

    /*
     * FAR/MEDMOR - begge rett eller aleneomsorg
     */
    private static final String QUERY_MERKING_FAR_BEGGE_ALENE = """
        select distinct f.id from fagsak f
        join behandling b on fagsak_id = f.id
        join behandling_resultat br  on b.id = br.behandling_id
        join behandling_vedtak bv on bv.behandling_resultat_id = br.id
        join (select beh.fagsak_id fsmax, max(bvsq.opprettet_tid) maxbr from behandling beh
              join behandling_resultat brsq on brsq.behandling_id=beh.id
              join behandling_vedtak bvsq on bvsq.behandling_resultat_id = brsq.id
              where beh.behandling_type in ('BT-002','BT-004') and beh.behandling_status in ('AVSLU', 'IVED')
              group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.opprettet_tid = maxbr)
        join uttak_resultat ur on (ur.behandling_resultat_id = br.id and ur.aktiv='J')
        join uttak_resultat_periode peri on uttak_resultat_perioder_id = nvl(ur.overstyrt_perioder_id, ur.opprinnelig_perioder_id)
        join uttak_resultat_periode_akt peria on peria.uttak_resultat_periode_id = peri.id
        join gr_ytelses_fordeling gy on (gy.behandling_id = b.id and gy.aktiv='J')
        left outer join so_rettighet soro on soro.id = gy.overstyrt_rettighet_id
        where 1=1
          and b.behandling_type in ('BT-002','BT-004') and b.behandling_status in ('AVSLU', 'IVED')
          and peri.tom >= '01.10.2021'
          and uttak_utsettelse_type <> '-'
          and (utbetalingsprosent is null or utbetalingsprosent = 0)
          and trekkdager_desimaler > 0
          and trekkonto <> 'FORELDREPENGER_FØR_FØDSEL'
          and periode_resultat_aarsak not in ( '4030', '4031', '4071', '4072', '4077', '4103', '4104', '4110', '4111', '4112', '4115', '4116', '4117' )
          and bruker_rolle <> 'MORA'
          --and soro.aleneomsorg = 'J'
          and f.id not in
              (select fagsak_en_id from fagsak_relasjon join STOENADSKONTO  on STOENADSKONTOBEREGNING_ID = nvl(OVERSTYRT_KONTO_BEREGNING_ID, KONTO_BEREGNING_ID)
               where aktiv='J' and stoenadskontotype = 'FORELDREPENGER'
               UNION
               select fagsak_to_id from fagsak_relasjon join STOENADSKONTO  on STOENADSKONTOBEREGNING_ID = nvl(OVERSTYRT_KONTO_BEREGNING_ID, KONTO_BEREGNING_ID)
               where aktiv='J' and stoenadskontotype = 'FORELDREPENGER' and fagsak_to_id is not null)
          and ytelse_type='FP'
          and til_infotrygd = 'N'
          and (bv.vedtak_resultat_type in ('INNVILGET', 'AVSLAG') or (bv.vedtak_resultat_type = 'OPPHØR'
          and exists (select * from  br_resultat_behandling ty
                      join br_periode brp on brp.BEREGNINGSRESULTAT_FP_ID = nvl(UTBET_BEREGNINGSRESULTAT_FP_ID,BG_BEREGNINGSRESULTAT_FP_ID)
                      join br_andel ba on ba.br_periode_id = brp.id
                      where ty.behandling_id=b.id and ty.aktiv='J' and dagsats > 0)))
          and f.id not in (select fagsak_id from fagsak_egenskap where egenskap_value = 'PRAKSIS_UTSETTELSE' and aktiv='J')
        """;

    public List<Long> alleSakerFarBeggeEllerAleneUtsettelse() {
        var query = entityManager.createNativeQuery(QUERY_MERKING_FAR_BEGGE_ALENE)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        @SuppressWarnings("unchecked")
        var resultat = query.getResultList();
        return resultat;
    }

    private static final String QUERY_MERKET = """
        select * from (
          select distinct fagsak_id from fagsak_egenskap
           where egenskap_value = 'PRAKSIS_UTSETTELSE' and aktiv='J'
           and fagsak_id not in (select fagsak_id from behandling bi1 where bi1.behandling_status <> 'AVSLU')
           and fagsak_id not in (select fagsak_id from behandling bi2 join behandling_arsak ba on bi2.id = ba.behandling_id
                            where ba.behandling_arsak_type = :feilp)
           and fagsak_id >:fraFagsakId
           order by fagsak_id
        ) where ROWNUM <= 100
        """;

    public List<Long> finnNesteHundreSakerSomErMerketFeilIverksettelseFriUtsettelse(Long fraFagsakId) {
        var query = entityManager.createNativeQuery(QUERY_MERKET)
            .setParameter("fraFagsakId", fraFagsakId == null ? 0 : fraFagsakId)
            .setParameter("feilp", BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE.getKode())
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        @SuppressWarnings("unchecked")
        var resultat = query.getResultList();
        return resultat;
    }

    /**
     * For å utvide ventefrist for behandlinger der bruker har tatt kontakt via gosys
     */
    private static final String QUERY_VENTEFRIST_DESEMBER = """
        select * from (
          select distinct b.FAGSAK_ID, b.ID, f.saksnummer from fpsak.BEHANDLING b
                                               join fpsak.FAGSAK f on b.FAGSAK_ID = f.ID
                                               join fpsak.AKSJONSPUNKT a on a.BEHANDLING_ID = b.ID
                                               join fpsak.BEHANDLING_STEG_TILSTAND steg on steg.BEHANDLING_ID = b.ID
                                               join fpsak.BEHANDLING_ARSAK ba on ba.BEHANDLING_ID = b.ID
                          where b.id > :fraBehandlingId
                          and ba.BEHANDLING_ARSAK_TYPE = 'FEIL_PRAKSIS_UTSETTELSE'
                          and a.AKSJONSPUNKT_DEF = '7013'
                          and a.AKSJONSPUNKT_STATUS = 'OPPR'
                          and a.FRIST_TID < to_date('2026-01-01', 'yyyy-mm-dd')
                          and b.BEHANDLING_STATUS = 'UTRED'
                          order by b.id
        ) where ROWNUM <= 100
        """;


    public List<BehandlingMedFagsakId> finnNesteHundreBehandlingerSomErPåVentTilDesember(Long fraBehandlingId) {
        var query = entityManager.createNativeQuery(QUERY_VENTEFRIST_DESEMBER, BehandlingMedFagsakId.class)
            .setParameter("fraBehandlingId", fraBehandlingId == null ? 0 : fraBehandlingId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

    public record BehandlingMedFagsakId(Long fagsakId, Long id, String saksnummer) {
    }
}
