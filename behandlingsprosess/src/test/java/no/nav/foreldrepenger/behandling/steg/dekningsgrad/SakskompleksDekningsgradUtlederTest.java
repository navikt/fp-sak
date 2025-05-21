package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;

class SakskompleksDekningsgradUtlederTest {

    @Test
    void skal_bruke_fagsak_når_dødsdato_er_seks_uker_pluss_en_dag() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6).plusDays(1);

        var barn = new UidentifisertBarnEntitet(1, fødselsdato, dødsdato);
        var resultat = SakskompleksDekningsgradUtleder.utledFor(Dekningsgrad._80, null, Dekningsgrad._80, null, List.of(barn));

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._80, DekningsgradUtledingResultat.DekningsgradKilde.FAGSAK_RELASJON));
    }

    @Test
    void skal_få_100_når_dødsdato_er_seks_uker_minus_en_dag() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6).minusDays(1);

        var barn = new UidentifisertBarnEntitet(1, fødselsdato, dødsdato);
        var resultat = SakskompleksDekningsgradUtleder.utledFor(Dekningsgrad._80, null, Dekningsgrad._80, null, List.of(barn));

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._100, DekningsgradUtledingResultat.DekningsgradKilde.DØDSFALL));
    }

    @Test
    void skal_få_100_når_dødsdato_er_seks_uker() {
        var fødselsdato = LocalDate.now();
        var dødsdato = fødselsdato.plusWeeks(6);

        var barn = new UidentifisertBarnEntitet(1, fødselsdato, dødsdato);
        var resultat = SakskompleksDekningsgradUtleder.utledFor(Dekningsgrad._80, null, Dekningsgrad._80, null, List.of(barn));

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._100, DekningsgradUtledingResultat.DekningsgradKilde.DØDSFALL));
    }

    @Test
    void skal_få_empty_når_partene_har_ulik_oppgitt() {
        var fødselsdato = LocalDate.now();

        var barn = new UidentifisertBarnEntitet(1, fødselsdato, null);
        var resultat = SakskompleksDekningsgradUtleder.utledFor(null, null, Dekningsgrad._80, Dekningsgrad._100, List.of(barn));

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_bruke_sakskompleks_når_fagsak_ikke_er_satt() {
        var fødselsdato = LocalDate.now();
        var barn = new UidentifisertBarnEntitet(1, fødselsdato, null);

        var resultat = SakskompleksDekningsgradUtleder.utledFor(null, Dekningsgrad._100, Dekningsgrad._80, Dekningsgrad._80, List.of(barn));

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._100, DekningsgradUtledingResultat.DekningsgradKilde.ALLEREDE_FASTSATT));
    }

    @Test
    void skal_bruke_fagsak() {
        var fødselsdato = LocalDate.now();
        var barn = new UidentifisertBarnEntitet(1, fødselsdato, null);

        var resultat = SakskompleksDekningsgradUtleder.utledFor(Dekningsgrad._80, Dekningsgrad._100, Dekningsgrad._100, Dekningsgrad._100, List.of(barn));

        assertThat(resultat).hasValue(new DekningsgradUtledingResultat(Dekningsgrad._80, DekningsgradUtledingResultat.DekningsgradKilde.FAGSAK_RELASJON));
    }

}
