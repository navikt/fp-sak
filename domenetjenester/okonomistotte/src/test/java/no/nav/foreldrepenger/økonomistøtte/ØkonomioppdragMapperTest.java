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
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Grad170;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsLinje150;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TkodeStatusLinje;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;

public class ØkonomioppdragMapperTest {

    private static final String KODE_KLASSIFIK_FODSEL = "FPENFOD-OP";
    private static final String TYPE_GRAD = "UFOR";
    private static final String REFUNDERES_ID = "123456789";

    private static final String TYPE_ENHET = "BOS";
    private static final String ENHET = "8020";
    private static final LocalDate DATO_ENHET_FOM = LocalDate.of(1900, 1, 1);

    private Oppdragskontroll oppdragskontroll;
    private ØkonomioppdragMapper økonomioppdragMapper;

    @BeforeEach
    public void setup() {
        oppdragskontroll = buildOppdragskontroll();
        økonomioppdragMapper = new ØkonomioppdragMapper(oppdragskontroll);
    }

    @Test
    public void testMapVedtaksDataToOppdragES() {
        List<Oppdrag110> oppdrag110 = opprettOppdrag110(oppdragskontroll, false);
        verifyMapVedtaksDataToOppdrag(oppdrag110, false);
    }

    @Test
    public void testMapVedtaksDataToOppdragFP() {
        List<Oppdrag110> oppdrag110 = opprettOppdrag110(oppdragskontroll, true);
        verifyMapVedtaksDataToOppdrag(oppdrag110, true);
    }

    @Test
    public void testMapVedtaksDataToOppdragFPNårOpp150IkkeErSortert() {
        //Arrange
        List<Oppdrag110> oppdrag110List = opprettOppdrag110(oppdragskontroll, true, false, false);
        List<no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag> oppdragGenerertList = new ArrayList<>();

        //Act
        oppdrag110List.forEach(opp110 ->
            oppdragGenerertList.add(økonomioppdragMapper.mapVedtaksDataToOppdrag(opp110)));

        //Assert
        for (int i = 0; i < oppdrag110List.size(); i++) {
            no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag110 oppdrag110Generert = oppdragGenerertList.get(i).getOppdrag110();
            Oppdrag110 oppdrag110 = oppdrag110List.get(i);
            List<String> delytelseIdFraOpp150GenerertList = oppdrag110Generert.getOppdragsLinje150()
                .stream()
                .map(OppdragsLinje150::getDelytelseId)
                .collect(Collectors.toList());
            List<String> ikkeSortertDelytelseIdFraOpp150List = oppdrag110.getOppdragslinje150Liste()
                .stream()
                .map(Oppdragslinje150::getDelytelseId)
                .map(Object::toString)
                .collect(Collectors.toList());

            assertThat(delytelseIdFraOpp150GenerertList).isNotEqualTo(ikkeSortertDelytelseIdFraOpp150List);

            List<String> sortertDelytelseIdFraOpp150List = ikkeSortertDelytelseIdFraOpp150List.stream()
                .sorted(Comparator.comparing(Long::parseLong))
                .collect(Collectors.toList());

            assertThat(delytelseIdFraOpp150GenerertList).isEqualTo(sortertDelytelseIdFraOpp150List);
        }
    }

    @Test
    public void generer_xml_for_oppdrag110_uten_kvittinger_ES() {
        opprettOppdrag110(oppdragskontroll, false);
        List<String> oppdragXmlListe = økonomioppdragMapper.generateOppdragXML();
        assertThat(oppdragXmlListe).hasSize(1);
        assertThat(oppdragXmlListe.get(0)).isNotNull();
    }

    @Test
    public void generer_xml_for_oppdrag110_uten_kvittinger_FP() {
        opprettOppdrag110(oppdragskontroll, true);
        List<String> oppdragXmlListe = økonomioppdragMapper.generateOppdragXML();
        assertThat(oppdragXmlListe).hasSize(2);
        assertThat(oppdragXmlListe.get(0)).isNotNull();
        assertThat(oppdragXmlListe.get(1)).isNotNull();
    }

    @Test
    public void ikke_generer_xml_for_oppdrag110_med_positiv_kvittering_ES() {
        opprettOppdrag110(oppdragskontroll, false);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        List<String> oppdragXmlListe = økonomioppdragMapper.generateOppdragXML();
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    public void ikke_generer_xml_for_oppdrag110_med_negativ_kvittering_ES() {
        opprettOppdrag110(oppdragskontroll, false);
        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);
        List<String> oppdragXmlListe = økonomioppdragMapper.generateOppdragXML();
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    public void ikke_generer_xml_for_oppdrag110_med_positiv_kvittering_FP() {
        opprettOppdrag110(oppdragskontroll, true);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        List<String> oppdragXmlListe = økonomioppdragMapper.generateOppdragXML();
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    public void ikke_generer_xml_for_oppdrag110_med_negativ_kvittering_FP() {
        opprettOppdrag110(oppdragskontroll, true);
        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);
        List<String> oppdragXmlListe = økonomioppdragMapper.generateOppdragXML();
        assertThat(oppdragXmlListe).isEmpty();
    }

    @Test
    public void mapperOmpostering116() {
        List<Oppdrag110> oppdrag110List = opprettOppdrag110(oppdragskontroll, true, true, true);
        List<no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag> oppdragGenerertList = new ArrayList<>();

        //Act
        oppdrag110List.forEach(opp110 ->
            oppdragGenerertList.add(økonomioppdragMapper.mapVedtaksDataToOppdrag(opp110)));

        Optional<Oppdrag> oppdrag = oppdragGenerertList.stream().filter(o -> o.getOppdrag110().getKodeFagomraade().equals("FP")).findFirst();
        assertThat(oppdrag).isPresent();

        no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Ompostering116 ompostering116 = oppdrag.get().getOppdrag110().getOmpostering116();
        assertThat(ompostering116).isNotNull();
        assertThat(ompostering116.getOmPostering()).isEqualTo("J");
    }


    //    ---------------------------------------------

    private List<Oppdrag110> opprettOppdrag110(Oppdragskontroll oppdragskontroll, boolean gjelderFP) {
        return opprettOppdrag110(oppdragskontroll, gjelderFP, true, false);
    }


    private List<Oppdrag110> opprettOppdrag110(Oppdragskontroll oppdragskontroll, boolean gjelderFP, boolean erOppdragslinje150Sortert, boolean erOmpostering) {

        List<Oppdrag110> oppdrag110Liste = buildOppdrag110(oppdragskontroll, gjelderFP, erOmpostering);
        List<Oppdragslinje150> oppdragslinje150Liste = buildOppdragslinje150(oppdrag110Liste, gjelderFP, erOppdragslinje150Sortert);
        if (gjelderFP) {
            buildGrad170(oppdragslinje150Liste);
            buildRefusjonsinfo156(oppdragslinje150Liste);
        }
        return oppdrag110Liste;
    }

    private void verifyMapVedtaksDataToOppdrag(List<Oppdrag110> oppdrag110Liste, boolean gjelderFP) {

        for (Oppdrag110 oppdrag110 : oppdrag110Liste) {
            Oppdrag oppdrag = økonomioppdragMapper.mapVedtaksDataToOppdrag(oppdrag110);

            no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag110 oppdrag110Generert = oppdrag.getOppdrag110();
            assertThat(oppdrag110Generert.getKodeAksjon()).isEqualTo(oppdrag110.getKodeAksjon());
            assertThat(oppdrag110Generert.getKodeEndring()).isEqualTo(oppdrag110.getKodeEndring());
            assertThat(oppdrag110Generert.getKodeFagomraade()).isEqualTo(oppdrag110.getKodeFagomrade()); //
            assertThat(oppdrag110Generert.getUtbetFrekvens()).isEqualTo(oppdrag110.getUtbetFrekvens());

            no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Avstemming115 avstemming115Generert = oppdrag110Generert.getAvstemming115();
            assertThat(avstemming115Generert.getKodeKomponent()).isEqualTo(oppdrag110.getAvstemming().getKodekomponent());
            assertThat(avstemming115Generert.getNokkelAvstemming()).isEqualTo(oppdrag110.getAvstemming().getNøkkel());
            assertThat(avstemming115Generert.getTidspktMelding()).isEqualTo(oppdrag110.getAvstemming().getTidspunkt());

            no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsEnhet120 oppdragsEnhet120Generert = oppdrag110Generert.getOppdragsEnhet120().get(0);

            assertThat(oppdragsEnhet120Generert.getTypeEnhet()).isEqualTo(TYPE_ENHET);
            assertThat(oppdragsEnhet120Generert.getEnhet()).isEqualTo(ENHET);
            assertThat(oppdragsEnhet120Generert.getDatoEnhetFom()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(DATO_ENHET_FOM));

            List<no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsLinje150> oppdragsLinje150GenerertListe = oppdrag110Generert.getOppdragsLinje150();

            int ix = 0;
            for (OppdragsLinje150 oppdragsLinje150Generert : oppdragsLinje150GenerertListe) {
                Oppdragslinje150 oppdragslinje150 = oppdrag110.getOppdragslinje150Liste().get(ix);
                assertThat(oppdragsLinje150Generert.getKodeEndringLinje()).isEqualTo(oppdragslinje150.getKodeEndringLinje());
                assertThat(oppdragsLinje150Generert.getKodeStatusLinje()).isEqualTo(TkodeStatusLinje.fromValue(oppdragslinje150.getKodeStatusLinje()));
                assertThat(oppdragsLinje150Generert.getDatoStatusFom()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(oppdragslinje150.getDatoStatusFom()));
                assertThat(oppdragsLinje150Generert.getVedtakId()).isEqualTo(String.valueOf(oppdragslinje150.getVedtakId()));
                assertThat(oppdragsLinje150Generert.getDelytelseId()).isEqualTo(String.valueOf(oppdragslinje150.getDelytelseId()));
                assertThat(oppdragsLinje150Generert.getKodeKlassifik()).isEqualTo(oppdragslinje150.getKodeKlassifik());
                assertThat(oppdragsLinje150Generert.getDatoVedtakFom()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(oppdragslinje150.getDatoVedtakFom()));
                assertThat(oppdragsLinje150Generert.getDatoVedtakTom()).isEqualTo(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(oppdragslinje150.getDatoVedtakTom()));
                assertThat(oppdragsLinje150Generert.getSats()).isEqualTo(new BigDecimal(oppdragslinje150.getSats()));
                assertThat(oppdragsLinje150Generert.getFradragTillegg()).isEqualTo(TfradragTillegg.fromValue(oppdragslinje150.getFradragTillegg()));
                assertThat(oppdragsLinje150Generert.getTypeSats()).isEqualTo(oppdragslinje150.getTypeSats());
                assertThat(oppdragsLinje150Generert.getBrukKjoreplan()).isEqualTo(oppdragslinje150.getBrukKjoreplan());
                assertThat(oppdragsLinje150Generert.getSaksbehId()).isEqualTo(oppdragslinje150.getSaksbehId());
                assertThat(oppdragsLinje150Generert.getUtbetalesTilId()).isEqualTo(oppdragslinje150.getUtbetalesTilId());
                assertThat(oppdragsLinje150Generert.getHenvisning()).isEqualTo(String.valueOf(oppdragslinje150.getHenvisning()));
                if (!gjelderFP) {
                    assertThat(oppdragsLinje150Generert.getRefFagsystemId()).isNull();
                    assertThat(oppdragsLinje150Generert.getRefDelytelseId()).isNull();
                }

                List<no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Attestant180> attestant180GenerertListe = oppdragsLinje150Generert.getAttestant180();
                for (no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Attestant180 attestant180Generert : attestant180GenerertListe) {
                    assertThat(attestant180Generert.getAttestantId()).isEqualTo(oppdrag110.getSaksbehId());
                }

                if (gjelderFP) {
                    List<no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Grad170> grad170GenerertListe = oppdragsLinje150Generert.getGrad170();
                    for (no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Grad170 grad170Generert : grad170GenerertListe) {
                        Grad170 grad170 = oppdragslinje150.getGrad170Liste().get(0);
                        assertThat(grad170Generert.getGrad()).isEqualTo(BigInteger.valueOf(grad170.getGrad()));
                        assertThat(grad170Generert.getTypeGrad()).isEqualTo(grad170.getTypeGrad());
                    }

                    no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Refusjonsinfo156 refusjonsinfo156Generert = oppdragsLinje150Generert.getRefusjonsinfo156();
                    Optional<Refusjonsinfo156> refusjonsinfo156Opt = Optional.ofNullable(oppdragslinje150.getRefusjonsinfo156());
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


    private List<Grad170> buildGrad170(List<Oppdragslinje150> oppdragslinje150Liste) {
        List<Grad170> grad170Liste = new ArrayList<>();
        for (Oppdragslinje150 oppdragslinje150 : oppdragslinje150Liste) {
            grad170Liste.add(buildGrad170(oppdragslinje150));
        }
        return grad170Liste;
    }

    private Grad170 buildGrad170(Oppdragslinje150 oppdragslinje150) {
        return Grad170.builder()
            .medGrad(100)
            .medTypeGrad(TYPE_GRAD)
            .medOppdragslinje150(oppdragslinje150)
            .build();
    }

    private List<Refusjonsinfo156> buildRefusjonsinfo156(List<Oppdragslinje150> oppdragslinje150Liste) {
        List<Refusjonsinfo156> refusjonsinfo156Liste = new ArrayList<>();
        List<Oppdragslinje150> oppdragslinje150List = oppdragslinje150Liste.stream().
            filter(oppdragslinje150 -> oppdragslinje150.getOppdrag110().getKodeFagomrade().equals("FPREF")).collect(Collectors.toList());
        for (Oppdragslinje150 opp150 : oppdragslinje150List) {
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

        List<Long> delytelseIdList = opprettDelytelseIdList(gjelderFP, erOppdragslinje150Sortert);
        List<Oppdragslinje150> oppdragslinje150Liste = new ArrayList<>();
        for (Oppdrag110 oppdrag110 : oppdrag110Liste) {
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
        for (Long delytelseId : delytelseIdList) {
            Oppdragslinje150.Builder builder = settFellesFelterIOpp150(delytelseId);
            String kodeKlassifik = finnKodeKlassifikVerdi(oppdrag110);
            Oppdragslinje150 oppdragslinje150 = builder
                .medKodeKlassifik(gjelderFP ? kodeKlassifik : KODE_KLASSIFIK_FODSEL)
                .medTypeSats(gjelderFP ? ØkonomiTypeSats.DAG.name() : ØkonomiTypeSats.UKE.name())
                .medOppdrag110(oppdrag110)
                .build();
            opp150Liste.add(oppdragslinje150);
        }
        return opp150Liste;
    }

    private Oppdragslinje150 buildOppdragslinje150Feriepenger(Oppdrag110 oppdrag110) {
        Oppdragslinje150.Builder builder = settFellesFelterIOpp150(3L);
        String kodeKlassifik = oppdrag110.getKodeFagomrade().equals("FP") ? ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik() : ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik();
        return builder
            .medKodeKlassifik(kodeKlassifik)
            .medTypeSats(ØkonomiTypeSats.ENG.name())
            .medOppdrag110(oppdrag110)
            .build();
    }

    private String finnKodeKlassifikVerdi(Oppdrag110 oppdrag110) {
        if (oppdrag110.getKodeFagomrade().equals("FP")) {
            return ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik();
        }
        return ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik();
    }

    private Oppdragslinje150.Builder settFellesFelterIOpp150(Long delytelseId) {
        return Oppdragslinje150.builder()
            .medKodeEndringLinje("ENDR")
            .medKodeStatusLinje("OPPH")
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("345")
            .medDelytelseId(delytelseId)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(1122L)
            .medFradragTillegg(TfradragTillegg.F.value())
            .medBrukKjoreplan("B")
            .medSaksbehId("F2365245")
            .medUtbetalesTilId("123456789")
            .medHenvisning(43L);
    }

    private List<Oppdrag110> buildOppdrag110(Oppdragskontroll oppdragskontroll, boolean gjelderFP, boolean erOmpostering) {

        List<Oppdrag110> oppdrag110Liste = new ArrayList<>();

        Oppdrag110 oppdrag110_1 = Oppdrag110.builder()
            .medKodeAksjon(ØkonomiKodeAksjon.TRE.getKodeAksjon())
            .medKodeEndring(ØkonomiKodeEndring.NY.name())
            .medKodeFagomrade(gjelderFP ? "FP" : "REFUTG")
            .medFagSystemId(44L)
            .medUtbetFrekvens(ØkonomiUtbetFrekvens.DAG.getUtbetFrekvens())
            .medOppdragGjelderId("12345678901")
            .medDatoOppdragGjelderFom(LocalDate.of(2000, 1, 1))
            .medSaksbehId("J5624215")
            .medOppdragskontroll(oppdragskontroll)
            .medAvstemming(Avstemming.ny())
            .medOmpostering116(erOmpostering ? new Ompostering116.Builder().medOmPostering("J")
                .medDatoOmposterFom(LocalDate.now())
                .medSaksbehId("J5624215")
                .medTidspktReg(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
                .build() : null)
            .build();

        oppdrag110Liste.add(oppdrag110_1);

        if (gjelderFP) {
            Oppdrag110 oppdrag110_2 = Oppdrag110.builder()
                .medKodeAksjon(ØkonomiKodeAksjon.TRE.getKodeAksjon())
                .medKodeEndring(ØkonomiKodeEndring.NY.name())
                .medKodeFagomrade("FPREF")
                .medFagSystemId(55L)
                .medUtbetFrekvens(ØkonomiUtbetFrekvens.DAG.getUtbetFrekvens())
                .medOppdragGjelderId("12345678901")
                .medDatoOppdragGjelderFom(LocalDate.of(2000, 1, 1))
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
