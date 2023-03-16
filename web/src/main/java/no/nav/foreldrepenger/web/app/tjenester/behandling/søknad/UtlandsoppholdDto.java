package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;

public class UtlandsoppholdDto {
    private String landNavn;
    private LocalDate fom;
    private LocalDate tom;

    public UtlandsoppholdDto() {
        // trengs for deserialisering av JSON
    }

    private UtlandsoppholdDto(String landNavn, LocalDate fom, LocalDate tom) {
        this.landNavn = landNavn;
        this.fom = fom;
        this.tom = tom;
    }

    public static List<UtlandsoppholdDto> mapFra(List<MedlemskapOppgittLandOppholdEntitet> utlandsoppholdList) {
        return utlandsoppholdList.stream()
                .map(utlandsopphold -> new UtlandsoppholdDto(
                        utlandsopphold.getLand().getNavn(),
                        utlandsopphold.getPeriodeFom(),
                        utlandsopphold.getPeriodeTom())
                ).toList();
    }

    public String getLandNavn() {
        return landNavn;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }
}
