package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakYrkesaktiviteter;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Arbeid;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.EndringAvStilling;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ArbeidGrunnlagBygger {

    private YtelsesFordelingRepository ytelsesfordelingRepository;


    @Inject
    public ArbeidGrunnlagBygger(UttakRepositoryProvider repositoryProvider) {
        this.ytelsesfordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    }

    ArbeidGrunnlagBygger() {
        // CDI
    }

    public Arbeid.Builder byggGrunnlag(UttakInput input) {
        var arbeid = new Arbeid.Builder();

        var beregningsgrunnlagStatuser = input.getBeregningsgrunnlagStatuser();
        var ytelseFordelingAggregat = ytelsesfordelingRepository.hentAggregat(
            input.getBehandlingReferanse().behandlingId());
        var uttakYrkesaktiviteter = new UttakYrkesaktiviteter(input);

        if (beregningsgrunnlagStatuser.isEmpty()) {
            throw new IllegalStateException("Beregningsgrunnlag mangler status");
        }

        var startdatoer = finnStartdatoer(beregningsgrunnlagStatuser, uttakYrkesaktiviteter);
        beregningsgrunnlagStatuser.forEach(bgs -> {
            var identifikator = bgs.toUttakAktivitetIdentifikator();
            var arbeidsforhold = new Arbeidsforhold(identifikator, startdatoer.get(identifikator));
            arbeid.arbeidsforhold(arbeidsforhold);
        });

        ytelseFordelingAggregat.getGjeldendeFordeling().getPerioder().stream()
            .map(OppgittPeriodeEntitet::getFom)
            .forEach(fom -> {
                var sumStillingsprosent =  uttakYrkesaktiviteter.summerStillingsprosentAlleYrkesaktiviteter(fom);
                arbeid.endringAvStilling(new EndringAvStilling(fom, sumStillingsprosent));
            });

        return arbeid;
    }


    private Map<AktivitetIdentifikator, LocalDate> finnStartdatoer(Collection<BeregningsgrunnlagStatus> statuser,
                                                                   UttakYrkesaktiviteter uttakYrkesaktiviteter) {
        var resultat = new HashMap<AktivitetIdentifikator, LocalDate>();
        for (var statusPeriode : statuser) {
            var startdato = finnStartdato(uttakYrkesaktiviteter, statusPeriode);
            resultat.put(statusPeriode.toUttakAktivitetIdentifikator(), startdato);
        }
        return resultat;
    }

    private LocalDate finnStartdato(UttakYrkesaktiviteter uttakYrkesaktiviteter,
                                    BeregningsgrunnlagStatus statusPeriode) {
        return statusPeriode.getArbeidsgiver()
            .flatMap(ag -> uttakYrkesaktiviteter.finnStartdato(ag, statusPeriode.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef())))
            .orElse(LocalDate.MIN);
    }
}
