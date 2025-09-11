package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public class OppgittTilknytningDto {

    @NotNull private boolean oppholdNorgeNa;
    @NotNull private boolean oppholdSistePeriode;
    @NotNull private boolean oppholdNestePeriode;
    @NotNull private List<UtlandsoppholdDto> utlandsoppholdFor;
    @NotNull private List<UtlandsoppholdDto> utlandsoppholdEtter;

    public OppgittTilknytningDto() {
        // trengs for deserialisering av JSON
    }

    private OppgittTilknytningDto(boolean oppholdNorgeNa,
                                  boolean oppholdSistePeriode,
                                  boolean oppholdNestePeriode,
                                  List<UtlandsoppholdDto> utlandsoppholdFor,
                                  List<UtlandsoppholdDto> utlandsoppholdEtter) {

        this.oppholdNorgeNa = oppholdNorgeNa;
        this.oppholdSistePeriode = oppholdSistePeriode;
        this.oppholdNestePeriode = oppholdNestePeriode;
        this.utlandsoppholdFor = utlandsoppholdFor;
        this.utlandsoppholdEtter = utlandsoppholdEtter;
    }

    public static OppgittTilknytningDto mapFra(MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
        if (oppgittTilknytning != null) {
            return new OppgittTilknytningDto(
                oppgittTilknytning.isOppholdNå(),
                oppgittTilknytning.isOppholdINorgeSistePeriode(),
                oppgittTilknytning.isOppholdINorgeNestePeriode(),
                mapFør(oppgittTilknytning.getOpphold()),
                mapEtter(oppgittTilknytning.getOpphold()));
        }
        return null;
    }

    private static List<UtlandsoppholdDto> mapFør(Set<MedlemskapOppgittLandOppholdEntitet> opphold) {
        return opphold.stream()
            .filter(MedlemskapOppgittLandOppholdEntitet::isTidligereOpphold)
            .filter(o -> !o.getLand().equals(Landkoder.NOR))
            .map(UtlandsoppholdDto::mapFra)
            .toList();
    }

    private static List<UtlandsoppholdDto> mapEtter(Set<MedlemskapOppgittLandOppholdEntitet> utlandsopphold) {
        return utlandsopphold.stream()
            .filter(o -> !o.isTidligereOpphold())
            .filter(o -> !o.getLand().equals(Landkoder.NOR))
            .map(UtlandsoppholdDto::mapFra)
            .toList();
    }

    public boolean isOppholdNorgeNa() {
        return oppholdNorgeNa;
    }

    public boolean isOppholdSistePeriode() {
        return oppholdSistePeriode;
    }

    public boolean isOppholdNestePeriode() {
        return oppholdNestePeriode;
    }

    public List<UtlandsoppholdDto> getUtlandsoppholdFor() {
        return utlandsoppholdFor;
    }

    public List<UtlandsoppholdDto> getUtlandsoppholdEtter() {
        return utlandsoppholdEtter;
    }
}
