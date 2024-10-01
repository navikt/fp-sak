package no.nav.foreldrepenger.domene.uttak.svp;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdKilde;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.UtsettelsePeriode;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.svangerskapspenger.domene.felles.AktivitetType;
import no.nav.svangerskapspenger.domene.felles.Arbeidsforhold;

@ExtendWith(MockitoExtension.class)
class OppholdTjenesteTest {
    private OppholdTjeneste oppholdTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    private final LocalDate BEHOV_FRA_DATO = LocalDate.of(2023,10,1);
    private final LocalDate BEHOV_FRA_DATO_2 = BEHOV_FRA_DATO.plusMonths(1);
    private final Arbeidsgiver ARBEIDSGIVER_1 =  Arbeidsgiver.virksomhet("123456789");

    @BeforeEach
    void setUp() {
        oppholdTjeneste = new OppholdTjeneste(inntektsmeldingTjeneste);
    }

    @Test
    void finnerOppholdFraTilrettelegging(){
        //lag svpGrunnlag med flere tilrettelegginger og 2 med opphold
        var fraDatoer = List.of(lagFraDatoTilr(BigDecimal.valueOf(50)));
        var oppholdFraSaksbehandler = List.of(lagOppholdSaksbehandler(BEHOV_FRA_DATO_2, BEHOV_FRA_DATO_2.plusDays(5), SvpOppholdÅrsak.FERIE), lagOppholdSaksbehandler(BEHOV_FRA_DATO_2.plusDays(20), BEHOV_FRA_DATO_2.plusDays(22), SvpOppholdÅrsak.SYKEPENGER));

        var tilrettelegginger = List.of(lagTilrettelegging(fraDatoer, oppholdFraSaksbehandler, ARBEIDSGIVER_1, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD));
        var svpGrunnlag = lagSvpGrunnlag(tilrettelegginger);
        var inntektsmeldingUtenFerie = lagInntektsmelding(null, null);

        SvangerskapspengerGrunnlag svangerskapspengerGrunnlag = new SvangerskapspengerGrunnlag().medSvpGrunnlagEntitet(svpGrunnlag);
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(BehandlingReferanse.class), any())).thenReturn(List.of(inntektsmeldingUtenFerie));

        //forventet resultat
        var oppholdFraSaksbehandlerOgInntektsmeldingMap = oppholdTjeneste.finnOppholdFraTilretteleggingOgInntektsmelding(lagBehandlingReferanse(),
            Skjæringstidspunkt.builder().medSkjæringstidspunktOpptjening(LocalDate.now().minusMonths(1)).build(), svangerskapspengerGrunnlag);

        var arbeidsforhold = Arbeidsforhold.virksomhet(AktivitetType.ARBEID, "123456789", null );
        var oppholdForventet = oppholdFraSaksbehandlerOgInntektsmeldingMap.get(arbeidsforhold);


        assertThat(oppholdFraSaksbehandlerOgInntektsmeldingMap).hasSize(1);
        assertThat(oppholdForventet).hasSize(2);
        assertThat(oppholdForventet.get(0).getFom()).isEqualTo(BEHOV_FRA_DATO_2);
        assertThat(oppholdForventet.get(0).getTom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(5));
        assertThat(oppholdForventet.get(0).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.FERIE.name());
        assertThat(oppholdForventet.get(1).getFom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(20));
        assertThat(oppholdForventet.get(1).getTom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(22));
        assertThat(oppholdForventet.get(1).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.SYKEPENGER.name());
    }

    @Test
    void finnerOppholdFraflereTilrettelegginger(){
        //lag svpGrunnlag med flere tilrettelegginger og 2 med opphold
        var fraDatoer = List.of(lagFraDatoTilr(BigDecimal.valueOf(50)));
        var oppholdFraSaksbehandler = List.of(lagOppholdSaksbehandler(BEHOV_FRA_DATO_2, BEHOV_FRA_DATO_2.plusDays(5), SvpOppholdÅrsak.FERIE), lagOppholdSaksbehandler(BEHOV_FRA_DATO_2.plusDays(20), BEHOV_FRA_DATO_2.plusDays(22), SvpOppholdÅrsak.SYKEPENGER));

        var tilrettelegginger = List.of(lagTilrettelegging(fraDatoer, oppholdFraSaksbehandler, ARBEIDSGIVER_1, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD),
            lagTilrettelegging(fraDatoer, List.of(lagOppholdSaksbehandler(BEHOV_FRA_DATO_2, BEHOV_FRA_DATO_2.plusDays(5), SvpOppholdÅrsak.FERIE)), null, ArbeidType.FRILANSER));
        var svpGrunnlag = lagSvpGrunnlag(tilrettelegginger);
        var inntektsmeldingUtenFerie = lagInntektsmelding(null, null);

        SvangerskapspengerGrunnlag svangerskapspengerGrunnlag = new SvangerskapspengerGrunnlag().medSvpGrunnlagEntitet(svpGrunnlag);

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(BehandlingReferanse.class), any())).thenReturn(List.of(inntektsmeldingUtenFerie));

        //forventet resultat
        var oppholdFraSaksbehandlerOgInntektsmeldingMap = oppholdTjeneste.finnOppholdFraTilretteleggingOgInntektsmelding(lagBehandlingReferanse(),
            Skjæringstidspunkt.builder().medSkjæringstidspunktOpptjening(LocalDate.now().minusMonths(1)).build(), svangerskapspengerGrunnlag);

        var arbeidsforhold1 = Arbeidsforhold.virksomhet(AktivitetType.ARBEID, "123456789", null );
        var arbeidsforhold2 = Arbeidsforhold.annet(AktivitetType.FRILANS);
        var oppholdForventetArbeidsforhold1 = oppholdFraSaksbehandlerOgInntektsmeldingMap.get(arbeidsforhold1);
        var oppholdForventetArbeidsforhold2 = oppholdFraSaksbehandlerOgInntektsmeldingMap.get(arbeidsforhold2);


        assertThat(oppholdFraSaksbehandlerOgInntektsmeldingMap).hasSize(2);
        assertThat(oppholdForventetArbeidsforhold1).hasSize(2);
        assertThat(oppholdForventetArbeidsforhold1.get(0).getFom()).isEqualTo(BEHOV_FRA_DATO_2);
        assertThat(oppholdForventetArbeidsforhold1.get(0).getTom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(5));
        assertThat(oppholdForventetArbeidsforhold1.get(0).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.FERIE.name());
        assertThat(oppholdForventetArbeidsforhold1.get(1).getFom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(20));
        assertThat(oppholdForventetArbeidsforhold1.get(1).getTom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(22));
        assertThat(oppholdForventetArbeidsforhold1.get(1).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.SYKEPENGER.name());

        assertThat(oppholdForventetArbeidsforhold2).hasSize(1);
        assertThat(oppholdForventetArbeidsforhold2.get(0).getFom()).isEqualTo(BEHOV_FRA_DATO_2);
        assertThat(oppholdForventetArbeidsforhold2.get(0).getTom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(5));
        assertThat(oppholdForventetArbeidsforhold2.get(0).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.FERIE.name());
    }

    @Test
    void finnerOppholdFraflereTilretteleggingerOgInntektsmelding(){
        //lag svpGrunnlag med flere tilrettelegginger og 2 med opphold
        var fraDatoer = List.of(lagFraDatoTilr(BigDecimal.valueOf(50)));
        var oppholdFraSaksbehandler = List.of(lagOppholdSaksbehandler(BEHOV_FRA_DATO_2, BEHOV_FRA_DATO_2.plusDays(5), SvpOppholdÅrsak.FERIE), lagOppholdSaksbehandler(BEHOV_FRA_DATO_2.plusDays(20), BEHOV_FRA_DATO_2.plusDays(22), SvpOppholdÅrsak.SYKEPENGER));

        var tilrettelegginger = List.of(lagTilrettelegging(fraDatoer, oppholdFraSaksbehandler, ARBEIDSGIVER_1, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD),
            lagTilrettelegging(fraDatoer, List.of(lagOppholdSaksbehandler(BEHOV_FRA_DATO_2, BEHOV_FRA_DATO_2.plusDays(5), SvpOppholdÅrsak.FERIE)), null, ArbeidType.FRILANSER));
        var svpGrunnlag = lagSvpGrunnlag(tilrettelegginger);
        var inntektsmeldingMedFerie = lagInntektsmelding(UtsettelsePeriode.ferie(BEHOV_FRA_DATO, BEHOV_FRA_DATO.plusDays(5)), UtsettelsePeriode.ferie(BEHOV_FRA_DATO.plusDays(10), BEHOV_FRA_DATO.plusDays(12)));

        SvangerskapspengerGrunnlag svangerskapspengerGrunnlag = new SvangerskapspengerGrunnlag().medSvpGrunnlagEntitet(svpGrunnlag);

        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(BehandlingReferanse.class), any())).thenReturn(List.of(inntektsmeldingMedFerie));

        //forventet resultat
        var oppholdFraSaksbehandlerOgInntektsmeldingMap = oppholdTjeneste.finnOppholdFraTilretteleggingOgInntektsmelding(lagBehandlingReferanse(),
            Skjæringstidspunkt.builder().medSkjæringstidspunktOpptjening(LocalDate.now().minusMonths(1)).build(), svangerskapspengerGrunnlag);

        var arbeidsforhold1 = Arbeidsforhold.virksomhet(AktivitetType.ARBEID, "123456789", null );
        var arbeidsforhold2 = Arbeidsforhold.annet(AktivitetType.FRILANS);
        var oppholdForventetArbeidsforhold1 = oppholdFraSaksbehandlerOgInntektsmeldingMap.get(arbeidsforhold1);
        var oppholdForventetArbeidsforhold2 = oppholdFraSaksbehandlerOgInntektsmeldingMap.get(arbeidsforhold2);


        assertThat(oppholdFraSaksbehandlerOgInntektsmeldingMap).hasSize(2);
        assertThat(oppholdForventetArbeidsforhold1).hasSize(4);
        assertThat(oppholdForventetArbeidsforhold1.get(0).getFom()).isEqualTo(BEHOV_FRA_DATO_2);
        assertThat(oppholdForventetArbeidsforhold1.get(0).getTom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(5));
        assertThat(oppholdForventetArbeidsforhold1.get(0).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.FERIE.name());
        assertThat(oppholdForventetArbeidsforhold1.get(1).getFom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(20));
        assertThat(oppholdForventetArbeidsforhold1.get(1).getTom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(22));
        assertThat(oppholdForventetArbeidsforhold1.get(1).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.SYKEPENGER.name());
        assertThat(oppholdForventetArbeidsforhold1.get(2).getFom()).isEqualTo(BEHOV_FRA_DATO);
        assertThat(oppholdForventetArbeidsforhold1.get(2).getTom()).isEqualTo(BEHOV_FRA_DATO.plusDays(5));
        assertThat(oppholdForventetArbeidsforhold1.get(2).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.FERIE.name());
        assertThat(oppholdForventetArbeidsforhold1.get(3).getFom()).isEqualTo(BEHOV_FRA_DATO.plusDays(10));
        assertThat(oppholdForventetArbeidsforhold1.get(3).getTom()).isEqualTo(BEHOV_FRA_DATO.plusDays(12));
        assertThat(oppholdForventetArbeidsforhold1.get(3).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.FERIE.name());

        assertThat(oppholdForventetArbeidsforhold2).hasSize(1);
        assertThat(oppholdForventetArbeidsforhold2.get(0).getFom()).isEqualTo(BEHOV_FRA_DATO_2);
        assertThat(oppholdForventetArbeidsforhold2.get(0).getTom()).isEqualTo(BEHOV_FRA_DATO_2.plusDays(5));
        assertThat(oppholdForventetArbeidsforhold2.get(0).getÅrsak().name()).isEqualTo(SvpOppholdÅrsak.FERIE.name());
    }

    SvpGrunnlagEntitet lagSvpGrunnlag(List<SvpTilretteleggingEntitet> tilrettelegginger) {
        return new SvpGrunnlagEntitet.Builder().medBehandlingId(123456L)
            .medOverstyrteTilrettelegginger(tilrettelegginger)
            .build();
    }

    private TilretteleggingFOM lagFraDatoTilr(BigDecimal stillingsprosent) {
        return new TilretteleggingFOM.Builder()
            .medFomDato(BEHOV_FRA_DATO)
            .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
            .medStillingsprosent(stillingsprosent)
            .medTidligstMottattDato(BEHOV_FRA_DATO.minusDays(5))
            .build();
    }
    private SvpTilretteleggingEntitet lagTilrettelegging(List<TilretteleggingFOM> fraDatoer, List<SvpAvklartOpphold> oppholdListe, Arbeidsgiver arbeidsgiver, ArbeidType arbeidType){
        var builder = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(BEHOV_FRA_DATO)
            .medTilretteleggingFraDatoer(fraDatoer)
            .medArbeidType(arbeidType)
            .medArbeidsgiver(arbeidsgiver)
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .medAvklarteOpphold(oppholdListe);
        if (arbeidsgiver != null) {
            builder.medArbeidsgiver(arbeidsgiver);
        }
        return builder.build();
    }

    private SvpAvklartOpphold lagOppholdSaksbehandler(LocalDate fom, LocalDate tom, SvpOppholdÅrsak årsak) {
        return SvpAvklartOpphold.Builder.nytt()
            .medOppholdÅrsak(årsak)
            .medOppholdPeriode(fom, tom)
            .medKilde(SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER)
            .build();
    }

    private Inntektsmelding lagInntektsmelding(UtsettelsePeriode utsettelsePeriode, UtsettelsePeriode utsettelsePeriode2) {
        var builder = InntektsmeldingBuilder.builder()
            .medStartDatoPermisjon(LocalDate.now())
            .medArbeidsgiver(ARBEIDSGIVER_1)
            .medBeløp(BigDecimal.valueOf(1000))
            .medNærRelasjon(false)
            .medInnsendingstidspunkt(LocalDateTime.now())
            .medJournalpostId(new JournalpostId("987654321"));

        if (utsettelsePeriode != null) {
            builder.leggTil(utsettelsePeriode);
        }

        if (utsettelsePeriode2 != null) {
            builder.leggTil(utsettelsePeriode2);
        }
        return  builder.build();
    }

    private static BehandlingReferanse lagBehandlingReferanse() {
        return new BehandlingReferanse(new Saksnummer("1234"),
            1234L,
            FagsakYtelseType.SVANGERSKAPSPENGER,
            123456L,
            UUID.randomUUID(),
            BehandlingStatus.UTREDES,
            BehandlingType.FØRSTEGANGSSØKNAD,
            null,
            null,
            RelasjonsRolleType.MORA);
    }

}
