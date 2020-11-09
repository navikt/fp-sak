package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;


import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class GraderingAktivitetUtenBGAksjonspunktUtlederTest {

    private final GraderingAktivitetUtenBGAksjonspunktUtleder utleder = new GraderingAktivitetUtenBGAksjonspunktUtleder();

    @Test
    public void skalUtledeAksjonspunktHvisDetFinnesAndelerUtenBeregningsgrunnlag(EntityManager entityManager) {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medErArbeidstaker(true)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        var søknadsperioder = List.of(søknadsperiode);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(søknadsperioder, false));
        var behandling = scenario.lagre(new UttakRepositoryProvider(entityManager));

        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null)
            .medFinnesAndelerMedGraderingUtenBeregningsgrunnlag(true);
        var resultat = utleder.utledAksjonspunkterFor(input);
        assertThat(resultat).containsExactly(AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG);
    }
}
