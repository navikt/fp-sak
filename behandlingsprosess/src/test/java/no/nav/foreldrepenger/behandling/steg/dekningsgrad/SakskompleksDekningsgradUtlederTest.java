package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;

class SakskompleksDekningsgradUtlederTest {

    @Test
    void skal_bruke_fagsak_når_dødsdato_er_seks_uker_pluss_en_dag() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6).plusDays(1);

        var familieHendelse = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
            .leggTilBarn(fødselsdato, dødsdato)
            .build();
        var resultat = SakskompleksDekningsgradUtleder.utledFor(Dekningsgrad._80, null, Dekningsgrad._80, null, familieHendelse);

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._80, DekningsgradUtledingResultat.DekningsgradKilde.FAGSAK_RELASJON));
    }

    @Test
    void skal_få_100_når_dødsdato_er_seks_uker_minus_en_dag() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6).minusDays(1);

        var familieHendelse = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
            .leggTilBarn(fødselsdato, dødsdato)
            .build();
        var resultat = SakskompleksDekningsgradUtleder.utledFor(Dekningsgrad._80, null, Dekningsgrad._80, null, familieHendelse);

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._100, DekningsgradUtledingResultat.DekningsgradKilde.DØDSFALL));
    }

    @Test
    void skal_få_100_når_dødsdato_er_seks_uker() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6);

        var familieHendelse = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
            .leggTilBarn(fødselsdato, dødsdato)
            .build();
        var resultat = SakskompleksDekningsgradUtleder.utledFor(Dekningsgrad._80, null, Dekningsgrad._80, null, familieHendelse);

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._100, DekningsgradUtledingResultat.DekningsgradKilde.DØDSFALL));
    }

    @Test
    void skal_få_empty_når_partene_har_ulik_oppgitt() {
        var fødselsdato = LocalDate.now();

        var familieHendelse = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
            .leggTilBarn(fødselsdato)
            .build();
        var resultat = SakskompleksDekningsgradUtleder.utledFor(null, null, Dekningsgrad._80, Dekningsgrad._100, familieHendelse);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_bruke_sakskompleks_når_fagsak_ikke_er_satt() {
        var fødselsdato = LocalDate.now();
        var familieHendelse = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
            .leggTilBarn(fødselsdato)
            .build();
        var resultat = SakskompleksDekningsgradUtleder.utledFor(null, Dekningsgrad._100, Dekningsgrad._80, Dekningsgrad._80, familieHendelse);

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._100, DekningsgradUtledingResultat.DekningsgradKilde.ALLEREDE_FASTSATT));
    }

    @Test
    void skal_bruke_fagsak() {
        var fødselsdato = LocalDate.now();
        var familieHendelse = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
            .leggTilBarn(fødselsdato)
            .build();
        var resultat = SakskompleksDekningsgradUtleder.utledFor(Dekningsgrad._80, Dekningsgrad._100, Dekningsgrad._100, Dekningsgrad._100, familieHendelse);

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._80, DekningsgradUtledingResultat.DekningsgradKilde.FAGSAK_RELASJON));
    }

}
