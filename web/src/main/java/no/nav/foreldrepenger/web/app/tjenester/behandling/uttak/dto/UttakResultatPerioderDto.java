package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

public class UttakResultatPerioderDto {

    private List<UttakResultatPeriodeDto> perioderSøker;
    private List<UttakResultatPeriodeDto> perioderAnnenpart;
    private boolean annenForelderHarRett;
    private boolean aleneomsorg;

    public UttakResultatPerioderDto(List<UttakResultatPeriodeDto> perioderSøker,
                                    List<UttakResultatPeriodeDto> perioderAnnenpart,
                                    boolean annenForelderHarRett,
                                    boolean aleneomsorg) {
        this.perioderSøker = perioderSøker;
        this.perioderAnnenpart = perioderAnnenpart;
        this.annenForelderHarRett = annenForelderHarRett;
        this.aleneomsorg = aleneomsorg;
    }

    public List<UttakResultatPeriodeDto> getPerioderSøker() {
        return perioderSøker;
    }

    public List<UttakResultatPeriodeDto> getPerioderAnnenpart() {
        return perioderAnnenpart;
    }

    public boolean isAnnenForelderHarRett() {
        return annenForelderHarRett;
    }

    public boolean isAleneomsorg() {
        return aleneomsorg;
    }
}
