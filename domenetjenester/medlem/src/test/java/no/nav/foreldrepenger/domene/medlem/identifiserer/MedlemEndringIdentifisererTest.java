package no.nav.foreldrepenger.domene.medlem.identifiserer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;

public class MedlemEndringIdentifisererTest {

    @Test
    public void skal_indikere_endring_før_stp() {
        var aggregat = new MedlemskapAggregat(null, Collections.emptySet(), null, null);
        final var set = new HashSet<MedlemskapPerioderEntitet>();
        final var builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(5));
        set.add(builder.build());
        var aggregat1 = new MedlemskapAggregat(null, set, null, null);
        final var identifiserer = new MedlemEndringIdentifiserer();

        assertThat(identifiserer.erEndretFørSkjæringstidspunkt(aggregat, aggregat1, LocalDate.now())).isTrue();
    }

    @Test
    public void skal_ikke_indikere_endring_hele_før_stp() {
        var aggregat = new MedlemskapAggregat(null, Collections.emptySet(), null, null);
        final var set = new HashSet<MedlemskapPerioderEntitet>();
        final var builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(18), LocalDate.now().minusMonths(13));
        set.add(builder.build());
        var aggregat1 = new MedlemskapAggregat(null, set, null, null);
        final var identifiserer = new MedlemEndringIdentifiserer();

        assertThat(identifiserer.erEndretFørSkjæringstidspunkt(aggregat, aggregat1, LocalDate.now())).isFalse();
    }

    @Test
    public void skal_indikere_endring_etter_stp() {
        final var set = new HashSet<MedlemskapPerioderEntitet>();
        final var builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(5));
        set.add(builder.build());
        var aggregat = new MedlemskapAggregat(null, set, null, null);
        final var setEtter = new HashSet<MedlemskapPerioderEntitet>();
        final var builder1 = new MedlemskapPerioderBuilder();
        builder1.medPeriode(LocalDate.now().plusMonths(5), LocalDate.now().plusMonths(15));
        setEtter.add(builder.build());
        setEtter.add(builder1.build());
        var aggregat1 = new MedlemskapAggregat(null, setEtter, null, null);
        final var identifiserer = new MedlemEndringIdentifiserer();

        assertThat(identifiserer.erEndretFørSkjæringstidspunkt(aggregat, aggregat1, LocalDate.now())).isFalse();
    }
}
