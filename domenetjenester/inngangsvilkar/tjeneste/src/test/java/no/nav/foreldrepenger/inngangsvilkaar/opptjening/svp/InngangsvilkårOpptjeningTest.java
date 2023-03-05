package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp.InngangsvilkårOpptjening;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelYtelse;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class InngangsvilkårOpptjeningTest {

    @Test
    void test_beregn_opptjening_fra_periode_over_mndskifte_godkjenn_antatt_case1() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-wait-1.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.SVANGERSKAPSPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        // Skal vente til 5te i neste mnd på inntektsregistrering
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P28D"));

        var expectedTimeline = new LocalDateTimeline<Boolean>(new LocalDateInterval(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-02-05")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    void test_beregn_opptjening_fra_periode_over_mndskifte_godkjenn_antatt_case2() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-wait.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.SVANGERSKAPSPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        // Skal vente til 5te i neste mnd på inntektsregistrering
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P28D"));

        var expectedTimeline = new LocalDateTimeline<Boolean>(new LocalDateInterval(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-02-05")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    void test_beregn_opptjening_fra_periode_over_mndskifte_avslag_case2() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-deny.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.SVANGERSKAPSPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        // Skal vente til 5te i neste mnd på inntektsregistrering
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P28D"));

        var expectedTimeline = new LocalDateTimeline<Boolean>(new LocalDateInterval(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-02-05")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    void test_aktivitet_første_og_siste() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-broken.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.SVANGERSKAPSPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);

        // får antatt 2 dager med opptjening i SVP
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P2D"));

        var første = LocalDate.of(2020, 1,9);
        var siste = LocalDate.of(2020, 2,5);
        var expectedTimeline = new LocalDateTimeline<Boolean>(Set.of(new LocalDateSegment<>(new LocalDateInterval(første, første), Boolean.TRUE), new LocalDateSegment<>(new LocalDateInterval(siste, siste), Boolean.TRUE)));
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }

    @Test
    void test_beregn_opptjening_nok_aktivitet() {
        var resource = InngangsvilkårOpptjening.class.getResource("/opptjening/TFP-2566-ok.json");
        var grunnlag = StandardJsonConfig.fromJson(resource, Opptjeningsgrunnlag.class);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.SVANGERSKAPSPENGER, grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);

        var output = (OpptjeningsvilkårResultat) vilkårData.ekstraVilkårresultat();

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        // får antatt 12 dager med opptjening i SVP
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.parse("P28D"));

        var expectedTimeline = new LocalDateTimeline<Boolean>(new LocalDateInterval(LocalDate.parse("2020-01-09"), LocalDate.parse("2020-02-05")), Boolean.TRUE);
        assertThat(output.getResultatTidslinje()).isEqualTo(expectedTimeline);
    }
}
