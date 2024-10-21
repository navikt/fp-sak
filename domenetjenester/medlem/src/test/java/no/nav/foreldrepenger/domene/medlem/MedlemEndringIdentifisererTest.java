package no.nav.foreldrepenger.domene.medlem;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class MedlemEndringIdentifisererTest {

    @Test
    void skal_indikere_endring_før_stp() {
        var aggregat = new MedlemskapAggregat(Collections.emptySet(), null);
        var set = new HashSet<MedlemskapPerioderEntitet>();
        var builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(5));
        set.add(builder.build());
        var aggregat1 = new MedlemskapAggregat(set, null);

        assertThat(MedlemEndringIdentifiserer.erEndretForPeriode(aggregat, aggregat1, DatoIntervallEntitet.fraOgMed(LocalDate.now()))).isTrue();
    }

    @Test
    void skal_ikke_indikere_endring_hele_før_stp() {
        var aggregat = new MedlemskapAggregat(Collections.emptySet(), null);
        var set = new HashSet<MedlemskapPerioderEntitet>();
        var builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(18), LocalDate.now().minusMonths(13));
        set.add(builder.build());
        var aggregat1 = new MedlemskapAggregat(set, null);

        assertThat(MedlemEndringIdentifiserer.erEndretForPeriode(aggregat, aggregat1, DatoIntervallEntitet.fraOgMed(LocalDate.now()))).isFalse();
    }

    @Test
    void skal_indikere_endring_etter_stp() {
        var set = new HashSet<MedlemskapPerioderEntitet>();
        var builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(5));
        set.add(builder.build());
        var aggregat = new MedlemskapAggregat(set, null);
        var setEtter = new HashSet<MedlemskapPerioderEntitet>();
        var builder1 = new MedlemskapPerioderBuilder();
        builder1.medPeriode(LocalDate.now().plusMonths(5), LocalDate.now().plusMonths(15));
        setEtter.add(builder.build());
        setEtter.add(builder1.build());
        var aggregat1 = new MedlemskapAggregat(setEtter, null);

        assertThat(MedlemEndringIdentifiserer.erEndretForPeriode(aggregat, aggregat1, DatoIntervallEntitet.fraOgMed(LocalDate.now()))).isTrue();
    }
}
