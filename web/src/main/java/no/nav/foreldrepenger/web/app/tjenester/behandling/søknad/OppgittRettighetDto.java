package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;

public record OppgittRettighetDto(boolean omsorgForBarnet, boolean aleneomsorgForBarnet) {

    public static OppgittRettighetDto mapFra(OppgittRettighetEntitet oppgittRettighet) {
        if (oppgittRettighet == null) {
            return null;
        }
        //TODO TFP-4962 palfi, rydd frontend
        return new OppgittRettighetDto(true, oppgittRettighet.getHarAleneomsorgForBarnet());
    }
}
