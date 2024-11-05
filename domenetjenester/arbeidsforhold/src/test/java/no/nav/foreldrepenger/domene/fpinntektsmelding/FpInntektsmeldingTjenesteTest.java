package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;


@ExtendWith(MockitoExtension.class)
class FpInntektsmeldingTjenesteTest {

    @Mock
    private FpinntektsmeldingKlient klient;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private HistorikkRepository historikkRepository;
    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    @Mock
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    @Mock
    private ArbeidsforholdInntektsmeldingMangelTjeneste inntektsmeldingMangelTjeneste;
    @Mock
    private FpInntektsmeldingForespørselTjeneste fpInntektsmeldingForespørselTjeneste;

    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;

    @BeforeEach
    void setup() {
        fpInntektsmeldingTjeneste = new FpInntektsmeldingTjeneste(klient, taskTjeneste, skjæringstidspunktTjeneste,  historikkRepository, arbeidsgiverTjeneste, inntektsmeldingRegisterTjeneste, inntektsmeldingMangelTjeneste, fpInntektsmeldingForespørselTjeneste);
    }

    @Test
    void skal_overstyre_inntektsmelding() {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var opphørsdato = stp.plusDays(15);
        var ref1 = new Refusjon(BigDecimal.valueOf(4000), stp.plusDays(10));
        var ref2 = new Refusjon(BigDecimal.valueOf(4000), stp.plusDays(20));
        var inntektsmelding = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medBeløp(BigDecimal.valueOf(5000))
            .medRefusjon(BigDecimal.valueOf(5000))
            .leggTil(ref1)
            .leggTil(ref2)
            .build();

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingRef.behandlingId())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build());

        // Act
        fpInntektsmeldingTjeneste.overstyrInntektsmelding(inntektsmelding, opphørsdato, "Truls Test", behandlingRef);

        // Assert
        var foventedeRefusjonsendringer = List.of(new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(stp.plusDays(10), BigDecimal.valueOf(4000)), new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(opphørsdato, BigDecimal.ZERO));
        var forventetRequest = new OverstyrInntektsmeldingRequest(new OverstyrInntektsmeldingRequest.AktørIdDto("9999999999999"), new OverstyrInntektsmeldingRequest.ArbeidsgiverDto("999999999"), stp, OverstyrInntektsmeldingRequest.YtelseType.FORELDREPENGER, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
            foventedeRefusjonsendringer, Collections.emptyList(), "Truls Test");
        verify(klient, times(1)).overstyrInntektsmelding(forventetRequest);
    }

    @Test
    void skal_opprette_opppgave_og_historikkinnslag() {
        // Arrange
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(klient.opprettForespørsel(any())).thenReturn(new OpprettForespørselResponse(OpprettForespørselResponse.ForespørselResultat.FORESPØRSEL_OPPRETTET));
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhet.getIdentifikator())).thenReturn(Virksomhet.getBuilder().medOrgnr(virksomhet.getIdentifikator()).medNavn("Testbedrift").build());
        when(fpInntektsmeldingForespørselTjeneste.lagForespørsel(any(), any())).thenReturn(new OpprettForespørselRequest(null, null, null, null, null, null));
        // Act
        fpInntektsmeldingTjeneste.lagForespørsel(virksomhet.getIdentifikator(), behandlingRef);

        // Assert
        verify(historikkRepository, times(1)).lagre(any());
    }

    @Test
    void skal_ikke_opprettet_historikk_når_ny_oppgave_ikke_ble_opprettet() {
        // Arrange
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(fpInntektsmeldingForespørselTjeneste.lagForespørsel(any(), any())).thenReturn(new OpprettForespørselRequest(null, null, null, null, null, null));
        when(klient.opprettForespørsel(any())).thenReturn(new OpprettForespørselResponse(OpprettForespørselResponse.ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE_ÅPEN));

        // Act
        fpInntektsmeldingTjeneste.lagForespørsel(virksomhet.getIdentifikator(), behandlingRef);

        // Assert
        verify(historikkRepository, times(0)).lagre(any());
    }

}
