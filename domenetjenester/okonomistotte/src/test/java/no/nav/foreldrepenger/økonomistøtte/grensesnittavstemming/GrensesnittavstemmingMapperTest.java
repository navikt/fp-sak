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
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.AksjonType;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.Aksjonsdata;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.AvstemmingType;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.Avstemmingsdata;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.KildeType;

public class GrensesnittavstemmingMapperTest {

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
        kodeFagområde = KodeFagområde.FORELDREPENGER_AG;
        Oppdragskontroll oppdragskontroll = opprettOppdrag(null, kodeFagområde);

        oppdragsliste = Collections.singletonList(oppdragskontroll.getOppdrag110Liste().get(0));
        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde.getKode());
    }

    private Oppdragskontroll opprettOppdrag(String status, KodeFagområde fagområde) {
        Oppdragskontroll oppdragskontroll = buildOppdragskontroll(status == null);
        Oppdrag110 oppdrag110 = buildOppdrag110(oppdragskontroll, fagområde, LocalDateTime.now());
        buildOppdragslinje150(oppdrag110);

        if (status != null) {
            OppdragKvittering oppdragKvittering = buildOppdragKvittering(oppdrag110);
            oppdragKvittering.setAlvorlighetsgrad(status);
            if (!"00".equals(status)) {
                oppdragKvittering.setBeskrMelding(BESKRIVENDE_MELDING);
                oppdragKvittering.setMeldingKode(MELDINGKODE);
            }
        }
        return oppdragskontroll;
    }

    @Test
    public void testStartmeldingXML() {
        // Arrange
        // Act
        String melding = grensesnittavstemmingMapper.lagStartmelding();
        // Assert
        assertThat(melding).isNotNull();
        assertThat(melding).startsWith("<?xml");
        assertThat(melding.length()).isLessThan(MAKS_AVSTEMMING_MELDING_BYTES);
    }

    @Test
    public void testDatameldingXML() {
        // Arrange
        // Act
        List<String> meldinger = grensesnittavstemmingMapper.lagDatameldinger();
        // Assert
        assertThat(meldinger).hasSize(1);
        for (String melding : meldinger) {
            assertThat(melding).isNotNull();
            assertThat(melding).startsWith("<?xml");
            assertThat(melding.length()).isLessThan(MAKS_AVSTEMMING_MELDING_BYTES);
        }
    }

    private void setupForStoreDatamengder(KodeFagområde kodeFagområde) {
        oppdragsliste = new ArrayList<>();
        for (int gruppe = 0; gruppe < 60; gruppe++) {
            Oppdragskontroll oppdrag = opprettOppdrag("00", kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag(null, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag("08", kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag("04", kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());
        }
        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde.getKode());
    }

    private void setupForStørreDatamengder(KodeFagområde kodeFagområde) {

        oppdragsliste = new ArrayList<>();
        for (int gruppe = 0; gruppe < 560; gruppe++) {
            Oppdragskontroll oppdrag = opprettOppdrag("00", kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag(null, kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag("08", kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

            oppdrag = opprettOppdrag("04", kodeFagområde);
            oppdragsliste.addAll(oppdrag.getOppdrag110Liste());
        }
        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde.getKode());
    }

    private void opprettOppdragMedFlereOppdrag110ForForskjelligeFagområder() {
        oppdragsliste = new ArrayList<>();

        Oppdragskontroll oppdrag = opprettOppdrag("00", KodeFagområde.FORELDREPENGER_BRUKER);
        Oppdragskontroll oppdrag2 = opprettOppdrag("00", kodeFagområde);
        Oppdragskontroll oppdrag3 = opprettOppdrag("00", KodeFagområde.ENGANGSSTØNAD);
        oppdrag.getOppdrag110Liste().add(oppdrag2.getOppdrag110Liste().get(0));
        oppdrag.getOppdrag110Liste().add(oppdrag3.getOppdrag110Liste().get(0));

        oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde.getKode());
    }

    @Test
    public void testDatameldingXMLvedStoreDatamengder() {
        // Arrange
        setupForStoreDatamengder(KodeFagområde.FORELDREPENGER_AG);
        // Act
        List<String> meldinger = grensesnittavstemmingMapper.lagDatameldinger();
        // Assert
        assertThat(meldinger).hasSize(3);
        for (String melding : meldinger) {
            assertThat(melding).isNotNull();
            assertThat(melding).startsWith("<?xml");
            assertThat(melding.length()).isLessThan(MAKS_AVSTEMMING_MELDING_BYTES);
        }
    }

    @Test
    public void testSluttmeldingXML() {
        // Arrange
        // Act
        String melding = grensesnittavstemmingMapper.lagSluttmelding();
        // Assert
        assertThat(melding).isNotNull();
        assertThat(melding).startsWith("<?xml");
        assertThat(melding.length()).isLessThan(MAKS_AVSTEMMING_MELDING_BYTES);
    }

    @Test
    public void testStartmeldingInnhold() {
        // Arrange
        // Act
        Avstemmingsdata avstemmingsdata = grensesnittavstemmingMapper.lagAvstemmingsdataFelles(AksjonType.START);
        // Assert
        sjekkAksjonsInnhold(avstemmingsdata, AksjonType.START, false, kodeFagområde);
    }

    @Test
    public void testDatameldingInnhold() {
        // Arrange
        // Act
        List<Avstemmingsdata> avstemmingsdataListe = grensesnittavstemmingMapper.lagAvstemmingsdataListe();
        // Assert
        assertThat(avstemmingsdataListe).hasSize(1);
        sjekkAksjonsInnhold(avstemmingsdataListe.get(0), AksjonType.DATA, true, kodeFagområde);
    }

    @Test
    public void testSluttmeldingInnhold() {
        // Arrange
        // Act
        Avstemmingsdata avstemmingsdata = grensesnittavstemmingMapper.lagAvstemmingsdataFelles(AksjonType.AVSL);
        // Assert
        sjekkAksjonsInnhold(avstemmingsdata, AksjonType.AVSL, false, kodeFagområde);
    }

    @Test
    public void testDatameldingVedStoreDatamengder() {
        // Arrange
        KodeFagområde kodeFagområde = KodeFagområde.ENGANGSSTØNAD;
        setupForStoreDatamengder(kodeFagområde);
        // Act
        List<Avstemmingsdata> avstemmingsdataListe = grensesnittavstemmingMapper.lagAvstemmingsdataListe();
        // Assert
        assertThat(avstemmingsdataListe).hasSize(3);
        sjekkAksjonsInnhold(avstemmingsdataListe.get(0), AksjonType.DATA, true, kodeFagområde);
        sjekkAksjonsInnhold(avstemmingsdataListe.get(1), AksjonType.DATA, false, kodeFagområde);
        sjekkAksjonsInnhold(avstemmingsdataListe.get(2), AksjonType.DATA, false, kodeFagområde);
    }

    @Test
    public void testAtSisteDataHarInnslag() {
        // Arrange
        setupForStørreDatamengder(KodeFagområde.FORELDREPENGER_BRUKER);
        // Act
        List<Avstemmingsdata> avstemmingsdataListe = grensesnittavstemmingMapper.lagAvstemmingsdataListe();
        // Assert
        assertThat(avstemmingsdataListe.get(avstemmingsdataListe.size() - 1).getDetalj()).isNotEmpty();
    }

    @Test
    public void testForFlereOppdrag110MedForskjelligeFagområder() {
        // Arrange
        opprettOppdragMedFlereOppdrag110ForForskjelligeFagområder();
        // Act
        List<Avstemmingsdata> avstemmingsdataListe = grensesnittavstemmingMapper.lagAvstemmingsdataListe();
        // Assert
        assertThat(avstemmingsdataListe).hasSize(1);
        assertThat(avstemmingsdataListe.get(avstemmingsdataListe.size() - 1).getAksjon().getUnderkomponentKode()).isEqualTo(kodeFagområde.getKode());
    }

    @Test
    public void testForFlereOppdrag110ForSammeOppdragskontroll() {
        //Arrange
        oppdragsliste = new ArrayList<>();
        LocalDateTime lavAvstemmingsDato = LocalDateTime.of(2018, 10, 25, 0, 0, 1);
        LocalDateTime mellomAvstemmingsDato = LocalDateTime.of(2018, 10, 25, 12, 10, 1);
        LocalDateTime høyestAvstemmingsDato = LocalDateTime.of(2018, 10, 25, 23, 3, 1);

        Oppdragskontroll oppdrag = buildOppdragskontroll(false);

        opprettOppdrag110MedAvsetmmingsDato(oppdrag, mellomAvstemmingsDato, kodeFagområde);
        Avstemming forventetTom = opprettOppdrag110MedAvsetmmingsDato(oppdrag, høyestAvstemmingsDato, kodeFagområde).getAvstemming();
        Avstemming forventetFom = opprettOppdrag110MedAvsetmmingsDato(oppdrag, lavAvstemmingsDato, kodeFagområde).getAvstemming();

        oppdragsliste.addAll(oppdrag.getOppdrag110Liste());

        //Act
        grensesnittavstemmingMapper = new GrensesnittavstemmingMapper(oppdragsliste, kodeFagområde.getKode());

        List<Avstemmingsdata> avstemmingsdata = grensesnittavstemmingMapper.lagAvstemmingsdataListe();

        //Assert
        assertThat(avstemmingsdata).isNotNull();
        assertThat(avstemmingsdata).hasSize(1);
        sjekkAksjonsInnhold(forventetFom, forventetTom, avstemmingsdata.get(0), AksjonType.DATA, true, kodeFagområde);
    }

    private Oppdrag110 opprettOppdrag110MedAvsetmmingsDato(Oppdragskontroll oppdrag, LocalDateTime lavAvstemmingsDato, KodeFagområde kodeFagområde) {
        Oppdrag110 oppdrag110 = buildOppdrag110(oppdrag, kodeFagområde, lavAvstemmingsDato);
        buildOppdragslinje150(oppdrag110);
        return oppdrag110;
    }

    private void sjekkAksjonsInnhold(Avstemming forvendetFom, Avstemming forvendetTom, Avstemmingsdata avstemmingsdata, AksjonType aksjonType, boolean første, KodeFagområde kodeFagområde) {
        sjekkAksjonsInnhold(avstemmingsdata, aksjonType, første, kodeFagområde);

        Aksjonsdata aksjon = avstemmingsdata.getAksjon();
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
        Aksjonsdata aksjon = avstemmingsdata.getAksjon();
        assertThat(aksjon).isNotNull();
        assertThat(aksjon.getAksjonType()).isEqualTo(aksjonType);
        assertThat(aksjon.getAvleverendeAvstemmingId()).isNotNull();
        assertThat(aksjon.getAvleverendeKomponentKode()).isEqualTo(ØkonomiKodekomponent.VLFP.getKode());
        assertThat(aksjon.getAvstemmingType()).isEqualTo(AvstemmingType.GRSN);
        assertThat(aksjon.getBrukerId()).isEqualTo(GrensesnittavstemmingMapper.BRUKER_ID_FOR_VEDTAKSLØSNINGEN);
        assertThat(aksjon.getKildeType()).isEqualTo(KildeType.AVLEV);
        assertThat(aksjon.getMottakendeKomponentKode()).isEqualTo(ØkonomiKodekomponent.OS.getKode());
        assertThat(aksjon.getUnderkomponentKode()).isEqualTo(kodeFagområde.getKode());
    }

    //    ---------------------------------------------

    private Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110) {

        return oppdrLinje150Builder
            .medKodeEndringLinje(KodeEndringLinje.ENDRING)
            .medKodeStatusLinje(KodeStatusLinje.OPPHØR)
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("345")
            .medDelytelseId(64L)
            .medKodeKlassifik(KodeKlassifik.ES_FØDSEL)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(Sats.på(61122L))
            .medTypeSats(TypeSats.ENGANG)
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
            .medBehandlingId(154L)
            .medSaksnummer(new Saksnummer("35"))
            .medVenterKvittering(venterKvittering)
            .medProsessTaskId(56L)
            .build();
    }
}
