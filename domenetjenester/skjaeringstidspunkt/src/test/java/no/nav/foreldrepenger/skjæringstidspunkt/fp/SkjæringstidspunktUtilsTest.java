package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;

class SkjæringstidspunktUtilsTest {

    private final SkjæringstidspunktUtils innhentingIntervall = new SkjæringstidspunktUtils();

    @Test
    void skal_gi_false_hvis_like() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now();
        var builder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD).medFødselsDato(oppgitt))
            .medBekreftetVersjon(FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET).medFødselsDato(bekreftet));

        var resultat = innhentingIntervall.utledSkjæringstidspunktRegisterinnhenting(builder.build());
        assertThat(resultat).isEqualTo(oppgitt);
    }

    @Test
    void skal_gi_false_hvis_innenfor() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now().plusMonths(1);
        var fhOppgitt = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        fhOppgitt.medTerminbekreftelse(
            fhOppgitt.getTerminbekreftelseBuilder().medTermindato(oppgitt).medNavnPå("aaa").medUtstedtDato(oppgitt.minusWeeks(1)));
        var fhBekreftet = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.OVERSTYRT);
        fhBekreftet.medTerminbekreftelse(
            fhBekreftet.getTerminbekreftelseBuilder().medTermindato(bekreftet).medNavnPå("aaa").medUtstedtDato(oppgitt.minusWeeks(1)));
        var builder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty()).medSøknadVersjon(fhOppgitt).medOverstyrtVersjon(fhBekreftet);

        var resultat = innhentingIntervall.utledSkjæringstidspunktRegisterinnhenting(builder.build());
        assertThat(resultat).isEqualTo(oppgitt);
    }

    @Test
    void skal_gi_true_hvis_før() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now().minusYears(1);

        var fhOppgitt = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        fhOppgitt.medTerminbekreftelse(
            fhOppgitt.getTerminbekreftelseBuilder().medTermindato(oppgitt).medNavnPå("aaa").medUtstedtDato(oppgitt.minusWeeks(1)));
        var fhBekreftet = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET);
        fhBekreftet.medFødselsDato(bekreftet);
        var builder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty()).medSøknadVersjon(fhOppgitt).medBekreftetVersjon(fhBekreftet);

        var resultat = innhentingIntervall.utledSkjæringstidspunktRegisterinnhenting(builder.build());
        assertThat(resultat).isEqualTo(bekreftet);
    }

    @Test
    void skal_gi_true_hvis_etter() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now().plusMonths(13);

        var fhOppgitt = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        fhOppgitt.medAdopsjon(fhOppgitt.getAdopsjonBuilder().medOmsorgsovertakelseDato(oppgitt));
        var fhBekreftet = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.OVERSTYRT);
        fhBekreftet.medAdopsjon(fhBekreftet.getAdopsjonBuilder().medOmsorgsovertakelseDato(bekreftet));
        var builder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty()).medSøknadVersjon(fhOppgitt).medOverstyrtVersjon(fhBekreftet);

        var resultat = innhentingIntervall.utledSkjæringstidspunktRegisterinnhenting(builder.build());
        assertThat(resultat).isEqualTo(bekreftet);
    }
}
