package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;

public record OppgittRettighetDto(boolean aleneomsorgForBarnet) {

    public static OppgittRettighetDto mapFra(OppgittRettighetEntitet oppgittRettighet) {
        if (oppgittRettighet == null) {
            return null;
        }
        return new OppgittRettighetDto(oppgittRettighet.getHarAleneomsorgForBarnet());
    }
}
