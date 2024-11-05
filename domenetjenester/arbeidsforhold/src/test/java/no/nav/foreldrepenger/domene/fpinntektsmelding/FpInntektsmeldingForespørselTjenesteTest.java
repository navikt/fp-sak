package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class FpInntektsmeldingForespørselTjenesteTest {

    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Mock
    private SvangerskapspengerRepository svangerskapspengerRepository;

    private FpInntektsmeldingForespørselTjeneste fpInntektsmeldingForespørselTjeneste;

    @BeforeEach
    void setup() {
        fpInntektsmeldingForespørselTjeneste = new FpInntektsmeldingForespørselTjeneste(skjæringstidspunktTjeneste, ytelsesFordelingRepository, svangerskapspengerRepository);
    }

    @Test
    void skal_lage_request_for_fp_en_periode() {
        // Arrange
        var stp = LocalDate.now();
        var orgnr = "999999999";
        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingRef.behandlingId())).thenReturn(
            Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build());
        var ytelseFordelingAggregat = Mockito.mock(YtelseFordelingAggregat.class);
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriode(stp, stp.plusDays(7))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .build();
        OppgittFordelingEntitet oppgittFordeling = new OppgittFordelingEntitet(Collections.singletonList(periode), false, false);
        when(ytelseFordelingAggregat.getOppgittFordeling()).thenReturn(oppgittFordeling);
        when(ytelsesFordelingRepository.hentAggregat(any())).thenReturn(ytelseFordelingAggregat);

        // Act
        OpprettForespørselRequest request = fpInntektsmeldingForespørselTjeneste.lagForespørsel(behandlingRef, orgnr);

        assertThat(request).isNotNull();
        assertThat(request.aktørId().id()).isEqualTo("9999999999999");
        assertThat(request.fagsakSaksnummer().saksnr()).isEqualTo("1234");
        assertThat(request.ytelsetype()).isEqualTo(OpprettForespørselRequest.YtelseType.FORELDREPENGER);
        assertThat(request.søknadsperioder()).hasSize(1);
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.fom().equals(stp))).isTrue();
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.tom().equals(stp.plusDays(7)))).isTrue();
    }

    @Test
    void skal_lage_request_for_fp_flere_perioder() {
        // Arrange
        var stp = LocalDate.now();
        var orgnr = "999999999";
        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.FORELDREPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingRef.behandlingId())).thenReturn(
            Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build());
        var ytelseFordelingAggregat = Mockito.mock(YtelseFordelingAggregat.class);
        var periode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(stp, stp.plusDays(7))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .build();
        var periode2 = OppgittPeriodeBuilder.ny()
            .medPeriode(stp.plusDays(10), stp.plusDays(15))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .build();
        OppgittFordelingEntitet oppgittFordeling = new OppgittFordelingEntitet(List.of(periode1, periode2), false, false);
        when(ytelseFordelingAggregat.getOppgittFordeling()).thenReturn(oppgittFordeling);
        when(ytelsesFordelingRepository.hentAggregat(any())).thenReturn(ytelseFordelingAggregat);

        // Act
        OpprettForespørselRequest request = fpInntektsmeldingForespørselTjeneste.lagForespørsel(behandlingRef, orgnr);

        assertThat(request).isNotNull();
        assertThat(request.aktørId().id()).isEqualTo("9999999999999");
        assertThat(request.fagsakSaksnummer().saksnr()).isEqualTo("1234");
        assertThat(request.ytelsetype()).isEqualTo(OpprettForespørselRequest.YtelseType.FORELDREPENGER);
        assertThat(request.søknadsperioder()).hasSize(2);
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.fom().equals(stp))).isTrue();
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.tom().equals(stp.plusDays(7)))).isTrue();
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.fom().equals(stp.plusDays(10)))).isTrue();
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.tom().equals(stp.plusDays(15)))).isTrue();

    }

    @Test
    void skal_lage_request_for_svp_en_periode() {
        // Arrange
        var stp = LocalDate.now();
        var orgnr = "999999999";
        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.SVANGERSKAPSPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingRef.behandlingId())).thenReturn(
            Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build());
        var tilretteleggingArbeid = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr)).medBehovForTilretteleggingFom(stp).medIngenTilrettelegging(stp, stp, SvpTilretteleggingFomKilde.SØKNAD).build();

        var grunnlagEntitet = new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(List.of(tilretteleggingArbeid))
            .medBehandlingId(4321L)
            .build();
        when(svangerskapspengerRepository.hentGrunnlag(any())).thenReturn(Optional.of(grunnlagEntitet));

        // Act
        OpprettForespørselRequest request = fpInntektsmeldingForespørselTjeneste.lagForespørsel(behandlingRef, orgnr);

        assertThat(request).isNotNull();
        assertThat(request.aktørId().id()).isEqualTo("9999999999999");
        assertThat(request.fagsakSaksnummer().saksnr()).isEqualTo("1234");
        assertThat(request.ytelsetype()).isEqualTo(OpprettForespørselRequest.YtelseType.SVANGERSKAPSPENGER);
        assertThat(request.søknadsperioder()).hasSize(1);
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.fom().equals(stp))).isTrue();
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.tom().equals(Tid.TIDENES_ENDE))).isTrue();
    }

    @Test
    void skal_lage_request_for_svp_flere_perioder_skal_mappes_til_en_periode() {
        // Arrange
        var stp = LocalDate.now();
        var orgnr = "999999999";
        var behandlingRef = new BehandlingReferanse(new Saksnummer("1234"), 1234L, FagsakYtelseType.SVANGERSKAPSPENGER, 4321L, UUID.randomUUID(),
            BehandlingStatus.UTREDES, BehandlingType.FØRSTEGANGSSØKNAD, 5432L, new AktørId("9999999999999"), RelasjonsRolleType.MORA);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingRef.behandlingId())).thenReturn(
            Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build());
        var tilretteleggingArbeid = new SvpTilretteleggingEntitet.Builder().medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr)).medBehovForTilretteleggingFom(stp)
            .medIngenTilrettelegging(stp, stp, SvpTilretteleggingFomKilde.SØKNAD)
            .medDelvisTilrettelegging(stp.plusDays(10), BigDecimal.valueOf(50), stp, SvpTilretteleggingFomKilde.SØKNAD)
            .build();

        var grunnlagEntitet = new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(List.of(tilretteleggingArbeid))
            .medBehandlingId(4321L)
            .build();
        when(svangerskapspengerRepository.hentGrunnlag(any())).thenReturn(Optional.of(grunnlagEntitet));

        // Act
        OpprettForespørselRequest request = fpInntektsmeldingForespørselTjeneste.lagForespørsel(behandlingRef, orgnr);

        assertThat(request).isNotNull();
        assertThat(request.aktørId().id()).isEqualTo("9999999999999");
        assertThat(request.fagsakSaksnummer().saksnr()).isEqualTo("1234");
        assertThat(request.ytelsetype()).isEqualTo(OpprettForespørselRequest.YtelseType.SVANGERSKAPSPENGER);
        assertThat(request.søknadsperioder()).hasSize(1);
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.fom().equals(stp))).isTrue();
        assertThat(request.søknadsperioder().stream().anyMatch(p-> p.tom().equals(Tid.TIDENES_ENDE))).isTrue();
    }

}
