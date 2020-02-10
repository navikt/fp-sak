package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

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
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class InngangsvilkårOpptjeningTest {

    @Test
    public void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_P5M7D() throws Exception {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pkmantis-1050.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        // litt rart men opptjeningsperiode 2017-08-29 - 2018-01-30 gir 5 måneder og 7 enkeltdager pga regel om at 26 dager = 1 måned
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P5M7D"));

        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(new LocalDateInterval(LocalDate.parse("2017-08-29"), LocalDate.parse("2018-01-30")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);

    }

    @Test
    public void ikke_duplikat_mellom_avslått_periode_og_mellomliggende() throws Exception {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/ingen-mellomliggende.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P9M19D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
    }

    @Test
    public void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_P5M3D() throws Exception {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pkmantis-1050_2.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        // merk siden dette dekker hele januar 2018, blir det mindre opptjening enn i forrige test (P5M7D), som teller noen dager enkeltvis i januar.
        // skyldes 26 dagers regelen.
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P5M3D"));

        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(new LocalDateInterval(LocalDate.parse("2017-08-29"), LocalDate.parse("2018-01-31")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    public void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_P9M18D() {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/fpfeil-1252.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P9M18D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(1);

        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(new LocalDateInterval(LocalDate.parse("2016-10-14"), LocalDate.parse("2017-07-31")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    public void test_frilans_underkjent_med_utlandsk_arbeidshold() {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pfp-6475.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        assertThat(output.getUnderkjentePerioder()).hasSize(1);
    }

    @Test
    public void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_P7M18D_med_utlandsk_arbeidshold() {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pk-53505_1.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P7M18D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(2);

        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(new LocalDateInterval(LocalDate.parse("2016-10-14"), LocalDate.parse("2017-05-31")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    public void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_med_utlandsk_arbeidshold_før_norsk_P4M() {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pk-53505_2.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P9M5D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(2);

        assertThat(output.getResultatTidslinje().getMinLocalDate()).isEqualTo(LocalDate.parse("2017-03-31"));
        assertThat(output.getResultatTidslinje().getMaxLocalDate()).isEqualTo(LocalDate.parse("2018-01-30"));
    }


    @Test
    public void test_beregn_opptjening_fra_vilkår_input_data_som_gir_duplikate_perioder() {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/opptjening-feil.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P10M5D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).hasSize(1);
        assertThat(output.getUnderkjentePerioder()).isEmpty();
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(1);
        assertThat(output.getAntattGodkjentePerioder()).isEmpty();

        assertThat(output.getResultatTidslinje().getMinLocalDate()).isEqualTo(LocalDate.parse("2018-08-15"));
        assertThat(output.getResultatTidslinje().getMaxLocalDate()).isEqualTo(LocalDate.parse("2019-06-14"));
    }

    @Test
    public void test_beregn_opptjening_fra_vilkår_input_data_som_gir_duplikate_perioder_2() {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/opptjening-feil_2.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P10M5D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).hasSize(0);
        assertThat(output.getUnderkjentePerioder()).isEmpty();
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(6);
        assertThat(output.getAntattGodkjentePerioder()).isEmpty();

        assertThat(output.getResultatTidslinje().getMinLocalDate()).isEqualTo(LocalDate.parse("2018-08-11"));
        assertThat(output.getResultatTidslinje().getMaxLocalDate()).isEqualTo(LocalDate.parse("2019-06-10"));
    }

    @Test
    public void avslå_med_kun_utlandsk_arbeidshold_før_norsk() {
        VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();

        URL resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pk-53505_3.json");
        Opptjeningsgrunnlag grunnlag = jsonMapper.readValue(resource, Opptjeningsgrunnlag.class);

        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårUtfallOversetter vilkårUtfallOversetter = new VilkårUtfallOversetter();
        VilkårData vilkårData = vilkårUtfallOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).isEmpty();
    }

}
