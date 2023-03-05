package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;

class AdopsjonTest {

    @Test
    void skal_merge_fra_gammel_til_ny_adopsjon() {
        final var now = LocalDate.now();

        final var hendelseAggregat = byggAggregat(now);
        final var søknadVersjon = hendelseAggregat.getGjeldendeVersjon();

        assertThat(søknadVersjon.getAntallBarn()).isEqualTo(2);
        assertThat(søknadVersjon.getBarna()).hasSize(2);
        assertThat(søknadVersjon.getBarna().stream().map(UidentifisertBarn::getFødselsdato)).containsExactly(now, now.minusDays(2));
        assertThat(søknadVersjon.getType()).isEqualTo(FamilieHendelseType.ADOPSJON);
        assertThat(søknadVersjon.getAdopsjon()).isPresent();
        assertThat(søknadVersjon.getAdopsjon().get().getOmsorgsovertakelseDato()).isEqualTo(now);
    }

    @Test
    void skal_merge_fra_gammel_til_ny_adopsjon_med_oppdaterte_verdier() {
        final var now = LocalDate.now();
        final var hendelseAggregat = byggAggregat(now);
        final var søknadVersjon = hendelseAggregat.getGjeldendeVersjon();

        assertThat(hendelseAggregat.getHarBekreftedeData()).isFalse();
        assertThat(søknadVersjon.getAntallBarn()).isEqualTo(2);
        assertThat(søknadVersjon.getBarna()).hasSize(2);
        assertThat(søknadVersjon.getBarna().stream().map(UidentifisertBarn::getFødselsdato)).containsExactly(now, now.minusDays(2));
        assertThat(søknadVersjon.getType()).isEqualTo(FamilieHendelseType.ADOPSJON);
        assertThat(søknadVersjon.getAdopsjon()).isPresent();
        assertThat(søknadVersjon.getAdopsjon().get().getOmsorgsovertakelseDato()).isEqualTo(now);

        final var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.of(hendelseAggregat));
        final var oppdatertHendelse = FamilieHendelseBuilder.oppdatere(Optional.of(hendelseAggregat.getSøknadVersjon()),
                HendelseVersjonType.SØKNAD);

        oppdatertHendelse.tilbakestillBarn();
        oppdatertHendelse.leggTilBarn(now.minusYears(1));
        oppdatertHendelse.leggTilBarn(now.minusYears(2));
        final var oppdatertHendelseAggregat = oppdatere.medBekreftetVersjon(oppdatertHendelse).build();
        final var gjellendeVersjon = oppdatertHendelseAggregat.getGjeldendeVersjon();

        assertThat(oppdatertHendelseAggregat.getHarBekreftedeData()).isTrue();
        assertThat(gjellendeVersjon.getAntallBarn()).isEqualTo(2);
        assertThat(gjellendeVersjon.getBarna()).hasSize(2);
        assertThat(gjellendeVersjon.getBarna().stream().map(UidentifisertBarn::getFødselsdato)).containsExactly(now.minusYears(1), now.minusYears(2));
        assertThat(gjellendeVersjon.getType()).isEqualTo(FamilieHendelseType.ADOPSJON);
        assertThat(gjellendeVersjon.getAdopsjon()).isPresent();
        assertThat(gjellendeVersjon.getAdopsjon().get().getOmsorgsovertakelseDato()).isEqualTo(now);
    }

    private FamilieHendelseGrunnlagEntitet byggAggregat(LocalDate now) {
        Map<Integer, LocalDate> fødselsdatoer = new HashMap<>();
        fødselsdatoer.put(1, now);
        fødselsdatoer.put(2, now.minusDays(2));

        final var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty());
        var builder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        final var adopsjonBuilder = builder.getAdopsjonBuilder();
        adopsjonBuilder.medOmsorgsovertakelseDato(now);
        for (var localDate : fødselsdatoer.values()) {
            builder.leggTilBarn(localDate);
        }
        builder.medAdopsjon(adopsjonBuilder);
        return oppdatere.medSøknadVersjon(builder).build();
    }
}
