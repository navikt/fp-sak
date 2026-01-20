package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.prosess.BeregningGraderingTjeneste;
import no.nav.foreldrepenger.domene.prosess.PeriodeMedGradering;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.YtelsespesifiktGrunnlagDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.foreldrepenger.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.foreldrepenger.gradering.AktivitetGraderingDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.foreldrepenger.gradering.AndelGraderingDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.foreldrepenger.gradering.GraderingDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Aktivitetsgrad;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Aktør;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.AktørIdPersonident;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Organisasjon;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Periode;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class MapKalkulusYtelsegrunnlagFP implements MapKalkulusYtelsegrunnlag {
    private DekningsgradTjeneste dekningsgradTjeneste;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private BeregningGraderingTjeneste beregningGraderingTjeneste;

    MapKalkulusYtelsegrunnlagFP() {
        // CDI
    }

    @Inject
    public MapKalkulusYtelsegrunnlagFP(DekningsgradTjeneste dekningsgradTjeneste,
                                       BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                                       BeregningGraderingTjeneste beregningGraderingTjeneste) {
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.beregningGraderingTjeneste = beregningGraderingTjeneste;
    }

    @Override
    public YtelsespesifiktGrunnlagDto mapYtelsegrunnlag(BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        var aktivitetGraderinger = finnAktivitetGraderingerKalkulus(referanse);
        var dekningsgrad = BigDecimal.valueOf(dekningsgradTjeneste.finnGjeldendeDekningsgrad(referanse).getVerdi());
        var kanBesteberegnes = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(referanse, stp);
        var ytelsegrunlagDto = besteberegningFødendeKvinneTjeneste.lagYtelsegrunnlagKalkulus(referanse, stp);
        return new ForeldrepengerGrunnlag(dekningsgrad, kanBesteberegnes, aktivitetGraderinger, ytelsegrunlagDto, stp.getFørsteUttaksdatoGrunnbeløp());
    }

    public AktivitetGraderingDto finnAktivitetGraderingerKalkulus(BehandlingReferanse ref) {
        var periodeMedGradering = beregningGraderingTjeneste.finnPerioderMedGradering(ref);
        return periodeMedGradering.isEmpty() ? null : new AktivitetGraderingDto(mapTilDto(periodeMedGradering));
    }

    private List<AndelGraderingDto> mapTilDto(List<PeriodeMedGradering> perioderMedGradering) {
        Map<AktivitetNøkkel, List<GraderingDto>> map = new HashMap<>();
        perioderMedGradering.forEach(periodeMedGradering -> {
            var nøkkel = new AktivitetNøkkel(periodeMedGradering.aktivitetStatus(), periodeMedGradering.arbeidsgiver());
            var graderingDto = new GraderingDto(new Periode(periodeMedGradering.fom(), periodeMedGradering.tom()),
                new Aktivitetsgrad(periodeMedGradering.arbeidsprosent()));
            if (map.containsKey(nøkkel)) {
                map.get(nøkkel).add(graderingDto);
            } else {
                map.put(nøkkel, new ArrayList<>(List.of(graderingDto)));
            }
        });
        return map.entrySet().stream().map(entry -> {
            var aktivitetStatus = KodeverkTilKalkulusMapper.mapAktivitetstatus(entry.getKey().aktivitetStatus());
            var arbeidsgiver = mapTilAktør(entry.getKey().arbeidsgiver());
            return new AndelGraderingDto(aktivitetStatus, arbeidsgiver, null, entry.getValue());
        }).toList();
    }

    private static Aktør mapTilAktør(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getOrgnr()) : new AktørIdPersonident(arbeidsgiver.getAktørId().getId());
    }

    record AktivitetNøkkel(AktivitetStatus aktivitetStatus, Arbeidsgiver arbeidsgiver) {}

}
