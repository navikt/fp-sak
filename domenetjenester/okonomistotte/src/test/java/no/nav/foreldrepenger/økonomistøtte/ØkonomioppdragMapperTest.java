package no.nav.foreldrepenger.økonomistøtte;

import static no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper.lagOppdrag110;
import static no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper.lagOppdragslinje150;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsLinje150;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TkodeStatusLinje;
import no.nav.foreldrepenger.xmlutils.DateUtil;

class ØkonomioppdragMapperTest {

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
    void setup() {
        oppdragskontroll = OppdragTestDataHelper.oppdragskontrollUtenOppdrag();
        økonomioppdragMapper = new ØkonomioppdragMapper();
    }

    @Test
    void testMapVedtaksDataToOppdragES() {
        var oppdrag110 = OppdragTestDataHelper.lagOppdrag110ES(oppdragskontroll, 1L);
        verifyMapVedtaksDataToOppdrag(List.of(oppdrag110), false, oppdragskontroll.getBehandlingId());
    }

    @Test
    void testMapVedtaksDataToOppdragFP() {
        var oppdrag110 = lagOppdrag110(oppdragskontroll, 1L, KodeFagområde.FPREF, true, true, true);
        verifyMapVedtaksDataToOppdrag(List.of(oppdrag110), true, oppdragskontroll.getBehandlingId());
    }

    @Test
    void testMapVedtaksDataToOppdragFPNårOpp150IkkeErSortert() {
        //Arrange
        var oppdrag110_1 = lagOppdrag110(oppdragskontroll, 1L, KodeFagområde.FP, true, true, false);
        var oppdrag110List = List.of(oppdrag110_1);
        lagOppdragslinje150(oppdrag110_1, 1L, true); // Inneholder en oppdragslinje150 med delytelsid 62L fra før av.
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
                .toList();
            var ikkeSortertDelytelseIdFraOpp150List = oppdrag110.getOppdragslinje150Liste()
                .stream()
                .map(Oppdragslinje150::getDelytelseId)
                .map(Object::toString)
                .toList();

            assertThat(delytelseIdFraOpp150GenerertList).isNotEqualTo(ikkeSortertDelytelseIdFraOpp150List);

            var sortertDelytelseIdFraOpp150List = ikkeSortertDelytelseIdFraOpp150List.stream()
                .sorted(Comparator.comparing(Long::parseLong))
                .toList();

            assertThat(delytelseIdFraOpp150GenerertList).isEqualTo(sortertDelytelseIdFraOpp150List);
        }
    }

    @Test
    void generer_xml_for_oppdrag110_uten_kvittinger_ES() {
        lagOppdrag110(oppdragskontroll, 1L, KodeFagområde.REFUTG, true, true, false);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).hasSize(1);
        assertThat(oppdragXmlListe.get(0)).isNotNull();
    }

    @Test
    void generer_xml_for_oppdrag110_uten_kvittinger_FP() {
        lagOppdrag110(oppdragskontroll, 1L, KodeFagområde.FP, true, true, true);
        lagOppdrag110(oppdragskontroll, 2L, KodeFagområde.FP, true, true, true);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).hasSize(2);
        assertThat(oppdragXmlListe.get(0)).isNotNull();
        assertThat(oppdragXmlListe.get(1)).isNotNull();
    }

    @Test
    void ikke_generer_xml_for_oppdrag110_med_positiv_kvittering_ES() {
        lagOppdrag110(oppdragskontroll, 1L, KodeFagområde.REFUTG, true, true, false);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    void ikke_generer_xml_for_oppdrag110_med_negativ_kvittering_ES() {
        lagOppdrag110(oppdragskontroll, 1L, KodeFagområde.REFUTG, true, true, false);
        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    void ikke_generer_xml_for_oppdrag110_med_positiv_kvittering_FP() {
        lagOppdrag110(oppdragskontroll, 1L, KodeFagområde.FP, true, true, true);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    void ikke_generer_xml_for_oppdrag110_med_negativ_kvittering_FP() {
        lagOppdrag110(oppdragskontroll, 1L, KodeFagområde.FP, true, true, true);
        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);
        var oppdragXmlListe = økonomioppdragMapper.generateOppdragXML(oppdragskontroll);
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    void mapperOmpostering116() {
        OppdragTestDataHelper.lagOppdrag110(oppdragskontroll, 2L, KodeFagområde.FP, true, true, true, true);
        OppdragTestDataHelper.lagOppdrag110(oppdragskontroll, 1L, KodeFagområde.FP, true, true, true, true);
        List<Oppdrag> oppdragGenerertList = new ArrayList<>();

        //Act
        oppdragskontroll.getOppdrag110Liste().forEach(opp110 ->
            oppdragGenerertList.add(økonomioppdragMapper.mapVedtaksDataToOppdrag(opp110, oppdragskontroll.getBehandlingId())));

        var oppdrag = oppdragGenerertList.stream().filter(o -> o.getOppdrag110().getKodeFagomraade().equals("FP")).findFirst();
        assertThat(oppdrag).isPresent();

        var ompostering116 = oppdrag.get().getOppdrag110().getOmpostering116();
        assertThat(ompostering116).isNotNull();
        assertThat(ompostering116.getOmPostering()).isEqualTo("J");
    }


    //    ---------------------------------------------
    private void verifyMapVedtaksDataToOppdrag(List<Oppdrag110> oppdrag110Liste, boolean gjelderFP, Long behandlingId) {

        for (var oppdrag110 : oppdrag110Liste) {
            var oppdrag = økonomioppdragMapper.mapVedtaksDataToOppdrag(oppdrag110, behandlingId);

            var oppdrag110Generert = oppdrag.getOppdrag110();
            assertThat(oppdrag110Generert.getKodeAksjon()).isEqualTo(KODE_AKSJON);
            assertThat(oppdrag110Generert.getKodeEndring()).isEqualTo(oppdrag110.getKodeEndring().name());
            assertThat(oppdrag110Generert.getKodeFagomraade()).isEqualTo(oppdrag110.getKodeFagomrade().name());
            assertThat(oppdrag110Generert.getUtbetFrekvens()).isEqualTo(UTBET_FREKVENS);

            var avstemming115Generert = oppdrag110Generert.getAvstemming115();
            assertThat(avstemming115Generert.getKodeKomponent()).isEqualTo(ØkonomiKodekomponent.VLFP.name());
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
}
