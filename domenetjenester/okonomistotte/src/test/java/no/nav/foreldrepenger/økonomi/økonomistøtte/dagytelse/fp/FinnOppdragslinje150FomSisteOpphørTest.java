package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør.FinnOppdragslinje150FomSisteOpphør;

public class FinnOppdragslinje150FomSisteOpphørTest {

    private static final String NY = ØkonomiKodeEndringLinje.NY.name();
    private static final String ENDR = ØkonomiKodeEndringLinje.ENDR.name();
    private static final String OPPH = ØkonomiKodeStatusLinje.OPPH.name();

    /**
     * Første oppdrag (delytelseId=1): Status NY
     * Andre oppdrag (delytelseId=1): Status ENDR (opphør)
     */
    @Test
    public void finn_oppdragslinje150_fom_siste_opphør_når_siste_er_opphør() {
        // Arrange
        Oppdragslinje150 første = opprettOppdragslinje150(1L, NY);
        //Opphør på forrige innvilgelse, så delytelseId skal være lik
        Oppdragslinje150 andre = opprettOppdragslinje150(1L, ENDR);
        List<Oppdragslinje150> oppdragslinje150List = List.of(første, andre);

        //Act
        List<Oppdragslinje150> filtrertOppdragslinje150List = FinnOppdragslinje150FomSisteOpphør.finnOppdragslinje150FomSisteOpphør(oppdragslinje150List);

        //Assert
        assertThat(filtrertOppdragslinje150List).hasSize(1);
        assertThat(filtrertOppdragslinje150List.get(0).gjelderOpphør()).isTrue();
    }

    /**
     * Første oppdrag (delytelseId=1): Status ENDR (opphør)
     * Andre oppdrag (delytelseId=2): Status NY
     */
    @Test
    public void finn_oppdragslinje150_fom_siste_opphør_når_siste_er_innvilget() {
        // Arrange
        Oppdragslinje150 første = opprettOppdragslinje150(1L, ENDR);
        Oppdragslinje150 andre = opprettOppdragslinje150(2L, NY);
        List<Oppdragslinje150> oppdragslinje150List = List.of(første, andre);

        //Act
        List<Oppdragslinje150> filtrertOppdragslinje150List = FinnOppdragslinje150FomSisteOpphør.finnOppdragslinje150FomSisteOpphør(oppdragslinje150List);

        //Assert
        assertThat(filtrertOppdragslinje150List).hasSize(2);
        assertThat(filtrertOppdragslinje150List.get(0).gjelderOpphør()).isTrue();
        assertThat(filtrertOppdragslinje150List.get(1).gjelderOpphør()).isFalse();
    }

    /**
     * Første oppdrag (delytelseId=1): Status ENDR (opphør)
     * Andre oppdrag (delytelseId=1): Status ENDR (opphør)
     * Tredje oppdrag (delytelseId=2): Status NY
     */
    @Test
    public void finn_oppdragslinje150_fom_siste_opphør_når_det_finnes_flere_opph_med_samme_delytelseId() {
        // Arrange
        Oppdragslinje150 første = opprettOppdragslinje150(1L, ENDR, "2018-08-01");
        //Opphør på forrige opphør, så delytelseId skal være lik
        Oppdragslinje150 andre = opprettOppdragslinje150(1L, ENDR, "2018-08-02");
        Oppdragslinje150 tredje = opprettOppdragslinje150(2L, NY);
        List<Oppdragslinje150> oppdragslinje150List = List.of(første, andre, tredje);

        //Act
        List<Oppdragslinje150> filtrertOppdragslinje150List = FinnOppdragslinje150FomSisteOpphør.finnOppdragslinje150FomSisteOpphør(oppdragslinje150List);

        //Assert
        assertThat(filtrertOppdragslinje150List).hasSize(2);
        assertThat(filtrertOppdragslinje150List.get(0).gjelderOpphør()).isTrue();
        assertThat(filtrertOppdragslinje150List.get(0).getVedtakId()).isEqualTo("2018-08-02");
        assertThat(filtrertOppdragslinje150List.get(1).gjelderOpphør()).isFalse();
        assertThat(filtrertOppdragslinje150List.get(1).getDelytelseId()).isEqualByComparingTo(2L);
    }

    /**
     * Første oppdrag (delytelseId=1): Status NY
     * Andre oppdrag (delytelseId=1): Status ENDR (opphør)
     * Tredje oppdrag (delytelseId=2): Status NY
     */
    @Test
    public void finn_oppdragslinje150_fom_siste_opphør_når_det_finnes_innvilget_opp150_før_og_etter_opph_for_opph() {
        // Arrange
        Oppdragslinje150 første = opprettOppdragslinje150(1L, NY);
        //Opphør på forrige innvilgelse, så delytelseId skal være lik
        Oppdragslinje150 andre = opprettOppdragslinje150(1L, ENDR);
        Oppdragslinje150 tredje = opprettOppdragslinje150(2L, NY);
        Oppdragslinje150 fjerde = opprettOppdragslinje150(3L, NY);
        List<Oppdragslinje150> oppdragslinje150List = List.of(første, andre, tredje, fjerde);

        //Act
        List<Oppdragslinje150> filtrertOppdragslinje150List = FinnOppdragslinje150FomSisteOpphør.finnOppdragslinje150FomSisteOpphør(oppdragslinje150List);

        //Assert
        assertThat(filtrertOppdragslinje150List).hasSize(3);
        assertThat(filtrertOppdragslinje150List.get(0).gjelderOpphør()).isTrue();
        assertThat(filtrertOppdragslinje150List.get(1).gjelderOpphør()).isFalse();
        assertThat(filtrertOppdragslinje150List.get(2).gjelderOpphør()).isFalse();
        assertThat(filtrertOppdragslinje150List.get(1).getDelytelseId()).isEqualByComparingTo(2L);
        assertThat(filtrertOppdragslinje150List.get(2).getDelytelseId()).isEqualByComparingTo(3L);
    }

    /**
     * Første oppdrag (delytelseId=1): Status ENDR (opphør)
     * Andre oppdrag (delytelseId=1): Status ENDR (opphør)
     */
    @Test
    public void finn_oppdragslinje150_fom_siste_opphør_når_det_kun_opphør_med_samme_delytelseId() {
        // Arrange
        Oppdragslinje150 første = opprettOppdragslinje150(1L, ENDR, "2018-01-01");
        //Opphør på forrige opphør, så delytelseId skal være lik
        Oppdragslinje150 andre = opprettOppdragslinje150(1L, ENDR, "2018-01-02");
        List<Oppdragslinje150> oppdragslinje150List = List.of(første, andre);

        //Act
        List<Oppdragslinje150> filtrertOppdragslinje150List = FinnOppdragslinje150FomSisteOpphør.finnOppdragslinje150FomSisteOpphør(oppdragslinje150List);

        //Assert
        assertThat(filtrertOppdragslinje150List).hasSize(1);
        assertThat(filtrertOppdragslinje150List.get(0).gjelderOpphør()).isTrue();
        assertThat(filtrertOppdragslinje150List.get(0).getDelytelseId()).isEqualByComparingTo(1L);
        assertThat(filtrertOppdragslinje150List.get(0).getVedtakId()).isEqualTo("2018-01-02");
    }

    private Oppdragslinje150 opprettOppdragslinje150(Long delytelseId, String økonomiKodeEndringLinje) {
        return opprettOppdragslinje150(delytelseId, økonomiKodeEndringLinje, "2018-08-01");
    }

    private Oppdragslinje150 opprettOppdragslinje150(Long delytelseId, String økonomiKodeEndringLinje, String vedtakId) {
        Oppdrag110 oppdrag110 = opprettOppdrag110();

        Oppdragslinje150.Builder builder = Oppdragslinje150.builder()
            .medDelytelseId(delytelseId)
            .medKodeEndringLinje(økonomiKodeEndringLinje)
            .medVedtakId(vedtakId)
            .medKodeKlassifik(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik())
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now().plusDays(15))
            .medSats(2000L)
            .medFradragTillegg(TfradragTillegg.T.name())
            .medTypeSats(ØkonomiTypeSats.DAG.name())
            .medBrukKjoreplan("N")
            .medSaksbehId("12345")
            .medHenvisning(123L)
            .medOppdrag110(oppdrag110);
        if (ENDR.equals(økonomiKodeEndringLinje)) {
            builder.medKodeStatusLinje(OPPH);
            builder.medKodeEndringLinje(ENDR);
        } else {
            builder.medKodeEndringLinje(NY);
        }
        return builder.build();
    }

    private Oppdrag110 opprettOppdrag110() {
        Oppdragskontroll oppdragskontroll = opprettOppdragskontroll();
        Avstemming115 avstemming115 = opprettAvstemming115();
        return Oppdrag110.builder()
            .medAvstemming115(avstemming115)
            .medKodeAksjon("kodeAksjon")
            .medKodeEndring("kodeEndring")
            .medKodeFagomrade("kode")
            .medFagSystemId(123L)
            .medUtbetFrekvens("utbetfrekvens")
            .medOppdragGjelderId("oppdraggjelderid")
            .medDatoOppdragGjelderFom(LocalDate.now())
            .medSaksbehId("saksbehid")
            .medOppdragskontroll(oppdragskontroll)
            .build();
    }

    private Oppdragskontroll opprettOppdragskontroll() {
        return Oppdragskontroll.builder()
            .medVenterKvittering(true)
            .medSaksnummer(new Saksnummer("1234"))
            .medBehandlingId(123L)
            .medProsessTaskId(12L)
            .build();
    }

    private Avstemming115 opprettAvstemming115() {
        return Avstemming115.builder()
            .medKodekomponent("kode")
            .medNokkelAvstemming("nøkkel")
            .medTidspnktMelding("tidspunkt")
            .build();
    }

}
