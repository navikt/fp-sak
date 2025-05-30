package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.domene.typer.Beløp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
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
    private HistorikkinnslagRepository historikkRepository;
    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    @Mock
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;

    @BeforeEach
    void setup() {
        fpInntektsmeldingTjeneste = new FpInntektsmeldingTjeneste(klient, taskTjeneste, skjæringstidspunktTjeneste,  historikkRepository, arbeidsgiverTjeneste, inntektsmeldingRegisterTjeneste);
    }

    @Test
    void skal_overstyre_inntektsmelding_uten_endringer() {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var saksnummerDto = new SaksnummerDto("1234");
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
        fpInntektsmeldingTjeneste.overstyrInntektsmelding(inntektsmelding, Optional.empty(), Optional.of(opphørsdato), Map.of(), Optional.empty(), "Truls Test", behandlingRef);

        // Assert
        var foventedeRefusjonsendringer = List.of(new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(stp.plusDays(10), BigDecimal.valueOf(4000)), new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(opphørsdato, BigDecimal.ZERO));
        var forventetRequest = new OverstyrInntektsmeldingRequest(new OverstyrInntektsmeldingRequest.AktørIdDto("9999999999999"), new OverstyrInntektsmeldingRequest.ArbeidsgiverDto("999999999"), stp, OverstyrInntektsmeldingRequest.YtelseType.FORELDREPENGER, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
            foventedeRefusjonsendringer, Collections.emptyList(), "Truls Test", saksnummerDto);
        verify(klient, times(1)).overstyrInntektsmelding(forventetRequest);
    }

    @Test
    void skal_overstyre_inntektsmelding_med_endringer() {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var saksnummerDto = new SaksnummerDto("1234");
        var inntektsmelding = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medBeløp(BigDecimal.valueOf(5000))
            .medRefusjon(BigDecimal.valueOf(5000))
            .build();

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingRef.behandlingId())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build());

        // Act
        fpInntektsmeldingTjeneste.overstyrInntektsmelding(inntektsmelding, Optional.empty(), Optional.empty(), Map.of(stp.plusDays(10), Beløp.ZERO, stp.plusDays(15), Beløp.av(4000)), Optional.empty(), "Truls Test", behandlingRef);

        // Assert
        var foventedeRefusjonsendringer = List.of(new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(stp.plusDays(10), BigDecimal.ZERO), new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(stp.plusDays(15), BigDecimal.valueOf(4000)));
        var forventetRequest = new OverstyrInntektsmeldingRequest(new OverstyrInntektsmeldingRequest.AktørIdDto("9999999999999"), new OverstyrInntektsmeldingRequest.ArbeidsgiverDto("999999999"), stp, OverstyrInntektsmeldingRequest.YtelseType.FORELDREPENGER, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
            foventedeRefusjonsendringer, Collections.emptyList(), "Truls Test", saksnummerDto);
        verify(klient, times(1)).overstyrInntektsmelding(forventetRequest);
    }

    @Test
    void skal_overstyre_opphørsdato_og_flette_refusjonsendringer() {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var saksnummerDto = new SaksnummerDto("1234");
        var ref1 = new Refusjon(BigDecimal.valueOf(4000), stp.plusDays(10));
        var inntektsmelding = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medBeløp(BigDecimal.valueOf(5000))
            .medRefusjon(BigDecimal.valueOf(5000))
            .leggTil(ref1)
            .build();

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingRef.behandlingId())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build());

        // Act
        fpInntektsmeldingTjeneste.overstyrInntektsmelding(inntektsmelding, Optional.empty(), Optional.of(stp.plusDays(15)), Map.of(stp.plusDays(5), Beløp.ZERO), Optional.empty(), "Truls Test", behandlingRef);

        // Assert
        var foventedeRefusjonsendringer = List.of(new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(stp.plusDays(5), BigDecimal.ZERO),
            new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(stp.plusDays(10), BigDecimal.valueOf(4000)),
            new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(stp.plusDays(15), BigDecimal.ZERO));
        var forventetRequest = new OverstyrInntektsmeldingRequest(new OverstyrInntektsmeldingRequest.AktørIdDto("9999999999999"), new OverstyrInntektsmeldingRequest.ArbeidsgiverDto("999999999"), stp, OverstyrInntektsmeldingRequest.YtelseType.FORELDREPENGER, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
            foventedeRefusjonsendringer, Collections.emptyList(), "Truls Test", saksnummerDto);
        verify(klient, times(1)).overstyrInntektsmelding(forventetRequest);
    }

    @Test
    void skal_overstyre_refusjon_fra_start_og_legge_til_endring() {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var saksnummerDto = new SaksnummerDto("1234");
        var inntektsmelding = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medBeløp(BigDecimal.valueOf(5000))
            .medRefusjon(BigDecimal.valueOf(5000))
            .build();

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingRef.behandlingId())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build());

        // Act
        fpInntektsmeldingTjeneste.overstyrInntektsmelding(inntektsmelding, Optional.of(0L), Optional.of(stp.plusYears(1)), Map.of(stp.plusDays(5), Beløp.av(5000L)), Optional.empty(), "Truls Test", behandlingRef);

        // Assert
        var foventedeRefusjonsendringer = List.of(new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(stp.plusDays(5), BigDecimal.valueOf(5000)), new OverstyrInntektsmeldingRequest.RefusjonendringRequestDto(stp.plusYears(1), BigDecimal.ZERO));
        var forventetRequest = new OverstyrInntektsmeldingRequest(new OverstyrInntektsmeldingRequest.AktørIdDto("9999999999999"), new OverstyrInntektsmeldingRequest.ArbeidsgiverDto("999999999"), stp, OverstyrInntektsmeldingRequest.YtelseType.FORELDREPENGER, BigDecimal.valueOf(5000), BigDecimal.valueOf(0),
            foventedeRefusjonsendringer, Collections.emptyList(), "Truls Test", saksnummerDto);
        verify(klient, times(1)).overstyrInntektsmelding(forventetRequest);
    }

    @Test
    void skal_overstyre_startdato_og_refusjon_fra_start() {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var saksnummerDto = new SaksnummerDto("1234");
        var inntektsmelding = InntektsmeldingBuilder.builder()
            .medStartDatoPermisjon(stp)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medBeløp(BigDecimal.valueOf(5000))
            .medRefusjon(BigDecimal.valueOf(5000))
            .build();

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);

        // Act
        fpInntektsmeldingTjeneste.overstyrInntektsmelding(inntektsmelding, Optional.of(6000L), Optional.empty(), Map.of(), Optional.of(stp.minusDays(10)), "Truls Test", behandlingRef);

        // Assert
        var forventetRequest = new OverstyrInntektsmeldingRequest(new OverstyrInntektsmeldingRequest.AktørIdDto("9999999999999"), new OverstyrInntektsmeldingRequest.ArbeidsgiverDto("999999999"),
            stp.minusDays(10), OverstyrInntektsmeldingRequest.YtelseType.FORELDREPENGER, BigDecimal.valueOf(5000), BigDecimal.valueOf(6000),
            List.of(), Collections.emptyList(), "Truls Test", saksnummerDto);
        verify(klient, times(1)).overstyrInntektsmelding(forventetRequest);
    }
    @Test
    void skal_opprette_opppgave_og_historikkinnslag() {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        var stpp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).medFørsteUttaksdato(stp).build();

        when(klient.opprettForespørsel(any())).thenReturn(new OpprettForespørselResponsNy(List.of(new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(new OrganisasjonsnummerDto(virksomhet.getOrgnr()), OpprettForespørselResponsNy.ForespørselResultat.FORESPØRSEL_OPPRETTET))));
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhet.getIdentifikator())).thenReturn(Virksomhet.getBuilder().medOrgnr(virksomhet.getIdentifikator()).medNavn("Testbedrift").build());
        when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingRef, stpp)).thenReturn(Map.of(Arbeidsgiver.virksomhet(virksomhet.getOrgnr()), Set.of(
            InternArbeidsforholdRef.nyRef())));
        // Act
        fpInntektsmeldingTjeneste.lagForespørsel(behandlingRef, stpp);

        // Assert
        verify(historikkRepository, times(1)).lagre(any());
    }

    @Test
    void skal_opprette_historikkinnslag_for_flere() {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var virksomhet = Arbeidsgiver.virksomhet("999999999");
        var virksomhet2 = Arbeidsgiver.virksomhet("123456789");

        var imer = Map.of(Arbeidsgiver.virksomhet(virksomhet.getOrgnr()), Set.of(InternArbeidsforholdRef.nullRef()), Arbeidsgiver.virksomhet(virksomhet2.getOrgnr()), Set.of(InternArbeidsforholdRef.nullRef()));

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        var stpp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).medFørsteUttaksdato(stp).build();
        when(klient.opprettForespørsel(any())).thenReturn(new OpprettForespørselResponsNy(
            List.of(new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(new OrganisasjonsnummerDto(virksomhet.getOrgnr()), OpprettForespørselResponsNy.ForespørselResultat.FORESPØRSEL_OPPRETTET),
                    new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(new OrganisasjonsnummerDto(virksomhet2.getOrgnr()), OpprettForespørselResponsNy.ForespørselResultat.FORESPØRSEL_OPPRETTET))));
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhet.getIdentifikator())).thenReturn(Virksomhet.getBuilder().medOrgnr(virksomhet.getIdentifikator()).medNavn("Testbedrift").build());
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhet2.getIdentifikator())).thenReturn(Virksomhet.getBuilder().medOrgnr(virksomhet2.getIdentifikator()).medNavn("Testbedrift 2").build());
        when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingRef, stpp)).thenReturn(imer);
        // Act
        fpInntektsmeldingTjeneste.lagForespørsel(behandlingRef, stpp);

        // Assert
        verify(historikkRepository, times(2)).lagre(any());
    }

    @Test
    void skal_ikke_opprettet_historikk_når_ny_oppgave_ikke_ble_opprettet() {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        var stpp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).medFørsteUttaksdato(stp.plusDays(1)).build();
        when(klient.opprettForespørsel(any())).thenReturn(new OpprettForespørselResponsNy(List.of(new OpprettForespørselResponsNy.OrganisasjonsnummerMedStatus(new OrganisasjonsnummerDto(virksomhet.getOrgnr()), OpprettForespørselResponsNy.ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE))));
        when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingRef, stpp)).thenReturn(Map.of(Arbeidsgiver.virksomhet(virksomhet.getOrgnr()), Set.of(InternArbeidsforholdRef.nullRef())));
        // Act
        fpInntektsmeldingTjeneste.lagForespørsel(behandlingRef, stpp);

        // Assert
        verify(historikkRepository, times(0)).lagre(any());
    }

}
