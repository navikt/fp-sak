package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
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
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;


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
        fpInntektsmeldingTjeneste = new FpInntektsmeldingTjeneste(klient, taskTjeneste, skjæringstidspunktTjeneste, historikkRepository, arbeidsgiverTjeneste, inntektsmeldingRegisterTjeneste);
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void skal_opprette_opppgave_og_historikkinnslag(boolean prod) {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        var stpp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).medFørsteUttaksdato(stp).build();

        var respons = new OpprettForespørselRespons(List.of(new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(
            new OrganisasjonsnummerDto(virksomhet.getOrgnr()), OpprettForespørselRespons.ForespørselResultat.FORESPØRSEL_OPPRETTET)));
        mockAlleForespørsler(prod, behandlingRef, stpp, Map.of(virksomhet, Set.of(InternArbeidsforholdRef.nyRef())), respons);
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhet.getIdentifikator())).thenReturn(Virksomhet.getBuilder().medOrgnr(virksomhet.getIdentifikator()).medNavn("Testbedrift").build());
        // Act
        kjørIMiljø(prod, () -> fpInntektsmeldingTjeneste.lagForespørselForAlleArbeidsgivere(behandlingRef, stpp));

        // Assert
        var organisasjonsnumre = List.of(new OrganisasjonsnummerDto(virksomhet.getOrgnr()));
        if (prod) {
            verify(klient).opprettForespørsel(new OpprettForespørselRequest(new AktørIdDto(behandlingRef.aktørId().getId()), null, stp,
                FpinntektsmeldingYtelse.FORELDREPENGER, new SaksnummerDto("1234"), stp, organisasjonsnumre));
        } else {
            verify(klient).opprettForespørselKomplett(new OpprettKomplettForespørslerRequest(new AktørIdDto(behandlingRef.aktørId().getId()),
                stp, FpinntektsmeldingYtelse.FORELDREPENGER, new SaksnummerDto("1234"), stp, organisasjonsnumre));
        }
        verifyNoMoreInteractions(klient);
        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository).lagre(captor.capture());
        var historikkinnslag = captor.getValue();
        assertThat(historikkinnslag.getTittel()).isEqualTo("Forespørsel om inntektsmelding");
        assertThat(historikkinnslag.getTekstLinjer()).anySatisfy(linje -> assertThat(linje).isEqualTo("Testbedrift (999999999)."));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void skal_opprette_forespørsel_for_enkelt_arbeidsgiver(boolean prod) {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        var stpp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).medFørsteUttaksdato(stp).build();

        var resultat = new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(new OrganisasjonsnummerDto(virksomhet.getOrgnr()),
            OpprettForespørselRespons.ForespørselResultat.FORESPØRSEL_OPPRETTET);
        if (prod) {
            when(klient.opprettForespørsel(any())).thenReturn(new OpprettForespørselRespons(List.of(resultat)));
        } else {
            when(klient.opprettSpesifikkForespørsel(any())).thenReturn(resultat);
        }
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhet.getIdentifikator())).thenReturn(Virksomhet.getBuilder().medOrgnr(virksomhet.getIdentifikator()).medNavn("Testbedrift").build());

        // Act
        kjørIMiljø(prod, () -> fpInntektsmeldingTjeneste.lagForespørselForBestemtArbeidsgiver(behandlingRef, stpp, virksomhet));

        // Assert
        var organisasjonsnummer = new OrganisasjonsnummerDto(virksomhet.getOrgnr());
        if (prod) {
            verify(klient).opprettForespørsel(new OpprettForespørselRequest(new AktørIdDto(behandlingRef.aktørId().getId()), null, stp,
                FpinntektsmeldingYtelse.FORELDREPENGER, new SaksnummerDto("1234"), stp, List.of(organisasjonsnummer)));
        } else {
            verify(klient).opprettSpesifikkForespørsel(new OpprettEnForespørselRequest(new AktørIdDto(behandlingRef.aktørId().getId()),
                organisasjonsnummer, stp, FpinntektsmeldingYtelse.FORELDREPENGER, new SaksnummerDto("1234"), stp));
        }
        verifyNoMoreInteractions(klient);
        verify(historikkRepository).lagre(any());
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void skal_opprette_historikkinnslag_for_flere(boolean prod) {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var virksomhet = Arbeidsgiver.virksomhet("999999999");
        var virksomhet2 = Arbeidsgiver.virksomhet("123456789");

        var imer = Map.of(Arbeidsgiver.virksomhet(virksomhet.getOrgnr()), Set.of(InternArbeidsforholdRef.nullRef()), Arbeidsgiver.virksomhet(virksomhet2.getOrgnr()), Set.of(InternArbeidsforholdRef.nullRef()));

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        var stpp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).medFørsteUttaksdato(stp).build();
        var respons = new OpprettForespørselRespons(
            List.of(new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(new OrganisasjonsnummerDto(virksomhet.getOrgnr()), OpprettForespørselRespons.ForespørselResultat.FORESPØRSEL_OPPRETTET),
                    new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(new OrganisasjonsnummerDto(virksomhet2.getOrgnr()), OpprettForespørselRespons.ForespørselResultat.FORESPØRSEL_OPPRETTET)));
        mockAlleForespørsler(prod, behandlingRef, stpp, imer, respons);
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhet.getIdentifikator())).thenReturn(Virksomhet.getBuilder().medOrgnr(virksomhet.getIdentifikator()).medNavn("Testbedrift").build());
        when(arbeidsgiverTjeneste.hentVirksomhet(virksomhet2.getIdentifikator())).thenReturn(Virksomhet.getBuilder().medOrgnr(virksomhet2.getIdentifikator()).medNavn("Testbedrift 2").build());
        // Act
        kjørIMiljø(prod, () -> fpInntektsmeldingTjeneste.lagForespørselForAlleArbeidsgivere(behandlingRef, stpp));

        // Assert
        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository).lagre(captor.capture());
        var tekstLinjer = captor.getValue().getTekstLinjer();
        assertThat(tekstLinjer).contains("Testbedrift (999999999).", "Testbedrift 2 (123456789).");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void skal_ikke_opprettet_historikk_når_ny_oppgave_ikke_ble_opprettet(boolean prod) {
        // Arrange
        var stp = LocalDate.of(2024,9,1);
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        var stpp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).medFørsteUttaksdato(stp.plusDays(1)).build();
        var respons = new OpprettForespørselRespons(List.of(new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(
            new OrganisasjonsnummerDto(virksomhet.getOrgnr()), OpprettForespørselRespons.ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE)));
        mockAlleForespørsler(prod, behandlingRef, stpp, Map.of(virksomhet, Set.of(InternArbeidsforholdRef.nullRef())), respons);
        // Act
        kjørIMiljø(prod, () -> fpInntektsmeldingTjeneste.lagForespørselForAlleArbeidsgivere(behandlingRef, stpp));

        // Assert
        verify(historikkRepository, times(0)).lagre(any());
    }

    @Test
    void skal_serialisere_komplett_forespørsel_med_organisasjonsnummer() {
        var request = new OpprettKomplettForespørslerRequest(new AktørIdDto("9999999999999"), LocalDate.of(2024, 9, 1),
            FpinntektsmeldingYtelse.FORELDREPENGER, new SaksnummerDto("1234"), LocalDate.of(2024, 9, 5),
            List.of(new OrganisasjonsnummerDto("999999999"), new OrganisasjonsnummerDto("123456789")));

        var json = DefaultJsonMapper.treeFromJson(DefaultJsonMapper.toJson(request));

        assertThat(json).isEqualTo(DefaultJsonMapper.treeFromJson("""
            {
              "aktørId": "9999999999999",
              "skjæringstidspunkt": "2024-09-01",
              "ytelsetype": "FORELDREPENGER",
              "fagsakSaksnummer": "1234",
              "førsteUttaksdato": "2024-09-05",
              "organisasjonsnummer": ["999999999", "123456789"]
            }
            """));
    }

    @Test
    void skal_serialisere_enkeltforespørsel_med_orgnummer() {
        var request = new OpprettEnForespørselRequest(new AktørIdDto("9999999999999"), new OrganisasjonsnummerDto("999999999"),
            LocalDate.of(2024, 9, 1), FpinntektsmeldingYtelse.FORELDREPENGER, new SaksnummerDto("1234"), LocalDate.of(2024, 9, 5));

        var json = DefaultJsonMapper.treeFromJson(DefaultJsonMapper.toJson(request));

        assertThat(json).isEqualTo(DefaultJsonMapper.treeFromJson("""
            {
              "aktørId": "9999999999999",
              "orgnummer": "999999999",
              "skjæringstidspunkt": "2024-09-01",
              "ytelsetype": "FORELDREPENGER",
              "fagsakSaksnummer": "1234",
              "førsteUttaksdato": "2024-09-05"
            }
            """));
    }

    @Test
    void skal_beholde_organisasjonsnumre_i_gammelt_endepunkt() {
        var request = new OpprettForespørselRequest(new AktørIdDto("9999999999999"), null, LocalDate.of(2024, 9, 1),
            FpinntektsmeldingYtelse.FORELDREPENGER, new SaksnummerDto("1234"), LocalDate.of(2024, 9, 5),
            List.of(new OrganisasjonsnummerDto("999999999")));

        var json = DefaultJsonMapper.treeFromJson(DefaultJsonMapper.toJson(request));

        assertThat(json).isEqualTo(DefaultJsonMapper.treeFromJson("""
            {
              "aktørId": "9999999999999",
              "skjæringstidspunkt": "2024-09-01",
              "ytelsetype": "FORELDREPENGER",
              "fagsakSaksnummer": "1234",
              "førsteUttaksdato": "2024-09-05",
              "organisasjonsnumre": ["999999999"]
            }
            """));
    }

    @ParameterizedTest
    @EnumSource(OpprettForespørselRespons.ForespørselResultat.class)
    void skal_lese_statusobjekt_fra_opprett_en(OpprettForespørselRespons.ForespørselResultat status) {
        var json = """
            {"organisasjonsnummerDto": "999999999", "status": "%s"}
            """.formatted(status);

        var respons = DefaultJsonMapper.fromJson(json, OpprettForespørselRespons.OrganisasjonsnummerMedStatus.class);

        assertThat(respons).isEqualTo(new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(
            new OrganisasjonsnummerDto("999999999"), status));
    }

    @ParameterizedTest
    @EnumSource(OpprettForespørselRespons.ForespørselResultat.class)
    void skal_lese_statusliste_fra_opprett_komplett(OpprettForespørselRespons.ForespørselResultat status) {
        var json = """
            {"organisasjonsnumreMedStatus": [{"organisasjonsnummerDto": "999999999", "status": "%s"}]}
            """.formatted(status);

        var respons = DefaultJsonMapper.fromJson(json, OpprettForespørselRespons.class);

        assertThat(respons.organisasjonsnumreMedStatus()).containsExactly(new OpprettForespørselRespons.OrganisasjonsnummerMedStatus(
            new OrganisasjonsnummerDto("999999999"), status));
    }

    private void mockAlleForespørsler(boolean prod, BehandlingReferanse ref, Skjæringstidspunkt stp,
                                    Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgivere, OpprettForespørselRespons respons) {
        if (prod) {
            when(inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp)).thenReturn(arbeidsgivere);
            when(klient.opprettForespørsel(any())).thenReturn(respons);
        } else {
            when(inntektsmeldingRegisterTjeneste.utledAllePåKrevdeInntektsmeldinger(ref, stp)).thenReturn(arbeidsgivere);
            when(klient.opprettForespørselKomplett(any())).thenReturn(respons);
        }
    }

    private void kjørIMiljø(boolean prod, Runnable handling) {
        var miljø = mock(Environment.class);
        when(miljø.isProd()).thenReturn(prod);
        try (var environment = mockStatic(Environment.class)) {
            environment.when(Environment::current).thenReturn(miljø);
            setup();
            handling.run();
        }
    }

    @Test
    void skal_opprette_task_for_forespørsel_og_lukk_i_riktig_sekvens() {
        // Arrange
        var orgnummer = "999999999";
        var saksnummer = new Saksnummer("1234");

        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(4321L);
        when(behandling.getFagsakId()).thenReturn(1234L);
        when(behandling.getSaksnummer()).thenReturn(saksnummer);

        // Act
        fpInntektsmeldingTjeneste.lagTaskForespørOgLukkBestemtInntektsmelding(behandling, orgnummer);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var tasks = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();

        assertThat(tasks).hasSize(2);

        var forespørselTask = tasks.get(0);
        assertThat(forespørselTask.taskType()).isEqualTo(TaskType.forProsessTask(FpinntektsmeldingTask.class));
        assertThat(forespørselTask.getPropertyValue(FpinntektsmeldingTask.ORGNUMMER)).isEqualTo(orgnummer);

        var lukkTask = tasks.get(1);
        assertThat(lukkTask.taskType()).isEqualTo(TaskType.forProsessTask(LukkForespørslerImTask.class));
        assertThat(lukkTask.getPropertyValue(LukkForespørslerImTask.ORG_NUMMER)).isEqualTo(orgnummer);
        assertThat(lukkTask.getPropertyValue(LukkForespørslerImTask.STATUS)).isEqualTo(ForespørselStatus.UTFØRT.name());
        assertThat(lukkTask.getPropertyValue(LukkForespørslerImTask.SAK_NUMMER)).isEqualTo("1234");
    }

    @Test
    void skal_opprette_én_task_for_forespørsel_uten_lukking() {
        // Arrange
        var orgnummer = "999999999";
        var saksnummer = new Saksnummer("1234");
        var behandlingRef = new BehandlingReferanse(saksnummer, 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);

        // Act
        fpInntektsmeldingTjeneste.lagTaskForespørBestemtInntektsmelding(behandlingRef, orgnummer);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var task = captor.getValue();

        assertThat(task.taskType()).isEqualTo(TaskType.forProsessTask(FpinntektsmeldingTask.class));
        assertThat(task.getPropertyValue(FpinntektsmeldingTask.ORGNUMMER)).isEqualTo(orgnummer);
        assertThat(task.getGruppe()).isEqualTo("FPIM_TASK_1234");
    }

}
