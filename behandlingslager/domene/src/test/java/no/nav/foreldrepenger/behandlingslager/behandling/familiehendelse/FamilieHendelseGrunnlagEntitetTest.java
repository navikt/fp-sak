package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class FamilieHendelseGrunnlagEntitetTest {

    /**
     * Scenario er relatert til en feil PFP-5591 hvor ved andre registerdata
     * innhending ble FamilieHendelseType erdret fra OMSORG til ADOPSJON.
     */
    @Test
    public void endring_av_bekreftet_omsorg_familie_hendelse_ved_registerinnhenting() {
        // Grunnlag
        final var fhGrunnlagBuilder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty());

        final var builderSøknad = opprettNyOmsorgovertagelseFamilieHendelse(HendelseVersjonType.SØKNAD, true);

        fhGrunnlagBuilder.medSøknadVersjon(builderSøknad);
        fhGrunnlagBuilder.medBekreftetVersjon(builderSøknad);

        var familieHendelseGrunnlag = fhGrunnlagBuilder.build();

        // Oppdatering
        var oppdateringFhGrunnlagBuilder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.of(familieHendelseGrunnlag));
        var oppdateringFhBuilder = FamilieHendelseBuilder.oppdatere(familieHendelseGrunnlag.getBekreftetVersjon(),
                HendelseVersjonType.BEKREFTET);

        oppdateringFhGrunnlagBuilder.medBekreftetVersjon(oppdateringFhBuilder);
        var build = oppdateringFhGrunnlagBuilder.build();

        assertThat(build.getSøknadVersjon()).isNotNull();
        assertThat(build.getBekreftetVersjon()).isNotNull();
        assertThat(build.getOverstyrtVersjon()).isEqualTo(Optional.empty());
        assertThat(build.getSøknadVersjon().getType()).isEqualTo(FamilieHendelseType.OMSORG);
        assertThat(build.getBekreftetVersjon().get().getType()).isEqualTo(FamilieHendelseType.OMSORG);
    }

    private FamilieHendelseBuilder opprettNyOmsorgovertagelseFamilieHendelse(HendelseVersjonType type, boolean erOmsorgovertakelse) {
        final var familieHendelseBuilder = FamilieHendelseBuilder.ny(type);
        final var antallBarn = 1;
        var ommsorgsovertakelseDato = LocalDate.of(2015, 7, 12);

        final var adopsjonBuilder = familieHendelseBuilder.getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(ommsorgsovertakelseDato);

        if (erOmsorgovertakelse) {
            familieHendelseBuilder.erOmsorgovertagelse();
        }

        familieHendelseBuilder.medAdopsjon(adopsjonBuilder);
        familieHendelseBuilder.medAntallBarn(antallBarn);
        familieHendelseBuilder.leggTilBarn(ommsorgsovertakelseDato);

        return familieHendelseBuilder;
    }
}
