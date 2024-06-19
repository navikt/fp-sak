package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelYtelse;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class InngangsvilkårOpptjeningTest {

    @Test
    void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_P5M7D() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pkmantis-1050.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        // litt rart men opptjeningsperiode 2017-08-29 - 2018-01-30 gir 5 måneder og 7 enkeltdager pga regel om at 26 dager = 1 måned
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P5M7D"));

        var expectedTimeline = new LocalDateTimeline<Boolean>(new LocalDateInterval(LocalDate.parse("2017-08-29"), LocalDate.parse("2018-01-30")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);

    }

    @Test
    void ikke_duplikat_mellom_avslått_periode_og_mellomliggende() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/ingen-mellomliggende.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P9M19D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
    }

    @Test
    void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_P5M3D() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pkmantis-1050_2.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        // merk siden dette dekker hele januar 2018, blir det mindre opptjening enn i forrige test (P5M7D), som teller noen dager enkeltvis i januar.
        // skyldes 26 dagers regelen.
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P5M3D"));

        var expectedTimeline = new LocalDateTimeline<Boolean>(new LocalDateInterval(LocalDate.parse("2017-08-29"), LocalDate.parse("2018-01-31")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_P9M18D() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/fpfeil-1252.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P9M18D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(1);

        var expectedTimeline = new LocalDateTimeline<Boolean>(new LocalDateInterval(LocalDate.parse("2016-10-14"), LocalDate.parse("2017-07-31")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    void test_frilans_underkjent_med_utlandsk_arbeidshold() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pfp-6475.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        assertThat(output.getUnderkjentePerioder()).hasSize(1);
    }

    @Test
    void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_P7M18D_med_utlandsk_arbeidshold() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pk-53505_1.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P7M18D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(2);

        var expectedTimeline = new LocalDateTimeline<Boolean>(new LocalDateInterval(LocalDate.parse("2016-10-14"), LocalDate.parse("2017-05-31")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    void test_beregn_opptjening_fra_vilkår_input_data_som_gir_opptjening_med_utlandsk_arbeidshold_før_norsk_P4M() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pk-53505_2.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P9M5D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(2);

        assertThat(output.getResultatTidslinje().getMinLocalDate()).isEqualTo(LocalDate.parse("2017-03-31"));
        assertThat(output.getResultatTidslinje().getMaxLocalDate()).isEqualTo(LocalDate.parse("2018-01-30"));
    }


    @Test
    void test_beregn_opptjening_fra_vilkår_input_data_som_gir_duplikate_perioder() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/opptjening-feil.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P10M5D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).hasSize(1);
        assertThat(output.getUnderkjentePerioder()).isEmpty();
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(1);
        assertThat(output.getAntattGodkjentePerioder()).isEmpty();

        assertThat(output.getResultatTidslinje().getMinLocalDate()).isEqualTo(LocalDate.parse("2018-08-15"));
        assertThat(output.getResultatTidslinje().getMaxLocalDate()).isEqualTo(LocalDate.parse("2019-06-14"));
    }

    @Test
    void test_beregn_opptjening_fra_vilkår_input_data_som_gir_duplikate_perioder_2() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/opptjening-feil_2.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P10M5D"));
        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).isEmpty();
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(6);
        assertThat(output.getAntattGodkjentePerioder()).isEmpty();

        assertThat(output.getResultatTidslinje().getMinLocalDate()).isEqualTo(LocalDate.parse("2018-08-11"));
        assertThat(output.getResultatTidslinje().getMaxLocalDate()).isEqualTo(LocalDate.parse("2019-06-10"));
    }

    @Test
    void avslå_med_kun_utlandsk_arbeidshold_før_norsk() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/pk-53505_3.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).isEmpty();
    }

    @Test
    void oppfylt_med_nok_arbeid_frilans() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-4174-nok-frilans.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(2);
        assertThat(output.getResultatTidslinje().getMinLocalDate()).isEqualTo(LocalDate.parse("2020-07-01"));
        assertThat(output.getResultatTidslinje().getMaxLocalDate()).isEqualTo(LocalDate.parse("2021-02-28"));
    }


    @Test
    void avslag_mangler_arbeid_frilans() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-4174-mangler-frilans.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(2);
        assertThat(output.getResultatTidslinje().getMinLocalDate()).isEqualTo(LocalDate.parse("2020-07-01"));
        assertThat(output.getResultatTidslinje().getMaxLocalDate()).isEqualTo(LocalDate.parse("2021-02-28"));
    }

    @Test
    void aapen_frilans_mangler_maaned_deny() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-4174-aapen-frilans-deny.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.FORELDREPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();
        assertThat(output.getUnderkjentePerioder()).hasSize(1);
        assertThat(output.getBekreftetGodkjentePerioder()).hasSize(1);
        assertThat(output.getResultatTidslinje().getMinLocalDate()).isEqualTo(LocalDate.parse("2020-10-01"));
        assertThat(output.getResultatTidslinje().getMaxLocalDate()).isEqualTo(LocalDate.parse("2021-02-28"));
    }

}
