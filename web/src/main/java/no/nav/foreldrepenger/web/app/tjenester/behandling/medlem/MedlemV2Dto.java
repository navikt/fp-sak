package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class MedlemV2Dto {

    private List<InntektDto> inntekt;
    private List<MedlemskapPerioderDto> medlemskapPerioder;
    private Set<MedlemPeriodeDto> perioder;
    private List<OppholdstilltatelseDto> opphold;
    private LocalDate fom; // gjeldendeFra

    public MedlemV2Dto() {
        // trengs for deserialisering av JSON
    }

    public List<InntektDto> getInntekt() {
        return inntekt;
    }

    void setInntekt(List<InntektDto> inntekt) {
        this.inntekt = inntekt;
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

    public List<OppholdstilltatelseDto> getOpphold() {
        return opphold;
    }

    public void setOpphold(List<OppholdstilltatelseDto> opphold) {
        this.opphold = opphold;
    }
}
