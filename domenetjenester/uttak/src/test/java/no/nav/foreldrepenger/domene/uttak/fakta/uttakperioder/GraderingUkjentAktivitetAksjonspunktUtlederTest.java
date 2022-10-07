package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;


import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakBeregningsandelTjenesteTestUtil;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

public class GraderingUkjentAktivitetAksjonspunktUtlederTest {

    private static final LocalDate FOM = LocalDate.of(2018, 1, 14);
    private static final LocalDate TOM = LocalDate.of(2018, 1, 31);

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final GraderingUkjentAktivitetAksjonspunktUtleder utleder = new GraderingUkjentAktivitetAksjonspunktUtleder(
        new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()));
    private final UttakBeregningsandelTjenesteTestUtil beregningandelUtil = new UttakBeregningsandelTjenesteTestUtil();

    @Test
    public void søktGraderingMedArbeidsforholdSomIkkeFinnesIBeregningSkalGiManuellBehandling_ordinærtArbeid() {
        var arbeidsgiver = arbeidsgiver("orgnr");
        var graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.valueOf(60))
            .medArbeidsgiver(arbeidsgiver)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var søknadsperioder = List.of(graderingPeriode);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(søknadsperioder, false));
        var behandling = scenario.lagre(repositoryProvider);

        var andeler = beregningandelUtil.leggTilOrdinærtArbeid(arbeidsgiver("annet"), InternArbeidsforholdRef.nyRef())
            .hentStatuser();
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null).medBeregningsgrunnlagStatuser(
            andeler);
        var resultat = utleder.utledAksjonspunkterFor(input);
        assertThat(resultat).containsExactly(AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET);
    }

    @Test
    public void søktGraderingMedArbeidsforholdSomIkkeFinnesIBeregningSkalGiManuellBehandling_frilans() {
        var graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.valueOf(60))
            .medGraderingAktivitetType(GraderingAktivitetType.FRILANS)
            .build();

        var søknadsperioder = List.of(graderingPeriode);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(søknadsperioder, false));
        var behandling = scenario.lagre(repositoryProvider);

        var andeler = beregningandelUtil.leggTilOrdinærtArbeid(arbeidsgiver("annet"), InternArbeidsforholdRef.nyRef())
            .hentStatuser();
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null).medBeregningsgrunnlagStatuser(
            andeler);
        var resultat = utleder.utledAksjonspunkterFor(input);
        assertThat(resultat).containsExactly(AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET);
    }

    @Test
    public void søktGraderingMedArbeidsforholdSomIkkeFinnesIBeregningSkalGiManuellBehandling_selvstendig() {
        var graderingPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, TOM)
            .medArbeidsprosent(BigDecimal.valueOf(60))
            .medGraderingAktivitetType(GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build();

        var søknadsperioder = List.of(graderingPeriode);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(søknadsperioder, false));
        var behandling = scenario.lagre(repositoryProvider);

        var andeler = beregningandelUtil.leggTilFrilans().hentStatuser();
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null).medBeregningsgrunnlagStatuser(
            andeler);
        var resultat = utleder.utledAksjonspunkterFor(input);
        assertThat(resultat).containsExactly(AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET);
    }

    private Arbeidsgiver arbeidsgiver(String virksomhetId) {
        return Arbeidsgiver.virksomhet(virksomhetId);
    }
}
