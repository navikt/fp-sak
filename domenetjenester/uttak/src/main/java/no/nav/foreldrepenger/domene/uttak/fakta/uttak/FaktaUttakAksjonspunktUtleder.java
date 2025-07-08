package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FAKTA_UTTAK_INGEN_PERIODER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.ARBEID;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.FRILANS;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.uttak.PerioderUtenHelgUtil;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

@ApplicationScoped
public class FaktaUttakAksjonspunktUtleder {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public FaktaUttakAksjonspunktUtleder(YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    FaktaUttakAksjonspunktUtleder() {
        //CDI
    }

    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(input.getBehandlingReferanse().behandlingId());
        return utledAksjonspunkterFor(input, ytelseFordelingAggregat.getJustertFordeling().orElseThrow().getPerioder());
    }

    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input, List<OppgittPeriodeEntitet> perioder) {

        var list = new ArrayList<AksjonspunktDefinisjon>();
        if (perioder.isEmpty()) {
            list.add(FAKTA_UTTAK_INGEN_PERIODER);
        }
        if (input.finnesAndelerMedGraderingUtenBeregningsgrunnlag()) {
            list.add(FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG);
        }
        Optional<AktørArbeid> aktørArbeidFraRegister = input.getIayGrunnlag() == null
            ? Optional.empty()
            : input.getIayGrunnlag().getAktørArbeidFraRegister(input.getBehandlingReferanse().aktørId());
        if (graderingPåAktivitetIkkeIBeregning(input.getBeregningsgrunnlagStatuser(), perioder) ||
            graderingPåAktivitetIkkeIIay(aktørArbeidFraRegister, perioder)) {
            list.add(FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET);
        }
        var erMor = RelasjonsRolleType.erMor(input.getBehandlingReferanse().relasjonRolle());
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(input.getBehandlingReferanse().behandlingId());
        ytelseFordelingAggregat.getAvklarteDatoer()
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato)
            .filter(fud -> !avklartStartdatLikFørsteDagIPerioder(perioder, erMor, fud))
            .ifPresent(fud -> list.add(FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO));
        return list;
    }

    private static boolean avklartStartdatLikFørsteDagIPerioder(List<OppgittPeriodeEntitet> perioder, boolean sammenhengendeEllerMor, LocalDate avklartFUD) {
        return førsteSøkteDag(perioder, sammenhengendeEllerMor).map(
            oppgittPeriodeEntitet -> PerioderUtenHelgUtil.datoerLikeNårHelgIgnoreres(oppgittPeriodeEntitet.getFom(), avklartFUD)).orElse(true);
    }

    private static Optional<OppgittPeriodeEntitet> førsteSøkteDag(List<OppgittPeriodeEntitet> perioder, boolean erMor) {
        if (erMor) {
            return perioder.stream().min(Comparator.comparing(OppgittPeriodeEntitet::getFom));
        } else {
            return UtsettelseCore2021.finnFørsteOppgittPeriodeFra(perioder);
        }
    }

    private static boolean graderingPåAktivitetIkkeIIay(Optional<AktørArbeid> aktørArbeid,
                                                        List<OppgittPeriodeEntitet> perioder) {
        var yrkesaktiviteter = aktørArbeid.map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(List.of());
        return perioder.stream()
            .filter(OppgittPeriodeEntitet::isGradert)
            .filter(op -> op.getGraderingAktivitetType() == ARBEID && op.getArbeidsgiver() != null)
            .anyMatch(periode -> gradererUkjentIAYAktivitet(periode, yrkesaktiviteter, finnFørsteGraderteFomArbeidsgiver(periode.getArbeidsgiver(), perioder)));
    }

    private static LocalDate finnFørsteGraderteFomArbeidsgiver(Arbeidsgiver arbeidsgiver, List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(p -> Objects.equals(p.getArbeidsgiver(), arbeidsgiver))
            .filter(OppgittPeriodeEntitet::isGradert)
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder())
            .orElseThrow();
    }

    private static boolean graderingPåAktivitetIkkeIBeregning(Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser, List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(OppgittPeriodeEntitet::isGradert)
            .anyMatch(periode -> gradererUkjentBeregningAktivitet(periode, beregningsgrunnlagStatuser));
    }

    private static boolean gradererUkjentIAYAktivitet(OppgittPeriodeEntitet søknadsperiode,
                                                      Collection<Yrkesaktivitet> iayAktiviteter,
                                                      LocalDate førsteDatoMedGraderingForAG) {

        // Sjekker kun arbeid, antar at andre graderinger er ok
        if (søknadsperiode.getGraderingAktivitetType() == ARBEID) {
            return iayAktiviteter.stream().filter(ya -> Objects.equals(ya.getArbeidsgiver(), søknadsperiode.getArbeidsgiver()))
                .map(Yrkesaktivitet::getAlleAktivitetsAvtaler)
                .flatMap(Collection::stream)
                .filter(AktivitetsAvtale::erAnsettelsesPeriode)
                .noneMatch(aa -> aa.getPeriode().inkluderer(førsteDatoMedGraderingForAG));
        }
        return false;
    }

    private static boolean gradererUkjentBeregningAktivitet(OppgittPeriodeEntitet søknadsperiode, Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        if (søknadsperiode.getGraderingAktivitetType() == ARBEID && !beregningHarArbeidsgiver(søknadsperiode.getArbeidsgiver(), beregningsgrunnlagStatuser)) {
            return true;
        }
        if (søknadsperiode.getGraderingAktivitetType() == FRILANS && !beregningHarFrilanser(beregningsgrunnlagStatuser)) {
            return true;
        }
        return søknadsperiode.getGraderingAktivitetType() == SELVSTENDIG_NÆRINGSDRIVENDE && !beregningHarSelvstendigNæringsdrivende(beregningsgrunnlagStatuser);
    }

    private static boolean beregningHarSelvstendigNæringsdrivende(Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        return beregningsgrunnlagStatuser.stream().anyMatch(BeregningsgrunnlagStatus::erSelvstendigNæringsdrivende);
    }

    private static boolean beregningHarFrilanser(Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        return beregningsgrunnlagStatuser.stream().anyMatch(BeregningsgrunnlagStatus::erFrilanser);
    }

    private static boolean beregningHarArbeidsgiver(Arbeidsgiver arbeidsgiver, Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        if (arbeidsgiver == null) {
            return beregningsgrunnlagStatuser.stream().anyMatch(BeregningsgrunnlagStatus::erArbeidstaker);
        }
        return beregningsgrunnlagStatuser.stream().anyMatch(statusPeriode ->
            statusPeriode.erArbeidstaker()
                && statusPeriode.getArbeidsgiver().isPresent()
                && statusPeriode.getArbeidsgiver().get().equals(arbeidsgiver)
        );
    }
}
