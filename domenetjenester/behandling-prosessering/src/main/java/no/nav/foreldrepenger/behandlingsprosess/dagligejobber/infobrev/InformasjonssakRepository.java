package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

/**
 * Spesialmetoder for å hente opp saker og personer som er kandidat for å sende informasjonsbrev om rettighet
 */

@ApplicationScoped
public class InformasjonssakRepository {

    private static final List<String> INFOBREV_TYPER = List.of(BehandlingÅrsakType.INFOBREV_OPPHOLD.getKode(), BehandlingÅrsakType.INFOBREV_BEHANDLING.getKode());
    private static final List<String> INNVILGET_TYPER = List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.FORELDREPENGER_ENDRET.getKode(),
        BehandlingResultatType.INGEN_ENDRING.getKode());
    private static final List<String> SENERE_TYPER = List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.FORELDREPENGER_ENDRET.getKode(),
        BehandlingResultatType.INGEN_ENDRING.getKode(), BehandlingResultatType.OPPHØR.getKode());

    // Query-resultat posisjon
    private static final int POS_FAGSAKID = 0;
    private static final int POS_OPPRDATO = 1;
    private static final int POS_AKTORID = 2;
    private static final int POS_FHDATO = 3;
    private static final int POS_ENHETID = 4;
    private static final int POS_ENHETNAVN = 5;

    private EntityManager entityManager;

    InformasjonssakRepository() {
        // for CDI proxy
    }

    @Inject
    public InformasjonssakRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Liste over siste ytelsesbehandling for saker med 4 uker til innvilget maksdato +andre kriterier */
    public List<InformasjonssakData> finnSakerMedInnvilgetMaksdatoInnenIntervall(LocalDate fom, LocalDate tom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         *  - Saker (Foreldrepenger, Mors sak, Ikke Stengt) med avsluttet behandling som har max uttaksdato i gitt intervall
         *  - Begrenset til ikke aleneomsorg
         *  - Begrenset til levende barm
         *  - Begrenset til at det er oppgitt annen part
         */
        List<String> avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).collect(Collectors.toList());
        Query query = entityManager.createNativeQuery(
        " select distinct fs.id, trunc(fs.opprettet_tid), anpa.aktoer_id, ub.foedsel_dato, beh.behandlende_enhet, beh.behandlende_enhet_navn from fagsak fs " +
            " join fagsak_relasjon fr on (fs.id in (fr.fagsak_en_id , fr.fagsak_to_id) and fr.aktiv='J' and fr.fagsak_to_id is null) " +
            " join behandling beh on beh.fagsak_id = fs.id " +
            " join behandling_resultat br on (br.behandling_id=beh.id and br.behandling_resultat_type in (:restyper)) " +
            " join uttak_resultat ur on (ur.behandling_resultat_id=br.id and ur.aktiv='J') " +
            " join (select uttak_resultat_perioder_id urpid, max(tom) " +
            "        from uttak_resultat_periode urp " +
            "        left join uttak_resultat_periode_akt pa on pa.uttak_resultat_periode_id = urp.id " +
            "        where periode_resultat_type=:uttakinnvilget OR nvl(trekkdager_desimaler,0) > 0  " +
            "        group by uttak_resultat_perioder_id " +
            "        having max(tom) <= :tomdato and max(tom) >= :fomdato " +
            "    ) on urpid=nvl(overstyrt_perioder_id, opprinnelig_perioder_id) " +
            " join GR_YTELSES_FORDELING gryf on (gryf.behandling_id=beh.id and gryf.aktiv='J') " +
            " join SO_RETTIGHET rett on (gryf.so_rettighet_id = rett.id and rett.aleneomsorg='N') " +
            " join gr_personopplysning grpo on (beh.id=grpo.behandling_id and grpo.aktiv='J') " +
            " join so_annen_part anpa on (grpo.so_annen_part_id=anpa.id and anpa.aktoer_id is not null) " +
            " join gr_familie_hendelse grfh on (beh.id=grfh.behandling_id and grfh.aktiv='J') " +
            " join fh_uidentifisert_barn ub on (ub.familie_hendelse_id=grfh.bekreftet_familie_hendelse_id and ub.doedsdato is null) " +
            " where beh.behandling_status in (:avsluttet) " +
            " and fs.ytelse_type = :foreldrepenger " +
            " and fs.bruker_rolle = :relrolle " +
            " and fs.til_infotrygd='N' " +
            " and not exists (select * from behandling bh1 join behandling_resultat br1 on br1.behandling_id=bh1.id " +
            "    where bh1.fagsak_id=beh.fagsak_id " +
            "    and br1.behandling_resultat_type in (:seneretyper) " +
            "    and br1.opprettet_tid>br.opprettet_tid and bh1.behandling_status in (:avsluttet) ) " +
            " and not exists ( select * from behandling bh2 join gr_familie_hendelse grfh2 on (bh2.id=grfh2.behandling_id and grfh2.aktiv='J') " +
            "    join fh_uidentifisert_barn ub2 on (ub2.familie_hendelse_id in (grfh2.bekreftet_familie_hendelse_id, grfh2.overstyrt_familie_hendelse_id) and ub2.doedsdato is not null ) " +
            "    where bh2.fagsak_id = beh.fagsak_id " +
            "    and grfh2.opprettet_tid > grfh.opprettet_tid ) " +
            " and not exists (select * from fagsak fs1 join bruker bru1 on fs1.bruker_id=bru1.id " +
            "    join behandling beh1 on beh1.fagsak_id = fs1.id " +
            "    join behandling_arsak ba1 on ba1.behandling_id=beh1.id " +
            "    where bru1.aktoer_id=anpa.aktoer_id " +
            "    and ba1.behandling_arsak_type in (:infobrev) " +
            "    and fs1.opprettet_tid > fs.opprettet_tid ) "
        ); //$NON-NLS-1$
        query.setParameter("fomdato", fom); //$NON-NLS-1$
        query.setParameter("tomdato", tom); //$NON-NLS-1$7
        query.setParameter("uttakinnvilget", PeriodeResultatType.INNVILGET.getKode()); //$NON-NLS-1$
        query.setParameter("foreldrepenger", FagsakYtelseType.FORELDREPENGER.getKode()); //$NON-NLS-1$
        query.setParameter("relrolle", RelasjonsRolleType.MORA.getKode()); //$NON-NLS-1$
        query.setParameter("infobrev", INFOBREV_TYPER); //$NON-NLS-1$
        query.setParameter("restyper", INNVILGET_TYPER); //$NON-NLS-1$
        query.setParameter("seneretyper", SENERE_TYPER); //$NON-NLS-1$
        query.setParameter("avsluttet", avsluttendeStatus); //$NON-NLS-1$
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return toInformasjonssakData(resultatList);
    }

    public List<InformasjonssakData> finnSakerMedMinsteInnvilgetOppholdperiodeInnenIntervall(LocalDate fom, LocalDate tom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         *  - Saker (Foreldrepenger, Mors sak, Ikke Stengt) med avsluttet behandling som har minst en innvilget oppholdsperiode i gitt intervall
         *  - Begrenset til ikke aleneomsorg
         *  - Begrenset til levende barm
         *  - Begrenset til at det er oppgitt annen part
         */
        List<String> avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).collect(Collectors.toList());
        Query query = entityManager.createNativeQuery(
            " select distinct fs.id, trunc(fs.opprettet_tid), anpa.aktoer_id, ub.foedsel_dato, beh.behandlende_enhet, beh.behandlende_enhet_navn from fagsak fs " +
                " join fagsak_relasjon fr on (fs.id in (fr.fagsak_en_id , fr.fagsak_to_id) and fr.aktiv='J' and fr.fagsak_to_id is null) " +
                " join behandling beh on beh.fagsak_id = fs.id " +
                " join behandling_resultat br on (br.behandling_id=beh.id and br.behandling_resultat_type in (:restyper)) " +
                " join uttak_resultat ur on (ur.behandling_resultat_id=br.id and ur.aktiv='J') " +
                " join (select uttak_resultat_perioder_id urpid, min(fom) " +
                "        from uttak_resultat_periode urp " +
                "        where periode_resultat_type=:uttakinnvilget " +
                "        and opphold_aarsak in (:oppholdsaarsaker)" +
                "        group by uttak_resultat_perioder_id " +
                "        having min(fom) >= :fomdato and min(fom) <= :tomdato " +
                "    ) on urpid=nvl(overstyrt_perioder_id, opprinnelig_perioder_id) " +
                " join gr_personopplysning grpo on (beh.id=grpo.behandling_id and grpo.aktiv='J') " +
                " join so_annen_part anpa on (grpo.so_annen_part_id=anpa.id and anpa.aktoer_id is not null) " +
                " join gr_familie_hendelse grfh on (beh.id=grfh.behandling_id and grfh.aktiv='J') " +
                " join fh_uidentifisert_barn ub on (ub.familie_hendelse_id=grfh.bekreftet_familie_hendelse_id and ub.doedsdato is null) " +
                " where beh.behandling_status in (:avsluttet) " +
                " and fs.ytelse_type = :foreldrepenger " +
                " and fs.bruker_rolle = :relrolle " +
                " and fs.til_infotrygd='N' " +
                " and not exists (select * from behandling bh1 join behandling_resultat br1 on br1.behandling_id=bh1.id " +
                "    where bh1.fagsak_id=beh.fagsak_id " +
                "    and br1.behandling_resultat_type in (:seneretyper) " +
                "    and br1.opprettet_tid>br.opprettet_tid and bh1.behandling_status in (:avsluttet) ) " +
                " and not exists ( select * from behandling bh2 join gr_familie_hendelse grfh2 on (bh2.id=grfh2.behandling_id and grfh2.aktiv='J') " +
                "    join fh_uidentifisert_barn ub2 on (ub2.familie_hendelse_id in (grfh2.bekreftet_familie_hendelse_id, grfh2.overstyrt_familie_hendelse_id) and ub2.doedsdato is not null ) " +
                "    where bh2.fagsak_id = beh.fagsak_id " +
                "    and grfh2.opprettet_tid > grfh.opprettet_tid ) " +
                " and not exists (select * from fagsak fs1 join bruker bru1 on fs1.bruker_id=bru1.id " +
                "    join behandling beh1 on beh1.fagsak_id = fs1.id " +
                "    join behandling_arsak ba1 on ba1.behandling_id=beh1.id " +
                "    where bru1.aktoer_id=anpa.aktoer_id " +
                "    and ba1.behandling_arsak_type in (:infobrev) " +
                "    and fs1.opprettet_tid > fs.opprettet_tid ) "
        ); //$NON-NLS-1$
        query.setParameter("tomdato", tom); //$NON-NLS-1$
        query.setParameter("fomdato", fom); //$NON-NLS-1$7
        query.setParameter("uttakinnvilget", PeriodeResultatType.INNVILGET.getKode()); //$NON-NLS-1$
        query.setParameter("foreldrepenger", FagsakYtelseType.FORELDREPENGER.getKode()); //$NON-NLS-1$
        query.setParameter("relrolle", RelasjonsRolleType.MORA.getKode()); //$NON-NLS-1$
        query.setParameter("infobrev", INFOBREV_TYPER); //$NON-NLS-1$
        query.setParameter("restyper", INNVILGET_TYPER); //$NON-NLS-1$
        query.setParameter("seneretyper", SENERE_TYPER); //$NON-NLS-1$
        query.setParameter("avsluttet", avsluttendeStatus); //$NON-NLS-1$
        query.setParameter("oppholdsaarsaker", List.of(OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER.getKode(), OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER.getKode(), OppholdÅrsak.KVOTE_FORELDREPENGER_ANNEN_FORELDER.getKode()));
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return toInformasjonssakData(resultatList);
    }

    private  List<InformasjonssakData> toInformasjonssakData(List<Object[]> resultatList) {
        List<InformasjonssakData> returnList = new ArrayList<>();
        resultatList.forEach(resultat -> {
            InformasjonssakData.InformasjonssakDataBuilder builder = InformasjonssakData.InformasjonssakDataBuilder
                .ny(((BigDecimal) resultat[POS_FAGSAKID]).longValue()) // NOSONAR
                .medAktørIdAnnenPart((String) resultat[POS_AKTORID]) // NOSONAR
                .medOpprettetDato(((Timestamp) resultat[POS_OPPRDATO]).toLocalDateTime().toLocalDate()) // NOSONAR
                .medHendelseDato(((Timestamp) resultat[POS_FHDATO]).toLocalDateTime().toLocalDate()) // NOSONAR
                .medEnhet((String) resultat[POS_ENHETID]) // NOSONAR
                .medEnhetNavn((String) resultat[POS_ENHETNAVN]); // NOSONAR
            returnList.add(builder.build());
            });
        return returnList;
    }

    public List<OverlappData> finnSakerOpprettetInnenIntervallMedSisteVedtak(LocalDate fom, LocalDate tom) {
        /*
         * Plukker saksnummer, siste ytelsebehandling, annenpart og første uttaksdato:
         *  - Saker der det finnes et beregnignsresultat/TY med utbetalt periode - inkusive noen få opphør fom etter tidligste periode
         *  - Første uttaksdato = tidligste periode, inkusive innvilget utsettelse.
         *  - Kan gi noen tilfelle med avslått første periode
         */
        List<String> avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).collect(Collectors.toList());
        Query query = entityManager.createNativeQuery(
            " select distinct saksnummer, ytelse_type, bru.aktoer_id braid, bruker_rolle, beh.id, anpa.aktoer_id, minbrfom " +
                "from fagsak fs join bruker bru on fs.bruker_id = bru.id join behandling beh on fagsak_id=fs.id" +
                " join behandling_resultat br on (br.behandling_id=beh.id and br.behandling_resultat_type in (:restyper)) " +
                " join (select beh1.fagsak_id fsmax, max(br1.opprettet_tid) maxbr from behandling beh1 " +
                "      join behandling_resultat br1 on (br1.behandling_id=beh1.id and br1.behandling_resultat_type in (:restyper)) " +
                "      where beh1.behandling_type in (:behtyper) and beh1.behandling_status in (:avsluttet) group by beh1.fagsak_id ) " +
                "    on (fsmax=beh.fagsak_id and br.opprettet_tid = maxbr) " +
                " left outer join gr_personopplysning grpo on (beh.id=grpo.behandling_id and grpo.aktiv='J') " +
                " left outer join so_annen_part anpa on (grpo.so_annen_part_id=anpa.id and anpa.aktoer_id is not null) " +
                " left outer join br_resultat_behandling brr on (brr.behandling_id=beh.id and brr.aktiv='J') " +
                " left outer join (select BEREGNINGSRESULTAT_FP_ID brpid, min(BR_PERIODE_FOM) minbrfom from br_periode brp " +
                "         group by BEREGNINGSRESULTAT_FP_ID " +
                "      ) on brpid = brr.BG_BEREGNINGSRESULTAT_FP_ID " +
                " left outer join (select BEREGNINGSRESULTAT_FP_ID utbbrpid, min(BR_PERIODE_FOM) utbminbrfom from br_periode brp " +
                "         left join br_andel ba on ba.br_periode_id = brp.id " +
                "         where ba.dagsats > 0  " +
                "         group by BEREGNINGSRESULTAT_FP_ID " +
                "      ) on utbbrpid = brr.BG_BEREGNINGSRESULTAT_FP_ID " +
                " where beh.behandling_status in (:avsluttet) and beh.behandling_type in (:behtyper) " +
                " and fs.ytelse_type in (:foreldrepenger) and minbrfom is not null " +
                " and ( br.behandling_resultat_type in (:innvilgetyper) or utbminbrfom is not null ) " +
                " and fs.opprettet_tid >= :fomdato and fs.opprettet_tid < :tomdato "
        ); //$NON-NLS-1$
        query.setParameter("fomdato", fom); //$NON-NLS-1$
        query.setParameter("tomdato", tom.plusDays(1)); //$NON-NLS-1$
        query.setParameter("foreldrepenger", List.of(FagsakYtelseType.FORELDREPENGER.getKode(), FagsakYtelseType.SVANGERSKAPSPENGER.getKode())); //$NON-NLS-1$
        query.setParameter("restyper", List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.INGEN_ENDRING.getKode(),
            BehandlingResultatType.FORELDREPENGER_ENDRET.getKode(), BehandlingResultatType.AVSLÅTT.getKode(), BehandlingResultatType.OPPHØR.getKode())); //$NON-NLS-1$
        query.setParameter("innvilgetyper", INNVILGET_TYPER); //$NON-NLS-1$
        query.setParameter("avsluttet", avsluttendeStatus); //$NON-NLS-1$
        query.setParameter("behtyper", List.of(BehandlingType.FØRSTEGANGSSØKNAD.getKode(), BehandlingType.REVURDERING.getKode())); //$NON-NLS-1$
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return toOverlappData(resultatList);
    }

    private  List<OverlappData> toOverlappData(List<Object[]> resultatList) {
        List<OverlappData> returnList = new ArrayList<>();
        resultatList.forEach(resultat -> {
            OverlappData.OverlappDataBuilder builder = OverlappData.OverlappDataBuilder.ny()
                .medSaksnummer((String) resultat[0]) // NOSONAR
                .medYtelseType((String) resultat[1]) // NOSONAR
                .medAktørId((String) resultat[2])  // NOSONAR
                .medRolle((String) resultat[3])  // NOSONAR
                .medBehandlingId(((BigDecimal) resultat[4]).longValue()) // NOSONAR
                .medAktørIdAnnenPart((String) resultat[5])  // NOSONAR
                .medTidligsteDato(((Timestamp) resultat[6]).toLocalDateTime().toLocalDate()); // NOSONAR
            returnList.add(builder.build());
        });
        return returnList;
    }

    public List<OverlappData> finnSakerOpprettetInnenIntervallMedKunUtbetalte(LocalDate fom, LocalDate tom) {
        /*
         * Plukker saksnummer, siste ytelsebehandling, annenpart og første uttaksdato:
         *  - Saker der det finnes et beregnignsresultat/TY med utbetalt periode - inkusive noen få opphør fom etter tidligste periode
         *  - Første uttaksdato = tidligste periode, inkusive innvilget utsettelse.
         *  - Kan gi noen tilfelle med avslått første periode
         */
        List<String> avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).collect(Collectors.toList());
        Query query = entityManager.createNativeQuery(
            " select distinct saksnummer, ytelse_type, bru.aktoer_id braid, bruker_rolle, beh.id, null, minbrfom " +
                "from fagsak fs join bruker bru on fs.bruker_id = bru.id join behandling beh on fagsak_id=fs.id" +
                " join behandling_resultat br on (br.behandling_id=beh.id and br.behandling_resultat_type in (:restyper)) " +
                " join (select beh1.fagsak_id fsmax, max(br1.opprettet_tid) maxbr from behandling beh1 " +
                "      join behandling_resultat br1 on (br1.behandling_id=beh1.id and br1.behandling_resultat_type in (:restyper)) " +
                "      where beh1.behandling_type in (:behtyper) and beh1.behandling_status in (:avsluttet) group by beh1.fagsak_id ) " +
                "    on (fsmax=beh.fagsak_id and br.opprettet_tid = maxbr) " +
                " left outer join br_resultat_behandling brr on (brr.behandling_id=beh.id and brr.aktiv='J') " +
                " left outer join (select BEREGNINGSRESULTAT_FP_ID utbbrpid, min(BR_PERIODE_FOM) minbrfom from br_periode brp " +
                "         left join br_andel ba on ba.br_periode_id = brp.id " +
                "         where ba.dagsats > 0  " +
                "         group by BEREGNINGSRESULTAT_FP_ID " +
                "      ) on utbbrpid = brr.BG_BEREGNINGSRESULTAT_FP_ID " +
                " where beh.behandling_status in (:avsluttet) and beh.behandling_type in (:behtyper) " +
                " and fs.ytelse_type in (:foreldrepenger) and minbrfom is not null " +
                " and fs.opprettet_tid >= :fomdato and fs.opprettet_tid < :tomdato "
        ); //$NON-NLS-1$
        query.setParameter("fomdato", fom); //$NON-NLS-1$
        query.setParameter("tomdato", tom.plusDays(1)); //$NON-NLS-1$
        query.setParameter("foreldrepenger", List.of(FagsakYtelseType.FORELDREPENGER.getKode(), FagsakYtelseType.SVANGERSKAPSPENGER.getKode())); //$NON-NLS-1$
        query.setParameter("restyper", List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.INGEN_ENDRING.getKode(),
            BehandlingResultatType.FORELDREPENGER_ENDRET.getKode(), BehandlingResultatType.AVSLÅTT.getKode(), BehandlingResultatType.OPPHØR.getKode())); //$NON-NLS-1$
        query.setParameter("avsluttet", avsluttendeStatus); //$NON-NLS-1$
        query.setParameter("behtyper", List.of(BehandlingType.FØRSTEGANGSSØKNAD.getKode(), BehandlingType.REVURDERING.getKode())); //$NON-NLS-1$
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return toOverlappData(resultatList);
    }

}
