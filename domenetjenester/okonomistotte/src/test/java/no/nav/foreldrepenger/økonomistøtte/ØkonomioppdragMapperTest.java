package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsLinje150;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TkodeStatusLinje;
import no.nav.foreldrepenger.xmlutils.DateUtil;

public class ØkonomioppdragMapperTest {

    private static final String REFUNDERES_ID = "123456789";

    private static final String TYPE_ENHET = "BOS";
    private static final String ENHET = "8020";
    private static final LocalDate DATO_ENHET_FOM = LocalDate.of(1900, 1, 1);
    private static final String FRADRAG_TILLEGG = "T";
    private static final String BRUK_KJØREPLAN = "N";
    private static final String TYPE_GRAD = "UFOR";
    private static final String KODE_AKSJON = "1";
    private static final String UTBET_FREKVENS = "MND";

    private Oppdragskontroll oppdragskontroll;
    private ØkonomioppdragMapper økonomioppdragMapper;

    @BeforeEach
    public void setup() {
        oppdragskontroll = buildOppdragskontroll();
        økonomioppdragMapper = new ØkonomioppdragMapper();
    }

    @Test
    public void testMapVedtaksDataToOppdragES() {
        var oppdrag110 = opprettOppdrag110(oppdragskontroll, false);
        verifyMapVedtaksDataToOppdrag(oppdrag110, false, oppdragskontroll.getBehandlingId());
    }

    @Test
    public void testMapVedtaksDataToOppdragFP() {
        var oppdrag110 = opprettOppdrag110(oppdragskontroll, true);
        verifyMapVedtaksDataToOppdrag(oppdrag110, true, oppdragskontroll.getBehandlingId());
    }

    @Test
    public void testMapVedtaksDataToOppdragFPNårOpp150IkkeErSortert() {
        //Arrange
        var oppdrag110List = opprettOppdrag110(oppdragskontroll, true, false, false);
        List<Oppdrag> oppdragGenerertList = new ArrayList<>();

        //Act
        oppdrag110List.forEach(opp110 ->
            oppdragGenerertList.add(økonomioppdragMapper.mapVedtaksDataToOppdrag(opp110, oppdragskontroll.getBehandlingId())));

        //Assert
        for (var i = 0; i < oppdrag110List.size(); i++) {
            var oppdrag110Generert = oppdragGenerertList.get(i).getOppdrag110();
            var oppdrag110 = oppdrag110List.get(i);
            var delytelseIdFraOpp150GenerertList = oppdrag110Generert.getOppdragsLinje150()
                .stream()
                .map(OppdragsLinje150::getDelytelseId)
                .collect(Collectors.toList());
            var ikkeSortertDelytelseIdFraOpp150List = oppdrag110.getOppdragslinje150Liste()
                .stream()
                .map(Oppdragslinje150::getDelytelseId)
                .map(Object::toString)
                .collect(Collectors.toList());

            assertThat(delytelseIdFraOpp150GenerertList).isNotEqualTo(ikkeSortertDelytelseIdFraOpp150List);

            var sortertDelytelseIdFraOpp150List = ikkeSortertDelytelseIdFraOpp150List.stream()
                .sorted(Comparator.comparing(Long::parseLong))
                .collect(Collectors.toList());

            assertThat(delytelseIdFraOpp150GenerertList).isEqualTo(sortertDelytelseIdFraOpp150List);
        }
    }

    @Test
    public void generer_xml_for_oppdrag110_uten_kvittinger_ES() {
        opprettOppdrag110(oppdragskontroll, false);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).hasSize(1);
        assertThat(oppdragXmlListe.get(0)).isNotNull();
    }

    @Test
    public void generer_xml_for_oppdrag110_uten_kvittinger_FP() {
        opprettOppdrag110(oppdragskontroll, true);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).hasSize(2);
        assertThat(oppdragXmlListe.get(0)).isNotNull();
        assertThat(oppdragXmlListe.get(1)).isNotNull();
    }

    @Test
    public void ikke_generer_xml_for_oppdrag110_med_positiv_kvittering_ES() {
        opprettOppdrag110(oppdragskontroll, false);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    public void ikke_generer_xml_for_oppdrag110_med_negativ_kvittering_ES() {
        opprettOppdrag110(oppdragskontroll, false);
        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    public void ikke_generer_xml_for_oppdrag110_med_positiv_kvittering_FP() {
        opprettOppdrag110(oppdragskontroll, true);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    public void ikke_generer_xml_for_oppdrag110_med_negativ_kvittering_FP() {
        opprettOppdrag110(oppdragskontroll, true);
        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    public void mapperOmpostering116() {
        var oppdrag110List = opprettOppdrag110(oppdragskontroll, true, true, true);
        List<Oppdrag> oppdragGenerertList = new ArrayList<>();

        //Act
        oppdrag110List.forEach(opp110 ->
            oppdragGenerertList.add(økonomioppdragMapper.mapVedtaksDataToOppdrag(opp110, oppdragskontroll.getBehandlingId())));

        var oppdrag = oppdragGenerertList.stream().filter(o -> o.getOppdrag110().getKodeFagomraade().equals("FP")).findFirst();
        assertThat(oppdrag).isPresent();

        var ompostering116 = oppdrag.get().getOppdrag110().getOmpostering116();
        assertThat(ompostering116).isNotNull();
        assertThat(ompostering116.getOmPostering()).isEqualTo("J");
    }


    //    ---------------------------------------------

    private List<Oppdrag110> opprettOppdrag110(Oppdragskontroll oppdragskontroll, boolean gjelderFP) {
        return opprettOppdrag110(oppdragskontroll, gjelderFP, true, false);
    }


    private List<Oppdrag110> opprettOppdrag110(Oppdragskontroll oppdragskontroll, boolean gjelderFP, boolean erOppdragslinje150Sortert, boolean erOmpostering) {

        var oppdrag110Liste = buildOppdrag110(oppdragskontroll, gjelderFP, erOmpostering);
        var oppdragslinje150Liste = buildOppdragslinje150(oppdrag110Liste, gjelderFP, erOppdragslinje150Sortert);
        if (gjelderFP) {
            buildRefusjonsinfo156(oppdragslinje150Liste);
        }
        return oppdrag110Liste;
    }

    private void verifyMapVedtaksDataToOppdrag(List<Oppdrag110> oppdrag110Liste, boolean gjelderFP, Long behandlingId) {

        for (var oppdrag110 : oppdrag110Liste) {
            var oppdrag = økonomioppdragMapper.mapVedtaksDataToOppdrag(oppdrag110, behandlingId);

            var oppdrag110Generert = oppdrag.getOppdrag110();
            assertThat(oppdrag110Generert.getKodeAksjon()).isEqualTo(KODE_AKSJON);
            assertThat(oppdrag110Generert.getKodeEndring()).isEqualTo(oppdrag110.getKodeEndring().name());
            assertThat(oppdrag110Generert.getKodeFagomraade()).isEqualTo(oppdrag110.getKodeFagomrade().getKode());
            assertThat(oppdrag110Generert.getUtbetFrekvens()).isEqualTo(UTBET_FREKVENS);

            var avstemming115Generert = oppdrag110Generert.getAvstemming115();
            assertThat(avstemming115Generert.getKodeKomponent()).isEqualTo(ØkonomiKodekomponent.VLFP.getKode());
            assertThat(avstemming115Generert.getNokkelAvstemming()).isEqualTo(oppdrag110.getAvstemming().getNøkkel());
            assertThat(avstemming115Generert.getTidspktMelding()).isEqualTo(oppdrag110.getAvstemming().getTidspunkt());

            var oppdragsEnhet120Generert = oppdrag110Generert.getOppdragsEnhet120().get(0);

            assertThat(oppdragsEnhet120Generert.getTypeEnhet()).isEqualTo(TYPE_ENHET);
            assertThat(oppdragsEnhet120Generert.getEnhet()).isEqualTo(ENHET);
            assertThat(oppdragsEnhet120Generert.getDatoEnhetFom()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(DATO_ENHET_FOM));

            var oppdragsLinje150GenerertListe = oppdrag110Generert.getOppdragsLinje150();

            var ix = 0;
            for (var oppdragsLinje150Generert : oppdragsLinje150GenerertListe) {
                var oppdragslinje150 = oppdrag110.getOppdragslinje150Liste().get(ix);
                assertThat(oppdragsLinje150Generert.getKodeEndringLinje()).isEqualTo(oppdragslinje150.getKodeEndringLinje().name());
                assertThat(oppdragsLinje150Generert.getKodeStatusLinje()).isEqualTo(TkodeStatusLinje.fromValue(oppdragslinje150.getKodeStatusLinje().name()));
                assertThat(oppdragsLinje150Generert.getDatoStatusFom()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(oppdragslinje150.getDatoStatusFom()));
                assertThat(oppdragsLinje150Generert.getVedtakId()).isEqualTo(String.valueOf(oppdragslinje150.getVedtakId()));
                assertThat(oppdragsLinje150Generert.getDelytelseId()).isEqualTo(String.valueOf(oppdragslinje150.getDelytelseId()));
                assertThat(oppdragsLinje150Generert.getKodeKlassifik()).isEqualTo(oppdragslinje150.getKodeKlassifik().getKode());
                assertThat(oppdragsLinje150Generert.getDatoVedtakFom()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(oppdragslinje150.getDatoVedtakFom()));
                assertThat(oppdragsLinje150Generert.getDatoVedtakTom()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(oppdragslinje150.getDatoVedtakTom()));
                assertThat(oppdragsLinje150Generert.getSats()).isEqualTo(BigDecimal.valueOf(oppdragslinje150.getSats().getVerdi()));
                assertThat(oppdragsLinje150Generert.getFradragTillegg()).isEqualTo(TfradragTillegg.fromValue(FRADRAG_TILLEGG));
                assertThat(oppdragsLinje150Generert.getTypeSats()).isEqualTo(oppdragslinje150.getTypeSats().name());
                assertThat(oppdragsLinje150Generert.getBrukKjoreplan()).isEqualTo(BRUK_KJØREPLAN);
                assertThat(oppdragsLinje150Generert.getSaksbehId()).isEqualTo(oppdrag110.getSaksbehId());
                assertThat(oppdragsLinje150Generert.getUtbetalesTilId()).isEqualTo(oppdragslinje150.getUtbetalesTilId());
                assertThat(oppdragsLinje150Generert.getHenvisning()).isEqualTo(String.valueOf(oppdrag110.getOppdragskontroll().getBehandlingId()));
                if (!gjelderFP) {
                    assertThat(oppdragsLinje150Generert.getRefFagsystemId()).isNull();
                    assertThat(oppdragsLinje150Generert.getRefDelytelseId()).isNull();
                }

                var attestant180GenerertListe = oppdragsLinje150Generert.getAttestant180();
                for (var attestant180Generert : attestant180GenerertListe) {
                    assertThat(attestant180Generert.getAttestantId()).isEqualTo(oppdrag110.getSaksbehId());
                }

                if (gjelderFP) {
                    var grad170GenerertListe = oppdragsLinje150Generert.getGrad170();
                    for (var grad170Generert : grad170GenerertListe) {
                        var utbetalingsgrad = oppdragslinje150.getUtbetalingsgrad();
                        assertThat(grad170Generert.getGrad()).isEqualTo(BigInteger.valueOf(utbetalingsgrad.getVerdi()));
                        assertThat(grad170Generert.getTypeGrad()).isEqualTo(TYPE_GRAD);
                    }

                    var refusjonsinfo156Generert = oppdragsLinje150Generert.getRefusjonsinfo156();
                    var refusjonsinfo156Opt = Optional.ofNullable(oppdragslinje150.getRefusjonsinfo156());
                    refusjonsinfo156Opt.ifPresent(refusjonsinfo156 -> {
                        assertThat(refusjonsinfo156Generert.getRefunderesId()).isEqualTo(refusjonsinfo156.getRefunderesId());
                        assertThat(refusjonsinfo156Generert.getDatoFom()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(refusjonsinfo156.getDatoFom()));
                        assertThat(refusjonsinfo156Generert.getMaksDato()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(refusjonsinfo156.getMaksDato()));
                    });
                }
                ix++;
            }
        }
    }

    private List<Refusjonsinfo156> buildRefusjonsinfo156(List<Oppdragslinje150> oppdragslinje150Liste) {
        List<Refusjonsinfo156> refusjonsinfo156Liste = new ArrayList<>();
        var oppdragslinje150List = oppdragslinje150Liste.stream().
            filter(oppdragslinje150 -> oppdragslinje150.getOppdrag110().getKodeFagomrade().getKode().equals("FPREF")).collect(Collectors.toList());
        for (var opp150 : oppdragslinje150List) {
            refusjonsinfo156Liste.add(buildRefusjonsinfo156(opp150));
        }
        return refusjonsinfo156Liste;
    }

    private Refusjonsinfo156 buildRefusjonsinfo156(Oppdragslinje150 opp150) {
        return Refusjonsinfo156.builder()
            .medMaksDato(LocalDate.now())
            .medDatoFom(LocalDate.now())
            .medRefunderesId(REFUNDERES_ID)
            .medOppdragslinje150(opp150)
            .build();
    }

    private List<Oppdragslinje150> buildOppdragslinje150(List<Oppdrag110> oppdrag110Liste, boolean gjelderFP, boolean erOppdragslinje150Sortert) {

        var delytelseIdList = opprettDelytelseIdList(gjelderFP, erOppdragslinje150Sortert);
        List<Oppdragslinje150> oppdragslinje150Liste = new ArrayList<>();
        for (var oppdrag110 : oppdrag110Liste) {
            oppdragslinje150Liste.addAll(buildOppdragslinje150(oppdrag110, delytelseIdList, gjelderFP));
            if (gjelderFP) {
                oppdragslinje150Liste.add(buildOppdragslinje150Feriepenger(oppdrag110));
            }
        }
        return oppdragslinje150Liste;
    }

    private List<Long> opprettDelytelseIdList(boolean gjelderFP, boolean erOppdragslinje150Sortert) {
        if (!gjelderFP) {
            return Collections.singletonList(1L);
        }
        return erOppdragslinje150Sortert ? List.of(1L, 2L) : List.of(2L, 1L);
    }

    private List<Oppdragslinje150> buildOppdragslinje150(Oppdrag110 oppdrag110, List<Long> delytelseIdList, boolean gjelderFP) {
        List<Oppdragslinje150> opp150Liste = new ArrayList<>();
        for (var delytelseId : delytelseIdList) {
            var builder = settFellesFelterIOpp150(delytelseId);
            var kodeKlassifik = finnKodeKlassifikVerdi(oppdrag110);
            var oppdragslinje150 = builder
                .medKodeKlassifik(gjelderFP ? kodeKlassifik : KodeKlassifik.ES_FØDSEL)
                .medTypeSats(gjelderFP ? TypeSats.DAG : TypeSats.ENG)
                .medOppdrag110(oppdrag110)
                .build();
            opp150Liste.add(oppdragslinje150);
        }
        return opp150Liste;
    }

    private Oppdragslinje150 buildOppdragslinje150Feriepenger(Oppdrag110 oppdrag110) {
        var builder = settFellesFelterIOpp150(3L);
        var kodeKlassifik = oppdrag110.getKodeFagomrade().equals("FP") ? KodeKlassifik.FERIEPENGER_BRUKER : KodeKlassifik.FPF_FERIEPENGER_AG;
        return builder
            .medKodeKlassifik(kodeKlassifik)
            .medTypeSats(TypeSats.ENG)
            .medUtbetalingsgrad(Utbetalingsgrad._100)
            .medOppdrag110(oppdrag110)
            .build();
    }

    private KodeKlassifik finnKodeKlassifikVerdi(Oppdrag110 oppdrag110) {
        if (oppdrag110.getKodeFagomrade().equals("FP")) {
            return KodeKlassifik.FPF_ARBEIDSTAKER;
        }
        return KodeKlassifik.FPF_REFUSJON_AG;
    }

    private Oppdragslinje150.Builder settFellesFelterIOpp150(Long delytelseId) {
        return Oppdragslinje150.builder()
            .medKodeEndringLinje(KodeEndringLinje.ENDR)
            .medKodeStatusLinje(KodeStatusLinje.OPPH)
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("345")
            .medDelytelseId(delytelseId)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(Sats.på(1122L))
            .medUtbetalesTilId("123456789");
    }

    private List<Oppdrag110> buildOppdrag110(Oppdragskontroll oppdragskontroll, boolean gjelderFP, boolean erOmpostering) {

        List<Oppdrag110> oppdrag110Liste = new ArrayList<>();

        var oppdrag110_1 = Oppdrag110.builder()
            .medKodeEndring(KodeEndring.NY)
            .medKodeFagomrade(gjelderFP ? KodeFagområde.FORELDREPENGER_BRUKER : KodeFagområde.ENGANGSSTØNAD)
            .medFagSystemId(44L)
            .medOppdragGjelderId("12345678901")
            .medSaksbehId("J5624215")
            .medOppdragskontroll(oppdragskontroll)
            .medAvstemming(Avstemming.ny())
            .medOmpostering116(erOmpostering ? new Ompostering116.Builder()
                .medOmPostering(true)
                .medDatoOmposterFom(LocalDate.now())
                .medTidspktReg(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
                .build() : null)
            .build();

        oppdrag110Liste.add(oppdrag110_1);

        if (gjelderFP) {
            var oppdrag110_2 = Oppdrag110.builder()
                .medKodeEndring(KodeEndring.NY)
                .medKodeFagomrade(KodeFagområde.FORELDREPENGER_ARBEIDSGIVER)
                .medFagSystemId(55L)
                .medOppdragGjelderId("12345678901")
                .medSaksbehId("J5624215")
                .medAvstemming(Avstemming.ny())
                .medOppdragskontroll(oppdragskontroll)
                .build();

            oppdrag110Liste.add(oppdrag110_2);
            return oppdrag110Liste;
        }
        return oppdrag110Liste;
    }

    private Oppdragskontroll buildOppdragskontroll() {
        return Oppdragskontroll.builder()
            .medBehandlingId(154L)
            .medSaksnummer(new Saksnummer("35"))
            .medVenterKvittering(true)
            .medProsessTaskId(56L)
            .build();
    }
}
