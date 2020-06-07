package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.ArbeidsforholdIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;


class UttakResultatHolderImpl implements UttakResultatHolder {

    private Optional<SvangerskapspengerUttakResultatEntitet> uttakresultat;


    public UttakResultatHolderImpl(Optional<SvangerskapspengerUttakResultatEntitet> uttakresultat) {
        this.uttakresultat = uttakresultat;
    }

    @Override
    public Object getUttakResultat(){
        return uttakresultat.orElse(null);
    }

    @Override
    public LocalDate getSisteDagAvSistePeriode() {
        return uttakresultat.flatMap(SvangerskapspengerUttakResultatEntitet::finnSisteUttaksdato).orElse(LocalDate.MIN);
    }

    @Override
    public LocalDate getFørsteDagAvFørstePeriode() {
        return uttakresultat.flatMap(SvangerskapspengerUttakResultatEntitet::finnFørsteUttaksdato).orElse(LocalDate.MAX);
    }

    @Override
    public Optional<BehandlingVedtak> getBehandlingVedtak() {
        return uttakresultat.map(svangerskapspengerUttakResultatEntitet -> svangerskapspengerUttakResultatEntitet.getBehandlingsresultat().getBehandlingVedtak());
    }

    @Override
    public boolean eksistererUttakResultat() {
        return uttakresultat.isPresent();
    }

    @Override
    public List<ForeldrepengerUttakPeriode> getGjeldendePerioder() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean kontrollerErSisteUttakAvslåttMedÅrsak() {
        if (uttakresultat.isEmpty()) {
            return false;
        }
        var antallAvslåtteArbeidsforhold = uttakresultat.get().getUttaksResultatArbeidsforhold().stream()
            .filter(arbeidsforhold ->
                arbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak() != null &&
                !arbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak().equals(ArbeidsforholdIkkeOppfyltÅrsak.INGEN))
            .count();
        if (antallAvslåtteArbeidsforhold == uttakresultat.get().getUttaksResultatArbeidsforhold().size()) {
            return true;
        }
        for (SvangerskapspengerUttakResultatPeriodeEntitet periode : finnSisteUttaksperiodePrArbeidsforhold()) {
            if (PeriodeIkkeOppfyltÅrsak.opphørsAvslagÅrsaker().contains(periode.getPeriodeIkkeOppfyltÅrsak())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean vurderOmErEndringIUttakFraEndringsdato(LocalDate endringsdato, UttakResultatHolder uttakresultatSammenligneMed){

        if(uttakresultatSammenligneMed.eksistererUttakResultat() != this.eksistererUttakResultat() ){
            return true;
        }
        if(!uttakresultatSammenligneMed.eksistererUttakResultat()){
            return true;
        }

        if (!uttakresultatSammenligneMed.getSisteDagAvSistePeriode().isEqual(this.getSisteDagAvSistePeriode()) ) {
            return true;
        }

        if (!uttakresultatSammenligneMed.getFørsteDagAvFørstePeriode().isEqual(this.getFørsteDagAvFørstePeriode()) ) {
            return true;
        }

        List<SvangerskapspengerUttakResultatArbeidsforholdEntitet>  listeMedArbeidsforhold_1 =uttakresultat.get().getUttaksResultatArbeidsforhold();
        SvangerskapspengerUttakResultatEntitet resultatSammenligne = (SvangerskapspengerUttakResultatEntitet) uttakresultatSammenligneMed.getUttakResultat();
        List<SvangerskapspengerUttakResultatArbeidsforholdEntitet>  listeMedArbeidsforhold_2 = resultatSammenligne.getUttaksResultatArbeidsforhold();

        return !erLikresultat(listeMedArbeidsforhold_1,listeMedArbeidsforhold_2);

    }

    private boolean erLikresultat(List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> r1, List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> r2) {
        if (r1.size() != r2.size())
            return false;
        return r1.stream().allMatch(a1 -> r2.stream().anyMatch(a2 -> erLikArbeid(a1, a2)));
    }

    private boolean erLikArbeid(SvangerskapspengerUttakResultatArbeidsforholdEntitet a1, SvangerskapspengerUttakResultatArbeidsforholdEntitet a2) {
        if (a1.getPerioder().size() != a2.getPerioder().size())
            return false;
        var likeperioder = a1.getPerioder().stream().allMatch(p1 -> a2.getPerioder().stream().anyMatch(p2 -> erLikResPeriode(p1, p2)));
        return Objects.equals(a1.getArbeidsforholdIkkeOppfyltÅrsak(), a2.getArbeidsforholdIkkeOppfyltÅrsak()) &&
            Objects.equals(a1.getArbeidsgiver(), a2.getArbeidsgiver()) &&
            Objects.equals(a1.getArbeidsforholdRef(), a2.getArbeidsforholdRef()) &&
            likeperioder;
    }

    private boolean erLikResPeriode(SvangerskapspengerUttakResultatPeriodeEntitet r1, SvangerskapspengerUttakResultatPeriodeEntitet r2) {
        return Objects.equals(r1.getTidsperiode(), r2.getTidsperiode()) &&
            Objects.equals(r1.getPeriodeIkkeOppfyltÅrsak(), r2.getPeriodeIkkeOppfyltÅrsak()) &&
            Objects.equals(r1.getPeriodeResultatType(), r2.getPeriodeResultatType()) &&
            (Objects.equals(r1.getUtbetalingsgrad(), r2.getUtbetalingsgrad()) || r1.getUtbetalingsgrad().compareTo(r2.getUtbetalingsgrad()) == 0);
    }


    private List<SvangerskapspengerUttakResultatPeriodeEntitet> finnSisteUttaksperiodePrArbeidsforhold() {
        List<SvangerskapspengerUttakResultatPeriodeEntitet> sistePerioder = new ArrayList<>();
        uttakresultat.ifPresent(ur -> ur.getUttaksResultatArbeidsforhold()
                .forEach(arbeidsforhold -> arbeidsforhold.getPerioder().stream()
                    .max(Comparator.comparing(SvangerskapspengerUttakResultatPeriodeEntitet::getTom))
                    .ifPresent(sistePerioder::add)));

        return sistePerioder;
    }
}
