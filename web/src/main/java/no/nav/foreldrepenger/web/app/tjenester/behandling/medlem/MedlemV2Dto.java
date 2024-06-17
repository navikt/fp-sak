package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MedlemV2Dto {

    private List<MedlemskapPerioderDto> medlemskapPerioder = new ArrayList<>();
    private Set<MedlemPeriodeDto> perioder = new HashSet<>();
    private List<OppholdstillatelseDto> opphold = new ArrayList<>();
    private LocalDate fom; // Opphør fra dato - stp eller senere

    public MedlemV2Dto() {
        // trengs for deserialisering av JSON
    }

    public List<MedlemskapPerioderDto> getMedlemskapPerioder() {
        return medlemskapPerioder;
    }

    void setMedlemskapPerioder(List<MedlemskapPerioderDto> medlemskapPerioder) {
        this.medlemskapPerioder = medlemskapPerioder;
    }

    public Set<MedlemPeriodeDto> getPerioder() {
        return perioder;
    }

    void setPerioder(Set<MedlemPeriodeDto> perioder) {
        this.perioder = perioder;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public List<OppholdstillatelseDto> getOpphold() {
        return opphold;
    }

    public void setOpphold(List<OppholdstillatelseDto> opphold) {
        this.opphold = opphold;
    }
}
