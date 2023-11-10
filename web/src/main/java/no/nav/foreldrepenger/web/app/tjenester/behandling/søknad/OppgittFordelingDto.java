package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;

public class OppgittFordelingDto {
    private Optional<LocalDate> startDatoForPermisjon;

    public OppgittFordelingDto() {
        // trengs for deserialisering av JSON
    }

    private OppgittFordelingDto(Optional<LocalDate> startDatoForPermisjon) {
        this.startDatoForPermisjon = startDatoForPermisjon;
    }

    public static OppgittFordelingDto mapFra(OppgittFordelingEntitet oppgittFordeling, Optional<LocalDate> oppgittStartDatoForPermisjon) {
        if (oppgittFordeling != null) {
            return new OppgittFordelingDto(oppgittStartDatoForPermisjon);
        }
        return null;
    }

    public Optional<LocalDate> getStartDatoForPermisjon() {
        return startDatoForPermisjon;
    }

    public void setStartDatoForPermisjon(Optional<LocalDate> startDatoForPermisjon) {
        this.startDatoForPermisjon = startDatoForPermisjon;
    }
}
