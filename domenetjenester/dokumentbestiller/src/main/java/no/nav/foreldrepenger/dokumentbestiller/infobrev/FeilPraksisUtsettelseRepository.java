package no.nav.foreldrepenger.dokumentbestiller.infobrev;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;

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

    private static final String QUERY_MOR = """
        select * from (
        select f.id fid
        from fagsak f
           join behandling b on fagsak_id = f.id
           join behandling_resultat br  on b.id = br.behandling_id
           join behandling_vedtak bv on bv.behandling_resultat_id = br.id
           join (select beh.fagsak_id fsmax, max(bvsq.opprettet_tid) maxbr from behandling beh
                join behandling_resultat brsq on brsq.behandling_id=beh.id
                join behandling_vedtak bvsq on bvsq.behandling_resultat_id = brsq.id
                where beh.behandling_type in ('BT-002','BT-004') and beh.behandling_status in ('AVSLU', 'IVED')
                group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.opprettet_tid = maxbr)
           join uttak_resultat ur on (ur.behandling_resultat_id = br.id and ur.aktiv='J')
        where 1=1
           and b.behandling_type in ('BT-002','BT-004') and b.behandling_status in ('AVSLU', 'IVED')
           and nvl(ur.overstyrt_perioder_id, ur.opprinnelig_perioder_id) in (
              select uttak_resultat_perioder_id from uttak_resultat_periode peri
              where (peri.gradering_avslag_AARSAK = '4501' and samtidig_uttaksprosent is null
                     and exists (select * from uttak_resultat_periode_akt peria
                          where peria.uttak_resultat_periode_id = peri.id and utbetalingsprosent < 100)
              ) or (peri.PERIODE_RESULTAT_AARSAK in ( '4005','4081','4082')
                    and exists (select * from uttak_resultat_periode_akt peria
                          where peria.uttak_resultat_periode_id = peri.id and utbetalingsprosent = 0 and trekkdager_desimaler > 0)
              )
           )
           and bruker_rolle = 'MORA'
           and ytelse_type='FP'
           and til_infotrygd = 'N'
           and (bv.vedtak_resultat_type = 'INNVILGET' or (bv.vedtak_resultat_type = 'OPPHØR'
                and exists (select * from  br_resultat_behandling ty
                       join br_periode brp on brp.BEREGNINGSRESULTAT_FP_ID = nvl(UTBET_BEREGNINGSRESULTAT_FP_ID,BG_BEREGNINGSRESULTAT_FP_ID)
                       join br_andel ba on ba.br_periode_id = brp.id
                       where ty.behandling_id=b.id and ty.aktiv='J' and dagsats > 0)))
           and f.id not in (select fagsak_id from behandling bi1 where bi1.behandling_status <> 'AVSLU' and bi1.behandling_type in ('BT-002','BT-004'))
           and f.id not in (select fagsak_id from behandling bi2 join behandling_arsak ba on bi2.id = ba.behandling_id
                            where ba.behandling_arsak_type = :feilp)
           and f.id >:fraFagsakId
           order by fid
        ) where ROWNUM <= 100
        """;

    public List<Long> finnNesteHundreAktuelleSakerMor(Long fraFagsakId) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene: - Saker
         * (Foreldrepenger, Mors sak, Ikke Stengt) med avsluttet behandling som har max
         * uttaksdato i gitt intervall - Begrenset til ikke aleneomsorg - Begrenset til
         * levende barn - Begrenset til at det er oppgitt annen part
         */
        var query = entityManager.createNativeQuery(QUERY_MOR)
            .setParameter("fraFagsakId", fraFagsakId == null ? 0 : fraFagsakId)
            .setParameter("feilp", BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE.getKode())
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        @SuppressWarnings("unchecked")
        var resultat = query.getResultList();
        return resultat;
    }

    /*
     * OBS: Her er avslag med.
     */
    private static final String QUERY_FAR_BEGGE_ALENE = """
        select * from (
           select f.id fid
           from fagsak f
              join behandling b on fagsak_id = f.id
              join behandling_resultat br  on b.id = br.behandling_id
              join behandling_vedtak bv on bv.behandling_resultat_id = br.id
              join (select beh.fagsak_id fsmax, max(bvsq.opprettet_tid) maxbr from behandling beh
                   join behandling_resultat brsq on brsq.behandling_id=beh.id
                   join behandling_vedtak bvsq on bvsq.behandling_resultat_id = brsq.id
                   where beh.behandling_type in ('BT-002','BT-004') and beh.behandling_status in ('AVSLU', 'IVED')
                   group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.opprettet_tid = maxbr)
              join uttak_resultat ur on (ur.behandling_resultat_id = br.id and ur.aktiv='J')
              join gr_ytelses_fordeling gy on (gy.behandling_id = b.id and gy.aktiv='J')
              left outer join so_rettighet soro on soro.id = gy.overstyrt_rettighet_id
           where 1=1
              and b.behandling_type in ('BT-002','BT-004') and b.behandling_status in ('AVSLU', 'IVED')
              and nvl(ur.overstyrt_perioder_id, ur.opprinnelig_perioder_id) in (
                 select uttak_resultat_perioder_id from uttak_resultat_periode peri
                 where (peri.gradering_avslag_AARSAK = '4501' and samtidig_uttaksprosent is null
                        and exists (select * from uttak_resultat_periode_akt peria
                             where peria.uttak_resultat_periode_id = peri.id and utbetalingsprosent < 100)
                 ) or (peri.PERIODE_RESULTAT_AARSAK in ( '4005','4081','4082')
                       and exists (select * from uttak_resultat_periode_akt peria
                             where peria.uttak_resultat_periode_id = peri.id  and utbetalingsprosent = 0 and trekkdager_desimaler > 0)
                 )
              )
              and bruker_rolle <> 'MORA'
              and (soro.aleneomsorg = 'J' or
                 f.id not in (select fagsak_en_id from fagsak_relasjon join STOENADSKONTO  on STOENADSKONTOBEREGNING_ID = nvl(OVERSTYRT_KONTO_BEREGNING_ID, KONTO_BEREGNING_ID)
                               where aktiv='J' and stoenadskontotype = 'FORELDREPENGER'
                               UNION
                               select fagsak_to_id from fagsak_relasjon join STOENADSKONTO  on STOENADSKONTOBEREGNING_ID = nvl(OVERSTYRT_KONTO_BEREGNING_ID, KONTO_BEREGNING_ID)
                               where aktiv='J' and stoenadskontotype = 'FORELDREPENGER' and fagsak_to_id is not null)
                )
              and ytelse_type='FP'
              and til_infotrygd = 'N'
              and (bv.vedtak_resultat_type in ('INNVILGET', 'AVSLAG') or (bv.vedtak_resultat_type = 'OPPHØR'
                   and exists (select * from  br_resultat_behandling ty
                          join br_periode brp on brp.BEREGNINGSRESULTAT_FP_ID = nvl(UTBET_BEREGNINGSRESULTAT_FP_ID,BG_BEREGNINGSRESULTAT_FP_ID)
                          join br_andel ba on ba.br_periode_id = brp.id
                          where ty.behandling_id=b.id and ty.aktiv='J' and dagsats > 0)))
           and f.id not in (select fagsak_id from behandling bi1 where bi1.behandling_status <> 'AVSLU' and bi1.behandling_type in ('BT-002','BT-004'))
           and f.id not in (select fagsak_id from behandling bi2 join behandling_arsak ba on bi2.id = ba.behandling_id
                            where ba.behandling_arsak_type = :feilp)
           and f.id >:fraFagsakId
           order by fid
        ) where ROWNUM <= 100
        """;

    public List<Long> finnNesteHundreAktuelleSakerFarBeggeEllerAlene(Long fraFagsakId) {
        var query = entityManager.createNativeQuery(QUERY_FAR_BEGGE_ALENE)
            .setParameter("fraFagsakId", fraFagsakId == null ? 0 : fraFagsakId)
            .setParameter("feilp", BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE.getKode())
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        @SuppressWarnings("unchecked")
        var resultat = query.getResultList();
        return resultat;
    }

    private static final String QUERY_BARE_FAR_HAR_RETT = """
        select * from (
         select f.id fid
         from fagsak f
            join behandling b on fagsak_id = f.id
            join behandling_resultat br  on b.id = br.behandling_id
            join behandling_vedtak bv on bv.behandling_resultat_id = br.id
            join (select beh.fagsak_id fsmax, max(bvsq.opprettet_tid) maxbr from behandling beh
                 join behandling_resultat brsq on brsq.behandling_id=beh.id
                 join behandling_vedtak bvsq on bvsq.behandling_resultat_id = brsq.id
                 where beh.behandling_type in ('BT-002','BT-004') and beh.behandling_status in ('AVSLU', 'IVED')
                 group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.opprettet_tid = maxbr)
            join uttak_resultat ur on (ur.behandling_resultat_id = br.id and ur.aktiv='J')
            join gr_ytelses_fordeling gy on (gy.behandling_id = b.id and gy.aktiv='J')
            left outer join so_rettighet soro on soro.id = gy.overstyrt_rettighet_id
         where 1=1
            and b.behandling_type in ('BT-002','BT-004') and b.behandling_status in ('AVSLU', 'IVED')
            and nvl(ur.overstyrt_perioder_id, ur.opprinnelig_perioder_id) in (
               select uttak_resultat_perioder_id from uttak_resultat_periode peri
               where (peri.gradering_avslag_AARSAK = '4501' and samtidig_uttaksprosent is null
                      and exists (select * from uttak_resultat_periode_akt peria
                           where peria.uttak_resultat_periode_id = peri.id and utbetalingsprosent < 100)
               ) or (peri.PERIODE_RESULTAT_AARSAK in ('4081','4082')
                     and exists (select * from uttak_resultat_periode_akt peria
                           where peria.uttak_resultat_periode_id = peri.id  and utbetalingsprosent = 0 and trekkdager_desimaler > 0)
               )
            )
            and bruker_rolle <> 'MORA'
            and (soro.aleneomsorg is null or soro.aleneomsorg = 'N')
            and f.id in (select fagsak_en_id from fagsak_relasjon join STOENADSKONTO  on STOENADSKONTOBEREGNING_ID = nvl(OVERSTYRT_KONTO_BEREGNING_ID, KONTO_BEREGNING_ID)
                             where aktiv='J' and stoenadskontotype = 'FORELDREPENGER'
                             UNION
                             select fagsak_to_id from fagsak_relasjon join STOENADSKONTO  on STOENADSKONTOBEREGNING_ID = nvl(OVERSTYRT_KONTO_BEREGNING_ID, KONTO_BEREGNING_ID)
                             where aktiv='J' and stoenadskontotype = 'FORELDREPENGER' and fagsak_to_id is not null)
            and ytelse_type='FP'
            and til_infotrygd = 'N'
            and (bv.vedtak_resultat_type = 'INNVILGET' or (bv.vedtak_resultat_type = 'OPPHØR'
                 and exists (select * from  br_resultat_behandling ty
                        join br_periode brp on brp.BEREGNINGSRESULTAT_FP_ID = nvl(UTBET_BEREGNINGSRESULTAT_FP_ID,BG_BEREGNINGSRESULTAT_FP_ID)
                        join br_andel ba on ba.br_periode_id = brp.id
                        where ty.behandling_id=b.id and ty.aktiv='J' and dagsats > 0)))
           and f.id not in (select fagsak_id from behandling bi1 where bi1.behandling_status <> 'AVSLU' and bi1.behandling_type in ('BT-002','BT-004'))
           and f.id not in (select fagsak_id from behandling bi2 join behandling_arsak ba on bi2.id = ba.behandling_id
                            where ba.behandling_arsak_type = :feilp)
           and f.id >:fraFagsakId
           order by fid
        ) where ROWNUM <= 100
        """;

    public List<Long> finnNesteHundreAktuelleSakerBareFarHarRett(Long fraFagsakId) {
        var query = entityManager.createNativeQuery(QUERY_BARE_FAR_HAR_RETT)
            .setParameter("fraFagsakId", fraFagsakId == null ? 0 : fraFagsakId)
            .setParameter("feilp", BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE.getKode())
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        @SuppressWarnings("unchecked")
        var resultat = query.getResultList();
        return resultat;
    }

    private static final String QUERY_MERKING_MOR = """
        select * from (
        select f.id fid
        from fagsak f
           join behandling b on fagsak_id = f.id
           join behandling_resultat br  on b.id = br.behandling_id
           join behandling_vedtak bv on bv.behandling_resultat_id = br.id
           join (select beh.fagsak_id fsmax, max(bvsq.opprettet_tid) maxbr from behandling beh
                join behandling_resultat brsq on brsq.behandling_id=beh.id
                join behandling_vedtak bvsq on bvsq.behandling_resultat_id = brsq.id
                where beh.behandling_type in ('BT-002','BT-004') and beh.behandling_status in ('AVSLU', 'IVED')
                group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.opprettet_tid = maxbr)
           join uttak_resultat ur on (ur.behandling_resultat_id = br.id and ur.aktiv='J')
        where 1=1
           and b.behandling_type in ('BT-002','BT-004') and b.behandling_status in ('AVSLU', 'IVED')
           and nvl(ur.overstyrt_perioder_id, ur.opprinnelig_perioder_id) in (
              select uttak_resultat_perioder_id from uttak_resultat_periode peri
              where (peri.gradering_avslag_AARSAK = '4501' and samtidig_uttaksprosent is null
                     and exists (select * from uttak_resultat_periode_akt peria
                          where peria.uttak_resultat_periode_id = peri.id and utbetalingsprosent < 100)
              ) or (peri.PERIODE_RESULTAT_AARSAK in ( '4005','4081','4082')
                    and exists (select * from uttak_resultat_periode_akt peria
                          where peria.uttak_resultat_periode_id = peri.id and utbetalingsprosent = 0 and trekkdager_desimaler > 0)
              )
           )
           and bruker_rolle = 'MORA'
           and ytelse_type='FP'
           and til_infotrygd = 'N'
           and (bv.vedtak_resultat_type = 'INNVILGET' or (bv.vedtak_resultat_type = 'OPPHØR'
                and exists (select * from  br_resultat_behandling ty
                       join br_periode brp on brp.BEREGNINGSRESULTAT_FP_ID = nvl(UTBET_BEREGNINGSRESULTAT_FP_ID,BG_BEREGNINGSRESULTAT_FP_ID)
                       join br_andel ba on ba.br_periode_id = brp.id
                       where ty.behandling_id=b.id and ty.aktiv='J' and dagsats > 0)))
           and f.id not in (select fagsak_id from fagsak_egenskap where egenskap_value = 'PRAKSIS_UTSETTELSE' and aktiv='J')
           and f.id >:fraFagsakId
           order by fid
        ) where ROWNUM <= 100
        """;

    public List<Long> finnNesteHundreSakerForMerkingMor(Long fraFagsakId) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene: - Saker
         * (Foreldrepenger, Mors sak, Ikke Stengt) med avsluttet behandling som har max
         * uttaksdato i gitt intervall - Begrenset til ikke aleneomsorg - Begrenset til
         * levende barn - Begrenset til at det er oppgitt annen part
         */
        var query = entityManager.createNativeQuery(QUERY_MERKING_MOR)
            .setParameter("fraFagsakId", fraFagsakId == null ? 0 : fraFagsakId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        @SuppressWarnings("unchecked")
        var resultat = query.getResultList();
        return resultat;
    }

    /*
     * OBS: Her er avslag med.
     */
    private static final String QUERY_MERKING_FAR_BEGGE_ALENE = """
        select * from (
           select f.id fid
           from fagsak f
              join behandling b on fagsak_id = f.id
              join behandling_resultat br  on b.id = br.behandling_id
              join behandling_vedtak bv on bv.behandling_resultat_id = br.id
              join (select beh.fagsak_id fsmax, max(bvsq.opprettet_tid) maxbr from behandling beh
                   join behandling_resultat brsq on brsq.behandling_id=beh.id
                   join behandling_vedtak bvsq on bvsq.behandling_resultat_id = brsq.id
                   where beh.behandling_type in ('BT-002','BT-004') and beh.behandling_status in ('AVSLU', 'IVED')
                   group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.opprettet_tid = maxbr)
              join uttak_resultat ur on (ur.behandling_resultat_id = br.id and ur.aktiv='J')
              join gr_ytelses_fordeling gy on (gy.behandling_id = b.id and gy.aktiv='J')
              left outer join so_rettighet soro on soro.id = gy.overstyrt_rettighet_id
           where 1=1
              and b.behandling_type in ('BT-002','BT-004') and b.behandling_status in ('AVSLU', 'IVED')
              and nvl(ur.overstyrt_perioder_id, ur.opprinnelig_perioder_id) in (
                 select uttak_resultat_perioder_id from uttak_resultat_periode peri
                 where (peri.gradering_avslag_AARSAK = '4501' and samtidig_uttaksprosent is null
                        and exists (select * from uttak_resultat_periode_akt peria
                             where peria.uttak_resultat_periode_id = peri.id and utbetalingsprosent < 100)
                 ) or (peri.PERIODE_RESULTAT_AARSAK in ( '4005','4081','4082')
                       and exists (select * from uttak_resultat_periode_akt peria
                             where peria.uttak_resultat_periode_id = peri.id  and utbetalingsprosent = 0 and trekkdager_desimaler > 0)
                 )
              )
              and bruker_rolle <> 'MORA'
              and (soro.aleneomsorg = 'J' or
                 f.id not in (select fagsak_en_id from fagsak_relasjon join STOENADSKONTO  on STOENADSKONTOBEREGNING_ID = nvl(OVERSTYRT_KONTO_BEREGNING_ID, KONTO_BEREGNING_ID)
                               where aktiv='J' and stoenadskontotype = 'FORELDREPENGER'
                               UNION
                               select fagsak_to_id from fagsak_relasjon join STOENADSKONTO  on STOENADSKONTOBEREGNING_ID = nvl(OVERSTYRT_KONTO_BEREGNING_ID, KONTO_BEREGNING_ID)
                               where aktiv='J' and stoenadskontotype = 'FORELDREPENGER' and fagsak_to_id is not null)
                )
              and ytelse_type='FP'
              and til_infotrygd = 'N'
              and (bv.vedtak_resultat_type in ('INNVILGET', 'AVSLAG') or (bv.vedtak_resultat_type = 'OPPHØR'
                   and exists (select * from  br_resultat_behandling ty
                          join br_periode brp on brp.BEREGNINGSRESULTAT_FP_ID = nvl(UTBET_BEREGNINGSRESULTAT_FP_ID,BG_BEREGNINGSRESULTAT_FP_ID)
                          join br_andel ba on ba.br_periode_id = brp.id
                          where ty.behandling_id=b.id and ty.aktiv='J' and dagsats > 0)))
              and f.id not in (select fagsak_id from fagsak_egenskap where egenskap_value = 'PRAKSIS_UTSETTELSE' and aktiv='J')
           and f.id >:fraFagsakId
           order by fid
        ) where ROWNUM <= 100
        """;

    public List<Long> finnNesteHundreSakerForMerkingFarBeggeEllerAlene(Long fraFagsakId) {
        var query = entityManager.createNativeQuery(QUERY_MERKING_FAR_BEGGE_ALENE)
            .setParameter("fraFagsakId", fraFagsakId == null ? 0 : fraFagsakId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        @SuppressWarnings("unchecked")
        var resultat = query.getResultList();
        return resultat;
    }



}
