package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårJsonObjectMapper;
import no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårUtfallOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp.InngangsvilkårOpptjening;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.svp.OpptjeningsvilkårSvangerskapspenger;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class InngangsvilkårOpptjeningTest {

    @Test
    public void test_beregn_opptjening_fra_periode_over_mndskifte_godkjenn_antatt_case1() throws Exception {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-wait-1.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        grunnlag.setMinsteAntallDagerGodkjent(28);
        grunnlag.setMinsteAntallMånederGodkjent(0);
        grunnlag.setMinsteAntallDagerForVent(0);
        grunnlag.setMinsteAntallMånederForVent(0);
        grunnlag.setSkalGodkjenneBasertPåAntatt(true);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårSvangerskapspenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        // Skal vente til 5te i neste mnd på inntektsregistrering
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P28D"));

        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(new LocalDateInterval(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-02-05")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    public void test_beregn_opptjening_fra_periode_over_mndskifte_godkjenn_antatt_case2() throws Exception {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-wait.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        grunnlag.setMinsteAntallDagerGodkjent(28);
        grunnlag.setMinsteAntallMånederGodkjent(0);
        grunnlag.setMinsteAntallDagerForVent(0);
        grunnlag.setMinsteAntallMånederForVent(0);
        grunnlag.setSkalGodkjenneBasertPåAntatt(true);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårSvangerskapspenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        // Skal vente til 5te i neste mnd på inntektsregistrering
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P28D"));

        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(new LocalDateInterval(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-02-05")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    public void test_beregn_opptjening_fra_periode_over_mndskifte_avslag_case2() throws Exception {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-deny.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        grunnlag.setMinsteAntallDagerGodkjent(28);
        grunnlag.setMinsteAntallMånederGodkjent(0);
        grunnlag.setMinsteAntallDagerForVent(0);
        grunnlag.setMinsteAntallMånederForVent(0);
        grunnlag.setSkalGodkjenneBasertPåAntatt(true);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårSvangerskapspenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        // Skal vente til 5te i neste mnd på inntektsregistrering
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P28D"));

        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(new LocalDateInterval(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-02-05")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    public void test_aktivitet_første_og_siste() throws Exception {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-broken.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        grunnlag.setMinsteAntallDagerGodkjent(28);
        grunnlag.setMinsteAntallMånederGodkjent(0);
        grunnlag.setMinsteAntallDagerForVent(0);
        grunnlag.setMinsteAntallMånederForVent(0);
        grunnlag.setSkalGodkjenneBasertPåAntatt(true);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårSvangerskapspenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        // får antatt 2 dager med opptjening i SVP
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P2D"));

        var første = LocalDate.of(2020, 1,9);
        var siste = LocalDate.of(2020, 2,5);
        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(Set.of(new LocalDateSegment<>(new LocalDateInterval(første, første), Boolean.TRUE), new LocalDateSegment<>(new LocalDateInterval(siste, siste), Boolean.TRUE)));
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    public void test_beregn_opptjening_nok_aktivitet() throws Exception {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-ok.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        grunnlag.setMinsteAntallDagerGodkjent(28);
        grunnlag.setMinsteAntallMånederGodkjent(0);
        grunnlag.setMinsteAntallDagerForVent(0);
        grunnlag.setMinsteAntallMånederForVent(0);
        grunnlag.setSkalGodkjenneBasertPåAntatt(true);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårSvangerskapspenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        // får antatt 12 dager med opptjening i SVP
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P28D"));

        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(new LocalDateInterval(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-02-05")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }
}
