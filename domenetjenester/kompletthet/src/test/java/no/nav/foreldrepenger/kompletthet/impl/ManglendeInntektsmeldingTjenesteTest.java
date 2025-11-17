package no.nav.foreldrepenger.kompletthet.impl;

import static no.nav.foreldrepenger.kompletthet.impl.ManglendeInntektsmeldingTjeneste.MAX_VENT_ETTER_STP;
import static no.nav.foreldrepenger.kompletthet.impl.ManglendeInntektsmeldingTjeneste.TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO;
import static no.nav.foreldrepenger.kompletthet.impl.ManglendeInntektsmeldingTjeneste.VENTEFRIST_IM_ETTER_ETTERLYSNING;
import static no.nav.foreldrepenger.kompletthet.impl.ManglendeInntektsmeldingTjeneste.VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;


@ExtendWith(MockitoExtension.class)
class ManglendeInntektsmeldingTjenesteTest {

    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    @Mock
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    private BehandlingRepositoryProvider repositoryProvider;
    private ManglendeInntektsmeldingTjeneste manglendeInntektsmeldingTjeneste;

    @BeforeEach
    void beforeAll() {
        repositoryProvider = ScenarioMorSøkerForeldrepenger.forFødsel().mockBehandlingRepositoryProvider();
        manglendeInntektsmeldingTjeneste = new ManglendeInntektsmeldingTjeneste(
            repositoryProvider,
            dokumentBehandlingTjenesteMock,
            inntektsmeldingArkivTjeneste,
            inntektsmeldingTjeneste
        );
    }

    @Test
    void skal_utlede_ventefrist_fra_mottattdato_fra_søknad_hvis_søkt_senere_enn_4_uker_før_stp() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(3);
        var mottattDato = LocalDate.now();

        var frist = manglendeInntektsmeldingTjeneste.finnInitiellVentefristTilManglendeInntektsmelding(behandlingMedMottattdatoForSøknad(mottattDato), tilSkjæringstidspunkt(skjæringstidspunkt));

        assertThat(frist).isEqualTo(mottattDato.plus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO));
    }

    @Test
    void skal_utlede_ventefrist_fra_stp_hvis_søkt_tidligere_enn_4_uker_før_stp() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(8);
        var mottattDato = LocalDate.now();

        var frist = manglendeInntektsmeldingTjeneste.finnInitiellVentefristTilManglendeInntektsmelding(behandlingMedMottattdatoForSøknad(mottattDato), tilSkjæringstidspunkt(skjæringstidspunkt));

        assertThat(frist).isEqualTo(skjæringstidspunkt.minus(TIDLIGST_VENTEFRIST_IM_FØR_UTTAKSDATO));
    }

    @Test
    void skal_utlede_ventefrist_basert_på_mottattdato_søknad_hvis_stp_er_etter_mottatdato_ingen_IM_mottatt() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(5);
        var mottattDato = LocalDate.now();
        when(dokumentBehandlingTjenesteMock.dokumentSistBestiltTidspunkt(any(), any())).thenReturn(Optional.of(mottattDato.atStartOfDay()));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of());

        var frist = manglendeInntektsmeldingTjeneste.finnNyVentefristVedEtterlysning(behandlingMedMottattdatoForSøknad(mottattDato), tilSkjæringstidspunkt(skjæringstidspunkt));

        assertThat(frist).isEqualTo(mottattDato.plus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO).plus(VENTEFRIST_IM_ETTER_ETTERLYSNING));
    }

    @Test
    void skal_utlede_ventefrist_basert_på_dagens_dato_hvis_søkt_tidligere_enn_4_uker_før_stp_ingen_IM_mottatt() {
        var mottattDato = LocalDate.now().minusWeeks(2);
        var skjæringstidspunkt = mottattDato.plusWeeks(5);
        when(dokumentBehandlingTjenesteMock.dokumentSistBestiltTidspunkt(any(), any())).thenReturn(Optional.of(mottattDato.atStartOfDay()));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of());

        var frist = manglendeInntektsmeldingTjeneste.finnNyVentefristVedEtterlysning(behandlingMedMottattdatoForSøknad(mottattDato), tilSkjæringstidspunkt(skjæringstidspunkt));

        assertThat(frist).isEqualTo(LocalDate.now().plus(VENTEFRIST_IM_ETTER_ETTERLYSNING));
    }

    @Test
    void skal_utlede_ventefrist_basert_på_mottattdato_søknad_hvis_søknad_er_sendt_kort_tid_etter_stp_ingen_IM_mottatt() {
        var skjæringstidspunkt = LocalDate.now().minusWeeks(2);
        var mottattDato = LocalDate.now();
        when(dokumentBehandlingTjenesteMock.dokumentSistBestiltTidspunkt(any(), any())).thenReturn(Optional.of(mottattDato.atStartOfDay()));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of());

        var frist = manglendeInntektsmeldingTjeneste.finnNyVentefristVedEtterlysning(behandlingMedMottattdatoForSøknad(mottattDato), tilSkjæringstidspunkt(skjæringstidspunkt));

        assertThat(frist).isEqualTo(mottattDato.plus(VENTEFRIST_IM_ETTER_SØKNAD_MOTTATT_DATO).plus(VENTEFRIST_IM_ETTER_ETTERLYSNING));
    }

    @Test
    void skal_utledes_ventefrist_for_etterlysning_fra_maks_ventetid_etter_stp_hvis_søknad_er_sendt_inn_senere_enn_4_uker_etter_stp_ingen_IM_mottatt() {
        var skjæringstidspunkt = LocalDate.now().minusWeeks(5);
        var mottattDato = LocalDate.now();
        when(dokumentBehandlingTjenesteMock.dokumentSistBestiltTidspunkt(any(), any())).thenReturn(Optional.of(mottattDato.atStartOfDay()));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of());

        var frist = manglendeInntektsmeldingTjeneste.finnNyVentefristVedEtterlysning(behandlingMedMottattdatoForSøknad(mottattDato), tilSkjæringstidspunkt(skjæringstidspunkt));

        assertThat(frist).isEqualTo(skjæringstidspunkt.plus(MAX_VENT_ETTER_STP).plus(VENTEFRIST_IM_ETTER_ETTERLYSNING));
    }

    @Test
    void skal_utlede_ventefrist_basert_på_dato_for_når_brev_for_etterlysning_av_IM_ble_sendt_hvis_ingen_nye_IM_og_mottatdato_for_søknad_er_før_stp() {
        var skjæringstidspunkt = LocalDate.now().minusWeeks(2);
        var mottattDato = LocalDate.now().minusWeeks(4);
        var datoSendtUtEtterlysIM = LocalDate.now().minusWeeks(1);
        when(dokumentBehandlingTjenesteMock.dokumentSistBestiltTidspunkt(any(), any())).thenReturn(Optional.of(datoSendtUtEtterlysIM.atStartOfDay()));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(List.of());

        var frist = manglendeInntektsmeldingTjeneste.finnNyVentefristVedEtterlysning(behandlingMedMottattdatoForSøknad(mottattDato), tilSkjæringstidspunkt(skjæringstidspunkt));

        assertThat(frist).isEqualTo(datoSendtUtEtterlysIM.plusWeeks(1));
    }

    @Test
    void skal_utledes_ventefrist_ut_fra_dato_for_den_tidligste_mottatte_inntektsmeldingen_hvis_lenger_frem_i_tid_enn_utsendingstidspunktet_for_etterlys_im_brev() {
        var skjæringstidspunkt = LocalDate.now().plusWeeks(10);
        var mottattDato = LocalDate.now().minusWeeks(1);
        var tidligstInnsendingstidspunktIM = LocalDate.now().minusDays(3);

        when(dokumentBehandlingTjenesteMock.dokumentSistBestiltTidspunkt(any(), any())).thenReturn(Optional.of(tidligstInnsendingstidspunktIM.minusWeeks(1).atStartOfDay()));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any()))
            .thenReturn(List.of(
                lagInntektsmelding(skjæringstidspunkt, tidligstInnsendingstidspunktIM),
                lagInntektsmelding(skjæringstidspunkt, tidligstInnsendingstidspunktIM.plusWeeks(1))
                ));

        var frist = manglendeInntektsmeldingTjeneste.finnNyVentefristVedEtterlysning(behandlingMedMottattdatoForSøknad(mottattDato), tilSkjæringstidspunkt(skjæringstidspunkt));

        if (tidligstInnsendingstidspunktIM.getDayOfWeek().getValue() > DayOfWeek.TUESDAY.getValue()) {
            assertThat(frist).isEqualTo(tidligstInnsendingstidspunktIM.plusDays(5));
        } else {
            assertThat(frist).isEqualTo(tidligstInnsendingstidspunktIM.plusDays(3));
        }
    }

    @Test
    void skal_utlede_frist_fra_utsendelsestidspunkt_for_brev_etterlysning_hvis_inntektsmeldinger_som_er_mottatt_er_lengre_tilbake_i_tid_enn_etterlys_IM() {
        var skjæringstidspunkt = LocalDate.now().minusWeeks(2);
        var mottattDato = LocalDate.now().minusWeeks(4);
        var datoSendtUtEtterlysIM = LocalDate.now().minusWeeks(1);
        var tidligstInnsendingstidspunktIM = mottattDato;

        when(dokumentBehandlingTjenesteMock.dokumentSistBestiltTidspunkt(any(), any())).thenReturn(Optional.of(datoSendtUtEtterlysIM.atStartOfDay()));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any()))
            .thenReturn(List.of(
                lagInntektsmelding(skjæringstidspunkt, tidligstInnsendingstidspunktIM),
                lagInntektsmelding(skjæringstidspunkt, tidligstInnsendingstidspunktIM.plusWeeks(1))
            ));

        var frist = manglendeInntektsmeldingTjeneste.finnNyVentefristVedEtterlysning(behandlingMedMottattdatoForSøknad(mottattDato), tilSkjæringstidspunkt(skjæringstidspunkt));
        assertThat(frist).isEqualTo(datoSendtUtEtterlysIM.plusWeeks(1));
    }


    private static Skjæringstidspunkt tilSkjæringstidspunkt(LocalDate stp) {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build();
    }

    private static Inntektsmelding lagInntektsmelding(LocalDate skjæringstidspunkt, LocalDate innsendingstidspunkt) {
        return InntektsmeldingBuilder.builder()
            .medStartDatoPermisjon(skjæringstidspunkt)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.valueOf(55000))
            .medNærRelasjon(false)
            .medInnsendingstidspunkt(innsendingstidspunkt.atStartOfDay())
            .build();
    }

    private BehandlingReferanse behandlingMedMottattdatoForSøknad(LocalDate mottattDato) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad().medMottattDato(mottattDato);
        var behandling = scenario.lagre(repositoryProvider);
        return BehandlingReferanse.fra(behandling);
    }



}
