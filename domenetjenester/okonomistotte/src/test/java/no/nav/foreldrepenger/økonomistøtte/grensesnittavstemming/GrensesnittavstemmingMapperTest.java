package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.AksjonType;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.AvstemmingType;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.Avstemmingsdata;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.KildeType;

class GrensesnittavstemmingMapperTest {

    private static final int MAKS_AVSTEMMING_MELDING_BYTES = 32000;

    private static final String MELDINGKODE = "Kode1234";

    private static final String BESKRIVENDE_MELDING = "Melding med lengde 70 tegn slik at vi tester maksimal lengde her......";

    private Oppdragskontroll.Builder oppdrkontrollBuilder;
    private Oppdrag110.Builder oppdr110Builder;
    private Oppdragslinje150.Builder oppdrLinje150Builder;
    private OppdragKvittering.Builder oppdragKvitteringBuilder;
    private GrensesnittavstemmingMapper grensesnittavstemmingMapper;
    private KodeFagområde kodeFagområde;

    private List<Oppdrag110> oppdragsliste;

    @BeforeEach
    void setUp() {
        oppdrkontrollBuilder = Oppdragskontroll.builder();
        oppdr110Builder = Oppdrag110.builder();
        oppdrLinje150Builder = Oppdragslinje150.builder();
        oppdragKvitteringBuilder = OppdragKvittering.builder();
        kodeFagområde = KodeFagområde.FPREF;
        var oppdragskontroll = opprettOppdrag(null, kodeFagområde);

        oppdragsliste = Collections.singletonList(oppdragskontroll.getOppdrag110Liste().get(0));
        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde);
    }

    private Oppdragskontroll opprettOppdrag(Alvorlighetsgrad status, KodeFagområde fagområde) {
        var oppdragskontroll = buildOppdragskontroll(status == null);
        var oppdrag110 = buildOppdrag110(oppdragskontroll, fagområde, LocalDateTime.now());
        buildOppdragslinje150(oppdrag110);

        if (status != null) {
            var oppdragKvittering = buildOppdragKvittering(oppdrag110);
            oppdragKvittering.setAlvorlighetsgrad(status);
            if (!Alvorlighetsgrad.OK.equals(status)) {
                oppdragKvittering.setBeskrMelding(BESKRIVENDE_MELDING);
                oppdragKvittering.setMeldingKode(MELDINGKODE);
            }
        }
        return oppdragskontroll;
    }

    @Test
    void testStartmeldingXML() {
        // Arrange
        // Act
        var melding = grensesnittavstemmingMapper.lagStartmelding();
        // Assert
        assertThat(melding)
            .isNotNull()
            .startsWith("<?xml");
        assertThat(melding.length()).isLessThan(MAKS_AVSTEMMING_MELDING_BYTES);
    }

    @Test
    void testDatameldingXML() {
        // Arrange
        // Act
        var meldinger = grensesnittavstemmingMapper.lagDatameldinger();
        // Assert
        assertThat(meldinger).hasSize(1);
        for (var melding : meldinger) {
            assertThat(melding)
                .isNotNull()
                .startsWith("<?xml");
            assertThat(melding.length()).isLessThan(MAKS_AVSTEMMING_MELDING_BYTES);
        }
    }

    private void setupForStoreDatamengder(KodeFagområde kodeFagområde) {
        oppdragsliste = new ArrayList<>();
        for (var gruppe = 0; gruppe < 60; gruppe++) {
            var oppdrag = opprettOppdrag(Alvorlighetsgrad.OK, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag(null, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag(Alvorlighetsgrad.FEIL, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag(Alvorlighetsgrad.OK_MED_MERKNAD, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());
        }
        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde);
    }

    private void setupForStørreDatamengder(KodeFagområde kodeFagområde) {

        oppdragsliste = new ArrayList<>();
        for (var gruppe = 0; gruppe < 560; gruppe++) {
            var oppdrag = opprettOppdrag(Alvorlighetsgrad.OK, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag(null, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag(Alvorlighetsgrad.FEIL, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag(Alvorlighetsgrad.OK_MED_MERKNAD, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());
        }
        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde);
    }

    private void opprettOppdragMedFlereOppdrag110ForForskjelligeFagområder() {
        oppdragsliste = new ArrayList<>();

        var oppdrag = opprettOppdrag(Alvorlighetsgrad.OK, KodeFagområde.FP);
        var oppdrag2 = opprettOppdrag(Alvorlighetsgrad.OK, kodeFagområde);
        var oppdrag3 = opprettOppdrag(Alvorlighetsgrad.OK, KodeFagområde.REFUTG);
        oppdrag.getOppdrag110Liste().add(oppdrag2.getOppdrag110Liste().get(0));
        oppdrag.getOppdrag110Liste().add(oppdrag3.getOppdrag110Liste().get(0));

        oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde);
    }

    @Test
    void testDatameldingXMLvedStoreDatamengder() {
        // Arrange
        setupForStoreDatamengder(KodeFagområde.FPREF);
        // Act
        var meldinger = grensesnittavstemmingMapper.lagDatameldinger();
        // Assert
        assertThat(meldinger).hasSize(3);
        for (var melding : meldinger) {
            assertThat(melding)
                .isNotNull()
                .startsWith("<?xml");
            assertThat(melding.length()).isLessThan(MAKS_AVSTEMMING_MELDING_BYTES);
        }
    }

    @Test
    void testSluttmeldingXML() {
        // Arrange
        // Act
        var melding = grensesnittavstemmingMapper.lagSluttmelding();
        // Assert
        assertThat(melding)
            .isNotNull()
            .startsWith("<?xml");
        assertThat(melding.length()).isLessThan(MAKS_AVSTEMMING_MELDING_BYTES);
    }

    @Test
    void testStartmeldingInnhold() {
        // Arrange
        // Act
        var avstemmingsdata = grensesnittavstemmingMapper.lagAvstemmingsdataFelles(AksjonType.START);
        // Assert
        sjekkAksjonsInnhold(avstemmingsdata, AksjonType.START, false, kodeFagområde);
    }

    @Test
    void testDatameldingInnhold() {
        // Arrange
        // Act
        var avstemmingsdataListe = grensesnittavstemmingMapper.lagAvstemmingsdataListe();
        // Assert
        assertThat(avstemmingsdataListe).hasSize(1);
        sjekkAksjonsInnhold(avstemmingsdataListe.get(0), AksjonType.DATA, true, kodeFagområde);
    }

    @Test
    void testSluttmeldingInnhold() {
        // Arrange
        // Act
        var avstemmingsdata = grensesnittavstemmingMapper.lagAvstemmingsdataFelles(AksjonType.AVSL);
        // Assert
        sjekkAksjonsInnhold(avstemmingsdata, AksjonType.AVSL, false, kodeFagområde);
    }

    @Test
    void testDatameldingVedStoreDatamengder() {
        // Arrange
        var kodeFagområde = KodeFagområde.REFUTG;
        setupForStoreDatamengder(kodeFagområde);
        // Act
        var avstemmingsdataListe = grensesnittavstemmingMapper.lagAvstemmingsdataListe();
        // Assert
        assertThat(avstemmingsdataListe).hasSize(3);
        sjekkAksjonsInnhold(avstemmingsdataListe.get(0), AksjonType.DATA, true, kodeFagområde);
        sjekkAksjonsInnhold(avstemmingsdataListe.get(1), AksjonType.DATA, false, kodeFagområde);
        sjekkAksjonsInnhold(avstemmingsdataListe.get(2), AksjonType.DATA, false, kodeFagområde);
    }

    @Test
    void testAtSisteDataHarInnslag() {
        // Arrange
        setupForStørreDatamengder(KodeFagområde.FP);
        // Act
        var avstemmingsdataListe = grensesnittavstemmingMapper.lagAvstemmingsdataListe();
        // Assert
        assertThat(avstemmingsdataListe.get(avstemmingsdataListe.size() - 1).getDetalj()).isNotEmpty();
    }

    @Test
    void testForFlereOppdrag110MedForskjelligeFagområder() {
        // Arrange
        opprettOppdragMedFlereOppdrag110ForForskjelligeFagområder();
        // Act
        var avstemmingsdataListe = grensesnittavstemmingMapper.lagAvstemmingsdataListe();
        // Assert
        assertThat(avstemmingsdataListe).hasSize(1);
        assertThat(avstemmingsdataListe.get(avstemmingsdataListe.size() - 1).getAksjon().getUnderkomponentKode()).isEqualTo(kodeFagområde.name());
    }

    @Test
    void testForFlereOppdrag110ForSammeOppdragskontroll() {
        //Arrange
        oppdragsliste = new ArrayList<>();
        var lavAvstemmingsDato = LocalDateTime.of(2018, 10, 25, 0, 0, 1);
        var mellomAvstemmingsDato = LocalDateTime.of(2018, 10, 25, 12, 10, 1);
        var høyestAvstemmingsDato = LocalDateTime.of(2018, 10, 25, 23, 3, 1);

        var oppdrag = buildOppdragskontroll(false);

        opprettOppdrag110MedAvsetmmingsDato(oppdrag, mellomAvstemmingsDato, kodeFagområde);
        var forventetTom = opprettOppdrag110MedAvsetmmingsDato(oppdrag, høyestAvstemmingsDato, kodeFagområde).getAvstemming();
        var forventetFom = opprettOppdrag110MedAvsetmmingsDato(oppdrag, lavAvstemmingsDato, kodeFagområde).getAvstemming();

        oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

        //Act
        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde);

        var avstemmingsdata = grensesnittavstemmingMapper.lagAvstemmingsdataListe();

        //Assert
        assertThat(avstemmingsdata)
            .isNotNull()
            .hasSize(1);
        sjekkAksjonsInnhold(forventetFom, forventetTom, avstemmingsdata.get(0), AksjonType.DATA, true, kodeFagområde);
    }

    private Oppdrag110 opprettOppdrag110MedAvsetmmingsDato(Oppdragskontroll oppdrag, LocalDateTime lavAvstemmingsDato, KodeFagområde kodeFagområde) {
        var oppdrag110 = buildOppdrag110(oppdrag, kodeFagområde, lavAvstemmingsDato);
        buildOppdragslinje150(oppdrag110);
        return oppdrag110;
    }

    private void sjekkAksjonsInnhold(Avstemming forvendetFom, Avstemming forvendetTom, Avstemmingsdata avstemmingsdata, AksjonType aksjonType, boolean første, KodeFagområde kodeFagområde) {
        sjekkAksjonsInnhold(avstemmingsdata, aksjonType, første, kodeFagområde);

        var aksjon = avstemmingsdata.getAksjon();
        assertThat(aksjon.getNokkelFom()).isEqualTo(forvendetFom.getNøkkel());
        assertThat(aksjon.getNokkelTom()).isEqualTo(forvendetTom.getNøkkel());
    }

    private void sjekkAksjonsInnhold(Avstemmingsdata avstemmingsdata, AksjonType aksjonType, boolean første, KodeFagområde kodeFagområde) {
        assertThat(avstemmingsdata).isNotNull();
        if (AksjonType.DATA.equals(aksjonType)) {
            assertThat(avstemmingsdata.getDetalj()).isNotEmpty();
            if (første) {
                assertThat(avstemmingsdata.getGrunnlag()).isNotNull();
                assertThat(avstemmingsdata.getPeriode()).isNotNull();
                assertThat(avstemmingsdata.getTotal()).isNotNull();
            } else {
                assertThat(avstemmingsdata.getGrunnlag()).isNull();
                assertThat(avstemmingsdata.getPeriode()).isNull();
                assertThat(avstemmingsdata.getTotal()).isNull();
            }
        } else {
            assertThat(avstemmingsdata.getDetalj()).isEmpty();
            assertThat(avstemmingsdata.getGrunnlag()).isNull();
            assertThat(avstemmingsdata.getPeriode()).isNull();
            assertThat(avstemmingsdata.getTotal()).isNull();
        }
        var aksjon = avstemmingsdata.getAksjon();
        assertThat(aksjon).isNotNull();
        assertThat(aksjon.getAksjonType()).isEqualTo(aksjonType);
        assertThat(aksjon.getAvleverendeAvstemmingId()).isNotNull();
        assertThat(aksjon.getAvleverendeKomponentKode()).isEqualTo(ØkonomiKodekomponent.VLFP.name());
        assertThat(aksjon.getAvstemmingType()).isEqualTo(AvstemmingType.GRSN);
        assertThat(aksjon.getBrukerId()).isEqualTo(GrensesnittavstemmingMapper.BRUKER_ID_FOR_VEDTAKSLØSNINGEN);
        assertThat(aksjon.getKildeType()).isEqualTo(KildeType.AVLEV);
        assertThat(aksjon.getMottakendeKomponentKode()).isEqualTo(ØkonomiKodekomponent.OS.name());
        assertThat(aksjon.getUnderkomponentKode()).isEqualTo(kodeFagområde.name());
    }

    //    ---------------------------------------------

    private Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110) {

        return oppdrLinje150Builder
            .medKodeEndringLinje(KodeEndringLinje.ENDR)
            .medKodeStatusLinje(KodeStatusLinje.OPPH)
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("345")
            .medDelytelseId(64L)
            .medKodeKlassifik(KodeKlassifik.ES_FØDSEL)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(Sats.på(61122L))
            .medTypeSats(TypeSats.ENG)
            .medUtbetalesTilId("123456789")
            .medOppdrag110(oppdrag110)
            .build();
    }

    private Oppdrag110 buildOppdrag110(Oppdragskontroll oppdragskontroll, KodeFagområde fagområde, LocalDateTime avstemmingsTidspunkt) {
        return oppdr110Builder
            .medKodeEndring(KodeEndring.NY)
            .medKodeFagomrade(fagområde)
            .medFagSystemId(44L)
            .medOppdragGjelderId("12345678901")
            .medSaksbehId("J5624215")
            .medAvstemming(Avstemming.fra(avstemmingsTidspunkt))
            .medOppdragskontroll(oppdragskontroll)
            .build();
    }

    private OppdragKvittering buildOppdragKvittering(Oppdrag110 oppdr110) {
        return oppdragKvitteringBuilder
            .medOppdrag110(oppdr110)
            .build();
    }

    private Oppdragskontroll buildOppdragskontroll(boolean venterKvittering) {
        return oppdrkontrollBuilder
            .medBehandlingId(15400L)
            .medSaksnummer(new Saksnummer("3500"))
            .medVenterKvittering(venterKvittering)
            .medProsessTaskId(560000L)
            .build();
    }
}
