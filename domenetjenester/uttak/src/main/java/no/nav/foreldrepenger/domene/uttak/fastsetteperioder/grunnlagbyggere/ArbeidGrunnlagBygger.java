package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.mapArbeidsgiver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakYrkesaktiviteter;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetType;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Arbeid;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.EndringAvStilling;

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
        var arbeidsforhold = lagArbeidsforhold(input);
        for (var a : arbeidsforhold) {
            arbeid.arbeidsforhold(a);
        }
        return arbeid;
    }

    private Set<Arbeidsforhold> lagArbeidsforhold(UttakInput input) {
        var resultat = new HashSet<Arbeidsforhold>();
        var beregningsgrunnlagStatuser = input.getBeregningsgrunnlagStatuser();
        var ytelseFordelingAggregat = ytelsesfordelingRepository.hentAggregat(
            input.getBehandlingReferanse().behandlingId());
        var uttakYrkesaktiviteter = new UttakYrkesaktiviteter(input);

        if (beregningsgrunnlagStatuser.isEmpty()) {
            throw new IllegalStateException("Beregningsgrunnlag mangler status");
        }

        for (var beregningsgrunnlagStatus : beregningsgrunnlagStatuser) {
            var arbeidsforhold = lagArbeidsforhold(beregningsgrunnlagStatuser, beregningsgrunnlagStatus,
                ytelseFordelingAggregat, uttakYrkesaktiviteter);
            resultat.add(arbeidsforhold);
        }

        return resultat;
    }

    private Arbeidsforhold lagArbeidsforhold(Collection<BeregningsgrunnlagStatus> alleStatuser,
                                             BeregningsgrunnlagStatus statusPeriode,
                                             YtelseFordelingAggregat ytelseFordelingAggregat,
                                             UttakYrkesaktiviteter uttakYrkesaktiviteter) {
        var identifikator = statusPeriode.toUttakAktivitetIdentifikator();
        var startdatoer = finnStartdatoer(alleStatuser, uttakYrkesaktiviteter);

        var arbeidsforhold = new Arbeidsforhold(identifikator,
            startdatoer.get(statusPeriode.toUttakAktivitetIdentifikator()));

        if (erArbeidstakerMedArbeidsgiver(arbeidsforhold)) {
            var endringerIStilling = finnEndringerIStilling(identifikator, ytelseFordelingAggregat, uttakYrkesaktiviteter).stream()
                .sorted(Comparator.comparing(EndringAvStilling::getDato))
                .toList();
            for (var endringAvStilling : endringerIStilling) {
                arbeidsforhold.leggTilEndringIStilling(endringAvStilling);
            }
        }

        return arbeidsforhold;
    }

    private boolean erArbeidstakerMedArbeidsgiver(Arbeidsforhold arbeidsforhold) {
        return arbeidsforhold.getIdentifikator().getAktivitetType().equals(AktivitetType.ARBEID)
            && arbeidsforhold.getIdentifikator().getArbeidsgiverIdentifikator() != null;
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
        if (statusPeriode.getArbeidsgiver().isPresent()) {
            var arbeidsforholdRef = statusPeriode.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef());
            return uttakYrkesaktiviteter.finnStartdato(statusPeriode.getArbeidsgiver().get(), arbeidsforholdRef)
                .orElse(LocalDate.MIN);
        }
        return LocalDate.MIN;
    }

    private Set<EndringAvStilling> finnEndringerIStilling(AktivitetIdentifikator aktivitetIdentifikator,
                                                          YtelseFordelingAggregat ytelseFordelingAggregat,
                                                          UttakYrkesaktiviteter uttakYrkesaktiviteter) {
        //Forenkling: Henter ikke faktisk endring. Bare sjekker stillingsprosent første dag i søknadsperioder
        var endringer = new HashSet<EndringAvStilling>();
        for (var søknadsperiode : ytelseFordelingAggregat.getGjeldendeSøknadsperioder()
            .getOppgittePerioder()) {
            var stillingsprosent = finnStillingsprosent(aktivitetIdentifikator, uttakYrkesaktiviteter,
                søknadsperiode.getFom());
            endringer.add(new EndringAvStilling(søknadsperiode.getFom(), stillingsprosent));
        }

        return endringer;
    }

    private BigDecimal finnStillingsprosent(AktivitetIdentifikator aktivitetIdentifikator,
                                            UttakYrkesaktiviteter uttakYrkesaktiviteter,
                                            LocalDate dato) {
        var arbeidsgiver = mapArbeidsgiver(aktivitetIdentifikator);
        var ref = InternArbeidsforholdRef.ref(aktivitetIdentifikator.getArbeidsforholdId());
        return uttakYrkesaktiviteter.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, ref, dato);
    }
}
