package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;

public class SkjæringstidspunktUtilsTest {

    private SkjæringstidspunktUtils innhentingIntervall = new SkjæringstidspunktUtils(Period.parse("P10M"), Period.parse("P12W"), Period.parse("P4M"), Period.parse("P1Y"));

    @Test
    public void skal_gi_false_hvis_like() {
        LocalDate oppgitt = LocalDate.now();
        LocalDate bekreftet = LocalDate.now();
        var builder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(FamilieHendelseBuilder.oppdatere(Optional.empty(),HendelseVersjonType.SØKNAD).medFødselsDato(oppgitt))
            .medBekreftetVersjon(FamilieHendelseBuilder.oppdatere(Optional.empty(),HendelseVersjonType.BEKREFTET).medFødselsDato(bekreftet));

        LocalDate resultat = innhentingIntervall.utledSkjæringstidspunktRegisterinnhenting(builder.build());
        assertThat(resultat).isEqualTo(oppgitt);
    }

    @Test
    public void skal_gi_false_hvis_innenfor() {
        LocalDate oppgitt = LocalDate.now();
        LocalDate bekreftet = LocalDate.now().plusMonths(1);
        var fhOppgitt = FamilieHendelseBuilder.oppdatere(Optional.empty(),HendelseVersjonType.SØKNAD);
        fhOppgitt.medTerminbekreftelse(fhOppgitt.getTerminbekreftelseBuilder().medTermindato(oppgitt).medNavnPå("aaa").medUtstedtDato(oppgitt.minusWeeks(1)));
        var fhBekreftet = FamilieHendelseBuilder.oppdatere(Optional.empty(),HendelseVersjonType.OVERSTYRT);
        fhBekreftet.medTerminbekreftelse(fhBekreftet.getTerminbekreftelseBuilder().medTermindato(bekreftet).medNavnPå("aaa").medUtstedtDato(oppgitt.minusWeeks(1)));
        var builder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(fhOppgitt)
            .medOverstyrtVersjon(fhBekreftet);

        LocalDate resultat = innhentingIntervall.utledSkjæringstidspunktRegisterinnhenting(builder.build());
        assertThat(resultat).isEqualTo(oppgitt);
    }

    @Test
    public void skal_gi_true_hvis_før() {
        LocalDate oppgitt = LocalDate.now();
        LocalDate bekreftet = LocalDate.now().minusYears(1);

        var fhOppgitt = FamilieHendelseBuilder.oppdatere(Optional.empty(),HendelseVersjonType.SØKNAD);
        fhOppgitt.medTerminbekreftelse(fhOppgitt.getTerminbekreftelseBuilder().medTermindato(oppgitt).medNavnPå("aaa").medUtstedtDato(oppgitt.minusWeeks(1)));
        var fhBekreftet = FamilieHendelseBuilder.oppdatere(Optional.empty(),HendelseVersjonType.BEKREFTET);
        fhBekreftet.medFødselsDato(bekreftet);
        var builder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(fhOppgitt)
            .medBekreftetVersjon(fhBekreftet);

        LocalDate resultat = innhentingIntervall.utledSkjæringstidspunktRegisterinnhenting(builder.build());
        assertThat(resultat).isEqualTo(bekreftet);
    }

    @Test
    public void skal_gi_true_hvis_etter() {
        LocalDate oppgitt = LocalDate.now();
        LocalDate bekreftet = LocalDate.now().plusMonths(13);

        var fhOppgitt = FamilieHendelseBuilder.oppdatere(Optional.empty(),HendelseVersjonType.SØKNAD);
        fhOppgitt.medAdopsjon(fhOppgitt.getAdopsjonBuilder().medOmsorgsovertakelseDato(oppgitt));
        var fhBekreftet = FamilieHendelseBuilder.oppdatere(Optional.empty(),HendelseVersjonType.OVERSTYRT);
        fhBekreftet.medAdopsjon(fhBekreftet.getAdopsjonBuilder().medOmsorgsovertakelseDato(bekreftet));
        var builder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(fhOppgitt)
            .medOverstyrtVersjon(fhBekreftet);

        LocalDate resultat = innhentingIntervall.utledSkjæringstidspunktRegisterinnhenting(builder.build());
        assertThat(resultat).isEqualTo(bekreftet);
    }
}
