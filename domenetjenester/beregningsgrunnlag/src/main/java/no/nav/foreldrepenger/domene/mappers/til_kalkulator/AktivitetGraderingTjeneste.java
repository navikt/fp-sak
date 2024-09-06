package no.nav.foreldrepenger.domene.mappers.til_kalkulator;

import static no.nav.foreldrepenger.domene.mappers.til_kalkulator.IAYMapperTilKalkulus.mapArbeidsgiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.modell.typer.Aktivitetsgrad;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.prosess.BeregningGraderingTjeneste;
import no.nav.foreldrepenger.domene.prosess.PeriodeMedGradering;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;

/**
 * Tjeneste som brukes av beregning for å populere beregningsgrunnlagInput med uttaksdata
 */
@ApplicationScoped
public class AktivitetGraderingTjeneste {
    private BeregningGraderingTjeneste beregningGraderingTjeneste;

    AktivitetGraderingTjeneste() {
        // CDI
    }

    @Inject
    public AktivitetGraderingTjeneste(BeregningGraderingTjeneste beregningGraderingTjeneste) {
        this.beregningGraderingTjeneste = beregningGraderingTjeneste;
    }


    public AktivitetGradering finnAktivitetGraderingerKalkulus(BehandlingReferanse ref) {
        return new AktivitetGradering(utled(ref));
    }

    private List<AndelGradering> utled(BehandlingReferanse ref) {
        var perioderMedGradering = beregningGraderingTjeneste.finnPerioderMedGradering(ref);
        return map(perioderMedGradering);
    }

    private List<AndelGradering> map(List<PeriodeMedGradering> perioderMedGradering) {
        Map<AndelGradering, AndelGradering.Builder> map = new HashMap<>();
        perioderMedGradering.forEach(periodeMedGradering -> {
            var aktivitetStatus = periodeMedGradering.aktivitetStatus();
            var nyBuilder = AndelGradering.builder()
                .medStatus(no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus.fraKode(aktivitetStatus.getKode()));
            if (AktivitetStatus.ARBEIDSTAKER.equals(aktivitetStatus)) {
                var arbeidsgiver = Objects.requireNonNull(periodeMedGradering.arbeidsgiver(), "arbeidsgiver");
                nyBuilder.medArbeidsgiver(mapArbeidsgiver(arbeidsgiver));
            }
            var andelGradering = nyBuilder.build();

            var builder = map.get(andelGradering);
            if (builder == null) {
                builder = nyBuilder;
                map.put(andelGradering, nyBuilder);
            }

            builder.leggTilGradering(mapGradering(periodeMedGradering));
        });
        return new ArrayList<>(map.keySet());
    }

    private AndelGradering.Gradering mapGradering(PeriodeMedGradering periodeMedGradering) {
        return new AndelGradering.Gradering(periodeMedGradering.fom(), periodeMedGradering.tom(), Aktivitetsgrad.fra(periodeMedGradering.arbeidsprosent()));
    }
}
