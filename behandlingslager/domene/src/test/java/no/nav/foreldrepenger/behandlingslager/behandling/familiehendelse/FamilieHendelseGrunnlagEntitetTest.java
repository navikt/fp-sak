package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

public class FamilieHendelseGrunnlagEntitetTest {

    /**
     * Scenario er relatert til en feil PFP-5591 hvor ved andre registerdata innhending ble
     * FamilieHendelseType erdret fra OMSORG til ADOPSJON.
     */
    @Test
    public void endring_av_bekreftet_omsorg_familie_hendelse_ved_registerinnhenting() {
        // Grunnlag
        final FamilieHendelseGrunnlagBuilder fhGrunnlagBuilder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty());

        final FamilieHendelseBuilder builderSøknad = opprettNyOmsorgovertagelseFamilieHendelse(HendelseVersjonType.SØKNAD, true);

        fhGrunnlagBuilder.medSøknadVersjon(builderSøknad);
        fhGrunnlagBuilder.medBekreftetVersjon(builderSøknad);

        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = fhGrunnlagBuilder.build();

        //Oppdatering
        FamilieHendelseGrunnlagBuilder oppdateringFhGrunnlagBuilder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.of(familieHendelseGrunnlag));
        FamilieHendelseBuilder oppdateringFhBuilder = FamilieHendelseBuilder.oppdatere(familieHendelseGrunnlag.getBekreftetVersjon(), HendelseVersjonType.BEKREFTET);

        oppdateringFhGrunnlagBuilder.medBekreftetVersjon(oppdateringFhBuilder);
        FamilieHendelseGrunnlagEntitet build = oppdateringFhGrunnlagBuilder.build();

        assertThat(build.getSøknadVersjon()).isNotNull();
        assertThat(build.getBekreftetVersjon()).isNotNull();
        assertThat(build.getOverstyrtVersjon()).isEqualTo(Optional.empty());
        assertThat(build.getSøknadVersjon().getType()).isEqualTo(FamilieHendelseType.OMSORG);
        assertThat(build.getBekreftetVersjon().get().getType()).isEqualTo(FamilieHendelseType.OMSORG);
    }

    private FamilieHendelseBuilder opprettNyOmsorgovertagelseFamilieHendelse(HendelseVersjonType type, boolean erOmsorgovertakelse) {
        final FamilieHendelseBuilder familieHendelseBuilder = FamilieHendelseBuilder.ny(type);
        final int antallBarn = 1;
        LocalDate ommsorgsovertakelseDato = LocalDate.of(2015, 7, 12);

        final FamilieHendelseBuilder.AdopsjonBuilder adopsjonBuilder = familieHendelseBuilder.getAdopsjonBuilder()
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
