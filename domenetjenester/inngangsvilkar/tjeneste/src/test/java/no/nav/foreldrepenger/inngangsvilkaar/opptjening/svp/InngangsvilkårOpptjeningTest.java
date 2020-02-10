package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.LocalDate;
import java.time.Period;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårJsonObjectMapper;
import no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårUtfallOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp.InngangsvilkårOpptjening;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.svp.OpptjeningsvilkårSvangerskapspenger;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class InngangsvilkårOpptjeningTest {

    @Test
    public void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_P5M7D() throws Exception {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        grunnlag.setMinsteAntallDagerGodkjent(28);
        grunnlag.setMinsteAntallMånederGodkjent(0);
        grunnlag.setMinsteAntallDagerForVent(0);
        grunnlag.setMinsteAntallMånederForVent(0);
        grunnlag.setSkalGodkjenneBasertPåAntatt(true);
        grunnlag.setPeriodeAntattGodkjentFørBehandlingstidspunkt(Period.ofYears(1));

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårSvangerskapspenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        // får antatt 12 dager med opptjening i SVP
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P12D"));

        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(new LocalDateInterval(LocalDate.parse("2020-01-25"), LocalDate.parse("2020-02-05")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }
}
