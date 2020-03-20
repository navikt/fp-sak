package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
        if (!uttakresultat.isPresent() || (!uttakresultat.get().finnSisteUttaksdato().isPresent() )) {
            return LocalDate.MIN;
        }
        return uttakresultat.get().finnSisteUttaksdato().get();
    }

    @Override
    public LocalDate getFørsteDagAvFørstePeriode() {
        if (!uttakresultat.isPresent() ) {
            return LocalDate.MIN;
        }
        return uttakresultat.get().finnFørsteUttaksdato().get();
    }

    @Override
    public Optional<BehandlingVedtak> getBehandlingVedtak() {
        if (uttakresultat.isPresent()) {
            return Optional.ofNullable(uttakresultat.get().getBehandlingsresultat().getBehandlingVedtak());
        }
        return Optional.empty();
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
        if (!uttakresultat.isPresent()) {
            return false;
        }
        var antallAvslåtteArbeidsforhold = uttakresultat.get().getUttaksResultatArbeidsforhold().stream()
            .filter(arbeidsforhold ->
                arbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak() != null &&
                !arbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak().equals(ArbeidsforholdIkkeOppfyltÅrsak.INGEN))
            .count();
        if (antallAvslåtteArbeidsforhold > 0){
            return true;
        }
        Set<PeriodeIkkeOppfyltÅrsak> opphørsAvslagÅrsaker = PeriodeIkkeOppfyltÅrsak.opphørsAvslagÅrsaker();
        return opphørsAvslagÅrsaker.contains(finnSisteUttaksperiode().getPeriodeIkkeOppfyltÅrsak());
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

        return !erUttaksResultatArbeidsforholdLike(listeMedArbeidsforhold_1,listeMedArbeidsforhold_2);

    }

    private boolean erUttaksResultatArbeidsforholdLike(List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> listeMedArbeidsforhold1, List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> listeMedArbeidsforhold2) {
        // Sjekk på Ny/fjernet
        if (listeMedArbeidsforhold1.size() != listeMedArbeidsforhold2.size()) {
            return false;
        }
        int antallPerioder = listeMedArbeidsforhold1.size();
        for (int i = 0; i < antallPerioder; i++) {
            SvangerskapspengerUttakResultatArbeidsforholdEntitet uttakResultatArbeidsforhold1 = listeMedArbeidsforhold1.get(i);
            SvangerskapspengerUttakResultatArbeidsforholdEntitet uttakResultatArbeidsforhold2 = listeMedArbeidsforhold2.get(i);

            if (!uttakResultatArbeidsforhold1.getArbeidsforholdIkkeOppfyltÅrsak().equals(uttakResultatArbeidsforhold2.getArbeidsforholdIkkeOppfyltÅrsak())) {
                return false;
            }

            if (!Objects.equals(uttakResultatArbeidsforhold1.getArbeidsgiver(), uttakResultatArbeidsforhold2.getArbeidsgiver())) {
                return false;
            }

            if (!Objects.equals(uttakResultatArbeidsforhold1.getArbeidsforholdRef(), uttakResultatArbeidsforhold2.getArbeidsforholdRef())) {
                return false;
            }

            if (!erUttakresultatperiodeneLike(uttakResultatArbeidsforhold1.getPerioder(), uttakResultatArbeidsforhold2.getPerioder())) {
                return false;
            }
        }
        return true;
    }

    private boolean erUttakresultatperiodeneLike(List<SvangerskapspengerUttakResultatPeriodeEntitet>  perioder1, List<SvangerskapspengerUttakResultatPeriodeEntitet>  perioder2) {

        if (perioder1.size() != perioder2.size()) {
            return false;
        }
        int antallPerioder = perioder1.size();
        for (int i = 0; i < antallPerioder; i++) {

            SvangerskapspengerUttakResultatPeriodeEntitet periode1 = perioder1.get(i);
            SvangerskapspengerUttakResultatPeriodeEntitet periode2 = perioder2.get(i);

            if (periode1.getUtbetalingsgrad().compareTo(periode2.getUtbetalingsgrad())!=0) {
                return false;
            }

            if (!periode1.getTidsperiode().equals(periode2.getTidsperiode())) {
                return false;
            }

            if (!periode1.getFom().equals(periode2.getFom())) {
                return false;
            }
            if (!periode1.getTom().equals(periode2.getTom())) {
                return false;
            }

            if (!periode1.getPeriodeIkkeOppfyltÅrsak().equals(periode2.getPeriodeIkkeOppfyltÅrsak())) {
                return false;
            }

            if (!periode1.getPeriodeResultatType().equals(periode2.getPeriodeResultatType())) {
                return false;
            }

        }
        return true;
    }


    private SvangerskapspengerUttakResultatPeriodeEntitet finnSisteUttaksperiode() {
        if (uttakresultat.isPresent()) {
            List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> uttaksResultatArbeidsforholdListe = uttakresultat.get().getUttaksResultatArbeidsforhold();
            List<SvangerskapspengerUttakResultatPeriodeEntitet> perioder = new ArrayList<>();
            for(SvangerskapspengerUttakResultatArbeidsforholdEntitet uttakResultatArbeidsforhold  :uttaksResultatArbeidsforholdListe ){
                perioder.addAll( uttakResultatArbeidsforhold.getPerioder());
            }
            perioder.sort(Comparator.comparing(SvangerskapspengerUttakResultatPeriodeEntitet::getTom).reversed());
            if (perioder.size() > 0) {
                return perioder.get(0);
            }
        }
        return null;
    }
}
