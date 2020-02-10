package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.FORELDREPENGER;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;

/**
 * Default test scenario builder for Mor søker Foreldrepenger. Kan opprettes for fødsel og brukes til å
 * opprette standard scenarioer.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
public class ScenarioForeldrepenger extends AbstractTestScenario<ScenarioForeldrepenger> {

    private ScenarioForeldrepenger(RelasjonsRolleType relasjonRolle, NavBrukerKjønn kjønn) {
        super(FORELDREPENGER, relasjonRolle, kjønn);
    }

    public static ScenarioForeldrepenger nyttScenario() {
        return new ScenarioForeldrepenger(RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
    }

    public BeregningAktivitetAggregatEntitet.Builder medBeregningAktiviteter() {
        BeregningAktivitetScenario beregningAktivitetScenario = new BeregningAktivitetScenario();
        leggTilScenario(beregningAktivitetScenario);
        return beregningAktivitetScenario.getBeregningAktiviteterBuilder();
    }

    public BeregningsgrunnlagEntitet.Builder medBeregningsgrunnlag() {
        BeregningsgrunnlagScenario beregningsgrunnlagScenario = new BeregningsgrunnlagScenario();
        leggTilScenario(beregningsgrunnlagScenario);
        return beregningsgrunnlagScenario.getBeregningsgrunnlagBuilder();
    }

    public static ScenarioForeldrepenger nyttScenarioFar() {
        return new ScenarioForeldrepenger(RelasjonsRolleType.FARA, NavBrukerKjønn.MANN);
    }


}
