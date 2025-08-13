package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;

class TilretteleggingOversetterTest {
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private TilretteleggingOversetter tilretteleggingOversetter;

    @BeforeEach
    void setUp() {
        tilretteleggingOversetter = new TilretteleggingOversetter(svangerskapspengerRepository, virksomhetTjeneste, personinfoAdapter );
    }

    @Test
    void sjekker_at_ferie_kopieres_riktig_når_eksisterende_ferie_er_før_nyeste_tilrettelegging_fom() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var førsteFraDato = LocalDate.now().plusWeeks(2);
        var ferieFom = førsteFraDato.plusWeeks(1).plusDays(2);
        var ferieTom = ferieFom.plusDays(5);
        var nyesteFraDato = førsteFraDato.plusWeeks(4);
        var oppholdFørsteSøknad = List.of(lagOpphold(ferieFom, ferieTom, SvpOppholdÅrsak.FERIE));
        var nyFerieFom = nyesteFraDato.plusDays(2);
        var nyFerieTom = nyesteFraDato.plusDays(4);
        var nyttOpphold = List.of(lagOpphold(nyFerieFom, nyFerieTom, SvpOppholdÅrsak.FERIE));

        var eksisterendeTilrettelegging = lagTilrettelegging(arbeidsgiver, førsteFraDato,  BigDecimal.valueOf(50), oppholdFørsteSøknad);
        var nyTilrettelegging = lagTilrettelegging(arbeidsgiver, nyesteFraDato, BigDecimal.valueOf(40), nyttOpphold);

        var resultat = tilretteleggingOversetter.oppdaterEksisterendeTlrMedNyeFomsOgOpphold(nyTilrettelegging, eksisterendeTilrettelegging);

        var oppholdEtterKopiering = resultat.getAvklarteOpphold().stream().toList();
        assertThat(oppholdEtterKopiering).hasSize(2);
        assertThat(oppholdEtterKopiering.getFirst().getFom()).isEqualTo(ferieFom);
        assertThat(oppholdEtterKopiering.getFirst().getTom()).isEqualTo(ferieTom);
        assertThat(oppholdEtterKopiering.getFirst().getKilde()).isEqualTo(SvpOppholdKilde.TIDLIGERE_VEDTAK);
        assertThat(oppholdEtterKopiering.get(1).getFom()).isEqualTo(nyFerieFom);
        assertThat(oppholdEtterKopiering.get(1).getTom()).isEqualTo(nyFerieTom);
        assertThat(oppholdEtterKopiering.get(1).getKilde()).isEqualTo(SvpOppholdKilde.SØKNAD);
    }

    @Test
    void sjekker_at_ferie_kopieres_riktig_når_eksisterende_ferie_er_etter_nyeste_tilrettelegging_fom() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var førsteFraDato = LocalDate.now().plusWeeks(2);
        var ferieFom = førsteFraDato.plusWeeks(1).plusDays(2);
        var ferieTom = ferieFom.plusDays(5);

        var nyesteFraDato = ferieFom.minusDays(1);
        var oppholdFørsteSøknad = List.of(lagOpphold(ferieFom, ferieTom, SvpOppholdÅrsak.FERIE));

        var nyFerieFom = nyesteFraDato.plusDays(6);
        var nyFerieTom = nyesteFraDato.plusDays(8);
        var nyttOpphold = List.of(lagOpphold(nyFerieFom, nyFerieTom, SvpOppholdÅrsak.FERIE));

        var eksisterendeTilrettelegging = lagTilrettelegging(arbeidsgiver, førsteFraDato, BigDecimal.valueOf(50), oppholdFørsteSøknad);
        var nyTilrettelegging = lagTilrettelegging(arbeidsgiver, nyesteFraDato, BigDecimal.valueOf(40), nyttOpphold);

        var resultat = tilretteleggingOversetter.oppdaterEksisterendeTlrMedNyeFomsOgOpphold(nyTilrettelegging, eksisterendeTilrettelegging);

        var oppholdEtterKopiering = resultat.getAvklarteOpphold().stream().toList();
        assertThat(oppholdEtterKopiering).hasSize(1);
        assertThat(oppholdEtterKopiering.getFirst().getFom()).isEqualTo(nyFerieFom);
        assertThat(oppholdEtterKopiering.getFirst().getTom()).isEqualTo(nyFerieTom);
        assertThat(oppholdEtterKopiering.getFirst().getKilde()).isEqualTo(SvpOppholdKilde.SØKNAD);
    }

    @Test
    void sjekker_at_ferie_kopieres_riktig_når_eksisterende_ferie_og_ny_er_like_og_etter_nyeste_tilrettelegging_fom() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var førsteFraDato = LocalDate.now().plusWeeks(2);
        var ferieFom = førsteFraDato.plusWeeks(1).plusDays(2);
        var ferieTom = ferieFom.plusDays(5);

        var nyesteFraDato = ferieFom.minusDays(1);
        var oppholdFørsteSøknad = List.of(lagOpphold(ferieFom, ferieTom, SvpOppholdÅrsak.FERIE));

        var eksisterendeTilrettelegging = lagTilrettelegging(arbeidsgiver, førsteFraDato,  BigDecimal.valueOf(50), oppholdFørsteSøknad);
        var nyTilrettelegging = lagTilrettelegging(arbeidsgiver, nyesteFraDato, BigDecimal.valueOf(40), oppholdFørsteSøknad);

        var resultat = tilretteleggingOversetter.oppdaterEksisterendeTlrMedNyeFomsOgOpphold(nyTilrettelegging, eksisterendeTilrettelegging);

        var oppholdEtterKopiering = resultat.getAvklarteOpphold().stream().toList();
        assertThat(oppholdEtterKopiering).hasSize(1);
        assertThat(oppholdEtterKopiering.getFirst().getFom()).isEqualTo(ferieFom);
        assertThat(oppholdEtterKopiering.getFirst().getTom()).isEqualTo(ferieTom);
        assertThat(oppholdEtterKopiering.getFirst().getKilde()).isEqualTo(SvpOppholdKilde.SØKNAD);
    }

    @Test
    void sjekker_at_ferie_kopieres_riktig_når_eksisterende_ferie_og_ny_er_like_og_før_nyeste_tilrettelegging_fom() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var førsteFraDato = LocalDate.now().plusWeeks(2);
        var ferieFom = førsteFraDato.plusWeeks(1).plusDays(2);
        var ferieTom = ferieFom.plusDays(5);

        var nyesteFraDato = ferieFom.plusDays(1);
        var oppholdFørsteSøknad = List.of(lagOpphold(ferieFom, ferieTom, SvpOppholdÅrsak.FERIE));

        var eksisterendeTilrettelegging = lagTilrettelegging(arbeidsgiver, førsteFraDato,  BigDecimal.valueOf(50), oppholdFørsteSøknad);
        var nyTilrettelegging = lagTilrettelegging(arbeidsgiver, nyesteFraDato, BigDecimal.valueOf(40), oppholdFørsteSøknad);

        var resultat = tilretteleggingOversetter.oppdaterEksisterendeTlrMedNyeFomsOgOpphold(nyTilrettelegging, eksisterendeTilrettelegging);

        var oppholdEtterKopiering = resultat.getAvklarteOpphold().stream().toList();
        assertThat(oppholdEtterKopiering).hasSize(1);
        assertThat(oppholdEtterKopiering.getFirst().getFom()).isEqualTo(ferieFom);
        assertThat(oppholdEtterKopiering.getFirst().getTom()).isEqualTo(ferieTom);
        assertThat(oppholdEtterKopiering.getFirst().getKilde()).isEqualTo(SvpOppholdKilde.SØKNAD);
    }

    @Test
    void sjekker_at_ferie_kopieres_riktig_når_eksisterende_ferie_og_ny_er_like_men_ulik_årsak_og_før_nyeste_tilrettelegging_fom() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var førsteFraDato = LocalDate.now().plusWeeks(2);
        var ferieFom = førsteFraDato.plusWeeks(1).plusDays(2);
        var ferieTom = ferieFom.plusDays(5);

        var nyesteFraDato = ferieFom.plusDays(1);
        var oppholdFørsteSøknad = List.of(lagOpphold(ferieFom, ferieTom, SvpOppholdÅrsak.SYKEPENGER));
        var oppholdAndreSøknad = List.of(lagOpphold(ferieFom, ferieTom, SvpOppholdÅrsak.FERIE));

        var eksisterendeTilrettelegging = lagTilrettelegging(arbeidsgiver, førsteFraDato,  BigDecimal.valueOf(50), oppholdFørsteSøknad);
        var nyTilrettelegging = lagTilrettelegging(arbeidsgiver, nyesteFraDato, BigDecimal.valueOf(40), oppholdAndreSøknad);

        var resultat = tilretteleggingOversetter.oppdaterEksisterendeTlrMedNyeFomsOgOpphold(nyTilrettelegging, eksisterendeTilrettelegging);

        var oppholdEtterKopiering = resultat.getAvklarteOpphold().stream().toList();
        assertThat(oppholdEtterKopiering).hasSize(2);
        assertThat(oppholdEtterKopiering.getFirst().getFom()).isEqualTo(ferieFom);
        assertThat(oppholdEtterKopiering.getFirst().getTom()).isEqualTo(ferieTom);
        assertThat(oppholdEtterKopiering.getFirst().getOppholdÅrsak()).isEqualTo(SvpOppholdÅrsak.SYKEPENGER);
        assertThat(oppholdEtterKopiering.getFirst().getKilde()).isEqualTo(SvpOppholdKilde.TIDLIGERE_VEDTAK);
        assertThat(oppholdEtterKopiering.get(1).getFom()).isEqualTo(ferieFom);
        assertThat(oppholdEtterKopiering.get(1).getTom()).isEqualTo(ferieTom);
        assertThat(oppholdEtterKopiering.get(1).getOppholdÅrsak()).isEqualTo(SvpOppholdÅrsak.FERIE);
        assertThat(oppholdEtterKopiering.get(1).getKilde()).isEqualTo(SvpOppholdKilde.SØKNAD);
    }

    @Test
    void sjekker_at_ferie_kopieres_riktig_når_eksisterende_ferie_og_ny_ikke_er_like_og_før_nyeste_tilrettelegging_fom() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var førsteFraDato = LocalDate.now().plusWeeks(2);
        var ferieFom = førsteFraDato.plusWeeks(1).plusDays(2);
        var ferieTom = ferieFom.plusDays(5);

        var nyesteFraDato = ferieFom.plusDays(1);
        var nyFerieTom = ferieTom.plusDays(2);
        var oppholdFørsteSøknad = List.of(lagOpphold(ferieFom, ferieTom, SvpOppholdÅrsak.FERIE));
        var oppholdAndreSøknad = List.of(lagOpphold(ferieFom, nyFerieTom, SvpOppholdÅrsak.FERIE));

        var eksisterendeTilrettelegging = lagTilrettelegging(arbeidsgiver, førsteFraDato,  BigDecimal.valueOf(50), oppholdFørsteSøknad);
        var nyTilrettelegging = lagTilrettelegging(arbeidsgiver, nyesteFraDato, BigDecimal.valueOf(40), oppholdAndreSøknad);

        var resultat = tilretteleggingOversetter.oppdaterEksisterendeTlrMedNyeFomsOgOpphold(nyTilrettelegging, eksisterendeTilrettelegging);

        var oppholdEtterKopiering = resultat.getAvklarteOpphold().stream().toList();
        assertThat(oppholdEtterKopiering).hasSize(2);
        assertThat(oppholdEtterKopiering.getFirst().getFom()).isEqualTo(ferieFom);
        assertThat(oppholdEtterKopiering.getFirst().getTom()).isEqualTo(ferieTom);
        assertThat(oppholdEtterKopiering.getFirst().getKilde()).isEqualTo(SvpOppholdKilde.TIDLIGERE_VEDTAK);
        assertThat(oppholdEtterKopiering.get(1).getFom()).isEqualTo(ferieFom);
        assertThat(oppholdEtterKopiering.get(1).getTom()).isEqualTo(nyFerieTom);
        assertThat(oppholdEtterKopiering.get(1).getKilde()).isEqualTo(SvpOppholdKilde.SØKNAD);
    }



    private SvpTilretteleggingEntitet lagTilrettelegging(Arbeidsgiver arbeidsgiver, LocalDate førsteFraDato, BigDecimal stillingsprosent, List<SvpAvklartOpphold> opphold) {
        return new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medMottattTidspunkt(LocalDateTime.now())
            .medTilretteleggingFraDatoer(List.of(new TilretteleggingFOM.Builder()
                .medStillingsprosent(stillingsprosent)
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(førsteFraDato)
                .build()))
            .medAvklarteOpphold(opphold)
            .build();
    }

    private SvpAvklartOpphold lagOpphold(LocalDate fom, LocalDate tom, SvpOppholdÅrsak svpOppholdÅrsak) {
            return SvpAvklartOpphold.Builder.nytt()
                .medOppholdÅrsak(svpOppholdÅrsak)
                .medOppholdPeriode(fom, tom)
                .medKilde(SvpOppholdKilde.SØKNAD)
                .build();
        }
    }

