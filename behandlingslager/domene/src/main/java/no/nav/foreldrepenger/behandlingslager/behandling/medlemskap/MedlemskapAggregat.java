package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable aggregat som samler informasjon fra ulike kilder for Medlemskap informasjon fra registere og søknad
 */
public class MedlemskapAggregat {

    private final Set<MedlemskapPerioderEntitet> registrertMedlemskapPeridoer;
    private final MedlemskapOppgittTilknytningEntitet oppgittTilknytning;

    public MedlemskapAggregat(Set<MedlemskapPerioderEntitet> medlemskapPerioder,
                              MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
        this.registrertMedlemskapPeridoer = medlemskapPerioder;
        this.oppgittTilknytning = oppgittTilknytning;
    }

    /** Hent Registrert medlemskapinformasjon (MEDL) slik det er innhentet. */
    public Set<MedlemskapPerioderEntitet> getRegistrertMedlemskapPerioder() {
        return registrertMedlemskapPeridoer;
    }

    public List<MedlemskapPerioderEntitet> getRegistrertMedlemskapPerioderList() {
        return registrertMedlemskapPeridoer.stream().sorted(MedlemskapPerioderEntitet.COMP_MEDLEMSKAP_PERIODER).toList();
    }

    /** Hent Oppgitt tilknytning slik det er oppgitt av Søker. */
    public Optional<MedlemskapOppgittTilknytningEntitet> getOppgittTilknytning() {
        return Optional.ofNullable(oppgittTilknytning);
    }
}
