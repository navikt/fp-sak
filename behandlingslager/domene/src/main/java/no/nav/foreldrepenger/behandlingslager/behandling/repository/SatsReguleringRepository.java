package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class SatsReguleringRepository {

    private static final String AVSLUTTET_KEY = "avsluttet";
    private static final String FOMDATO_KEY = "fomdato";
    private static final String RESTYPER_KEY = "restyper";
    private static final String STONAD_KEY = "stonad";
    private static final String YTELSE_BEH_KEY = "ytelsebeh";

    private static final List<String> VRES_TYPER_REGULERES = List.of(VedtakResultatType.INNVILGET.getKode(), VedtakResultatType.OPPHØR.getKode());
    private static final List<String> VRES_TYPER_SJEKK = List.of(VedtakResultatType.INNVILGET.getKode(), VedtakResultatType.OPPHØR.getKode(),
        VedtakResultatType.AVSLAG.getKode());
    private static final List<String> STATUS_FERDIG = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).toList();
    private static final List<String> YTELSE_TYPER = BehandlingType.getYtelseBehandlingTyper().stream().map(BehandlingType::getKode).toList();

    private EntityManager entityManager;

    public record FagsakIdSaksnummer(Long fagsakId, Saksnummer saksnummer) { }

    SatsReguleringRepository() {
    }

    @Inject
    public SatsReguleringRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    EntityManager getEntityManager() {
        return entityManager;
    }

    /*
     * Tabeller og joins
     * - Velger siste vedtatte behandling for fagsk - basert på med seneste vedtakstidspunkt
     * - Finner tidligste uttaksdato for sist vedtatte behandling
     * - Finner sats med periode som er brukt i beregningen
     * Where
     * - Ytelse Foreldrepenger
     * - Første utbetalingsdato er etter tom-dato for grunnbeløpet som er brukt
     * - Det er en sak med kjent reguleringsbehov
     * - Det ikke finnes noen åpne behandlinger (utenom berørt) for saken
     */
    private static final String REGULERING_FP = """
        SELECT DISTINCT f.id , f.saksnummer
          from Fagsak f
          join behandling b on b.fagsak_id=f.id
          join behandling_resultat br on br.behandling_id=b.id
          join behandling_vedtak bv on br.id = bv.behandling_resultat_id
          join (select beh.fagsak_id fsmax, max(bvsq.vedtak_dato) maxbv from behandling beh
                join fagsak f on (beh.fagsak_id = f.id and ytelse_type = :stonad)
                join behandling_resultat brsq on brsq.behandling_id=beh.id
                join behandling_vedtak bvsq on brsq.id = bvsq.behandling_resultat_id
                where beh.behandling_type in (:ytelsebeh) and beh.behandling_status in (:avsluttet) and bvsq.vedtak_resultat_type in (:restyper)
                group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.vedtak_dato = maxbv)
          join (select ur.behandling_resultat_id bruttak, min(per.fom) uttakfom
            from uttak_resultat_periode per
            join uttak_resultat ur on per.uttak_resultat_perioder_id = nvl(ur.overstyrt_perioder_id,ur.opprinnelig_perioder_id)
            where ur.aktiv = 'J'
            and (per.PERIODE_RESULTAT_AARSAK = :sokfrist or per.PERIODE_RESULTAT_TYPE = :utinnvilg)
            group by ur.behandling_resultat_id) futtak on (futtak.bruttak = br.id)
          join BG_EKSTERN_KOBLING beko on beko.behandling_id = b.id
          join br_sats sats on (sats.verdi = beko.grunnbeloep and sats_type=:grunnbelop)
        where b.behandling_status in (:avsluttet) and b.behandling_type in (:ytelsebeh) and bv.vedtak_resultat_type in (:restyper)
          and futtak.uttakfom > sats.tom
          and futtak.uttakfom >= :fomdato
          and f.ytelse_type = :stonad
          and beko.reguleringsbehov = 'J'
          and f.id not in ( select beh.fagsak_id from behandling beh
            where beh.behandling_status not in (:avsluttet) and beh.behandling_type in (:ytelsebeh)
              and beh.id not in (select ba.behandling_id from behandling_arsak ba where behandling_arsak_type in (:berort)) )
        """;

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering over 6G og det ikke finnes åpen behandling */
    public List<FagsakIdSaksnummer> finnFpSakerMedBehovForGrunnbeløpReguleringKobling(LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, saksnummer fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, med reguleringsbehov og uttak starter etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         */
        var query = getEntityManager().createNativeQuery(REGULERING_FP)
            .setParameter(RESTYPER_KEY, VRES_TYPER_REGULERES)
            .setParameter(AVSLUTTET_KEY, STATUS_FERDIG)
            .setParameter(FOMDATO_KEY, gjeldendeFom)
            .setParameter(YTELSE_BEH_KEY, YTELSE_TYPER)
            .setParameter(STONAD_KEY, FagsakYtelseType.FORELDREPENGER.getKode())
            .setParameter("berort", BehandlingÅrsakType.alleTekniskeÅrsaker().stream().map(BehandlingÅrsakType::getKode).toList())
            .setParameter("grunnbelop", BeregningSatsType.GRUNNBELØP.getKode())
            .setParameter("sokfrist", PeriodeResultatÅrsak.SØKNADSFRIST.getKode())
            .setParameter("utinnvilg", PeriodeResultatType.INNVILGET.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdSaksnummer(Long.parseLong(row[0].toString()), new Saksnummer((String) row[1]))).toList();
    }

    /*
     * Tabeller og joins
     * - Velger siste vedtatte behandling for fagsk - basert på med seneste vedtakstidspunkt
     * - Finner tidligste utbetalingsdato for sist vedtatte behandling
     * - Finner sats med periode som er brukt i beregningen
     * Where
     * - Ytelse Svangerskapspenger
     * - Første utbetalingsdato er etter tom-dato for grunnbeløpet som er brukt
     * - Det er en sak med kjent reguleringsbehov
     * - Det ikke finnes noen åpne behandlinger for saken
     */
    private static final String REGULERING_SVP = """
        SELECT DISTINCT f.id , f.saksnummer
          from Fagsak f
          join behandling b on b.fagsak_id=f.id
          join behandling_resultat br on br.behandling_id=b.id
          join behandling_vedtak bv on br.id = bv.behandling_resultat_id
          join (select beh.fagsak_id fsmax, max(bvsq.vedtak_dato) maxbv from behandling beh
                join fagsak f on (beh.fagsak_id = f.id and ytelse_type = :stonad)
                join behandling_resultat brsq on brsq.behandling_id=beh.id
                join behandling_vedtak bvsq on brsq.id = bvsq.behandling_resultat_id
                where beh.behandling_type in (:ytelsebeh) and beh.behandling_status in (:avsluttet) and bvsq.vedtak_resultat_type in (:restyper)
                group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.vedtak_dato = maxbv)
          join BR_RESULTAT_BEHANDLING grbr on (grbr.behandling_id=b.id and grbr.aktiv = 'J')
          join (select BEREGNINGSRESULTAT_FP_ID brfpid, min(BR_PERIODE_FOM) fom
                from br_periode brp left join br_andel bra on bra.br_periode_id = brp.id
                where (dagsats>0) group by BEREGNINGSRESULTAT_FP_ID
               ) brnetto on brnetto.brfpid=grbr.BG_BEREGNINGSRESULTAT_FP_ID
          join BG_EKSTERN_KOBLING beko on beko.behandling_id = b.id
          join br_sats sats on (sats.verdi = beko.grunnbeloep and sats_type=:grunnbelop)
        where b.behandling_status in (:avsluttet) and b.behandling_type in (:ytelsebeh) and bv.vedtak_resultat_type in (:restyper)
          and brnetto.fom > sats.tom
          and brnetto.fom >= :fomdato
          and f.ytelse_type = :stonad
          and beko.reguleringsbehov = 'J'
          and f.id not in ( select beh.fagsak_id from behandling beh
            where beh.behandling_status not in (:avsluttet) and beh.behandling_type in (:ytelsebeh) )
        """;

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering over 6G og det ikke finnes åpen behandling */
    public List<FagsakIdSaksnummer> finnSvpSakerMedBehovForGrunnbeløpReguleringKobling(LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, med et reguleringsbehov og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         */
        var query = getEntityManager().createNativeQuery(REGULERING_SVP)
            .setParameter(RESTYPER_KEY, VRES_TYPER_REGULERES)
            .setParameter(AVSLUTTET_KEY, STATUS_FERDIG)
            .setParameter(FOMDATO_KEY, gjeldendeFom)
            .setParameter(YTELSE_BEH_KEY, YTELSE_TYPER)
            .setParameter(STONAD_KEY, FagsakYtelseType.SVANGERSKAPSPENGER.getKode())
            .setParameter("grunnbelop", BeregningSatsType.GRUNNBELØP.getKode());

        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdSaksnummer(Long.parseLong(row[0].toString()), new Saksnummer((String) row[1]))).toList();
    }

    private static final String REGULERING_SELECT_STD_ES = """
        SELECT DISTINCT f.id , f.saksnummer
          from fagsak f
          join behandling b on b.fagsak_id=f.id
          join behandling_resultat br on br.behandling_id=b.id
          join behandling_vedtak bv on br.id = bv.behandling_resultat_id
          join (select beh.fagsak_id fsmax, max(bvsq.vedtak_dato) maxbv from behandling beh
                join fagsak f on (beh.fagsak_id = f.id and ytelse_type = :stonad)
                join behandling_resultat brsq on brsq.behandling_id=beh.id
                join behandling_vedtak bvsq on brsq.id = bvsq.behandling_resultat_id
                where beh.behandling_type in (:ytelsebeh) and beh.behandling_status in (:avsluttet) and bvsq.vedtak_resultat_type in (:restyper)
                group by beh.fagsak_id) on (fsmax=b.fagsak_id and bv.vedtak_dato = maxbv)
        join gr_familie_hendelse gf on (gf.behandling_id = b.id and gf.aktiv='J')
        join fh_familie_hendelse fh on fh.id=nvl(gf.overstyrt_familie_hendelse_id, gf.bekreftet_familie_hendelse_id)
        join BR_LEGACY_ES_BEREGNING esbr on esbr.beregning_resultat_id = br.beregning_resultat_id
        join br_sats sats on (sats.fom = :fomdato and sats_type=:engang)
        where b.behandling_status in (:avsluttet) and b.behandling_type in (:ytelsebeh) and bv.vedtak_resultat_type in (:innvilget)
          and f.ytelse_type = :stonad
          and esbr.sats_verdi <> sats.verdi
          and (
             exists (select * from fh_uidentifisert_barn ub where ub.familie_hendelse_id = fh.id and ub.foedsel_dato >= :fomdato)
            or
             exists (select * from fh_adopsjon ad where ad.familie_hendelse_id = fh.id and ad.omsorgsovertakelse_dato >= :fomdato)
            or
             exists (select * from FH_TERMINBEKREFTELSE tb where tb.familie_hendelse_id = fh.id and tb.termindato > :maxtermindato and tb.termindato < :idag)
            or
             ( exists (select * from FH_TERMINBEKREFTELSE tb where tb.familie_hendelse_id = fh.id and tb.termindato > :mintermindato)
               and
              exists (select * from etterkontroll ek where ek.fagsak_id = f.id and behandlet='J' and kontroll_tid > :ekdato) )
          )
          and not exists ( select * from behandling beh where beh.fagsak_id = f.id
                           and beh.behandling_status not in (:avsluttet) and beh.behandling_type in (:ytelsebeh))
        """;


    /** Liste av fagsakId, aktørId for saker som potensielt trenger regulering pga endret sats */
    public List<FagsakIdSaksnummer> finnSakerMedBehovForEngangsstønadRegulering(LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har siste avsluttet behandling med gammel sats, brutto overstiger 6G og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen åpne ytelsesbehandlinger (berørt telles ikke)
         */
        var query = getEntityManager().createNativeQuery(REGULERING_SELECT_STD_ES)
            .setParameter(AVSLUTTET_KEY, STATUS_FERDIG)
            .setParameter(FOMDATO_KEY, gjeldendeFom)
            .setParameter(RESTYPER_KEY, VRES_TYPER_SJEKK)
            .setParameter(STONAD_KEY, FagsakYtelseType.ENGANGSTØNAD.getKode())
            .setParameter(YTELSE_BEH_KEY, YTELSE_TYPER)
            .setParameter("innvilget", VRES_TYPER_REGULERES)
            .setParameter("mintermindato", gjeldendeFom.minusMonths(1)) // Tar med fødsel inntil 4 uker etter termin
            .setParameter("maxtermindato", gjeldendeFom.plusWeeks(18)) // Innvilget fram i tid - kan søke i uke 22 - legg på 18 uker
            .setParameter("idag", LocalDate.now().plusDays(1))
            .setParameter("ekdato", gjeldendeFom.plusMonths(1))
            .setParameter("engang", BeregningSatsType.ENGANG.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new FagsakIdSaksnummer(Long.parseLong(row[0].toString()), new Saksnummer((String) row[1]))).toList();
    }

    public int setReguleringsbehovBehandlingerDagpengerAAP() {
        var antall = entityManager.createNativeQuery("""
            MERGE INTO BG_EKSTERN_KOBLING a
            USING (
               SELECT distinct grbg.behandling_id
               from GR_BEREGNINGSGRUNNLAG grbg
               JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in ('AAP', 'DP') )
               where grbg.aktiv = 'J' and exists (select * from BG_EKSTERN_KOBLING beko where beko.behandling_id = grbg.behandling_id)
                ) b
             ON ( a.behandling_id = b.behandling_id )
             WHEN MATCHED THEN
                UPDATE SET a.reguleringsbehov = 'J'
            """)
            .executeUpdate();
        entityManager.flush();

        return antall;
    }

}
