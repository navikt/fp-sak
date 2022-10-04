package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.UttakPeriodeEndringDto;

@ApplicationScoped
public class FastsettePerioderEndringTjeneste {

    private FpUttakRepository fpUttakRepository;

    FastsettePerioderEndringTjeneste() {
        //CDI
    }

    @Inject
    public FastsettePerioderEndringTjeneste(FpUttakRepository fpUttakRepository) {
        this.fpUttakRepository = fpUttakRepository;
    }

    public List<UttakPeriodeEndringDto> finnEndringerMellomOpprinneligOgOverstyrt(Long uttakResultatId) {
        var uttakResultat = fpUttakRepository.hentUttakResultatPåId(uttakResultatId)
            .orElseThrow(
                () -> new IllegalStateException("Fant ingen uttakresultat med id " + uttakResultatId.toString()));
        return lagEndringDto(uttakResultat);
    }

    public List<UttakPeriodeEndringDto> finnEndringerMellomOpprinneligOgOverstyrtForBehandling(Long behandlingId) {
        var uttakResultat = fpUttakRepository.hentUttakResultat(behandlingId);
        return lagEndringDto(uttakResultat);
    }

    private List<UttakPeriodeEndringDto> lagEndringDto(UttakResultatEntitet uttakResultat) {
        if (uttakResultat.getOverstyrtPerioder() == null || uttakResultat.getOverstyrtPerioder()
            .getPerioder()
            .isEmpty()) {
            return Collections.emptyList();
        }

        List<UttakPeriodeEndringDto> perioderMedEndringer = new ArrayList<>();
        perioderMedEndringer.addAll(finnEndringerAvOpprinnelig(uttakResultat));
        perioderMedEndringer.addAll(finnEndringerAvOverstyrt(uttakResultat));

        return perioderMedEndringer;
    }

    private List<UttakPeriodeEndringDto> finnEndringerAvOpprinnelig(UttakResultatEntitet uttakResultat) {
        List<UttakPeriodeEndringDto> perioderMedEndringer = new ArrayList<>();
        for (var opprinneligPeriode : uttakResultat.getOpprinneligPerioder().getPerioder()) {
            if (erSlettet(opprinneligPeriode, uttakResultat.getOverstyrtPerioder())) {
                perioderMedEndringer.add(lagEndretDto(opprinneligPeriode.getFom(), opprinneligPeriode.getTom(),
                    UttakPeriodeEndringDto.TypeEndring.SLETTET));
            }
        }
        return perioderMedEndringer;
    }

    private List<UttakPeriodeEndringDto> finnEndringerAvOverstyrt(UttakResultatEntitet uttakResultat) {
        List<UttakPeriodeEndringDto> perioderMedEndringer = new ArrayList<>();
        for (var overstyrtPeriode : uttakResultat.getOverstyrtPerioder().getPerioder()) {
            if (erLagtTil(overstyrtPeriode, uttakResultat.getOpprinneligPerioder())) {
                perioderMedEndringer.add(lagEndretDto(overstyrtPeriode.getFom(), overstyrtPeriode.getTom(),
                    UttakPeriodeEndringDto.TypeEndring.LAGT_TIL));
            } else if (erEndret(overstyrtPeriode, uttakResultat.getOpprinneligPerioder())) {
                perioderMedEndringer.add(lagEndretDto(overstyrtPeriode.getFom(), overstyrtPeriode.getTom(),
                    UttakPeriodeEndringDto.TypeEndring.ENDRET));
            }
        }
        return perioderMedEndringer;
    }

    private boolean erSlettet(UttakResultatPeriodeEntitet opprinneligPeriode,
                              UttakResultatPerioderEntitet overstyrtPerioder) {
        for (var overstyrtPeriode : overstyrtPerioder.getPerioder()) {
            if (overstyrtPeriode.getTom().isBefore(opprinneligPeriode.getTom()) && overstyrtPeriode.getFom()
                .isEqual(opprinneligPeriode.getFom())) {
                return true;
            }
        }
        return false;
    }

    private boolean erEndret(UttakResultatPeriodeEntitet overstyrtPeriode,
                             UttakResultatPerioderEntitet opprinneligePerioder) {
        for (var opprinnelig : opprinneligePerioder.getPerioder()) {
            if (erLik(opprinnelig, overstyrtPeriode)) {
                return false;
            }
        }
        return true;
    }

    private boolean erLagtTil(UttakResultatPeriodeEntitet overstyrtPeriode,
                              UttakResultatPerioderEntitet opprinneligePerioder) {
        for (var opprinnligPeriode : opprinneligePerioder.getPerioder()) {
            if (opprinnligPeriode.getFom().isEqual(overstyrtPeriode.getFom()) && opprinnligPeriode.getTom()
                .isEqual(overstyrtPeriode.getTom())) {
                return false;
            }
        }
        return true;
    }

    private boolean erLik(UttakResultatPeriodeEntitet periode1, UttakResultatPeriodeEntitet periode2) {
        if (!Objects.equals(periode1.getFom(), periode2.getFom()) || !Objects.equals(periode1.getTom(),
            periode2.getTom()) || !Objects.equals(periode1.getResultatType(), periode2.getResultatType())
            || !Objects.equals(periode1.getBegrunnelse(), periode2.getBegrunnelse())) {
            return false;
        }

        if (periode1.getAktiviteter().size() != periode2.getAktiviteter().size()) {
            return false;
        }

        for (var aktivitet1 : periode1.getAktiviteter()) {
            var fantLikAktivetet = false;
            for (var aktivitet2 : periode2.getAktiviteter()) {
                if (erLik(aktivitet1, aktivitet2)) {
                    fantLikAktivetet = true;
                }
            }
            if (!fantLikAktivetet) {
                return false;
            }
        }
        return true;
    }

    private boolean erLik(UttakResultatPeriodeAktivitetEntitet aktivitet1,
                          UttakResultatPeriodeAktivitetEntitet aktivitet2) {
        return Objects.equals(aktivitet1.getFom(), aktivitet2.getFom())
            && Objects.equals(aktivitet1.getTom(), aktivitet2.getTom())
            && Objects.equals(aktivitet1.getTrekkonto(), aktivitet2.getTrekkonto())
            && Objects.equals(aktivitet1.getTrekkdager(), aktivitet2.getTrekkdager())
            && Objects.equals(aktivitet1.getArbeidsprosentSomStillingsprosent(), aktivitet2.getArbeidsprosentSomStillingsprosent())
            && Objects.equals(aktivitet1.getUtbetalingsgrad(), aktivitet2.getUtbetalingsgrad());
    }


    private UttakPeriodeEndringDto lagEndretDto(LocalDate fom,
                                                LocalDate tom,
                                                UttakPeriodeEndringDto.TypeEndring endringType) {
        return new UttakPeriodeEndringDto.Builder().medTypeEndring(endringType).medPeriode(fom, tom).build();
    }
}
